package com.iagothz.mergetd.logic

import kotlin.random.Random

/** Row 0 is the top row. */
data class Cell(val row: Int, val col: Int)

class Board {
    private val slots = arrayOfNulls<Die>(Balance.ROWS * Balance.COLS)
    private val allCells = List(Balance.ROWS * Balance.COLS) { Cell(it / Balance.COLS, it % Balance.COLS) }

    operator fun get(c: Cell): Die? = slots[index(c)]

    operator fun set(c: Cell, d: Die?) {
        slots[index(c)] = d
    }

    fun cells(): List<Cell> = allCells

    fun emptyCells(): List<Cell> = allCells.filter { this[it] == null }

    fun occupied(): List<Pair<Cell, Die>> = allCells.mapNotNull { c -> this[c]?.let { c to it } }

    /**
     * Same pips always. Same type merges; a wildcard (Mirror, Boost, Clone) pairs with anything.
     * Cloning doesn't add a pip, so it is the one pairing still allowed at 7.
     */
    fun canMerge(from: Cell, to: Cell): Boolean {
        if (from == to) return false
        val a = this[from] ?: return false
        val b = this[to] ?: return false
        val clone = cloneOf(a, b)
        // A 7-pip Clone copies anything; otherwise pips must match.
        if (clone != null && clone.pips >= Balance.MAX_PIPS) return true
        if (a.pips != b.pips) return false
        if (clone != null) return true
        if (a.pips >= Balance.MAX_PIPS) return false
        return a.type == b.type || a.type.wildcard || b.type.wildcard
    }

    /** The cell whose die changes when [from] is dropped on [to]: the Clone's own cell, or [to]. */
    fun resultCell(from: Cell, to: Cell): Cell {
        val a = this[from] ?: return to
        val b = this[to] ?: return to
        return if (cloneOf(a, b) === a) from else to
    }

    fun merge(from: Cell, to: Cell, deck: List<DieType>, rng: Random): Boolean {
        if (!canMerge(from, to)) return false
        val a = this[from]!!
        val b = this[to]!!
        val clone = cloneOf(a, b)
        if (clone != null) {
            val source = if (clone === a) b else a
            this[if (clone === a) from else to] = Die(source.type, source.pips).apply { disabledFor = clone.disabledFor }
            return true
        }
        val type = when {
            // The Boost guarantees the other die's type (so two Boosts make a Boost); the Mirror and ordinary merges roll the deck.
            a.type == DieType.BOOST -> b.type
            b.type == DieType.BOOST -> a.type
            else -> deck.random(rng)
        }
        this[from] = null
        // A merge can't be used to shake off a Silencer.
        this[to] = Die(type, a.pips + 1).apply {
            disabledFor = maxOf(a.disabledFor, b.disabledFor)
            // A Hunter keeps the kills of the Hunters that formed it.
            if (type == DieType.HUNTER) {
                stacks = minOf(Balance.hunterCap(pips), listOf(a, b).filter { it.type == DieType.HUNTER }.sumOf { it.stacks })
            }
        }
        return true
    }

    /** The Clone in a pair of exactly one Clone and one other die, else null. */
    private fun cloneOf(a: Die, b: Die): Die? = when {
        a.type == DieType.CLONE && b.type != DieType.CLONE -> a
        b.type == DieType.CLONE && a.type != DieType.CLONE -> b
        else -> null
    }

    fun swap(a: Cell, b: Cell) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }

    private fun index(c: Cell) = c.row * Balance.COLS + c.col
}
