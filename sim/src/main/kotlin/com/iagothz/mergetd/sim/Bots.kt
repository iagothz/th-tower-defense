package com.iagothz.mergetd.sim

import com.iagothz.mergetd.logic.Board
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Match
import kotlin.random.Random

/** A player strategy; [act] is called every half second of game time. */
interface Bot {
    fun act(m: Match)
}

private fun Match.mergeablePairs(): List<Pair<Cell, Cell>> {
    val cells = board.occupied().map { it.first }
    return cells.flatMap { a -> cells.filter { b -> canMerge(a, b) }.map { b -> a to b } }
}

private fun Board.isClonePair(a: Cell, b: Cell): Boolean = (this[a]!!.type == DieType.CLONE) != (this[b]!!.type == DieType.CLONE)

/** Buys whenever it can and merges a random pair whenever one exists. Never powers up. */
class SimpleBot(private val rng: Random) : Bot {
    override fun act(m: Match) {
        m.buyDie()
        m.mergeablePairs().randomOrNull(rng)?.let { (a, b) -> m.merge(a, b) }
    }
}

/**
 * Keeps the board full (more dice = more shots), merges only when there's no room,
 * highest pips first, and spends spare SP on the type with the most pips on the board.
 */
class SmartBot : Bot {
    override fun act(m: Match) {
        while (m.buyDie()) Unit
        if (m.board.emptyCells().isNotEmpty()) return

        // A Clone is only worth spending on the strongest dice on the board.
        val maxPips = m.board.occupied().maxOf { it.second.pips }
        val best = m.mergeablePairs()
            .filter { (a, b) -> !m.board.isClonePair(a, b) || m.board[a]!!.pips >= maxPips }
            .maxByOrNull { (a, _) -> m.board[a]!!.pips }
        if (best != null) {
            m.merge(best.first, best.second)
            m.buyDie()
            return
        }

        val strongest = m.board.occupied()
            .map { it.second }
            .filter { it.type.attacks }
            .groupBy { it.type }
            .maxByOrNull { (_, dice) -> dice.sumOf { it.pips } }
            ?.key ?: return
        if ((m.powerUpCost(strongest) ?: return) <= m.sp) m.powerUp(strongest)
    }
}

/** All bots by CLI name. */
val BOTS: Map<String, (Long) -> Bot> = mapOf(
    "simple" to { seed -> SimpleBot(Random(seed)) },
    "smart" to { _ -> SmartBot() },
)

/** Types that make sense to power up for the deck view in reports. */
val ATTACK_TYPES = DieType.entries.filter { it.attacks }
