package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchStatsTest {
    @Test
    fun countsKillsMergesAndPurchases() {
        val stats = MatchStats()
        stats.consume(
            listOf(
                GameEvent.Killed(EnemyKind.NORMAL, Vec2(0f, 0f)),
                GameEvent.Killed(EnemyKind.BOSS_REGEN, Vec2(0f, 0f)),
                GameEvent.Merged(Cell(0, 0)),
                GameEvent.DieBought(Cell(0, 1)),
                GameEvent.DieBought(Cell(0, 2)),
                GameEvent.PoweredUp(DieType.FIRE),
                GameEvent.Leaked(EnemyKind.FAST),
            ),
        )
        stats.consume(listOf(GameEvent.Killed(EnemyKind.FAST, Vec2(0f, 0f))))
        assertEquals(3, stats.kills)
        assertEquals(1, stats.bosses)
        assertEquals(1, stats.merges)
        assertEquals(2, stats.bought)
    }
}
