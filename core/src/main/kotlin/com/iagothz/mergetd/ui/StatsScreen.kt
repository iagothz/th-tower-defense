package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Vec2

/** Lifetime totals, the record on each map, the best decks and the last daily challenges. */
class StatsScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.SETTINGS

    private val back = Button(Rectangle(474f, 124f, 206f, 80f), S["Voltar"], color = Skin.PANEL)
    private val achievements = Button(Rectangle(40f, 124f, 206f, 80f), S["Conquistas"])
    private val history = Button(Rectangle(257f, 124f, 206f, 80f), S["Histórico"])
    private val tap = Tap<Button>()
    private val totalsPanel = Rectangle(24f, 1010f, 672f, 170f)
    private val mapsPanel = Rectangle(24f, 850f, 672f, 140f)
    private val decksPanel = Rectangle(24f, 560f, 672f, 270f)
    private val dailyPanel = Rectangle(24f, 316f, 672f, 224f)
    private val totals = game.save.totals
    private val decks = game.save.deckRecords().take(4)
    private val daily = game.save.dailyHistory().take(4)

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        achievements.pressed = tap.current === achievements
        history.pressed = tap.current === history
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (p in listOf(totalsPanel, mapsPanel, decksPanel, dailyPanel)) {
            shapes.color = Skin.SHADOW
            shapes.roundedRect(p.x + 3f, p.y - 8f, p.width, p.height, 18f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(p.x, p.y, p.width, p.height, 18f)
        }
        for ((i, entry) in decks.withIndex()) {
            val y = decksPanel.y + decksPanel.height - 84f - i * 54f
            for ((j, t) in entry.first.withIndex()) shapes.die(t, 1, decksPanel.x + 52f + j * 50f, y, 40f)
        }
        back.drawShape(shapes)
        achievements.drawShape(shapes)
        history.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Estatísticas"], 360f, 1222f)
        header(totalsPanel, S["Total"])
        val t = listOf(
            S["Partidas"] to "${totals.games}", S["Ondas somadas"] to "${totals.waves}",
            S["Abates"] to "${totals.kills}", S["Chefes"] to "${totals.bosses}",
            S["Merges"] to "${totals.merges}", S["Média de ondas"] to if (totals.games == 0) "–" else "%.1f".format(totals.waves.toFloat() / totals.games).replace('.', ','),
        )
        for ((i, kv) in t.withIndex()) {
            val x = totalsPanel.x + 24f + (i % 2) * 336f
            val y = totalsPanel.y + totalsPanel.height - 66f - (i / 2) * 38f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, kv.first, x, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.smallFont, kv.second, x + 300f, y)
        }
        header(mapsPanel, S["Recorde por mapa"])
        for ((i, m) in GameMap.entries.withIndex()) {
            val x = mapsPanel.x + 24f + (i % 2) * 336f
            val y = mapsPanel.y + mapsPanel.height - 66f - (i / 2) * 38f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, S[m.title], x, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.smallFont, S.f("onda %d", game.save.bestWave(m)), x + 300f, y)
        }
        header(decksPanel, S["Melhores decks"])
        if (decks.isEmpty()) empty(decksPanel, S["Jogue uma partida para aparecer aqui."])
        for ((i, entry) in decks.withIndex()) {
            val y = decksPanel.y + decksPanel.height - 84f - i * 54f
            game.batch.textRight(game.font, S.f("onda %d", entry.second), decksPanel.x + decksPanel.width - 28f, y)
        }
        header(dailyPanel, S["Desafio do dia"])
        if (daily.isEmpty()) empty(dailyPanel, S["Nenhum desafio jogado ainda."])
        for ((i, entry) in daily.withIndex()) {
            val y = dailyPanel.y + dailyPanel.height - 70f - i * 44f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, DailyChallenge.of(entry.first).dateLabel, dailyPanel.x + 24f, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.font, S.f("onda %d", entry.second), dailyPanel.x + dailyPanel.width - 28f, y)
        }
        back.drawText(game.batch, game.smallFont)
        achievements.drawText(game.batch, game.smallFont)
        history.drawText(game.batch, game.smallFont)
        game.batch.end()
    }

    private fun header(p: Rectangle, title: String) {
        game.font.color = Skin.GOLD
        game.batch.textLeft(game.font, title, p.x + 24f, p.y + p.height - 28f)
        game.font.color = Color.WHITE
    }

    private fun empty(p: Rectangle, text: String) {
        game.smallFont.color = KEY_COLOR
        game.batch.textLeft(game.smallFont, text, p.x + 24f, p.y + p.height - 72f)
        game.smallFont.color = Color.WHITE
    }

    private fun buttonAt(p: Vec2) = listOf(back, achievements, history).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            back -> game.showSettings()
            achievements -> game.showAchievements()
            history -> game.showHistory()
        }
    }

    private companion object {
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
