package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameMode
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveDataTest {
    companion object {
    /** In-memory Preferences covering the calls SaveData makes. */
    fun memoryPrefs(): Preferences {
        val values = mutableMapOf<String, Any?>()
        return Proxy.newProxyInstance(Preferences::class.java.classLoader, arrayOf(Preferences::class.java)) { proxy, method, args ->
            when (method.name) {
                "getInteger" -> values[args[0] as String] ?: args.getOrNull(1) ?: 0
                "getString" -> values[args[0] as String] ?: args.getOrNull(1)
                "getBoolean" -> values[args[0] as String] ?: args.getOrNull(1) ?: false
                "putInteger", "putString", "putBoolean" -> { values[args[0] as String] = args[1]; proxy }
                "remove" -> { values.remove(args[0] as String); null }
                "get" -> values.toMap()
                "flush" -> null
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Preferences
    }
    }

    @Test
    fun soundIsOnByDefaultAndPersists() {
        val prefs = memoryPrefs()
        assertTrue(SaveData(prefs).soundOn)
        SaveData(prefs).soundOn = false
        assertFalse(SaveData(prefs).soundOn)
    }

    @Test
    fun recordWaveOnlyIncreasesAndIsPerMap() {
        val save = SaveData(memoryPrefs())
        assertTrue(save.recordWave(GameMap.RING, 5))
        assertEquals(5, save.bestWave(GameMap.RING))
        assertEquals(0, save.bestWave(GameMap.STRAIGHT))
        assertFalse(save.recordWave(GameMap.RING, 3))
        assertFalse(save.recordWave(GameMap.RING, 5))
        assertTrue(save.recordWave(GameMap.RING, 6))
        assertEquals(6, save.bestWave(GameMap.RING))
    }

    /** The record from before maps existed belongs to the Horseshoe, the only map there was. */
    @Test
    fun legacyRecordCountsForTheHorseshoe() {
        val prefs = memoryPrefs()
        prefs.putInteger("bestWave", 21)
        val save = SaveData(prefs)
        assertEquals(21, save.bestWave(GameMap.HORSESHOE))
        assertEquals(0, save.bestWave(GameMap.SERPENT))
        assertFalse(save.recordWave(GameMap.HORSESHOE, 20))
        assertTrue(save.recordWave(GameMap.HORSESHOE, 22))
        assertEquals(22, save.bestWave(GameMap.HORSESHOE))
    }

    @Test
    fun selectedMapDefaultsToTheHorseshoeAndPersists() {
        val prefs = memoryPrefs()
        assertEquals(GameMap.HORSESHOE, SaveData(prefs).map)
        SaveData(prefs).map = GameMap.SERPENT
        assertEquals(GameMap.SERPENT, SaveData(prefs).map)
    }

    @Test
    fun matchSnapshotIsStoredWithItsBestAndCleared() {
        val prefs = memoryPrefs()
        val save = SaveData(prefs)
        assertNull(save.savedMatch)
        save.saveMatch("v=1", previousBest = 7)
        assertEquals("v=1" to 7, SaveData(prefs).savedMatch)
        save.clearMatch()
        assertNull(SaveData(prefs).savedMatch)
    }

    @Test
    fun vibrationIsOnByDefaultAndPersists() {
        val prefs = memoryPrefs()
        assertTrue(SaveData(prefs).vibrationOn)
        SaveData(prefs).vibrationOn = false
        assertFalse(SaveData(prefs).vibrationOn)
    }

    @Test
    fun tutorialIsPendingUntilMarkedDone() {
        val prefs = memoryPrefs()
        assertFalse(SaveData(prefs).tutorialDone)
        SaveData(prefs).tutorialDone = true
        assertTrue(SaveData(prefs).tutorialDone)
    }

    @Test
    fun lifetimeTotalsAccumulateAcrossMatches() {
        val save = SaveData(memoryPrefs())
        val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        save.recordMatch(GameMap.RING, deck, wave = 12, kills = 100, bosses = 1, merges = 20)
        save.recordMatch(GameMap.RING, deck, wave = 8, kills = 50, bosses = 0, merges = 9)
        val t = save.totals
        assertEquals(2, t.games)
        assertEquals(150, t.kills)
        assertEquals(1, t.bosses)
        assertEquals(29, t.merges)
        assertEquals(20, t.waves)
        assertEquals(12, save.bestWave(GameMap.RING))
    }

    @Test
    fun bestWavePerDeckIsKeptAndRanked() {
        val save = SaveData(memoryPrefs())
        val a = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        val b = listOf(DieType.IRON, DieType.SNIPER, DieType.CURSE, DieType.HUNTER, DieType.MINE)
        save.recordMatch(GameMap.HORSESHOE, a, 10, 0, 0, 0)
        save.recordMatch(GameMap.HORSESHOE, b, 25, 0, 0, 0)
        save.recordMatch(GameMap.HORSESHOE, a, 7, 0, 0, 0)
        assertEquals(listOf(b to 25, a to 10), save.deckRecords())
    }

    @Test
    fun dailyRecordsArePerDayAndListedNewestFirst() {
        val save = SaveData(memoryPrefs())
        assertEquals(0, save.dailyBest(20_750))
        assertTrue(save.recordDaily(20_750, 9))
        assertFalse(save.recordDaily(20_750, 5))
        assertTrue(save.recordDaily(20_751, 14))
        assertEquals(listOf(20_751 to 14, 20_750 to 9), save.dailyHistory())
    }

    @Test
    fun musicIsOnByDefaultAndPersists() {
        val prefs = memoryPrefs()
        assertTrue(SaveData(prefs).musicOn)
        SaveData(prefs).musicOn = false
        assertFalse(SaveData(prefs).musicOn)
    }

    @Test
    fun recordsAreKeptPerMapAndMode() {
        val save = SaveData(memoryPrefs())
        assertTrue(save.recordWave(GameMap.RING, 7, GameMode.RUSH))
        assertEquals(7, save.bestWave(GameMap.RING, GameMode.RUSH))
        assertEquals(0, save.bestWave(GameMap.RING))
        assertTrue(save.recordWave(GameMap.RING, 5))
        assertEquals(5, save.bestWave(GameMap.RING, GameMode.NORMAL))
        assertEquals(GameMode.NORMAL, save.mode)
        save.mode = GameMode.SURVIVAL
        assertEquals(GameMode.SURVIVAL, save.mode)
    }

    @Test
    fun historyKeepsTheLastTwentyNewestFirst() {
        val save = SaveData(memoryPrefs())
        val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        for (i in 1..25) {
            save.addHistory(MatchRecord(day = 20_700 + i, map = GameMap.RING, mode = GameMode.RUSH, deck = deck, wave = i, killedBy = if (i % 2 == 0) EnemyKind.TANK else null, code = "C$i"))
        }
        val h = save.history()
        assertEquals(20, h.size)
        assertEquals(25, h.first().wave)
        assertEquals(6, h.last().wave)
        assertEquals(EnemyKind.TANK, h.last().killedBy)
        assertEquals(null, h.first().killedBy)
        assertEquals("C25", h.first().code)
        assertEquals(deck, h.first().deck)
    }
}
