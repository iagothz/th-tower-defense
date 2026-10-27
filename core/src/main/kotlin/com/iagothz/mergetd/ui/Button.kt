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
    /** True while a finger is down on this button; it sinks and darkens. */
    var pressed = false

    fun contains(x: Float, y: Float): Boolean = enabled && rect.contains(x, y)

    private val press: Float get() = if (pressed && enabled) 1f else 0f

    fun drawShape(shapes: ShapeRenderer) {
        val base = if (enabled) color else DISABLED
        val inset = 3f * press
        val fill = if (press > 0f) tmp.set(base).mul(0.82f, 0.82f, 0.82f, 1f) else base
        shapes.bevelRect(rect.x + inset, rect.y + inset, rect.width - 2 * inset, rect.height - 2 * inset, 16f, fill, 5f - 3f * press)
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont) {
        font.color = if (enabled) Color.WHITE else Color.LIGHT_GRAY
        batch.textCentered(font, text, rect.x + rect.width / 2, rect.y + rect.height / 2 + 2f - 2f * press)
        font.color = Color.WHITE
    }

    companion object {
        private val DISABLED = Color(0.3f, 0.3f, 0.34f, 1f)
        private val tmp = Color()
    }
}
