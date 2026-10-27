package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Short-lived juice driven by [GameEvent]s: damage numbers, flashes, particles, rings, pops, shake and wave banners. */
class Effects(private val rng: Random = Random.Default) {
    class FloatingText(
        val text: String,
        val x: Float,
        var y: Float,
        var age: Float = 0f,
        val crit: Boolean = false,
        val reward: Boolean = false,
    )
    class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, val color: Color, var age: Float = 0f)
    /** An explosion ring that grows from a third of [max] to [max] while fading. */
    class Ring(val x: Float, val y: Float, val max: Float, val color: Color, var age: Float = 0f) {
        val radius: Float get() = max * (0.33f + 0.67f * (age / RING_TIME).coerceIn(0f, 1f))
    }

    val texts = mutableListOf<FloatingText>()
    val particles = mutableListOf<Particle>()
    val rings = mutableListOf<Ring>()
    private val flashes = mutableMapOf<Enemy, Float>()
    private val pops = mutableMapOf<Cell, Float>()
    private var shakeLeft = 0f
    private var shakeDuration = 0f
    private var shakeAmp = 0f
    var banner: String? = null
        private set
    private var bannerAge = 0f
    private val tmp = Color()

    fun consume(events: List<GameEvent>) {
        val hits = linkedMapOf<Enemy, Float>()
        val crits = mutableSetOf<Enemy>()
        for (e in events) {
            when (e) {
                is GameEvent.Hit -> {
                    hits[e.enemy] = (hits[e.enemy] ?: 0f) + e.damage
                    if (e.crit) crits += e.enemy
                }
                is GameEvent.Killed -> {
                    burst(e.at, Skin.color(e.kind), PARTICLES_PER_KILL)
                    if (texts.size < MAX_TEXTS) texts += FloatingText("+${e.kind.reward}", e.at.x, e.at.y + 10f, reward = true)
                }
                is GameEvent.Blast -> rings += Ring(e.at.x, e.at.y, e.radius, Skin.color(e.type))
                is GameEvent.DieBought -> pops[e.cell] = 0f
                is GameEvent.Merged -> {
                    pops[e.cell] = 0f
                    val c = Layout.cellCenter(e.cell)
                    burst(c, Skin.GOLD, PARTICLES_PER_MERGE)
                }
                is GameEvent.Leaked -> shake(LEAK_SHAKE, 0.25f)
                is GameEvent.WaveStarted -> {
                    banner = if (e.boss) "Chefe!" else "Onda ${e.wave}"
                    bannerAge = 0f
                    if (e.boss) shake(6f, 0.4f)
                }
                is GameEvent.PoweredUp -> Unit
            }
        }
        for ((enemy, amount) in hits) {
            flashes[enemy] = FLASH_TIME
            if (texts.size >= MAX_TEXTS) continue
            val p = Layout.PATH.positionAt(enemy.distance)
            texts += FloatingText(amount.roundToInt().coerceAtLeast(1).toString(), p.x, p.y + 30f, crit = enemy in crits)
        }
    }

    fun update(dt: Float) {
        texts.forEach {
            it.age += dt
            it.y += TEXT_RISE * dt
        }
        texts.removeAll { it.age >= TEXT_TIME }
        particles.forEach {
            it.age += dt
            it.x += it.vx * dt
            it.y += it.vy * dt
        }
        particles.removeAll { it.age >= PARTICLE_TIME }
        rings.forEach { it.age += dt }
        rings.removeAll { it.age >= RING_TIME }
        flashes.replaceAll { _, t -> t - dt }
        flashes.values.removeAll { it <= 0f }
        pops.replaceAll { _, t -> t + dt }
        pops.values.removeAll { it >= POP_TIME }
        shakeLeft = maxOf(0f, shakeLeft - dt)
        if (banner != null) {
            bannerAge += dt
            if (bannerAge >= BANNER_TIME) banner = null
        }
    }

    fun isFlashing(enemy: Enemy): Boolean = enemy in flashes

    /** 1 when idle; overshoots from 0.6 up to 1 just after a die appears. */
    fun popScale(cell: Cell): Float {
        val age = pops[cell] ?: return 1f
        val t = (age / POP_TIME).coerceIn(0f, 1f) - 1f
        val c1 = 1.70158f
        val easeOutBack = 1f + (c1 + 1f) * t.pow(3) + c1 * t.pow(2)
        return 0.6f + 0.4f * easeOutBack
    }

    fun shakeOffset(): Vec2 {
        if (shakeLeft <= 0f) return Vec2(0f, 0f)
        val a = shakeAmp * (shakeLeft / shakeDuration)
        return Vec2(rng.nextFloat() * 2 * a - a, rng.nextFloat() * 2 * a - a)
    }

    /** Banner opacity: snaps in, holds, fades over the last 0.4 s. 0 when there is none. */
    fun bannerAlpha(): Float = if (banner == null) 0f else minOf(1f, (BANNER_TIME - bannerAge) / 0.4f)

    /** Call inside a Filled ShapeRenderer batch, with GL blending enabled. */
    fun drawShapes(shapes: ShapeRenderer) {
        for (r in rings) {
            val life = 1f - r.age / RING_TIME
            shapes.color = tmp.set(r.color).also { it.a = 0.25f * life }
            shapes.circle(r.x, r.y, r.radius, 32)
            shapes.color = tmp.set(r.color).also { it.a = life }
            shapes.ring(r.x, r.y, r.radius, 3f + 3f * life)
        }
        for (p in particles) {
            val life = 1f - p.age / PARTICLE_TIME
            shapes.color = tmp.set(p.color).also { it.a = life }
            shapes.circle(p.x, p.y, 3f + 4f * life, 8)
        }
        val a = bannerAlpha()
        if (a > 0f) {
            shapes.color = tmp.set(0f, 0f, 0f, 0.55f * a)
            shapes.rect(0f, BANNER_Y - 52f, Layout.WORLD_W, 104f)
            shapes.color = tmp.set(1f, 0.9f, 0.5f, a)
            shapes.rect(0f, BANNER_Y - 52f, Layout.WORLD_W, 3f)
            shapes.rect(0f, BANNER_Y + 49f, Layout.WORLD_W, 3f)
        }
    }

    /** Call inside a SpriteBatch batch. */
    fun drawText(batch: SpriteBatch, small: BitmapFont, big: BitmapFont, medium: BitmapFont) {
        for (t in texts) {
            val fade = 1f - t.age / TEXT_TIME
            when {
                t.crit -> batch.textShadowed(medium, t.text, t.x, t.y, tmp.set(1f, 0.6f, 0.15f, fade))
                t.reward -> batch.textShadowed(small, t.text, t.x + 26f, t.y, tmp.set(Skin.GOLD).also { it.a = fade })
                else -> batch.textShadowed(small, t.text, t.x, t.y, tmp.set(1f, 1f, 1f, fade))
            }
        }
        banner?.let {
            val a = bannerAlpha()
            big.setColor(1f, 0.9f, 0.5f, a)
            batch.textCentered(big, it, Layout.WORLD_W / 2, BANNER_Y)
            big.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun burst(at: Vec2, color: Color, count: Int) {
        repeat(count) {
            val angle = rng.nextFloat() * 2 * PI.toFloat()
            val speed = 60f + rng.nextFloat() * 120f
            particles += Particle(at.x, at.y, cos(angle) * speed, sin(angle) * speed, color)
        }
    }

    /** Starts a shake unless a stronger one is still running. */
    private fun shake(amp: Float, duration: Float) {
        if (shakeLeft > 0f && shakeAmp * shakeLeft / shakeDuration > amp) return
        shakeAmp = amp
        shakeDuration = duration
        shakeLeft = duration
    }

    companion object {
        const val PARTICLES_PER_KILL = 10
        const val PARTICLES_PER_MERGE = 14
        const val PARTICLE_TIME = 0.45f
        const val RING_TIME = 0.35f
        const val TEXT_TIME = 0.6f
        const val TEXT_RISE = 70f
        const val MAX_TEXTS = 60
        const val FLASH_TIME = 0.08f
        const val POP_TIME = 0.25f
        const val LEAK_SHAKE = 10f
        const val BANNER_TIME = 1.5f
        const val BANNER_Y = 1010f
    }
}
