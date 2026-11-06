package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieLevels
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.roundToInt

/**
 * Every die in a scrolling list: name, one-line summary and its damage, speed and target at a glance. Tapping a
 * row opens a window with everything about that die; the X or a tap outside closes it.
 */
class DiceScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.DECK

    private val level = game.save.accountLevel
    private val list = ScrollList(DieType.entries.size, top = LIST_TOP, bottom = LIST_BOTTOM, rowHeight = ROW_H, gap = ROW_GAP)
    private val infos = DieType.entries.associateWith { DieInfo.of(it) }
    private val back = Button(Rectangle(160f, 124f, 400f, 80f), S["Voltar"], color = Skin.PANEL)
    private val tap = Tap<Any>()
    private val stats = DieStatsView(game)
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val identity = Matrix4()

    /** The die whose window is open; null shows the list alone. */
    private var open: DieType? = null
    /** The window, sized to the open die's text and centred between the bottom bar and the title. */
    private val card = Rectangle(24f, 150f, 672f, CARD_MAX)
    private val close = Rectangle()

    private fun openWindow(t: DieType) {
        open = t
        val h = contentHeight(infos.getValue(t)).coerceAtMost(CARD_MAX)
        card.set(24f, CARD_CENTER - h / 2, 672f, h)
        close.set(card.x + card.width - 76f, card.y + card.height - 76f, 60f, 60f)
    }

    /** Header, summary, the three blocks and every detail section, laid out as [drawWindow] does. */
    private fun contentHeight(info: DieInfo): Float {
        val width = card.width - 56f
        var h = 158f + measure.apply { setText(game.smallFont, info.summary, Color.WHITE, width, Align.left, true) }.height + 26f
        h += DieStatsView.HEIGHT + 28f
        for ((_, value) in info.rows) h += 18f + measure.apply { setText(game.smallFont, value, Color.WHITE, width, Align.left, true) }.height + 24f
        return h + 8f
    }

    private fun isOpen(t: DieType) = DiceUnlocks.isUnlocked(t, level)

    override fun draw(delta: Float) {
        back.pressed = tap.current === back
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        back.drawShape(shapes)
        drawScrollBar(shapes)
        shapes.end()
        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Dados"], 360f, 1222f)
        game.tinyFont.color = DieStatsView.KEY_COLOR
        game.batch.textCentered(game.tinyFont, S["Toque num dado para ver tudo"], 360f, 1166f)
        game.tinyFont.color = Color.WHITE
        back.drawText(game.batch, game.font)
        game.batch.end()

        drawList()
        open?.let { drawWindow(it) }
    }

    // --- The list ---

    private fun drawList() {
        clip.set(0f, LIST_BOTTOM, viewport.worldWidth + 800f, LIST_TOP - LIST_BOTTOM)
        clip.x = -400f
        ScissorStack.calculateScissors(viewport.camera, viewport.screenX.toFloat(), viewport.screenY.toFloat(), viewport.screenWidth.toFloat(), viewport.screenHeight.toFloat(), identity, clip, scissor)
        if (!ScissorStack.pushScissors(scissor)) return
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((i, t) in DieType.entries.withIndex()) {
            if (!list.isVisible(i)) continue
            val r = rowRect(i)
            shapes.color = if (isOpen(t)) Skin.PANEL else LOCKED_ROW
            shapes.roundedRect(r.x, r.y, r.width, r.height, 16f)
            shapes.color = if (isOpen(t)) Skin.color(t) else LOCKED
            shapes.roundedRect(r.x, r.y + 10f, 6f, r.height - 20f, 3f)
            drawDie(shapes, t, r.x + 56f, r.y + r.height / 2, 64f)
            if (isOpen(t)) for ((k, s) in infos.getValue(t).stats.withIndex()) {
                stats.icon(shapes, s.kind, r.x + STAT_X[k] + 10f, r.y + 22f, 0.62f)
            }
        }
        shapes.end()
        game.batch.begin()
        for ((i, t) in DieType.entries.withIndex()) {
            if (!list.isVisible(i)) continue
            val r = rowRect(i)
            val info = infos.getValue(t)
            game.smallFont.color = if (isOpen(t)) Color.WHITE else LOCKED_TEXT
            game.batch.textLeft(game.smallFont, Skin.fullName(t), r.x + TEXT_X, r.y + r.height - 22f)
            game.smallFont.color = Color.WHITE
            game.tinyFont.color = if (isOpen(t)) Skin.GOLD else DieStatsView.KEY_COLOR
            val tag = if (isOpen(t)) S.f("Nv. %d", game.save.dieLevel(t)) else S.f("Abre no nível de conta %d", DiceUnlocks.unlockLevel(t))
            game.batch.textRight(game.tinyFont, tag, r.x + r.width - 18f, r.y + r.height - 22f)
            game.tinyFont.color = DieStatsView.KEY_COLOR
            game.batch.textLeft(game.tinyFont, info.summary, r.x + TEXT_X, r.y + r.height - 52f)
            game.tinyFont.color = Color.WHITE
            if (isOpen(t)) for ((k, s) in info.stats.withIndex()) {
                stats.textWithArrow(game.tinyFont, s.value, r.x + STAT_X[k] + 28f, r.y + 22f, Color.WHITE)
            }
        }
        game.batch.end()
        stats.drawArrows()
        ScissorStack.popScissors()
    }

    private fun rowRect(i: Int) = Rectangle(24f, list.rowTop(i) - ROW_H, 672f, ROW_H)

    private fun drawScrollBar(shapes: ShapeRenderer) {
        if (list.maxOffset <= 0f) return
        val h = LIST_TOP - LIST_BOTTOM
        val thumb = h * h / (h + list.maxOffset)
        val y = LIST_TOP - thumb - (h - thumb) * (list.offset / list.maxOffset)
        shapes.color = SCROLL_TRACK
        shapes.roundedRect(704f, LIST_BOTTOM, 6f, h, 3f)
        shapes.color = DieStatsView.KEY_COLOR
        shapes.roundedRect(704f, y, 6f, thumb, 3f)
    }

    /** The die itself when open, a grey tile with a padlock when the account level hasn't reached it. */
    private fun drawDie(shapes: ShapeRenderer, t: DieType, cx: Float, cy: Float, size: Float) {
        if (isOpen(t)) {
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

    // --- The window ---

    private fun drawWindow(t: DieType) {
        val info = infos.getValue(t)
        val left = card.x + 28f
        val width = card.width - 56f
        val top = card.y + card.height
        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = DIM
        shapes.rect(-400f, -400f, viewport.worldWidth + 800f, viewport.worldHeight + 800f)
        shapes.color = Skin.SHADOW
        shapes.roundedRect(card.x + 3f, card.y - 10f, card.width, card.height, 24f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(card.x, card.y, card.width, card.height, 24f)
        shapes.color = if (isOpen(t)) Skin.color(t) else LOCKED
        shapes.roundedRect(card.x, top - 14f, card.width, 14f, 7f)
        drawDie(shapes, t, left + 48f, top - 84f, 96f)
        // Close button.
        shapes.color = Skin.CELL
        shapes.circle(close.x + close.width / 2, close.y + close.height / 2, 26f, 24)
        shapes.color = Color.WHITE
        val cx = close.x + close.width / 2
        val cy = close.y + close.height / 2
        shapes.rectLine(cx - 10f, cy - 10f, cx + 10f, cy + 10f, 4f)
        shapes.rectLine(cx - 10f, cy + 10f, cx + 10f, cy - 10f, 4f)
        if (isOpen(t)) {
            val lit = game.save.dieLevel(t)
            for (i in 0 until DieLevels.MAX) {
                shapes.color = if (i < lit) Skin.GOLD else STAR_OFF
                shapes.polygon(left + 124f + i * 26f, top - 112f, 10f, 5, Math.PI.toFloat() / 2)
            }
        }
        val statsTop = statsTop(info)
        stats.drawShapes(info, left, width, statsTop)
        shapes.end()

        val batch = game.batch
        batch.begin()
        game.font.draw(batch, Skin.fullName(t), left + 112f, top - 50f)
        game.tinyFont.color = DieStatsView.KEY_COLOR
        val sub = if (isOpen(t)) levelText(t) else S.f("Abre no nível de conta %d", DiceUnlocks.unlockLevel(t))
        batch.textLeft(game.tinyFont, sub, left + (if (isOpen(t)) 252f else 112f), top - 112f)
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
        batch.end()
        stats.drawArrows()
    }

    private fun statsTop(info: DieInfo): Float {
        val summary = measure.apply { setText(game.smallFont, info.summary, Color.WHITE, card.width - 56f, Align.left, true) }.height
        return card.y + card.height - 158f - summary - 26f
    }

    private val measure = com.badlogic.gdx.graphics.g2d.GlyphLayout()

    /** Its bonus and the cards towards the next level, as on the deck panel. */
    private fun levelText(t: DieType): String {
        val lvl = game.save.dieLevel(t)
        val bonus = ((DieLevels.mult(lvl) - 1f) * 100f).roundToInt()
        val cost = DieLevels.cardsToNext(lvl) ?: return S.f("máximo · +%d%%", bonus)
        return S.f("+%d%% · cartas %d/%d", bonus, game.save.cards(t), cost)
    }

    // --- Input ---

    override fun onTouchDown(p: Vec2) {
        if (open != null) {
            tap.press(if (close.contains(p.x, p.y) || !card.contains(p.x, p.y)) CLOSE else null)
            return
        }
        if (back.contains(p.x, p.y)) tap.press(back) else list.down(p)
    }

    override fun onDrag(p: Vec2) {
        if (open == null) list.move(p)
    }

    override fun onTouchCancelled() {
        tap.cancel()
        list.cancel()
    }

    override fun onTouchUp(p: Vec2) {
        if (open != null) {
            if (tap.release(if (close.contains(p.x, p.y) || !card.contains(p.x, p.y)) CLOSE else null) === CLOSE) open = null
            return
        }
        if (tap.release(back.takeIf { it.contains(p.x, p.y) }) === back) {
            game.showDeck()
            return
        }
        list.up(p)?.let { openWindow(DieType.entries[it]) }
    }

    private companion object {
        const val LIST_TOP = 1140f
        const val CARD_MAX = 1010f
        const val CARD_CENTER = 655f
        const val LIST_BOTTOM = 222f
        const val ROW_H = 104f
        const val ROW_GAP = 10f
        const val TEXT_X = 100f
        /** Where each of the three small numbers starts in a row, from the row's left edge. */
        val STAT_X = floatArrayOf(100f, 300f, 486f)
        val CLOSE = Any()
        val DIM = Color(0f, 0f, 0f, 0.72f)
        val LOCKED = Color(0.22f, 0.23f, 0.29f, 1f)
        val LOCKED_ROW = Color(0.13f, 0.14f, 0.18f, 1f)
        val LOCKED_INK = Color(0.32f, 0.33f, 0.40f, 1f)
        val LOCKED_TEXT = Color(0.55f, 0.57f, 0.64f, 1f)
        val LOCK_COLOR = Color(0.62f, 0.64f, 0.72f, 1f)
        val STAR_OFF = Color(0.26f, 0.27f, 0.34f, 1f)
        val SCROLL_TRACK = Color(1f, 1f, 1f, 0.08f)
    }
}
