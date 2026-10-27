package com.iagothz.mergetd.logic

data class DieStats(val baseDamage: Float, val baseInterval: Float)

/** Every tunable number of the game lives here. */
object Balance {
    const val ROWS = 3
    const val COLS = 5
    const val MAX_PIPS = 7

    const val START_SP = 150
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
    const val HP_GROWTH = 1.09f
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
    /** Waves 5, 15, 25... are special (never boss waves). */
    const val SPECIAL_EVERY = 5
    const val HORDE_SIZE = 40
    const val HORDE_HP = 0.5f
    const val HEALER_FROM_WAVE = 8
    const val GHOST_FROM_WAVE = 12
    /** HEALER: fraction of max HP restored per second to enemies within RADIUS path units. */
    const val HEALER_RADIUS = 90f
    const val HEALER_PER_SEC = 0.02f
    /** BOSS_ARMORED ignores this fraction of a plain soldier's HP (of its wave) on every hit. */
    const val ARMOR_FRACTION = 0.15f
    /** BOSS_THIEF takes this fraction of the player's SP every ability tick. */
    const val THIEF_FRACTION = 0.15f
    /** BOSS_SPLITTER leaves two tanks with this fraction of its max HP each. */
    const val SPLITTER_FRACTION = 0.25f
    /** BOSS_COMMANDER: enemies within RADIUS path units move HASTE times faster. */
    const val COMMANDER_RADIUS = 150f
    const val COMMANDER_HASTE = 1.4f

    const val ATTACK_SPEED_PER_PIP = 0.15f
    const val FIRE_SPLASH_RADIUS = 70f
    const val FIRE_SPLASH_FRACTION = 0.5f
    /** Ice slow = BASE + PER_PIP × pips: 20% at 1 pip, 50% at 7. */
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
    const val MINE_SP_PER_PIP = 10
    const val GROWTH_INTERVAL = 75f

    /** LIGHT: attack-speed bonus per pip for each orthogonal neighbour. */
    const val LIGHT_SPEED_PER_PIP = 0.15f
    /** CRIT: crit chance per pip, shared by every die on the board. */
    const val CRIT_CHANCE_PER_PIP = 0.03f
    const val CRIT_MAX = 0.6f
    const val CRIT_MULT = 3f
    /** COMBO: damage bonus for each other COMBO die on the board. */
    const val COMBO_BONUS = 0.3f
    /** CURSE: extra damage taken = BASE + PER_PIP × pips (18% at 1 pip, 36% at 7). */
    const val CURSE_BASE = 0.15f
    const val CURSE_PER_PIP = 0.03f
    const val CURSE_DURATION = 3f
    const val HUNTER_BONUS_PER_STACK = 0.05f
    /** Stack ceiling per pip; a 7-pip Hunter has none. */
    const val HUNTER_STACKS_PER_PIP = 10

    fun hunterCap(pips: Int): Int = if (pips >= MAX_PIPS) Int.MAX_VALUE else HUNTER_STACKS_PER_PIP * pips
    /** GRAVITY: path units pushed back = BASE + PER_PIP × pips, × BOSS_FACTOR for bosses. */
    const val GRAVITY_PUSH_BASE = 40f
    const val GRAVITY_PUSH_PER_PIP = 10f
    const val GRAVITY_BOSS_FACTOR = 0.25f
    /** Total push one enemy can take (× BOSS_FACTOR for bosses), so Gravity can't hold anyone forever. */
    const val GRAVITY_MAX_PUSH = 300f
    /** TIME: stun seconds = BASE + PER_PIP × pips, × BOSS_FACTOR for bosses. */
    const val TIME_STUN_BASE = 0.3f
    const val TIME_STUN_PER_PIP = 0.1f
    const val TIME_BOSS_FACTOR = 0.5f
    /** After a stun ends the enemy can't be stunned again for this long. */
    const val STUN_IMMUNITY = 1f
    /** TRAP: planted this far ahead of the leading enemy; goes off when an enemy is within TRIGGER of it. */
    const val TRAP_AHEAD = 150f
    const val TRAP_TRIGGER = 16f
    const val TRAP_RADIUS = 60f
    const val TRAP_MAX_PER_DIE = 3
    /** REAPER: kills a non-boss left under BASE + PER_PIP × pips of its max HP. */
    const val REAPER_BASE = 0.05f
    const val REAPER_PER_PIP = 0.02f
    const val NOVA_RADIUS = 90f
    const val LUCK_MAX_MULT = 4f
    const val VAMPIRE_SP_PER_PIP = 1

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
        DieType.MIRROR -> DieStats(16f, 1.0f)
        DieType.GROWTH -> DieStats(8f, 1.0f)
        DieType.LIGHT -> DieStats(8f, 1.0f)
        DieType.CRIT -> DieStats(10f, 1.0f)
        DieType.COMBO -> DieStats(12f, 1.0f)
        DieType.CURSE -> DieStats(6f, 1.0f)
        DieType.HUNTER -> DieStats(12f, 1.0f)
        DieType.GRAVITY -> DieStats(8f, 1.5f)
        DieType.TIME -> DieStats(8f, 1.2f)
        DieType.TRAP -> DieStats(30f, 2.5f)
        DieType.QUAKE -> DieStats(6f, 2.0f)
        DieType.REAPER -> DieStats(15f, 1.2f)
        DieType.NOVA -> DieStats(18f, 1.2f)
        DieType.LUCK -> DieStats(10f, 1.0f)
        DieType.CLONE -> DieStats(12f, 1.0f)
        DieType.BOOST -> DieStats(12f, 1.0f)
        DieType.VAMPIRE -> DieStats(14f, 1.0f)
    }
}
