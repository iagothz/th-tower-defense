package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.Vec2
import java.util.Locale

/** Volumes, vibration and language, plus the privacy line the store asks for. */
class SettingsScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.SETTINGS

    private val header = ScreenHeader(S["Configurações"])

    private val soundSlider = Rectangle(60f, 980f, 600f, 40f)
    private val musicSlider = Rectangle(60f, 860f, 600f, 40f)
    private val reduced = { game.save.reduceMotion }
    private val vibration = Toggle(Rectangle(60f, 748f, 600f, 72f), S["Vibração"], game.save.vibrationOn, reduced)
    private val motion = Toggle(Rectangle(60f, 664f, 600f, 72f), S["Reduzir movimento"], game.save.reduceMotion, reduced)
    private val largeText = Toggle(Rectangle(60f, 580f, 600f, 72f), S["Texto grande"], game.save.largeText, reduced)
    private val toggles = listOf(vibration, motion, largeText)
    private val languages = Language.entries.mapIndexed { i, l -> l to Button(Rectangle(60f + i * 204f, 432f, 192f, 80f), S[l.title], style = ButtonStyle.SECONDARY) }
    private val stats = Button(Rectangle(60f, 124f, 192f, 80f), S["Estatísticas"], style = ButtonStyle.SECONDARY)
    private val achievements = Button(Rectangle(264f, 124f, 192f, 80f), S["Conquistas"], style = ButtonStyle.SECONDARY)
    private val history = Button(Rectangle(468f, 124f, 192f, 80f), S["Histórico"], style = ButtonStyle.SECONDARY)
    private val links = listOf(stats, achievements, history)

    /** The cloud row's button; its text follows the controller's state and it is drawn only when there is one. */
    private val cloudButton = Button(Rectangle(468f, 352f, 192f, 56f), "", style = ButtonStyle.SECONDARY)
    private val tap = Tap<Any>()
    private var dragging: Rectangle? = null

    override fun draw(delta: Float) {
        links.forEach { it.pressed = tap.current === it }
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawSlider(soundSlider, game.save.soundVolume)
        drawSlider(musicSlider, game.save.musicVolume)
        toggles.forEach { it.drawShapes(shapes) }
        for ((l, b) in languages) {
            b.style = if (l == game.save.language) ButtonStyle.SELECTED else ButtonStyle.SECONDARY
            b.pressed = tap.current === b
            b.drawShape(shapes)
        }
        links.forEach { it.drawShape(shapes) }
        cloudLabel()?.let {
            cloudButton.text = it
            cloudButton.pressed = tap.current === cloudButton
            cloudButton.drawShape(shapes)
        }
        shapes.end()

        game.batch.begin()
        header.drawText(game.batch, game)
        label(S["Som"], game.save.soundVolume, soundSlider)
        label(S["Música"], game.save.musicVolume, musicSlider)
        toggles.forEach { it.drawText(game.batch, game.smallFont) }
        game.smallFont.color = Skin.MUTED
        game.batch.textLeft(game.smallFont, S["Idioma"], 60f, 540f)
        game.smallFont.color = Color.WHITE
        for ((_, b) in languages) b.drawText(game.batch, game.smallFont)
        val cloud = game.cloud.status
        if (cloud != CloudController.Status.Unavailable) {
            game.smallFont.color = Skin.MUTED
            game.batch.textLeft(game.smallFont, CloudText.line(cloud, DailyChallenge.today()), 60f, 388f)
            game.smallFont.color = Color.WHITE
            if (cloudLabel() != null) cloudButton.drawText(game.batch, game.smallFont)
        }
        game.tinyFont.color = Skin.MUTED
        val privacy = if (cloud == CloudController.Status.Unavailable) {
            "Privacidade: este jogo funciona sem internet, não coleta nem envia nenhum dado."
        } else {
            "Privacidade: este jogo funciona sem internet e não coleta nada. Com a nuvem ligada, seu progresso fica na sua conta do Google Play Games."
        }
        game.tinyFont.draw(game.batch, S[privacy], 60f, 330f, 600f, Align.left, true)
        game.tinyFont.color = Color.WHITE
        links.forEach { it.drawText(game.batch, game.smallFont) }
        game.batch.end()
    }

    private fun drawSlider(r: Rectangle, value: Float) {
        val shapes = game.shapes
        shapes.color = Skin.PANEL
        shapes.roundedRect(r.x, r.y, r.width, r.height, r.height / 2)
        shapes.color = Skin.GOLD
        if (value > 0f) shapes.roundedRect(r.x, r.y, maxOf(r.height, r.width * value), r.height, r.height / 2)
        shapes.color = Color.WHITE
        shapes.circle(r.x + r.width * value, r.y + r.height / 2, 26f, 24)
    }

    private fun label(text: String, value: Float, r: Rectangle) {
        game.smallFont.color = Skin.MUTED
        game.batch.textLeft(game.smallFont, text, r.x, r.y + r.height + 30f)
        game.smallFont.color = Color.WHITE
        game.batch.textRight(game.smallFont, "${(value * 100).toInt()}%", r.x + r.width, r.y + r.height + 30f)
    }

    private fun sliderAt(p: Vec2): Rectangle? = listOf(soundSlider, musicSlider).firstOrNull {
        p.x >= it.x - 30f && p.x <= it.x + it.width + 30f && p.y >= it.y - 30f && p.y <= it.y + it.height + 30f
    }

    private fun setSlider(r: Rectangle, p: Vec2) {
        val v = ((p.x - r.x) / r.width).coerceIn(0f, 1f)
        if (r === soundSlider) game.save.soundVolume = v else game.save.musicVolume = v
    }

    private fun cloudLabel(): String? = CloudText.button(game.cloud.status)?.let { S[it] }

    private fun buttonAt(p: Vec2): Any? =
        toggles.firstOrNull { it.contains(p.x, p.y) } ?: (links + languages.map { it.second } + listOfNotNull(cloudButton.takeIf { cloudLabel() != null })).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) {
        dragging = sliderAt(p)?.also { setSlider(it, p) }
        if (dragging == null) tap.press(buttonAt(p))
    }

    override fun onDrag(p: Vec2) {
        dragging?.let { setSlider(it, p) }
    }

    override fun onTouchCancelled() {
        dragging = null
        tap.cancel()
    }

    override fun onTouchUp(p: Vec2) {
        if (dragging != null) {
            dragging = null
            game.sfx.click()
            return
        }
        when (val b = tap.release(buttonAt(p))) {
            cloudButton -> when (game.cloud.status) {
                CloudController.Status.SignedOut -> game.cloud.signIn()
                CloudController.Status.Off -> game.cloud.setEnabled(true)
                is CloudController.Status.Synced, CloudController.Status.Offline -> game.cloud.setEnabled(false)
                is CloudController.Status.Error -> game.cloud.open()
                else -> Unit
            }
            stats -> game.showStats()
            achievements -> game.showAchievements { game.showSettings() }
            history -> game.showHistory { game.showSettings() }
            vibration -> {
                vibration.toggle()
                game.save.vibrationOn = vibration.on
                if (game.save.vibrationOn) game.sfx.play(setOf("boss"))
            }
            motion -> {
                motion.toggle()
                game.save.reduceMotion = motion.on
            }
            largeText -> {
                largeText.toggle()
                game.save.largeText = largeText.on
                game.buildFonts()
                game.showSettings()
            }
            null -> Unit
            else -> languages.firstOrNull { it.second === b }?.let { (l, _) ->
                game.save.language = l
                S.language = Language.resolve(l, Locale.getDefault().language)
                game.showSettings()
            }
        }
    }
}
