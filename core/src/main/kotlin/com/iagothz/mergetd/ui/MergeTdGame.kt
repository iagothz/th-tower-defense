package com.iagothz.mergetd.ui

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class MergeTdGame : Game() {
    lateinit var shapes: ShapeRenderer
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var bigFont: BitmapFont
    lateinit var save: SaveData

    override fun create() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        font = newFont(2.2f)
        bigFont = newFont(4f)
        save = SaveData(Gdx.app.getPreferences("merge-td"))
        showMenu()
    }

    fun showMenu() = switchTo(MenuScreen(this))

    fun showDeck() = switchTo(DeckScreen(this))

    fun startMatch() = showGameOver(0, false)

    fun showGameOver(wave: Int, isRecord: Boolean) = switchTo(GameOverScreen(this, wave, isRecord))

    private fun switchTo(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    private fun newFont(scale: Float) = BitmapFont().apply {
        data.setScale(scale)
        region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setUseIntegerPositions(false)
    }

    override fun dispose() {
        screen?.dispose()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        bigFont.dispose()
    }
}
