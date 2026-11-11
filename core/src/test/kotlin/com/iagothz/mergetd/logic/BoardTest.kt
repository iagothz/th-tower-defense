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

    /** The Mirror is the wildcard: it merges with anything of its pips, and the result is a deck roll like any merge. */
    @Test
    fun mirrorMergesWithAnyTypeIntoADeckRoll() {
        val results = (0 until 40).map { seed ->
            val board = board(Die(DieType.MIRROR, 3), Die(DieType.HUNTER, 3))
            assertTrue(board.merge(a, b, deck, Random(seed)))
            assertNull(board[a])
            assertEquals(4, board[b]!!.pips)
            board[b]!!.type
        }
        assertTrue(results.all { it in deck }, "$results")
        assertTrue(results.toSet().size > 1, "a Mirror merge must be a roll, got $results")
    }

    /** The Boost is the guaranteed wildcard: the result keeps the other die's type. */
    @Test
    fun boostKeepsTheOtherType() {
        val board = board(Die(DieType.BOOST, 3), Die(DieType.HUNTER, 3).apply { stacks = 9 })
        assertTrue(board.merge(a, b, deck, rng))
        assertNull(board[a])
        assertEquals(DieType.HUNTER, board[b]!!.type)
        assertEquals(4, board[b]!!.pips)
        assertEquals(9, board[b]!!.stacks)

        val reversed = board(Die(DieType.ICE, 3), Die(DieType.BOOST, 3))
        assertTrue(reversed.merge(a, b, deck, rng))
        assertEquals(DieType.ICE, reversed[b]!!.type)
        assertEquals(4, reversed[b]!!.pips)

        // Two Boosts make a bigger Boost: each guarantees the other's type.
        val two = board(Die(DieType.BOOST, 3), Die(DieType.BOOST, 3))
        assertTrue(two.merge(a, b, deck, rng))
        assertEquals(DieType.BOOST, two[b]!!.type)
        assertEquals(4, two[b]!!.pips)

        val withMirror = board(Die(DieType.BOOST, 2), Die(DieType.MIRROR, 2))
        assertTrue(withMirror.merge(a, b, deck, rng))
        assertEquals(DieType.MIRROR, withMirror[b]!!.type)
    }

    /** The Clone becomes a copy of a die with the same pips; that die stays put. Either die can be dragged. */
    @Test
    fun cloneCopiesTheOtherDieAndLeavesIt() {
        val hunter = Die(DieType.HUNTER, 3).apply { stacks = 9 }
        val board = board(Die(DieType.CLONE, 3), hunter)
        assertTrue(board.canMerge(a, b))
        assertEquals(a, board.resultCell(a, b))
        assertTrue(board.merge(a, b, deck, rng))
        assertSame(hunter, board[b])
        assertEquals(DieType.HUNTER, board[a]!!.type)
        assertEquals(3, board[a]!!.pips)
        assertEquals(0, board[a]!!.stacks)

        val clone = Die(DieType.CLONE, 7)
        val reversed = board(Die(DieType.ICE, 7), clone)
        assertEquals(b, reversed.resultCell(a, b))
        assertTrue(reversed.merge(a, b, deck, rng))
        assertEquals(DieType.ICE, reversed[a]!!.type)
        assertEquals(DieType.ICE, reversed[b]!!.type)
        assertEquals(7, reversed[b]!!.pips)

        assertFalse(board(Die(DieType.CLONE, 2), Die(DieType.ICE, 3)).canMerge(a, b))
    }

    /** Between two wildcards, the one being dragged applies its effect to the one it lands on. */
    @Test
    fun theDraggedWildcardDecides() {
        // Boost onto Clone: the Clone grows a pip.
        val boostOntoClone = board(Die(DieType.BOOST, 2), Die(DieType.CLONE, 2))
        assertEquals(b, boostOntoClone.resultCell(a, b))
        assertTrue(boostOntoClone.merge(a, b, deck, rng))
        assertNull(boostOntoClone[a])
        assertEquals(DieType.CLONE, boostOntoClone[b]!!.type)
        assertEquals(3, boostOntoClone[b]!!.pips)

        // Clone onto Boost: a copy of the Boost, which stays put.
        val cloneOntoBoost = board(Die(DieType.CLONE, 2), Die(DieType.BOOST, 2))
        assertEquals(a, cloneOntoBoost.resultCell(a, b))
        assertTrue(cloneOntoBoost.merge(a, b, deck, rng))
        assertEquals(DieType.BOOST, cloneOntoBoost[a]!!.type)
        assertEquals(2, cloneOntoBoost[a]!!.pips)
        assertEquals(DieType.BOOST, cloneOntoBoost[b]!!.type)

        // Mirror onto Boost rolls the deck; Boost onto Mirror keeps the Mirror.
        val mirrorOntoBoost = board(Die(DieType.MIRROR, 2), Die(DieType.BOOST, 2))
        assertTrue(mirrorOntoBoost.merge(a, b, deck, rng))
        assertTrue(mirrorOntoBoost[b]!!.type in deck)
        val boostOntoMirror = board(Die(DieType.BOOST, 2), Die(DieType.MIRROR, 2))
        assertTrue(boostOntoMirror.merge(a, b, deck, rng))
        assertEquals(DieType.MIRROR, boostOntoMirror[b]!!.type)
    }

    @Test
    fun twoClonesMergeNormally() {
        val board = board(Die(DieType.CLONE, 3), Die(DieType.CLONE, 3))
        assertEquals(b, board.resultCell(a, b))
        assertTrue(board.merge(a, b, deck, rng))
        assertNull(board[a])
        assertEquals(4, board[b]!!.pips)
        assertTrue(board[b]!!.type in deck)
    }

    @Test
    fun resultCellIsTheTargetForOrdinaryMerges() {
        assertEquals(b, board(Die(DieType.FIRE, 2), Die(DieType.FIRE, 2)).resultCell(a, b))
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
        val silenced = Die(DieType.FIRE, 2).apply {
            disabledFor = 5f
            cooldown = 1f
            timer = 3f
        }
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
