package com.iagothz.mergetd.sim

import com.iagothz.mergetd.logic.DeckCodec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimulatorTest {
    @Test
    fun sameSeedGivesSameResult() {
        val a = Simulator.play(DeckCodec.DEFAULT, { SimpleBot(Random(it)) }, seed = 11)
        val b = Simulator.play(DeckCodec.DEFAULT, { SimpleBot(Random(it)) }, seed = 11)
        assertEquals(a, b)
    }

    @Test
    fun gameEndsWithAKiller() {
        val r = Simulator.play(DeckCodec.DEFAULT, { SimpleBot(Random(it)) }, seed = 5)
        assertTrue(r.wave in 1..Simulator.MAX_WAVE)
        if (r.wave < Simulator.MAX_WAVE) assertNotNull(r.killedBy)
    }

    @Test
    fun capStopsEndlessGames() {
        val r = Simulator.play(DeckCodec.DEFAULT, { SmartBot() }, seed = 5, maxWave = 2)
        assertTrue(r.wave <= 3)
    }
}
