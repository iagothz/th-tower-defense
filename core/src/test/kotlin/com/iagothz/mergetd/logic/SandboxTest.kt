package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxTest {
    private fun sandbox() = Match(DeckCodec.DEFAULT, Random(7), sandbox = true)

    @Test
    fun wavesWaitUntilAskedFor() {
        val m = sandbox()
        repeat(2000) { m.update(Balance.STEP) }
        assertEquals(0, m.wave)
        assertEquals(0f, m.waveCountdown)
        m.sendWave(3)
        assertEquals(3, m.wave)
        assertTrue(m.waveRunning)
    }

    @Test
    fun leaksCostNoLife() {
        val m = sandbox()
        val lives = m.lives
        m.sendWave(1)
        repeat(20_000) { m.update(Balance.STEP) }
        assertEquals(lives, m.lives)
        assertFalse(m.isGameOver)
        // The wave ends once everything has walked through, and the next one still waits.
        assertFalse(m.waveRunning)
        assertEquals(1, m.wave)
    }

    @Test
    fun placesAnyDieAtAnyPipsAndRemovesIt() {
        val m = sandbox()
        val c = Cell(1, 2)
        assertTrue(m.place(c, DieType.BOOST, 9))
        assertEquals(DieType.BOOST, m.board[c]?.type)
        assertEquals(Balance.MAX_PIPS, m.board[c]?.pips)
        m.place(c, DieType.FIRE, 0)
        assertEquals(1, m.board[c]?.pips)
        assertTrue(m.remove(c))
        assertNull(m.board[c])
        assertFalse(m.remove(c))
    }

    @Test
    fun stopAndClearEmptyPathAndBoard() {
        val m = sandbox()
        m.place(Cell(0, 0), DieType.ICE, 4)
        m.sendWave(5)
        m.update(1f)
        m.stopWave()
        assertTrue(m.enemies.isEmpty())
        assertFalse(m.waveRunning)
        m.clearBoard()
        assertTrue(m.board.occupied().isEmpty())
    }

    @Test
    fun anOrdinaryMatchIgnoresTheSandboxActions() {
        val m = Match(DeckCodec.DEFAULT, Random(7))
        assertFalse(m.place(Cell(0, 0), DieType.FIRE, 7))
        assertNull(m.board[Cell(0, 0)])
        m.sendWave(9)
        assertEquals(0, m.wave)
        m.board[Cell(0, 0)] = Die(DieType.ICE, 1)
        assertFalse(m.moveDie(Cell(0, 0), Cell(0, 1)))
        assertNull(m.board[Cell(0, 1)])
    }

    @Test
    fun movingADieTakesAnEmptyCellOrSwapsWithAnother() {
        val m = sandbox()
        m.place(Cell(0, 0), DieType.ICE, 2)
        m.place(Cell(2, 4), DieType.FIRE, 5)
        assertTrue(m.moveDie(Cell(0, 0), Cell(1, 1)))
        assertNull(m.board[Cell(0, 0)])
        assertEquals(DieType.ICE, m.board[Cell(1, 1)]?.type)
        assertTrue(m.moveDie(Cell(1, 1), Cell(2, 4)))
        assertEquals(DieType.FIRE, m.board[Cell(1, 1)]?.type)
        assertEquals(DieType.ICE, m.board[Cell(2, 4)]?.type)
        assertFalse(m.moveDie(Cell(0, 0), Cell(1, 1)))
        assertFalse(m.moveDie(Cell(1, 1), Cell(1, 1)))
    }
}
