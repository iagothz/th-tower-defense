package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.CardDrop
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.MatchCode
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionProgress
import com.iagothz.mergetd.logic.Progression
import com.iagothz.mergetd.ui.Achievement
import com.iagothz.mergetd.ui.AchievementProgress
import com.iagothz.mergetd.ui.Achievements
import com.iagothz.mergetd.ui.MatchRecord
import com.iagothz.mergetd.ui.SaveData
import kotlin.random.Random

data class MissionRun(val day: Int, val missions: List<Mission>, val progress: List<MissionProgress>)

data class Booking(val unlocked: Set<Achievement>, val reward: MatchReward?)

data class MatchOutcome(
    val map: GameMap,
    val wave: Int,
    val isRecord: Boolean,
    val stats: MatchStats,
    val challengeDay: Int?,
    val mode: GameMode,
    val unlocked: Set<Achievement>,
    val code: String?,
    val reward: MatchReward?,
    val score: Int?,
    val week: Int?,
)

class MatchBooking(private val save: SaveData, private val dev: Boolean, private val today: () -> Int = DailyChallenge::today, private val rng: Random = Random(System.nanoTime())) {
    fun suspend(match: Match, previousBest: Int) {
        if (dev || match.isGameOver) return
        val day = match.challengeDay
        val week = match.week
        when {
            day != null -> save.recordDaily(day, match.wave)
            week != null -> save.recordWeekly(week, match.score)
            else -> save.recordWave(match.map, match.wave, match.mode)
        }
        save.saveMatch(match.snapshot(), previousBest)
    }

    fun book(match: Match, stats: MatchStats, missions: MissionRun): Booking {
        save.clearMatch()
        if (dev) return Booking(emptySet(), null)
        recordResult(match, stats)
        val unlocked = save.unlock(Achievements.earned(achievementProgress(match, stats)))
        return Booking(unlocked, reward(match, stats, missions))
    }

    fun finish(match: Match, stats: MatchStats, missions: MissionRun, previousBest: Int): MatchOutcome {
        val booking = book(match, stats, missions)
        val isRecord = !dev && (if (match.week != null) match.score > previousBest else match.wave > previousBest)
        return MatchOutcome(
            map = match.map,
            wave = match.wave,
            isRecord = isRecord,
            stats = stats,
            challengeDay = match.challengeDay,
            mode = match.mode,
            unlocked = booking.unlocked,
            code = match.setup?.takeIf { match.challengeDay == null && match.week == null }?.let { MatchCode.encode(it) },
            reward = booking.reward,
            score = match.score.takeIf { match.mutators.isNotEmpty() },
            week = match.week,
        )
    }

    private fun recordResult(match: Match, stats: MatchStats) {
        save.recordMatch(match.map, match.deck, match.wave, stats.kills, stats.bosses, stats.merges, match.challengeDay, match.mode, match.locked)
        match.week?.let { save.recordWeekly(it, match.score) }
        if (match.week == null && match.mutators.isNotEmpty() && match.score > save.bestScore) save.bestScore = match.score
        val code = match.setup?.takeIf { match.challengeDay == null }?.let { MatchCode.encode(it) } ?: ""
        save.addHistory(MatchRecord(today(), match.map, match.mode, match.deck, match.wave, stats.killedBy, code))
    }

    private fun achievementProgress(match: Match, stats: MatchStats): AchievementProgress {
        val t = save.totals
        return AchievementProgress(
            wave = match.wave, mode = match.mode, kills = t.kills, bosses = t.bosses, merges = t.merges, games = t.games,
            maxPips = stats.maxPips, synergy = stats.synergyUsed, hunterStacks = stats.hunterStacks,
            daily = match.challengeDay != null, rich = stats.rich, mapsAtTen = save.mapsAt(MAPS_AT_WAVE),
        )
    }

    private fun reward(match: Match, stats: MatchStats, missions: MissionRun): MatchReward {
        val levelBefore = Progression.level(save.xp)
        val matchXp = Progression.xpForMatch(match.wave, stats.bosses)
        save.xp += matchXp
        val done = save.applyMissions(missions.day, missions.missions, missions.progress)
        val level = Progression.level(save.xp)
        val newDice = (levelBefore + 1..level).mapNotNull { DiceUnlocks.openedAt(it) }
        val chest = CardDrop.draw(save.cardCandidates(), CardDrop.count(match.wave, stats.bosses), rng)
        save.addCards(chest)
        return MatchReward(matchXp + done.sumOf { it.xp }, level, level > levelBefore, done, chest, newDice)
    }

    private companion object {
        const val MAPS_AT_WAVE = 10
    }
}
