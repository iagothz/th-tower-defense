package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.sin
import kotlin.random.Random

class MenuScreen(game: MergeTdGame) : BaseScreen(game) {
    /** A decorative die drifting down behind the menu. */
    private class Drop(var type: DieType, var pips: Int, var x: Float, var y: Float, var size: Float, var speed: Float, val sway: Float)

    private val play = Button(Rectangle(160f, 620f, 400f, 110f), "Jogar")
    private val deck = Button(Rectangle(160f, 470f, 400f, 110f), "Deck")
    private val sound = Button(Rectangle(160f, 320f, 400f, 110f), "", color = Skin.PANEL)
    private val buttons = listOf(play, deck, sound)
    private val tap = Tap<Button>()
    private val rng = Random(System.nanoTime())
    private val drops = List(DROPS) { newDrop(rng.nextFloat() * Layout.WORLD_H) }
    private var time = 0f

    private fun newDrop(y: Float) = Drop(
        DieType.entries.random(rng),
        1 + rng.nextInt(7),
        40f + rng.nextFloat() * (Layout.WORLD_W - 80f),
        y,
        44f + rng.nextFloat() * 40f,
        25f + rng.nextFloat() * 35f,
        rng.nextFloat() * 6.28f,
    )

    override fun draw(delta: Float) {
        time += delta
        sound.text = if (game.save.soundOn) "Som: ligado" else "Som: desligado"
        buttons.forEach { it.pressed = tap.current === it }
        for (d in drops) {
            d.y -= d.speed * delta
            if (d.y < -60f) {
                val fresh = newDrop(Layout.WORLD_H + 60f)
                d.type = fresh.type
                d.pips = fresh.pips
                d.x = fresh.x
                d.y = fresh.y
                d.size = fresh.size
                d.speed = fresh.speed
            }
        }

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (d in drops) shapes.die(d.type, d.pips, d.x + sin(time * 0.7f + d.sway) * 12f, d.y, d.size, 0.75f, 0.45f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(200f, 812f, 320f, 56f, 28f)
        shapes.color = Skin.GOLD
        shapes.rectLine(236f, 826f, 236f, 856f, 3f)
        shapes.triangle(237f, 856f, 237f, 840f, 259f, 848f)
        play.drawShape(shapes)
        deck.drawShape(shapes)
        sound.drawShape(shapes)
        // Three little dice sitting on the title line.
        shapes.die(DieType.FIRE, 3, 236f, 1046f, 44f)
        shapes.die(DieType.ELECTRIC, 5, 360f, 1058f, 52f)
        shapes.die(DieType.ICE, 2, 484f, 1046f, 44f)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, "Merge TD", 360f, 960f)
        game.batch.textCentered(game.smallFont, "Junte dados iguais, segure as ondas", 360f, 900f)
        game.batch.textLeft(game.font, "Recorde: onda ${game.save.bestWave}", 272f, 840f)
        play.drawText(game.batch, game.font)
        deck.drawText(game.batch, game.font)
        sound.drawText(game.batch, game.font)
        game.smallFont.color = Color(1f, 1f, 1f, 0.4f)
        game.batch.textCentered(game.smallFont, "v${MergeTdGame.VERSION}", 360f, 40f)
        game.smallFont.color = Color.WHITE
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

    private companion object {
        const val DROPS = 12
    }
}
