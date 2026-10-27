package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {
    @Test
    fun walksBuyMergePowerThenSaysGoodbye() {
        val t = Tutorial()
        assertEquals(Tutorial.Step.BUY, t.step)
        t.consume(listOf(GameEvent.Killed(EnemyKind.NORMAL, Vec2(0f, 0f))))
        assertEquals(Tutorial.Step.BUY, t.step, "unrelated events don't advance it")
        t.consume(listOf(GameEvent.DieBought(Cell(0, 0))))
        assertEquals(Tutorial.Step.MERGE, t.step)
        t.consume(listOf(GameEvent.Merged(Cell(0, 0))))
        assertEquals(Tutorial.Step.POWER, t.step)
        t.consume(listOf(GameEvent.PoweredUp(DieType.FIRE)))
        assertEquals(Tutorial.Step.DONE, t.step)
        t.update(Tutorial.GOODBYE_SECONDS - 0.1f)
        assertEquals(Tutorial.Step.DONE, t.step)
        t.update(0.2f)
        assertNull(t.step)
        assertTrue(t.finished)
    }

    @Test
    fun skipEndsItAtOnce() {
        val t = Tutorial()
        t.skip()
        assertNull(t.step)
        assertTrue(t.finished)
        assertFalse(Tutorial(active = false).let { it.step != null })
    }

    @Test
    fun everyStepHasTextAndATarget() {
        for (s in Tutorial.Step.entries) assertTrue(s.text.isNotBlank(), "$s")
        assertEquals(Tutorial.Target.BUY, Tutorial.Step.BUY.target)
        assertEquals(Tutorial.Target.BOARD, Tutorial.Step.MERGE.target)
        assertEquals(Tutorial.Target.POWER, Tutorial.Step.POWER.target)
        assertNull(Tutorial.Step.DONE.target)
    }
}
