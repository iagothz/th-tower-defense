package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Vec2

/** Screen with a 720×1280 FitViewport; subclasses draw in world units and get touches in world units. */
abstract class BaseScreen(protected val game: MergeTdGame) : ScreenAdapter() {
    protected val viewport = FitViewport(Layout.WORLD_W, Layout.WORLD_H)
    private val tmp = Vector2()
    /** Ignores releases of touches that began on a previous screen. */
    private val gesture = Tap<Unit>()

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0) {
                gesture.press(Unit)
                onTouchDown(world(screenX, screenY))
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer == 0) onDrag(world(screenX, screenY))
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0 && gesture.release(Unit) != null) onTouchUp(world(screenX, screenY))
            return true
        }

        override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == 0) {
                gesture.cancel()
                onTouchCancelled()
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
        viewport.camera.update()
        game.shapes.projectionMatrix = viewport.camera.combined
        game.batch.projectionMatrix = viewport.camera.combined
        draw(delta)
    }

    abstract fun draw(delta: Float)

    /** Camera offset for this frame, in world units. */
    open fun cameraShake(): Vec2 = NO_SHAKE

    open fun onTouchDown(p: Vec2) {}
    open fun onDrag(p: Vec2) {}
    open fun onTouchUp(p: Vec2) {}
    open fun onTouchCancelled() {}

    private companion object {
        val NO_SHAKE = Vec2(0f, 0f)
    }

    private fun world(x: Int, y: Int): Vec2 {
        viewport.unproject(tmp.set(x.toFloat(), y.toFloat()))
        return Vec2(tmp.x, tmp.y)
    }
}
