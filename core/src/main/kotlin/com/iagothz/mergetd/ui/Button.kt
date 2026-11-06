package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import kotlin.math.abs
import kotlin.math.sin

/** What a button is for, which sets its fill: one primary action per screen, danger for losing progress. */
enum class ButtonStyle(val fill: Color) {
    PRIMARY(Skin.PRIMARY),
    SECONDARY(Skin.PANEL),
    DANGER(Skin.DANGER),
    SELECTED(Skin.SELECTED),
    REWARD(Skin.REWARD),
}

/** [color], when set, overrides the style's fill (power-ups wear their die's colour). */
class Button(
    val rect: Rectangle,
    var text: String,
    var enabled: Boolean = true,
    var style: ButtonStyle = ButtonStyle.PRIMARY,
    var color: Color? = null,
) {
    val fill: Color get() = color ?: style.fill

    /** Text colour that reads on [fill]. */
    val ink: Color get() = Skin.inkOn(fill)

    /** True while a finger is down on this button; it sinks and darkens. */
    var pressed = false

    /** Drawn size relative to [rect], about its centre; a pulse or a pop, never the hit area. */
    var scale = 1f

    /** 0 up, 1 fully sunk; eases towards [pressed] a few frames at a time. */
    var press = 0f
        private set
    private var denyLeft = 0f

    fun contains(x: Float, y: Float): Boolean = enabled && rect.contains(x, y)

    /** Ages the press and the deny shake; [drawShape] calls it with the frame time. */
    fun update(dt: Float) {
        val target = if (pressed && enabled) 1f else 0f
        press += (target - press) * minOf(1f, dt * 25f)
        if (abs(target - press) < 0.02f) press = target
        denyLeft = maxOf(0f, denyLeft - dt)
    }

    /** Tapped while disabled: shakes sideways for a moment to say no. */
    fun deny() {
        denyLeft = DENY_TIME
    }

    fun shakeX(): Float = if (denyLeft <= 0f) 0f else sin(denyLeft * 70f) * 7f * (denyLeft / DENY_TIME)

    fun drawShape(shapes: ShapeRenderer) {
        update(Gdx.graphics.deltaTime)
        val base = if (enabled) fill else DISABLED
        val inset = 3f * press
        val shade = if (press > 0f) tmp.set(base).lerp(Color.BLACK, 0.18f * press) else base
        val w = rect.width * scale
        val h = rect.height * scale
        val x = rect.x + (rect.width - w) / 2 + shakeX()
        val y = rect.y + (rect.height - h) / 2
        shapes.bevelRect(x + inset, y + inset, w - 2 * inset, h - 2 * inset, 16f, shade, 5f - 3f * press)
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont) {
        font.color = if (enabled) ink else Color.LIGHT_GRAY
        batch.textCentered(font, text, rect.x + rect.width / 2 + shakeX(), rect.y + rect.height / 2 + 2f - 2f * press)
        font.color = Color.WHITE
    }

    companion object {
        const val DENY_TIME = 0.25f
        private val DISABLED = Color(0.3f, 0.3f, 0.34f, 1f)
        private val tmp = Color()
    }
}
