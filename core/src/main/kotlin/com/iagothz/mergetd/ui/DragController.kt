package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.sqrt

/** Drag a die onto a compatible one to merge; any other drop puts it back. A compatible cell within [SNAP_RADIUS] pulls the die in. */
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
        val to = dropTarget(p)
        cancel()
        if (from == null || to == null) return false
        return match.merge(from, to)
    }

    /** Where a release at [p] lands the dragged die: the cell under it, or a compatible one close enough to snap to. */
    fun dropTarget(p: Vec2): Cell? {
        val from = dragging ?: return null
        val under = Layout.cellAt(p)
        if (under != null && match.canMerge(from, under)) return under
        return snapNear(from, p) ?: under
    }

    /** The compatible cell the pointer is currently snapped to, if any. */
    fun snapTarget(): Cell? {
        val from = dragging ?: return null
        val p = pointer ?: return null
        return snapNear(from, p)
    }

    private fun snapNear(from: Cell, p: Vec2): Cell? {
        var best: Cell? = null
        var bestDist = SNAP_RADIUS
        for (cell in match.board.cells()) {
            if (!match.canMerge(from, cell)) continue
            val c = Layout.cellCenter(cell)
            val d = sqrt((c.x - p.x) * (c.x - p.x) + (c.y - p.y) * (c.y - p.y))
            if (d <= bestDist) {
                best = cell
                bestDist = d
            }
        }
        return best
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

    companion object {
        /** Reaches across the gap between cells, so a drop that lands just short still merges. */
        const val SNAP_RADIUS = 64f
    }
}
