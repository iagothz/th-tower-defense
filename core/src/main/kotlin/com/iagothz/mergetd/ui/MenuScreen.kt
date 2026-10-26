package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

class MenuScreen(game: MergeTdGame) : BaseScreen(game) {
    private val play = Button(Rectangle(160f, 620f, 400f, 110f), "Jogar")
    private val deck = Button(Rectangle(160f, 470f, 400f, 110f), "Deck")

    override fun draw(delta: Float) {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)
        play.drawShape(game.shapes)
        deck.drawShape(game.shapes)
        game.shapes.end()

        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Merge TD", 360f, 960f)
        game.batch.textCentered(game.font, "Recorde: onda ${game.save.bestWave}", 360f, 840f)
        play.drawText(game.batch, game.font)
        deck.drawText(game.batch, game.font)
        game.batch.end()
    }

    override fun onTouchUp(p: Vec2) {
        when {
            play.contains(p.x, p.y) -> game.startMatch()
            deck.contains(p.x, p.y) -> game.showDeck()
        }
    }
}
