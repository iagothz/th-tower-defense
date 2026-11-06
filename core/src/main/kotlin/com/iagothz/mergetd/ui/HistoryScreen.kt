package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.Vec2

/** The last twenty matches: when, where, with what, how far, and the code to play them again. */
class HistoryScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.SETTINGS

    private val records = game.save.history()
    private val rows = records.mapIndexed { i, r -> r to Rectangle(24f, 1124f - i * 48f, 672f, 44f) }
    private val back = Button(Rectangle(160f, 120f, 400f, 80f), S["Voltar"], style = ButtonStyle.SECONDARY)
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((r, rect) in rows) {
            shapes.color = Skin.PANEL
            shapes.roundedRect(rect.x, rect.y, rect.width, rect.height, 12f)
            for ((j, t) in r.deck.withIndex()) shapes.die(t, 1, rect.x + 262f + j * 30f, rect.y + rect.height / 2, 24f)
        }
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Histórico"], 360f, 1222f)
        if (records.isEmpty()) {
            game.smallFont.color = Skin.MUTED
            game.batch.textCentered(game.smallFont, S["Nenhuma partida terminada ainda."], 360f, 1100f)
            game.smallFont.color = Color.WHITE
        }
        for ((r, rect) in rows) {
            val cy = rect.y + rect.height / 2
            game.tinyFont.color = Skin.MUTED
            game.batch.textLeft(game.tinyFont, DailyChallenge.of(r.day).dateLabel.dropLast(5), rect.x + 12f, cy + 9f)
            game.batch.textLeft(game.tinyFont, "${S[r.map.title]} · ${S[r.mode.title]}", rect.x + 12f, cy - 10f)
            game.batch.textLeft(game.tinyFont, r.killedBy?.let { Skin.enemyName(it) } ?: "–", rect.x + 430f, cy - 10f)
            game.batch.textLeft(game.tinyFont, r.code, rect.x + 430f, cy + 9f)
            game.tinyFont.color = Color.WHITE
            game.font.color = Skin.GOLD
            game.batch.textRight(game.font, "${r.wave}", rect.x + rect.width - 16f, cy)
            game.font.color = Color.WHITE
        }
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    override fun onTouchDown(p: Vec2) = tap.press(back.takeIf { it.contains(p.x, p.y) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        if (tap.release(back.takeIf { it.contains(p.x, p.y) }) === back) game.showStats()
    }
}
