package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.cos
import kotlin.math.sin

private val layout = GlyphLayout()
private val tmp = Color()

/** Filled rounded rectangle; call inside a Filled ShapeRenderer batch. */
fun ShapeRenderer.roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    rect(x + r, y, w - 2 * r, h)
    rect(x, y + r, r, h - 2 * r)
    rect(x + w - r, y + r, r, h - 2 * r)
    circle(x + r, y + r, r, 20)
    circle(x + w - r, y + r, r, 20)
    circle(x + r, y + h - r, r, 20)
    circle(x + w - r, y + h - r, r, 20)
}

/**
 * Rounded rectangle with a drop shadow, a darker bottom lip and a light top edge, in [base].
 * Everything in the game that should look like a solid object (dice, buttons, tiles) uses this.
 */
fun ShapeRenderer.bevelRect(x: Float, y: Float, w: Float, h: Float, r: Float, base: Color, lip: Float = 5f) {
    color = Skin.SHADOW
    roundedRect(x + 2f, y - 5f, w, h, r)
    color = tmp.set(base).mul(0.62f, 0.62f, 0.62f, 1f)
    roundedRect(x, y, w, h, r)
    color = base
    roundedRect(x, y + lip, w, h - lip, r)
    color = tmp.set(base).lerp(Color.WHITE, 0.22f)
    rect(x + r, y + h - 4f, w - 2 * r, 3f)
}

/** Regular polygon with [sides] sides, one vertex pointing at [angle] (radians). */
fun ShapeRenderer.polygon(cx: Float, cy: Float, r: Float, sides: Int, angle: Float = 0f) {
    val step = 2f * Math.PI.toFloat() / sides
    var ax = cx + cos(angle) * r
    var ay = cy + sin(angle) * r
    for (i in 1..sides) {
        val a = angle + step * i
        val bx = cx + cos(a) * r
        val by = cy + sin(a) * r
        triangle(cx, cy, ax, ay, bx, by)
        ax = bx
        ay = by
    }
}

/** Circle outline of the given [thickness], as a polygon of short line segments. */
fun ShapeRenderer.ring(cx: Float, cy: Float, r: Float, thickness: Float, segments: Int = 32) {
    val step = 2f * Math.PI.toFloat() / segments
    var ax = cx + r
    var ay = cy
    for (i in 1..segments) {
        val bx = cx + cos(step * i) * r
        val by = cy + sin(step * i) * r
        rectLine(ax, ay, bx, by, thickness)
        ax = bx
        ay = by
    }
}

fun ShapeRenderer.heart(cx: Float, cy: Float, s: Float) {
    circle(cx - s * 0.5f, cy + s * 0.3f, s * 0.52f, 16)
    circle(cx + s * 0.5f, cy + s * 0.3f, s * 0.52f, 16)
    triangle(cx - s * 1f, cy + s * 0.1f, cx + s * 1f, cy + s * 0.1f, cx, cy - s * 1f)
}

fun ShapeRenderer.coin(cx: Float, cy: Float, r: Float) {
    color = tmp.set(Skin.GOLD).mul(0.7f, 0.7f, 0.7f, 1f)
    circle(cx, cy, r, 20)
    color = Skin.GOLD
    circle(cx, cy, r * 0.75f, 20)
    color = tmp.set(Skin.GOLD).mul(0.7f, 0.7f, 0.7f, 1f)
    rectLine(cx - r * 0.1f, cy - r * 0.4f, cx - r * 0.1f, cy + r * 0.4f, r * 0.2f)
    rectLine(cx - r * 0.35f, cy + r * 0.35f, cx + r * 0.2f, cy + r * 0.35f, r * 0.2f)
    rectLine(cx - r * 0.35f, cy - r * 0.35f, cx + r * 0.2f, cy - r * 0.35f, r * 0.2f)
}

/** Draws [text] centred on (x, y). */
fun SpriteBatch.textCentered(font: BitmapFont, text: String, x: Float, y: Float) {
    layout.setText(font, text)
    font.draw(this, text, x - layout.width / 2, y + layout.height / 2)
}

/** Draws [text] with its left edge at [x], vertically centred on [y]. */
fun SpriteBatch.textLeft(font: BitmapFont, text: String, x: Float, y: Float) {
    layout.setText(font, text)
    font.draw(this, text, x, y + layout.height / 2)
}

/** Draws [text] with its right edge at [x], vertically centred on [y]. */
fun SpriteBatch.textRight(font: BitmapFont, text: String, x: Float, y: Float) {
    layout.setText(font, text)
    font.draw(this, text, x - layout.width, y + layout.height / 2)
}

/** [text] with a dark offset copy underneath, for text drawn over the board. */
fun SpriteBatch.textShadowed(font: BitmapFont, text: String, x: Float, y: Float, ink: Color = Color.WHITE) {
    val a = ink.a
    font.setColor(0f, 0f, 0f, 0.6f * a)
    textCentered(font, text, x + 2f, y - 2f)
    font.color = ink
    textCentered(font, text, x, y)
    font.color = Color.WHITE
}
