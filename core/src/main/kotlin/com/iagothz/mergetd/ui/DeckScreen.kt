package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2

/** Tapping a die toggles it in the deck and shows what it does in the panel below. */
class DeckScreen(game: MergeTdGame) : BaseScreen(game) {
    private val selected = game.save.deck.toMutableList()
    private var shown = selected.firstOrNull() ?: DieType.FIRE
    private val tiles = DieType.entries.mapIndexed { i, t ->
        t to Rectangle(34f + (i % COLS) * 112f, 1010f - (i / COLS) * 136f, 92f, 92f)
    }
    private val panel = Rectangle(24f, 150f, 672f, 400f)
    private val confirm = Button(Rectangle(40f, 30f, 310f, 100f), "Confirmar")
    private val back = Button(Rectangle(370f, 30f, 310f, 100f), "Voltar", color = Skin.PANEL)

    override fun draw(delta: Float) {
        confirm.enabled = selected.size == DeckCodec.SIZE
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((t, r) in tiles) {
            if (t in selected) {
                shapes.color = Color.WHITE
                shapes.roundedRect(r.x - 6, r.y - 6, r.width + 12, r.height + 12, 20f)
            }
            shapes.color = Skin.color(t)
            shapes.roundedRect(r.x, r.y, r.width, r.height, 16f)
            if (t == shown) {
                shapes.color = Skin.pipColor(t)
                shapes.circle(r.x + r.width / 2, r.y + r.height / 2, 10f, 16)
            }
        }
        shapes.color = Skin.PANEL
        shapes.roundedRect(panel.x, panel.y, panel.width, panel.height, 18f)
        shapes.color = Skin.color(shown)
        shapes.roundedRect(panel.x, panel.y + panel.height - 12f, panel.width, 12f, 6f)
        confirm.drawShape(shapes)
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Deck", 360f, 1215f)
        game.batch.textCentered(game.font, "Escolha 5 dados (${selected.size}/5)", 360f, 1145f)
        for ((t, r) in tiles) game.batch.textCentered(game.smallFont, Skin.label(t), r.x + r.width / 2, r.y - 20f)
        drawInfo()
        confirm.drawText(game.batch, game.font)
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun drawInfo() {
        val info = DieInfo.of(shown)
        val left = panel.x + 24f
        val keyWidth = 150f
        val valueWidth = panel.width - 48f - keyWidth
        var y = panel.y + panel.height - 36f

        game.font.draw(game.batch, Skin.fullName(shown), left, y)
        y -= 46f
        y -= game.smallFont.draw(game.batch, info.summary, left, y, panel.width - 48f, Align.left, true).height + 16f

        for ((key, value) in info.rows) {
            game.smallFont.color = KEY_COLOR
            game.smallFont.draw(game.batch, key, left, y)
            game.smallFont.color = Color.WHITE
            y -= game.smallFont.draw(game.batch, value, left + keyWidth, y, valueWidth, Align.left, true).height + 12f
        }
    }

    override fun onTouchUp(p: Vec2) {
        val tile = tiles.firstOrNull { it.second.contains(p.x, p.y) }?.first
        if (tile != null) shown = tile
        when {
            tile != null && tile in selected -> selected.remove(tile)
            tile != null && selected.size < DeckCodec.SIZE -> selected.add(tile)
            tile != null -> Unit
            confirm.contains(p.x, p.y) -> {
                game.save.deck = selected.toList()
                game.showMenu()
            }
            back.contains(p.x, p.y) -> game.showMenu()
        }
    }

    private companion object {
        const val COLS = 6
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
