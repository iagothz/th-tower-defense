package com.iagothz.mergetd.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DailyMissions
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Progression
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
    private val missionsPanel = MissionsPanel(game, Rectangle(24f, 150f, 672f, MissionsPanel.HEIGHT), day, missions)
    private var time = 0f

    /** The cloud's one-line message, taken from the controller when the tab opens and shown for a few seconds. */
    private val notice = game.cloud.notice.also { game.cloud.notice = null }
    private var noticeLeft = NOTICE_TIME

    private fun newDrop(y: Float) = Drop(
        DieType.entries.random(rng),
        1 + rng.nextInt(7),
        rainX(rng.nextFloat()),
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
            // Off the visible world (taller than 1280 on long phones) before recycling, so none pops in or out on screen.
            val half = viewport.worldHeight / 2
            if (d.y < Layout.WORLD_H / 2 - half - 60f) {
                val fresh = newDrop(Layout.WORLD_H / 2 + half + 60f)
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
        // The record pill hugs its text (map, mode and record vary in length) and stays centred.
        val record = S.f("%s · %s: recorde %d", S[game.save.map.title], S[game.save.mode.title], game.save.bestWave(game.save.map, game.save.mode))
        val recordWidth = recordLayout.setText(game.font, record).let { recordLayout.width }
        val pillWidth = RECORD_TEXT_X + recordWidth + RECORD_PAD
        val pillX = 360f - pillWidth / 2
        shapes.color = Skin.PANEL
        shapes.roundedRect(pillX, 790f, pillWidth, 56f, 28f)
        shapes.color = Skin.GOLD
        val flagX = pillX + RECORD_FLAG_X
        shapes.rectLine(flagX, 804f, flagX, 834f, 3f)
        shapes.triangle(flagX + 1f, 834f, flagX + 1f, 818f, flagX + 23f, 826f)
        play.drawShape(shapes)
        // Three little dice sitting on the title line.
        shapes.die(DieType.FIRE, 3, 236f, 1046f, 44f)
        shapes.die(DieType.ELECTRIC, 5, 360f, 1058f, 52f)
        shapes.die(DieType.ICE, 2, 484f, 1046f, 44f)
        missionsPanel.drawShapes(progress)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Merge TD", 360f, 960f)
        game.batch.textCentered(game.smallFont, S.f("Nível %d · %s", level, S[Progression.title(level)]), 360f, 912f)
        game.batch.textLeft(game.font, record, pillX + RECORD_TEXT_X, 818f)
        play.drawText(game.batch, game.font)
        missionsPanel.drawText(progress)
        game.smallFont.color = Color(1f, 1f, 1f, 0.4f)
        game.batch.textCentered(game.smallFont, "v${MergeTdGame.VERSION}", 360f, 128f)
        game.smallFont.color = Color.WHITE
        if (notice != null && noticeLeft > 0f) {
            noticeLeft -= delta
            game.batch.textShadowed(game.tinyFont, S[notice], 360f, 770f, Skin.GOLD)
        }
        game.batch.end()
    }

    override fun onTouchDown(p: Vec2) = tap.press(play.takeIf { it.contains(p.x, p.y) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        if (tap.release(play.takeIf { it.contains(p.x, p.y) }) === play) game.startMatch()
    }

    /** Dev mode only: C shows the cloud conflict screen with the current save on both sides. */
    override fun onKey(keycode: Int) {
        if (!game.devMode || keycode != Input.Keys.C) return
        val day = DailyChallenge.today() - 2
        game.showCloudConflict(game.save.export() + "\ni:sync.version=1\ni:sync.at=" + day)
    }

    private val recordLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout()

    private companion object {
        const val NOTICE_TIME = 3f

        /** Flag pole and text start, from the pill's left edge; right padding after the text. */
        const val RECORD_FLAG_X = 36f
        const val RECORD_TEXT_X = 72f
        const val RECORD_PAD = 28f
        const val DROPS = 12
    }
}

/** The home screen's falling dice keep to two side lanes, [RAIN_LANE_MIN]..[RAIN_LANE_MAX] from each edge, so they
 * never cross the title, the record pill or Jogar. [r] in 0..1: the first half fills the left lane, the rest the right. */
fun rainX(r: Float): Float {
    val lane = RAIN_LANE_MAX - RAIN_LANE_MIN
    return if (r < 0.5f) RAIN_LANE_MIN + r * 2f * lane else Layout.WORLD_W - RAIN_LANE_MIN - (r - 0.5f) * 2f * lane
}

const val RAIN_LANE_MIN = 40f
const val RAIN_LANE_MAX = 140f
