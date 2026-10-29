package com.iagothz.mergetd.ui

import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.abs

/**
 * The deck changes only by dragging: a die from the grid onto a slot joins the deck (replacing the die there),
 * a die from a slot dropped outside the slots leaves it, and slot onto slot swaps them. A tap, on a tile or a slot,
 * only picks the die to show, so reading what a die does never touches the deck.
 */
class DeckEditor(
    val deck: MutableList<DieType>,
    private val tiles: List<Pair<DieType, Rectangle>>,
    private val slots: List<Rectangle>,
    private val isOpen: (DieType) -> Boolean,
) {
    /** What a release reports: the die to show (if the touch was on one) and whether the deck changed. */
    data class Release(val shown: DieType?, val changed: Boolean)

    private var pressedTile: DieType? = null
    private var pressedSlot: Int? = null
    private var origin: Vec2? = null

    /** The die under the finger once the press drifted past [DRAG_START]; null while it still counts as a tap. */
    var dragging: DieType? = null
        private set
    var pointer: Vec2? = null
        private set

    /** The slot the drag started from, for drawing it empty while its die is in the air. */
    val fromSlot: Int? get() = if (dragging != null) pressedSlot else null

    /** True when the press lands on a tile or a slot; anything else is the screen's. */
    fun down(p: Vec2): Boolean {
        cancel()
        pressedTile = tiles.firstOrNull { it.second.contains(p.x, p.y) }?.first
        pressedSlot = slots.indexOfFirst { it.contains(p.x, p.y) }.takeIf { it >= 0 }
        if (pressedTile == null && pressedSlot == null) return false
        origin = p
        return true
    }

    fun move(p: Vec2) {
        val o = origin ?: return
        if (dragging == null && p.dst(o) > DRAG_START) {
            dragging = pressedTile?.takeIf(isOpen) ?: pressedSlot?.let { deck.getOrNull(it) }
        }
        if (dragging != null) pointer = p
    }

    fun up(p: Vec2): Release {
        val die = dragging
        val tile = pressedTile
        val slot = pressedSlot
        cancel()
        if (die == null) return Release(tile ?: slot?.let { deck.getOrNull(it) }, false)
        val target = slotAt(p)
        val from = slot ?: deck.indexOf(die).takeIf { it >= 0 }
        val changed = when {
            // Dropped away from the slots: out of the deck, unless it was lifted from the grid.
            target == null -> if (from != null && tile == null) { deck.removeAt(from); true } else false
            from != null -> target != from && swap(from, target)
            target < deck.size -> { deck[target] = die; true }
            deck.size < DeckCodec.SIZE -> { deck += die; true }
            else -> false
        }
        return Release(die, changed)
    }

    /** Swaps the dice in two slot positions; a target past the end moves the die to the end instead. */
    private fun swap(from: Int, to: Int): Boolean {
        if (to < deck.size) {
            val other = deck[to]
            deck[to] = deck[from]
            deck[from] = other
        } else {
            deck.add(deck.removeAt(from))
        }
        return true
    }

    fun cancel() {
        pressedTile = null
        pressedSlot = null
        origin = null
        dragging = null
        pointer = null
    }

    /** The slot a drop at [p] means: the nearest one while the finger is in the slots' band (with some slop). */
    fun slotAt(p: Vec2): Int? {
        val first = slots.first()
        val last = slots.last()
        if (p.y < first.y - SLOT_SLOP || p.y > first.y + first.height + SLOT_SLOP) return null
        if (p.x < first.x - SLOT_SLOP || p.x > last.x + last.width + SLOT_SLOP) return null
        return slots.indices.minByOrNull { abs(slots[it].x + slots[it].width / 2 - p.x) }
    }

    companion object {
        /** How far the finger drifts before a press becomes a drag, in world units. */
        const val DRAG_START = 14f
        const val SLOT_SLOP = 20f
    }
}
