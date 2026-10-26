package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.iagothz.mergetd.logic.GameEvent

/** Decides which sounds a frame's events play: each at most once per frame, hits rate-limited. */
class SoundPlanner {
    private var lastHit = Float.NEGATIVE_INFINITY

    fun plan(events: List<GameEvent>, now: Float): Set<String> {
        val names = linkedSetOf<String>()
        for (e in events) {
            when (e) {
                is GameEvent.DieBought -> names += "buy"
                is GameEvent.Merged -> names += "merge"
                is GameEvent.PoweredUp -> names += "power"
                is GameEvent.Killed -> names += "kill"
                is GameEvent.Leaked -> names += "leak"
                is GameEvent.WaveStarted -> names += if (e.boss) "boss" else "wave"
                is GameEvent.Hit -> if (now - lastHit >= HIT_GAP) {
                    names += "hit"
                    lastHit = now
                }
            }
        }
        return names
    }

    companion object {
        const val HIT_GAP = 0.08f
    }
}

/** Loads the generated sounds from assets/sounds and plays them unless muted. */
class Sfx(private val save: SaveData) {
    private val sounds = NAMES.associateWith { Gdx.audio.newSound(Gdx.files.internal("sounds/$it.wav")) }

    fun play(names: Set<String>) {
        if (!save.soundOn) return
        names.forEach { sounds[it]?.play(VOLUME) }
    }

    fun click() = play(setOf("buy"))

    fun dispose() = sounds.values.forEach(Sound::dispose)

    private companion object {
        val NAMES = listOf("buy", "merge", "power", "hit", "kill", "leak", "wave", "boss")
        const val VOLUME = 0.6f
    }
}
