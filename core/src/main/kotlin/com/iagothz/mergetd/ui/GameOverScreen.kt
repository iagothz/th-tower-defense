package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class GameOverScreen(
    game: MergeTdGame,
    private val map: GameMap,
    private val wave: Int,
    private val isRecord: Boolean,
    private val stats: MatchStats,
    private val challengeDay: Int? = null,
    private val mode: GameMode = GameMode.NORMAL,
    private val unlocked: Set<Achievement> = emptySet(),
    private val code: String? = null,
) : BaseScreen(game) {
    private class Confetto(var x: Float, var y: Float, val speed: Float, val sway: Float, val color: Color, val w: Float, val h: Float)

    private val menu = Button(Rectangle(160f, 160f, 400f, 110f), S["Menu"])
    private val tap = Tap<Button>()
    private val panel = Rectangle(70f, 300f, 580f, 540f)
    private val rng = Random(System.nanoTime())
    private val confetti = if (isRecord) List(CONFETTI) { newConfetto(rng.nextFloat() * Layout.WORLD_H) } else emptyList()
    private var time = 0f
    /** The number rows count up one after another instead of just being there. */
    private val counters = listOf(wave, stats.kills, stats.bosses, stats.merges, stats.bought).map { Counter(0) to it }

    init {
        if (isRecord) game.sfx.play(setOf("record"))
    }

    private fun newConfetto(y: Float) = Confetto(
        rng.nextFloat() * Layout.WORLD_W, y, 90f + rng.nextFloat() * 120f, rng.nextFloat() * 6.28f,
        Skin.color(DieType.entries.random(rng)), 8f + rng.nextFloat() * 6f, 12f + rng.nextFloat() * 8f,
    )

    private val rows: List<Pair<String, String>> = listOf(
        S[if (challengeDay != null) "Desafio do dia" else "Mapa"] to
            (if (challengeDay != null) DailyChallenge.of(challengeDay).dateLabel else "${S[map.title]} · ${S[mode.title]}"),
        S["Onda alcançada"] to "$wave",
        S["Inimigos derrotados"] to "${stats.kills}",
        S["Chefes derrotados"] to "${stats.bosses}",
        S["Merges"] to "${stats.merges}",
        S["Dados comprados"] to "${stats.bought}",
        S["Recorde"] to S.f("onda %d", if (challengeDay != null) game.save.dailyBest(challengeDay) else game.save.bestWave(map, mode)),
    )

    override fun draw(delta: Float) {
        time += delta
        menu.pressed = tap.current === menu
        for ((i, c) in counters.withIndex()) if (time > COUNT_FROM + i * COUNT_STAGGER) c.first.update(delta, c.second)
        for (c in confetti) {
            c.y -= c.speed * delta
            c.x += sin(time * 2f + c.sway) * 40f * delta
            if (c.y < -20f) {
                c.y = Layout.WORLD_H + 20f
                c.x = rng.nextFloat() * Layout.WORLD_W
            }
        }

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (c in confetti) {
            shapes.color = c.color
            val wobble = 0.3f + 0.7f * abs(sin(time * 3f + c.sway))
            shapes.rect(c.x - c.w * wobble / 2, c.y - c.h / 2, c.w * wobble, c.h)
        }
        shapes.color = Skin.SHADOW
        shapes.roundedRect(panel.x + 3f, panel.y - 8f, panel.width, panel.height, 24f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(panel.x, panel.y, panel.width, panel.height, 24f)
        shapes.color = if (isRecord) Skin.GOLD else Skin.HEART
        shapes.roundedRect(panel.x, panel.y + panel.height - 10f, panel.width, 10f, 5f)
        menu.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        // The title slams in: big and settling over the first instants.
        val slam = (1f - (time / SLAM_TIME).coerceIn(0f, 1f))
        game.bigFont.data.setScale(1f + 1.2f * slam * slam)
        game.batch.textShadowed(game.bigFont, S["Fim de jogo"], 360f, 1020f)
        game.bigFont.data.setScale(1f)
        if (isRecord) {
            val pulse = 0.8f + 0.2f * sin(time * 4f)
            game.batch.textShadowed(game.font, S["Novo recorde!"], 360f, 920f, Color(1f, 0.85f * pulse + 0.1f, 0.3f, 1f))
        }
        var y = panel.y + panel.height - 44f
        for ((i, row) in rows.withIndex()) {
            val (key, value) = row
            val shown = counters.getOrNull(i - 1)?.first?.shown?.toString() ?: value
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, key, panel.x + 32f, y)
            game.smallFont.color = Color.WHITE
            game.font.color = if (key == S["Onda alcançada"]) Skin.GOLD else Color.WHITE
            game.batch.textRight(game.font, shown, panel.x + panel.width - 32f, y)
            game.font.color = Color.WHITE
            y -= 58f
        }
        game.smallFont.color = Skin.GOLD
        game.smallFont.draw(game.batch, Tips.forKiller(stats.killedBy), panel.x + 32f, y + 10f, panel.width - 64f, Align.left, true)
        game.smallFont.color = Color.WHITE
        if (unlocked.isNotEmpty()) {
            val names = unlocked.joinToString(", ") { S[it.title] }
            game.batch.textShadowed(game.smallFont, S.f("Conquista: %s", names), 360f, panel.y - 24f, Skin.GOLD)
        }
        if (code != null) {
            game.tinyFont.color = KEY_COLOR
            game.batch.textCentered(game.tinyFont, S.f("Código da partida: %s", code), 360f, 1080f)
            game.tinyFont.color = Color.WHITE
        }
        menu.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun buttonAt(p: Vec2) = menu.takeIf { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        if (tap.release(buttonAt(p)) === menu) game.showMenu()
    }

    private companion object {
        const val CONFETTI = 60
        const val SLAM_TIME = 0.3f
        const val COUNT_FROM = 0.3f
        const val COUNT_STAGGER = 0.12f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
