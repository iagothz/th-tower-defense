package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DailyMissions
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind
import com.iagothz.mergetd.logic.Progression
import com.iagothz.mergetd.logic.Vec2

/** The mission sentence in the current language: "Mate 40 inimigos com Fogo". */
fun Mission.text(): String {
    val what = when (kind) {
        MissionKind.KILLS_WITH -> Skin.fullName(type!!)
        MissionKind.REACH_WAVE -> S[map!!.title]
        MissionKind.PLAY_MODE -> S[mode!!.title]
        else -> null
    }
    return when {
        !template.contains("%d") -> S[template]
        what == null -> S.f(template, target)
        else -> S.f(template, target, what)
    }
}

/** What a match paid: XP for the waves, the missions it completed, and whether that crossed a level. */
data class MatchReward(val xp: Int, val level: Int, val leveledUp: Boolean, val missions: List<Mission>)

/** Today's three missions with their progress, under the account level. */
class MissionsScreen(game: MergeTdGame) : BaseScreen(game) {
    private val day = DailyChallenge.today()
    private val missions = DailyMissions.of(day)
    private val progress = game.save.missionProgress(day, missions)
    private val cards = missions.mapIndexed { i, m -> m to Rectangle(24f, 860f - i * 190f, 672f, 170f) }
    private val levelPanel = Rectangle(24f, 1030f, 672f, 120f)
    private val back = Button(Rectangle(160f, 30f, 400f, 90f), S["Voltar"], color = Skin.PANEL)
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        val xp = game.save.xp
        val level = Progression.level(xp)
        val into = Progression.intoLevel(xp)
        val cost = Progression.costToNext(level)

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Skin.PANEL
        shapes.roundedRect(levelPanel.x, levelPanel.y, levelPanel.width, levelPanel.height, 20f)
        bar(levelPanel.x + 24f, levelPanel.y + 22f, levelPanel.width - 48f, 18f, into.toFloat() / cost, Skin.GOLD)
        for ((i, card) in cards.withIndex()) {
            val (m, r) = card
            val done = progress[i] >= m.target
            shapes.color = if (done) ROW_DONE else Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 20f)
            shapes.color = if (done) Skin.GOLD else Color(0.3f, 0.32f, 0.4f, 1f)
            shapes.circle(r.x + 30f, r.y + r.height - 30f, 10f, 16)
            bar(r.x + 24f, r.y + 30f, r.width - 240f, 16f, progress[i].toFloat() / m.target, if (done) Skin.GOLD else Skin.color(com.iagothz.mergetd.logic.DieType.ELECTRIC))
        }
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Missões"], 360f, 1222f)
        game.tinyFont.color = KEY_COLOR
        game.batch.textCentered(game.tinyFont, "${S["Missões de hoje"]} · ${DailyChallenge.of(day).dateLabel}", 360f, 1178f)
        game.tinyFont.color = Color.WHITE
        game.batch.textLeft(game.font, S.f("Nível %d · %s", level, S[Progression.title(level)]), levelPanel.x + 24f, levelPanel.y + 86f)
        game.smallFont.color = KEY_COLOR
        game.batch.textRight(game.smallFont, S.f("%d / %d XP", into, cost), levelPanel.x + levelPanel.width - 24f, levelPanel.y + 86f)
        game.smallFont.color = Color.WHITE
        for ((i, card) in cards.withIndex()) {
            val (m, r) = card
            val done = progress[i] >= m.target
            game.smallFont.color = if (done) Color.WHITE else Color(0.9f, 0.9f, 0.95f, 1f)
            game.smallFont.draw(game.batch, m.text(), r.x + 52f, r.y + r.height - 18f, r.width - 76f, Align.left, true)
            game.tinyFont.color = KEY_COLOR
            game.batch.textLeft(game.tinyFont, "${minOf(progress[i], m.target)}/${m.target}", r.x + r.width - 200f, r.y + 38f)
            game.tinyFont.color = Skin.GOLD
            game.batch.textRight(game.tinyFont, if (done) S["Concluída"] else S.f("+%d XP", m.xp), r.x + r.width - 24f, r.y + 38f)
            game.tinyFont.color = Color.WHITE
        }
        game.smallFont.color = Color.WHITE
        game.tinyFont.color = KEY_COLOR
        game.batch.textCentered(game.tinyFont, S["Renova amanhã"], 360f, 250f)
        game.tinyFont.color = Color.WHITE
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun bar(x: Float, y: Float, w: Float, h: Float, frac: Float, color: Color) {
        val shapes = game.shapes
        shapes.color = Color(0.12f, 0.13f, 0.18f, 1f)
        shapes.roundedRect(x, y, w, h, h / 2)
        val f = frac.coerceIn(0f, 1f)
        if (f > 0f) {
            shapes.color = color
            shapes.roundedRect(x, y, maxOf(h, w * f), h, h / 2)
        }
    }

    override fun onTouchDown(p: Vec2) = tap.press(back.takeIf { it.contains(p.x, p.y) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        if (tap.release(back.takeIf { it.contains(p.x, p.y) }) === back) game.showMenu()
    }

    private companion object {
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        val ROW_DONE = Color(0.20f, 0.22f, 0.30f, 1f)
    }
}
