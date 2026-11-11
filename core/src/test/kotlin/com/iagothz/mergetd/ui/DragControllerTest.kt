package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DragControllerTest {
    private val a = Cell(0, 0)
    private val b = Cell(0, 1)
    private val c = Cell(1, 1)

    private fun setup(): Pair<Match, DragController> {
        val match = Match(DeckCodec.DEFAULT, Random(3))
        match.board[a] = Die(DieType.FIRE, 1)
        match.board[b] = Die(DieType.FIRE, 1)
        match.board[c] = Die(DieType.ICE, 1)
        return match to DragController(match)
    }

    @Test
    fun dropOnCompatibleMerges() {
        val (match, drag) = setup()
        assertTrue(drag.down(Layout.cellCenter(a)))
        assertEquals(a, drag.dragging)
        assertEquals(listOf(b), drag.highlights())
        drag.move(Vec2(300f, 700f))
        assertEquals(Vec2(300f, 700f), drag.pointer)
        assertTrue(drag.up(Layout.cellCenter(b)))
        assertNull(match.board[a])
        assertEquals(2, match.board[b]!!.pips)
        assertNull(drag.dragging)
    }

    @Test
    fun dropOnIncompatibleCancels() {
        val (match, drag) = setup()
        val fire = match.board[a]
        val ice = match.board[c]
        drag.down(Layout.cellCenter(a))
        assertFalse(drag.up(Layout.cellCenter(c)))
        assertSame(fire, match.board[a])
        assertSame(ice, match.board[c])
        assertNull(drag.dragging)
    }

    @Test
    fun dropOutsideCancels() {
        val (match, drag) = setup()
        drag.down(Layout.cellCenter(a))
        assertFalse(drag.up(Vec2(5f, 5f)))
        assertEquals(1, match.board[a]!!.pips)
        assertTrue(drag.highlights().isEmpty())
    }

    @Test
    fun cancelClearsDrag() {
        val (match, drag) = setup()
        drag.down(Layout.cellCenter(a))
        drag.cancel()
        assertNull(drag.dragging)
        assertNull(drag.pointer)
        assertFalse(drag.up(Layout.cellCenter(b)))
        assertEquals(1, match.board[b]!!.pips)
    }

    @Test
    fun downOnEmptyClearsStaleDrag() {
        val (_, drag) = setup()
        drag.down(Layout.cellCenter(a))
        assertFalse(drag.down(Vec2(360f, 300f)))
        assertNull(drag.dragging)
    }

    @Test
    fun downOnEmptyCellDoesNothing() {
        val (_, drag) = setup()
        assertFalse(drag.down(Layout.cellCenter(Cell(2, 4))))
        assertNull(drag.dragging)
        assertFalse(drag.up(Layout.cellCenter(a)))
    }

    @Test
    fun dragFollowsDieMovedBySwapper() {
        val (match, drag) = setup()
        val fire = match.board[a]
        val ice = match.board[c]
        drag.down(Layout.cellCenter(a))
        match.board.swap(a, c)
        assertEquals(c, drag.dragging)
        assertEquals(listOf(b), drag.highlights())
        assertTrue(drag.up(Layout.cellCenter(b)))
        assertNull(match.board[c])
        assertSame(ice, match.board[a])
        assertEquals(2, match.board[b]!!.pips)
        assertFalse(fire === match.board[b])
    }

    @Test
    fun nearbyCompatibleCellSnapsTheDrag() {
        val (match, drag) = setup()
        drag.down(Layout.cellCenter(a))
        val near = Layout.cellCenter(b).let { Vec2(it.x + DragController.SNAP_RADIUS * 0.8f, it.y) }
        drag.move(near)
        assertEquals(b, drag.snapTarget())
        val far = Layout.cellCenter(b).let { Vec2(it.x + Layout.CELL, it.y) }
        drag.move(far)
        assertNull(drag.snapTarget())
        drag.move(Layout.cellCenter(c))
        assertNull(drag.snapTarget(), "Ice is not compatible, no snap")
        assertNotNull(match)
    }

    @Test
    fun droppingNextToACompatibleCellStillMerges() {
        val (match, drag) = setup()
        drag.down(Layout.cellCenter(a))
        val gapPoint = Layout.cellCenter(b).let { Vec2(it.x - Layout.CELL / 2 - Layout.GAP / 2, it.y) }
        assertNull(Layout.cellAt(gapPoint), "the point is in the gap between cells")
        assertTrue(drag.up(gapPoint))
        assertNull(match.board[a])
        assertEquals(2, match.board[b]?.pips)
    }
}
