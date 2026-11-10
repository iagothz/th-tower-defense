package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CatMetricsTest {
    private val text = """
        # comment
        ref=200
        foot=400
        scale=0.7
        1.w=250
        1.h=300
        1.foot=10
        3.w=400
        3.h=380
        3.foot=20
    """.trimIndent()

    @Test
    fun readsEveryLevel() {
        assertEquals(setOf(1, 3), CatMetrics.parse(text).pips)
    }

    @Test
    fun scalesFramesSoTheBareCatHasTheGivenHeight() {
        val f = CatMetrics.parse(text).frame(1, 100f)
        assertEquals(125f, f.width)
        assertEquals(150f, f.height)
        assertEquals(5f, f.foot)
    }

    @Test
    fun everyLevelSharesTheSameScale() {
        val m = CatMetrics.parse(text)
        assertEquals(m.frame(1, 100f).height / 300f, m.frame(3, 100f).height / 380f)
    }
}
