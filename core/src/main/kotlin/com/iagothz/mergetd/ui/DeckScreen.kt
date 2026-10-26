package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2

class DeckScreen(game: MergeTdGame) : BaseScreen(game) {
    private val selected = game.save.deck.toMutableList()
    private val tiles = DieType.entries.mapIndexed { i, t ->
        t to Rectangle(40f + (i % 5) * 132f, 760f - (i / 5) * 200f, 112f, 112f)
    }
    private val confirm = Button(Rectangle(160f, 300f, 400f, 110f), "Confirmar")
    private val back = Button(Rectangle(160f, 160f, 400f, 100f), "Voltar", color = Skin.PANEL)

    override fun draw(delta: Float) {
        confirm.enabled = selected.size == DeckCodec.SIZE
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((t, r) in tiles) {
            if (t in selected) {
                shapes.color = Color.WHITE
                shapes.roundedRect(r.x - 8, r.y - 8, r.width + 16, r.height + 16, 22f)
            }
            shapes.color = Skin.color(t)
            shapes.roundedRect(r.x, r.y, r.width, r.height, 18f)
        }
        confirm.drawShape(shapes)
        back.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Deck", 360f, 1100f)
        game.batch.textCentered(game.font, "Escolha 5 dados (${selected.size}/5)", 360f, 1000f)
        for ((t, r) in tiles) game.batch.textCentered(game.font, Skin.label(t), r.x + r.width / 2, r.y - 30f)
        confirm.drawText(game.batch, game.font)
        back.drawText(game.batch, game.font)
        game.batch.end()
    }

    override fun onTouchUp(p: Vec2) {
        val tile = tiles.firstOrNull { it.second.contains(p.x, p.y) }?.first
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
}
