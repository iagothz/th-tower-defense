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
 * It is [HEIGHT] tall in world units and, on phones taller than 9:16, its background runs on down to the edge.
 */
class BottomNav(val active: Tab) {
    val rects: List<Pair<Tab, Rectangle>> = Tab.entries.mapIndexed { i, t ->
        t to Rectangle(i * Layout.WORLD_W / Tab.entries.size, 0f, Layout.WORLD_W / Tab.entries.size, HEIGHT)
    }

    /** The tab a finger is resting on. */
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

    /** The tab under (x, y), reaching below 0 so the bar's extension on taller phones answers too. */
    fun tabAt(x: Float, y: Float): Tab? {
        if (y > HEIGHT) return null
        return rects.firstOrNull { (_, r) -> x >= r.x && x < r.x + r.width }?.first
    }

    /** Shapes: the bar from [bottom] (the lowest visible world y) up to [HEIGHT], then the icons. */
    fun draw(shapes: ShapeRenderer, bottom: Float) {
        shapes.color = BAR
        shapes.rect(-200f, bottom - 200f, Layout.WORLD_W + 400f, HEIGHT - bottom + 200f)
        shapes.color = LINE
        shapes.rect(0f, HEIGHT - 2f, Layout.WORLD_W, 2f)
        for ((t, r) in rects) {
            val cx = r.x + r.width / 2
            val cy = ICON_Y
            if (t == active) {
                shapes.color = ACTIVE_PILL
                shapes.roundedRect(cx - 34f, cy - 24f, 68f, 48f, 22f)
            }
            val ink = if (t == active) Skin.GOLD else if (t == pressed) Color.WHITE else INK
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
            batch.textCentered(font, S[t.title], r.x + r.width / 2, LABEL_Y)
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
                // A flag on a pole.
                shapes.rect(cx - 14f, cy - 16f, 4f, 34f)
                shapes.triangle(cx - 10f, cy + 18f, cx - 10f, cy, cx + 18f, cy + 9f)
            }
            Tab.CHALLENGES -> {
                // A medal: ribbon tails under a disc.
                shapes.triangle(cx - 12f, cy + 18f, cx - 2f, cy + 18f, cx - 4f, cy)
                shapes.triangle(cx + 12f, cy + 18f, cx + 2f, cy + 18f, cx + 4f, cy)
                shapes.circle(cx, cy - 6f, 12f, 24)
                shapes.color = if (ink === Skin.GOLD) ACTIVE_PILL else BAR
                shapes.circle(cx, cy - 6f, 5f, 12)
            }
            Tab.SETTINGS -> {
                // A gear: eight teeth around a ring.
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
        val BAR = Color(0.09f, 0.10f, 0.14f, 0.97f)
        val LINE = Color(1f, 1f, 1f, 0.08f)
        val INK = Color(0.62f, 0.64f, 0.72f, 1f)
        /** Opaque: the bar is drawn after the screen, where blending isn't guaranteed. */
        val ACTIVE_PILL = Color(0.26f, 0.24f, 0.18f, 1f)
    }
}
