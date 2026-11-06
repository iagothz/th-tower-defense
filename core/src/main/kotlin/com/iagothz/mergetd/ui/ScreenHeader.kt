package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

/**
 * The top of every tab and subscreen: the title, an optional line under it, and on subscreens a back arrow in the
 * top-left corner. The screen owns the tap; it sets [pressed] while a finger is on the arrow.
 */
class ScreenHeader(val title: String, var subtitle: String? = null, val back: Boolean = false) {
    var pressed = false

    fun backHit(p: Vec2): Boolean = back && BACK_AREA.contains(p.x, p.y)

    fun drawShapes(shapes: ShapeRenderer) {
        if (!back) return
        shapes.color = if (pressed) tmp.set(Skin.PANEL).lerp(Color.BLACK, 0.18f) else Skin.PANEL
        shapes.circle(CX, CY, 30f, 28)
        shapes.color = Color.WHITE
        shapes.rectLine(72f, CY, 50f, CY, 5f)
        shapes.triangle(42f, CY, 54f, CY - 12f, 54f, CY + 12f)
    }

    fun drawText(batch: SpriteBatch, game: MergeTdGame) {
        batch.textShadowed(game.bigFont, title, 360f, CY)
        val sub = subtitle ?: return
        game.smallFont.color = Skin.MUTED
        batch.textCentered(game.smallFont, sub, 360f, 1176f)
        game.smallFont.color = Color.WHITE
    }

    private companion object {
        const val CX = 60f
        const val CY = 1222f
        val BACK_AREA = Rectangle(16f, 1178f, 88f, 88f)
        val tmp = Color()
    }
}
