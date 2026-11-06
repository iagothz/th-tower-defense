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
import com.iagothz.mergetd.logic.WeeklyChallenge
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Match
import java.util.Locale
import com.iagothz.mergetd.logic.MatchSetup

/** [devMode] enables desktop-only cheat keys and disables record saving. */
/**
 * [shareText] is the platform's share sheet; without one, sharing copies to the clipboard. [cloudSave] is the
 * account's online save; without one the cloud row never shows and everything stays on the device.
 */
class MergeTdGame(val devMode: Boolean = false, private val shareText: ((String) -> Unit)? = null, private val cloudSave: CloudSave = NoCloud) : Game() {
    lateinit var shapes: ShapeRenderer
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var bigFont: BitmapFont
    lateinit var smallFont: BitmapFont
    lateinit var tinyFont: BitmapFont
    lateinit var save: SaveData
    lateinit var sfx: Sfx
    lateinit var cloud: CloudController
    val backdrop = Backdrop()
    val fader = Fader()
    /** Seconds since launch, for ambient animation. */
    var clock = 0f
        private set
    private var next: Screen? = null

    override fun create() {
        // Back navigates inside the game (see BaseScreen.onBack) instead of closing it.
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        save = SaveData(Gdx.app.getPreferences("merge-td"))
        buildFonts()
        sfx = Sfx(save)
        Cosmetics.load(save)
        save.retroCards(kotlin.random.Random(System.nanoTime()))
        S.language = Language.resolve(save.language, Locale.getDefault().language)
        cloud = CloudController(save, cloudSave).also {
            it.onAdopted = {
                Cosmetics.load(save)
                showTab(Tab.HOME)
            }
            it.onConflict = { blob -> showCloudConflict(blob) }
        }
        if (devMode || !resumeSavedMatch()) showMenu()
        cloud.open()
    }

    /** Back from the background: another device may have played meanwhile. */
    override fun resume() {
        super.resume()
        if (::cloud.isInitialized) cloud.open()
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime
        clock += dt
        if (fader.update(dt)) next?.let { apply(it) }
        sfx.updateMusic(dt)
        cloud.tick(dt)
        super.render()
    }

    fun showMenu() = switchTo(MenuScreen(this))

    /** Jumps to a bar tab with no fade: the bar stays put and only the content above it changes. */
    fun showTab(tab: Tab) = apply(
        when (tab) {
            Tab.HOME -> MenuScreen(this)
            Tab.DECK -> DeckScreen(this)
            Tab.MAPS -> MapScreen(this)
            Tab.CHALLENGES -> ChallengeScreen(this)
            Tab.SETTINGS -> SettingsScreen(this)
        },
    )

    fun showDeck() = switchTo(DeckScreen(this))

    fun showCloudConflict(blob: String) = switchTo(CloudConflictScreen(this, blob))

    fun showEnemies() = switchTo(EnemiesScreen(this))

    fun showMap() = switchTo(MapScreen(this))

    fun showStats() = switchTo(StatsScreen(this))

    fun showSettings() = switchTo(SettingsScreen(this))

    /** Today's challenge: fixed map, deck and seed; its record is per day. */
    fun startDaily() {
        save.clearMatch()
        val c = DailyChallenge.of(DailyChallenge.today())
        switchTo(MatchScreen(this, previousBest = save.dailyBest(c.day), fresh = c.newMatch(save.dieLevels())))
    }

    fun startMatch() {
        save.clearMatch()
        switchTo(MatchScreen(this))
    }

    fun showChallenges() = switchTo(ChallengeScreen(this))

    /** This week's fixed run with its two mutators, booked against the week's score. */
    fun startWeekly() {
        save.clearMatch()
        val w = WeeklyChallenge.of(WeeklyChallenge.thisWeek())
        switchTo(MatchScreen(this, previousBest = save.weeklyBest(w.week), fresh = w.newMatch(save.dieLevels())))
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
        mode: GameMode = GameMode.NORMAL, unlocked: Set<Achievement> = emptySet(), code: String? = null, reward: MatchReward? = null,
        score: Int? = null, week: Int? = null,
    ) = switchTo(GameOverScreen(this, map, wave, isRecord, stats, challengeDay, mode, unlocked, code, reward, score, week))

    /** Hands [text] to the share sheet; returns false when it was only copied to the clipboard. */
    fun share(text: String): Boolean {
        val sheet = shareText
        if (sheet != null) {
            sheet(text)
            return true
        }
        Gdx.app.clipboard.contents = text
        return false
    }

    /** [back] leads to the screen it was opened from: Config or Estatísticas. */
    fun showHistory(back: () -> Unit) = switchTo(HistoryScreen(this, back))

    /** Replays a shared match: its map, mode, deck and seed, booked for the totals only. */
    fun startFromCode(setup: MatchSetup) {
        save.clearMatch()
        switchTo(MatchScreen(this, previousBest = save.bestWave(setup.map, setup.mode), fresh = setup.newMatch(save.dieLevels())))
    }

    /** [back] leads to the screen it was opened from: Config or Estatísticas. */
    fun showAchievements(back: () -> Unit) = switchTo(AchievementsScreen(this, back))

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
        sfx.calm()
        setScreen(screen)
        old?.dispose()
        next = null
        cloud.inMatch = screen is MatchScreen || screen is GameOverScreen
    }

    /** Rasterizes the TTF at [size] world pixels, including Portuguese accented letters. */
    /** Rasterizes the four fonts, 15% bigger with the large-text setting; call again when that setting changes. */
    fun buildFonts() {
        if (::font.isInitialized) listOf(font, bigFont, smallFont, tinyFont).forEach { it.dispose() }
        val scale = if (save.largeText) LARGE_TEXT else 1f
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Medium.ttf"))
        font = newFont(generator, (34 * scale).toInt())
        bigFont = newFont(generator, (64 * scale).toInt())
        smallFont = newFont(generator, (24 * scale).toInt())
        tinyFont = newFont(generator, (19 * scale).toInt())
        generator.dispose()
    }

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
        const val LARGE_TEXT = 1.15f
        const val VERSION = "0.7.0"
    }
}
