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

    fun canMerge(from: Cell, to: Cell): Boolean {
        if (from == to) return false
        val a = this[from] ?: return false
        val b = this[to] ?: return false
        if (a.pips != b.pips || a.pips >= Balance.MAX_PIPS) return false
        return a.type == b.type || a.type == DieType.MIRROR || b.type == DieType.MIRROR
    }

    fun merge(from: Cell, to: Cell, deck: List<DieType>, rng: Random): Boolean {
        if (!canMerge(from, to)) return false
        val a = this[from]!!
        val b = this[to]!!
        val type = when {
            a.type == DieType.MIRROR && b.type != DieType.MIRROR -> b.type
            b.type == DieType.MIRROR && a.type != DieType.MIRROR -> a.type
            else -> deck.random(rng)
        }
        this[from] = null
        // A merge can't be used to shake off a Silencer.
        this[to] = Die(type, a.pips + 1).apply {
            disabledFor = maxOf(a.disabledFor, b.disabledFor)
            // A Hunter keeps the kills of the Hunters that formed it.
            if (type == DieType.HUNTER) {
                stacks = minOf(Balance.HUNTER_MAX_STACKS, listOf(a, b).filter { it.type == DieType.HUNTER }.sumOf { it.stacks })
            }
        }
        return true
    }

    fun swap(a: Cell, b: Cell) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }

    private fun index(c: Cell) = c.row * Balance.COLS + c.col
}
