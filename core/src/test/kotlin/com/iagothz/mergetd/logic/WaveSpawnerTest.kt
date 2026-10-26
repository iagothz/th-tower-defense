package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveSpawnerTest {
    @Test
    fun waveSize() {
        assertEquals(10, WaveSpawner.waveSize(1))
        assertEquals(18, WaveSpawner.waveSize(5))
        assertEquals(40, WaveSpawner.waveSize(16))
        assertEquals(40, WaveSpawner.waveSize(50))
    }

    @Test
    fun earlyWavesOnlyNormal() {
        assertTrue(WaveSpawner.composition(1).all { it == EnemyKind.NORMAL })
        val w3 = WaveSpawner.composition(3)
        assertTrue(EnemyKind.FAST in w3)
        assertFalse(EnemyKind.TANK in w3)
        assertTrue(EnemyKind.TANK in WaveSpawner.composition(5))
        assertEquals(WaveSpawner.waveSize(7), WaveSpawner.composition(7).size)
    }

    @Test
    fun bossWaves() {
        assertEquals(listOf(EnemyKind.BOSS_SILENCER), WaveSpawner.composition(10))
        assertEquals(listOf(EnemyKind.BOSS_SWAPPER), WaveSpawner.composition(20))
        assertEquals(listOf(EnemyKind.BOSS_REGEN), WaveSpawner.composition(30))
        assertEquals(listOf(EnemyKind.BOSS_SILENCER), WaveSpawner.composition(40))
    }

    @Test
    fun hpScaling() {
        assertEquals(100f, WaveSpawner.enemyHp(1, EnemyKind.NORMAL), 0.01f)
        assertEquals(109f, WaveSpawner.enemyHp(2, EnemyKind.NORMAL), 0.01f)
        assertEquals(
            40 * WaveSpawner.enemyHp(10, EnemyKind.NORMAL),
            WaveSpawner.enemyHp(10, EnemyKind.BOSS_SILENCER),
            0.01f,
        )
    }
}
