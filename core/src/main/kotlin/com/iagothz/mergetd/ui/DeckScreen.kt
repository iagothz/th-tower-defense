package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Synergy
import com.iagothz.mergetd.logic.Vec2

/** Tapping a die toggles it in the deck and shows what it does in the panel below. */
class DeckScreen(game: MergeTdGame) : BaseScreen(game) {
    private val selected = game.save.deck.toMutableList()
    private var shown = selected.firstOrNull() ?: DieType.FIRE
    private val tiles = DieType.entries.mapIndexed { i, t ->
        t to Rectangle(24f + (i % COLS) * 98f, 1010f - (i / COLS) * 126f, 84f, 84f)
    }
    private val panel = Rectangle(24f, 150f, 672f, 430f)
    private val confirm = Button(Rectangle(40f, 30f, 310f, 100f), "Confirmar")
    private val back = Button(Rectangle(370f, 30f, 310f, 100f), "Voltar", color = Skin.PANEL)
    private val tap = Tap<Button>()
    private val slots = List(DeckCodec.SIZE) { Rectangle(186f + it * 72f, 1112f, 60f, 60f) }

    override fun draw(delta: Float) {
        confirm.enabled = selected.size == DeckCodec.SIZE
        confirm.pressed = tap.current === confirm
        back.pressed = tap.current === back
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((t, r) in tiles) {
            if (t == shown) {
                shapes.color = Color.WHITE
                shapes.roundedRect(r.x - 6, r.y - 6, r.width + 12, r.height + 12, 20f)
            }
            shapes.bevelRect(r.x, r.y, r.width, r.height, 16f, Skin.color(t))
            shapes.glyph(t, r.x + r.width / 2, r.y + r.height / 2 + 2f, 25f, Skin.glyphColor(t, 0.85f), Skin.color(t))
            if (t in selected) {
                shapes.color = Skin.GOLD
                shapes.circle(r.x + r.width - 2f, r.y + r.height - 2f, 14f, 20)
                shapes.color = Color(0.15f, 0.15f, 0.2f, 1f)
                shapes.rectLine(r.x + r.width - 9f, r.y + r.height - 2f, r.x + r.width - 4f, r.y + r.height - 8f, 3.5f)
                shapes.rectLine(r.x + r.width - 4f, r.y + r.height - 8f, r.x + r.width + 5f, r.y + r.height + 5f, 3.5f)
            }
        }
        for ((i, r) in slots.withIndex()) {
            val t = selected.getOrNull(i)
            if (t == null) {
                shapes.color = Skin.CELL
                shapes.roundedRect(r.x, r.y, r.width, r.height, 12f)
            } else {
                shapes.die(t, 1, r.x + r.width / 2, r.y + r.height / 2, r.width)
            }
        }
        shapes.color = Skin.SHADOW
        shapes.roundedRect(panel.x + 3f, panel.y - 8f, panel.width, panel.height, 18f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(panel.x, panel.y, panel.width, panel.height, 18f)
        shapes.color = Skin.color(shown)
        shapes.roundedRect(panel.x, panel.y + panel.height - 12f, panel.width, 12f, 6f)
        shapes.glyph(shown, panel.x + 48f, panel.y + panel.height - 42f, 18f, Skin.color(shown), Skin.PANEL)
        confirm.drawShape(shapes)
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Deck", 360f, 1222f)
        if (selected.size < DeckCodec.SIZE) {
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, "escolha ${DeckCodec.SIZE - selected.size}", 556f, 1142f)
            game.smallFont.color = Color.WHITE
        }
        for ((t, r) in tiles) game.batch.textCentered(game.tinyFont, Skin.label(t), r.x + r.width / 2, r.y - 16f)
        drawInfo()
        val pairs = Synergy.forDeck(selected)
        game.tinyFont.color = Skin.GOLD
        val line = if (pairs.isEmpty()) "Sinergias do deck: nenhuma ainda (veja os pares na descrição de cada dado)" else "Sinergias do deck: " + pairs.joinToString(", ") { it.title }
        game.batch.textCentered(game.tinyFont, line, 360f, panel.y + 18f)
        game.tinyFont.color = Color.WHITE
        confirm.drawText(game.batch, game.font)
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun drawInfo() {
        val info = DieInfo.of(shown)
        val left = panel.x + 24f
        val keyWidth = 150f
        val valueWidth = panel.width - 48f - keyWidth
        var y = panel.y + panel.height - 30f

        game.font.draw(game.batch, Skin.fullName(shown), left + 52f, y)
        y -= 46f
        y -= game.smallFont.draw(game.batch, info.summary, left, y, panel.width - 48f, Align.left, true).height + 16f

        for ((key, value) in info.rows) {
            game.smallFont.color = KEY_COLOR
            game.smallFont.draw(game.batch, key, left, y)
            game.smallFont.color = Color.WHITE
            y -= game.smallFont.draw(game.batch, value, left + keyWidth, y, valueWidth, Align.left, true).height + 12f
        }
    }

    private fun buttonAt(p: Vec2) = listOf(confirm, back).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        val tile = tiles.firstOrNull { it.second.contains(p.x, p.y) }?.first
        if (tile != null) shown = tile
        val slot = slots.indexOfFirst { it.contains(p.x, p.y) }
        when {
            slot in selected.indices -> {
                shown = selected[slot]
                selected.removeAt(slot)
                tap.cancel()
            }
            tile != null && tile in selected -> selected.remove(tile)
            tile != null && selected.size < DeckCodec.SIZE -> selected.add(tile)
            tile != null -> tap.cancel()
            else -> when (tap.release(buttonAt(p))) {
                confirm -> {
                    game.save.deck = selected.toList()
                    game.showMenu()
                }
                back -> game.showMenu()
            }
        }
    }

    private companion object {
        const val COLS = 7
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
