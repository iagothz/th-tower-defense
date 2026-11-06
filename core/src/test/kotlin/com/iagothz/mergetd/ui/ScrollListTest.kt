package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrollListTest {
    // Window from y 200 to 1000 (800 tall); rows 100 tall with 10 between: 25 rows take 2740.
    private fun list() = ScrollList(count = 25, top = 1000f, bottom = 200f, rowHeight = 100f, gap = 10f)

    @Test
    fun rowsStackDownFromTheTop() {
        val l = list()
        assertEquals(1000f, l.rowTop(0))
        assertEquals(890f, l.rowTop(1))
        assertTrue(l.isVisible(0))
        assertFalse(l.isVisible(10), "row 10 starts at -100, under the window")
        assertEquals(1940f, l.maxOffset)
    }

    @Test
    fun aTapWithoutMovingPicksTheRowUnderTheFinger() {
        val l = list()
        l.down(Vec2(300f, 850f))
        assertEquals(1, l.up(Vec2(302f, 848f)))
    }

    @Test
    fun aTapInTheGapOrOutsideTheWindowPicksNothing() {
        val l = list()
        l.down(Vec2(300f, 895f))
        assertNull(l.up(Vec2(300f, 895f)), "the 10-unit gap between rows 0 and 1")
        l.down(Vec2(300f, 1100f))
        assertNull(l.up(Vec2(300f, 1100f)), "above the window")
    }

    @Test
    fun draggingScrollsAndIsNotATap() {
        val l = list()
        l.down(Vec2(300f, 500f))
        l.move(Vec2(300f, 700f))
        assertEquals(200f, l.offset)
        assertNull(l.up(Vec2(300f, 700f)))
        assertEquals(1200f, l.rowTop(0))
    }

    @Test
    fun scrollingStopsAtBothEnds() {
        val l = list()
        l.down(Vec2(300f, 500f))
        l.move(Vec2(300f, 300f))
        assertEquals(0f, l.offset, "can't pull the first row below the top")
        l.move(Vec2(300f, 5000f))
        assertEquals(l.maxOffset, l.offset, "the last row stops at the bottom")
    }

    @Test
    fun aSmallWobbleStillCountsAsATap() {
        val l = list()
        l.down(Vec2(300f, 850f))
        l.move(Vec2(300f, 850f + ScrollList.SLOP - 1f))
        assertEquals(0f, l.offset)
        assertEquals(1, l.up(Vec2(300f, 850f + ScrollList.SLOP - 1f)))
    }

    @Test
    fun aShortListDoesNotScroll() {
        val l = ScrollList(count = 3, top = 1000f, bottom = 200f, rowHeight = 100f, gap = 10f)
        assertEquals(0f, l.maxOffset)
        l.down(Vec2(300f, 500f))
        l.move(Vec2(300f, 900f))
        assertEquals(0f, l.offset)
    }

    @Test
    fun aDragThatStartsOutsideTheWindowDoesNothing() {
        val l = list()
        l.down(Vec2(300f, 100f))
        l.move(Vec2(300f, 600f))
        assertEquals(0f, l.offset)
        assertNull(l.up(Vec2(300f, 600f)))
    }

    @Test
    fun cancelForgetsThePress() {
        val l = list()
        l.down(Vec2(300f, 850f))
        l.cancel()
        assertNull(l.up(Vec2(300f, 850f)))
    }
}
