package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TipsTest {
    @Test
    fun everyKillerHasATipAndSoDoesNoKiller() {
        for (k in EnemyKind.entries) {
            val tip = Tips.forKiller(k)
            assertTrue(tip.isNotBlank() && tip.length < 160, "$k: $tip")
        }
        assertTrue(Tips.forKiller(null).isNotBlank())
        assertTrue("anque" in Tips.forKiller(EnemyKind.TANK))
    }

    @Test
    fun statsRememberWhoLeakedLast() {
        val stats = MatchStats()
        assertEquals(null, stats.killedBy)
        stats.consume(listOf(GameEvent.Leaked(EnemyKind.FAST), GameEvent.Leaked(EnemyKind.BOSS_THIEF)))
        assertEquals(EnemyKind.BOSS_THIEF, stats.killedBy)
    }
}
