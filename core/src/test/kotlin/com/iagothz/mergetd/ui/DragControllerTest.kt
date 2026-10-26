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
    fun downOnEmptyCellDoesNothing() {
        val (_, drag) = setup()
        assertFalse(drag.down(Layout.cellCenter(Cell(2, 4))))
        assertNull(drag.dragging)
        assertFalse(drag.up(Layout.cellCenter(a)))
    }
}
