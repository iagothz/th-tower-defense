package com.iagothz.mergetd.logic

enum class EnemyKind(val hpMult: Float, val speedMult: Float, val reward: Int, val lifeDamage: Int, val isBoss: Boolean = false) {
    NORMAL(1f, 1f, 10, 1),
    FAST(0.5f, 2f, 10, 1),
    TANK(3f, 0.6f, 20, 1),

    /** Heals the enemies around it (Balance.HEALER_*). */
    HEALER(1.2f, 0.9f, 15, 1),

    /** Immune to slows, stuns, pushes and traps. */
    GHOST(0.8f, 1.3f, 15, 1),

    /** A big soldier that heals the enemies around it; worth three soldiers. */
    ELITE(3f, 0.9f, 30, 1),

    /** Runs for the exit, costs no life if it makes it, pays a lot if it doesn't. */
    TREASURE(0.6f, 1.8f, 60, 0),
    BOSS_SILENCER(40f, 0.5f, 100, 2, isBoss = true),
    BOSS_SWAPPER(40f, 0.5f, 100, 2, isBoss = true),
    BOSS_REGEN(40f, 0.5f, 100, 2, isBoss = true),

    /** Leaves two tanks behind when it dies. */
    BOSS_SPLITTER(40f, 0.5f, 100, 2, isBoss = true),

    /** Shrugs off the first [Enemy.armor] of every hit. */
    BOSS_ARMORED(40f, 0.5f, 100, 2, isBoss = true),

    /** Steals SP every ability tick; killing it gives it all back. */
    BOSS_THIEF(40f, 0.5f, 100, 2, isBoss = true),

    /** Enemies near it move faster. */
    BOSS_COMMANDER(40f, 0.5f, 100, 2, isBoss = true),
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

    /** Flat damage ignored on every direct hit (Blindado); poison ticks bypass it. */
    var armor = 0f

    /** SP taken from the player so far (Ladrão), refunded on its death. */
    var stolen = 0

    /** Set by the match each step while a Comandante is close. */
    var hasted = false

    /** Mode speed factor (Rápido), set at spawn. */
    var speedScale = 1f

    /** The die that last hit this enemy, credited with the kill. */
    var lastHit: Die? = null

    val alive: Boolean get() = hp > 0f

    /** Ghosts can't be slowed, frozen or pushed. */
    private val immovable: Boolean get() = kind == EnemyKind.GHOST

    fun speed(): Float {
        if (stunTime > 0f) return 0f
        val haste = if (hasted) Balance.COMMANDER_HASTE else 1f
        return Balance.BASE_SPEED * kind.speedMult * (1 - slow) * haste * speedScale
    }

    /** Applies [amount] (raised by any curse, cut by [armor] unless [pierce]) and credits [by]; returns the damage dealt. */
    fun takeDamage(amount: Float, by: Die?, pierce: Boolean = false): Float {
        val dealt = maxOf(0f, amount - if (pierce) 0f else armor) * (1 + curse)
        hp -= dealt
        if (by != null) lastHit = by
        return dealt
    }

    /** Curses don't stack: the strongest one wins and the duration is refreshed. */
    fun applyCurse(amount: Float, duration: Float = Balance.CURSE_DURATION) {
        curse = maxOf(curse, amount)
        curseTime = maxOf(curseTime, duration)
    }

    /** A running stun isn't refreshed, and a fresh one is followed by [Balance.STUN_IMMUNITY]: no permanent lock. */
    fun applyStun(seconds: Float) {
        if (immovable || stunTime > 0f || stunImmune > 0f) return
        stunTime = seconds
    }

    /** Moves the enemy back along the path, within its lifetime push budget. */
    fun pushBack(amount: Float, unlimited: Boolean = false) {
        if (immovable) return
        val budget = Balance.GRAVITY_MAX_PUSH * (if (kind.isBoss) Balance.GRAVITY_BOSS_FACTOR else 1f)
        val step = if (unlimited) amount else minOf(amount, budget - pushed).coerceAtLeast(0f)
        pushed += step
        distance = maxOf(0f, distance - step)
    }

    /** Slows don't stack: the strongest one wins and the duration is refreshed. */
    fun applySlow(amount: Float, duration: Float = Balance.SLOW_DURATION) {
        if (immovable) return
        slow = maxOf(slow, amount)
        slowTime = maxOf(slowTime, duration)
    }

    /** At the stack cap, the stack closest to expiring is replaced. */
    fun addPoison(dps: Float, duration: Float = Balance.POISON_DURATION) {
        if (poison.size >= Balance.POISON_MAX_STACKS) {
            poison.remove(poison.minBy { it.remaining })
        }
        poison.add(PoisonStack(dps, duration))
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
