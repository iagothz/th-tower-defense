package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Layout
import kotlin.math.sin
import kotlin.random.Random

/** Ambient particles that tell the maps apart: fireflies, blowing sand, snow or rising embers. */
class Weather(map: GameMap, rng: Random = Random(7)) {
    enum class Kind { FIREFLIES, SAND, SNOW, EMBERS }

    class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, val size: Float, val phase: Float)

    val kind = kindFor(map)
    val particles: List<Particle> = List(COUNT) {
        val (vx, vy) = when (kind) {
            Kind.FIREFLIES -> 6f + rng.nextFloat() * 10f to -4f + rng.nextFloat() * 8f
            Kind.SAND -> 140f + rng.nextFloat() * 120f to -30f - rng.nextFloat() * 30f
            Kind.SNOW -> 4f + rng.nextFloat() * 10f to -35f - rng.nextFloat() * 30f
            Kind.EMBERS -> -8f + rng.nextFloat() * 16f to 40f + rng.nextFloat() * 50f
        }
        Particle(
            -MARGIN + rng.nextFloat() * (Layout.WORLD_W + 2 * MARGIN),
            -MARGIN + rng.nextFloat() * (Layout.WORLD_H + 2 * MARGIN),
            vx, vy, 1.5f + rng.nextFloat() * 2.5f, rng.nextFloat() * 6.28f,
        )
    }
    private var time = 0f
    private val tmp = Color()

    fun update(dt: Float) {
        time += dt
        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (kind == Kind.FIREFLIES || kind == Kind.SNOW) p.x += sin(time * 0.8f + p.phase) * 18f * dt
            // Wrap around the extended sky so the field never thins out.
            val w = Layout.WORLD_W + 2 * MARGIN
            val h = Layout.WORLD_H + 2 * MARGIN
            if (p.x < -MARGIN) p.x += w else if (p.x > Layout.WORLD_W + MARGIN) p.x -= w
            if (p.y < -MARGIN) p.y += h else if (p.y > Layout.WORLD_H + MARGIN) p.y -= h
        }
    }

    /** Call inside a Filled batch with blending on. */
    fun draw(shapes: ShapeRenderer) {
        for (p in particles) {
            val glow = 0.5f + 0.5f * sin(time * 2.5f + p.phase)
            when (kind) {
                Kind.FIREFLIES -> {
                    shapes.color = tmp.set(0.85f, 1f, 0.55f, 0.15f + 0.6f * glow)
                    shapes.circle(p.x, p.y, p.size * (0.6f + 0.4f * glow), 8)
                }
                Kind.SAND -> {
                    shapes.color = tmp.set(0.85f, 0.72f, 0.45f, 0.35f)
                    shapes.rectLine(p.x, p.y, p.x - p.vx * 0.06f, p.y - p.vy * 0.06f, p.size * 0.6f)
                }
                Kind.SNOW -> {
                    shapes.color = tmp.set(1f, 1f, 1f, 0.55f + 0.3f * glow)
                    shapes.circle(p.x, p.y, p.size, 8)
                }
                Kind.EMBERS -> {
                    shapes.color = tmp.set(1f, 0.45f + 0.3f * glow, 0.15f, 0.3f + 0.6f * glow)
                    shapes.circle(p.x, p.y, p.size * 0.8f, 6)
                }
            }
        }
    }

    companion object {
        const val COUNT = 40
        const val MARGIN = 240f

        fun kindFor(map: GameMap): Kind = when (map) {
            GameMap.HORSESHOE -> Kind.FIREFLIES
            GameMap.SERPENT -> Kind.SAND
            GameMap.RING -> Kind.SNOW
            GameMap.STRAIGHT -> Kind.EMBERS
        }
    }
}
