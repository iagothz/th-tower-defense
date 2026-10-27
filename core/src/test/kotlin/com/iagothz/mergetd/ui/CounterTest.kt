package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CounterTest {
    @Test
    fun startsAtItsInitialValue() {
        assertEquals(250, Counter(250).shown)
    }

    @Test
    fun rollsTowardTheTargetAndLands() {
        val c = Counter(0)
        c.update(0.05f, 100)
        assertTrue(c.shown in 1..99, "${c.shown}")
        c.update(1f, 100)
        assertEquals(100, c.shown)
    }

    @Test
    fun smallChangesSnapWithinAFrameOrTwo() {
        val c = Counter(10)
        c.update(0.2f, 11)
        assertEquals(11, c.shown)
    }
}
