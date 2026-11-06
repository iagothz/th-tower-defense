package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieLevels
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.roundToInt

/**
 * The sheet of one die over the deck screen: header (die, stars, cards), summary, the damage/speed/target blocks,
 * then Efeito, 7 pips, Sinergia and Power-up as sections, and the level-up button when the die has a level to
 * take. Sized to its text and centred; the X or a tap outside closes it.
 */
class DieWindow(private val game: MergeTdGame, private val accountLevel: Int, private val onLevelUp: (DieType) -> Unit) {
    var shown: DieType? = null
        private set

    val isOpen: Boolean get() = shown != null

    private val stats = DieStatsView(game)
    private val measure = GlyphLayout()
    private val card = Rectangle()
    private val close = Rectangle()
    private val levelUp = Button(Rectangle(), S["Subir nível"], color = LEVEL_COLOR)
    private val tap = Tap<Any>()

    private fun unlocked(t: DieType) = DiceUnlocks.isUnlocked(t, accountLevel)

    /** The level-up button belongs to an open die that isn't at the top yet. */
    private fun levelUpShown(t: DieType) = unlocked(t) && DieLevels.cardsToNext(game.save.dieLevel(t)) != null

    fun open(t: DieType) {
        shown = t
        val h = contentHeight(t).coerceAtMost(MAX_HEIGHT)
        card.set(24f, CENTER_Y - h / 2, 672f, h)
        close.set(card.x + card.width - 76f, card.y + card.height - 76f, 60f, 60f)
        levelUp.rect.set(card.x + card.width / 2 - 140f, card.y + 24f, 280f, LEVEL_UP_H)
    }

    fun close() {
        shown = null
        tap.cancel()
    }

    private fun contentHeight(t: DieType): Float {
        val info = DieInfo.of(t)
        val width = card.width.takeIf { it > 0f }?.minus(56f) ?: (672f - 56f)
        var h = 158f + measure.apply { setText(game.smallFont, info.summary, Color.WHITE, width, Align.left, true) }.height + 26f
        h += DieStatsView.HEIGHT + 28f
        for ((_, value) in info.rows) h += 18f + measure.apply { setText(game.smallFont, value, Color.WHITE, width, Align.left, true) }.height + 24f
        if (levelUpShown(t)) h += LEVEL_UP_H + 16f
        return h + 8f
    }

