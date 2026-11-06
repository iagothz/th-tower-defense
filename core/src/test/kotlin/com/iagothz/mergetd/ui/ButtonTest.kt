package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ButtonTest {
    @Test
    fun pressSinksGraduallyAndRises() {
        val b = Button(Rectangle(0f, 0f, 100f, 50f), "x")
        assertEquals(0f, b.press)
        b.pressed = true
        b.update(0.016f)
        assertTrue(b.press > 0f && b.press < 1f, "${b.press}")
        b.update(0.5f)
        assertEquals(1f, b.press, 0.01f)
        b.pressed = false
        b.update(0.5f)
        assertEquals(0f, b.press, 0.01f)
    }

    @Test
    fun denyShakesSidewaysThenStops() {
        val b = Button(Rectangle(0f, 0f, 100f, 50f), "x", enabled = false)
        assertEquals(0f, b.shakeX())
        b.deny()
        b.update(0.02f)
        assertTrue(b.shakeX() != 0f)
        b.update(Button.DENY_TIME)
        assertEquals(0f, b.shakeX())
    }

    @Test
    fun styleSetsTheFill() {
        val r = Rectangle(0f, 0f, 100f, 50f)
        assertEquals(Skin.PRIMARY, Button(r, "x").fill)
        assertEquals(Skin.PANEL, Button(r, "x", style = ButtonStyle.SECONDARY).fill)
        assertEquals(Skin.DANGER, Button(r, "x", style = ButtonStyle.DANGER).fill)
        assertEquals(Skin.SELECTED, Button(r, "x", style = ButtonStyle.SELECTED).fill)
        assertEquals(Skin.REWARD, Button(r, "x", style = ButtonStyle.REWARD).fill)
    }

    @Test
    fun ownColourWinsOverTheStyle() {
        val b = Button(Rectangle(0f, 0f, 100f, 50f), "x", style = ButtonStyle.SECONDARY, color = Skin.GOLD)
        assertEquals(Skin.GOLD, b.fill)
        assertEquals(Skin.INK_DARK, b.ink)
    }

    @Test
    fun inkIsWhiteOnTheDarkStyles() {
        assertEquals(Color.WHITE, Button(Rectangle(0f, 0f, 100f, 50f), "x").ink)
    }
}
