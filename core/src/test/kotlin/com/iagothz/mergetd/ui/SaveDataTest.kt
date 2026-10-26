package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun recordWaveOnlyIncreases() {
        val save = SaveData(memoryPrefs())
        assertTrue(save.recordWave(5))
        assertEquals(5, save.bestWave)
        assertFalse(save.recordWave(3))
        assertEquals(5, save.bestWave)
        assertFalse(save.recordWave(5))
        assertTrue(save.recordWave(6))
        assertEquals(6, save.bestWave)
    }
}
