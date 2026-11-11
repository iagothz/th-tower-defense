package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

/**
 * A settings row: its name on the left, an on/off switch on the right; tapping anywhere on the row flips it.
 * The knob slides over [SLIDE] seconds, or jumps when [instant] says motion is reduced.
 */
class Toggle(val rect: Rectangle, val label: String, on: Boolean, private val instant: () -> Boolean = { false }) {
    var on = on
        private set

    /** 0 off, 1 on; follows [on]. */
    var knob = if (on) 1f else 0f
        private set

    fun toggle() {
        on = !on
    }

    fun update(dt: Float) {
        val target = if (on) 1f else 0f
        knob = if (instant()) {
            target
        } else if (knob < target) {
            minOf(target, knob + dt / SLIDE)
        } else {
            maxOf(target, knob - dt / SLIDE)
        }
    }

    fun contains(x: Float, y: Float): Boolean = rect.contains(x, y)

    fun drawShapes(shapes: ShapeRenderer) {
        update(Gdx.graphics.deltaTime)
        shapes.color = Skin.PANEL
        shapes.roundedRect(rect.x, rect.y, rect.width, rect.height, 16f)
        val tx = rect.x + rect.width - 24f - TRACK_W
        val ty = rect.y + (rect.height - TRACK_H) / 2
        shapes.color = tmp.set(Skin.CELL).lerp(Skin.GOLD, knob)
        shapes.roundedRect(tx, ty, TRACK_W, TRACK_H, TRACK_H / 2)
        shapes.color = Color.WHITE
        shapes.circle(tx + 20f + 36f * knob, ty + TRACK_H / 2, 16f, 24)
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont) {
        batch.textLeft(font, label, rect.x + 24f, rect.y + rect.height / 2)
    }

    companion object {
        const val SLIDE = 0.15f
        private const val TRACK_W = 76f
        private const val TRACK_H = 40f
        private val tmp = Color()
    }
}
