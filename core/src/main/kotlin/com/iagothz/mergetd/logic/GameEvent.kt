package com.iagothz.mergetd.logic

/** Something that happened in a [Match], for effects and sound. Drained with [Match.drainEvents]. */
sealed interface GameEvent {
    data class DieBought(val cell: Cell) : GameEvent

    /** [pips] of the die that came out of it (0 when unknown). */
    data class Merged(val cell: Cell, val pips: Int = 0) : GameEvent
    data class PoweredUp(val type: DieType) : GameEvent

    /** Direct damage from one shot (splash and chain included, poison ticks not). */
    data class Hit(val enemy: Enemy, val damage: Float, val crit: Boolean = false, val type: DieType? = null) : GameEvent

    /** An area explosion (trap or Supernova) went off at [at]. */
    data class Blast(val at: Vec2, val radius: Float, val type: DieType) : GameEvent

    /** [by] is the die that landed the last hit, when one did. */
    data class Killed(val kind: EnemyKind, val at: Vec2, val by: DieType? = null) : GameEvent
    data class Leaked(val kind: EnemyKind) : GameEvent

    /** The last enemy of [wave] died before the next one was due. */
    data class WaveCleared(val wave: Int) : GameEvent
    data class WaveStarted(val wave: Int, val boss: Boolean, val special: SpecialWave? = null, val bossKind: EnemyKind? = null) : GameEvent

    /** A Thief boss took [amount] SP. */
    data class Stolen(val amount: Int) : GameEvent
}
