package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

/** Drag a die onto a compatible one to merge; any other drop puts it back. */
class DragController(private val match: Match) {
    var dragging: Cell? = null
        private set
    var pointer: Vec2? = null
        private set

    fun down(p: Vec2): Boolean {
        cancel()
        val cell = Layout.cellAt(p) ?: return false
        if (match.board[cell] == null) return false
        dragging = cell
        pointer = p
        return true
    }

    fun move(p: Vec2) {
        if (dragging != null) pointer = p
    }

    fun up(p: Vec2): Boolean {
        val from = dragging ?: return false
        dragging = null
        pointer = null
        val to = Layout.cellAt(p) ?: return false
        return match.merge(from, to)
    }

    /** Drops the drag without merging (touch cancelled, app paused). */
    fun cancel() {
        dragging = null
        pointer = null
    }

    fun highlights(): List<Cell> {
        val from = dragging ?: return emptyList()
        return match.board.cells().filter { match.canMerge(from, it) }
    }
}
