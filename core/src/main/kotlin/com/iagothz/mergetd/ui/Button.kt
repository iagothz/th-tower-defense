package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class Button(
    val rect: Rectangle,
    var text: String,
    var enabled: Boolean = true,
    var color: Color = Color(0.25f, 0.45f, 0.85f, 1f),
) {
    fun contains(x: Float, y: Float): Boolean = enabled && rect.contains(x, y)

    fun drawShape(shapes: ShapeRenderer) {
        shapes.color = if (enabled) color else DISABLED
        shapes.roundedRect(rect.x, rect.y, rect.width, rect.height, 16f)
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont) {
        font.color = if (enabled) Color.WHITE else Color.LIGHT_GRAY
        batch.textCentered(font, text, rect.x + rect.width / 2, rect.y + rect.height / 2)
        font.color = Color.WHITE
    }

    companion object {
        private val DISABLED = Color(0.3f, 0.3f, 0.34f, 1f)
    }
}
