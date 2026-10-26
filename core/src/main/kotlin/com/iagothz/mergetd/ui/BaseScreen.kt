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
        game.shapes.projectionMatrix = viewport.camera.combined
        game.batch.projectionMatrix = viewport.camera.combined
        draw(delta)
    }

    abstract fun draw(delta: Float)

    open fun onTouchDown(p: Vec2) {}
    open fun onDrag(p: Vec2) {}
    open fun onTouchUp(p: Vec2) {}
    open fun onTouchCancelled() {}

    private fun world(x: Int, y: Int): Vec2 {
        viewport.unproject(tmp.set(x.toFloat(), y.toFloat()))
        return Vec2(tmp.x, tmp.y)
    }
}
