package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class WavePreviewTest {
    @Test
    fun wavesUntilBossCountsFromTheCurrentWave() {
        assertEquals(10, WaveSpawner.wavesUntilBoss(0))
        assertEquals(3, WaveSpawner.wavesUntilBoss(7))
        assertEquals(1, WaveSpawner.wavesUntilBoss(9))
        assertEquals(10, WaveSpawner.wavesUntilBoss(10))
    }

    /** The preview is the real composition, counted per kind in spawn order of first appearance. */
    @Test
    fun previewCountsTheNextWave() {
        assertEquals(linkedMapOf(EnemyKind.NORMAL to 10), WaveSpawner.preview(1))
        val w7 = WaveSpawner.preview(7)
        assertEquals(WaveSpawner.composition(7).size, w7.values.sum())
        assertEquals(WaveSpawner.composition(7).count { it == EnemyKind.TANK }, w7[EnemyKind.TANK])
        assertEquals(listOf(EnemyKind.NORMAL, EnemyKind.FAST, EnemyKind.TANK), w7.keys.toList())
        assertEquals(mapOf(EnemyKind.BOSS_SWAPPER to 1), WaveSpawner.preview(20))
    }
}
