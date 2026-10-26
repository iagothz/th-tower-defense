package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

class MatchScreen(game: MergeTdGame) : BaseScreen(game) {
    private val match = Match(game.save.deck)
    private val drag = DragController(match)
    private val renderer = MatchRenderer(game.shapes, game.batch, game.font)
    private var paused = false

    private val buy = Button(Rectangle(160f, 320f, 400f, 100f), "")
    private val pauseButton = Button(Rectangle(620f, 1170f, 80f, 80f), "II", color = Skin.PANEL)
    private val powerButtons = match.deck.mapIndexed { i, t ->
        t to Button(Rectangle(26f + i * 136f, 50f, 124f, 120f), "", color = Skin.color(t))
    }
    private val resume = Button(Rectangle(160f, 700f, 400f, 110f), "Continuar")
    private val quit = Button(Rectangle(160f, 540f, 400f, 110f), "Sair", color = Color(0.7f, 0.25f, 0.25f, 1f))

    override fun pause() {
        paused = true
    }

    override fun draw(delta: Float) {
        if (!paused) {
            match.update(delta)
            renderer.update(match, delta)
            if (match.isGameOver) {
                finish()
                return
            }
        }
        refreshButtons()

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        renderer.drawShapes(match, drag)
        buy.drawShape(shapes)
        pauseButton.drawShape(shapes)
        powerButtons.forEach { it.second.drawShape(shapes) }
        shapes.end()

        val batch = game.batch
        batch.begin()
        renderer.drawHud(match)
        buy.drawText(batch, game.font)
        pauseButton.drawText(batch, game.font)
        for ((t, b) in powerButtons) {
            val cx = b.rect.x + b.rect.width / 2
            val cost = match.powerUpCost(t)
            batch.textCentered(game.font, "Nv${match.powerLevel(t)}", cx, b.rect.y + 85f)
            batch.textCentered(game.font, cost?.toString() ?: "MAX", cx, b.rect.y + 35f)
        }
        batch.end()

        if (paused) drawPauseOverlay()
    }

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

    override fun onTouchDown(p: Vec2) {
        if (!paused) drag.down(p)
    }

    override fun onDrag(p: Vec2) {
        drag.move(p)
    }

    override fun onTouchUp(p: Vec2) {
        if (paused) {
            when {
                resume.contains(p.x, p.y) -> paused = false
                quit.contains(p.x, p.y) -> finish()
            }
            return
        }
        if (drag.dragging != null) {
            drag.up(p)
            return
        }
        when {
            buy.contains(p.x, p.y) -> match.buyDie()
            pauseButton.contains(p.x, p.y) -> paused = true
            else -> powerButtons.firstOrNull { it.second.contains(p.x, p.y) }?.let { match.powerUp(it.first) }
        }
    }

    private fun finish() {
        val isRecord = match.wave > game.save.bestWave
        if (isRecord) game.save.bestWave = match.wave
        game.showGameOver(match.wave, isRecord)
    }
}
