package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.iagothz.mergetd.logic.Balance
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
                is GameEvent.Merged -> names += if (e.pips >= Balance.MAX_PIPS) "max" else "merge_${e.pips.coerceIn(2, 6)}"
                is GameEvent.Unmerged -> names += "buy"
                is GameEvent.PoweredUp -> names += "power"
                is GameEvent.Killed -> names += if (e.kind.isBoss) "bosskill" else "kill"
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
        /** One merge sound per result size, a semitone pair higher each pip, and a fanfare at 7. */
        val MERGES = (2..6).map { "merge_$it" }.toSet() + "max"

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

class Sfx(private val save: SaveData, private val rng: kotlin.random.Random = kotlin.random.Random.Default) {
    private val sounds = NAMES.associateWith { Gdx.audio.newSound(Gdx.files.internal("sounds/$it.wav")) }
    private val music: Music = Gdx.audio.newMusic(Gdx.files.internal("sounds/music.wav")).apply {
        isLooping = true
        volume = MUSIC_VOLUME
    }

    fun play(names: Set<String>) {
        if (save.vibrationOn && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
            when {
                "leak" in names -> Gdx.input.vibrate(80)
                "bosskill" in names -> Gdx.input.vibrate(60)
                "boss" in names -> Gdx.input.vibrate(40)
                "max" in names -> Gdx.input.vibrate(30)
                names.any { it in SoundPlanner.MERGES } -> Gdx.input.vibrate(12)
            }
        }
        if (!save.soundOn || save.soundVolume <= 0f) return
        names.forEach { sounds[it]?.play(VOLUME * save.soundVolume, pitchFor(it), 0f) }
    }

    /** Hits and kills land a little differently every time, so a volley doesn't sound like a machine. */
    private fun pitchFor(name: String): Float =
        if (name.startsWith("hit") || name == "kill") 1f - PITCH_SPREAD + rng.nextFloat() * 2 * PITCH_SPREAD else 1f

    fun click() = play(setOf("buy"))

    /** Keeps the loop in step with the setting; cheap enough to call every frame. */
    fun updateMusic() {
        val on = save.musicOn && save.musicVolume > 0f
        if (on && !music.isPlaying) music.play()
        if (!on && music.isPlaying) music.pause()
        if (on) music.volume = MUSIC_VOLUME * save.musicVolume
    }

    fun dispose() {
        sounds.values.forEach(Sound::dispose)
        music.dispose()
    }

    private companion object {
        val NAMES = listOf("buy", "power", "hit", "kill", "bosskill", "leak", "wave", "boss") + SoundPlanner.FAMILIES.map { "hit_$it" } + SoundPlanner.MERGES
        const val VOLUME = 0.6f
        const val PITCH_SPREAD = 0.1f
        const val MUSIC_VOLUME = 0.35f
    }
}
