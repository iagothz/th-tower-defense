package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Vec2

class GameOverScreen(game: MergeTdGame, private val wave: Int, private val isRecord: Boolean) : BaseScreen(game) {
    private val menu = Button(Rectangle(160f, 440f, 400f, 110f), "Menu")
    private val tap = Tap<Button>()

    override fun draw(delta: Float) {
        menu.pressed = tap.current === menu
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)
        menu.drawShape(game.shapes)
        game.shapes.end()

        game.batch.begin()
        game.batch.textCentered(game.bigFont, "Fim de jogo", 360f, 900f)
        game.batch.textCentered(game.font, "Onda $wave", 360f, 780f)
        if (isRecord) {
            game.font.color = Color.GOLD
            game.batch.textCentered(game.font, "Novo recorde!", 360f, 700f)
            game.font.color = Color.WHITE
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
}
