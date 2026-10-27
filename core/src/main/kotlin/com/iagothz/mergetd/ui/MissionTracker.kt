package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind
import com.iagothz.mergetd.logic.MissionProgress

/** Watches one match for the day's missions; [progress] is what the match contributed, applied to the save at the end. */
class MissionTracker(private val missions: List<Mission>, private val match: Match) {
    private val counts = IntArray(missions.size)

    fun consume(events: List<GameEvent>) {
        for (e in events) {
            for ((i, m) in missions.withIndex()) {
                val hit = when (m.kind) {
                    MissionKind.KILLS_WITH -> e is GameEvent.Killed && e.by == m.type
                    MissionKind.BOSSES -> e is GameEvent.Killed && e.kind.isBoss
                    MissionKind.MERGES -> e is GameEvent.Merged
                    MissionKind.MAX_PIPS -> e is GameEvent.Merged && e.pips >= Balance.MAX_PIPS
                    MissionKind.REACH_WAVE, MissionKind.PLAY_MODE -> false
                }
                if (hit) counts[i]++
            }
        }
    }

    fun progress(): List<MissionProgress> = missions.mapIndexed { i, m ->
        when (m.kind) {
            MissionKind.REACH_WAVE -> MissionProgress(absolute = if (match.map == m.map) match.wave else 0)
            MissionKind.PLAY_MODE -> MissionProgress(absolute = if (match.mode == m.mode) match.wave else 0)
            else -> MissionProgress(delta = counts[i])
        }
    }
}
