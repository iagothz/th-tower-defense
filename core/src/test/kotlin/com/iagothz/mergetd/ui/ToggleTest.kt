package com.iagothz.mergetd.ui

import com.badlogic.gdx.math.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToggleTest {
    private fun t(on: Boolean = false, instant: Boolean = false) = Toggle(Rectangle(60f, 700f, 600f, 72f), "x", on) { instant }

    @Test
    fun toggleFlips() {
        val a = t()
        a.toggle()
        assertTrue(a.on)
        a.toggle()
        assertFalse(a.on)
    }

    @Test
    fun knobStartsWhereTheStateIs() {
        assertEquals(1f, t(on = true).knob)
        assertEquals(0f, t().knob)
    }

    @Test
    fun knobSlidesOverASlideAndStops() {
        val a = t()
        a.toggle()
        a.update(Toggle.SLIDE / 2)
        assertEquals(0.5f, a.knob, 0.01f)
        a.update(Toggle.SLIDE)
        assertEquals(1f, a.knob)
    }

    @Test
    fun reducedMotionJumps() {
        val a = t(instant = true)
        a.toggle()
        a.update(0.001f)
        assertEquals(1f, a.knob)
    }

    @Test
    fun wholeRowIsTheTarget() {
        assertTrue(t().contains(70f, 710f))
        assertTrue(t().contains(650f, 760f))
        assertFalse(t().contains(70f, 780f))
    }
}
