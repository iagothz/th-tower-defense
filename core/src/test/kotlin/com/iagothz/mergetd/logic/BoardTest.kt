package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoardTest {
    private val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MIRROR)
    private val rng = Random(42)
    private val a = Cell(0, 0)
    private val b = Cell(0, 1)

    private fun board(first: Die?, second: Die?) = Board().apply {
        this[a] = first
        this[b] = second
    }

    @Test
    fun mergeSameTypeSamePips() {
        val board = board(Die(DieType.FIRE, 2), Die(DieType.FIRE, 2))
        assertTrue(board.merge(a, b, deck, rng))
        assertNull(board[a])
        assertEquals(3, board[b]!!.pips)
        assertTrue(board[b]!!.type in deck)
    }

    @Test
    fun rejectDifferentPips() {
        val first = Die(DieType.FIRE, 2)
        val second = Die(DieType.FIRE, 3)
        val board = board(first, second)
        assertFalse(board.merge(a, b, deck, rng))
        assertSame(first, board[a])
        assertSame(second, board[b])
    }

    @Test
    fun rejectDifferentTypes() {
        val board = board(Die(DieType.FIRE, 2), Die(DieType.ICE, 2))
        assertFalse(board.canMerge(a, b))
        assertFalse(board.merge(a, b, deck, rng))
    }

    @Test
    fun rejectSelfAndEmpty() {
        val board = board(Die(DieType.FIRE, 2), null)
        assertFalse(board.canMerge(a, a))
        assertFalse(board.canMerge(a, b))
        assertFalse(board.canMerge(b, a))
    }

    @Test
    fun rejectAtMaxPips() {
        val board = board(Die(DieType.FIRE, 7), Die(DieType.FIRE, 7))
        assertFalse(board.merge(a, b, deck, rng))
    }

    @Test
    fun mirrorCopiesOtherType() {
        val board = board(Die(DieType.MIRROR, 3), Die(DieType.ICE, 3))
        assertTrue(board.merge(a, b, deck, rng))
        assertEquals(DieType.ICE, board[b]!!.type)
        assertEquals(4, board[b]!!.pips)

        val reversed = board(Die(DieType.ICE, 3), Die(DieType.MIRROR, 3))
        assertTrue(reversed.merge(a, b, deck, rng))
        assertEquals(DieType.ICE, reversed[b]!!.type)
        assertEquals(4, reversed[b]!!.pips)
    }

    @Test
    fun mirrorWithMirrorIsRandomDeckType() {
        val board = board(Die(DieType.MIRROR, 3), Die(DieType.MIRROR, 3))
        assertTrue(board.merge(a, b, deck, rng))
        assertEquals(4, board[b]!!.pips)
        assertTrue(board[b]!!.type in deck)
    }

    @Test
    fun mergeKeepsSilenceButResetsTimers() {
        val silenced = Die(DieType.FIRE, 2).apply { disabledFor = 5f; cooldown = 1f; timer = 3f }
        val board = board(Die(DieType.FIRE, 2).apply { disabledFor = 2f }, silenced)
        assertTrue(board.merge(a, b, deck, rng))
        assertEquals(5f, board[b]!!.disabledFor)
        assertEquals(0f, board[b]!!.cooldown)
        assertEquals(0f, board[b]!!.timer)
    }

    @Test
    fun swapMovesDice() {
        val first = Die(DieType.FIRE, 1)
        val board = board(first, null)
        board.swap(a, b)
        assertNull(board[a])
        assertSame(first, board[b])
    }

    @Test
    fun emptyAndOccupiedCells() {
        val board = board(Die(DieType.FIRE, 1), null)
        assertEquals(15, board.cells().size)
        assertEquals(14, board.emptyCells().size)
        assertEquals(listOf(a), board.occupied().map { it.first })
    }
}
