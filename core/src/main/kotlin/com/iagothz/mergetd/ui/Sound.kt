package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.iagothz.mergetd.logic.DieType
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
                is GameEvent.Blast -> Unit
                is GameEvent.Stolen -> names += "leak"
                is GameEvent.Hit -> if (now - lastHit >= HIT_GAP) {
                    names += e.type?.let { "hit_${family(it)}" } ?: "hit"
                    lastHit = now
                }
            }
        }
        return names
    }

    companion object {
        const val HIT_GAP = 0.05f
        val FAMILIES = setOf("fire", "ice", "zap", "wind", "heavy", "magic")

        /** Which attack sound a die makes. */
        fun family(t: DieType): String = when (t) {
            DieType.FIRE, DieType.NOVA, DieType.TRAP, DieType.QUAKE -> "fire"
            DieType.ICE, DieType.TIME -> "ice"
            DieType.ELECTRIC, DieType.LIGHT, DieType.CRIT -> "zap"
            DieType.WIND -> "wind"
            DieType.IRON, DieType.SNIPER, DieType.GRAVITY, DieType.HUNTER -> "heavy"
            else -> "magic"
        }
    }
}

class Sfx(private val save: SaveData) {
    private val sounds = NAMES.associateWith { Gdx.audio.newSound(Gdx.files.internal("sounds/$it.wav")) }
    private val music: Music = Gdx.audio.newMusic(Gdx.files.internal("sounds/music.wav")).apply {
        isLooping = true
        volume = MUSIC_VOLUME
    }

    fun play(names: Set<String>) {
        if (save.vibrationOn && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
            when {
                "leak" in names -> Gdx.input.vibrate(80)
                "boss" in names -> Gdx.input.vibrate(40)
            }
        }
        if (!save.soundOn) return
        names.forEach { sounds[it]?.play(VOLUME) }
    }

    fun click() = play(setOf("buy"))

    /** Keeps the loop in step with the setting; cheap enough to call every frame. */
    fun updateMusic() {
        if (save.musicOn && !music.isPlaying) music.play()
        if (!save.musicOn && music.isPlaying) music.pause()
    }

    fun dispose() {
        sounds.values.forEach(Sound::dispose)
        music.dispose()
    }

    private companion object {
        val NAMES = listOf("buy", "merge", "power", "hit", "kill", "leak", "wave", "boss") + SoundPlanner.FAMILIES.map { "hit_$it" }
        const val VOLUME = 0.6f
        const val MUSIC_VOLUME = 0.35f
    }
}
