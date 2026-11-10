package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Fire dice at 1 pip are drawn as a kitten. This keeps, per board cell, which way it faces and where it is in
 * its idle or attack animation, plus the fireballs in flight and their bursts. Pure state: [MatchRenderer] draws it.
 */
class FireCats {
    class Frame(val row: Int, val index: Int, val attacking: Boolean)
    class Ball(val from: Vec2, val to: Vec2) {
        var t = 0f
        val done: Boolean get() = t >= FLIGHT
        val position: Vec2 get() {
            val f = (t / FLIGHT).coerceIn(0f, 1f)
            return Vec2(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
        }
        /** Degrees, counter-clockwise from +x. */
        val angle: Float get() = (atan2(to.y - from.y, to.x - from.x) * 180.0 / PI).toFloat()
    }
    class Boom(val at: Vec2) {
        var t = 0f
        val index: Int get() = (t * BOOM_FPS).toInt().coerceAtMost(BOOM_FRAMES - 1)
        val done: Boolean get() = t * BOOM_FPS >= BOOM_FRAMES
    }

    private class State(val phase: Float) {
        var row = ROWS.indexOf("S")
        /** Seconds into the attack, or negative when idle. */
        var attack = -1f
    }

    private val states = HashMap<Cell, State>()
    val balls = mutableListOf<Ball>()
    val booms = mutableListOf<Boom>()
    private var clock = 0f

    fun update(delta: Float) {
        clock += delta
        for (s in states.values) if (s.attack >= 0f) {
            s.attack += delta
            if (s.attack * ATTACK_FPS >= ATTACK_FRAMES - ATTACK_START) s.attack = -1f
        }
        for (b in balls) b.t += delta
        balls.removeAll { b -> b.done.also { if (it) booms += Boom(b.to) } }
        for (b in booms) b.t += delta
        booms.removeAll { it.done }
    }

    /** The cat in [cell] fired from [from] at [to]: it turns that way, throws, and a fireball flies there. */
    fun shot(cell: Cell, from: Vec2, to: Vec2) {
        val s = state(cell)
        s.row = rowFor(to.x - from.x, to.y - from.y)
        s.attack = 0f
        balls += Ball(from, to)
    }

    fun frame(cell: Cell): Frame {
        val s = state(cell)
        if (s.attack >= 0f) {
            val i = (ATTACK_START + (s.attack * ATTACK_FPS).toInt()).coerceAtMost(ATTACK_FRAMES - 1)
            return Frame(s.row, i, true)
        }
        return Frame(s.row, ((clock + s.phase) * IDLE_FPS).toInt() % IDLE_FRAMES, false)
    }

    /** Drops the state of cells that no longer hold a cat (merged, sold, moved). */
    fun retain(cells: Set<Cell>) {
        states.keys.retainAll(cells)
    }

    private fun state(cell: Cell) = states.getOrPut(cell) {
        // Each cat breathes on its own beat so a row of them doesn't move in lockstep.
        State(((cell.row * 7 + cell.col * 3) % 10) / 10f)
    }

    companion object {
        /** Row order of the sprite sheets. */
        val ROWS = listOf("S", "SE", "E", "NE", "N", "NW", "W", "SW")
        const val IDLE_FRAMES = 6
        const val IDLE_FPS = 8f
        const val ATTACK_FRAMES = 8
        const val ATTACK_FPS = 16f
        /** The shot is instant in the game, so the attack starts at the throw instead of the wind-up. */
        const val ATTACK_START = 3
        /** Seconds a fireball takes to reach its target; short, since the damage already landed. */
        const val FLIGHT = 0.16f
        const val BOOM_FRAMES = 8
        const val BOOM_FPS = 18f

        private val SECTOR_ROWS = listOf("E", "NE", "N", "NW", "W", "SW", "S", "SE").map { ROWS.indexOf(it) }

        /** Sheet row for a direction in world space (y up). */
        fun rowFor(dx: Float, dy: Float): Int {
            val sector = ((atan2(dy, dx) / (PI / 4)).roundToInt() % 8 + 8) % 8
            return SECTOR_ROWS[sector]
        }
    }
}
