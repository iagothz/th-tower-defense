package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongPressTest {
    private val press = LongPress(holdSeconds = 0.45f, slop = 12f)

    @Test
    fun firesAfterTheHoldTimeWhenTheFingerStaysPut() {
        press.down(Vec2(100f, 100f), at = 10f)
        assertFalse(press.isHeld(at = 10.3f))
        press.move(Vec2(105f, 104f))
        assertTrue(press.isHeld(at = 10.5f))
    }

    @Test
    fun movingPastTheSlopTurnsItIntoADrag() {
        press.down(Vec2(100f, 100f), at = 0f)
        press.move(Vec2(130f, 100f))
        assertFalse(press.isHeld(at = 1f))
        press.move(Vec2(100f, 100f))
        assertFalse(press.isHeld(at = 2f), "a press that drifted away doesn't come back")
    }

    @Test
    fun upCancelsIt() {
        press.down(Vec2(100f, 100f), at = 0f)
        press.up()
        assertFalse(press.isHeld(at = 1f))
    }
}
