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
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Synergy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.ShotEvent
import com.iagothz.mergetd.logic.Vec2
import com.iagothz.mergetd.logic.WaveSpawner
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sin

/** Draws the path, board, enemies, shot traces and HUD of a [Match]. */
class MatchRenderer(
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch,
    private val font: BitmapFont,
    private val smallFont: BitmapFont,
    private val tinyFont: BitmapFont,
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
    private var clock = 0f

    /** Collects the shots of the last update and ages old traces. */
    fun update(match: Match, delta: Float) {
        clock += delta
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
        drawPath(match)
        drawTraps(match)
        drawBoard(match, drag, effects)
        drawEnemies(match, effects)
        for (s in effects.slides) shapes.die(s.type, s.pips, s.x, s.y, DIE)
        drawTraces()
        effects.drawShapes(shapes)
        val from = drag.dragging
        val at = drag.pointer
        if (from != null && at != null) match.board[from]?.let { die ->
            // Lifted: bigger, with a shadow further below, and pulled onto a compatible cell when close.
            val p = drag.snapTarget()?.let { Layout.cellCenter(it) } ?: at
            val s = DIE * DRAG_SCALE
            shapes.color = tmp2.set(0f, 0f, 0f, 0.35f)
            shapes.roundedRect(p.x - s / 2 + 8f, p.y - s / 2 - 16f, s, s, 20f)
            drawDie(die, p, 1f, DRAG_SCALE)
        }
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
            val until = WaveSpawner.wavesUntilBoss(match.wave + 1)
            val boss = if (match.mode == GameMode.BOSS_RUSH || WaveSpawner.isBossWave(match.wave + 1)) "" else S.f("  ·  chefe em %d", until)
            batch.textCentered(font, S.f("Próxima onda em %d", ceil(match.waveCountdown).toInt()) + boss, 360f, 1106f)
            batch.textCentered(tinyFont, wavePreview(match.wave + 1, match.bossOrder, match.mode), 360f, 1140f)
        }
        boss(match)?.let { batch.textCentered(smallFont, Skin.enemyName(it.kind), 360f, BOSS_BAR.y + BOSS_BAR.height + 18f) }
        for ((s, r) in synergyChips(match)) {
            tinyFont.color = Skin.GOLD
            batch.textCentered(tinyFont, S[s.title], r.x + r.width / 2 + 3f, r.y + r.height / 2)
            tinyFont.color = Color.WHITE
        }
        if (match.challengeDay != null) {
            tinyFont.color = Skin.GOLD
            batch.textCentered(tinyFont, S["Desafio do dia"], 360f, 1166f)
            tinyFont.color = Color.WHITE
        }
    }

    /** "12 soldados · 4 corredores", "Horda! 40 soldados", or "Chefe: Trocador" on a boss wave. */
    /** Chips for the active synergies, side by side on the board's top rim. */
    private fun synergyChips(match: Match): List<Pair<Synergy, Rectangle>> {
        val active = match.synergies().toList()
        if (active.isEmpty()) return emptyList()
        val widths = active.map { 22f + S[it.title].length * 9.5f }
        var x = 360f - (widths.sum() + 8f * (active.size - 1)) / 2
        return active.mapIndexed { i, s ->
            val r = Rectangle(x, BOARD.y + BOARD.height - 14f, widths[i], 28f)
            x += widths[i] + 8f
            s to r
        }
    }

    private fun wavePreview(next: Int, bosses: List<EnemyKind>, mode: GameMode): String {
        val counts = WaveSpawner.preview(next, bosses, mode)
        val boss = counts.keys.firstOrNull { it.isBoss }
        if (boss != null) return S.f("Chefe: %s", Skin.enemyName(boss))
        val special = WaveSpawner.special(next)?.let { S.f("%s!", S[it.title]) + " " } ?: ""
        return special + counts.entries.joinToString(" · ") { (kind, n) -> S.f("%d %s", n, Skin.enemyPlural(kind)) }
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
            val frac = (match.waveCountdown / match.waveDelay).coerceIn(0f, 1f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width, COUNTDOWN_BAR.height, 6f)
            shapes.color = Skin.color(DieType.ELECTRIC)
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width * frac, COUNTDOWN_BAR.height, 6f)
        }
        val chips = synergyChips(match)
        for ((s, r) in chips) {
            shapes.color = Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, r.height / 2)
            shapes.color = Skin.GOLD
            shapes.roundedRect(r.x, r.y, 6f, r.height, 3f)
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

    private fun drawPath(match: Match) {
        val pts = match.path.points
        val theme = Skin.theme(match.map)
        for ((c, w) in listOf(theme.edge to PATH_WIDTH + 8f, theme.ground to PATH_WIDTH)) {
            shapes.color = c
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, w)
            pts.forEach { shapes.rect(it.x - w / 2, it.y - w / 2, w, w) }
        }
        val start = pts.first()
        val end = pts.last()
        shapes.color = theme.edge
        shapes.circle(start.x, start.y, 20f, 24)
        shapes.circle(end.x, end.y, 20f, 24)
        shapes.color = Color(0.3f, 0.75f, 0.35f, 1f)
        shapes.circle(start.x, start.y, 15f, 24)
        shapes.color = Skin.HEART
        shapes.heart(end.x, end.y + 1f, 11f)
    }

    private fun drawTraps(match: Match) {
        for (t in match.traps) {
            val p = match.path.positionAt(t.distance)
            shapes.color = Skin.theme(match.map).edge
            shapes.circle(p.x, p.y, 13f, 16)
            shapes.color = Skin.color(DieType.TRAP)
            shapes.circle(p.x, p.y, 11f, 16)
            shapes.color = Color(0.95f, 0.3f, 0.2f, 1f)
            shapes.circle(p.x, p.y, 4f, 10)
        }
    }

    private fun drawBoard(match: Match, drag: DragController, effects: Effects) {
        val highlights = drag.highlights().toSet()
        val snap = drag.snapTarget()
        val pulse = 0.55f + 0.45f * sin(clock * 9f)
        shapes.color = Skin.SHADOW
        shapes.roundedRect(BOARD.x + 3f, BOARD.y - 8f, BOARD.width, BOARD.height, 26f)
        shapes.color = Cosmetics.frameColor
        shapes.roundedRect(BOARD.x, BOARD.y, BOARD.width, BOARD.height, 26f)
        shapes.color = Skin.BOARD
        shapes.roundedRect(BOARD.x + 4f, BOARD.y + 4f, BOARD.width - 8f, BOARD.height - 8f, 22f)
        for (cell in match.board.cells()) {
            val c = Layout.cellCenter(cell)
            val half = Layout.CELL / 2
            if (cell in highlights) {
                shapes.color = if (cell == snap) tmp2.set(Skin.GOLD) else tmp2.set(1f, 1f, 1f, pulse)
                shapes.roundedRect(c.x - half - 4, c.y - half - 4, Layout.CELL + 8, Layout.CELL + 8, 20f)
            }
            shapes.color = Skin.CELL
            shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            val flash = effects.cellFlash(cell)
            if (flash > 0f) {
                shapes.color = tmp2.set(1f, 1f, 1f, 0.85f * flash)
                shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            }
            val die = match.board[cell] ?: continue
            if (effects.slidingTo(cell)) continue
            val kick = effects.kickOffset(cell)
            val lifted = Vec2(c.x + kick.x, c.y + effects.dropOffset(cell) + kick.y)
            if (cell == drag.dragging) {
                drawDie(die, c, 0.35f)
            } else {
                val s = effects.dieScale(cell)
                shapes.die(die.type, die.pips, lifted.x, lifted.y, DIE, if (die.disabled) 0.4f else 1f, 1f, s.x, s.y)
            }
        }
    }

    private fun drawDie(die: Die, c: Vec2, brightness: Float, scale: Float = 1f) {
        shapes.die(die.type, die.pips, c.x, c.y, DIE * scale, brightness)
    }

    private fun drawEnemies(match: Match, effects: Effects) {
        for ((i, e) in match.enemies.withIndex()) {
            // A heavy hit throws the body back a step and puffs it up for an instant.
            val jolt = effects.jolt(e)
            val ground = match.path.positionAt(e.distance - jolt)
            val r = Skin.enemyRadius(e.kind) * (1f + 0.18f * jolt / Effects.JOLT_AMP)
            val sides = Skin.enemySides(e.kind)
            val ahead = match.path.positionAt(e.distance + 4f)
            val heading = atan2(ahead.y - ground.y, ahead.x - ground.x)
            // A little bob while walking, and a shadow that stays on the ground.
            val bob = if (e.speed() > 0f && !e.kind.isBoss) kotlin.math.abs(kotlin.math.sin(clock * 9f + i)) * 4f else 0f
            shapes.color = tmp2.set(0f, 0f, 0f, 0.3f)
            shapes.ellipse(ground.x - r * 0.9f, ground.y - r * 0.85f - 4f, r * 1.8f, r * 0.55f)
            val p = Vec2(ground.x, ground.y + bob)
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
            val ghost = e.kind == EnemyKind.GHOST
            val outline = if (e.kind == EnemyKind.BOSS_ARMORED) 7f else 3f
            shapes.color = tmp2.set(body).mul(0.4f, 0.4f, 0.45f, 1f).also { if (ghost) it.a = 0.45f }
            shapes.polygon(p.x, p.y, r + outline, sides, heading)
            shapes.color = tmp2.set(body).also { if (ghost) it.a = 0.55f }
            shapes.polygon(p.x, p.y, r, sides, heading)
            if (e.kind == EnemyKind.HEALER) {
                shapes.color = Color(0.85f, 0.25f, 0.3f, 1f)
                shapes.rectLine(p.x - 9f, p.y, p.x + 9f, p.y, 5f)
                shapes.rectLine(p.x, p.y - 9f, p.x, p.y + 9f, 5f)
            }
            if (e.kind.isBoss) {
                shapes.color = tmp2.set(body).lerp(Color.WHITE, 0.3f)
                shapes.circle(p.x, p.y, r * 0.5f, 20)
                shapes.color = Skin.GOLD
                val spikes = if (e.kind == EnemyKind.BOSS_COMMANDER) listOf(-22f, -11f, 0f, 11f, 22f) else listOf(-14f, 0f, 14f)
                for (dx in spikes) {
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

    /** A jagged bolt with a white core; the jitter is fixed per trace so it doesn't shimmer, but it flickers. */
    private fun drawLightning(t: Trace, color: Color) {
        val rng = kotlin.random.Random(t.seed)
        val flicker = t.life * (0.7f + 0.3f * sin(clock * 90f + t.seed))
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
            shapes.color = tmp2.set(color).also { it.a = flicker }
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, 5f)
            shapes.color = tmp2.set(1f, 1f, 1f, flicker)
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
        } else {
            // Ghosts of where the ball just was.
            for (i in 1..3) {
                val g = f - i * 0.09f
                if (g <= 0f) break
                shapes.color = tmp2.set(color).also { it.a = (0.35f - i * 0.1f) * t.life }
                shapes.circle(from.x + (to.x - from.x) * g, from.y + (to.y - from.y) * g, 9f - i * 1.5f, 12)
            }
        }
        shapes.color = tmp2.set(color).mul(0.5f, 0.5f, 0.5f, 1f)
        shapes.circle(x, y, if (heavy) 11f else 7f, 16)
        shapes.color = if (heavy) color else Color.WHITE
        shapes.circle(x, y, if (heavy) 8f else 4f, 16)
    }

    companion object {
        private const val SHOT_TTL = 0.1f
        private const val DRAG_SCALE = 1.12f
        const val PATH_WIDTH = 40f
        const val DIE = Layout.CELL - 8f
        private const val BOARD_PAD = 18f
        val BOARD = Rectangle(
            Layout.GRID_X - BOARD_PAD,
            Layout.GRID_Y - BOARD_PAD,
            Balance.COLS * (Layout.CELL + Layout.GAP) - Layout.GAP + 2 * BOARD_PAD,
            Balance.ROWS * (Layout.CELL + Layout.GAP) - Layout.GAP + 2 * BOARD_PAD,
        )

        const val HUD_Y = 1210f
        val WAVE_PILL = Rectangle(16f, HUD_Y - 28f, 150f, 56f)
        val LIVES_PILL = Rectangle(176f, HUD_Y - 28f, 150f, 56f)
        val SP_PILL = Rectangle(336f, HUD_Y - 28f, 180f, 56f)
        val COUNTDOWN_BAR = Rectangle(160f, 1072f, 400f, 12f)
        val BOSS_BAR = Rectangle(80f, 1120f, 560f, 22f)
    }
}
