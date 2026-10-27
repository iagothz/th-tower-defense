package com.iagothz.mergetd.ui

import kotlin.test.assertNull
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundPlannerTest {
    private val enemy = Enemy(EnemyKind.NORMAL, 10f)

    @Test
    fun mapsEachEventToItsSound() {
        val planner = SoundPlanner()
        val events = listOf(
            GameEvent.DieBought(Cell(0, 0)),
            GameEvent.Merged(Cell(0, 1), pips = 2),
            GameEvent.PoweredUp(DieType.FIRE),
            GameEvent.Hit(enemy, 3f),
            GameEvent.Killed(EnemyKind.NORMAL, Vec2(0f, 0f)),
            GameEvent.Leaked(EnemyKind.FAST),
            GameEvent.WaveStarted(2, boss = false),
        )
        assertEquals(setOf("buy", "merge_2", "power", "hit", "kill", "leak", "wave"), planner.plan(events, 0f))
    }

    @Test
    fun bossWaveUsesBossSound() {
        assertEquals(setOf("boss"), SoundPlanner().plan(listOf(GameEvent.WaveStarted(10, boss = true)), 0f))
    }

    @Test
    fun sameSoundPlaysOncePerFrame() {
        val kills = List(5) { GameEvent.Killed(EnemyKind.NORMAL, Vec2(0f, 0f)) }
        assertEquals(setOf("kill"), SoundPlanner().plan(kills, 0f))
    }

    @Test
    fun hitSoundIsThrottled() {
        val planner = SoundPlanner()
        val hit = listOf(GameEvent.Hit(enemy, 1f))
        assertEquals(setOf("hit"), planner.plan(hit, 1.0f))
        assertEquals(emptySet(), planner.plan(hit, 1.0f + SoundPlanner.HIT_GAP / 2))
        assertEquals(setOf("hit"), planner.plan(hit, 1.0f + SoundPlanner.HIT_GAP + 0.01f))
    }

    @Test
    fun hitsSoundLikeTheirDieFamily() {
        assertEquals(setOf("hit_fire"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.FIRE)), 0f))
        assertEquals(setOf("hit_ice"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.TIME)), 0f))
        assertEquals(setOf("hit_zap"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.ELECTRIC)), 0f))
        assertEquals(setOf("hit_heavy"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.IRON)), 0f))
        assertEquals(setOf("hit_wind"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.WIND)), 0f))
        assertEquals(setOf("hit_magic"), SoundPlanner().plan(listOf(GameEvent.Hit(enemy, 3f, type = DieType.CURSE)), 0f))
        for (t in DieType.entries.filter { it.attacks }) {
            assertTrue(SoundPlanner.family(t) in SoundPlanner.FAMILIES, "$t")
        }
    }

    @Test
    fun mergesRiseInPitchWithPipsAndSevenHasItsOwn() {
        assertEquals(setOf("merge_5"), SoundPlanner().plan(listOf(GameEvent.Merged(Cell(0, 0), pips = 5)), 0f))
        assertEquals(setOf("max"), SoundPlanner().plan(listOf(GameEvent.Merged(Cell(0, 0), pips = 7)), 0f))
        assertEquals(setOf("merge_2"), SoundPlanner().plan(listOf(GameEvent.Merged(Cell(0, 0))), 0f), "unknown pips sound like a small merge")
        for (p in 2..6) assertTrue("merge_$p" in SoundPlanner.MERGES)
        assertTrue("max" in SoundPlanner.MERGES)
    }

    @Test
    fun countdownTicksAtThreeTwoOne() {
        val p = SoundPlanner()
        assertNull(p.countdown(3.5f, 3.2f))
        assertEquals("tick_3", p.countdown(3.2f, 2.9f))
        assertNull(p.countdown(2.9f, 2.5f))
        assertEquals("tick_2", p.countdown(2.1f, 1.9f))
        assertEquals("tick_1", p.countdown(1.2f, 0.8f))
        assertNull(p.countdown(0.8f, 0f), "the wave sound takes over at zero")
        assertNull(p.countdown(0f, 0f))
        assertTrue(SoundPlanner.TICKS.containsAll(listOf("tick_1", "tick_2", "tick_3")))
    }
}
