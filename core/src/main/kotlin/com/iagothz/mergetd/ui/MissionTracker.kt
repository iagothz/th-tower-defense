package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind
import com.iagothz.mergetd.logic.MissionProgress

/** The mission sentence in the current language: "Mate 40 inimigos com Fogo". */
fun Mission.text(): String {
    val what = when (kind) {
        MissionKind.KILLS_WITH -> Skin.fullName(type!!)
        MissionKind.REACH_WAVE -> S[map!!.title]
        MissionKind.PLAY_MODE -> S[mode!!.title]
        else -> null
    }
    return when {
        !template.contains("%d") -> S[template]
        what == null -> S.f(template, target)
        else -> S.f(template, target, what)
    }
}

/** What a finished match paid: XP, the level it left the account at, the chest of cards and any die the level-up opened. */
data class MatchReward(
    val xp: Int,
    val level: Int,
    val leveledUp: Boolean,
    val missions: List<Mission>,
    val cards: Map<DieType, Int> = emptyMap(),
    val newDice: List<DieType> = emptyList(),
)

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
