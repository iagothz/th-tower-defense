package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import kotlin.math.abs
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameEvent
import kotlin.math.ceil

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
                is GameEvent.WaveCleared -> names += "clear"
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

    /** "tick_3", "tick_2", "tick_1" as the wave countdown crosses each of its last three seconds; null otherwise. */
    fun countdown(prev: Float, now: Float): String? {
        if (now <= 0f) return null
        val n = ceil(now).toInt()
        if (n !in 1..3) return null
        return if (ceil(prev).toInt() > n) "tick_$n" else null
    }

    companion object {
        const val HIT_GAP = 0.05f
        val TICKS = (1..3).map { "tick_$it" }.toSet()
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
    private val tracks: Map<String, Music> = MusicMix.TRACKS.associateWith {
        Gdx.audio.newMusic(Gdx.files.internal("sounds/$it.wav")).apply {
            isLooping = true
            volume = 0f
        }
    }
    private val mix = MusicMix()

    private var duck = 1f

    /** What the music should feel like right now; the match calls this every frame. */
    fun mood(boss: Boolean, fast: Boolean) {
        mix.boss = boss
        mix.fast = fast
    }

    /** Back to the plain loop, for every screen that isn't a match. */
    fun calm() = mood(boss = false, fast = false)

    fun play(names: Set<String>) {
        if ("boss" in names) duck = DUCK
        if (save.vibrationOn && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
            // Amplitude where the device supports it (Android 8+); plain duration elsewhere.
            Haptics.pulse(names)?.let { Gdx.input.vibrate(it.millis, it.amplitude, true) }
        }
        if (!save.soundOn || save.soundVolume <= 0f) return
        names.forEach { sounds[it]?.play(VOLUME * save.soundVolume, pitchFor(it), 0f) }
    }

    /** Hits and kills land a little differently every time, so a volley doesn't sound like a machine. */
    private fun pitchFor(name: String): Float =
        if (name.startsWith("hit") || name == "kill") 1f - PITCH_SPREAD + rng.nextFloat() * 2 * PITCH_SPREAD else 1f

    fun click() = play(setOf("buy"))

    /**
     * Keeps the loops in step with the setting and the mood: the base track and its drum layer play together,
     * the drums at the mix's volume, re-synced whenever they drift; a boss entrance ducks everything for a moment.
     */
    fun updateMusic(dt: Float) {
        duck += (1f - duck) * minOf(1f, dt * DUCK_RECOVERY)
        mix.update(dt)
        val base = tracks.getValue(mix.base)
        val drums = tracks.getValue(mix.drumsTrack)
        for ((name, t) in tracks) if (name != mix.base && name != mix.drumsTrack && t.isPlaying) t.stop()
        val on = save.musicOn && save.musicVolume > 0f
        if (!on) {
            if (base.isPlaying) base.pause()
            if (drums.isPlaying) drums.pause()
            return
        }
        if (!base.isPlaying) {
            base.play()
            drums.play()
            drums.position = base.position
        } else if (abs(base.position - drums.position) > SYNC_SLACK) {
            drums.position = base.position
        }
        val volume = MUSIC_VOLUME * save.musicVolume * duck
        base.volume = volume
        drums.volume = volume * mix.drums
    }

    fun dispose() {
        sounds.values.forEach(Sound::dispose)
        tracks.values.forEach(Music::dispose)
    }

    private companion object {
        val NAMES = listOf("buy", "power", "hit", "kill", "bosskill", "leak", "wave", "boss", "deny", "clear", "record", "gameover", "pick", "drop") +
            SoundPlanner.FAMILIES.map { "hit_$it" } + SoundPlanner.MERGES + SoundPlanner.TICKS
        const val VOLUME = 0.6f
        const val DUCK = 0.2f
        const val DUCK_RECOVERY = 1.2f
        /** Seconds the drum layer may drift from the base before it is snapped back. */
        const val SYNC_SLACK = 0.08f
        const val PITCH_SPREAD = 0.1f
        const val MUSIC_VOLUME = 0.35f
    }
}
