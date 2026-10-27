package com.iagothz.mergetd.ui

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/** A number on the HUD that rolls towards its real value instead of jumping, and snaps once it is close. */
class Counter(initial: Int) {
    private var value = initial.toFloat()

    val shown: Int get() = value.roundToInt()

    fun update(dt: Float, target: Int) {
        value += (target - value) * (1f - exp(-dt * RATE))
        if (abs(target - value) < 0.5f) value = target.toFloat()
    }

    private companion object {
        const val RATE = 12f
    }
}
