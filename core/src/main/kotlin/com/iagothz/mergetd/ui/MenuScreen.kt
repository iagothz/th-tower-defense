package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

class MenuScreen(game: MergeTdGame) : BaseScreen(game) {
    private val play = Button(Rectangle(160f, 620f, 400f, 110f), "Jogar")
    private val deck = Button(Rectangle(160f, 470f, 400f, 110f), "Deck")
    private val sound = Button(Rectangle(160f, 320f, 400f, 110f), "", color = Skin.PANEL)
    private val buttons = listOf(play, deck, sound)
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        sound.text = if (game.save.soundOn) "Som: ligado" else "Som: desligado"
        buttons.forEach { it.pressed = tap.current === it }
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)
        play.drawShape(game.shapes)
        deck.drawShape(game.shapes)
        sound.drawShape(game.shapes)
        game.shapes.end()

        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Merge TD", 360f, 960f)
        game.batch.textCentered(game.font, "Recorde: onda ${game.save.bestWave}", 360f, 840f)
        play.drawText(game.batch, game.font)
        deck.drawText(game.batch, game.font)
        sound.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun buttonAt(p: Vec2) = buttons.firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            play -> game.startMatch()
            deck -> game.showDeck()
            sound -> {
                game.save.soundOn = !game.save.soundOn
                game.sfx.click()
            }
        }
    }
}
