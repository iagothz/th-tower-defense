package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchSnapshotTest {
    /** A match in the middle of wave 3, with dice, power-ups, statuses and a half-spawned wave. */
    private fun midWave(): Match {
        val deck = listOf(DieType.ICE, DieType.POISON, DieType.MINE, DieType.GROWTH, DieType.SNIPER)
        val m = Match(deck, Random(11))
        m.sp = 2000
        repeat(6) { m.buyDie() }
        m.powerUp(DieType.ICE)
        m.board[Cell(2, 4)] = Die(DieType.POISON, 3).apply { disabledFor = 2.5f; timer = 1.25f }
        m.board[Cell(0, 0)] = Die(DieType.ICE, 2)
        m.board[Cell(0, 1)] = Die(DieType.POISON, 1)
        m.debugStartWave(3)
        repeat(120) { m.update(Balance.STEP) }
        return m
    }

    @Test
    fun roundTripKeepsEveryField() {
        val m = midWave()
        assertTrue(m.enemies.isNotEmpty())
        assertTrue(m.enemies.any { it.slow > 0f || it.poison.isNotEmpty() }, "fixture should have status effects")
        val text = m.snapshot()
        val restored = assertNotNull(Match.restore(text))
        assertEquals(text, restored.snapshot())
        assertEquals(m.deck, restored.deck)
        assertEquals(m.wave, restored.wave)
        assertEquals(m.sp, restored.sp)
        assertEquals(m.lives, restored.lives)
        assertEquals(m.buyCost, restored.buyCost)
        assertEquals(m.powerLevel(DieType.ICE), restored.powerLevel(DieType.ICE))
        assertEquals(m.board[Cell(2, 4)]!!.timer, restored.board[Cell(2, 4)]!!.timer)
        assertTrue(restored.board[Cell(2, 4)]!!.disabled)
        assertEquals(m.enemies.map { it.hp }, restored.enemies.map { it.hp })
    }

    @Test
    fun restoredMatchKeepsPlaying() {
        val restored = assertNotNull(Match.restore(midWave().snapshot()))
        val wave = restored.wave
        repeat(60 * 120) { restored.update(Balance.STEP) }
        assertTrue(restored.wave > wave || restored.isGameOver)
    }

    @Test
    fun countdownBetweenWavesSurvives() {
        val m = Match(DeckCodec.DEFAULT, Random(1))
        m.update(1f)
        val restored = assertNotNull(Match.restore(m.snapshot()))
        assertEquals(m.waveCountdown, restored.waveCountdown)
    }

    @Test
    fun garbageIsRejected() {
        assertNull(Match.restore(""))
        assertNull(Match.restore("not a snapshot"))
        assertNull(Match.restore(midWave().snapshot().replace("sniper", "laser")))
        assertNull(Match.restore(midWave().snapshot().lines().dropLast(3).joinToString("\n") + "\nenemy=BOSS"))
    }
}
