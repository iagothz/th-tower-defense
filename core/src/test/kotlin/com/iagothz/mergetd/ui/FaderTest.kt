package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FaderTest {
    @Test
    fun fadesOutSignalsOnceThenFadesIn() {
        val f = Fader(0.2f)
        assertEquals(0f, f.alpha)
        assertTrue(f.start())
        assertFalse(f.update(0.1f))
        assertEquals(0.5f, f.alpha, 1e-4f)
        assertTrue(f.update(0.1f), "the switch moment")
        assertEquals(1f, f.alpha, 1e-4f)
        assertFalse(f.update(0.1f))
        assertEquals(0.5f, f.alpha, 1e-4f)
        assertFalse(f.update(0.2f))
        assertEquals(0f, f.alpha)
        assertFalse(f.fading)
    }

    @Test
    fun secondStartWhileFadingIsRefused() {
        val f = Fader(0.2f)
        assertTrue(f.start())
        assertFalse(f.start())
    }

    @Test
    fun idleUpdatesDoNothing() {
        val f = Fader(0.2f)
        assertFalse(f.update(1f))
        assertEquals(0f, f.alpha)
    }
}
