package com.iagothz.mergetd.logic

/**
 * One die attack. [to] lists every position hit in order (chains have several), for drawing;
 * [damage] is the direct damage each enemy took from this shot, splash and chain included.
 */
data class ShotEvent(
    val from: Vec2,
    val to: List<Vec2>,
    val type: DieType,
    val damage: Map<Enemy, Float> = emptyMap(),
)

object Combat {
    fun attackInterval(t: DieType, pips: Int): Float =
        Balance.stats(t).baseInterval / (1 + Balance.ATTACK_SPEED_PER_PIP * (pips - 1))

    fun hitDamage(t: DieType, pips: Int, powerLevel: Int, target: Enemy): Float {
        val base = Balance.stats(t).baseDamage * pips * Balance.powerMult(powerLevel)
        return if (t == DieType.IRON && target.kind.isBoss) base * Balance.IRON_BOSS_MULT else base
    }

    fun selectTarget(t: DieType, enemies: List<Enemy>): Enemy? {
        val alive = enemies.filter { it.alive }
        return if (t == DieType.SNIPER) alive.maxByOrNull { it.hp } else alive.maxByOrNull { it.distance }
    }

    /** Applies damage and the die's effect. Does not remove dead enemies or pay rewards. */
    fun attack(die: Die, origin: Vec2, powerLevel: Int, enemies: List<Enemy>, path: EnemyPath): ShotEvent? {
        val target = selectTarget(die.type, enemies) ?: return null
        val damage = hitDamage(die.type, die.pips, powerLevel, target)
        val targetPos = path.positionAt(target.distance)
        val dealt = linkedMapOf<Enemy, Float>()
        fun deal(e: Enemy, amount: Float) {
            e.hp -= amount
            dealt[e] = (dealt[e] ?: 0f) + amount
        }
        deal(target, damage)
        val hits = mutableListOf(targetPos)

        when (die.type) {
            DieType.FIRE -> enemies
                .filter { it !== target && it.alive && path.positionAt(it.distance).dst(targetPos) <= Balance.FIRE_SPLASH_RADIUS }
                .forEach { deal(it, damage * Balance.FIRE_SPLASH_FRACTION) }
            DieType.ICE -> target.applySlow(
                minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * die.pips),
            )
            DieType.POISON -> target.addPoison(Balance.POISON_DPS * die.pips * Balance.powerMult(powerLevel))
            DieType.ELECTRIC -> chain(target, damage, enemies, path, hits, ::deal)
            else -> Unit
        }
        return ShotEvent(origin, hits, die.type, dealt)
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
