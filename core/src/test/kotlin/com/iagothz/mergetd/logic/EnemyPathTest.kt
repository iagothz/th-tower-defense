package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnemyPathTest {
    private val path = Layout.PATH

    @Test
    fun lengthIs1984() {
        assertEquals(1984f, path.length, 0.01f)
    }

    @Test
    fun positionsAlongPath() {
        assertEquals(Vec2(44f, 180f), path.positionAt(0f))
        assertEquals(Vec2(44f, 856f), path.positionAt(676f))
        assertEquals(Vec2(360f, 856f), path.positionAt(676f + 316f))
        assertEquals(Vec2(676f, 180f), path.positionAt(9999f))
        assertEquals(Vec2(44f, 180f), path.positionAt(-5f))
    }

    /** The path, enemies on it (boss radius 32) and the board must all fit on screen without touching. */
    @Test
    fun pathAndEnemiesStayOnScreenAndOffTheBoard() {
        val margin = 32f
        for (p in path.points) {
            assertTrue(p.x - margin >= 0f && p.x + margin <= Layout.WORLD_W, "$p")
            assertTrue(p.y - margin >= 0f && p.y + margin <= Layout.WORLD_H, "$p")
        }
        val left = path.points.minOf { it.x }
        val right = path.points.maxOf { it.x }
        assertTrue(left + margin <= Layout.GRID_X)
        assertTrue(right - margin >= Layout.GRID_X + Balance.COLS * (Layout.CELL + Layout.GAP) - Layout.GAP)
    }

    @Test
    fun cellAtRoundTrip() {
        Board().cells().forEach { assertEquals(it, Layout.cellAt(Layout.cellCenter(it))) }
    }

    @Test
    fun topRowIsHighest() {
        val top = Layout.cellCenter(Cell(0, 0))
        val bottom = Layout.cellCenter(Cell(2, 0))
        assertEquals(84f + 52f, top.x)
        assertEquals(464f + 2 * 112f + 52f, top.y)
        assertEquals(464f + 52f, bottom.y)
    }

    @Test
    fun cellAtOutsideOrGapIsNull() {
        assertNull(Layout.cellAt(Vec2(10f, 10f)))
        assertNull(Layout.cellAt(Vec2(84f + 104f + 4f, 500f)))
    }
}
