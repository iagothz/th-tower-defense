package com.iagothz.mergetd.ui

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class MergeTdGame : Game() {
    lateinit var shapes: ShapeRenderer
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var bigFont: BitmapFont
    lateinit var smallFont: BitmapFont
    lateinit var save: SaveData
    lateinit var sfx: Sfx

    override fun create() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Medium.ttf"))
        font = newFont(generator, 34)
        bigFont = newFont(generator, 64)
        smallFont = newFont(generator, 24)
        generator.dispose()
        save = SaveData(Gdx.app.getPreferences("merge-td"))
        sfx = Sfx(save)
        showMenu()
    }

    fun showMenu() = switchTo(MenuScreen(this))

    fun showDeck() = switchTo(DeckScreen(this))

    fun startMatch() = switchTo(MatchScreen(this))

    fun showGameOver(wave: Int, isRecord: Boolean) = switchTo(GameOverScreen(this, wave, isRecord))

    private fun switchTo(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    /** Rasterizes the TTF at [size] world pixels, including Portuguese accented letters. */
    private fun newFont(generator: FreeTypeFontGenerator, size: Int): BitmapFont {
        val params = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            this.size = size
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ"
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
        }
        return generator.generateFont(params).apply { setUseIntegerPositions(false) }
    }

    override fun dispose() {
        screen?.dispose()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        bigFont.dispose()
        smallFont.dispose()
        sfx.dispose()
    }
}
