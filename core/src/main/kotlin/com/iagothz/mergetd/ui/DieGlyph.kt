package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.DieType
import kotlin.math.cos
import kotlin.math.sin

/**
 * One icon per die type, built from filled primitives in a unit square (-1..1, y up).
 * [cut] shapes are drawn in the background colour, which is how rings, eyes and cracks are made.
 */
sealed class GlyphShape(val cut: Boolean) {
    class Circle(val x: Float, val y: Float, val r: Float, cut: Boolean = false) : GlyphShape(cut)
    class Tri(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float, cut: Boolean = false,
    ) : GlyphShape(cut)
    class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val w: Float, cut: Boolean = false) :
        GlyphShape(cut)
}

object DieGlyph {
    private fun c(x: Float, y: Float, r: Float, cut: Boolean = false) = GlyphShape.Circle(x, y, r, cut)
    private fun t(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, cut: Boolean = false) =
        GlyphShape.Tri(x1, y1, x2, y2, x3, y3, cut)
    private fun l(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, cut: Boolean = false) =
        GlyphShape.Line(x1, y1, x2, y2, w, cut)

    private fun rays(n: Int, from: Float, to: Float, w: Float): List<GlyphShape> = List(n) { i ->
        val a = i * 2f * Math.PI.toFloat() / n
        l(cos(a) * from, sin(a) * from, cos(a) * to, sin(a) * to, w)
    }

