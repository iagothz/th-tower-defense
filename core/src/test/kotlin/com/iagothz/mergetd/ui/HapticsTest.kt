package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HapticsTest {
    @Test
    fun quietFramesDoNotBuzz() {
        assertNull(Haptics.pulse(emptySet()))
        assertNull(Haptics.pulse(setOf("hit_fire", "kill", "buy")))
    }

    @Test
    fun mergesAreLightAndLeaksAreHeavy() {
        assertEquals(Haptics.MERGE, Haptics.pulse(setOf("merge_3")))
        assertEquals(Haptics.Pulse(80, 255), Haptics.pulse(setOf("leak")))
        assertEquals(Haptics.Pulse(40, 180), Haptics.pulse(setOf("max")))
    }

    @Test
    fun theStrongestPulseOfTheFrameWins() {
        assertEquals(Haptics.Pulse(80, 255), Haptics.pulse(setOf("merge_2", "leak", "boss")))
        assertEquals(Haptics.Pulse(50, 130), Haptics.pulse(setOf("merge_2", "boss")))
    }
}
