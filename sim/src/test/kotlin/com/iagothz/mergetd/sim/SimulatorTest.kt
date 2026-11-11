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

    @Test
    fun gameTimeLimitStopsAStalledGame() {
        // A bot that never acts on a board with no dice still loses; a 10 s limit must end it first.
        val r = Simulator.play(DeckCodec.DEFAULT, {
            object : Bot {
                override fun act(m: com.iagothz.mergetd.logic.Match) = Unit
            }
        }, seed = 1, maxSeconds = 10f)
        assertEquals(1, r.wave)
        assertEquals(null, r.killedBy)
    }
}
