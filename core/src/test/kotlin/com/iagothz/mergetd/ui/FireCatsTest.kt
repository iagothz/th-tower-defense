package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FireCatsTest {
    private val cats = FireCats()
    private val cell = Cell(1, 1)

    @Test
    fun facesTheEightDirectionsOfTheShot() {
        // y points up in the world; sheet rows run S, SE, E, NE, N, NW, W, SW.
        val cases = listOf(
            Vec2(0f, -1f) to "S",
            Vec2(1f, -1f) to "SE",
            Vec2(1f, 0f) to "E",
            Vec2(1f, 1f) to "NE",
            Vec2(0f, 1f) to "N",
            Vec2(-1f, 1f) to "NW",
            Vec2(-1f, 0f) to "W",
            Vec2(-1f, -1f) to "SW",
        )
        for ((d, name) in cases) assertEquals(FireCats.ROWS.indexOf(name), FireCats.rowFor(d.x, d.y), name)
    }

    @Test
    fun startsFacingDownAndIdle() {
        val f = cats.frame(cell)
        assertEquals(FireCats.ROWS.indexOf("S"), f.row)
        assertFalse(f.attacking)
    }

    @Test
    fun shotTurnsTheCatAndPlaysTheAttackFromTheThrow() {
        cats.shot(cell, Vec2(0f, 0f), Vec2(100f, 0f))
        val f = cats.frame(cell)
        assertEquals(FireCats.ROWS.indexOf("E"), f.row)
        assertTrue(f.attacking)
        assertEquals(FireCats.ATTACK_START, f.index)
    }

    @Test
    fun attackRunsToTheLastFrameThenGoesBackToIdle() {
        cats.shot(cell, Vec2(0f, 0f), Vec2(0f, 100f))
        var last = -1
        while (cats.frame(cell).attacking) {
            val i = cats.frame(cell).index
            assertTrue(i >= last, "frames never go backwards")
            last = i
            cats.update(0.01f)
        }
        assertEquals(FireCats.ATTACK_FRAMES - 1, last)
        assertEquals(FireCats.ROWS.indexOf("N"), cats.frame(cell).row, "keeps facing its last target")
    }

    @Test
    fun idleLoopsThroughItsFrames() {
        val seen = mutableSetOf<Int>()
        repeat(200) {
            seen += cats.frame(cell).index
            cats.update(0.02f)
        }
        assertEquals((0 until FireCats.IDLE_FRAMES).toSet(), seen)
    }

    @Test
    fun fireballFliesToTheTargetThenBurstsThere() {
        cats.shot(cell, Vec2(0f, 0f), Vec2(200f, 0f))
        val ball = cats.balls.single()
        assertTrue(ball.position.x < 100f, "starts near the cat")
        while (cats.balls.isNotEmpty()) cats.update(0.016f)
        val boom = cats.booms.single()
        assertEquals(200f, boom.at.x)
        while (cats.booms.isNotEmpty()) cats.update(0.016f)
    }

    @Test
    fun forgetsCellsThatAreNoLongerCats() {
        cats.shot(cell, Vec2(0f, 0f), Vec2(-100f, 0f))
        cats.retain(emptySet())
        assertEquals(FireCats.ROWS.indexOf("S"), cats.frame(cell).row)
    }
}
