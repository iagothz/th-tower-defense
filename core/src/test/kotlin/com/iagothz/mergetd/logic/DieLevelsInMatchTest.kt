package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DieLevelsInMatchTest {
    private val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MINE)

    @Test
    fun aDieLevelMultipliesOnTopOfThePowerUp() {
        val m = Match(deck, Random(1), dieLevels = mapOf(DieType.FIRE to DieLevels.MAX))
        assertEquals(DieLevels.MAX, m.dieLevel(DieType.FIRE))
        assertEquals(1, m.dieLevel(DieType.ICE))
        assertEquals(1.4f, m.powerMult(DieType.FIRE), 1e-5f)
        assertEquals(1f, m.powerMult(DieType.ICE), 1e-5f)
        m.sp = 1000
        m.powerUp(DieType.FIRE)
        assertEquals(1.4f * Balance.powerMult(2), m.powerMult(DieType.FIRE), 1e-5f)
    }

    @Test
    fun theLevelReachesTheDamageDealt() {
        val plain = Match(deck, Random(1))
        val levelled = Match(deck, Random(1), dieLevels = mapOf(DieType.FIRE to 3))
        for (m in listOf(plain, levelled)) {
            m.board[Cell(0, 0)] = Die(DieType.FIRE, 3)
            m.debugStartWave(1)
            repeat(90) { m.update(Balance.STEP) }
        }
        val hitPlain = plain.drainEvents().filterIsInstance<GameEvent.Hit>().first { it.type == DieType.FIRE }.damage
        val hitLevelled = levelled.drainEvents().filterIsInstance<GameEvent.Hit>().first { it.type == DieType.FIRE }.damage
        assertEquals(hitPlain * DieLevels.mult(3), hitLevelled, 1e-3f)
    }

    @Test
    fun snapshotsKeepTheLevelsAndOldOnesReadAsLevelOne() {
        val m = Match(deck, Random(2), dieLevels = mapOf(DieType.ICE to 4, DieType.MINE to 2))
        val restored = assertNotNull(Match.restore(m.snapshot()))
        assertEquals(4, restored.dieLevel(DieType.ICE))
        assertEquals(2, restored.dieLevel(DieType.MINE))
        assertEquals(1, restored.dieLevel(DieType.FIRE))
        assertEquals(m.snapshot(), restored.snapshot())
        val old = Match(deck, Random(2)).snapshot().lines().filterNot { it.startsWith("levels=") }.joinToString("\n")
        assertEquals(1, assertNotNull(Match.restore(old)).dieLevel(DieType.ICE))
    }
}
