package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

/**
 * How a die's numbers look wherever they show (deck panel, dice page, die window): three blocks for damage, speed
 * and target, each with its icon and colour, and coloured labels for the detail rows. The font has no arrow
 * glyph, so "→" in a text leaves a gap that [drawArrows] fills once the batch has ended.
 */
class DieStatsView(private val game: MergeTdGame) {
    private class Arrow(val x: Float, val y: Float, val size: Float, val color: Color)
    private val arrows = mutableListOf<Arrow>()
    private val measure = GlyphLayout()

    /** Block [i] of three across [width] starting at [x], hanging down from [top]. */
    fun rect(i: Int, x: Float, width: Float, top: Float): Rectangle {
        val w = (width - 2 * GAP) / 3
        return Rectangle(x + i * (w + GAP), top - HEIGHT, w, HEIGHT)
    }

    fun drawShapes(info: DieInfo, x: Float, width: Float, top: Float) {
        val shapes = game.shapes
        for ((i, s) in info.stats.withIndex()) {
            val r = rect(i, x, width, top)
            shapes.color = Skin.CELL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 14f)
            shapes.color = color(s.kind)
            shapes.roundedRect(r.x, r.y + r.height - 5f, r.width, 5f, 2.5f)
            icon(shapes, s.kind, r.x + 24f, r.y + r.height - 28f)
        }
    }

    fun drawText(info: DieInfo, x: Float, width: Float, top: Float) {
        for ((i, s) in info.stats.withIndex()) {
            val r = rect(i, x, width, top)
            game.tinyFont.color = color(s.kind)
            game.batch.textLeft(game.tinyFont, s.label.uppercase(), r.x + 46f, r.y + r.height - 28f)
            textWithArrow(game.smallFont, s.value, r.x + 14f, r.y + 38f, Color.WHITE)
            textWithArrow(game.tinyFont, s.note, r.x + 14f, r.y + 14f, KEY_COLOR)
        }
        game.tinyFont.color = Color.WHITE
    }

    /** Left-aligned text vertically centred on [y]; returns where it ends. Each "→" leaves a gap for an arrow. */
    fun textWithArrow(font: BitmapFont, text: String, x: Float, y: Float, color: Color): Float {
        val parts = text.split("→")
        val arrowW = font.capHeight * 1.6f
        var cx = x
        font.color = color
        for ((i, part) in parts.withIndex()) {
            val p = if (i == 0) part.trimEnd() else part.trim()
            game.batch.textLeft(font, p, cx, y)
            cx += measure.apply { setText(font, p) }.width
            if (i < parts.size - 1) {
                arrows += Arrow(cx + arrowW / 2 + 2f, y, font.capHeight * 1.1f, color.cpy())
                cx += arrowW + 4f
            }
        }
        font.color = Color.WHITE
        return cx
    }

    /** Draws the arrows queued by [textWithArrow]; call after the batch has ended. */
    fun drawArrows() {
        if (arrows.isEmpty()) return
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (a in arrows) {
            shapes.color = a.color
            shapes.rectLine(a.x - a.size * 0.45f, a.y, a.x + a.size * 0.2f, a.y, a.size * 0.16f)
            shapes.triangle(a.x + a.size * 0.05f, a.y + a.size * 0.3f, a.x + a.size * 0.05f, a.y - a.size * 0.3f, a.x + a.size * 0.5f, a.y)
        }
        shapes.end()
        arrows.clear()
    }

    /** Sword, stopwatch or crosshair, about 28 units times [scale], centred on (cx, cy). */
    fun icon(shapes: ShapeRenderer, k: StatKind, cx: Float, cy: Float, scale: Float = 1f) {
        val s = scale
        shapes.color = color(k)
        when (k) {
            StatKind.DAMAGE -> {
                shapes.rectLine(cx - 9f * s, cy - 9f * s, cx + 11f * s, cy + 11f * s, 5f * s)
                shapes.triangle(cx + 9f * s, cy + 13f * s, cx + 14f * s, cy + 14f * s, cx + 13f * s, cy + 9f * s)
                shapes.rectLine(cx - 13f * s, cy - 3f * s, cx - 3f * s, cy - 13f * s, 4f * s)
                shapes.rectLine(cx - 10f * s, cy - 10f * s, cx - 15f * s, cy - 15f * s, 4f * s)
            }
            StatKind.SPEED -> {
                shapes.ring(cx, cy - 1f * s, 12f * s, 3.5f * s, 24)
                shapes.rect(cx - 3f * s, cy + 12f * s, 6f * s, 5f * s)
                shapes.rectLine(cx, cy - 1f * s, cx, cy + 7f * s, 3f * s)
                shapes.rectLine(cx, cy - 1f * s, cx + 6f * s, cy - 4f * s, 3f * s)
            }
            StatKind.TARGET -> {
                shapes.ring(cx, cy, 11f * s, 3.5f * s, 24)
                shapes.rectLine(cx - 16f * s, cy, cx - 7f * s, cy, 3f * s)
                shapes.rectLine(cx + 7f * s, cy, cx + 16f * s, cy, 3f * s)
                shapes.rectLine(cx, cy - 16f * s, cx, cy - 7f * s, 3f * s)
                shapes.rectLine(cx, cy + 7f * s, cx, cy + 16f * s, 3f * s)
                shapes.circle(cx, cy, 3f * s, 12)
            }
        }
    }

    companion object {
        const val HEIGHT = 92f
        const val GAP = 12f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        private val SPEED_COLOR = Color(0.45f, 0.72f, 1f, 1f)
        private val EFFECT_COLOR = Color(0.78f, 0.82f, 0.95f, 1f)
        private val SYNERGY_COLOR = Color(0.45f, 0.85f, 0.55f, 1f)
        private val POWER_COLOR = Color(0.78f, 0.55f, 1f, 1f)

        fun color(k: StatKind): Color = when (k) {
            StatKind.DAMAGE -> Skin.HEART
            StatKind.SPEED -> SPEED_COLOR
            StatKind.TARGET -> Skin.GOLD
        }

        /** Labels of the detail rows in a colour each, so they read as separate things. */
        fun rowColor(key: String): Color = when (key) {
            S["Efeito"] -> EFFECT_COLOR
            S["7 pips"] -> Skin.GOLD
            S["Sinergia"] -> SYNERGY_COLOR
            S["Power-up"] -> POWER_COLOR
            else -> KEY_COLOR
        }
    }
}
