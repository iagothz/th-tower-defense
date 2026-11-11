package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameMapTest {
    /** Boss radius plus crown, for the screen edges and the HUD. */
    private val margin = 44f

    /** Boss radius plus outline, for the board, which enemies pass beside. */
    private val side = 32f

    @Test
    fun idsAreUniqueAndLookedUp() {
        assertEquals(GameMap.entries.size, GameMap.entries.map { it.id }.toSet().size)
        for (m in GameMap.entries) assertEquals(m, GameMap.byId(m.id))
        assertNull(GameMap.byId("moon"))
        assertEquals(GameMap.HORSESHOE, GameMap.DEFAULT)
        assertEquals(1984f, GameMap.DEFAULT.path.length, 0.01f)
    }

    /** Every path stays on screen, under the HUD, and clear of the board and the bottom buttons. */
    @Test
    fun pathsAvoidTheBoardTheHudAndTheEdges() {
        val boardLeft = Layout.GRID_X - side
        val boardRight = Layout.GRID_X + Balance.COLS * (Layout.CELL + Layout.GAP) - Layout.GAP + side
        val boardBottom = Layout.GRID_Y - side
        val boardTop = Layout.GRID_Y + Balance.ROWS * (Layout.CELL + Layout.GAP) - Layout.GAP + side
        for (m in GameMap.entries) {
            val pts = m.path.points
            assertTrue(pts.size >= 2, m.id)
            for (p in pts) {
                assertTrue(p.x - margin >= 0f && p.x + margin <= Layout.WORLD_W, "${m.id} $p off screen")
                assertTrue(p.y >= 180f && p.y + margin <= 1070f, "${m.id} $p hits the buttons or the HUD")
            }
            for ((a, b) in pts.zipWithNext()) {
                assertTrue(a.x == b.x || a.y == b.y, "${m.id}: segments are axis-aligned")
                val minX = minOf(a.x, b.x)
                val maxX = maxOf(a.x, b.x)
                val minY = minOf(a.y, b.y)
                val maxY = maxOf(a.y, b.y)
                val crossesBoard = maxX > boardLeft && minX < boardRight && maxY > boardBottom && minY < boardTop
                assertTrue(!crossesBoard, "${m.id}: segment $a-$b crosses the board")
            }
        }
    }

    @Test
    fun everyDifficultyIsRepresented() {
        assertEquals(Difficulty.entries.toSet(), GameMap.entries.map { it.difficulty }.toSet())
    }
}
