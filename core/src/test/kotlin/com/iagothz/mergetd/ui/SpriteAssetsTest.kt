package com.iagothz.mergetd.ui

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every pip level the renderer draws as a kitten must ship its sheets, cut into whole frames. */
class SpriteAssetsTest {
    private val dir = listOf(File("../assets/sprites"), File("assets/sprites")).first { it.isDirectory }

    private fun checkSheet(name: String, cols: Int, rows: Int, square: Boolean = false) {
        val f = File(dir, "$name.png")
        assertTrue(f.isFile, "missing ${f.path}")
        val img = ImageIO.read(f)
        assertEquals(0, img.width % cols, "$name width splits into $cols frames")
        assertEquals(0, img.height % rows, "$name height splits into $rows rows")
        if (square) assertEquals(img.width / cols, img.height / rows, "$name frames are square")
    }

    @Test
    fun everyKittenLevelHasItsSheets() {
        for (pips in 1..Sprites.CAT_PIPS) {
            checkSheet("fire_cat${pips}_idle", FireCats.IDLE_FRAMES, FireCats.ROWS.size)
            checkSheet("fire_cat${pips}_attack", FireCats.ATTACK_FRAMES, FireCats.ROWS.size)
        }
    }

    @Test
    fun everyKittenLevelHasMetricsMatchingItsFrames() {
        val m = CatMetrics.parse(File(dir, "fire_cats.properties").readText())
        for (pips in 1..Sprites.CAT_PIPS) {
            assertTrue(pips in m.pips, "fire_cats.properties lacks level $pips")
            val img = ImageIO.read(File(dir, "fire_cat${pips}_idle.png"))
            val f = m.frame(pips, 100f)
            val sheetAspect = (img.width / FireCats.IDLE_FRAMES).toFloat() / (img.height / FireCats.ROWS.size)
            assertEquals(f.width / f.height, sheetAspect, 0.02f, "level $pips frame aspect")
        }
    }

    @Test
    fun fireEffectsHaveTheirStrips() {
        checkSheet("fire_impact", FireCats.BOOM_FRAMES, 1, square = true)
        assertTrue(File(dir, "fireball.png").isFile)
    }
}
