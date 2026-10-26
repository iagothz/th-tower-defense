package com.iagothz.mergetd.logic

enum class EnemyKind(
    val hpMult: Float,
    val speedMult: Float,
    val reward: Int,
    val lifeDamage: Int,
    val isBoss: Boolean = false,
) {
    NORMAL(1f, 1f, 10, 1),
    FAST(0.5f, 2f, 10, 1),
    TANK(3f, 0.6f, 20, 1),
    BOSS_SILENCER(40f, 0.5f, 100, 2, isBoss = true),
    BOSS_SWAPPER(40f, 0.5f, 100, 2, isBoss = true),
    BOSS_REGEN(40f, 0.5f, 100, 2, isBoss = true),
}

data class PoisonStack(val dps: Float, var remaining: Float)

class Enemy(val kind: EnemyKind, val maxHp: Float) {
    var hp = maxHp
    /** Distance travelled along the path. */
    var distance = 0f
    var slow = 0f
    var slowTime = 0f
    val poison = mutableListOf<PoisonStack>()
    var abilityTimer = 0f

    val alive: Boolean get() = hp > 0f

    fun speed(): Float = Balance.BASE_SPEED * kind.speedMult * (1 - slow)

    /** Slows don't stack: the strongest one wins and the duration is refreshed. */
    fun applySlow(amount: Float) {
        slow = maxOf(slow, amount)
        slowTime = Balance.SLOW_DURATION
    }

    /** At the stack cap, the stack closest to expiring is replaced. */
    fun addPoison(dps: Float) {
        if (poison.size >= Balance.POISON_MAX_STACKS) {
            poison.remove(poison.minBy { it.remaining })
        }
        poison.add(PoisonStack(dps, Balance.POISON_DURATION))
    }

    fun tickEffects(dt: Float) {
        for (stack in poison) {
            hp -= stack.dps * minOf(dt, stack.remaining)
            stack.remaining -= dt
        }
        poison.removeAll { it.remaining <= 0f }

        if (slowTime > 0f) {
            slowTime -= dt
            if (slowTime <= 0f) {
                slowTime = 0f
                slow = 0f
            }
        }

        if (kind == EnemyKind.BOSS_REGEN && alive) {
            hp = minOf(maxHp, hp + Balance.REGEN_PER_SEC * maxHp * dt)
        }
    }
}
