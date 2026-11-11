package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.MatchSetup
import com.iagothz.mergetd.logic.WeeklyChallenge
import com.iagothz.mergetd.ui.SaveData

class MatchLauncher(private val save: SaveData) {
    fun newMatch(): MatchSession {
        save.clearMatch()
        val match = Match.seeded(save.playableDeck(), save.map, save.mode, save.mutators, save.dieLevels())
        return MatchSession(match, save.bestWave(save.map, save.mode), SessionKind.NEW)
    }

    fun daily(day: Int = DailyChallenge.today()): MatchSession {
        save.clearMatch()
        val c = DailyChallenge.of(day)
        return MatchSession(c.newMatch(save.dieLevels()), save.dailyBest(c.day), SessionKind.CHALLENGE)
    }

    fun weekly(week: Int = WeeklyChallenge.thisWeek()): MatchSession {
        save.clearMatch()
        val w = WeeklyChallenge.of(week)
        return MatchSession(w.newMatch(save.dieLevels()), save.weeklyBest(w.week), SessionKind.CHALLENGE)
    }

    fun fromCode(setup: MatchSetup): MatchSession {
        save.clearMatch()
        return MatchSession(setup.newMatch(save.dieLevels()), save.bestWave(setup.map, setup.mode), SessionKind.CHALLENGE)
    }

    fun resume(): MatchSession? {
        val (text, previousBest) = save.savedMatch ?: return null
        val match = Match.restore(text)
        if (match == null) save.clearMatch()
        return match?.let { MatchSession(it, previousBest, SessionKind.RESUMED) }
    }
}
