package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2

/**
 * Screen on a 720×1280 world that extends (never crops) to the device's aspect: taller phones get more sky above
 * and below, with the HUD and board exactly where they are on 9:16. Subclasses draw and get touches in world units.
 */
abstract class BaseScreen(protected val game: MergeTdGame) : ScreenAdapter() {
    protected val viewport = ExtendViewport(Layout.WORLD_W, Layout.WORLD_H)
    private val tmp = Vector2()
    /** Ignores releases of touches that began on a previous screen. */
    private val gesture = Tap<Unit>()

    /** The bottom bar's tab this screen belongs to; null for screens without the bar (the match, game over). */
    open val tab: Tab? get() = null

    private val nav: BottomNav? by lazy { tab?.let { BottomNav(it).apply { if (game.save.canLevelUpAny()) badges = setOf(Tab.DECK) } } }
    /** True while the current touch began on the bar, so the screen never sees it. */
    private var onBar = false

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0) {
                gesture.press(Unit)
                val p = world(screenX, screenY)
                val bar = nav
                onBar = bar != null && bar.tabAt(p.x, p.y) != null
                if (onBar) bar!!.pressed = bar.tabAt(p.x, p.y) else onTouchDown(p)
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer == 0 && !onBar) onDrag(world(screenX, screenY))
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0 && gesture.release(Unit) != null) {
                val p = world(screenX, screenY)
                val bar = nav
                if (onBar && bar != null) {
                    val hit = bar.tabAt(p.x, p.y)
                    bar.pressed = null
                    if (hit != null && hit == bar.pressedWas) game.showTab(hit)
                } else if (!onBar) onTouchUp(p)
                onBar = false
            }
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            onKey(keycode)
            return true
        }

        override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0) {
                gesture.cancel()
                nav?.pressed = null
                if (!onBar) onTouchCancelled()
                onBar = false
            }
            return true
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = input
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(Skin.BACKGROUND)
        viewport.apply()
        val shake = cameraShake()
        viewport.camera.position.set(Layout.WORLD_W / 2 + shake.x, Layout.WORLD_H / 2 + shake.y, 0f)
        (viewport.camera as? OrthographicCamera)?.zoom = cameraZoom()
        viewport.camera.update()
        game.shapes.projectionMatrix = viewport.camera.combined
        game.batch.projectionMatrix = viewport.camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)
        game.backdrop.draw(game.shapes, game.clock, theme, viewport.worldWidth, viewport.worldHeight)
        val w = weather?.takeIf { it.kind == Weather.kindFor(skyMap) } ?: Weather(skyMap).also { weather = it }
        w.update(delta)
        w.draw(game.shapes)
        game.shapes.end()
        draw(delta)
        nav?.let { bar ->
            game.shapes.begin(ShapeRenderer.ShapeType.Filled)
            bar.draw(game.shapes, Layout.WORLD_H / 2 - viewport.worldHeight / 2)
            game.shapes.end()
            game.batch.begin()
            bar.drawText(game.batch, game.tinyFont)
            game.batch.end()
        }
        val fade = game.fader.alpha
        if (fade > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            game.shapes.begin(ShapeRenderer.ShapeType.Filled)
            game.shapes.setColor(0f, 0f, 0f, fade)
            game.shapes.rect(-400f, -400f, Layout.WORLD_W + 800f, Layout.WORLD_H + 800f)
            game.shapes.end()
        }
    }

    abstract fun draw(delta: Float)

    /** The map whose sky and weather sit behind this screen: the chosen one, unless the screen knows better. */
    open val skyMap: GameMap get() = game.save.map

    val theme: MapTheme get() = Skin.theme(skyMap)

    private var weather: Weather? = null

    /** Camera offset for this frame, in world units. */
    open fun cameraShake(): Vec2 = NO_SHAKE

    /** Camera zoom for this frame: 1 normally, below 1 leans in. */
    open fun cameraZoom(): Float = 1f

    open fun onTouchDown(p: Vec2) {}
    open fun onDrag(p: Vec2) {}
    open fun onTouchUp(p: Vec2) {}
    open fun onTouchCancelled() {}
    open fun onKey(keycode: Int) {}

    private companion object {
        val NO_SHAKE = Vec2(0f, 0f)
    }

    private fun world(x: Int, y: Int): Vec2 {
        viewport.unproject(tmp.set(x.toFloat(), y.toFloat()))
        return Vec2(tmp.x, tmp.y)
    }
}
