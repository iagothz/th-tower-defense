package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.EnemyPath
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Short-lived juice driven by [GameEvent]s: damage numbers, flashes, particles, rings, pops, squash, shake,
 * hit-stop, recoil, wave banners. Everything here is cosmetic; the match never reads it.
 */
class Effects(private val rng: Random = Random.Default, private val path: EnemyPath = GameMap.DEFAULT.path) {
    class FloatingText(
        val text: String,
        val x: Float,
        var y: Float,
        var age: Float = 0f,
        val crit: Boolean = false,
        val reward: Boolean = false,
        val bad: Boolean = false,
    ) {
        /** Crits pop in at 1.6× and settle to 1 over [TEXT_POP_TIME]; everything else stays at 1. */
        val scale: Float
            get() = if (!crit) 1f else 1f + 0.6f * (1f - (age / TEXT_POP_TIME).coerceIn(0f, 1f))
    }

    /** Sparks fly straight; embers ([ttl] longer than [PARTICLE_TIME]) drift up and are drawn smaller. */
    class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val color: Color,
        var age: Float = 0f,
        val ttl: Float = PARTICLE_TIME,
    ) {
        val life: Float get() = 1f - (age / ttl).coerceIn(0f, 1f)
        val ember: Boolean get() = ttl > PARTICLE_TIME
    }

    /** The dragged die gliding to the cell it merged into; the result stays hidden until it arrives. */
    class Slide(val from: Vec2, val target: Vec2, val to: Cell, val type: DieType, val pips: Int, var age: Float = 0f) {
        private val t: Float get() = (age / SLIDE_TIME).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) }
        val x: Float get() = from.x + (target.x - from.x) * t
        val y: Float get() = from.y + (target.y - from.y) * t
    }

    /** A dead enemy: a quick swell, then shrinking away where it fell. */
    class Dying(val kind: EnemyKind, val x: Float, val y: Float, var age: Float = 0f) {
        val scale: Float
            get() {
                val t = (age / DYING_TIME).coerceIn(0f, 1f)
                return if (t < 0.2f) 1f + 0.3f * (t / 0.2f) else 1.3f * (1f - (t - 0.2f) / 0.8f)
            }
    }

    /** An explosion ring that grows from a third of [max] to [max] while fading. */
    class Ring(val x: Float, val y: Float, val max: Float, val color: Color, var age: Float = 0f) {
        val radius: Float get() = max * (0.33f + 0.67f * (age / RING_TIME).coerceIn(0f, 1f))
    }

    private class Kick(val dir: Vec2, var age: Float = 0f)

    val texts = mutableListOf<FloatingText>()
    val particles = mutableListOf<Particle>()
    val rings = mutableListOf<Ring>()
    val slides = mutableListOf<Slide>()
    val dying = mutableListOf<Dying>()
    private val drops = mutableMapOf<Cell, Float>()
    private val flashes = mutableMapOf<Enemy, Float>()
    private val jolts = mutableMapOf<Enemy, Float>()
    private val pops = mutableMapOf<Cell, Float>()
    private val squashes = mutableMapOf<Cell, Float>()
    private val cellFlashes = mutableMapOf<Cell, Float>()
    private val kicks = mutableMapOf<Cell, Kick>()
    private var shakeLeft = 0f
    private var shakeDuration = 0f
    private var shakeAmp = 0f
    private var freezeLeft = 0f
    private var slowLeft = 0f
    var banner: String? = null
        private set
    /** Second line under the banner: what the boss does. */
    var bannerNote: String? = null
        private set
    private var bannerAge = 0f
    private var bannerTime = BANNER_TIME
    private val tmp = Color()

    fun consume(events: List<GameEvent>) {
        val hits = linkedMapOf<Enemy, Float>()
        val crits = mutableSetOf<Enemy>()
        for (e in events) {
            when (e) {
                is GameEvent.Hit -> {
                    hits[e.enemy] = (hits[e.enemy] ?: 0f) + e.damage
                    if (e.crit) crits += e.enemy
                    if (e.crit || e.type in HEAVY) jolts[e.enemy] = 0f
                }
                is GameEvent.Killed -> {
                    burst(e.at, Skin.color(e.kind), PARTICLES_PER_KILL)
                    dying += Dying(e.kind, e.at.x, e.at.y)
                    if (texts.size < MAX_TEXTS) texts += FloatingText("+${e.kind.reward}", e.at.x, e.at.y + 10f, reward = true)
                    if (e.kind.isBoss) {
                        burst(e.at, Skin.GOLD, PARTICLES_PER_BOSS, speed = 2f)
                        burst(e.at, Skin.color(e.kind), PARTICLES_PER_BOSS / 2, speed = 1.5f)
                        rings += Ring(e.at.x, e.at.y, 140f, Skin.GOLD)
                        rings += Ring(e.at.x, e.at.y, 90f, Skin.color(e.kind))
                        shake(BOSS_KILL_SHAKE, 0.4f)
                        slowLeft = SLOWMO_TIME
                    }
                }
                is GameEvent.Blast -> {
                    rings += Ring(e.at.x, e.at.y, e.radius, Skin.color(e.type))
                    if (e.type == DieType.FIRE) embers(e.at, e.radius)
                }
                is GameEvent.DieBought -> {
                    pops[e.cell] = 0f
                    drops[e.cell] = 0f
                }
                is GameEvent.Unmerged -> {
                    pops[e.from] = 0f
                    pops[e.to] = 0f
                }
                is GameEvent.Merged -> {
                    pops[e.cell] = 0f
                    squashes[e.cell] = 0f
                    cellFlashes[e.cell] = 0f
                    freezeLeft = HIT_STOP
                    val c = Layout.cellCenter(e.cell)
                    burst(c, Skin.GOLD, PARTICLES_PER_MERGE)
                    if (e.pips >= Balance.MAX_PIPS) {
                        burst(c, Skin.GOLD, PARTICLES_PER_MAX, speed = 1.8f)
                        rings += Ring(c.x, c.y, 110f, Skin.GOLD)
                        shake(MAX_SHAKE, 0.3f)
                        if (texts.size < MAX_TEXTS) texts += FloatingText(S["Máximo!"], c.x, c.y + 50f, crit = true)
                    }
                }
                is GameEvent.Leaked -> shake(LEAK_SHAKE, 0.25f)
                is GameEvent.Stolen -> if (texts.size < MAX_TEXTS) {
                    texts += FloatingText(S.f("-%d SP", e.amount), Layout.WORLD_W / 2, 1150f, bad = true)
                }
                is GameEvent.WaveStarted -> {
                    val boss = e.bossKind
                    show(
                        when {
                            boss != null -> S.f("Chefe: %s", Skin.enemyName(boss))
                            e.boss -> S["Chefe!"]
                            e.special != null -> S.f("%s!", S[e.special.title])
                            else -> S.f("Onda %d", e.wave)
                        },
                        note = boss?.let { Skin.bossAbility(it) },
                        seconds = if (e.boss) BOSS_BANNER_TIME else BANNER_TIME,
                    )
                    if (e.boss) shake(6f, 0.4f)
                }
                is GameEvent.PoweredUp -> Unit
            }
        }
        for ((enemy, amount) in hits) {
            flashes[enemy] = FLASH_TIME
            if (texts.size >= MAX_TEXTS) continue
            val p = path.positionAt(enemy.distance)
            texts += FloatingText(amount.roundToInt().coerceAtLeast(1).toString(), p.x, p.y + 30f, crit = enemy in crits)
        }
    }

    /** Ages everything by real time [dt]; the match itself runs on [timeScale] of it. */
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
        particles.removeAll { it.age >= it.ttl }
        rings.forEach { it.age += dt }
        rings.removeAll { it.age >= RING_TIME }
        slides.forEach { it.age += dt }
        slides.removeAll { it.age >= SLIDE_TIME }
        squashes.replaceAll { _, t -> t + dt }
        squashes.values.removeAll { it >= SQUASH_TIME }
        drops.replaceAll { _, t -> t + dt }
        // Touching down starts the squash.
        for ((cell, t) in drops) if (t >= DROP_TIME) squashes[cell] = 0f
        drops.values.removeAll { it >= DROP_TIME }
        cellFlashes.replaceAll { _, t -> t + dt }
        cellFlashes.values.removeAll { it >= CELL_FLASH_TIME }
        flashes.replaceAll { _, t -> t - dt }
        flashes.values.removeAll { it <= 0f }
        jolts.replaceAll { _, t -> t + dt }
        jolts.values.removeAll { it >= JOLT_TIME }
        pops.replaceAll { _, t -> t + dt }
        pops.values.removeAll { it >= POP_TIME }
        kicks.values.forEach { it.age += dt }
        kicks.values.removeAll { it.age >= KICK_TIME }
        shakeLeft = maxOf(0f, shakeLeft - dt)
        freezeLeft = maxOf(0f, freezeLeft - dt)
        slowLeft = maxOf(0f, slowLeft - dt)
        dying.forEach { it.age += dt }
        dying.removeAll { it.age >= DYING_TIME }
        if (banner != null) {
            bannerAge += dt
            if (bannerAge >= bannerTime) {
                banner = null
                bannerNote = null
            }
        }
    }

    /** How fast the match should run this frame: frozen for an instant after a merge, slow after a boss dies. */
    fun timeScale(): Float = when {
        freezeLeft > 0f -> 0f
        slowLeft > 0f -> SLOWMO_SCALE
        else -> 1f
    }

    fun isFlashing(enemy: Enemy): Boolean = enemy in flashes

    /** How far back along the path a heavily hit enemy is thrown right now, in path units. */
    fun jolt(enemy: Enemy): Float {
        val age = jolts[enemy] ?: return 0f
        return JOLT_AMP * (1f - (age / JOLT_TIME).coerceIn(0f, 1f))
    }

    /** White overlay strength on a cell that just received a merge: 1 → 0. */
    fun cellFlash(cell: Cell): Float {
        val age = cellFlashes[cell] ?: return 0f
        return 1f - (age / CELL_FLASH_TIME).coerceIn(0f, 1f)
    }

    /** A die at [cell] fired towards [dir]: it recoils the other way and eases back. */
    fun kick(cell: Cell, dir: Vec2) {
        val len = sqrt(dir.x * dir.x + dir.y * dir.y)
        if (len == 0f) return
        kicks[cell] = Kick(Vec2(dir.x / len, dir.y / len))
    }

    fun kickOffset(cell: Cell): Vec2 {
        val k = kicks[cell] ?: return Vec2(0f, 0f)
        val a = KICK_AMP * (1f - (k.age / KICK_TIME).coerceIn(0f, 1f))
        return Vec2(-k.dir.x * a, -k.dir.y * a)
    }

    /** Puts [text] on the banner, like a wave start. */
    fun announce(text: String, seconds: Float = BANNER_TIME) = show(text, null, seconds)

    private fun show(text: String, note: String?, seconds: Float) {
        banner = text
        bannerNote = note
        bannerAge = 0f
        bannerTime = seconds
    }

    fun slide(from: Cell, to: Cell, type: DieType, pips: Int) {
        slides += Slide(Layout.cellCenter(from), Layout.cellCenter(to), to, type, pips)
    }

    /** True while a merged die is still gliding into [cell]; the renderer holds the result back until then. */
    fun slidingTo(cell: Cell): Boolean = slides.any { it.to == cell }

    /** How far above its cell a freshly bought die still hangs: falls from DROP_HEIGHT, lands, hops once and settles. */
    fun dropOffset(cell: Cell): Float {
        val age = drops[cell] ?: return 0f
        val t = (age / DROP_TIME).coerceIn(0f, 1f)
        return if (t < LAND_AT) {
            val f = t / LAND_AT
            DROP_HEIGHT * (1f - f * f)
        } else {
            DROP_HEIGHT * HOP * sin(PI.toFloat() * (t - LAND_AT) / (1f - LAND_AT))
        }
    }

    /** 1 when idle; overshoots from 0.6 up to 1 just after a die appears. */
    fun popScale(cell: Cell): Float {
        val age = pops[cell] ?: return 1f
        val t = (age / POP_TIME).coerceIn(0f, 1f) - 1f
        val c1 = 1.70158f
        val easeOutBack = 1f + (c1 + 1f) * t.pow(3) + c1 * t.pow(2)
        return 0.6f + 0.4f * easeOutBack
    }

    /** Width and height scale of the die at [cell]: the pop, times a squash that lands wide and rings down. */
    fun dieScale(cell: Cell): Vec2 {
        val pop = popScale(cell)
        val age = squashes[cell]
        if (age == null) return if (pop == 1f) Vec2(1f, 1f) else Vec2(pop, pop)
        val t = (age / SQUASH_TIME).coerceIn(0f, 1f)
        val q = (1f - t).pow(2) * cos(t * 1.5f * PI.toFloat()) * SQUASH
        return Vec2(pop * (1f + q), pop * (1f - q))
    }

    fun shakeOffset(): Vec2 {
        if (shakeLeft <= 0f) return Vec2(0f, 0f)
        val a = shakeAmp * (shakeLeft / shakeDuration)
        return Vec2(rng.nextFloat() * 2 * a - a, rng.nextFloat() * 2 * a - a)
    }

    /** Banner opacity: snaps in, holds, fades over the last 0.4 s. 0 when there is none. */
    fun bannerAlpha(): Float = if (banner == null) 0f else minOf(1f, (bannerTime - bannerAge) / 0.4f)

    /** Call inside a Filled ShapeRenderer batch, with GL blending enabled. */
    fun drawShapes(shapes: ShapeRenderer) {
        for (r in rings) {
            val life = 1f - r.age / RING_TIME
            shapes.color = tmp.set(r.color).also { it.a = 0.25f * life }
            shapes.circle(r.x, r.y, r.radius, 32)
            shapes.color = tmp.set(r.color).also { it.a = life }
            shapes.ring(r.x, r.y, r.radius, 3f + 3f * life)
        }
        for (d in dying) {
            val r = Skin.enemyRadius(d.kind) * d.scale
            shapes.color = tmp.set(Skin.color(d.kind)).also { it.a = 0.8f * minOf(1f, d.scale) }
            shapes.polygon(d.x, d.y, r, Skin.enemySides(d.kind))
        }
        for (p in particles) {
            val life = p.life
            shapes.color = tmp.set(p.color).also { it.a = life }
            if (p.ember) shapes.circle(p.x, p.y, 1.5f + 2.5f * life, 6) else shapes.circle(p.x, p.y, 3f + 4f * life, 8)
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
                t.crit -> {
                    medium.data.setScale(t.scale)
                    batch.textShadowed(medium, t.text, t.x, t.y, tmp.set(1f, 0.6f, 0.15f, fade))
                    medium.data.setScale(1f)
                }
                t.reward -> batch.textShadowed(small, t.text, t.x + 26f, t.y, tmp.set(Skin.GOLD).also { it.a = fade })
                t.bad -> batch.textShadowed(medium, t.text, t.x, t.y, tmp.set(Skin.HEART).also { it.a = fade })
                else -> batch.textShadowed(small, t.text, t.x, t.y, tmp.set(1f, 1f, 1f, fade))
            }
        }
        banner?.let {
            val a = bannerAlpha()
            big.setColor(1f, 0.9f, 0.5f, a)
            batch.textCentered(big, it, Layout.WORLD_W / 2, BANNER_Y + if (bannerNote != null) 14f else 0f)
            big.setColor(1f, 1f, 1f, 1f)
            bannerNote?.let { note ->
                small.setColor(1f, 1f, 1f, a)
                batch.textCentered(small, note, Layout.WORLD_W / 2, BANNER_Y - 34f)
                small.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun burst(at: Vec2, color: Color, count: Int, speed: Float = 1f) {
        repeat(count) {
            val angle = rng.nextFloat() * 2 * PI.toFloat()
            val v = (60f + rng.nextFloat() * 120f) * speed
            particles += Particle(at.x, at.y, cos(angle) * v, sin(angle) * v, color)
        }
    }

    /** Glowing bits that drift up from a fire blast and linger on the ground. */
    private fun embers(at: Vec2, radius: Float) {
        repeat(EMBERS_PER_BLAST) {
            val angle = rng.nextFloat() * 2 * PI.toFloat()
            val d = rng.nextFloat() * radius * 0.7f
            val color = if (rng.nextBoolean()) EMBER else EMBER_DARK
            particles += Particle(at.x + cos(angle) * d, at.y + sin(angle) * d, (rng.nextFloat() - 0.5f) * 20f, 15f + rng.nextFloat() * 30f, color, ttl = EMBER_TIME)
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
        const val PARTICLES_PER_MAX = 30
        const val PARTICLES_PER_BOSS = 40
        const val EMBERS_PER_BLAST = 6
        const val PARTICLE_TIME = 0.45f
        const val EMBER_TIME = 0.8f
        const val RING_TIME = 0.35f
        const val SLIDE_TIME = 0.14f
        const val DROP_TIME = 0.3f
        const val DROP_HEIGHT = 90f
        /** Fraction of DROP_TIME spent falling; the rest is one small hop. */
        private const val LAND_AT = 0.6f
        private const val HOP = 0.16f
        const val TEXT_TIME = 0.6f
        const val TEXT_RISE = 70f
        const val TEXT_POP_TIME = 0.15f
        const val MAX_TEXTS = 60
        const val FLASH_TIME = 0.08f
        const val CELL_FLASH_TIME = 0.18f
        const val POP_TIME = 0.25f
        const val SQUASH_TIME = 0.22f
        private const val SQUASH = 0.28f
        const val HIT_STOP = 0.06f
        const val SLOWMO_TIME = 0.45f
        const val SLOWMO_SCALE = 0.3f
        const val KICK_TIME = 0.12f
        const val KICK_AMP = 5f
        const val JOLT_TIME = 0.12f
        const val JOLT_AMP = 7f
        const val LEAK_SHAKE = 10f
        const val MAX_SHAKE = 5f
        const val BOSS_KILL_SHAKE = 9f
        const val BANNER_TIME = 1.5f
        const val BOSS_BANNER_TIME = 2.6f
        const val DYING_TIME = 0.3f
        const val BANNER_Y = 1010f
        /** Dice whose hits throw the enemy back a step. */
        val HEAVY = setOf(DieType.IRON, DieType.SNIPER, DieType.GRAVITY)
        private val EMBER = Color(1f, 0.55f, 0.15f, 1f)
        private val EMBER_DARK = Color(0.9f, 0.3f, 0.1f, 1f)
    }
}
