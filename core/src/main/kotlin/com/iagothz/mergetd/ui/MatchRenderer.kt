package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.ShotEvent
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.atan2
import kotlin.math.ceil

/** Draws the path, board, enemies, shot traces and HUD of a [Match]. */
class MatchRenderer(
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch,
    private val font: BitmapFont,
    private val smallFont: BitmapFont,
) {
    private class Trace(val shot: ShotEvent, val ttl: Float, val seed: Int) {
        var left = ttl
        /** 1 when fired, 0 when gone. */
        val life: Float get() = (left / ttl).coerceIn(0f, 1f)
    }

    private val traces = mutableListOf<Trace>()
    private val tmp = Color()
    private val tmp2 = Color()
    private var seedCounter = 0

    /** Collects the shots of the last update and ages old traces. */
    fun update(match: Match, delta: Float) {
        traces.forEach { it.left -= delta }
        traces.removeAll { it.left <= 0f }
        match.lastShots.forEach { traces += Trace(it, traceTtl(it.type), seedCounter++) }
    }

    private fun traceTtl(t: DieType): Float = when (t) {
        DieType.SNIPER -> 0.22f
        DieType.IRON -> 0.18f
        DieType.FIRE, DieType.NOVA -> 0.2f
        else -> SHOT_TTL
    }

    /** Call inside a Filled ShapeRenderer batch. */
    fun drawShapes(match: Match, drag: DragController, effects: Effects) {
        drawPath()
        drawTraps(match)
        drawBoard(match, drag, effects)
        drawEnemies(match, effects)
        drawTraces()
        effects.drawShapes(shapes)
        val from = drag.dragging
        val at = drag.pointer
        if (from != null && at != null) match.board[from]?.let { drawDie(it, at, 1f) }
        drawHudShapes(match)
    }

    /** Call inside a SpriteBatch batch. */
    fun drawHud(match: Match) {
        batch.textLeft(font, "${match.wave}", WAVE_PILL.x + 58f, HUD_Y)
        batch.textLeft(font, "${match.lives}", LIVES_PILL.x + 58f, HUD_Y)
        batch.textLeft(font, "${match.sp}", SP_PILL.x + 58f, HUD_Y)
        // Hunter kill stacks, bottom-right of the die.
        for ((cell, die) in match.board.occupied()) {
            if (die.type != DieType.HUNTER || die.stacks == 0) continue
            val c = Layout.cellCenter(cell)
            batch.textShadowed(smallFont, "${die.stacks}", c.x + 34f, c.y - 36f)
        }
        if (match.waveCountdown > 0f) {
            batch.textCentered(font, "Próxima onda em ${ceil(match.waveCountdown).toInt()}", 360f, 1110f)
        }
        boss(match)?.let { batch.textCentered(smallFont, Skin.enemyName(it.kind), 360f, BOSS_BAR.y + BOSS_BAR.height + 18f) }
    }

    private fun boss(match: Match): Enemy? = match.enemies.firstOrNull { it.kind.isBoss && it.alive }

    private fun drawHudShapes(match: Match) {
        for (pill in listOf(WAVE_PILL, LIVES_PILL, SP_PILL)) {
            shapes.color = Skin.PANEL
            shapes.roundedRect(pill.x, pill.y, pill.width, pill.height, pill.height / 2)
        }
        val iy = HUD_Y
        // Wave: a flag.
        shapes.color = Color.WHITE
        shapes.rectLine(WAVE_PILL.x + 22f, iy - 14f, WAVE_PILL.x + 22f, iy + 16f, 3f)
        shapes.color = Skin.color(DieType.COMBO)
        shapes.triangle(WAVE_PILL.x + 23f, iy + 16f, WAVE_PILL.x + 23f, iy, WAVE_PILL.x + 45f, iy + 8f)
        shapes.color = Skin.HEART
        shapes.heart(LIVES_PILL.x + 32f, iy, 11f)
        shapes.coin(SP_PILL.x + 32f, iy, 14f)

        if (match.waveCountdown > 0f) {
            val frac = (match.waveCountdown / Balance.WAVE_DELAY).coerceIn(0f, 1f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width, COUNTDOWN_BAR.height, 6f)
            shapes.color = Skin.color(DieType.ELECTRIC)
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width * frac, COUNTDOWN_BAR.height, 6f)
        }
        boss(match)?.let { b ->
            val frac = (b.hp / b.maxHp).coerceIn(0f, 1f)
            shapes.color = Color.BLACK
            shapes.roundedRect(BOSS_BAR.x - 3f, BOSS_BAR.y - 3f, BOSS_BAR.width + 6f, BOSS_BAR.height + 6f, 10f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(BOSS_BAR.x, BOSS_BAR.y, BOSS_BAR.width, BOSS_BAR.height, 8f)
            shapes.color = Skin.color(b.kind)
            if (frac > 0f) shapes.roundedRect(BOSS_BAR.x, BOSS_BAR.y, maxOf(16f, BOSS_BAR.width * frac), BOSS_BAR.height, 8f)
        }
    }

    private fun drawPath() {
        val pts = Layout.PATH.points
        for ((c, w) in listOf(Skin.PATH_EDGE to PATH_WIDTH + 8f, Skin.PATH to PATH_WIDTH)) {
            shapes.color = c
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, w)
            pts.forEach { shapes.rect(it.x - w / 2, it.y - w / 2, w, w) }
        }
        val start = pts.first()
        val end = pts.last()
        shapes.color = Skin.PATH_EDGE
        shapes.circle(start.x, start.y, 20f, 24)
        shapes.circle(end.x, end.y, 20f, 24)
        shapes.color = Color(0.3f, 0.75f, 0.35f, 1f)
        shapes.circle(start.x, start.y, 15f, 24)
        shapes.color = Skin.HEART
        shapes.heart(end.x, end.y + 1f, 11f)
    }

    private fun drawTraps(match: Match) {
        for (t in match.traps) {
            val p = Layout.PATH.positionAt(t.distance)
            shapes.color = Skin.PATH_EDGE
            shapes.circle(p.x, p.y, 13f, 16)
            shapes.color = Skin.color(DieType.TRAP)
            shapes.circle(p.x, p.y, 11f, 16)
            shapes.color = Color(0.95f, 0.3f, 0.2f, 1f)
            shapes.circle(p.x, p.y, 4f, 10)
        }
    }

    private fun drawBoard(match: Match, drag: DragController, effects: Effects) {
        val highlights = drag.highlights().toSet()
        for (cell in match.board.cells()) {
            val c = Layout.cellCenter(cell)
            val half = Layout.CELL / 2
            if (cell in highlights) {
                shapes.color = Color.WHITE
                shapes.roundedRect(c.x - half - 4, c.y - half - 4, Layout.CELL + 8, Layout.CELL + 8, 20f)
            }
            shapes.color = Skin.CELL
            shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            val die = match.board[cell] ?: continue
            when {
                cell == drag.dragging -> drawDie(die, c, 0.35f)
                else -> drawDie(die, c, if (die.disabled) 0.4f else 1f, effects.popScale(cell))
            }
        }
    }

    private fun drawDie(die: Die, c: Vec2, brightness: Float, scale: Float = 1f) {
        val size = DIE * scale
        val half = size / 2
        val lip = 5f * scale
        val base = tmp.set(Skin.color(die.type)).mul(brightness, brightness, brightness, 1f)
        shapes.bevelRect(c.x - half, c.y - half, size, size, 18f * scale, base, lip)
        // The body sits on top of the lip, so everything on its face is centred a little higher.
        val cy = c.y + lip / 2
        val ink = tmp2.set(Skin.glyphColor(die.type)).mul(brightness, brightness, brightness, 1f)
        shapes.glyph(die.type, c.x, cy, half * 0.62f, ink, base)
        shapes.color = tmp2.set(Skin.pipColor(die.type)).mul(brightness, brightness, brightness, 1f)
        for ((dx, dy) in PIPS[die.pips - 1]) {
            shapes.circle(c.x + dx * PIP_STEP * scale, cy + dy * PIP_STEP * scale, PIP_R * scale, 14)
        }
    }

    private fun drawEnemies(match: Match, effects: Effects) {
        for (e in match.enemies) {
            val p = Layout.PATH.positionAt(e.distance)
            val r = Skin.enemyRadius(e.kind)
            val sides = Skin.enemySides(e.kind)
            val ahead = Layout.PATH.positionAt(e.distance + 4f)
            val heading = atan2(ahead.y - p.y, ahead.x - p.x)
            if (e.curse > 0f) {
                shapes.color = Skin.color(DieType.CURSE)
                shapes.circle(p.x, p.y, r + 9f, 24)
            }
            if (e.poison.isNotEmpty()) {
                shapes.color = Color(0.3f, 0.85f, 0.3f, 1f)
                shapes.circle(p.x, p.y, r + 5f, 24)
            }
            val body = tmp.set(Skin.color(e.kind))
            if (e.slow > 0f) body.lerp(0.5f, 0.8f, 1f, 1f, 0.5f)
            if (e.stunTime > 0f) body.lerp(Skin.color(DieType.TIME), 0.6f)
            if (effects.isFlashing(e)) body.set(Color.WHITE)
            shapes.color = tmp2.set(body).mul(0.4f, 0.4f, 0.45f, 1f)
            shapes.polygon(p.x, p.y, r + 3f, sides, heading)
            shapes.color = body
            shapes.polygon(p.x, p.y, r, sides, heading)
            if (e.kind.isBoss) {
                shapes.color = tmp2.set(body).lerp(Color.WHITE, 0.3f)
                shapes.circle(p.x, p.y, r * 0.5f, 20)
                shapes.color = Skin.GOLD
                for (dx in listOf(-14f, 0f, 14f)) {
                    shapes.triangle(p.x + dx - 7f, p.y + r - 2f, p.x + dx + 7f, p.y + r - 2f, p.x + dx, p.y + r + 14f)
                }
            }
            drawHpBar(p, r + if (e.kind.isBoss) 20f else 8f, r + 6f, e)
        }
    }

    private fun drawHpBar(p: Vec2, above: Float, halfWidth: Float, e: Enemy) {
        val w = halfWidth * 2
        val ratio = (e.hp / e.maxHp).coerceIn(0f, 1f)
        shapes.color = Color.BLACK
        shapes.rect(p.x - halfWidth - 1f, p.y + above - 1f, w + 2f, 8f)
        shapes.color = Skin.PANEL
        shapes.rect(p.x - halfWidth, p.y + above, w, 6f)
        shapes.color = tmp2.set(0.9f, 0.2f, 0.2f, 1f).lerp(0.35f, 0.85f, 0.35f, 1f, ratio)
        shapes.rect(p.x - halfWidth, p.y + above, w * ratio, 6f)
    }

    private fun drawTraces() {
        for (t in traces) {
            val type = t.shot.type
            val color = tmp.set(Skin.color(type))
            val life = t.life
            // Muzzle flash on the die.
            shapes.color = tmp2.set(color).lerp(Color.WHITE, 0.5f).also { it.a = life }
            shapes.circle(t.shot.from.x, t.shot.from.y, 6f + 10f * life, 12)
            when (type) {
                DieType.ELECTRIC -> drawLightning(t, color)
                DieType.SNIPER, DieType.IRON -> drawProjectile(t, color)
                else -> drawBeam(t, color)
            }
            if (type == DieType.FIRE) {
                val hit = t.shot.to.first()
                val r = Balance.FIRE_SPLASH_RADIUS * (0.4f + 0.6f * (1f - life))
                shapes.color = tmp2.set(color).also { it.a = 0.25f * life }
                shapes.circle(hit.x, hit.y, r, 28)
                shapes.color = tmp2.set(color).also { it.a = life }
                shapes.ring(hit.x, hit.y, r, 3f)
            }
            if (t.shot.crit) {
                val hit = t.shot.to.first()
                shapes.color = tmp2.set(1f, 0.85f, 0.4f, life)
                val s = 14f + 10f * (1f - life)
                shapes.rectLine(hit.x - s, hit.y, hit.x + s, hit.y, 3f)
                shapes.rectLine(hit.x, hit.y - s, hit.x, hit.y + s, 3f)
                shapes.rectLine(hit.x - s * 0.7f, hit.y - s * 0.7f, hit.x + s * 0.7f, hit.y + s * 0.7f, 2f)
                shapes.rectLine(hit.x - s * 0.7f, hit.y + s * 0.7f, hit.x + s * 0.7f, hit.y - s * 0.7f, 2f)
            }
        }
    }

    /** Straight lines; chains hop from hit to hit, a quake fans out to every enemy from the die. */
    private fun drawBeam(t: Trace, color: Color) {
        val fan = t.shot.type == DieType.QUAKE
        val width = when {
            t.shot.crit -> 8f
            t.shot.type == DieType.WIND -> 2.5f
            else -> 4f
        } * (0.5f + 0.5f * t.life)
        shapes.color = tmp2.set(color).also { it.a = t.life }
        var from = t.shot.from
        for (to in t.shot.to) {
            shapes.rectLine(from.x, from.y, to.x, to.y, width)
            if (!fan) from = to
        }
        if (t.shot.crit) {
            shapes.color = tmp2.set(1f, 1f, 1f, t.life)
            from = t.shot.from
            for (to in t.shot.to) {
                shapes.rectLine(from.x, from.y, to.x, to.y, 2f)
                if (!fan) from = to
            }
        }
    }

    /** A jagged bolt with a white core; the jitter is fixed per trace so it doesn't shimmer. */
    private fun drawLightning(t: Trace, color: Color) {
        val rng = kotlin.random.Random(t.seed)
        var from = t.shot.from
        for (to in t.shot.to) {
            val dx = to.x - from.x
            val dy = to.y - from.y
            val len = maxOf(1f, kotlin.math.sqrt(dx * dx + dy * dy))
            val nx = -dy / len
            val ny = dx / len
            val steps = (len / 28f).toInt().coerceIn(2, 10)
            val pts = ArrayList<Vec2>(steps + 1)
            pts += from
            for (i in 1 until steps) {
                val f = i.toFloat() / steps
                val off = (rng.nextFloat() * 2f - 1f) * 14f
                pts += Vec2(from.x + dx * f + nx * off, from.y + dy * f + ny * off)
            }
            pts += to
            shapes.color = tmp2.set(color).also { it.a = t.life }
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, 5f)
            shapes.color = tmp2.set(1f, 1f, 1f, t.life)
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, 1.5f)
            from = to
        }
    }

    /** A visible bullet (Sniper) or cannonball (Iron) flying from the die to the target. */
    private fun drawProjectile(t: Trace, color: Color) {
        val from = t.shot.from
        val to = t.shot.to.first()
        val f = 1f - t.life
        val x = from.x + (to.x - from.x) * f
        val y = from.y + (to.y - from.y) * f
        val heavy = t.shot.type == DieType.IRON
        if (!heavy) {
            shapes.color = tmp2.set(color).also { it.a = 0.5f * t.life }
            shapes.rectLine(from.x, from.y, x, y, 2f)
        }
        shapes.color = tmp2.set(color).mul(0.5f, 0.5f, 0.5f, 1f)
        shapes.circle(x, y, if (heavy) 11f else 7f, 16)
        shapes.color = if (heavy) color else Color.WHITE
        shapes.circle(x, y, if (heavy) 8f else 4f, 16)
    }

    private companion object {
        const val SHOT_TTL = 0.1f
        const val PATH_WIDTH = 40f
        const val DIE = Layout.CELL - 8f
        const val PIP_STEP = 24f
        const val PIP_R = 8.5f

        const val HUD_Y = 1210f
        val WAVE_PILL = Rectangle(16f, HUD_Y - 28f, 180f, 56f)
        val LIVES_PILL = Rectangle(212f, HUD_Y - 28f, 180f, 56f)
        val SP_PILL = Rectangle(408f, HUD_Y - 28f, 196f, 56f)
        val COUNTDOWN_BAR = Rectangle(160f, 1072f, 400f, 12f)
        val BOSS_BAR = Rectangle(80f, 1120f, 560f, 22f)

        private val C = 0 to 0
        private val TL = -1 to 1
        private val TR = 1 to 1
        private val BL = -1 to -1
        private val BR = 1 to -1
        private val ML = -1 to 0
        private val MR = 1 to 0

        /** Pip offsets (in PIP_STEP units) for 1..7 pips. */
        val PIPS = listOf(
            listOf(C),
            listOf(TL, BR),
            listOf(TL, C, BR),
            listOf(TL, TR, BL, BR),
            listOf(TL, TR, C, BL, BR),
            listOf(TL, TR, ML, MR, BL, BR),
            listOf(TL, TR, ML, C, MR, BL, BR),
        )
    }
}
