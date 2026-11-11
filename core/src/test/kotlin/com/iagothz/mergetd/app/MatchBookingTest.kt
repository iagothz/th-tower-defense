package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DailyMissions
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Mutator
import com.iagothz.mergetd.logic.Progression
import com.iagothz.mergetd.logic.WeeklyChallenge
import com.iagothz.mergetd.ui.SaveData
import com.iagothz.mergetd.ui.SaveDataTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchBookingTest {
    private val day = 20_400
    private val save = SaveData(SaveDataTest.memoryPrefs())

    private fun booking(dev: Boolean = false) = MatchBooking(save, dev, today = { day }, rng = Random(3))

    private fun ended(match: Match, wave: Int): Match = match.apply {
        debugStartWave(wave)
        while (!isGameOver) devLeak()
    }

    private fun missions() = MissionRun(day, DailyMissions.of(day), emptyList())

    @Test
    fun anOrdinaryMatchBooksTotalsRecordHistoryAndXp() {
        val match = ended(Match.seeded(DeckCodec.DEFAULT, GameMap.RING, save.mode, rng = Random(1)), 6)
        save.saveMatch(match.snapshot(), 0)
        val result = booking().book(match, MatchStats(), missions())
        assertNull(save.savedMatch)
        assertEquals(1, save.totals.games)
        assertEquals(6, save.bestWave(GameMap.RING, save.mode))
        val record = save.history().single()
        assertEquals(6, record.wave)
        assertTrue(record.code.isNotEmpty())
        val reward = assertNotNull(result.reward)
        assertTrue(reward.xp >= Progression.xpForMatch(6, 0))
        assertEquals(Progression.level(save.xp), reward.level)
    }

    @Test
    fun aDailyMatchBooksTheDayAndNotTheMap() {
        val c = DailyChallenge.of(day)
        val match = ended(c.newMatch(), 4)
        booking().book(match, MatchStats(), missions())
        assertEquals(4, save.dailyBest(c.day))
        assertEquals(0, save.bestWave(c.map))
        assertEquals("", save.history().single().code)
    }

    @Test
    fun aWeeklyMatchBooksItsScore() {
        val w = WeeklyChallenge.of(WeeklyChallenge.weekOf(day))
        val match = ended(w.newMatch(), 5)
        booking().book(match, MatchStats(), missions())
        assertEquals(match.score, save.weeklyBest(w.week))
        assertEquals(0, save.bestScore)
    }

    @Test
    fun mutatorsOutsideTheWeeklyKeepTheBestScore() {
        val match = ended(Match(DeckCodec.DEFAULT, Random(2), mutators = setOf(Mutator.SWIFT)), 5)
        booking().book(match, MatchStats(), missions())
        assertEquals(match.score, save.bestScore)
    }

    @Test
    fun devModeOnlyClearsTheSavedMatch() {
        val match = ended(Match(DeckCodec.DEFAULT, Random(2)), 7)
        save.saveMatch(match.snapshot(), 0)
        val result = booking(dev = true).book(match, MatchStats(), missions())
        assertNull(save.savedMatch)
        assertEquals(0, save.totals.games)
        assertNull(result.reward)
        assertTrue(result.unlocked.isEmpty())
    }

    @Test
    fun suspendKeepsAProvisionalRecordAndTheSnapshot() {
        val match = Match.seeded(DeckCodec.DEFAULT, GameMap.RING, save.mode, rng = Random(1)).apply { debugStartWave(3) }
        booking().suspend(match, previousBest = 2)
        assertEquals(match.snapshot() to 2, save.savedMatch)
        assertEquals(3, save.bestWave(GameMap.RING, save.mode))
    }

    @Test
    fun suspendSkipsDevModeAndFinishedMatches() {
        val running = Match(DeckCodec.DEFAULT, Random(2)).apply { debugStartWave(3) }
        booking(dev = true).suspend(running, 0)
        booking().suspend(ended(Match(DeckCodec.DEFAULT, Random(2)), 4), 0)
        assertNull(save.savedMatch)
        assertEquals(0, save.bestWave(GameMap.DEFAULT, save.mode))
    }

    @Test
    fun finishTellsARecordAndHidesTheCodeOfChallenges() {
        val match = ended(Match.seeded(DeckCodec.DEFAULT, GameMap.RING, save.mode, rng = Random(1)), 6)
        val outcome = booking().finish(match, MatchStats(), missions(), previousBest = 5)
        assertTrue(outcome.isRecord)
        assertNotNull(outcome.code)
        assertNull(outcome.score)

        val daily = booking().finish(ended(DailyChallenge.of(day).newMatch(), 2), MatchStats(), missions(), previousBest = 3)
        assertFalse(daily.isRecord)
        assertNull(daily.code)
        assertEquals(day, daily.challengeDay)
    }

    @Test
    fun finishComparesTheWeeklyByScoreAndNeverRecordsInDevMode() {
        val w = WeeklyChallenge.of(WeeklyChallenge.weekOf(day))
        val match = ended(w.newMatch(), 5)
        val outcome = booking().finish(match, MatchStats(), missions(), previousBest = match.score - 1)
        assertTrue(outcome.isRecord)
        assertEquals(match.score, outcome.score)
        assertEquals(w.week, outcome.week)
        assertFalse(booking(dev = true).finish(ended(Match(DeckCodec.DEFAULT, Random(4)), 9), MatchStats(), missions(), 0).isRecord)
    }
}
