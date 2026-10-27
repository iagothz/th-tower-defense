package com.iagothz.mergetd.ui

/** A fade to black and back around a screen switch. [update] returns true at the moment to switch. */
class Fader(private val duration: Float = 0.18f) {
    private enum class Phase { IDLE, OUT, IN }

    private var phase = Phase.IDLE
    private var t = 0f

    /** 0 = screen fully visible, 1 = black. */
    val alpha: Float
        get() = when (phase) {
            Phase.IDLE -> 0f
            Phase.OUT -> (t / duration).coerceIn(0f, 1f)
            Phase.IN -> 1f - (t / duration).coerceIn(0f, 1f)
        }

    val fading: Boolean get() = phase != Phase.IDLE

    /** Begins fading out; false (and nothing happens) if a fade is already running. */
    fun start(): Boolean {
        if (fading) return false
        phase = Phase.OUT
        t = 0f
        return true
    }

    fun update(dt: Float): Boolean {
        if (phase == Phase.IDLE) return false
        t += dt
        if (t < duration) return false
        t = 0f
        return when (phase) {
            Phase.OUT -> {
                phase = Phase.IN
                true
            }
            else -> {
                phase = Phase.IDLE
                false
            }
        }
    }
}
