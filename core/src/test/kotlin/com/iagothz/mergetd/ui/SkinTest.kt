package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import kotlin.math.sqrt
import kotlin.test.Test
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
        for (i in types.indices) for (j in i + 1 until types.size) {
            val d = dist(Skin.color(types[i]), Skin.color(types[j]))
            assertTrue(d >= 0.2f, "${types[i]} and ${types[j]} are too close: $d")
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
}
