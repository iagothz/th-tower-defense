package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

/** The twenty badges, done or not, each with the cosmetic it unlocks; tap an unlocked cosmetic to wear it. */
class AchievementsScreen(game: MergeTdGame, val goBack: () -> Unit) : BaseScreen(game) {
    override val tab get() = Tab.SETTINGS

    private val rows = Achievement.entries.mapIndexed { i, a -> a to Rectangle(24f, 1120f - i * 47f, 672f, 44f) }
    private val header = ScreenHeader(S["Conquistas"], back = true)
    private val tap = Tap<ScreenHeader>()

    override fun draw(delta: Float) {
        header.pressed = tap.current === header
        val done = Achievement.entries.count { game.save.hasAchievement(it) }
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((a, r) in rows) {
            val has = game.save.hasAchievement(a)
            shapes.color = if (has) Skin.ROW_DONE else Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 12f)
            shapes.color = if (has) Skin.GOLD else Color(0.3f, 0.32f, 0.4f, 1f)
            shapes.circle(r.x + 24f, r.y + r.height / 2, 10f, 16)
            val c = a.unlocks
            if (c != null) {
                shapes.color = pillColor(c, has)
                shapes.roundedRect(r.x + r.width - 150f, r.y + 8f, 140f, 30f, 10f)
            }
        }
        header.drawShapes(shapes)
        shapes.end()

        game.batch.begin()
        header.subtitle = S.f("%d de %d", done, Achievement.entries.size)
        header.drawText(game.batch, game)
        for ((a, r) in rows) {
            val has = game.save.hasAchievement(a)
            game.smallFont.color = if (has) Color.WHITE else Skin.MUTED
            game.batch.textLeft(game.smallFont, S[a.title], r.x + 44f, r.y + r.height / 2 + 8f)
            game.tinyFont.color = Skin.MUTED
            game.batch.textLeft(game.tinyFont, S[a.description], r.x + 44f, r.y + r.height / 2 - 12f)
            val c = a.unlocks
            if (c != null) {
                game.tinyFont.color = Skin.inkOn(pillColor(c, has))
                game.batch.textCentered(game.tinyFont, "${S[c.slot.title]}: ${S[c.title]}", r.x + r.width - 80f, r.y + 23f)
            }
        }
        game.tinyFont.color = Color.WHITE
        game.smallFont.color = Color.WHITE
        game.batch.end()
    }

    /** The cosmetic pill's fill: gold when worn, its own colour once earned, grey before. */
    private fun pillColor(c: Cosmetic, has: Boolean): Color =
        if (game.save.equipped(c.slot) == c) Skin.GOLD else if (has) c.color else PILL_LOCKED

    override fun onTouchDown(p: Vec2) = tap.press(header.takeIf { it.backHit(p) })

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
        if (tap.release(header.takeIf { it.backHit(p) }) === header) goBack()
    }

    private companion object {
        val PILL_LOCKED = Color(0.25f, 0.26f, 0.32f, 1f)
    }
}
