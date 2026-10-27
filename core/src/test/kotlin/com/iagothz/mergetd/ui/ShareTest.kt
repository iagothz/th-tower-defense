package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareTest {
    @Test
    fun shareTextNamesWaveMapModeAndCode() {
        val t = Share.text(17, GameMap.DEFAULT, GameMode.RUSH, "0004-ABCD-EFGH-IJ")
        assertTrue("17" in t, t)
        assertTrue(GameMap.DEFAULT.title in t, t)
        assertTrue(GameMode.RUSH.title in t, t)
        assertTrue("0004-ABCD-EFGH-IJ" in t, t)
    }

    @Test
    fun noCodeNoCodeLine() {
        val t = Share.text(3, GameMap.DEFAULT, GameMode.NORMAL, null)
        assertFalse("digo" in t, t)
        assertTrue("3" in t, t)
    }
}
