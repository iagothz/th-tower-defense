package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.MatchSetup
import com.iagothz.mergetd.logic.WeeklyChallenge
import com.iagothz.mergetd.ui.SaveData
import com.iagothz.mergetd.ui.SaveDataTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchLauncherTest {
    private val save = SaveData(SaveDataTest.memoryPrefs())
    private val launcher = MatchLauncher(save)

    @Test
    fun aNewMatchUsesTheChosenMapModeAndDeckAgainstTheirRecord() {
        save.map = GameMap.RING
        save.mode = GameMode.RUSH
        save.recordWave(GameMap.RING, 8, GameMode.RUSH)
        save.saveMatch("old", 1)
        val s = launcher.newMatch()
        assertNull(save.savedMatch)
        assertEquals(SessionKind.NEW, s.kind)
        assertEquals(GameMap.RING, s.match.map)
        assertEquals(GameMode.RUSH, s.match.mode)
        assertEquals(save.playableDeck(), s.match.deck)
        assertEquals(8, s.previousBest)
        assertNotNull(s.match.seed)
    }

    @Test
    fun theDailyIsBookedAgainstItsDay() {
        val day = 20_400
        save.recordDaily(day, 6)
        val s = launcher.daily(day)
        assertEquals(SessionKind.CHALLENGE, s.kind)
        assertEquals(day, s.match.challengeDay)
        assertEquals(DailyChallenge.of(day).deck, s.match.deck)
        assertEquals(6, s.previousBest)
    }

    @Test
    fun theWeeklyIsBookedAgainstItsScore() {
        val week = WeeklyChallenge.weekOf(20_400)
        save.recordWeekly(week, 50)
        val s = launcher.weekly(week)
        assertEquals(SessionKind.CHALLENGE, s.kind)
        assertEquals(week, s.match.week)
        assertEquals(50, s.previousBest)
    }

    @Test
    fun aCodeReplaysItsSetupAgainstThatMapRecord() {
        val setup = MatchSetup(GameMap.STRAIGHT, GameMode.NORMAL, 42, DeckCodec.DEFAULT)
        save.recordWave(GameMap.STRAIGHT, 11, GameMode.NORMAL)
        val s = launcher.fromCode(setup)
        assertEquals(SessionKind.CHALLENGE, s.kind)
        assertEquals(setup, s.match.setup)
        assertTrue(s.match.locked)
        assertEquals(11, s.previousBest)
    }

    @Test
    fun resumeRestoresTheSavedMatchWithItsRecord() {
        val match = Match(DeckCodec.DEFAULT, Random(5)).apply { debugStartWave(4) }
        save.saveMatch(match.snapshot(), 9)
        val s = assertNotNull(launcher.resume())
        assertEquals(SessionKind.RESUMED, s.kind)
        assertEquals(4, s.match.wave)
        assertEquals(9, s.previousBest)
    }

    @Test
    fun resumeDropsADamagedSaveAndReturnsNothingWithoutOne() {
        assertNull(launcher.resume())
        save.saveMatch(listOf("v=4", "wave=oops").joinToString(System.lineSeparator()), 3)
        assertNull(launcher.resume())
        assertNull(save.savedMatch)
    }
}
