package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DailyMissions
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Progression
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.sin
import kotlin.random.Random

/** The home tab: title, account level, the chosen map's record, Jogar, and today's three missions. */
class MenuScreen(game: MergeTdGame) : BaseScreen(game) {
    /** A decorative die drifting down behind the menu. */
    private class Drop(var type: DieType, var pips: Int, var x: Float, var y: Float, var size: Float, var speed: Float, val sway: Float)

    override val tab get() = Tab.HOME

    private val play = Button(Rectangle(160f, 640f, 400f, 100f), S["Jogar"])
    private val tap = Tap<Button>()
    private val rng = Random(System.nanoTime())
    private val drops = List(DROPS) { newDrop(rng.nextFloat() * Layout.WORLD_H) }
    private val day = DailyChallenge.today()
    private val missions = DailyMissions.of(day)
    private val progress = game.save.missionProgress(day, missions)
    private val missionsPanel = Rectangle(24f, 150f, 672f, 440f)
    private val rows = missions.mapIndexed { i, m -> m to Rectangle(missionsPanel.x + 16f, missionsPanel.y + missionsPanel.height - 74f - (i + 1) * 112f, missionsPanel.width - 32f, 102f) }
    private var time = 0f
    /** The cloud's one-line message, taken from the controller when the tab opens and shown for a few seconds. */
    private val notice = game.cloud.notice.also { game.cloud.notice = null }
    private var noticeLeft = NOTICE_TIME

    private fun newDrop(y: Float) = Drop(
        DieType.entries.random(rng),
        1 + rng.nextInt(7),
        40f + rng.nextFloat() * (Layout.WORLD_W - 80f),
        y,
        44f + rng.nextFloat() * 40f,
        25f + rng.nextFloat() * 35f,
        rng.nextFloat() * 6.28f,
    )

    override fun draw(delta: Float) {
        time += delta
        play.pressed = tap.current === play
        for (d in drops) {
            d.y -= d.speed * delta
            if (d.y < -60f) {
                val fresh = newDrop(Layout.WORLD_H + 60f)
                d.type = fresh.type
                d.pips = fresh.pips
                d.x = fresh.x
                d.y = fresh.y
                d.size = fresh.size
                d.speed = fresh.speed
            }
        }

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (d in drops) shapes.die(d.type, d.pips, d.x + sin(time * 0.7f + d.sway) * 12f, d.y, d.size, 0.75f, 0.45f)
        // Account level under the title, as a thin bar.
        val xp = game.save.xp
        val level = Progression.level(xp)
        shapes.color = Color(0.12f, 0.13f, 0.18f, 1f)
        shapes.roundedRect(200f, 876f, 320f, 10f, 5f)
        shapes.color = Skin.GOLD
        shapes.roundedRect(200f, 876f, maxOf(10f, 320f * Progression.intoLevel(xp) / Progression.costToNext(level)), 10f, 5f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(140f, 790f, 440f, 56f, 28f)
        shapes.color = Skin.GOLD
        shapes.rectLine(176f, 804f, 176f, 834f, 3f)
        shapes.triangle(177f, 834f, 177f, 818f, 199f, 826f)
        play.drawShape(shapes)
        // Three little dice sitting on the title line.
        shapes.die(DieType.FIRE, 3, 236f, 1046f, 44f)
        shapes.die(DieType.ELECTRIC, 5, 360f, 1058f, 52f)
        shapes.die(DieType.ICE, 2, 484f, 1046f, 44f)
        // Today's missions.
        shapes.color = Skin.SHADOW
        shapes.roundedRect(missionsPanel.x + 3f, missionsPanel.y - 8f, missionsPanel.width, missionsPanel.height, 20f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(missionsPanel.x, missionsPanel.y, missionsPanel.width, missionsPanel.height, 20f)
        for ((i, row) in rows.withIndex()) {
            val (m, r) = row
            val done = progress[i] >= m.target
            shapes.color = if (done) ROW_DONE else Skin.CELL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 16f)
            shapes.color = if (done) Skin.GOLD else Color(0.3f, 0.32f, 0.4f, 1f)
            shapes.circle(r.x + 26f, r.y + r.height - 26f, 9f, 16)
            bar(r.x + 20f, r.y + 18f, r.width - 220f, 14f, progress[i].toFloat() / m.target, if (done) Skin.GOLD else Skin.color(DieType.ELECTRIC))
        }
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Merge TD", 360f, 960f)
        game.batch.textCentered(game.smallFont, S.f("Nível %d · %s", level, S[Progression.title(level)]), 360f, 912f)
        val chosen = game.save.map
        val mode = game.save.mode
        game.batch.textLeft(game.font, S.f("%s · %s: recorde %d", S[chosen.title], S[mode.title], game.save.bestWave(chosen, mode)), 212f, 818f)
        play.drawText(game.batch, game.font)
        game.font.color = Skin.GOLD
        game.batch.textLeft(game.font, S["Missões de hoje"], missionsPanel.x + 24f, missionsPanel.y + missionsPanel.height - 32f)
        game.font.color = Color.WHITE
        game.tinyFont.color = KEY_COLOR
        game.batch.textRight(game.tinyFont, "${DailyChallenge.of(day).dateLabel} · ${S["Renova amanhã"]}", missionsPanel.x + missionsPanel.width - 24f, missionsPanel.y + missionsPanel.height - 32f)
        game.tinyFont.color = Color.WHITE
        for ((i, row) in rows.withIndex()) {
            val (m, r) = row
            val done = progress[i] >= m.target
            game.smallFont.draw(game.batch, m.text(), r.x + 44f, r.y + r.height - 14f, r.width - 64f, Align.left, true)
            game.tinyFont.color = KEY_COLOR
            game.batch.textLeft(game.tinyFont, "${minOf(progress[i], m.target)}/${m.target}", r.x + r.width - 186f, r.y + 25f)
            game.tinyFont.color = Skin.GOLD
            game.batch.textRight(game.tinyFont, if (done) S["Concluída"] else S.f("+%d XP", m.xp), r.x + r.width - 20f, r.y + 25f)
            game.tinyFont.color = Color.WHITE
        }
        game.smallFont.color = Color(1f, 1f, 1f, 0.4f)
        game.batch.textCentered(game.smallFont, "v${MergeTdGame.VERSION}", 360f, 128f)
        game.smallFont.color = Color.WHITE
        if (notice != null && noticeLeft > 0f) {
            noticeLeft -= delta
            game.batch.textShadowed(game.tinyFont, S[notice], 360f, 770f, Skin.GOLD)
        }
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

    override fun onTouchDown(p: Vec2) = tap.press(play.takeIf { it.contains(p.x, p.y) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        if (tap.release(play.takeIf { it.contains(p.x, p.y) }) === play) game.startMatch()
    }

    private companion object {
        const val NOTICE_TIME = 3f
        const val DROPS = 12
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        val ROW_DONE = Color(0.20f, 0.22f, 0.30f, 1f)
    }
}
