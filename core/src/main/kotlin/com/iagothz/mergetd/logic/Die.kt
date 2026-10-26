package com.iagothz.mergetd.logic

class Die(val type: DieType, var pips: Int) {
    var cooldown = 0f
    /** Accumulator for periodic effects (MINE income, GROWTH pips). */
    var timer = 0f
    var disabledFor = 0f

    val disabled: Boolean get() = disabledFor > 0f
}
