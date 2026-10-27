package com.iagothz.mergetd.ui

/**
 * Which music plays and how loud the drum layer is: the drums fade in while a boss is alive and out when it
 * dies, and Rápido runs the faster pair of tracks. Pure, so the fades can be tested without audio.
 */
class MusicMix {
    /** A boss is on the path: bring the drums in. */
    var boss = false

    /** Rápido mode: the faster tracks. */
    var fast = false

    /** Drum layer volume, 0..1. */
    var drums = 0f
        private set

    val base: String get() = if (fast) "music_fast" else "music"
    val drumsTrack: String get() = if (fast) "music_drums_fast" else "music_drums"

    fun update(dt: Float) {
        val target = if (boss) 1f else 0f
        drums = if (target > drums) minOf(target, drums + dt * FADE_IN) else maxOf(target, drums - dt * FADE_OUT)
    }

    companion object {
        /** Per second. */
        const val FADE_IN = 1f
        const val FADE_OUT = 0.7f
        val TRACKS = listOf("music", "music_fast", "music_drums", "music_drums_fast")
    }
}
