package com.iagothz.mergetd.logic

import kotlin.random.Random

/**
 * One die attack. [to] lists every position hit in order (chains have several), for drawing;
 * [damage] is the direct damage each enemy took from this shot, splash and chain included.
 */
data class ShotEvent(val from: Vec2, val to: List<Vec2>, val type: DieType, val damage: Map<Enemy, Float> = emptyMap(), val crit: Boolean = false)

/**
 * What the rest of the board adds to every attack: crit chance and multiplier, the number of COMBO dice,
 * the active synergies, and the strongest base damage on the board (what a 7-pip Mirror copies).
 */
data class AttackBonus(val critChance: Float = 0f, val comboCount: Int = 1, val critMult: Float = Balance.CRIT_MULT, val synergies: Set<Synergy> = emptySet(), val strongestDamage: Float = 0f) {
    companion object {
        val NONE = AttackBonus()
    }
}

object Combat {
    private val Die.maxed: Boolean get() = pips >= Balance.MAX_PIPS

    fun attackInterval(t: DieType, pips: Int): Float {
        val base = Balance.stats(t).baseInterval / (1 + Balance.ATTACK_SPEED_PER_PIP * (pips - 1))
        return if (t == DieType.WIND && pips >= Balance.MAX_PIPS) base / 2 else base
    }

    /** Damage before target or board modifiers; [power] is the die's multiplier (power-ups times its level, see [Match.powerMult]). */
    fun baseDamage(t: DieType, pips: Int, power: Float): Float = Balance.stats(t).baseDamage * pips * power

    fun hitDamage(t: DieType, pips: Int, power: Float, target: Enemy): Float {
        val base = baseDamage(t, pips, power)
        if (t != DieType.IRON || !target.kind.isBoss) return base
        return base * if (pips >= Balance.MAX_PIPS) Balance.IRON_BOSS_MULT_MAX else Balance.IRON_BOSS_MULT
    }

    fun selectTarget(t: DieType, enemies: List<Enemy>): Enemy? {
        val alive = enemies.filter { it.alive }
        return if (t == DieType.SNIPER) alive.maxByOrNull { it.hp } else alive.maxByOrNull { it.distance }
    }

