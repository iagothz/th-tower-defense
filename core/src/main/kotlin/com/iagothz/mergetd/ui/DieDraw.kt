package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType

private val base = Color()
private val ink = Color()

private val C = 0 to 0
private val TL = -1 to 1
private val TR = 1 to 1
private val BL = -1 to -1
private val BR = 1 to -1
private val ML = -1 to 0
private val MR = 1 to 0

/** Pip offsets (as fractions of a quarter die) for 1..7 pips. */
private val PIPS = listOf(
    listOf(C),
    listOf(TL, BR),
    listOf(TL, C, BR),
    listOf(TL, TR, BL, BR),
    listOf(TL, TR, C, BL, BR),
    listOf(TL, TR, ML, MR, BL, BR),
    listOf(TL, TR, ML, C, MR, BL, BR),
)

/**
 * A die of [type] with [pips], [size] wide and centred on (cx, cy): bevelled body, glyph watermark, pips.
 * [brightness] below 1 dims it (silenced dice, drag ghosts); [alpha] fades it (menu decoration).
 */
fun ShapeRenderer.die(type: DieType, pips: Int, cx: Float, cy: Float, size: Float, brightness: Float = 1f, alpha: Float = 1f) {
    val half = size / 2
    val lip = size * 0.052f
    if (pips >= Balance.MAX_PIPS) {
        color = ink.set(Skin.GOLD).also { it.a = alpha }
        roundedRect(cx - half - 4f, cy - half - 4f, size + 8f, size + 8f, size * 0.19f + 4f)
    }
    base.set(Skin.color(type)).mul(brightness, brightness, brightness, 1f).also { it.a = alpha }
    bevelRect(cx - half, cy - half, size, size, size * 0.19f, base, lip)
    // The body sits on top of the lip, so everything on its face is centred a little higher.
    val fy = cy + lip / 2
    ink.set(Skin.glyphColor(type)).mul(brightness, brightness, brightness, 1f).also { it.a = alpha }
    glyph(type, cx, fy, half * 0.62f, ink, base)
    color = ink.set(Skin.pipColor(type)).mul(brightness, brightness, brightness, 1f).also { it.a = alpha }
    val step = size * 0.25f
    val r = size * 0.0885f
    for ((dx, dy) in PIPS[pips - 1]) pip(cx + dx * step, fy + dy * step, r)
}

/** One pip in the equipped shape. */
private fun ShapeRenderer.pip(x: Float, y: Float, r: Float) {
    when (Cosmetics.pips) {
        Cosmetic.PIPS_SQUARE -> rect(x - r * 0.85f, y - r * 0.85f, r * 1.7f, r * 1.7f)
        Cosmetic.PIPS_DIAMOND -> polygon(x, y, r * 1.15f, 4, 0.785f)
        Cosmetic.PIPS_STAR -> {
            polygon(x, y, r * 1.1f, 3, 1.571f)
            polygon(x, y, r * 1.1f, 3, -1.571f)
        }
        else -> circle(x, y, r, 14)
    }
}
