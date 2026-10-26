package com.iagothz.mergetd.ui

/** A tap counts only when it is released on the same target it was pressed on. */
class Tap<T> {
    private var pressed: T? = null
    private var active = false

    fun press(target: T?) {
        pressed = target
        active = true
    }

    /** Returns the tapped target, or null when the release doesn't complete a press on it. */
    fun release(target: T?): T? {
        val hit = active && target != null && target == pressed
        cancel()
        return if (hit) target else null
    }

    fun cancel() {
        pressed = null
        active = false
    }
}
