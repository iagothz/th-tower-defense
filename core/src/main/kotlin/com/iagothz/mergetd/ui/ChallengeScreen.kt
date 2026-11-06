package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.Mutator
import com.iagothz.mergetd.logic.Vec2
import com.iagothz.mergetd.logic.WeeklyChallenge

/** The two fixed runs everyone shares: today's challenge and this week's, each with its own record. */
class ChallengeScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.CHALLENGES

    private val daily = DailyChallenge.of(DailyChallenge.today())
    private val weekly = WeeklyChallenge.of(WeeklyChallenge.thisWeek())
    private val dailyCard = Rectangle(24f, 760f, 672f, 330f)
    private val weeklyCard = Rectangle(24f, 380f, 672f, 350f)
    private val playDaily = Button(Rectangle(dailyCard.x + 24f, dailyCard.y + 24f, 624f, 80f), S["Jogar"], color = Color(0.55f, 0.30f, 0.75f, 1f))
    private val playWeekly = Button(Rectangle(weeklyCard.x + 24f, weeklyCard.y + 24f, 624f, 80f), S["Jogar"], color = Color(0.75f, 0.35f, 0.30f, 1f))
    private val buttons = listOf(playDaily, playWeekly)
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        buttons.forEach { it.pressed = tap.current === it }
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((card, accent) in listOf(dailyCard to Color(0.55f, 0.30f, 0.75f, 1f), weeklyCard to Color(0.75f, 0.35f, 0.30f, 1f))) {
            shapes.color = Skin.SHADOW
            shapes.roundedRect(card.x + 3f, card.y - 8f, card.width, card.height, 22f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(card.x, card.y, card.width, card.height, 22f)
            shapes.color = accent
            shapes.roundedRect(card.x, card.y + card.height - 10f, card.width, 10f, 5f)
        }
        for ((i, t) in daily.deck.withIndex()) shapes.die(t, 1, dailyCard.x + 60f + i * 56f, dailyCard.y + 172f, 44f)
        for ((i, t) in weekly.deck.withIndex()) shapes.die(t, 1, weeklyCard.x + 60f + i * 56f, weeklyCard.y + 206f, 44f)
        buttons.forEach { it.drawShape(shapes) }
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Desafios"], 360f, 1222f)
        // Daily.
        game.batch.textLeft(game.font, S["Desafio do dia"], dailyCard.x + 28f, dailyCard.y + dailyCard.height - 40f)
        game.smallFont.color = Skin.MUTED
        game.batch.textRight(game.smallFont, daily.dateLabel, dailyCard.x + dailyCard.width - 28f, dailyCard.y + dailyCard.height - 40f)
        game.batch.textLeft(game.smallFont, S[daily.map.title], dailyCard.x + 28f, dailyCard.y + dailyCard.height - 86f)
        game.smallFont.color = Color.WHITE
        game.batch.textRight(game.smallFont, S.f("recorde %d", game.save.dailyBest(daily.day)), dailyCard.x + dailyCard.width - 28f, dailyCard.y + dailyCard.height - 86f)
        // Weekly.
        game.batch.textLeft(game.font, S["Desafio semanal"], weeklyCard.x + 28f, weeklyCard.y + weeklyCard.height - 40f)
        game.smallFont.color = Skin.MUTED
        game.batch.textRight(game.smallFont, S.f("Semana de %s", weekly.dateLabel), weeklyCard.x + weeklyCard.width - 28f, weeklyCard.y + weeklyCard.height - 40f)
        game.batch.textLeft(game.smallFont, S[weekly.map.title], weeklyCard.x + 28f, weeklyCard.y + weeklyCard.height - 86f)
        game.smallFont.color = Color.WHITE
        game.batch.textRight(game.smallFont, S.f("recorde: pontuação %d", game.save.weeklyBest(weekly.week)), weeklyCard.x + weeklyCard.width - 28f, weeklyCard.y + weeklyCard.height - 86f)
        game.tinyFont.color = Skin.GOLD
        val names = weekly.mutators.joinToString(" · ") { "${S[it.title]} (${S[it.description]})" }
        // Wrapped: two mutators with their descriptions don't fit one line in English.
        game.tinyFont.draw(game.batch, S.f("Mutadores ×%s: %s", DieInfo.num(Mutator.multiplier(weekly.mutators)), names), weeklyCard.x + 28f, weeklyCard.y + 162f, weeklyCard.width - 56f, Align.left, true)
        game.tinyFont.color = Color.WHITE
        buttons.forEach { it.drawText(game.batch, game.font) }
        game.batch.end()
    }

    private fun buttonAt(p: Vec2) = buttons.firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            playDaily -> game.startDaily()
            playWeekly -> game.startWeekly()
        }
    }
}
