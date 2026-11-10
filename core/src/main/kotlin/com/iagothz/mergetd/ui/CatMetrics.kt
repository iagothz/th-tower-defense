package com.iagothz.mergetd.ui

/**
 * Frame sizes of the fire kitten sheets, read from assets/sprites/fire_cats.properties (written by the art
 * pipeline). Every level's frames are cropped differently around the cat, but all share one render scale, so a
 * kitten is the same size at every level. Sizes are in render pixels: `ref` is the bare (pip-1) cat's height,
 * `N.w` / `N.h` a level's frame and `N.foot` how far above the frame's bottom the feet stand.
 */
class CatMetrics(private val ref: Float, private val levels: Map<Int, Level>) {
    class Level(val w: Float, val h: Float, val foot: Float)

    /** A frame in world units, for a bare kitten [bareHeight] tall. */
    class Frame(val width: Float, val height: Float, val foot: Float)

    val pips: Set<Int> get() = levels.keys

    fun frame(pips: Int, bareHeight: Float): Frame {
        val l = levels.getValue(pips)
        val k = bareHeight / ref
        return Frame(l.w * k, l.h * k, l.foot * k)
    }

    companion object {
        fun parse(text: String): CatMetrics {
            val kv = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            val levels = kv.keys.mapNotNull { it.substringBefore('.').toIntOrNull() }.toSet().associateWith { p ->
                Level(kv.getValue("$p.w").toFloat(), kv.getValue("$p.h").toFloat(), kv.getValue("$p.foot").toFloat())
            }
            return CatMetrics(kv.getValue("ref").toFloat(), levels)
        }
    }
}
