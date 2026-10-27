package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Vec2

/** Lifetime totals, the record on each map, the best decks and the last daily challenges. */
class StatsScreen(game: MergeTdGame) : BaseScreen(game) {
    private val back = Button(Rectangle(370f, 30f, 310f, 100f), "Voltar", color = Skin.PANEL)
    private val achievements = Button(Rectangle(40f, 30f, 310f, 100f), "Conquistas")
    private val tap = Tap<Button>()
    private val totalsPanel = Rectangle(24f, 1010f, 672f, 170f)
    private val mapsPanel = Rectangle(24f, 840f, 672f, 150f)
    private val decksPanel = Rectangle(24f, 490f, 672f, 330f)
    private val dailyPanel = Rectangle(24f, 160f, 672f, 310f)
    private val totals = game.save.totals
    private val decks = game.save.deckRecords().take(5)
    private val daily = game.save.dailyHistory().take(5)

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        achievements.pressed = tap.current === achievements
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
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Estatísticas", 360f, 1222f)
        header(totalsPanel, "Total")
        val t = listOf(
            "Partidas" to "${totals.games}", "Ondas somadas" to "${totals.waves}",
            "Abates" to "${totals.kills}", "Chefes" to "${totals.bosses}",
            "Merges" to "${totals.merges}", "Média de ondas" to if (totals.games == 0) "–" else "%.1f".format(totals.waves.toFloat() / totals.games).replace('.', ','),
        )
        for ((i, kv) in t.withIndex()) {
            val x = totalsPanel.x + 24f + (i % 2) * 336f
            val y = totalsPanel.y + totalsPanel.height - 66f - (i / 2) * 38f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, kv.first, x, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.smallFont, kv.second, x + 300f, y)
        }
        header(mapsPanel, "Recorde por mapa")
        for ((i, m) in GameMap.entries.withIndex()) {
            val x = mapsPanel.x + 24f + (i % 2) * 336f
            val y = mapsPanel.y + mapsPanel.height - 66f - (i / 2) * 38f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, m.title, x, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.smallFont, "onda ${game.save.bestWave(m)}", x + 300f, y)
        }
        header(decksPanel, "Melhores decks")
        if (decks.isEmpty()) empty(decksPanel, "Jogue uma partida para aparecer aqui.")
        for ((i, entry) in decks.withIndex()) {
            val y = decksPanel.y + decksPanel.height - 84f - i * 54f
            game.batch.textRight(game.font, "onda ${entry.second}", decksPanel.x + decksPanel.width - 28f, y)
        }
        header(dailyPanel, "Desafio do dia")
        if (daily.isEmpty()) empty(dailyPanel, "Nenhum desafio jogado ainda.")
        for ((i, entry) in daily.withIndex()) {
            val y = dailyPanel.y + dailyPanel.height - 70f - i * 44f
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, DailyChallenge.of(entry.first).dateLabel, dailyPanel.x + 24f, y)
            game.smallFont.color = Color.WHITE
            game.batch.textRight(game.font, "onda ${entry.second}", dailyPanel.x + dailyPanel.width - 28f, y)
        }
        back.drawText(game.batch, game.font)
        achievements.drawText(game.batch, game.font)
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

    private fun buttonAt(p: Vec2) = listOf(back, achievements).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            back -> game.showMenu()
            achievements -> game.showAchievements()
        }
    }

    private companion object {
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
