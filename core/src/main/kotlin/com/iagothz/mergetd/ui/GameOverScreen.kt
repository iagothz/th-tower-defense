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
    private val reward: MatchReward? = null,
    /** Mutator score, shown when any mutator was on. */
    private val score: Int? = null,
    /** The weekly challenge this was, booked by score. */
    private val week: Int? = null,
) : BaseScreen(game) {
    private class Confetto(var x: Float, var y: Float, val speed: Float, val sway: Float, val color: Color, val w: Float, val h: Float)

    private val menu = Button(Rectangle(370f, 40f, 310f, 100f), S["Início"])
    private val share = Button(Rectangle(40f, 40f, 310f, 100f), S["Compartilhar"], style = ButtonStyle.SECONDARY)
    private var copied = 0f
    private val tap = Tap<Button>()
    private val panel = Rectangle(70f, 300f, 580f, 480f)

    /** The chest's cards, most first, as (die, count); at most [CHEST_SHOWN] kinds fit the strip. */
    private val chest: List<Pair<DieType, Int>> = reward?.cards?.entries?.sortedWith(compareByDescending<Map.Entry<DieType, Int>> { it.value }.thenBy { it.key })
        ?.take(CHEST_SHOWN)?.map { it.key to it.value } ?: emptyList()
    private var cardsShown = 0
    private val rng = Random(System.nanoTime())
    private val confetti = if (isRecord) List(CONFETTI) { newConfetto(rng.nextFloat() * Layout.WORLD_H) } else emptyList()
    private var time = 0f

    /** The number rows count up one after another instead of just being there. */
    private val counters = listOf(wave, stats.kills, stats.bosses, stats.merges, stats.bought).map { Counter(0) to it }

    init {
        if (isRecord) game.sfx.play(setOf("record"))
    }

    private fun newConfetto(y: Float) = Confetto(
        rng.nextFloat() * Layout.WORLD_W,
        y,
        90f + rng.nextFloat() * 120f,
        rng.nextFloat() * 6.28f,
        Skin.color(DieType.entries.random(rng)),
        8f + rng.nextFloat() * 6f,
        12f + rng.nextFloat() * 8f,
    )

    private val rows: List<Pair<String, String>> = listOf(
        S[
            if (challengeDay != null) {
                "Desafio do dia"
            } else if (week != null) {
                "Desafio semanal"
            } else {
                "Mapa"
            },
        ] to
            (if (challengeDay != null) DailyChallenge.of(challengeDay).dateLabel else "${S[map.title]} · ${S[mode.title]}"),
        S["Onda alcançada"] to "$wave",
        S["Inimigos derrotados"] to "${stats.kills}",
        S["Chefes derrotados"] to "${stats.bosses}",
        S["Merges"] to "${stats.merges}",
        S["Dados comprados"] to "${stats.bought}",
        S["Recorde"] to when {
            challengeDay != null -> S.f("onda %d", game.save.dailyBest(challengeDay))
            week != null -> S.f("pontuação %d", game.save.weeklyBest(week))
            else -> S.f("onda %d", game.save.bestWave(map, mode))
        },
    )

    override fun draw(delta: Float) {
        time += delta
        copied = maxOf(0f, copied - delta)
        menu.pressed = tap.current === menu
        share.pressed = tap.current === share
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
        share.drawShape(shapes)
        // The chest: one die per kind pops in, left to right.
        for ((i, card) in chest.withIndex()) {
            val k = cardScale(i)
            if (k > 0f) shapes.die(card.first, 1, cardX(i), CHEST_Y, cardSize() * k)
        }
        shapes.end()

        game.batch.begin()
        // The title slams in: big and settling over the first instants.
        val slam = (1f - (time / SLAM_TIME).coerceIn(0f, 1f))
        game.bigFont.data.setScale(1f + 1.2f * slam * slam)
        game.batch.textShadowed(game.bigFont, S["Fim de jogo"], 360f, 1160f)
        game.bigFont.data.setScale(1f)
        if (isRecord) {
            val pulse = 0.8f + 0.2f * sin(time * 4f)
            game.batch.textShadowed(game.font, S["Novo recorde!"], 360f, 950f, Color(1f, 0.85f * pulse + 0.1f, 0.3f, 1f))
        }
        var y = panel.y + panel.height - 44f
        for ((i, row) in rows.withIndex()) {
            val (key, value) = row
            val shown = counters.getOrNull(i - 1)?.first?.shown?.toString() ?: value
            game.smallFont.color = Skin.MUTED
            game.batch.textLeft(game.smallFont, key, panel.x + 32f, y)
            game.smallFont.color = Color.WHITE
            game.font.color = if (key == S["Onda alcançada"]) Skin.GOLD else Color.WHITE
            game.batch.textRight(game.font, shown, panel.x + panel.width - 32f, y)
            game.font.color = Color.WHITE
            y -= 50f
        }
        game.smallFont.color = Skin.GOLD
        game.smallFont.draw(game.batch, Tips.forKiller(stats.killedBy), panel.x + 32f, y + 10f, panel.width - 64f, Align.left, true)
        game.smallFont.color = Color.WHITE
        // Several achievements can land at once (an old save's first match): the line wraps and pushes the missions down.
        var achievementsHeight = 0f
        if (unlocked.isNotEmpty()) {
            val names = unlocked.joinToString(", ") { S[it.title] }
            game.smallFont.color = Skin.GOLD
            achievementsHeight = game.smallFont.draw(game.batch, S.f("Conquista: %s", names), panel.x, panel.y - 12f, panel.width, Align.center, true).height + 6f
            game.smallFont.color = Color.WHITE
        }
        if (code != null) {
            game.tinyFont.color = Skin.MUTED
            game.batch.textCentered(game.tinyFont, S.f("Código da partida: %s", code), 360f, 1000f)
            game.tinyFont.color = Color.WHITE
        }
        if (score != null) {
            game.batch.textShadowed(game.smallFont, S.f("Pontuação %d (onda %d × mutadores)", score, wave), 360f, 1094f, Skin.GOLD)
        }
        for ((i, card) in chest.withIndex()) {
            if (cardScale(i) < 1f) continue
            game.batch.textShadowed(game.tinyFont, "×${card.second}", cardX(i), CHEST_Y - cardSize() / 2 - 12f, Skin.GOLD)
        }
        val popped = chest.indices.count { cardScale(it) >= 1f }
        if (popped > cardsShown) {
            cardsShown = popped
            game.sfx.play(setOf("pick"))
        }
        if (reward != null && reward.newDice.isNotEmpty()) {
            game.batch.textShadowed(game.smallFont, S.f("Novo dado: %s", reward.newDice.joinToString(", ") { Skin.fullName(it) }), 360f, 906f, Skin.GOLD)
        }
        if (reward != null) {
            val line = if (reward.leveledUp) S.f("Novo nível: %d!", reward.level) + "  ·  " + S.f("+%d XP", reward.xp) else S.f("+%d XP", reward.xp)
            game.batch.textShadowed(game.smallFont, line, 360f, 1050f, if (reward.leveledUp) Skin.GOLD else Color.WHITE)
            var my = panel.y - 24f - achievementsHeight
            for (m in reward.missions) {
                game.batch.textShadowed(game.tinyFont, S.f("Missão: %s", m.text()), 360f, my, Skin.GOLD)
                my -= 26f
            }
        }
        menu.drawText(game.batch, game.font)
        share.drawText(game.batch, game.font)
        if (copied > 0f) game.batch.textShadowed(game.smallFont, S["Copiado!"], 195f, 162f, Skin.GOLD)
        game.batch.end()
    }

    private fun cardSize(): Float = minOf(46f, cardSpacing() - 10f)

    private fun cardSpacing(): Float = minOf(76f, 560f / maxOf(1, chest.size))

    private fun cardX(i: Int): Float = 360f - (chest.size - 1) * cardSpacing() / 2 + i * cardSpacing()

    /** 0 before the card's turn, overshooting to 1 as it pops. */
    private fun cardScale(i: Int): Float {
        val t = ((time - CHEST_FROM - i * CHEST_STAGGER) / CHEST_POP).coerceIn(0f, 1f)
        return if (t <= 0f) {
            0f
        } else if (t >= 1f) {
            1f
        } else {
            t * (1.25f - 0.25f * t)
        }
    }

    private fun buttonAt(p: Vec2) = listOf(menu, share).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            menu -> game.showMenu()
            share -> if (!game.share(Share.text(wave, map, mode, code))) copied = 2f
            else -> Unit
        }
    }

    private companion object {
        const val CONFETTI = 60
        const val SLAM_TIME = 0.3f
        const val COUNT_FROM = 0.3f
        const val COUNT_STAGGER = 0.12f
        const val CHEST_SHOWN = 8
        const val CHEST_Y = 830f
        const val CHEST_FROM = 0.9f
        const val CHEST_STAGGER = 0.15f
        const val CHEST_POP = 0.2f
    }

    override fun backState() = BackState(ScreenKind.GAME_OVER)

    override fun perform(a: BackAction) {
        if (a == BackAction.TO_HOME) game.showMenu()
    }
}
