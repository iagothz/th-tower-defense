package com.iagothz.mergetd.ui

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Match
import java.util.Locale
import com.iagothz.mergetd.logic.MatchSetup

/** [devMode] enables desktop-only cheat keys and disables record saving. */
class MergeTdGame(val devMode: Boolean = false) : Game() {
    lateinit var shapes: ShapeRenderer
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var bigFont: BitmapFont
    lateinit var smallFont: BitmapFont
    lateinit var tinyFont: BitmapFont
    lateinit var save: SaveData
    lateinit var sfx: Sfx
    val backdrop = Backdrop()
    val fader = Fader()
    /** Seconds since launch, for ambient animation. */
    var clock = 0f
        private set
    private var next: Screen? = null

    override fun create() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Medium.ttf"))
        font = newFont(generator, 34)
        bigFont = newFont(generator, 64)
        smallFont = newFont(generator, 24)
        tinyFont = newFont(generator, 19)
        generator.dispose()
        save = SaveData(Gdx.app.getPreferences("merge-td"))
        sfx = Sfx(save)
        Cosmetics.load(save)
        S.language = Language.resolve(save.language, Locale.getDefault().language)
        if (devMode || !resumeSavedMatch()) showMenu()
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime
        clock += dt
        if (fader.update(dt)) next?.let { apply(it) }
        sfx.updateMusic()
        super.render()
    }

    fun showMenu() = switchTo(MenuScreen(this))

    fun showDeck() = switchTo(DeckScreen(this))

    fun showMap() = switchTo(MapScreen(this))

    fun showStats() = switchTo(StatsScreen(this))

    fun showSettings() = switchTo(SettingsScreen(this))

    /** Today's challenge: fixed map, deck and seed; its record is per day. */
    fun startDaily() {
        save.clearMatch()
        val c = DailyChallenge.of(DailyChallenge.today())
        switchTo(MatchScreen(this, previousBest = save.dailyBest(c.day), fresh = c.newMatch()))
    }

    fun startMatch() {
        save.clearMatch()
        switchTo(MatchScreen(this))
    }

    /** Reopens the match Android closed while the app was in the background; false if there is none. */
    private fun resumeSavedMatch(): Boolean {
        val (text, previousBest) = save.savedMatch ?: return false
        val match = Match.restore(text)
        if (match == null) {
            save.clearMatch()
            return false
        }
        switchTo(MatchScreen(this, match, previousBest))
        return true
    }

    fun showGameOver(
        map: GameMap, wave: Int, isRecord: Boolean, stats: MatchStats, challengeDay: Int? = null,
        mode: GameMode = GameMode.NORMAL, unlocked: Set<Achievement> = emptySet(), code: String? = null,
    ) = switchTo(GameOverScreen(this, map, wave, isRecord, stats, challengeDay, mode, unlocked, code))

    fun showHistory() = switchTo(HistoryScreen(this))

    /** Replays a shared match: its map, mode, deck and seed, booked for the totals only. */
    fun startFromCode(setup: MatchSetup) {
        save.clearMatch()
        switchTo(MatchScreen(this, previousBest = save.bestWave(setup.map, setup.mode), fresh = setup.newMatch()))
    }

    fun showAchievements() = switchTo(AchievementsScreen(this))

    /** Fades to black, swaps the screen, fades back in. The first screen, or one requested mid-fade, swaps at once. */
    private fun switchTo(screen: Screen) {
        if (this.screen == null || !fader.start()) {
            apply(screen)
            return
        }
        next = screen
    }

    private fun apply(screen: Screen) {
        val old = this.screen
        setScreen(screen)
        old?.dispose()
        next = null
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
        tinyFont.dispose()
        sfx.dispose()
    }

    companion object {
        const val VERSION = "0.5.0"
    }
}
