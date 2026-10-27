package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MissionsTest {
    @Test
    fun aDayAlwaysGetsTheSameThreeMissionsOfDifferentKinds() {
        val a = DailyMissions.of(20_000)
        val b = DailyMissions.of(20_000)
        assertEquals(a, b)
        assertEquals(DailyMissions.COUNT, a.size)
        assertEquals(DailyMissions.COUNT, a.map { it.kind }.toSet().size)
        assertTrue(a.all { it.target > 0 && it.xp > 0 }, "$a")
    }

    @Test
    fun daysDiffer() {
        val days = (20_000 until 20_010).map { DailyMissions.of(it) }
        assertTrue(days.toSet().size > 5, "$days")
        assertNotEquals(DailyMissions.of(20_000), DailyMissions.of(20_001))
    }

    @Test
    fun missionParametersFitTheirKind() {
        for (day in 20_000 until 20_200) {
            for (m in DailyMissions.of(day)) {
                when (m.kind) {
                    MissionKind.KILLS_WITH -> {
                        assertNotNull(m.type, "$m")
                        assertTrue(m.type!!.attacks && !m.type.wildcard && m.type != DieType.CLONE, "$m")
                    }
                    MissionKind.REACH_WAVE -> assertNotNull(m.map, "$m")
                    MissionKind.PLAY_MODE -> assertTrue(m.mode != null && m.mode != GameMode.NORMAL, "$m")
                    else -> Unit
                }
                assertTrue(m.template.contains("%d") || m.kind == MissionKind.MAX_PIPS, m.template)
            }
        }
    }
}
