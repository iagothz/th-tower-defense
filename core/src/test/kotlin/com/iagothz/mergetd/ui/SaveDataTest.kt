package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.GameMap
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveDataTest {
    /** In-memory Preferences covering the calls SaveData makes. */
    private fun memoryPrefs(): Preferences {
        val values = mutableMapOf<String, Any?>()
        return Proxy.newProxyInstance(Preferences::class.java.classLoader, arrayOf(Preferences::class.java)) { proxy, method, args ->
            when (method.name) {
                "getInteger" -> values[args[0] as String] ?: args.getOrNull(1) ?: 0
                "getString" -> values[args[0] as String] ?: args.getOrNull(1)
                "getBoolean" -> values[args[0] as String] ?: args.getOrNull(1) ?: false
                "putInteger", "putString", "putBoolean" -> { values[args[0] as String] = args[1]; proxy }
                "remove" -> { values.remove(args[0] as String); null }
                "flush" -> null
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Preferences
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
}
