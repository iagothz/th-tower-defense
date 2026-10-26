package com.iagothz.mergetd.sim

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private val waves = (1..10).toList()

    @Test
    fun nearestRankPercentiles() {
        assertEquals(1, Stats.percentile(waves, 10))
        assertEquals(5, Stats.percentile(waves, 50))
        assertEquals(9, Stats.percentile(waves, 90))
        assertEquals(10, Stats.percentile(waves, 100))
    }

    @Test
    fun deathRateCountsGamesEndingAtOrBeforeAWave() {
        assertEquals(0.3, Stats.deathRateBy(waves, 3), 1e-9)
        assertEquals(0.0, Stats.deathRateBy(waves, 0), 1e-9)
    }
}
