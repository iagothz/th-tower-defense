package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TapTest {
    @Test
    fun releaseOnPressedTargetFires() {
        val tap = Tap<String>()
        tap.press("fire")
        assertEquals("fire", tap.release("fire"))
    }

    @Test
    fun releaseOnOtherTargetIsIgnored() {
        val tap = Tap<String>()
        tap.press("fire")
        assertNull(tap.release("ice"))
    }

    @Test
    fun pressOnNothingThenReleaseOnTargetIsIgnored() {
        val tap = Tap<String>()
        tap.press(null)
        assertNull(tap.release("ice"))
    }

    @Test
    fun releaseWithoutPressIsIgnored() {
        val tap = Tap<String>()
        assertNull(tap.release("menu"))
    }

    @Test
    fun releaseConsumesThePress() {
        val tap = Tap<String>()
        tap.press("fire")
        tap.release("fire")
        assertNull(tap.release("fire"))
    }

    @Test
    fun cancelForgetsThePress() {
        val tap = Tap<String>()
        tap.press("fire")
        tap.cancel()
        assertNull(tap.release("fire"))
    }
}
