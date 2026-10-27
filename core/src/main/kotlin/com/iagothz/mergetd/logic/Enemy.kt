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
    /** Extra fraction of damage taken while cursed. */
    var curse = 0f
    var curseTime = 0f
    var stunTime = 0f
    var stunImmune = 0f
    /** Total distance Gravity has pushed this enemy back. */
    var pushed = 0f
    /** The die that last hit this enemy, credited with the kill. */
    var lastHit: Die? = null

    val alive: Boolean get() = hp > 0f

    fun speed(): Float = if (stunTime > 0f) 0f else Balance.BASE_SPEED * kind.speedMult * (1 - slow)

    /** Applies [amount] (raised by any curse) and credits [by]; returns the damage actually dealt. */
    fun takeDamage(amount: Float, by: Die?): Float {
        val dealt = amount * (1 + curse)
        hp -= dealt
        if (by != null) lastHit = by
        return dealt
    }

    /** Curses don't stack: the strongest one wins and the duration is refreshed. */
    fun applyCurse(amount: Float) {
        curse = maxOf(curse, amount)
        curseTime = Balance.CURSE_DURATION
    }

    /** A running stun isn't refreshed, and a fresh one is followed by [Balance.STUN_IMMUNITY]: no permanent lock. */
    fun applyStun(seconds: Float) {
        if (stunTime > 0f || stunImmune > 0f) return
        stunTime = seconds
    }

    /** Moves the enemy back along the path, within its lifetime push budget. */
    fun pushBack(amount: Float) {
        val budget = Balance.GRAVITY_MAX_PUSH * (if (kind.isBoss) Balance.GRAVITY_BOSS_FACTOR else 1f)
        val step = minOf(amount, budget - pushed).coerceAtLeast(0f)
        pushed += step
        distance = maxOf(0f, distance - step)
    }

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
            hp -= stack.dps * minOf(dt, stack.remaining) * (1 + curse)
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

        if (curseTime > 0f) {
            curseTime -= dt
            if (curseTime <= 0f) {
                curseTime = 0f
                curse = 0f
            }
        }
        if (stunTime > 0f) {
            stunTime -= dt
            if (stunTime <= 0f) {
                stunTime = 0f
                stunImmune = Balance.STUN_IMMUNITY
            }
        } else if (stunImmune > 0f) {
            stunImmune = maxOf(0f, stunImmune - dt)
        }

        if (kind == EnemyKind.BOSS_REGEN && alive) {
            hp = minOf(maxHp, hp + Balance.REGEN_PER_SEC * maxHp * dt)
        }
    }
}