    fun draw(worldWidth: Float, worldHeight: Float) {
        val t = shown ?: return
        val info = DieInfo.of(t)
        val left = card.x + 28f
        val width = card.width - 56f
        val top = card.y + card.height
        val withButton = levelUpShown(t)
        levelUp.enabled = game.save.canLevelUp(t)
        levelUp.pressed = tap.current === levelUp
        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = DIM
        shapes.rect(-400f, -400f, worldWidth + 800f, worldHeight + 800f)
        shapes.color = Skin.SHADOW
        shapes.roundedRect(card.x + 3f, card.y - 10f, card.width, card.height, 24f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(card.x, card.y, card.width, card.height, 24f)
        shapes.color = if (unlocked(t)) Skin.color(t) else LOCKED
        shapes.roundedRect(card.x, top - 14f, card.width, 14f, 7f)
        drawDie(shapes, t, left + 48f, top - 84f, 96f)
        val cx = close.x + close.width / 2
        val cy = close.y + close.height / 2
        shapes.color = Skin.CELL
        shapes.circle(cx, cy, 26f, 24)
        shapes.color = Color.WHITE
        shapes.rectLine(cx - 10f, cy - 10f, cx + 10f, cy + 10f, 4f)
        shapes.rectLine(cx - 10f, cy + 10f, cx + 10f, cy - 10f, 4f)
        if (unlocked(t)) {
            val lit = game.save.dieLevel(t)
            for (i in 0 until DieLevels.MAX) {
                shapes.color = if (i < lit) Skin.GOLD else STAR_OFF
                shapes.polygon(left + 124f + i * 26f, top - 112f, 10f, 5, Math.PI.toFloat() / 2)
            }
        }
        val statsTop = statsTop(info)
        stats.drawShapes(info, left, width, statsTop)
        if (withButton) levelUp.drawShape(shapes)
        shapes.end()

        val batch = game.batch
        batch.begin()
        game.font.draw(batch, Skin.fullName(t), left + 112f, top - 50f)
        game.tinyFont.color = DieStatsView.KEY_COLOR
        val sub = if (unlocked(t)) levelText(t) else S.f("Abre no nível de conta %d", DiceUnlocks.unlockLevel(t))
        batch.textLeft(game.tinyFont, sub, left + (if (unlocked(t)) 252f else 112f), top - 112f)
        game.tinyFont.color = Color.WHITE
        game.smallFont.draw(batch, info.summary, left, top - 158f, width, Align.left, true)
        stats.drawText(info, left, width, statsTop)
        var y = statsTop - DieStatsView.HEIGHT - 28f
        for ((key, value) in info.rows) {
            game.tinyFont.color = DieStatsView.rowColor(key)
            batch.textLeft(game.tinyFont, key.uppercase(), left, y)
            game.tinyFont.color = Color.WHITE
            y -= 18f
            y -= game.smallFont.draw(batch, value, left, y, width, Align.left, true).height + 24f
        }
        if (withButton) levelUp.drawText(batch, game.smallFont)
        batch.end()
        stats.drawArrows()
    }

    private fun statsTop(info: DieInfo): Float {
        val summary = measure.apply { setText(game.smallFont, info.summary, Color.WHITE, card.width - 56f, Align.left, true) }.height
        return card.y + card.height - 158f - summary - 26f
    }

    /** Its bonus and the cards towards the next level. */
    private fun levelText(t: DieType): String {
        val lvl = game.save.dieLevel(t)
        val bonus = ((DieLevels.mult(lvl) - 1f) * 100f).roundToInt()
        val cost = DieLevels.cardsToNext(lvl) ?: return S.f("máximo · +%d%%", bonus)
        return S.f("+%d%% · cartas %d/%d", bonus, game.save.cards(t), cost)
    }

    /** The die itself when open, a grey tile with a padlock when the account level hasn't reached it. */
    private fun drawDie(shapes: ShapeRenderer, t: DieType, cx: Float, cy: Float, size: Float) {
        if (unlocked(t)) {
            shapes.die(t, 1, cx, cy, size)
            return
        }
        val h = size / 2
        shapes.bevelRect(cx - h, cy - h, size, size, size * 0.19f, LOCKED)
        shapes.glyph(t, cx, cy + 2f, size * 0.3f, LOCKED_INK, LOCKED)
        shapes.color = LOCK_COLOR
        shapes.ring(cx, cy + 4f, size * 0.11f, 4f, 20)
        shapes.roundedRect(cx - size * 0.15f, cy - size * 0.19f, size * 0.3f, size * 0.23f, 5f)
        shapes.color = LOCKED
        shapes.circle(cx, cy - size * 0.08f, 3f, 12)
    }

    // --- Input: while open, the window takes every touch. ---

    private fun targetAt(p: Vec2): Any? {
        val t = shown ?: return null
        return when {
            close.contains(p.x, p.y) || !card.contains(p.x, p.y) -> CLOSE
            levelUpShown(t) && levelUp.contains(p.x, p.y) -> levelUp
            else -> null
        }
    }

    fun touchDown(p: Vec2) = tap.press(targetAt(p))

    fun touchCancelled() = tap.cancel()

    fun touchUp(p: Vec2) {
        val t = shown ?: return
        when (tap.release(targetAt(p))) {
            CLOSE -> close()
            levelUp -> {
                onLevelUp(t)
                open(t) // the stars, cards and button change; the height may too
            }
        }
    }

    private companion object {
        const val MAX_HEIGHT = 1010f
        const val CENTER_Y = 655f
        const val LEVEL_UP_H = 64f
        val CLOSE = Any()
        val DIM = Color(0f, 0f, 0f, 0.72f)
        val LEVEL_COLOR = Color(0.72f, 0.52f, 0.12f, 1f)
        val LOCKED = Color(0.22f, 0.23f, 0.29f, 1f)
        val LOCKED_INK = Color(0.32f, 0.33f, 0.40f, 1f)
        val LOCK_COLOR = Color(0.62f, 0.64f, 0.72f, 1f)
        val STAR_OFF = Color(0.26f, 0.27f, 0.34f, 1f)
    }
}
