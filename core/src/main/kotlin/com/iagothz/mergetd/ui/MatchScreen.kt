package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.iagothz.mergetd.logic.GameMode
import kotlin.math.sin
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Combat
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.MatchCode
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

/**
 * [restored] is a match saved when the app last went to the background; it opens paused.
 * [previousBest] is the record from before that match began, so "Novo recorde!" still shows after a restore.
 */
class MatchScreen(
    game: MergeTdGame,
    restored: Match? = null,
    private val previousBest: Int = game.save.bestWave(game.save.map, game.save.mode),
    /** A fresh daily challenge: starts running, no tutorial, booked against the day's record. */
    fresh: Match? = null,
) : BaseScreen(game) {
    private enum class Confirm(val question: String) { RESTART("Reiniciar a partida?"), QUIT("Sair da partida?") }

    private val match = restored ?: fresh ?: Match.seeded(game.save.deck, game.save.map, game.save.mode)
    private val drag = DragController(match)
    private val hold = LongPress()
    private val renderer = MatchRenderer(game.shapes, game.batch, game.font, game.smallFont, game.tinyFont)
    private val buttonTap = Tap<Button>()
    private val effects = Effects(path = match.path)
    private val stats = MatchStats()
    private val tutorial = Tutorial(active = restored == null && fresh == null && !game.save.tutorialDone)

    init {
        if (fresh != null) effects.announce(S["Desafio do dia: deck e mapa do dia"], 2.5f)
    }
    private val sounds = SoundPlanner()
    private val spCounter = Counter(match.sp)
    private val vignetteColor = Color()
    private val vignetteClear = Color()
    private var clock = 0f
    private var paused = restored != null
    private var confirm: Confirm? = null
    /** Simulation speed multiplier: the 2x button, or dev keys up to 4x. */
    private var speed = 1

    private val buy = Button(Rectangle(160f, 320f, 400f, 100f), "")
    private val undo = Button(Rectangle(572f, 320f, 124f, 100f), S["Desfazer"], color = Skin.PANEL)
    private val speedButton = Button(Rectangle(528f, 1182f, 76f, 56f), "1x", color = Skin.PANEL)
    private val pauseButton = Button(Rectangle(616f, 1182f, 84f, 56f), "", color = Skin.PANEL)
    private val powerButtons = match.deck.mapIndexed { i, t ->
        t to Button(Rectangle(26f + i * 136f, 50f, 124f, 120f), "", color = Skin.color(t))
    }
    private val resume = Button(Rectangle(160f, 720f, 400f, 100f), S["Continuar"])
    private val restart = Button(Rectangle(160f, 590f, 400f, 100f), S["Reiniciar"], color = Skin.PANEL)
    private val quit = Button(Rectangle(160f, 460f, 400f, 100f), S["Sair"], color = DANGER)
    private val yes = Button(Rectangle(160f, 560f, 400f, 100f), S["Sim"], color = DANGER)
    private val no = Button(Rectangle(160f, 690f, 400f, 100f), S["Não"])
    private val skipTutorial = Button(Rectangle(540f, 1080f, 140f, 56f), S["Pular"], color = Skin.PANEL)
    private val allButtons = listOf(buy, undo, speedButton, pauseButton, resume, restart, quit, yes, no, skipTutorial) + powerButtons.map { it.second }

    override val skyMap get() = match.map

    /** App going to the background: stop, drop any gesture, and save the match and record in case we're killed. */
    override fun pause() {
        paused = true
        onTouchCancelled()
        if (game.devMode || match.isGameOver) return
        val day = match.challengeDay
        if (day != null) game.save.recordDaily(day, match.wave) else game.save.recordWave(match.map, match.wave, match.mode)
        game.save.saveMatch(match.snapshot(), previousBest)
    }

    override fun draw(delta: Float) {
        if (!paused) {
            clock += delta
            val countdownBefore = match.waveCountdown
            match.update(delta * speed * effects.timeScale())
            renderer.update(match, delta)
            for (s in match.lastShots) {
                val cell = Layout.cellAt(s.from) ?: continue
                val target = s.to.firstOrNull() ?: continue
                effects.kick(cell, Vec2(target.x - s.from.x, target.y - s.from.y))
            }
            val events = match.drainEvents()
            effects.consume(events)
            stats.consume(events)
            stats.observe(match)
            tutorial.consume(events)
            tutorial.update(delta)
            if (tutorial.finished && !game.save.tutorialDone) game.save.tutorialDone = true
            effects.update(delta)
            effects.lastLife = match.lives == 1 && match.mode != GameMode.SURVIVAL && !match.isGameOver
            spCounter.update(delta, match.sp)
            val names = sounds.plan(events, clock).toMutableSet()
            sounds.countdown(countdownBefore, match.waveCountdown)?.let {
                names += it
                effects.hudPulse("tick")
            }
            game.sfx.play(names)
            if (match.isGameOver) {
                finish()
                return
            }
        }
        refreshButtons()
        allButtons.forEach { it.pressed = buttonTap.current === it }

        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        renderer.drawShapes(match, drag, effects)
        drawVignette(effects.vignetteAlpha(), effects.vignetteColor())
        buy.drawShape(shapes)
        if (match.canUndo) undo.drawShape(shapes)
        speedButton.drawShape(shapes)
        pauseButton.drawShape(shapes)
        shapes.color = Color.WHITE
        val pc = pauseButton.rect
        shapes.rect(pc.x + pc.width / 2 - 12f, pc.y + 18f, 8f, 24f)
        shapes.rect(pc.x + pc.width / 2 + 4f, pc.y + 18f, 8f, 24f)
        for ((t, b) in powerButtons) {
            b.drawShape(shapes)
            val base = if (b.enabled) Skin.color(t) else Color(0.3f, 0.3f, 0.34f, 1f)
            shapes.glyph(t, b.rect.x + b.rect.width / 2, b.rect.y + 62f, 30f, Skin.glyphColor(t, 0.25f), base)
        }
        val info = heldDie()
        if (info != null) {
            val t = info.second.type
            shapes.color = Skin.SHADOW
            shapes.roundedRect(INFO.x + 3f, INFO.y - 8f, INFO.width, INFO.height, 20f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(INFO.x, INFO.y, INFO.width, INFO.height, 20f)
            shapes.color = Skin.color(t)
            shapes.roundedRect(INFO.x, INFO.y + INFO.height - 10f, INFO.width, 10f, 5f)
            shapes.glyph(t, INFO.x + 44f, INFO.y + INFO.height - 44f, 18f, Skin.color(t), Skin.PANEL)
        }
        shapes.end()

        val batch = game.batch
        batch.begin()
        renderer.drawHud(match, effects, spCounter.shown)
        effects.drawText(batch, game.smallFont, game.bigFont, game.font)
        if (game.devMode) {
            game.smallFont.color = Color.ORANGE
            batch.textCentered(game.smallFont, "DEV ${speed}x  N B S L H K F", 360f, 1260f)
            game.smallFont.color = Color.WHITE
        }
        buy.drawText(batch, game.font)
        if (match.canUndo) undo.drawText(batch, game.smallFont)
        speedButton.drawText(batch, game.font)
        for ((t, b) in powerButtons) {
            val cx = b.rect.x + b.rect.width / 2
            val cost = match.powerUpCost(t)
            val ink = if (b.enabled) Skin.pipColor(t) else Color.LIGHT_GRAY
            game.smallFont.color = ink
            game.font.color = ink
            batch.textCentered(game.smallFont, Skin.label(t), cx, b.rect.y + 100f)
            batch.textCentered(game.font, S.f("Nv%d", match.powerLevel(t)), cx, b.rect.y + 62f)
            batch.textCentered(game.smallFont, cost?.toString() ?: S["MAX"], cx, b.rect.y + 26f)
        }
        game.smallFont.color = Color.WHITE
        game.font.color = Color.WHITE
        if (info != null) drawDieInfo(info.first, info.second)
        batch.end()

        // The balloon goes over the HUD text, so it gets its own pass.
        if (tutorial.step != null) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawTutorialShapes()
            shapes.end()
            batch.begin()
            drawTutorialText()
            batch.end()
        }

        if (paused) drawPauseOverlay()
    }

    /** Darkens the screen edges: strips that fade inward, plus solid cover beyond the world for wide screens. */
    private fun drawVignette(alpha: Float, color: Color) {
        if (alpha <= 0f) return
        val shapes = game.shapes
        val solid = vignetteColor.set(color).also { it.a = alpha }
        val clear = vignetteClear.set(color).also { it.a = 0f }
        val d = VIGNETTE_DEPTH
        val w = Layout.WORLD_W
        val h = Layout.WORLD_H
        shapes.rect(0f, 0f, d, h, solid, clear, clear, solid)
        shapes.rect(w - d, 0f, d, h, clear, solid, solid, clear)
        shapes.rect(0f, 0f, w, d, solid, solid, clear, clear)
        shapes.rect(0f, h - d, w, d, clear, clear, solid, solid)
        shapes.color = solid
        shapes.rect(-400f, -400f, 400f, h + 800f)
        shapes.rect(w, -400f, 400f, h + 800f)
        shapes.rect(0f, -400f, w, 400f)
        shapes.rect(0f, h, w, 400f)
    }

    /** The balloon's panel plus a pulsing gold ring around the step's target; the balloon sits in the sky. */
    private fun drawTutorialShapes() {
        val step = tutorial.step ?: return
        val shapes = game.shapes
        val pulse = 0.5f + 0.5f * kotlin.math.sin(game.clock * 5f)
        val target: Rectangle? = when (step.target) {
            Tutorial.Target.BUY -> buy.rect
            Tutorial.Target.BOARD -> MatchRenderer.BOARD
            Tutorial.Target.POWER -> POWER_ROW
            null -> null
        }
        if (target != null) {
            shapes.color = tmpColor.set(Skin.GOLD).also { it.a = 0.45f + 0.5f * pulse }
            val pad = 8f + 6f * pulse
            shapes.ring(target.x - pad, target.y - pad, target.width + 2 * pad, target.height + 2 * pad, 5f)
        }
        shapes.color = Skin.SHADOW
        shapes.roundedRect(TUTORIAL.x + 3f, TUTORIAL.y - 8f, TUTORIAL.width, TUTORIAL.height, 20f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(TUTORIAL.x, TUTORIAL.y, TUTORIAL.width, TUTORIAL.height, 20f)
        shapes.color = Skin.GOLD
        shapes.roundedRect(TUTORIAL.x, TUTORIAL.y + TUTORIAL.height - 8f, TUTORIAL.width, 8f, 4f)
        if (step != Tutorial.Step.DONE) skipTutorial.drawShape(shapes)
    }

    private fun drawTutorialText() {
        val step = tutorial.step ?: return
        game.smallFont.draw(game.batch, S[step.text], TUTORIAL.x + 24f, TUTORIAL.y + TUTORIAL.height - 30f, TUTORIAL.width - 190f, Align.left, true)
        if (step != Tutorial.Step.DONE) skipTutorial.drawText(game.batch, game.smallFont)
    }

    override fun cameraShake(): Vec2 = if (paused) super.cameraShake() else effects.shakeOffset()

    /** The die under a finger held still on the board, with its cell. */
    private fun heldDie(): Pair<Cell, Die>? {
        if (!hold.isHeld(game.clock)) return null
        val cell = drag.dragging ?: return null
        return cell to (match.board[cell] ?: return null)
    }

    /** Name, pips, the numbers this very die hits for right now, and what it does. */
    private fun drawDieInfo(cell: Cell, die: Die) {
        val batch = game.batch
        val t = die.type
        val left = INFO.x + 24f
        var y = INFO.y + INFO.height - 36f
        val stacks = if (t == DieType.HUNTER) S.f(" · %d stacks", die.stacks) else ""
        val pips = S[if (die.pips == 1) "pip" else "pips"]
        game.font.draw(batch, S.f("%s · %d %s%s", Skin.fullName(t), die.pips, pips, stacks), left + 48f, y + 8f)
        y -= 44f
        if (t.attacks) {
            val dmg = Combat.baseDamage(t, die.pips, match.powerLevel(t))
            val every = Combat.attackInterval(t, die.pips) / match.speedMultiplier(cell)
            game.smallFont.color = Skin.GOLD
            game.smallFont.draw(batch, S.f("Dano %s  ·  a cada %s s  ·  Nv%d", fmt(dmg), fmt(every), match.powerLevel(t)), left, y)
            game.smallFont.color = Color.WHITE
            y -= 34f
        }
        val info = DieInfo.of(t)
        y -= game.smallFont.draw(batch, info.summary, left, y, INFO.width - 48f, Align.left, true).height + 10f
        game.smallFont.color = KEY_COLOR
        y -= game.smallFont.draw(batch, info.rows.first { it.first == S["Efeito"] }.second, left, y, INFO.width - 48f, Align.left, true).height + 8f
        if (die.pips >= Balance.MAX_PIPS) {
            game.smallFont.color = Skin.GOLD
            game.smallFont.draw(batch, S.f("7 pips: %s", info.rows.first { it.first == S["7 pips"] }.second), left, y, INFO.width - 48f, Align.left, true)
        }
        game.smallFont.color = Color.WHITE
    }

    private fun fmt(f: Float): String =
        if (f == f.toInt().toFloat()) "${f.toInt()}" else "%.2f".format(f).trimEnd('0').trimEnd('.', ',').replace('.', ',')

    private fun refreshButtons() {
        buy.text = S.f("Comprar (%d)", match.buyCost)
        buy.enabled = match.canBuy()
        // Breathes while you can afford a die.
        buy.scale = if (buy.enabled && !paused) 1f + 0.02f + 0.02f * sin(clock * 6f) else 1f
        speedButton.text = "${speed}x"
        for ((t, b) in powerButtons) {
            val cost = match.powerUpCost(t)
            b.enabled = cost != null && match.sp >= cost
            b.scale = 1f + 0.08f * effects.typeGlow(t)
        }
    }

    private fun drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.7f)
        shapes.rect(-400f, -400f, Layout.WORLD_W + 800f, Layout.WORLD_H + 800f)
        for (b in pauseButtons()) b.drawShape(shapes)
        shapes.end()
        game.batch.begin()
        val c = confirm
        if (c == null) {
            game.batch.textShadowed(game.bigFont, S["Pausa"], 360f, 920f)
        } else {
            game.batch.textShadowed(game.font, S[c.question], 360f, 900f)
            game.batch.textCentered(game.smallFont, S["O progresso desta partida será perdido."], 360f, 850f)
        }
        for (b in pauseButtons()) b.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun pauseButtons(): List<Button> = if (confirm != null) listOf(no, yes) else listOf(resume, restart, quit)

    private fun buttonAt(p: Vec2): Button? {
        val visible = if (paused) pauseButtons() else listOf(buy, speedButton, pauseButton) + powerButtons.map { it.second } +
            listOfNotNull(undo.takeIf { match.canUndo }) +
            listOfNotNull(skipTutorial.takeIf { tutorial.step != null && tutorial.step != Tutorial.Step.DONE })
        return visible.firstOrNull { it.contains(p.x, p.y) }
    }

    override fun onTouchDown(p: Vec2) {
        if (!paused && drag.down(p)) {
            hold.down(p, game.clock)
            return
        }
        if (!paused) {
            // A tap on something you can't afford says no instead of doing nothing.
            val denied = (listOf(buy) + powerButtons.map { it.second }).firstOrNull { !it.enabled && it.rect.contains(p.x, p.y) }
            if (denied != null) {
                denied.deny()
                game.sfx.play(setOf("deny"))
            }
        }
        buttonTap.press(buttonAt(p))
    }

    override fun onDrag(p: Vec2) {
        drag.move(p)
        hold.move(p)
    }

    override fun onTouchUp(p: Vec2) {
        hold.up()
        val from = drag.dragging
        if (from != null) {
            val die = match.board[from]
            val to = drag.dropTarget(p)
            val result = if (to != null && match.canMerge(from, to)) match.board.resultCell(from, to) else null
            if (drag.up(p) && die != null && result != null && result != from) effects.slide(from, result, die.type, die.pips)
            return
        }
        when (val b = buttonTap.release(buttonAt(p))) {
            null -> Unit
            resume -> paused = false
            restart -> confirm = Confirm.RESTART
            quit -> confirm = Confirm.QUIT
            no -> confirm = null
            yes -> when (confirm) {
                Confirm.RESTART -> abandon(restartAfter = true)
                Confirm.QUIT -> abandon(restartAfter = false)
                null -> Unit
            }
            buy -> match.buyDie()
            undo -> match.undoMerge()
            speedButton -> speed = if (speed == 1) 2 else 1
            skipTutorial -> tutorial.skip()
            pauseButton -> {
                paused = true
                confirm = null
            }
            else -> powerButtons.firstOrNull { it.second === b }?.let { match.powerUp(it.first) }
        }
    }

    override fun onTouchCancelled() {
        drag.cancel()
        hold.up()
        buttonTap.cancel()
    }

    /** Desktop cheats: N next wave, B boss, S +500 SP, L +1 life, H lose a life, K kill all, F speed 1x/2x/4x. */
    override fun onKey(keycode: Int) {
        if (!game.devMode || paused) return
        when (keycode) {
            Input.Keys.N -> match.devNextWave()
            Input.Keys.B -> match.devBossWave()
            Input.Keys.S -> match.devAddSp(500)
            Input.Keys.L -> match.devAddLife()
            Input.Keys.H -> match.devLeak()
            Input.Keys.K -> match.devKillAll()
            Input.Keys.F -> speed = if (speed >= 4) 1 else speed * 2
        }
    }

    /** Leaves the match on purpose: the wave reached still counts for the record. */
    private fun abandon(restartAfter: Boolean) {
        if (restartAfter) {
            book()
            if (match.challengeDay != null) game.startDaily() else game.startMatch()
        } else {
            finish()
        }
    }

    /** Clears the saved match and, outside dev mode, books the result (totals, the relevant record, achievements). */
    private fun book(): Set<Achievement> {
        game.save.clearMatch()
        if (game.devMode) return emptySet()
        game.save.recordMatch(match.map, match.deck, match.wave, stats.kills, stats.bosses, stats.merges, match.challengeDay, match.mode, match.locked)
        game.save.addHistory(
            MatchRecord(
                DailyChallenge.today(), match.map, match.mode, match.deck, match.wave, stats.killedBy,
                match.setup?.takeIf { match.challengeDay == null }?.let { MatchCode.encode(it) } ?: "",
            ),
        )
        val t = game.save.totals
        val progress = AchievementProgress(
            wave = match.wave, mode = match.mode, kills = t.kills, bosses = t.bosses, merges = t.merges, games = t.games,
            maxPips = stats.maxPips, synergy = stats.synergyUsed, hunterStacks = stats.hunterStacks,
            daily = match.challengeDay != null, undo = stats.undoUsed, rich = stats.rich, mapsAtTen = game.save.mapsAt(10),
        )
        return game.save.unlock(Achievements.earned(progress))
    }

    private fun finish() {
        val fresh = book()
        game.showGameOver(match.map, match.wave, !game.devMode && match.wave > previousBest, stats, match.challengeDay, match.mode, fresh, match.setup?.takeIf { match.challengeDay == null }?.let { MatchCode.encode(it) })
    }

    private companion object {
        const val VIGNETTE_DEPTH = 190f
        val DANGER = Color(0.7f, 0.25f, 0.25f, 1f)
        val KEY_COLOR = Color(0.75f, 0.77f, 0.85f, 1f)
        /** Die info card, in the sky above the board. */
        val INFO = Rectangle(24f, 840f, 672f, 270f)
        /** Tutorial balloon, same place; the two never show together for long. */
        val TUTORIAL = Rectangle(24f, 1000f, 672f, 150f)
        val POWER_ROW = Rectangle(26f, 50f, 668f, 120f)
        val tmpColor = Color()
    }
}
