package com.iagothz.mergetd.logic

import kotlin.random.Random

/**
 * One die attack. [to] lists every position hit in order (chains have several), for drawing;
 * [damage] is the direct damage each enemy took from this shot, splash and chain included.
 */
data class ShotEvent(
    val from: Vec2,
    val to: List<Vec2>,
    val type: DieType,
    val damage: Map<Enemy, Float> = emptyMap(),
    val crit: Boolean = false,
)

/** What the rest of the board adds to every attack (CRIT chance, number of COMBO dice). */
data class AttackBonus(val critChance: Float = 0f, val comboCount: Int = 1) {
    companion object {
        val NONE = AttackBonus()
    }
}

object Combat {
    fun attackInterval(t: DieType, pips: Int): Float =
        Balance.stats(t).baseInterval / (1 + Balance.ATTACK_SPEED_PER_PIP * (pips - 1))

    /** Damage before target or board modifiers. */
    fun baseDamage(t: DieType, pips: Int, powerLevel: Int): Float =
        Balance.stats(t).baseDamage * pips * Balance.powerMult(powerLevel)

    fun hitDamage(t: DieType, pips: Int, powerLevel: Int, target: Enemy): Float {
        val base = baseDamage(t, pips, powerLevel)
        return if (t == DieType.IRON && target.kind.isBoss) base * Balance.IRON_BOSS_MULT else base
    }

    fun selectTarget(t: DieType, enemies: List<Enemy>): Enemy? {
        val alive = enemies.filter { it.alive }
        return if (t == DieType.SNIPER) alive.maxByOrNull { it.hp } else alive.maxByOrNull { it.distance }
    }

    /**
     * Applies damage and the die's effect. Does not remove dead enemies or pay rewards.
     * TRAP dice don't come through here: [Match] plants their traps.
     */
    fun attack(
        die: Die,
        origin: Vec2,
        powerLevel: Int,
        enemies: List<Enemy>,
        path: EnemyPath,
        bonus: AttackBonus = AttackBonus.NONE,
        rng: Random = Random.Default,
    ): ShotEvent? {
        val target = selectTarget(die.type, enemies) ?: return null
        var damage = hitDamage(die.type, die.pips, powerLevel, target)
        when (die.type) {
            DieType.COMBO -> damage *= 1 + Balance.COMBO_BONUS * (bonus.comboCount - 1)
            DieType.HUNTER -> damage *= 1 + Balance.HUNTER_BONUS_PER_STACK * die.stacks
            DieType.LUCK -> damage *= rng.nextFloat() * Balance.LUCK_MAX_MULT
            else -> Unit
        }
        val crit = bonus.critChance > 0f && rng.nextFloat() < bonus.critChance
        if (crit) damage *= Balance.CRIT_MULT

        val targetPos = path.positionAt(target.distance)
        val dealt = linkedMapOf<Enemy, Float>()
        fun deal(e: Enemy, amount: Float) {
            dealt[e] = (dealt[e] ?: 0f) + e.takeDamage(amount, die)
        }
        val hits = mutableListOf(targetPos)

        if (die.type == DieType.QUAKE) {
            enemies.filter { it.alive }.forEach { e ->
                deal(e, damage)
                if (e !== target) hits += path.positionAt(e.distance)
            }
            return ShotEvent(origin, hits, die.type, dealt, crit)
        }
        deal(target, damage)

        when (die.type) {
            DieType.FIRE -> enemies
                .filter { it !== target && it.alive && path.positionAt(it.distance).dst(targetPos) <= Balance.FIRE_SPLASH_RADIUS }
                .forEach { deal(it, damage * Balance.FIRE_SPLASH_FRACTION) }
            DieType.ICE -> target.applySlow(
                minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * die.pips),
            )
            DieType.POISON -> target.addPoison(Balance.POISON_DPS * die.pips * Balance.powerMult(powerLevel))
            DieType.ELECTRIC -> chain(target, damage, enemies, path, hits, ::deal)
            DieType.CURSE -> target.applyCurse(Balance.CURSE_BASE + Balance.CURSE_PER_PIP * die.pips)
            DieType.GRAVITY -> {
                val push = (Balance.GRAVITY_PUSH_BASE + Balance.GRAVITY_PUSH_PER_PIP * die.pips) *
                    (if (target.kind.isBoss) Balance.GRAVITY_BOSS_FACTOR else 1f)
                target.pushBack(push)
            }
            DieType.TIME -> target.applyStun(
                (Balance.TIME_STUN_BASE + Balance.TIME_STUN_PER_PIP * die.pips) *
                    (if (target.kind.isBoss) Balance.TIME_BOSS_FACTOR else 1f),
            )
            DieType.REAPER -> {
                val threshold = (Balance.REAPER_BASE + Balance.REAPER_PER_PIP * die.pips) * target.maxHp
                if (!target.kind.isBoss && target.alive && target.hp < threshold) {
                    dealt[target] = (dealt[target] ?: 0f) + target.hp
                    target.hp = 0f
                    target.lastHit = die
                }
            }
            else -> Unit
        }
        return ShotEvent(origin, hits, die.type, dealt, crit)
    }

    private fun chain(
        first: Enemy,
        damage: Float,
        enemies: List<Enemy>,
        path: EnemyPath,
        hits: MutableList<Vec2>,
        deal: (Enemy, Float) -> Unit,
    ) {
        val struck = mutableSetOf(first)
        var last = path.positionAt(first.distance)
        var jumpDamage = damage
        repeat(Balance.CHAIN_JUMPS) {
            val next = enemies
                .filter { it.alive && it !in struck }
                .map { it to path.positionAt(it.distance).dst(last) }
                .filter { it.second <= Balance.CHAIN_RADIUS }
                .minByOrNull { it.second }
                ?.first ?: return
            jumpDamage *= Balance.CHAIN_FALLOFF
            deal(next, jumpDamage)
            struck += next
            last = path.positionAt(next.distance)
            hits += last
        }
    }
}
