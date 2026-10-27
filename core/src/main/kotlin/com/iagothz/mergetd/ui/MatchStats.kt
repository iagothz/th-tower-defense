package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.Match
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
    var maxPips = 0
        private set
    var hunterStacks = 0
        private set
    var synergyUsed = false
        private set
    var undoUsed = false
        private set
    /** Held 1000 SP at some point. */
    var rich = false
        private set

    /** Looks at the board; cheap enough for every frame. */
    fun observe(m: Match) {
        for ((_, d) in m.board.occupied()) {
            if (d.pips > maxPips) maxPips = d.pips
            if (d.type == DieType.HUNTER && d.stacks > hunterStacks) hunterStacks = d.stacks
        }
        if (!synergyUsed && m.synergies().isNotEmpty()) synergyUsed = true
        if (m.sp >= RICH_SP) rich = true
    }

    companion object {
        const val RICH_SP = 1000
    }

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
                is GameEvent.Unmerged -> {
                    undoUsed = true
                    merges--
                }
                else -> Unit
            }
        }
    }
}
