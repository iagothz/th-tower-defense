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
        assertTrue(EnemyKind.TANK in WaveSpawner.composition(6))
        assertEquals(WaveSpawner.waveSize(7), WaveSpawner.composition(7).size)
    }

    @Test
    fun bossWavesFollowTheGivenOrderAndWrap() {
        val order = listOf(EnemyKind.BOSS_THIEF, EnemyKind.BOSS_REGEN, EnemyKind.BOSS_SPLITTER)
        assertEquals(listOf(EnemyKind.BOSS_THIEF), WaveSpawner.composition(10, order))
        assertEquals(listOf(EnemyKind.BOSS_REGEN), WaveSpawner.composition(20, order))
        assertEquals(listOf(EnemyKind.BOSS_SPLITTER), WaveSpawner.composition(30, order))
        assertEquals(listOf(EnemyKind.BOSS_THIEF), WaveSpawner.composition(40, order))
        assertEquals(listOf(EnemyKind.BOSS_SILENCER), WaveSpawner.composition(10))
        assertEquals(7, WaveSpawner.DEFAULT_BOSS_ORDER.size)
    }

    @Test
    fun healersAndGhostsJoinLater() {
        assertFalse(EnemyKind.HEALER in WaveSpawner.composition(7))
        assertTrue(EnemyKind.HEALER in WaveSpawner.composition(8))
        assertFalse(EnemyKind.GHOST in WaveSpawner.composition(11))
        assertTrue(EnemyKind.GHOST in WaveSpawner.composition(12))
        assertTrue(WaveSpawner.composition(13).count { it == EnemyKind.NORMAL } >= WaveSpawner.waveSize(13) * 0.4f)
    }

    /** Waves 5, 15, 25... rotate Horde, Tanks, Rush; a horde's soldiers have half HP. */
    @Test
    fun everyFifthWaveIsSpecial() {
        assertEquals(null, WaveSpawner.special(4))
        assertEquals(SpecialWave.HORDE, WaveSpawner.special(5))
        assertEquals(null, WaveSpawner.special(10))
        assertEquals(SpecialWave.TANKS, WaveSpawner.special(15))
        assertEquals(SpecialWave.RUSH, WaveSpawner.special(25))
        assertEquals(SpecialWave.HORDE, WaveSpawner.special(35))
        assertEquals(List(40) { EnemyKind.NORMAL }, WaveSpawner.composition(5))
        assertEquals(0.5f, WaveSpawner.hpScale(5))
        assertEquals(1f, WaveSpawner.hpScale(6))
        assertTrue(WaveSpawner.composition(15).all { it == EnemyKind.TANK })
        assertEquals(WaveSpawner.waveSize(15) / 3, WaveSpawner.composition(15).size)
        assertTrue(WaveSpawner.composition(25).all { it == EnemyKind.FAST })
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
