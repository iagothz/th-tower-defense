package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnemyPathTest {
    private val path = Layout.PATH

    @Test
    fun lengthIs1984() {
        assertEquals(1984f, path.length, 0.01f)
    }

    @Test
    fun positionsAlongPath() {
        assertEquals(Vec2(24f, 200f), path.positionAt(0f))
        assertEquals(Vec2(24f, 856f), path.positionAt(656f))
        assertEquals(Vec2(360f, 856f), path.positionAt(656f + 336f))
        assertEquals(Vec2(696f, 200f), path.positionAt(9999f))
        assertEquals(Vec2(24f, 200f), path.positionAt(-5f))
    }

    @Test
    fun cellAtRoundTrip() {
        Board().cells().forEach { assertEquals(it, Layout.cellAt(Layout.cellCenter(it))) }
    }

    @Test
    fun topRowIsHighest() {
        val top = Layout.cellCenter(Cell(0, 0))
        val bottom = Layout.cellCenter(Cell(2, 0))
        assertEquals(64f + 56f, top.x)
        assertEquals(464f + 2 * 120f + 56f, top.y)
        assertEquals(464f + 56f, bottom.y)
    }

    @Test
    fun cellAtOutsideOrGapIsNull() {
        assertNull(Layout.cellAt(Vec2(10f, 10f)))
        assertNull(Layout.cellAt(Vec2(64f + 112f + 4f, 500f)))
    }
}
