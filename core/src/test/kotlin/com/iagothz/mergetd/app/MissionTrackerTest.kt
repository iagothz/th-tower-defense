package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind
import com.iagothz.mergetd.logic.Vec2
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class MissionTrackerTest {
    private val missions = listOf(
        Mission(MissionKind.KILLS_WITH, 30, 60, type = DieType.FIRE),
        Mission(MissionKind.MERGES, 20, 50),
        Mission(MissionKind.MAX_PIPS, 1, 90),
        Mission(MissionKind.BOSSES, 2, 70),
        Mission(MissionKind.REACH_WAVE, 10, 80, map = GameMap.DEFAULT),
        Mission(MissionKind.PLAY_MODE, 5, 70, mode = GameMode.RUSH),
    )

    @Test
    fun countsKillsMergesSevensAndBosses() {
        val match = Match(DeckCodec.DEFAULT, Random(1))
        val tracker = MissionTracker(missions, match)
        val at = Vec2(0f, 0f)
        tracker.consume(
            listOf(
                GameEvent.Killed(EnemyKind.NORMAL, at, by = DieType.FIRE),
                GameEvent.Killed(EnemyKind.NORMAL, at, by = DieType.ICE),
                GameEvent.Killed(EnemyKind.BOSS_REGEN, at, by = DieType.FIRE),
                GameEvent.Merged(Cell(0, 0), pips = 3),
                GameEvent.Merged(Cell(0, 1), pips = 7),
            ),
        )
        val p = tracker.progress()
        assertEquals(2, p[0].delta, "kills with Fire")
        assertEquals(2, p[1].delta, "merges")
        assertEquals(1, p[2].delta, "sevens")
        assertEquals(1, p[3].delta, "bosses")
    }

    @Test
    fun waveGoalsReportTheWaveReachedOnlyWhereTheyApply() {
        val onDefault = Match(DeckCodec.DEFAULT, Random(1), GameMap.DEFAULT, mode = GameMode.NORMAL)
        onDefault.devNextWave()
        val p = MissionTracker(missions, onDefault).progress()
        assertEquals(1, p[4].absolute, "wave reached on the right map")
        assertEquals(0, p[5].absolute, "wrong mode: nothing")

        val rush = Match(DeckCodec.DEFAULT, Random(1), GameMap.entries.last(), mode = GameMode.RUSH)
        rush.devNextWave()
        val q = MissionTracker(missions, rush).progress()
        assertEquals(0, q[4].absolute)
        assertEquals(1, q[5].absolute)
    }
}
