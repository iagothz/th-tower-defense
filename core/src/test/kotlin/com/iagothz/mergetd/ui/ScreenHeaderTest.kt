package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenHeaderTest {
    @Test
    fun backArrowHitsOnlyInsideItsSquare() {
        val h = ScreenHeader("x", back = true)
        assertTrue(h.backHit(Vec2(60f, 1222f)))
        assertTrue(h.backHit(Vec2(17f, 1179f)))
        assertTrue(h.backHit(Vec2(103f, 1265f)))
        assertFalse(h.backHit(Vec2(110f, 1222f)))
        assertFalse(h.backHit(Vec2(60f, 1170f)))
    }

    @Test
    fun noArrowNoHit() {
        assertFalse(ScreenHeader("x").backHit(Vec2(60f, 1222f)))
    }
}
