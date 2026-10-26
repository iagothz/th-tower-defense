package com.iagothz.mergetd.logic

class EnemyPath(val points: List<Vec2>) {
    private val segmentLengths = points.zipWithNext { a, b -> a.dst(b) }
    val length: Float = segmentLengths.sum()

    /** Position after travelling [distance] along the path, clamped to its ends. */
    fun positionAt(distance: Float): Vec2 {
        var left = distance.coerceIn(0f, length)
        for (i in segmentLengths.indices) {
            val seg = segmentLengths[i]
            if (left <= seg) {
                val a = points[i]
                val b = points[i + 1]
                val t = if (seg == 0f) 0f else left / seg
                return Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            }
            left -= seg
        }
        return points.last()
    }
}
