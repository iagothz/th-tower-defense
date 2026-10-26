package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

private val layout = GlyphLayout()

/** Filled rounded rectangle; call inside a Filled ShapeRenderer batch. */
fun ShapeRenderer.roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    rect(x + r, y, w - 2 * r, h)
    rect(x, y + r, r, h - 2 * r)
    rect(x + w - r, y + r, r, h - 2 * r)
    circle(x + r, y + r, r, 16)
    circle(x + w - r, y + r, r, 16)
    circle(x + r, y + h - r, r, 16)
    circle(x + w - r, y + h - r, r, 16)
}

/** Draws [text] centred on (x, y). */
fun SpriteBatch.textCentered(font: BitmapFont, text: String, x: Float, y: Float) {
    layout.setText(font, text)
    font.draw(this, text, x - layout.width / 2, y + layout.height / 2)
}
