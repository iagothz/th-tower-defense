package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

/** Drag a die onto a compatible one to merge; any other drop puts it back. */
class DragController(private val match: Match) {
    /** The die being dragged; tracked by identity so a Swapper boss moving it mid-drag is followed. */
    private var held: Die? = null

    /** Cell the dragged die currently sits in. */
    val dragging: Cell?
        get() {
            val die = held ?: return null
            return match.board.cells().firstOrNull { match.board[it] === die }
        }
    var pointer: Vec2? = null
        private set

    fun down(p: Vec2): Boolean {
        cancel()
        val cell = Layout.cellAt(p) ?: return false
        held = match.board[cell] ?: return false
        pointer = p
        return true
    }

    fun move(p: Vec2) {
        if (held != null) pointer = p
    }

    fun up(p: Vec2): Boolean {
        val from = dragging
        cancel()
        if (from == null) return false
        val to = Layout.cellAt(p) ?: return false
        return match.merge(from, to)
    }

    /** Drops the drag without merging (touch cancelled, app paused). */
    fun cancel() {
        held = null
        pointer = null
    }

    fun highlights(): List<Cell> {
        val from = dragging ?: return emptyList()
        return match.board.cells().filter { match.canMerge(from, it) }
    }
}