    /**
     * Applies damage and the die's effect. Does not remove dead enemies or pay rewards.
     * TRAP dice don't come through here: [Match] plants their traps.
     */
    fun attack(die: Die, origin: Vec2, power: Float, enemies: List<Enemy>, path: EnemyPath, bonus: AttackBonus = AttackBonus.NONE, rng: Random = Random.Default): ShotEvent? {
        val target = selectTarget(die.type, enemies) ?: return null
        val maxed = die.maxed
        val syn = bonus.synergies
        var damage = hitDamage(die.type, die.pips, power, target)
        when (die.type) {
            DieType.COMBO -> damage *= 1 + Balance.COMBO_BONUS * (bonus.comboCount - 1)
            DieType.HUNTER -> damage *= 1 + (if (maxed) Balance.HUNTER_BONUS_PER_STACK_MAX else Balance.HUNTER_BONUS_PER_STACK) * die.stacks
            DieType.LUCK -> damage *= (if (maxed) Balance.LUCK_MIN_MULT_MAX else 0f) + rng.nextFloat() * Balance.LUCK_MAX_MULT
            DieType.MIRROR -> if (maxed && bonus.strongestDamage > 0f) damage = bonus.strongestDamage
            else -> Unit
        }
        val pierce = Synergy.PIERCE in syn && (die.type == DieType.IRON || die.type == DieType.SNIPER)
        if (pierce && target.kind.isBoss) damage *= Balance.PIERCE_BOSS
        if (Synergy.PREDATOR in syn && (die.type == DieType.HUNTER || die.type == DieType.VAMPIRE)) damage *= Balance.PREDATOR
        val crit = bonus.critChance > 0f && rng.nextFloat() < bonus.critChance
        if (crit) damage *= bonus.critMult

        val targetPos = path.positionAt(target.distance)
        val dealt = linkedMapOf<Enemy, Float>()
        fun deal(e: Enemy, amount: Float) {
            dealt[e] = (dealt[e] ?: 0f) + e.takeDamage(amount, die, pierce)
        }
        val hits = mutableListOf(targetPos)

        if (die.type == DieType.QUAKE) {
            enemies.filter { it.alive }.forEach { e ->
                val shattered = Synergy.SHATTER in syn && e.slow > 0f
                deal(e, if (shattered) damage * 2 else damage)
                if (maxed) e.applySlow(Balance.QUAKE_SLOW, Balance.QUAKE_SLOW_TIME)
                if (e !== target) hits += path.positionAt(e.distance)
            }
            return ShotEvent(origin, hits, die.type, dealt, crit)
        }
        deal(target, damage)

        when (die.type) {
            DieType.FIRE -> {
                val radius = Balance.FIRE_SPLASH_RADIUS * (if (maxed) Balance.FIRE_MAX_RADIUS else 1f) * (if (Synergy.BLAZE in syn) Balance.BLAZE_RADIUS else 1f)
                val fraction = if (maxed) 1f else Balance.FIRE_SPLASH_FRACTION
                enemies
                    .filter { it !== target && it.alive && path.positionAt(it.distance).dst(targetPos) <= radius }
                    .forEach { deal(it, damage * fraction) }
            }
            DieType.ICE -> {
                target.applySlow(minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * die.pips))
                if (maxed) target.applyStun(Balance.ICE_FREEZE)
            }
            DieType.POISON -> target.addPoison(
                Balance.POISON_DPS * die.pips * power * (if (Synergy.PLAGUE in syn) Balance.PLAGUE_DPS else 1f),
                Balance.POISON_DURATION * (if (maxed) 2f else 1f),
            )
            DieType.ELECTRIC -> chain(target, damage, enemies, path, hits, if (maxed) Balance.CHAIN_JUMPS_MAX else Balance.CHAIN_JUMPS, Synergy.SHOCK in syn, ::deal)
            DieType.CURSE -> target.applyCurse(
                (Balance.CURSE_BASE + Balance.CURSE_PER_PIP * die.pips) * (if (maxed) Balance.CURSE_MAX_MULT else 1f),
                Balance.CURSE_DURATION * (if (maxed) 2f else 1f),
            )
            DieType.GRAVITY -> {
                val push = (Balance.GRAVITY_PUSH_BASE + Balance.GRAVITY_PUSH_PER_PIP * die.pips) *
                    (if (target.kind.isBoss) Balance.GRAVITY_BOSS_FACTOR else 1f)
                target.pushBack(push, unlimited = maxed && !target.kind.isBoss)
            }
            DieType.TIME -> {
                val seconds = (Balance.TIME_STUN_BASE + Balance.TIME_STUN_PER_PIP * die.pips) *
                    (if (target.kind.isBoss) Balance.TIME_BOSS_FACTOR else 1f)
                target.applyStun(seconds)
                if (maxed) {
                    enemies
                        .filter { it !== target && it.alive && path.positionAt(it.distance).dst(targetPos) <= Balance.TIME_AREA }
                        .forEach { it.applyStun(seconds * (if (it.kind.isBoss) Balance.TIME_BOSS_FACTOR else 1f)) }
                }
            }
            DieType.REAPER -> {
                var threshold = (Balance.REAPER_BASE + Balance.REAPER_PER_PIP * die.pips) * target.maxHp
                if (target.kind.isBoss) threshold *= if (maxed) Balance.REAPER_BOSS_FACTOR else 0f
                if (target.alive && target.hp < threshold) {
                    dealt[target] = (dealt[target] ?: 0f) + target.hp
                    target.hp = 0f
                    target.lastHit = die
                }
            }
            DieType.SNIPER -> if (maxed) {
                val second = enemies.filter { it.alive && it !== target }.maxByOrNull { it.hp }
                if (second != null) {
                    deal(second, damage)
                    hits += path.positionAt(second.distance)
                }
            }
            else -> Unit
        }
        return ShotEvent(origin, hits, die.type, dealt, crit)
    }

    private fun chain(first: Enemy, damage: Float, enemies: List<Enemy>, path: EnemyPath, hits: MutableList<Vec2>, jumps: Int, shock: Boolean, deal: (Enemy, Float) -> Unit) {
        val struck = mutableSetOf(first)
        var last = path.positionAt(first.distance)
        var jumpDamage = damage
        repeat(jumps) {
            val next = enemies
                .filter { it.alive && it !in struck }
                .map { it to path.positionAt(it.distance).dst(last) }
                .filter { it.second <= Balance.CHAIN_RADIUS }
                .minByOrNull { it.second }
                ?.first ?: return
            jumpDamage *= Balance.CHAIN_FALLOFF
            deal(next, jumpDamage)
            if (shock) next.applyStun(Balance.SHOCK_STUN)
            struck += next
            last = path.positionAt(next.distance)
            hits += last
        }
    }
}
