package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Difficulty
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2

/** Four cards, one per map: a miniature of the path, the difficulty and the record there. Tap to pick. */
class MapScreen(game: MergeTdGame) : BaseScreen(game) {
    private var selected = game.save.map
    private val cards = GameMap.entries.mapIndexed { i, m ->
        m to Rectangle(24f + (i % 2) * 340f, 830f - (i / 2) * 340f, 332f, 320f)
    }
    private val confirm = Button(Rectangle(40f, 30f, 310f, 100f), "Confirmar")
    private val back = Button(Rectangle(370f, 30f, 310f, 100f), "Voltar", color = Skin.PANEL)
    private val tap = Tap<Button>()

    /** The menu behind the cards already shows the chosen map's sky. */
    override val skyMap get() = selected

    override fun draw(delta: Float) {
        confirm.pressed = tap.current === confirm
        back.pressed = tap.current === back
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((m, r) in cards) {
            if (m == selected) {
                shapes.color = Skin.GOLD
                shapes.roundedRect(r.x - 6f, r.y - 6f, r.width + 12f, r.height + 12f, 26f)
            }
            shapes.color = Skin.SHADOW
            shapes.roundedRect(r.x + 3f, r.y - 8f, r.width, r.height, 22f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 22f)
            drawMiniature(m, r.x + r.width / 2, r.y + 22f, PREVIEW_SCALE)
        }
        confirm.drawShape(shapes)
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Mapa", 360f, 1222f)
        for ((m, r) in cards) {
            game.batch.textCentered(game.font, m.title, r.x + r.width / 2, r.y + r.height - 32f)
            game.smallFont.color = Skin.difficultyColor(m.difficulty)
            game.batch.textCentered(game.smallFont, m.difficulty.label, r.x + r.width / 2 - 70f, r.y + r.height - 66f)
            game.smallFont.color = KEY_COLOR
            game.batch.textCentered(game.smallFont, "recorde ${game.save.bestWave(m)}", r.x + r.width / 2 + 60f, r.y + r.height - 66f)
            game.smallFont.color = Color.WHITE
        }
        confirm.drawText(game.batch, game.font)
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    /** The map at [s] scale with its bottom-centre at (cx, y): sky, board frame, ground, start and end. */
    private fun drawMiniature(m: GameMap, cx: Float, y: Float, s: Float) {
        val shapes = game.shapes
        val t = Skin.theme(m)
        val x0 = cx - Layout.WORLD_W * s / 2
        fun wx(x: Float) = x0 + x * s
        fun wy(wy: Float) = y + wy * s
        shapes.rect(x0, y, Layout.WORLD_W * s, Layout.WORLD_H * s, t.skyBottom, t.skyBottom, t.skyTop, t.skyTop)
        shapes.color = Skin.BOARD_RIM
        val bw = Balance.COLS * (Layout.CELL + Layout.GAP) - Layout.GAP
        val bh = Balance.ROWS * (Layout.CELL + Layout.GAP) - Layout.GAP
        shapes.roundedRect(wx(Layout.GRID_X - 18f), wy(Layout.GRID_Y - 18f), (bw + 36f) * s, (bh + 36f) * s, 5f)
        val pts = m.path.points
        for ((c, w) in listOf(t.edge to 48f * s, t.ground to 40f * s)) {
            shapes.color = c
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(wx(a.x), wy(a.y), wx(b.x), wy(b.y), w)
            pts.forEach { shapes.rect(wx(it.x) - w / 2, wy(it.y) - w / 2, w, w) }
        }
        shapes.color = Color(0.3f, 0.75f, 0.35f, 1f)
        shapes.circle(wx(pts.first().x), wy(pts.first().y), 5f, 12)
        shapes.color = Skin.HEART
        shapes.circle(wx(pts.last().x), wy(pts.last().y), 5f, 12)
    }

    private fun buttonAt(p: Vec2) = listOf(confirm, back).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        val card = cards.firstOrNull { it.second.contains(p.x, p.y) }?.first
        if (card != null) {
            selected = card
            tap.cancel()
            return
        }
        when (tap.release(buttonAt(p))) {
            confirm -> {
                game.save.map = selected
                game.showMenu()
            }
            back -> game.showMenu()
        }
    }

    private companion object {
        const val PREVIEW_SCALE = 0.17f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
