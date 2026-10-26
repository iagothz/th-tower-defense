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
