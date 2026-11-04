package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Mission

/** The day's missions as a panel of rows with a bar each: on the home tab and in the match's pause menu. */
class MissionsPanel(private val game: MergeTdGame, val rect: Rectangle, private val day: Int, private val missions: List<Mission>) {
    private val rows = missions.mapIndexed { i, _ ->
        Rectangle(rect.x + 16f, rect.y + rect.height - 74f - (i + 1) * 112f, rect.width - 32f, 102f)
    }

    fun drawShapes(progress: List<Int>) {
        val shapes = game.shapes
        shapes.color = Skin.SHADOW
        shapes.roundedRect(rect.x + 3f, rect.y - 8f, rect.width, rect.height, 20f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(rect.x, rect.y, rect.width, rect.height, 20f)
        for ((i, m) in missions.withIndex()) {
            val r = rows[i]
            val done = progress[i] >= m.target
            shapes.color = if (done) ROW_DONE else Skin.CELL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 16f)
            shapes.color = if (done) Skin.GOLD else Color(0.3f, 0.32f, 0.4f, 1f)
            shapes.circle(r.x + 26f, r.y + r.height - 26f, 9f, 16)
            bar(shapes, r.x + 20f, r.y + 18f, r.width - 220f, 14f, progress[i].toFloat() / m.target, if (done) Skin.GOLD else Skin.color(DieType.ELECTRIC))
        }
    }

    fun drawText(progress: List<Int>) {
        val batch = game.batch
        game.font.color = Skin.GOLD
        batch.textLeft(game.font, S["Missões de hoje"], rect.x + 24f, rect.y + rect.height - 32f)
        game.font.color = Color.WHITE
        game.tinyFont.color = KEY_COLOR
        batch.textRight(game.tinyFont, "${DailyChallenge.of(day).dateLabel} · ${S["Renova amanhã"]}", rect.x + rect.width - 24f, rect.y + rect.height - 32f)
        game.tinyFont.color = Color.WHITE
        for ((i, m) in missions.withIndex()) {
            val r = rows[i]
            val done = progress[i] >= m.target
            game.smallFont.draw(batch, m.text(), r.x + 44f, r.y + r.height - 14f, r.width - 64f, Align.left, true)
            game.tinyFont.color = KEY_COLOR
            batch.textLeft(game.tinyFont, "${minOf(progress[i], m.target)}/${m.target}", r.x + r.width - 186f, r.y + 25f)
            game.tinyFont.color = Skin.GOLD
            batch.textRight(game.tinyFont, if (done) S["Concluída"] else S.f("+%d XP", m.xp), r.x + r.width - 20f, r.y + 25f)
            game.tinyFont.color = Color.WHITE
        }
    }

    private fun bar(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, frac: Float, color: Color) {
        shapes.color = Color(0.12f, 0.13f, 0.18f, 1f)
        shapes.roundedRect(x, y, w, h, h / 2)
        val f = frac.coerceIn(0f, 1f)
        if (f > 0f) {
            shapes.color = color
            shapes.roundedRect(x, y, maxOf(h, w * f), h, h / 2)
        }
    }

    companion object {
        /** Height that fits the header and three rows. */
        const val HEIGHT = 440f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        val ROW_DONE = Color(0.20f, 0.22f, 0.30f, 1f)
    }
}
