package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicMixTest {
    @Test
    fun drumsFadeInForABossAndOutAfterwards() {
        val mix = MusicMix()
        assertEquals(0f, mix.drums)
        mix.boss = true
        mix.update(0.5f)
        assertEquals(0.5f, mix.drums, 0.01f)
        mix.update(1f)
        assertEquals(1f, mix.drums)
        mix.boss = false
        mix.update(0.5f)
        assertEquals(1f - 0.5f * MusicMix.FADE_OUT, mix.drums, 0.01f)
        mix.update(3f)
        assertEquals(0f, mix.drums)
    }

    @Test
    fun rushModePicksTheFasterPair() {
        val mix = MusicMix()
        assertEquals("music", mix.base)
        assertEquals("music_drums", mix.drumsTrack)
        mix.fast = true
        assertEquals("music_fast", mix.base)
        assertEquals("music_drums_fast", mix.drumsTrack)
        assertEquals(setOf("music", "music_fast", "music_drums", "music_drums_fast"), MusicMix.TRACKS.toSet())
    }
}
