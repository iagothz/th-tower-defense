package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Vec2

/** A finger held still for [holdSeconds]; drifting more than [slop] turns it into a drag and it never fires. */
class LongPress(private val holdSeconds: Float = 0.45f, private val slop: Float = 12f) {
    var origin: Vec2? = null
        private set
    private var since = 0f
    private var drifted = false

    fun down(p: Vec2, at: Float) {
        origin = p
        since = at
        drifted = false
    }

    fun move(p: Vec2) {
        val o = origin ?: return
        if (p.dst(o) > slop) drifted = true
    }

    fun up() {
        origin = null
    }

    fun isHeld(at: Float): Boolean = origin != null && !drifted && at - since >= holdSeconds
}
