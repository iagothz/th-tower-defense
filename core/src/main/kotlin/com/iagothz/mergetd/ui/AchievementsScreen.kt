package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

/** The twenty badges, done or not, each with the cosmetic it unlocks; tap an unlocked cosmetic to wear it. */
class AchievementsScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.SETTINGS

    private val rows = Achievement.entries.mapIndexed { i, a -> a to Rectangle(24f, 1120f - i * 47f, 672f, 44f) }
    private val back = Button(Rectangle(160f, 124f, 400f, 80f), S["Voltar"], color = Skin.PANEL)
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        val done = Achievement.entries.count { game.save.hasAchievement(it) }
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((a, r) in rows) {
            val has = game.save.hasAchievement(a)
            shapes.color = if (has) ROW_DONE else Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 12f)
            shapes.color = if (has) Skin.GOLD else Color(0.3f, 0.32f, 0.4f, 1f)
            shapes.circle(r.x + 24f, r.y + r.height / 2, 10f, 16)
            val c = a.unlocks
            if (c != null) {
                val equipped = game.save.equipped(c.slot) == c
                shapes.color = if (equipped) Skin.GOLD else if (has) c.color else Color(0.25f, 0.26f, 0.32f, 1f)
                shapes.roundedRect(r.x + r.width - 150f, r.y + 8f, 140f, 30f, 10f)
            }
        }
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Conquistas"], 360f, 1222f)
        game.smallFont.color = KEY_COLOR
        game.batch.textCentered(game.smallFont, S.f("%d de %d", done, Achievement.entries.size), 360f, 1176f)
        game.smallFont.color = Color.WHITE
        for ((a, r) in rows) {
            val has = game.save.hasAchievement(a)
            game.smallFont.color = if (has) Color.WHITE else KEY_COLOR
            game.batch.textLeft(game.smallFont, S[a.title], r.x + 44f, r.y + r.height / 2 + 8f)
            game.tinyFont.color = KEY_COLOR
            game.batch.textLeft(game.tinyFont, S[a.description], r.x + 44f, r.y + r.height / 2 - 12f)
            val c = a.unlocks
            if (c != null) {
                val equipped = game.save.equipped(c.slot) == c
                game.tinyFont.color = if (equipped) Color(0.15f, 0.15f, 0.2f, 1f) else Color.WHITE
                game.batch.textCentered(game.tinyFont, "${S[c.slot.title]}: ${S[c.title]}", r.x + r.width - 80f, r.y + 23f)
            }
        }
        game.tinyFont.color = Color.WHITE
        game.smallFont.color = Color.WHITE
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    override fun onTouchDown(p: Vec2) = tap.press(back.takeIf { it.contains(p.x, p.y) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        for ((a, r) in rows) {
            val c = a.unlocks ?: continue
            if (r.contains(p.x, p.y) && game.save.hasCosmetic(c)) {
                // Tap again to go back to the default of that slot.
                game.save.equip(if (game.save.equipped(c.slot) == c) Cosmetic.default(c.slot) else c)
                Cosmetics.load(game.save)
                tap.cancel()
                return
            }
        }
        if (tap.release(back.takeIf { it.contains(p.x, p.y) }) === back) game.showStats()
    }

    private companion object {
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        val ROW_DONE = Color(0.20f, 0.22f, 0.30f, 1f)
    }
}
