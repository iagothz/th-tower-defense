package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.ShotEvent
import com.iagothz.mergetd.logic.Synergy
import com.iagothz.mergetd.logic.Vec2
import com.iagothz.mergetd.logic.WaveSpawner
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sin

/** Draws the path, board, enemies, shot traces and HUD of a [Match]. */
class MatchRenderer(
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch,
    private val sprites: Sprites,
    private val font: BitmapFont,
    private val smallFont: BitmapFont,
    private val tinyFont: BitmapFont,
) {
    /** [cat]: thrown by a fire kitten, which draws its own fireball instead of a beam. */
    private class Trace(val shot: ShotEvent, val ttl: Float, val seed: Int, val cat: Boolean = false) {
        var left = ttl

        /** 1 when fired, 0 when gone. */
        val life: Float get() = (left / ttl).coerceIn(0f, 1f)
    }

    private val traces = mutableListOf<Trace>()
    private val cats = FireCats()
    private val tmp = Color()
    private val tmp2 = Color()
    private val cellSide = Color(Skin.CELL).mul(0.5f, 0.5f, 0.56f, 1f)
    private val cellRim = Color(Skin.CELL).lerp(Color.WHITE, 0.12f)
    private var seedCounter = 0
    private var clock = 0f
    private var lastDelta = 0f

    /** Boss HP bar: fills up from empty when a boss appears, then tracks its HP. */
    private var bossSeen: Enemy? = null
    private var bossShown = 0f

    /**
     * Collects the shots of the last update and ages old traces. [newShots] is false while the match is frozen
     * (game over), when the last update's shots are still listed but were already shown.
     */
    fun update(match: Match, delta: Float, newShots: Boolean = true) {
        clock += delta
        lastDelta = delta
        traces.forEach { it.left -= delta }
        traces.removeAll { it.left <= 0f }
        cats.update(delta)
        if (newShots) {
            for (shot in match.lastShots) {
                val cell = Layout.cellAt(shot.from)
                val cat = cell != null && match.board[cell]?.let(::isCat) == true
                if (cat && cell != null) cats.shot(cell, chest(Layout.cellCenter(cell)), shot.to.first())
                traces += Trace(shot, traceTtl(shot.type), seedCounter++, cat)
            }
        }
        val catCells = match.board.occupied().filter { isCat(it.second) }
        catCells.forEach { sprites.prepareCat(it.second.pips) }
        cats.retain(catCells.map { it.first }.toSet())
    }

    /** Fire dice are drawn as the rendered kitten, at the pip levels that have art. */
    private fun isCat(die: Die) = die.type == DieType.FIRE && die.pips <= Sprites.CAT_PIPS

    private fun chest(cellCenter: Vec2) = Vec2(cellCenter.x, cellCenter.y + Layout.CELL * 0.02f)

    private fun traceTtl(t: DieType): Float = when (t) {
        DieType.SNIPER -> 0.22f
        DieType.IRON -> 0.18f
        DieType.FIRE, DieType.NOVA -> 0.2f
        else -> SHOT_TTL
    }

    /** The ground layer: path, board and enemies. Call inside a Filled ShapeRenderer batch, before [drawSprites]. */
    fun drawWorldShapes(match: Match, drag: DragController, effects: Effects) {
        drawPath(match)
        drawTraps(match)
        drawBoard(match, drag, effects)
        drawEnemies(match, effects)
        lifted(match, drag)?.let { (die, p) ->
            if (!isCat(die)) return@let
            shapes.color = tmp2.set(0f, 0f, 0f, 0.35f)
            val w = CAT_BASE_W * DRAG_SCALE
            shapes.ellipse(p.x - w / 2 + 8f, catFoot(p.y) - 22f, w, CAT_BASE_H * DRAG_SCALE)
        }
    }

    /** Above the sprites: slides, shot traces, effects, the lifted die and the HUD. Call inside a Filled ShapeRenderer batch. */
    fun drawOverlayShapes(match: Match, drag: DragController, effects: Effects) {
        for (s in effects.slides) shapes.die(s.type, s.pips, s.x, s.y, DIE)
        drawTraces()
        effects.drawShapes(shapes)
        lifted(match, drag)?.let { (die, p) ->
            if (isCat(die)) return@let
            // Lifted: bigger, with a shadow further below.
            val s = DIE * DRAG_SCALE
            shapes.color = tmp2.set(0f, 0f, 0f, 0.35f)
            shapes.roundedRect(p.x - s / 2 + 8f, p.y - s / 2 - 16f, s, s, 20f)
            drawDie(die, p, 1f, DRAG_SCALE)
        }
        drawHudShapes(match, effects)
    }

    /** The die being dragged and where it is drawn: at the finger, or pulled onto a compatible cell when close. */
    private fun lifted(match: Match, drag: DragController): Pair<Die, Vec2>? {
        val from = drag.dragging ?: return null
        val at = drag.pointer ?: return null
        val die = match.board[from] ?: return null
        return die to (drag.snapTarget()?.let { Layout.cellCenter(it) } ?: at)
    }

    /** The fire kittens, their fireballs and the bursts. Call inside a SpriteBatch batch, between the two shape layers. */
    fun drawSprites(match: Match, drag: DragController, effects: Effects) {
        for ((cell, die) in match.board.occupied()) {
            if (!isCat(die) || effects.slidingTo(cell)) continue
            val c = Layout.cellCenter(cell)
            if (cell == drag.dragging) {
                drawCat(cell, die.pips, c.x, c.y, 1f, 0.35f)
                continue
            }
            val kick = effects.kickOffset(cell)
            val glow = effects.typeGlow(die.type)
            val s = effects.dieScale(cell)
            drawCat(cell, die.pips, c.x + kick.x, c.y + effects.dropOffset(cell) + kick.y, 1f + 0.1f * glow, if (die.disabled) 0.4f else 1f, s.x, s.y)
        }
        val from = drag.dragging
        lifted(match, drag)?.let { (die, p) -> if (isCat(die) && from != null) drawCat(from, die.pips, p.x, p.y, DRAG_SCALE, 1f) }
        for (b in cats.balls) {
            val p = b.position
            val ox = BALL * Sprites.BALL_CORE_X
            val oy = BALL * Sprites.BALL_CORE_Y
            batch.draw(sprites.fireball(clock), p.x - ox, p.y - oy, ox, oy, BALL, BALL, 1f, 1f, b.angle)
        }
        for (b in cats.booms) batch.draw(sprites.impact(b.index), b.at.x - BOOM / 2, b.at.y - BOOM / 2, BOOM, BOOM)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** The [pips]-level kitten of [cell] standing in a cell centred on (x, y); squash and stretch keep its feet down. */
    private fun drawCat(cell: Cell, pips: Int, x: Float, y: Float, scale: Float, brightness: Float, scaleX: Float = 1f, scaleY: Float = 1f) {
        val f = sprites.catMetrics.frame(pips, CAT_BARE)
        val w = f.width * scale * scaleX
        val h = f.height * scale * scaleY
        batch.setColor(brightness, brightness, brightness, 1f)
        batch.draw(sprites.cat(pips, cats.frame(cell)), x - w / 2, catFoot(y) - f.foot * scale * scaleY, w, h)
    }

    private fun catFoot(cellCenterY: Float) = cellCenterY - Layout.CELL * 0.36f

    /** Call inside a SpriteBatch batch. [sp] is the rolling counter's value, not the match's. */
    fun drawHud(match: Match, effects: Effects, sp: Int) {
        popped(effects.pulse("wave")) { batch.textLeft(font, "${match.wave}", WAVE_PILL.x + 58f, HUD_Y) }
        popped(effects.pulse("lives")) { batch.textLeft(font, "${match.lives}", LIVES_PILL.x + 58f, HUD_Y) }
        popped(effects.pulse("sp")) { batch.textLeft(font, "$sp", SP_PILL.x + 58f, HUD_Y) }
        // Hunter kill stacks, bottom-right of the die.
        for ((cell, die) in match.board.occupied()) {
            if (die.type != DieType.HUNTER || die.stacks == 0) continue
            val c = Layout.cellCenter(cell)
            batch.textShadowed(smallFont, "${die.stacks}", c.x + 34f, c.y - 36f)
        }
        if (match.waveCountdown > 0f) {
            val until = WaveSpawner.wavesUntilBoss(match.wave + 1)
            val boss = if (match.mode == GameMode.BOSS_RUSH || WaveSpawner.isBossWave(match.wave + 1)) "" else S.f("  ·  chefe em %d", until)
            if (match.waveCountdown <= URGENT) font.color = tmp.set(1f, 0.75f, 0.3f, 1f)
            popped(effects.pulse("tick"), 0.25f) { batch.textCentered(font, S.f("Próxima onda em %d", ceil(match.waveCountdown).toInt()) + boss, 360f, 1106f) }
            font.color = Color.WHITE
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

    /** Draws [block] with the font scaled up by [amount] × [pulse] (a quick pop), then resets it. */
    private inline fun popped(pulse: Float, amount: Float = 0.35f, block: () -> Unit) {
        if (pulse > 0f) font.data.setScale(1f + amount * pulse)
        block()
        if (pulse > 0f) font.data.setScale(1f)
    }

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

    private fun drawHudShapes(match: Match, effects: Effects) {
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
        // The heart swells and shudders when a life goes.
        val hurt = effects.pulse("lives")
        shapes.color = Skin.HEART
        shapes.heart(LIVES_PILL.x + 32f + sin(clock * 70f) * 4f * hurt, iy, 11f + 7f * hurt)
        shapes.coin(SP_PILL.x + 32f, iy, 14f)

        if (match.waveCountdown > 0f) {
            val frac = (match.waveCountdown / match.waveDelay).coerceIn(0f, 1f)
            val urgency = ((URGENT - match.waveCountdown) / URGENT).coerceIn(0f, 1f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width, COUNTDOWN_BAR.height, 6f)
            shapes.color = tmp2.set(Skin.color(DieType.ELECTRIC)).lerp(1f, 0.3f, 0.2f, 1f, urgency)
            shapes.roundedRect(COUNTDOWN_BAR.x, COUNTDOWN_BAR.y, COUNTDOWN_BAR.width * frac, COUNTDOWN_BAR.height, 6f)
        }
        val chips = synergyChips(match)
        for ((s, r) in chips) {
            shapes.color = Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, r.height / 2)
            shapes.color = Skin.GOLD
            shapes.roundedRect(r.x, r.y, 6f, r.height, 3f)
        }
        val boss = boss(match)
        if (boss !== bossSeen) {
            bossSeen = boss
            bossShown = 0f
        }
        boss?.let { b ->
            val frac = (b.hp / b.maxHp).coerceIn(0f, 1f)
            bossShown += (frac - bossShown) * minOf(1f, lastDelta * 4f)
            shapes.color = Color.BLACK
            shapes.roundedRect(BOSS_BAR.x - 3f, BOSS_BAR.y - 3f, BOSS_BAR.width + 6f, BOSS_BAR.height + 6f, 10f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(BOSS_BAR.x, BOSS_BAR.y, BOSS_BAR.width, BOSS_BAR.height, 8f)
            shapes.color = Skin.color(b.kind)
            // Blinks once it is almost down.
            if (frac < 0.2f) shapes.color = tmp2.set(Skin.color(b.kind)).lerp(Color.WHITE, 0.3f + 0.3f * sin(clock * 14f))
            if (bossShown > 0.01f) shapes.roundedRect(BOSS_BAR.x, BOSS_BAR.y, maxOf(16f, BOSS_BAR.width * bossShown), BOSS_BAR.height, 8f)
        }
    }

    private fun drawPath(match: Match) {
        val pts = match.path.points
        val theme = Skin.theme(match.map)
        // Seen at an angle the path is a sunken track: the rim, then its far wall (darker), then the floor a
        // little lower, which leaves the wall showing along the top.
        val wall = tmp.set(theme.ground).mul(0.5f, 0.5f, 0.55f, 1f)
        for ((c, w, dy) in listOf(Triple(theme.edge, PATH_WIDTH + 14f, 0f), Triple(wall, PATH_WIDTH, 0f), Triple(theme.ground, PATH_WIDTH - 4f, -PATH_DEPTH))) {
            shapes.color = c
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y + dy, b.x, b.y + dy, w)
            pts.forEach { shapes.rect(it.x - w / 2, it.y + dy - w / 2, w, w) }
        }
        val start = pts.first()
        val end = pts.last()
        for (pt in listOf(start, end)) {
            shapes.color = tmp2.set(theme.edge).mul(0.6f, 0.6f, 0.6f, 1f)
            shapes.ellipse(pt.x - 21f, pt.y - 19f, 42f, 30f)
            shapes.color = theme.edge
            shapes.ellipse(pt.x - 21f, pt.y - 13f, 42f, 30f)
        }
        shapes.color = Color(0.3f, 0.75f, 0.35f, 1f)
        shapes.ellipse(start.x - 15f, start.y - 9f, 30f, 22f)
        shapes.color = Skin.HEART
        shapes.heart(end.x, end.y + 1f, 11f)
    }

    private fun drawTraps(match: Match) {
        for (t in match.traps) {
            val p = match.path.positionAt(t.distance)
            shapes.color = Skin.theme(match.map).edge
            shapes.ellipse(p.x - 13f, p.y - 14f, 26f, 20f)
            shapes.color = tmp2.set(Skin.color(DieType.TRAP)).mul(0.6f, 0.6f, 0.6f, 1f)
            shapes.ellipse(p.x - 11f, p.y - 11f, 22f, 16f)
            shapes.color = Skin.color(DieType.TRAP)
            shapes.ellipse(p.x - 11f, p.y - 7f, 22f, 16f)
            shapes.color = Color(0.95f, 0.3f, 0.2f, 1f)
            shapes.ellipse(p.x - 4f, p.y - 2f, 8f, 6f)
        }
    }

    private fun drawBoard(match: Match, drag: DragController, effects: Effects) {
        val highlights = drag.highlights().toSet()
        val snap = drag.snapTarget()
        val pulse = 0.55f + 0.45f * sin(clock * 9f)
        shapes.color = Skin.SHADOW
        shapes.roundedRect(BOARD.x + 3f, BOARD.y - BOARD_DEPTH - 10f, BOARD.width, BOARD.height, 26f)
        // Front face of the slab, then its top.
        shapes.color = tmp2.set(Cosmetics.frameColor).mul(0.6f, 0.6f, 0.6f, 1f)
        shapes.roundedRect(BOARD.x, BOARD.y - BOARD_DEPTH, BOARD.width, BOARD.height, 26f)
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
            shapes.color = cellSide
            shapes.roundedRect(c.x - half, c.y - half - CELL_DEPTH, Layout.CELL, Layout.CELL, 16f)
            shapes.color = Skin.CELL
            shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            // A lit top edge separates the pedestal's top from the face below it.
            shapes.color = cellRim
            shapes.rect(c.x - half + 14f, c.y + half - 4f, Layout.CELL - 28f, 3f)
            val flash = effects.cellFlash(cell)
            if (flash > 0f) {
                shapes.color = tmp2.set(1f, 1f, 1f, 0.85f * flash)
                shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            }
            val die = match.board[cell] ?: continue
            if (effects.slidingTo(cell)) continue
            if (isCat(die)) {
                // The kitten itself is a sprite (drawSprites); here only the warm spot it stands on.
                val bright = if (die.disabled || cell == drag.dragging) 0.4f else 1f
                val fy = catFoot(c.y + effects.dropOffset(cell))
                shapes.color = tmp2.set(Skin.color(DieType.FIRE)).mul(0.55f * bright, 0.4f * bright, 0.4f * bright, 1f)
                shapes.ellipse(c.x - CAT_BASE_W / 2, fy - CAT_BASE_H / 2 - 3f, CAT_BASE_W, CAT_BASE_H)
                shapes.color = tmp2.set(Skin.color(DieType.FIRE)).mul(bright, bright, bright, 1f)
                shapes.ellipse(c.x - CAT_BASE_W / 2, fy - CAT_BASE_H / 2, CAT_BASE_W, CAT_BASE_H)
                continue
            }
            val kick = effects.kickOffset(cell)
            val lifted = Vec2(c.x + kick.x, c.y + effects.dropOffset(cell) + kick.y)
            if (cell == drag.dragging) {
                drawDie(die, c, 0.35f)
            } else {
                // A power-up lights every die of its type and gives it a little jump.
                val glow = effects.typeGlow(die.type)
                val s = effects.dieScale(cell)
                val bright = if (die.disabled) 0.4f else 1f + 0.45f * glow
                shapes.die(die.type, die.pips, lifted.x, lifted.y, DIE * (1f + 0.1f * glow), bright, 1f, s.x, s.y)
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
            if (!ghost) shapes.polygon(p.x, p.y - ENEMY_DEPTH, r + outline, sides, heading)
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
            if (t.cat) {
                // The kitten throws a rendered fireball (drawSprites); only a crit still gets its star.
                if (t.shot.crit) drawCrit(t.shot.to.first(), life)
                continue
            }
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
            if (t.shot.crit) drawCrit(t.shot.to.first(), life)
        }
    }

    private fun drawCrit(hit: Vec2, life: Float) {
        shapes.color = tmp2.set(1f, 0.85f, 0.4f, life)
        val s = 14f + 10f * (1f - life)
        shapes.rectLine(hit.x - s, hit.y, hit.x + s, hit.y, 3f)
        shapes.rectLine(hit.x, hit.y - s, hit.x, hit.y + s, 3f)
        shapes.rectLine(hit.x - s * 0.7f, hit.y - s * 0.7f, hit.x + s * 0.7f, hit.y + s * 0.7f, 2f)
        shapes.rectLine(hit.x - s * 0.7f, hit.y + s * 0.7f, hit.x + s * 0.7f, hit.y - s * 0.7f, 2f)
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

        /** Last seconds of the countdown: the bar reddens and the ticks play. */
        private const val URGENT = 3f
        const val PATH_WIDTH = 40f

        /** 3/4 view: how far the path floor sits below its rim, and the front faces of the board, cells and enemies. */
        private const val PATH_DEPTH = 13f
        private const val BOARD_DEPTH = 36f
        private const val CELL_DEPTH = 20f
        private const val ENEMY_DEPTH = 10f
        const val DIE = Layout.CELL - 8f

        /** A bare (pip-1) kitten stands about one cell tall; hats and staffs rise above. */
        private const val CAT_BARE = Layout.CELL * 1.04f
        private const val CAT_BASE_W = Layout.CELL * 0.74f
        private const val CAT_BASE_H = Layout.CELL * 0.26f
        private const val BALL = 66f

        /** The burst's ring roughly matches the fire splash. */
        private const val BOOM = Balance.FIRE_SPLASH_RADIUS * 2.3f
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
