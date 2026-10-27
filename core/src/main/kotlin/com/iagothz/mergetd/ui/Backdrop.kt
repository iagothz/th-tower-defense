package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.Layout
import kotlin.math.sin
import kotlin.random.Random

/** The night-sky background every screen sits on: a vertical gradient and slowly twinkling stars. */
class Backdrop {
    private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)

    private val stars = Random(42).let { rng ->
        List(STARS) {
            Star(
                -MARGIN + rng.nextFloat() * (Layout.WORLD_W + 2 * MARGIN),
                -MARGIN + rng.nextFloat() * (Layout.WORLD_H + 2 * MARGIN),
                1.2f + rng.nextFloat() * 1.8f,
                rng.nextFloat() * 6.28f,
                0.6f + rng.nextFloat() * 1.2f,
            )
        }
    }
    private val tmp = Color()
    private val tmp2 = Color()

    /** Call inside a Filled batch with blending on. [time] is the clock in seconds; the sky covers the whole visible world. */
    fun draw(shapes: ShapeRenderer, time: Float, theme: MapTheme, worldWidth: Float, worldHeight: Float) {
        val x = (Layout.WORLD_W - worldWidth) / 2
        val y = (Layout.WORLD_H - worldHeight) / 2
        val tint = Cosmetics.sky.color
        val top = tmp.set(theme.skyTop).mul(tint.r, tint.g, tint.b, 1f)
        val bottom = tmp2.set(theme.skyBottom).mul(tint.r, tint.g, tint.b, 1f)
        shapes.rect(x, y, worldWidth, worldHeight, bottom, bottom, top, top)
        for (s in stars) {
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * s.speed + s.phase))
            shapes.color = tmp.set(0.75f, 0.8f, 0.95f, 0.55f * twinkle)
            shapes.circle(s.x, s.y, s.r * (0.8f + 0.2f * twinkle), 8)
        }
    }

    private companion object {
        const val STARS = 90
        /** Stars reach this far outside the 720×1280 world, for taller or wider screens. */
        const val MARGIN = 240f
    }
}
