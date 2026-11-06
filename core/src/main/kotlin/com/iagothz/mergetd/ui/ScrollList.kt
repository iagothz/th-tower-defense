package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Vec2
import kotlin.math.abs
import kotlin.math.floor

/**
 * A vertical list of [count] rows shown in the window between [bottom] and [top] (world units). Dragging inside
 * the window scrolls it; a press that moves less than [SLOP] and is released on a row is a tap on that row.
 */
class ScrollList(val count: Int, val top: Float, val bottom: Float, val rowHeight: Float, val gap: Float) {
    /** How far the list is scrolled up: 0 shows the first row at the top. */
    var offset = 0f
        private set

    val maxOffset: Float get() = maxOf(0f, count * (rowHeight + gap) - gap - (top - bottom))

    private var pressing = false
    private var moved = false
    private var startY = 0f
    private var startOffset = 0f

    /** World y of the top edge of row [i] at the current scroll. */
    fun rowTop(i: Int): Float = top + offset - i * (rowHeight + gap)

    fun isVisible(i: Int): Boolean = rowTop(i) > bottom && rowTop(i) - rowHeight < top

    fun down(p: Vec2) {
        pressing = p.y in bottom..top
        moved = false
        startY = p.y
        startOffset = offset
    }

    fun move(p: Vec2) {
        if (!pressing) return
        val dy = p.y - startY
        if (abs(dy) >= SLOP) moved = true
        if (moved) offset = (startOffset + dy).coerceIn(0f, maxOffset)
    }

    /** The row tapped, or null when the press scrolled, started outside the window or landed between rows. */
    fun up(p: Vec2): Int? {
        val tapped = pressing && !moved
        cancel()
        return if (tapped) rowAt(p.y) else null
    }

    fun cancel() {
        pressing = false
        moved = false
    }

    private fun rowAt(y: Float): Int? {
        if (y !in bottom..top) return null
        val fromTop = top + offset - y
        val i = floor(fromTop / (rowHeight + gap)).toInt()
        if (i !in 0 until count) return null
        return i.takeIf { fromTop - i * (rowHeight + gap) <= rowHeight }
    }

    companion object {
        /** World units a press may wobble and still be a tap. */
        const val SLOP = 14f
    }
}
