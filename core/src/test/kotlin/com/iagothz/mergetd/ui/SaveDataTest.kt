package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Mutator
import com.iagothz.mergetd.logic.MissionKind
import com.iagothz.mergetd.logic.MissionProgress
import com.iagothz.mergetd.logic.Mission
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
                "getFloat" -> values[args[0] as String] ?: args.getOrNull(1) ?: 0f
                "putInteger", "putString", "putBoolean", "putFloat" -> { values[args[0] as String] = args[1]; proxy }
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

    @Test
    fun volumesDefaultToFullAndAreClamped() {
        val prefs = memoryPrefs()
        assertEquals(1f, SaveData(prefs).soundVolume)
        assertEquals(1f, SaveData(prefs).musicVolume)
        SaveData(prefs).soundVolume = 0.4f
        SaveData(prefs).musicVolume = 1.7f
        assertEquals(0.4f, SaveData(prefs).soundVolume, 1e-4f)
        assertEquals(1f, SaveData(prefs).musicVolume, 1e-4f)
        SaveData(prefs).musicVolume = -1f
        assertEquals(0f, SaveData(prefs).musicVolume, 1e-4f)
    }

    @Test
    fun languageDefaultsToAutoAndPersists() {
        val prefs = memoryPrefs()
        assertEquals(Language.AUTO, SaveData(prefs).language)
        SaveData(prefs).language = Language.EN
        assertEquals(Language.EN, SaveData(prefs).language)
    }

    @Test
    fun xpPersists() {
        val prefs = memoryPrefs()
        assertEquals(0, SaveData(prefs).xp)
        SaveData(prefs).xp = 320
        assertEquals(320, SaveData(prefs).xp)
    }

    @Test
    fun missionsCompleteOnceAndPayTheirXp() {
        val prefs = memoryPrefs()
        val save = SaveData(prefs)
        val missions = listOf(
            Mission(MissionKind.MERGES, 10, 50),
            Mission(MissionKind.REACH_WAVE, 8, 80, map = GameMap.DEFAULT),
            Mission(MissionKind.BOSSES, 2, 70),
        )
        val day = 20_000
        assertEquals(listOf(0, 0, 0), save.missionProgress(day, missions))
        var done = save.applyMissions(day, missions, listOf(MissionProgress(delta = 6), MissionProgress(absolute = 5), MissionProgress()))
        assertTrue(done.isEmpty())
        assertEquals(listOf(6, 5, 0), save.missionProgress(day, missions))
        assertEquals(0, save.xp)

        done = save.applyMissions(day, missions, listOf(MissionProgress(delta = 6), MissionProgress(absolute = 3), MissionProgress(delta = 2)))
        assertEquals(listOf(missions[0], missions[2]), done)
        assertEquals(listOf(10, 5, 2), save.missionProgress(day, missions), "counters cap at the target, wave goals keep their best")
        assertEquals(120, save.xp)

        done = save.applyMissions(day, missions, listOf(MissionProgress(delta = 6), MissionProgress(absolute = 9), MissionProgress(delta = 2)))
        assertEquals(listOf(missions[1]), done, "already done ones don't pay twice")
        assertEquals(200, save.xp)
        assertEquals(listOf(10, 8, 2), SaveData(prefs).missionProgress(day, missions))
    }

    @Test
    fun hintsSeenPersistAsASet() {
        val prefs = memoryPrefs()
        assertTrue(SaveData(prefs).hints.isEmpty())
        SaveData(prefs).hints = setOf("undo", "hold")
        assertEquals(setOf("undo", "hold"), SaveData(prefs).hints)
    }

    @Test
    fun accessibilityTogglesPersist() {
        val prefs = memoryPrefs()
        assertFalse(SaveData(prefs).reduceMotion)
        assertFalse(SaveData(prefs).largeText)
        SaveData(prefs).reduceMotion = true
        SaveData(prefs).largeText = true
        assertTrue(SaveData(prefs).reduceMotion)
        assertTrue(SaveData(prefs).largeText)
    }

    @Test
    fun deckSlotsPersist() {
        val prefs = memoryPrefs()
        assertNull(SaveData(prefs).deckSlot(0))
        val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        SaveData(prefs).saveDeckSlot(1, deck)
        assertEquals(deck, SaveData(prefs).deckSlot(1))
        assertNull(SaveData(prefs).deckSlot(0))
    }

    @Test
    fun mutatorsAndWeeklyRecordsPersist() {
        val prefs = memoryPrefs()
        assertTrue(SaveData(prefs).mutators.isEmpty())
        SaveData(prefs).mutators = setOf(Mutator.SWIFT, Mutator.POOR)
        assertEquals(setOf(Mutator.SWIFT, Mutator.POOR), SaveData(prefs).mutators)

        assertEquals(0, SaveData(prefs).weeklyBest(2_900))
        assertTrue(SaveData(prefs).recordWeekly(2_900, 21))
        assertFalse(SaveData(prefs).recordWeekly(2_900, 15))
        assertEquals(21, SaveData(prefs).weeklyBest(2_900))
        assertEquals(0, SaveData(prefs).bestScore)
        SaveData(prefs).bestScore = 30
        assertEquals(30, SaveData(prefs).bestScore)
    }
}
