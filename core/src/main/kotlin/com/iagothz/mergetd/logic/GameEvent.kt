package com.iagothz.mergetd.logic

/** Something that happened in a [Match], for effects and sound. Drained with [Match.drainEvents]. */
sealed interface GameEvent {
    data class DieBought(val cell: Cell) : GameEvent
    data class Merged(val cell: Cell) : GameEvent
    data class PoweredUp(val type: DieType) : GameEvent
    /** Direct damage from one shot (splash and chain included, poison ticks not). */
    data class Hit(val enemy: Enemy, val damage: Float, val crit: Boolean = false) : GameEvent
    /** An area explosion (trap or Supernova) went off at [at]. */
    data class Blast(val at: Vec2, val radius: Float, val type: DieType) : GameEvent
    data class Killed(val kind: EnemyKind, val at: Vec2) : GameEvent
    data class Leaked(val kind: EnemyKind) : GameEvent
    data class WaveStarted(val wave: Int, val boss: Boolean) : GameEvent
}
