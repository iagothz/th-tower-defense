package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchDevTest {
    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    @Test
    fun nextWaveClearsEnemiesAndStartsTheNextOne() {
        val m = match()
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 100f))
        m.devNextWave()
        assertEquals(1, m.wave)
        assertTrue(m.enemies.isEmpty())
        m.devNextWave()
        assertEquals(2, m.wave)
    }

    @Test
    fun bossWaveJumpsToNextMultipleOfTen() {
        val m = match()
        m.devBossWave()
        assertEquals(10, m.wave)
        m.devBossWave()
        assertEquals(20, m.wave)
    }

    @Test
    fun killAllPaysRewards() {
        val m = match()
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 100f))
        m.debugAddEnemy(Enemy(EnemyKind.TANK, 100f))
        m.devKillAll()
        m.update(Balance.STEP)
        assertTrue(m.enemies.isEmpty())
        assertEquals(150 + 10 + 20, m.sp)
    }

    @Test
    fun spAndLivesCanBeAdded() {
        val m = match()
        m.devAddSp(500)
        m.devAddLife()
        assertEquals(650, m.sp)
        assertEquals(4, m.lives)
    }
}
