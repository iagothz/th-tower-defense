package com.iagothz.mergetd.logic

data class DieStats(val baseDamage: Float, val baseInterval: Float)

/** Every tunable number of the game lives here. */
object Balance {
    const val ROWS = 3
    const val COLS = 5
    const val MAX_PIPS = 7

    const val START_SP = 100
    const val START_LIVES = 3
    const val BUY_BASE = 10
    const val BUY_STEP = 10

    /** Cost to go from level i+1 to level i+2. */
    val POWER_COSTS = intArrayOf(100, 200, 400, 700)
    /** Damage multiplier for power levels 1..5. */
    val POWER_MULT = floatArrayOf(1.0f, 1.3f, 1.6f, 2.0f, 2.5f)

    const val STEP = 1f / 60f
    const val MAX_FRAME = 0.25f

    const val BASE_HP = 100f
    /** Path units per second at speedMult 1. */
    const val BASE_SPEED = 120f
    const val HP_GROWTH = 1.12f
    const val SPAWN_INTERVAL = 0.6f
    const val WAVE_DELAY = 3f
    const val MAX_WAVE_SIZE = 40
    const val BOSS_EVERY = 10

    const val SLOW_DURATION = 2f
    const val POISON_DURATION = 3f
    const val POISON_MAX_STACKS = 5
    const val BOSS_ABILITY_INTERVAL = 6f
    const val SILENCE_DURATION = 5f
    /** Fraction of max HP healed per second by the regenerating boss. */
    const val REGEN_PER_SEC = 0.02f

    const val ATTACK_SPEED_PER_PIP = 0.15f
    const val FIRE_SPLASH_RADIUS = 70f
    const val FIRE_SPLASH_FRACTION = 0.5f
    const val ICE_SLOW_BASE = 0.15f
    const val ICE_SLOW_PER_PIP = 0.05f
    const val ICE_SLOW_MAX = 0.5f
    /** Poison damage per second per pip, before the power multiplier. */
    const val POISON_DPS = 8f
    const val CHAIN_JUMPS = 2
    const val CHAIN_RADIUS = 120f
    const val CHAIN_FALLOFF = 0.7f
    const val IRON_BOSS_MULT = 2f

    const val MINE_INTERVAL = 5f
    const val MINE_SP_PER_PIP = 5
    const val GROWTH_INTERVAL = 20f

    fun powerMult(level: Int): Float = POWER_MULT[level - 1]

    fun stats(t: DieType): DieStats = when (t) {
        DieType.FIRE -> DieStats(20f, 1.0f)
        DieType.ICE -> DieStats(10f, 1.0f)
        DieType.POISON -> DieStats(5f, 1.0f)
        DieType.ELECTRIC -> DieStats(15f, 1.2f)
        DieType.WIND -> DieStats(6f, 0.35f)
        DieType.IRON -> DieStats(18f, 1.0f)
        DieType.MINE -> DieStats(0f, 0f)
        DieType.SNIPER -> DieStats(60f, 2.5f)
        DieType.MIRROR -> DieStats(0f, 0f)
        DieType.GROWTH -> DieStats(8f, 1.0f)
    }
}
