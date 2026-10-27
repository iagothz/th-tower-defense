package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Match
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HintsTest {
    private val deck = listOf(DieType.FIRE, DieType.ICE, DieType.MIRROR, DieType.WIND, DieType.POISON)
    private fun match() = Match(deck, Random(1))
    private val none = emptyList<GameEvent>()

    @Test
    fun firstWildcardBoughtGetsAHintThatExpires() {
        val m = match()
        val hints = Hints(mutableSetOf())
        m.board[Cell(0, 0)] = Die(DieType.MIRROR, 1)
        hints.update(0f, listOf(GameEvent.DieBought(Cell(0, 0))), m)
        assertEquals("wildcard", hints.current)
        assertNotNull(hints.text)
        hints.update(Hints.SHOW_SECONDS + 0.1f, none, m)
        assertNull(hints.current)
        assertTrue("wildcard" in hints.seen)
    }

    @Test
    fun undoHintAfterTheFirstMerge() {
        val hints = Hints(mutableSetOf())
        hints.update(0f, listOf(GameEvent.Merged(Cell(1, 1), pips = 2)), match())
        assertEquals("undo", hints.current)
    }

    @Test
    fun holdHintAfterThreeBuysUnlessThePlayerAlreadyHeld() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        val bought = listOf(GameEvent.DieBought(Cell(0, 0)))
        val hints = Hints(mutableSetOf())
        repeat(2) { hints.update(0f, bought, m) }
        assertNull(hints.current)
        hints.update(0f, bought, m)
        assertEquals("hold", hints.current)

        val patient = Hints(mutableSetOf())
        patient.noteHold()
        repeat(4) { patient.update(0f, bought, m) }
        assertNull(patient.current)
    }

    @Test
    fun synergyAndSevenComeFromTheBoard() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.WIND, 1)
        val hints = Hints(mutableSetOf())
        hints.update(0f, none, m)
        assertEquals("synergy", hints.current)

        val six = match()
        six.board[Cell(0, 0)] = Die(DieType.ICE, 6)
        val h2 = Hints(mutableSetOf())
        h2.update(0f, none, six)
        assertEquals("seven", h2.current)
    }

    @Test
    fun hintsShowOnceEverAndOneAtATime() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.WIND, 1)
        val hints = Hints(mutableSetOf("synergy"))
        hints.update(0f, listOf(GameEvent.Merged(Cell(0, 1), pips = 2), GameEvent.Merged(Cell(0, 1), pips = 2)), m)
        assertEquals("undo", hints.current, "synergy was already seen; undo shows once")
        hints.update(Hints.SHOW_SECONDS + 0.1f, none, m)
        assertNull(hints.current)
        assertEquals(setOf("synergy", "undo"), hints.seen)
    }
}
