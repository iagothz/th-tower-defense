package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Layout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BottomNavTest {
    @Test
    fun fiveTabsShareTheWidthWithoutGapsOrOverlaps() {
        val nav = BottomNav(Tab.HOME)
        assertEquals(Tab.entries.size, nav.rects.size)
        assertEquals(5, Tab.entries.size)
        var x = 0f
        for ((tab, r) in nav.rects) {
            assertEquals(x, r.x, 1e-3f, "$tab starts where the previous one ends")
            assertEquals(0f, r.y, 1e-3f)
            assertEquals(BottomNav.HEIGHT, r.height, 1e-3f)
            x += r.width
        }
        assertEquals(Layout.WORLD_W, x, 1e-3f, "the tabs fill the width")
    }

    @Test
    fun aTouchLandsOnTheTabUnderIt() {
        val nav = BottomNav(Tab.DECK)
        assertEquals(Tab.HOME, nav.tabAt(10f, 20f))
        assertEquals(Tab.DECK, nav.tabAt(200f, BottomNav.HEIGHT - 1f))
        assertEquals(Tab.MAPS, nav.tabAt(Layout.WORLD_W / 2, 50f))
        assertEquals(Tab.SETTINGS, nav.tabAt(Layout.WORLD_W - 1f, 50f))
        assertNull(nav.tabAt(200f, BottomNav.HEIGHT + 1f), "above the bar is the screen's")
        assertEquals(Tab.CHALLENGES, nav.tabAt(500f, -40f), "the bar reaches down to the bottom of taller phones")
    }

    @Test
    fun everyTabHasATranslatedTitle() {
        for (t in Tab.entries) assertTrue(S.has(Language.EN, t.title), t.title)
    }
}
