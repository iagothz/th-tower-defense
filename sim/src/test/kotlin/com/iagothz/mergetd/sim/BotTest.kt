package com.iagothz.mergetd.sim

import com.iagothz.mergetd.logic.Board
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Match
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BotTest {
    private fun match() = Match(DeckCodec.DEFAULT, Random(3))

    /** Fills every empty cell with dice that can't merge with each other (unique type+pips, no Mirror). */
    private fun Board.fillUnmergeable(skip: Set<Pair<DieType, Int>> = emptySet()) {
        val attackers = DieType.entries.filter { it.attacks }
        val combos = attackers.flatMap { t -> (1..7).map { t to it } }.filter { it !in skip }.iterator()
        for (c in emptyCells()) {
            val (t, p) = combos.next()
            this[c] = Die(t, p)
        }
    }

    @Test
    fun simpleBotBuysWhenItCan() {
        val m = match()
        SimpleBot(Random(1)).act(m)
        assertEquals(1, m.board.occupied().size)
    }

    @Test
    fun simpleBotMergesAPair() {
        val m = match()
        m.sp = 0
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(1, 1)] = Die(DieType.FIRE, 1)
        SimpleBot(Random(1)).act(m)
        assertEquals(listOf(2), m.board.occupied().map { it.second.pips })
    }

    @Test
    fun smartBotFillsTheBoardMergesThenRefills() {
        val m = match()
        m.sp = 100_000
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 1)
        SmartBot().act(m)
        assertEquals(15, m.board.occupied().size)
        assertEquals(1, m.board.occupied().count { it.second.pips == 2 })
    }

    @Test
    fun smartBotMergesHighestPipsFirst() {
        val m = match()
        m.sp = 0
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 3)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 3)
        m.board[Cell(0, 2)] = Die(DieType.ICE, 1)
        m.board[Cell(0, 3)] = Die(DieType.ICE, 1)
        m.board.fillUnmergeable(skip = setOf(DieType.FIRE to 3, DieType.ICE to 1, DieType.FIRE to 4))
        SmartBot().act(m)
        val dice = m.board.occupied().map { it.second }
        assertEquals(2, dice.count { it.type == DieType.ICE && it.pips == 1 })
        assertEquals(0, dice.count { it.type == DieType.FIRE && it.pips == 3 })
    }

    @Test
    fun smartBotPowersUpTheStrongestTypeWhenStuck() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 7)
        m.board.fillUnmergeable(skip = setOf(DieType.FIRE to 7, DieType.ICE to 7))
        m.sp = 1000
        SmartBot().act(m)
        assertEquals(2, m.powerLevel(DieType.FIRE))
    }
}
