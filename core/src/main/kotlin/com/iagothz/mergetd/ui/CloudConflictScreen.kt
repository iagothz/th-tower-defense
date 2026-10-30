package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.Vec2

/**
 * Both this device and the cloud moved on since they last agreed: the player picks which progress to keep.
 * No bar, no back: the choice is the only way out, and either one leads to the home tab.
 */
class CloudConflictScreen(game: MergeTdGame, private val cloudBlob: String) : BaseScreen(game) {
    private val local = Rectangle(40f, 700f, 640f, 300f)
    private val cloud = Rectangle(40f, 360f, 640f, 300f)
    private val keepLocal = Button(Rectangle(local.x + 40f, local.y + 24f, 560f, 80f), S["Manter este"])
    private val keepCloud = Button(Rectangle(cloud.x + 40f, cloud.y + 24f, 560f, 80f), S["Manter a nuvem"], color = Color(0.55f, 0.30f, 0.75f, 1f))
    private val tap = Tap<Button>()
    private val mine = CloudBlob.summary(game.save.export())
    private val theirs = CloudBlob.summary(cloudBlob)

    override fun draw(delta: Float) {
        keepLocal.pressed = tap.current === keepLocal
        keepCloud.pressed = tap.current === keepCloud
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (p in listOf(local, cloud)) {
            shapes.color = Skin.SHADOW
            shapes.roundedRect(p.x + 3f, p.y - 8f, p.width, p.height, 22f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(p.x, p.y, p.width, p.height, 22f)
        }
        keepLocal.drawShape(shapes)
        keepCloud.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Qual progresso manter?"], 360f, 1100f)
        card(local, S["Este aparelho"], mine, null)
        card(cloud, S["Nuvem"], theirs, theirs.day)
        keepLocal.drawText(game.batch, game.font)
        keepCloud.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun card(r: Rectangle, title: String, s: CloudBlob.Summary, day: Int?) {
        game.font.color = Skin.GOLD
        game.batch.textLeft(game.font, title, r.x + 32f, r.y + r.height - 40f)
        game.font.color = Color.WHITE
        game.batch.textLeft(game.smallFont, S.f("nível %d · %d partidas", s.level, s.games), r.x + 32f, r.y + r.height - 92f)
        if (day != null) {
            val ago = DailyChallenge.today() - day
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, if (ago <= 0) S["hoje"] else S.f("há %d dias", ago), r.x + 32f, r.y + r.height - 132f)
            game.smallFont.color = Color.WHITE
        }
    }

    private fun buttonAt(p: Vec2) = listOf(keepLocal, keepCloud).firstOrNull { it.contains(p.x, p.y) }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        when (tap.release(buttonAt(p))) {
            keepLocal -> game.cloud.keepLocal()
            keepCloud -> game.cloud.keepCloud()
            else -> return
        }
        game.sfx.click()
        game.showTab(Tab.HOME)
    }

    private companion object {
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
