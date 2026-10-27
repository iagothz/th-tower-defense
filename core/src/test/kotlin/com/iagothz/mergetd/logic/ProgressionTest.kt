package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressionTest {
    @Test
    fun levelsCostMoreAsTheyGoUp() {
        assertEquals(1, Progression.level(0))
        assertEquals(1, Progression.level(Progression.costToNext(1) - 1))
        assertEquals(2, Progression.level(Progression.costToNext(1)))
        assertEquals(3, Progression.level(Progression.costToNext(1) + Progression.costToNext(2)))
        assertEquals(150, Progression.costToNext(1))
        assertEquals(200, Progression.costToNext(2))
    }

    @Test
    fun progressWithinTheCurrentLevelIsExposedForTheBar() {
        val xp = Progression.costToNext(1) + 40
        assertEquals(2, Progression.level(xp))
        assertEquals(40, Progression.intoLevel(xp))
        assertEquals(Progression.costToNext(2), Progression.costToNext(Progression.level(xp)))
    }

    @Test
    fun titlesFollowTheLevel() {
        assertEquals("Recruta", Progression.title(1))
        assertEquals("Recruta", Progression.title(2))
        assertEquals("Soldado", Progression.title(3))
        assertEquals("Veterano", Progression.title(5))
        assertEquals("Lenda", Progression.title(99))
    }

    @Test
    fun aMatchPaysByWaveAndBoss() {
        assertEquals(10 * Progression.XP_PER_WAVE, Progression.xpForMatch(wave = 10, bosses = 0))
        assertEquals(10 * Progression.XP_PER_WAVE + 2 * Progression.XP_PER_BOSS, Progression.xpForMatch(wave = 10, bosses = 2))
    }
}
