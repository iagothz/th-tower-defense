package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Progression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudBlobTest {
    @Test
    fun encodeSkipsLocalKeysAndSortsTheRest() {
        val blob = CloudBlob.encode(mapOf("xp" to 140, "soundVolume" to 0.5f, "deck" to "fire,ice", "sync.version" to 3, "tutorialDone" to true))
        assertEquals("s:deck=fire,ice\nb:tutorialDone=true\ni:xp=140", blob)
    }

    @Test
    fun decodeRoundTripsEveryTypeAndEscapes() {
        val values = mapOf("xp" to 7, "history" to "a=b\nc\\d", "tutorialDone" to false, "ratio" to 0.25f)
        assertEquals(values, CloudBlob.decode(CloudBlob.encode(values)))
    }

    @Test
    fun decodeKeepsEverythingAfterTheFirstEquals() {
        assertEquals(mapOf("code" to "AB=CD=E"), CloudBlob.decode("s:code=AB=CD=E"))
    }

    @Test
    fun decodeAcceptsUnknownKeysButRejectsBadLines() {
        assertEquals(mapOf("future.thing" to 1), CloudBlob.decode("i:future.thing=1"))
        assertNull(CloudBlob.decode("xp=1"))
        assertNull(CloudBlob.decode("i:xp=abc"))
        assertNull(CloudBlob.decode("z:xp=1"))
    }

    @Test
    fun decodeDropsLocalKeys() {
        assertEquals(mapOf("xp" to 1), CloudBlob.decode("i:xp=1\nf:soundVolume=0.5\ni:sync.at=3"))
    }

    @Test
    fun hashIgnoresSyncLines() {
        assertEquals(CloudBlob.hash("i:xp=1"), CloudBlob.hash("i:xp=1\ni:sync.version=4\ni:sync.at=9"))
        assertNotEquals(CloudBlob.hash("i:xp=1"), CloudBlob.hash("i:xp=2"))
    }

    @Test
    fun versionAndSummaryReadTheBlob() {
        val blob = "i:sync.at=20000\ni:sync.version=5\ni:total.games=42\ni:xp=1310"
        assertEquals(5, CloudBlob.version(blob))
        assertNull(CloudBlob.version("i:xp=1"))
        assertEquals(CloudBlob.Summary(Progression.level(1310), 42, 20000), CloudBlob.summary(blob))
        assertEquals(CloudBlob.Summary(1, 0, null), CloudBlob.summary(""))
    }

    @Test
    fun saveDataExportsAndImportsWithoutTouchingLocalKeys() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        save.xp = 300
        save.soundVolume = 0.3f
        save.deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        val blob = save.export()
        assertFalse("soundVolume" in blob)
        val other = SaveData(SaveDataTest.memoryPrefs()).apply {
            soundVolume = 0.9f
            xp = 1
        }
        assertTrue(other.import(blob))
        assertEquals(300, other.xp)
        assertEquals(0.9f, other.soundVolume)
        assertEquals(save.deck, other.deck)
    }

    @Test
    fun importReplacesTheWholeSyncedSetAndRejectsBadBlobs() {
        val save = SaveData(SaveDataTest.memoryPrefs()).apply {
            xp = 50
            recordWave(GameMap.DEFAULT, 12)
        }
        assertTrue(save.import("i:xp=9"))
        assertEquals(9, save.xp)
        assertEquals(0, save.bestWave(GameMap.DEFAULT))
        assertFalse(save.import("garbage"))
        assertEquals(9, save.xp)
    }
}