    private val glyphs: Map<DieType, List<GlyphShape>> = mapOf(
        DieType.FIRE to listOf(t(-0.65f, -0.3f, 0.65f, -0.3f, 0f, 1f), c(0f, -0.4f, 0.6f), c(0.05f, -0.45f, 0.22f, cut = true)),
        DieType.ICE to listOf(
            l(-1f, 0f, 1f, 0f, 0.2f), l(-0.5f, -0.87f, 0.5f, 0.87f, 0.2f), l(-0.5f, 0.87f, 0.5f, -0.87f, 0.2f),
            c(0f, 0f, 0.24f),
        ),
        DieType.POISON to listOf(c(-0.38f, -0.35f, 0.5f), c(0.42f, -0.2f, 0.36f), c(0.08f, 0.5f, 0.4f), c(0.2f, 0.62f, 0.12f, cut = true)),
        DieType.ELECTRIC to listOf(t(0.25f, 1f, -0.65f, -0.05f, 0.15f, -0.05f), t(-0.25f, -1f, 0.65f, 0.05f, -0.15f, 0.05f)),
        DieType.WIND to listOf(
            l(-0.9f, 0.5f, 0.45f, 0.5f, 0.18f), c(0.5f, 0.68f, 0.18f),
            l(-0.9f, 0f, 0.9f, 0f, 0.18f),
            l(-0.9f, -0.5f, 0.25f, -0.5f, 0.18f), c(0.3f, -0.32f, 0.18f),
        ),
        DieType.IRON to listOf(l(-0.8f, 0.55f, 0.8f, 0.55f, 0.5f), t(-0.8f, 0.3f, 0.8f, 0.3f, 0f, -1f), l(0f, 0.75f, 0f, -0.6f, 0.1f, cut = true)),
        DieType.MINE to listOf(
            t(-0.9f, 0.3f, 0.9f, 0.3f, 0f, -1f), t(-0.9f, 0.3f, -0.5f, 0.8f, 0.5f, 0.8f), t(-0.9f, 0.3f, 0.5f, 0.8f, 0.9f, 0.3f),
            l(-0.9f, 0.3f, 0.9f, 0.3f, 0.07f, cut = true), l(-0.5f, 0.8f, 0f, -1f, 0.07f, cut = true),
        ),
        DieType.SNIPER to listOf(
            c(0f, 0f, 0.9f), c(0f, 0f, 0.65f, cut = true), l(-1f, 0f, 1f, 0f, 0.14f), l(0f, -1f, 0f, 1f, 0.14f), c(0f, 0f, 0.2f),
        ),
        DieType.MIRROR to listOf(t(-0.95f, 0f, -0.15f, 0.8f, -0.15f, -0.8f), t(0.95f, 0f, 0.15f, 0.8f, 0.15f, -0.8f), l(0f, -1f, 0f, 1f, 0.1f)),
        DieType.GROWTH to listOf(l(0f, -0.9f, 0f, 0.3f, 0.22f), t(-0.7f, 0.2f, 0.7f, 0.2f, 0f, 1f), l(-0.6f, -0.9f, 0.6f, -0.9f, 0.18f)),
        DieType.LIGHT to listOf(c(0f, 0f, 0.45f)) + rays(8, 0.62f, 1f, 0.16f),
        DieType.CRIT to listOf(
            t(-1f, 0f, 0f, 0.25f, 0f, -0.25f), t(1f, 0f, 0f, 0.25f, 0f, -0.25f),
            t(0f, 1f, -0.25f, 0f, 0.25f, 0f), t(0f, -1f, -0.25f, 0f, 0.25f, 0f),
            t(0.4f, 0.7f, 0.7f, 0.8f, 0.7f, 0.6f), t(1f, 0.7f, 0.7f, 0.8f, 0.7f, 0.6f),
            t(0.7f, 1f, 0.6f, 0.7f, 0.8f, 0.7f), t(0.7f, 0.4f, 0.6f, 0.7f, 0.8f, 0.7f),
        ),
        DieType.COMBO to listOf(c(-0.4f, 0f, 0.55f), c(-0.4f, 0f, 0.32f, cut = true), c(0.4f, 0f, 0.55f), c(0.4f, 0f, 0.32f, cut = true)),
        DieType.CURSE to listOf(c(0f, 0f, 0.9f), c(0.38f, 0.25f, 0.75f, cut = true), c(0.6f, -0.5f, 0.15f)),
        DieType.HUNTER to listOf(l(-0.8f, 0.9f, -0.3f, -0.9f, 0.2f), l(-0.25f, 0.95f, 0.2f, -0.95f, 0.2f), l(0.3f, 0.9f, 0.75f, -0.9f, 0.2f)),
        DieType.GRAVITY to listOf(l(0f, 0.9f, 0f, -0.1f, 0.22f), t(-0.6f, 0f, 0.6f, 0f, 0f, -0.7f), l(-0.8f, -0.9f, 0.8f, -0.9f, 0.18f)),
        DieType.TIME to listOf(
            t(-0.7f, 0.9f, 0.7f, 0.9f, 0f, 0.05f), t(-0.7f, -0.9f, 0.7f, -0.9f, 0f, -0.05f),
            l(-0.8f, 0.95f, 0.8f, 0.95f, 0.14f), l(-0.8f, -0.95f, 0.8f, -0.95f, 0.14f),
            t(-0.4f, 0.8f, 0.4f, 0.8f, 0f, 0.35f, cut = true),
        ),
        DieType.TRAP to listOf(
            l(-1f, -0.7f, 1f, -0.7f, 0.3f),
            t(-0.9f, -0.55f, -0.4f, -0.55f, -0.65f, 0.7f), t(-0.25f, -0.55f, 0.25f, -0.55f, 0f, 0.9f), t(0.4f, -0.55f, 0.9f, -0.55f, 0.65f, 0.7f),
        ),
        DieType.QUAKE to listOf(
            t(-1f, -0.6f, 0.1f, -0.6f, -0.45f, 0.5f), t(-0.2f, -0.6f, 1f, -0.6f, 0.4f, 0.85f),
            l(0.4f, 0.85f, 0.2f, 0.2f, 0.09f, cut = true), l(0.2f, 0.2f, 0.45f, -0.2f, 0.09f, cut = true), l(0.45f, -0.2f, 0.3f, -0.6f, 0.09f, cut = true),
        ),
        DieType.REAPER to listOf(
            c(0f, 0.2f, 0.7f), l(-0.38f, -0.45f, 0.38f, -0.45f, 0.5f),
            c(-0.27f, 0.3f, 0.19f, cut = true), c(0.27f, 0.3f, 0.19f, cut = true), t(-0.1f, -0.1f, 0.1f, -0.1f, 0f, 0.1f, cut = true),
            l(-0.15f, -0.72f, -0.15f, -0.5f, 0.07f, cut = true), l(0.15f, -0.72f, 0.15f, -0.5f, 0.07f, cut = true),
        ),
        DieType.NOVA to rays(8, 0f, 1f, 0.16f) + listOf(c(0f, 0f, 0.35f)),
        DieType.LUCK to listOf(c(-0.35f, 0.35f, 0.4f), c(0.35f, 0.35f, 0.4f), c(-0.35f, -0.35f, 0.4f), c(0.35f, -0.35f, 0.4f), l(0f, -0.3f, 0.3f, -1f, 0.14f)),
        DieType.VAMPIRE to listOf(l(-0.8f, 0.6f, 0.8f, 0.6f, 0.35f), t(-0.7f, 0.45f, -0.2f, 0.45f, -0.45f, -0.8f), t(0.2f, 0.45f, 0.7f, 0.45f, 0.45f, -0.8f)),
    )

    fun of(t: DieType): List<GlyphShape> = glyphs.getValue(t)
}

/** Draws [type]'s glyph centred on (cx, cy), spanning ±[s]; call inside a Filled batch. */
fun ShapeRenderer.glyph(type: DieType, cx: Float, cy: Float, s: Float, ink: Color, background: Color) {
    for (g in DieGlyph.of(type)) {
        color = if (g.cut) background else ink
        when (g) {
            is GlyphShape.Circle -> circle(cx + g.x * s, cy + g.y * s, g.r * s, 20)
            is GlyphShape.Tri -> triangle(cx + g.x1 * s, cy + g.y1 * s, cx + g.x2 * s, cy + g.y2 * s, cx + g.x3 * s, cy + g.y3 * s)
            is GlyphShape.Line -> rectLine(cx + g.x1 * s, cy + g.y1 * s, cx + g.x2 * s, cy + g.y2 * s, g.w * s)
        }
    }
}
