package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.Synergy
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkinTest {
    private fun dist(a: Color, b: Color): Float {
        val dr = a.r - b.r
        val dg = a.g - b.g
        val db = a.b - b.b
        return sqrt(dr * dr + dg * dg + db * db)
    }

    @Test
    fun dieColorsAreTellableApart() {
        val types = DieType.entries
        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val d = dist(Skin.color(types[i]), Skin.color(types[j]))
                assertTrue(d >= 0.2f, "${types[i]} and ${types[j]} are too close: $d")
            }
        }
    }

    @Test
    fun dieColorsStandOutFromEmptyCells() {
        for (t in DieType.entries) {
            assertTrue(dist(Skin.color(t), Skin.CELL) >= 0.35f, "$t fades into an empty cell")
            assertTrue(dist(Skin.color(t), Skin.PANEL) >= 0.35f, "$t fades into the panel")
        }
    }

    @Test
    fun everyDieHasAGlyph() {
        for (t in DieType.entries) assertTrue(DieGlyph.of(t).isNotEmpty(), "$t has no glyph")
    }

    @Test
    fun theFireDieIsCalledIgnisInEveryLanguage() {
        val before = S.language
        try {
            for (lang in listOf(Language.PT, Language.EN)) {
                S.language = lang
                assertEquals("Ignis", Skin.label(DieType.FIRE), "$lang")
                assertEquals("Ignis", Skin.fullName(DieType.FIRE), "$lang")
                val texts = EnemyKind.entries.map { Tips.forKiller(it) } + Synergy.entries.map { S[it.description] }
                for (text in texts) assertFalse(Regex("\\b(Fogo|Fire)\\b").containsMatchIn(text), "$lang: $text")
            }
        } finally {
            S.language = before
        }
    }

    @Test
    fun everyEnemyKindHasAShape() {
        for (k in EnemyKind.entries) {
            assertTrue(Skin.enemySides(k) >= 3, "$k")
            assertTrue(Skin.enemyRadius(k) > 0f, "$k")
        }
    }

    @Test
    fun everyBossExplainsItself() {
        for (k in EnemyKind.entries.filter { it.isBoss }) assertTrue(Skin.bossAbility(k).isNotBlank(), "$k")
    }

    @Test
    fun inkOnPicksWhiteOnDarkFills() {
        for (c in listOf(Skin.PANEL, Skin.PRIMARY, Skin.DANGER)) assertEquals(Color.WHITE, Skin.inkOn(c), "$c")
    }

    @Test
    fun inkOnPicksDarkOnLightFills() {
        for (c in listOf(Skin.GOLD, Color.WHITE, Cosmetic.FRAME_GOLD.color, Cosmetic.FRAME_SILVER.color, Cosmetic.FRAME_DIAMOND.color)) {
            assertEquals(Skin.INK_DARK, Skin.inkOn(c), "$c")
        }
    }

    @Test
    fun inkOnSwitchesAroundTheThreshold() {
        assertEquals(Skin.INK_DARK, Skin.inkOn(Color(0.56f, 0.56f, 0.56f, 1f)))
        assertEquals(Color.WHITE, Skin.inkOn(Color(0.54f, 0.54f, 0.54f, 1f)))
    }

    @Test
    fun inkOnClampsTintsAboveOne() {
        for (c in listOf(Cosmetic.SKY_DUSK, Cosmetic.SKY_AURORA, Cosmetic.SKY_NEBULA, Cosmetic.SKY_GOLDEN)) {
            assertEquals(Skin.INK_DARK, Skin.inkOn(c.color), "$c")
        }
    }
}
