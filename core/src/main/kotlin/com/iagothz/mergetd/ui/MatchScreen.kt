package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

/**
 * [restored] is a match saved when the app last went to the background; it opens paused.
 * [previousBest] is the record from before that match began, so "Novo recorde!" still shows after a restore.
 */
class MatchScreen(
    game: MergeTdGame,
    restored: Match? = null,
    private val previousBest: Int = game.save.bestWave,
) : BaseScreen(game) {
    private val match = restored ?: Match(game.save.deck)
    private val drag = DragController(match)
    private val renderer = MatchRenderer(game.shapes, game.batch, game.font, game.smallFont)
    private val buttonTap = Tap<Button>()
    private val effects = Effects()
    private val sounds = SoundPlanner()
    private var clock = 0f
    private var paused = restored != null
    /** Simulation speed multiplier; only changed by dev-mode keys. */
    private var speed = 1

    private val buy = Button(Rectangle(160f, 320f, 400f, 100f), "")
    private val pauseButton = Button(Rectangle(620f, 1182f, 80f, 56f), "", color = Skin.PANEL)
    private val powerButtons = match.deck.mapIndexed { i, t ->
        t to Button(Rectangle(26f + i * 136f, 50f, 124f, 120f), "", color = Skin.color(t))
    }
    private val resume = Button(Rectangle(160f, 700f, 400f, 110f), "Continuar")
    private val quit = Button(Rectangle(160f, 540f, 400f, 110f), "Sair", color = Color(0.7f, 0.25f, 0.25f, 1f))
    private val allButtons = listOf(buy, pauseButton, resume, quit) + powerButtons.map { it.second }

    /** App going to the background: stop, drop any gesture, and save the match and record in case we're killed. */
    override fun pause() {
        paused = true
        onTouchCancelled()
        if (game.devMode || match.isGameOver) return
        game.save.recordWave(match.wave)
        game.save.saveMatch(match.snapshot(), previousBest)
    }

    override fun draw(delta: Float) {
        if (!paused) {
            clock += delta
            match.update(delta * speed)
            renderer.update(match, delta)
            val events = match.drainEvents()
            effects.consume(events)
            effects.update(delta)
            game.sfx.play(sounds.plan(events, clock))
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
        buy.drawShape(shapes)
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
        shapes.end()

        val batch = game.batch
        batch.begin()
        renderer.drawHud(match)
        effects.drawText(batch, game.smallFont, game.bigFont, game.font)
        if (game.devMode) {
            game.smallFont.color = Color.ORANGE
            batch.textCentered(game.smallFont, "DEV ${speed}x  N B S L K F", 360f, 1260f)
            game.smallFont.color = Color.WHITE
        }
        buy.drawText(batch, game.font)
        pauseButton.drawText(batch, game.font)
        for ((t, b) in powerButtons) {
            val cx = b.rect.x + b.rect.width / 2
            val cost = match.powerUpCost(t)
            val ink = if (b.enabled) Skin.pipColor(t) else Color.LIGHT_GRAY
            game.smallFont.color = ink
            game.font.color = ink
            batch.textCentered(game.smallFont, Skin.label(t), cx, b.rect.y + 100f)
            batch.textCentered(game.font, "Nv${match.powerLevel(t)}", cx, b.rect.y + 62f)
            batch.textCentered(game.smallFont, cost?.toString() ?: "MAX", cx, b.rect.y + 26f)
        }
        game.smallFont.color = Color.WHITE
        game.font.color = Color.WHITE
        batch.end()

        if (paused) drawPauseOverlay()
    }

    override fun cameraShake(): Vec2 = if (paused) super.cameraShake() else effects.shakeOffset()

    private fun refreshButtons() {
        buy.text = "Comprar (${match.buyCost})"
        buy.enabled = match.canBuy()
        for ((t, b) in powerButtons) {
            val cost = match.powerUpCost(t)
            b.enabled = cost != null && match.sp >= cost
        }
    }

    private fun drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.7f)
        shapes.rect(0f, 0f, 720f, 1280f)
        resume.drawShape(shapes)
        quit.drawShape(shapes)
        shapes.end()
        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Pausa", 360f, 920f)
        resume.drawText(game.batch, game.font)
        quit.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun buttonAt(p: Vec2): Button? {
        val visible = if (paused) listOf(resume, quit) else listOf(buy, pauseButton) + powerButtons.map { it.second }
        return visible.firstOrNull { it.contains(p.x, p.y) }
    }

    override fun onTouchDown(p: Vec2) {
        if (paused || !drag.down(p)) buttonTap.press(buttonAt(p))
    }

    override fun onDrag(p: Vec2) {
        drag.move(p)
    }

    override fun onTouchUp(p: Vec2) {
        if (drag.dragging != null) {
            drag.up(p)
            return
        }
        when (val b = buttonTap.release(buttonAt(p))) {
            null -> Unit
            resume -> paused = false
            quit -> finish()
            buy -> match.buyDie()
            pauseButton -> paused = true
            else -> powerButtons.firstOrNull { it.second === b }?.let { match.powerUp(it.first) }
        }
    }

    override fun onTouchCancelled() {
        drag.cancel()
        buttonTap.cancel()
    }

    /** Desktop cheats: N next wave, B boss, S +500 SP, L +1 life, K kill all, F speed 1x/2x/4x. */
    override fun onKey(keycode: Int) {
        if (!game.devMode || paused) return
        when (keycode) {
            Input.Keys.N -> match.devNextWave()
            Input.Keys.B -> match.devBossWave()
            Input.Keys.S -> match.devAddSp(500)
            Input.Keys.L -> match.devAddLife()
            Input.Keys.K -> match.devKillAll()
            Input.Keys.F -> speed = if (speed >= 4) 1 else speed * 2
        }
    }

    private fun finish() {
        game.save.clearMatch()
        if (!game.devMode) game.save.recordWave(match.wave)
        game.showGameOver(match.wave, !game.devMode && match.wave > previousBest)
    }
}
