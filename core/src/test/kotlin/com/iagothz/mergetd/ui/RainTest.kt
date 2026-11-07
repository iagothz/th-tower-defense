package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertTrue

class RainTest {
    @Test
    fun fallingDiceStayInTheSideLanesClearOfTheTitleColumn() {
        for (i in 0..100) {
            val x = rainX(i / 100f)
            val left = x in RAIN_LANE_MIN..RAIN_LANE_MAX
            val right = x in 720f - RAIN_LANE_MAX..720f - RAIN_LANE_MIN
            assertTrue(left || right, "x=$x for r=${i / 100f}")
        }
    }

    @Test
    fun theRainUsesBothSides() {
        assertTrue(rainX(0.1f) < 360f)
        assertTrue(rainX(0.9f) > 360f)
    }
}
