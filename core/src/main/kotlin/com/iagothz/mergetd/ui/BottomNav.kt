package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import kotlin.math.cos
import kotlin.math.sin

/** The five sections of the game outside a match, in bar order. [title] is the Portuguese label; the UI translates. */
enum class Tab(val title: String) {
    HOME("Início"),
    DECK("Deck"),
    MAPS("Mapas"),
    CHALLENGES("Desafios"),
    SETTINGS("Config"),
}

/**
 * The bar along the bottom of every screen outside a match: one icon and label per [Tab], the [active] one lit.
 * It is [HEIGHT] tall in world units. The screens are laid out for 9:16 and centred; on taller phones the bar moves
 * down to the screen's bottom edge (see [place]) instead of floating over an empty band.
 */
class BottomNav(val active: Tab) {
    val rects: List<Pair<Tab, Rectangle>> = Tab.entries.mapIndexed { i, t ->
        t to Rectangle(i * Layout.WORLD_W / Tab.entries.size, 0f, Layout.WORLD_W / Tab.entries.size, HEIGHT)
    }

    var pressed: Tab? = null
        set(value) {
            field = value
            if (value != null) pressedWas = value
        }

    /** The tab the current touch began on; a release has to land on the same one. */
    var pressedWas: Tab? = null
        private set

    /** Tabs with a gold dot: something new waits there. */
    var badges: Set<Tab> = emptySet()

    /** World y of the bar's bottom edge: 0 on a 9:16 screen, lower on taller phones. */
    var base = 0f
        private set

    /** Puts the bar on the screen bottom ([visibleBottom], the lowest visible world y), over a [GESTURE_PAD] strip. */
    fun place(visibleBottom: Float) {
        base = minOf(0f, visibleBottom + GESTURE_PAD)
    }

    /** The tab under (x, y), reaching below [base] so the gesture strip under the bar answers too. */
    fun tabAt(x: Float, y: Float): Tab? {
        if (y > base + HEIGHT) return null
        return rects.firstOrNull { (_, r) -> x >= r.x && x < r.x + r.width }?.first
    }

    /** Shapes: the bar from below the screen up to [base] + [HEIGHT], then the icons. Call [place] first. */
    fun draw(shapes: ShapeRenderer) {
        shapes.color = BAR
        shapes.rect(-200f, base - 400f, Layout.WORLD_W + 400f, HEIGHT + 400f)
        shapes.color = LINE
        shapes.rect(0f, base + HEIGHT - 2f, Layout.WORLD_W, 2f)
        for ((t, r) in rects) {
            val cx = r.x + r.width / 2
            val cy = base + ICON_Y
            if (t == active) {
                shapes.color = ACTIVE_PILL
                shapes.roundedRect(cx - 34f, cy - 24f, 68f, 48f, 22f)
            }
            val ink = if (t == active) {
                Skin.GOLD
            } else if (t == pressed) {
                Color.WHITE
            } else {
                INK
            }
            icon(shapes, t, cx, cy, ink)
            if (t in badges) {
                shapes.color = Skin.GOLD
                shapes.circle(cx + 24f, cy + 18f, 7f, 16)
            }
        }
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont) {
        for ((t, r) in rects) {
            font.color = if (t == active) Skin.GOLD else INK
            batch.textCentered(font, S[t.title], r.x + r.width / 2, base + LABEL_Y)
        }
        font.color = Color.WHITE
    }

    private fun icon(shapes: ShapeRenderer, t: Tab, cx: Float, cy: Float, ink: Color) {
        shapes.color = ink
        when (t) {
            Tab.HOME -> {
                shapes.triangle(cx - 20f, cy + 2f, cx + 20f, cy + 2f, cx, cy + 18f)
                shapes.rect(cx - 14f, cy - 14f, 28f, 17f)
                shapes.color = if (ink === Skin.GOLD) ACTIVE_PILL else BAR
                shapes.rect(cx - 4f, cy - 14f, 8f, 10f)
            }
            Tab.DECK -> shapes.die(DieType.FIRE, 3, cx, cy, 30f, if (ink === Skin.GOLD) 1f else 0.6f)
            Tab.MAPS -> {
                shapes.rect(cx - 14f, cy - 16f, 4f, 34f)
                shapes.triangle(cx - 10f, cy + 18f, cx - 10f, cy, cx + 18f, cy + 9f)
            }
            Tab.CHALLENGES -> {
                shapes.triangle(cx - 12f, cy + 18f, cx - 2f, cy + 18f, cx - 4f, cy)
                shapes.triangle(cx + 12f, cy + 18f, cx + 2f, cy + 18f, cx + 4f, cy)
                shapes.circle(cx, cy - 6f, 12f, 24)
                shapes.color = if (ink === Skin.GOLD) ACTIVE_PILL else BAR
                shapes.circle(cx, cy - 6f, 5f, 12)
            }
            Tab.SETTINGS -> {
                for (i in 0 until 8) {
                    val a = i * (Math.PI.toFloat() / 4f)
                    shapes.rectLine(cx + cos(a) * 8f, cy + sin(a) * 8f, cx + cos(a) * 18f, cy + sin(a) * 18f, 7f)
                }
                shapes.circle(cx, cy, 12f, 24)
                shapes.color = if (ink === Skin.GOLD) ACTIVE_PILL else BAR
                shapes.circle(cx, cy, 5f, 12)
            }
        }
    }

    companion object {
        const val HEIGHT = 112f
        const val ICON_Y = 72f
        const val LABEL_Y = 26f

        /** Room under the labels for the system's gesture handle. */
        const val GESTURE_PAD = 12f
        val BAR = Color(0.09f, 0.10f, 0.14f, 0.97f)
        val LINE = Color(1f, 1f, 1f, 0.08f)
        val INK = Color(0.62f, 0.64f, 0.72f, 1f)

        /** Opaque: the bar is drawn after the screen, where blending isn't guaranteed. */
        val ACTIVE_PILL = Color(0.26f, 0.24f, 0.18f, 1f)
    }
}
