package com.iagothz.mergetd.logic

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    fun dst(o: Vec2): Float {
        val dx = x - o.x
        val dy = y - o.y
        return sqrt(dx * dx + dy * dy)
    }
}
