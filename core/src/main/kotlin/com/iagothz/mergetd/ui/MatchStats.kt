package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent

/** Tallies shown on the game-over screen, fed from the match's event stream. */
class MatchStats {
    var kills = 0
        private set
    var bosses = 0
        private set
    var merges = 0
        private set
    var bought = 0
        private set
    /** The kind of the last enemy that leaked: who ended the match. */
    var killedBy: EnemyKind? = null
        private set

    fun consume(events: List<GameEvent>) {
        for (e in events) {
            when (e) {
                is GameEvent.Killed -> {
                    kills++
                    if (e.kind.isBoss) bosses++
                }
                is GameEvent.Merged -> merges++
                is GameEvent.DieBought -> bought++
                is GameEvent.Leaked -> killedBy = e.kind
                else -> Unit
            }
        }
    }
}
