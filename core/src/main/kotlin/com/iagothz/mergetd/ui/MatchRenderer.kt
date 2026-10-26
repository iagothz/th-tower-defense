package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.iagothz.mergetd.logic.Die
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.ShotEvent
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.ceil

/** Draws the path, board, enemies and shot traces of a [Match]. */
class MatchRenderer(private val shapes: ShapeRenderer, private val batch: SpriteBatch, private val font: BitmapFont) {
    private class Trace(val shot: ShotEvent, var ttl: Float)

    private val traces = mutableListOf<Trace>()
    private val tmp = Color()

    /** Collects the shots of the last update and ages old traces. */
    fun update(match: Match, delta: Float) {
        traces.forEach { it.ttl -= delta }
        traces.removeAll { it.ttl <= 0f }
        match.lastShots.forEach { traces += Trace(it, SHOT_TTL) }
    }

    /** Call inside a Filled ShapeRenderer batch. */
    fun drawShapes(match: Match, drag: DragController) {
        drawPath()
        drawBoard(match, drag)
        drawEnemies(match)
        drawTraces()
        val from = drag.dragging
        val at = drag.pointer
        if (from != null && at != null) match.board[from]?.let { drawDie(it, at, 1f) }
    }

    /** Call inside a SpriteBatch batch. */
    fun drawHud(match: Match) {
        batch.textCentered(font, "Onda ${match.wave}", 130f, 1210f)
        batch.textCentered(font, "Vidas ${match.lives}", 330f, 1210f)
        batch.textCentered(font, "SP ${match.sp}", 520f, 1210f)
        if (match.waveCountdown > 0f) {
            batch.textCentered(font, "Próxima onda em ${ceil(match.waveCountdown).toInt()}", 360f, 1110f)
        }
    }

    private fun drawPath() {
        shapes.color = Skin.PATH
        val pts = Layout.PATH.points
        for ((a, b) in pts.zipWithNext()) shapes.rectLine(a.x, a.y, b.x, b.y, PATH_WIDTH)
        pts.forEach { shapes.rect(it.x - PATH_WIDTH / 2, it.y - PATH_WIDTH / 2, PATH_WIDTH, PATH_WIDTH) }
        shapes.color = Color(0.3f, 0.7f, 0.3f, 1f)
        shapes.circle(pts.first().x, pts.first().y, 14f, 20)
        shapes.color = Color(0.8f, 0.25f, 0.25f, 1f)
        shapes.circle(pts.last().x, pts.last().y, 14f, 20)
    }

    private fun drawBoard(match: Match, drag: DragController) {
        val highlights = drag.highlights().toSet()
        for (cell in match.board.cells()) {
            val c = Layout.cellCenter(cell)
            val half = Layout.CELL / 2
            if (cell in highlights) {
                shapes.color = Color.WHITE
                shapes.roundedRect(c.x - half - 4, c.y - half - 4, Layout.CELL + 8, Layout.CELL + 8, 20f)
            }
            shapes.color = Skin.PANEL
            shapes.roundedRect(c.x - half, c.y - half, Layout.CELL, Layout.CELL, 16f)
            val die = match.board[cell] ?: continue
            if (cell == drag.dragging) drawDie(die, c, 0.35f) else drawDie(die, c, if (die.disabled) 0.4f else 1f)
        }
    }

    private fun drawDie(die: Die, c: Vec2, brightness: Float) {
        val half = DIE / 2
        shapes.color = tmp.set(Skin.color(die.type)).mul(brightness, brightness, brightness, 1f)
        shapes.roundedRect(c.x - half, c.y - half, DIE, DIE, 18f)
        shapes.color = tmp.set(Skin.pipColor(die.type)).mul(brightness, brightness, brightness, 1f)
        for ((dx, dy) in PIPS[die.pips - 1]) shapes.circle(c.x + dx * PIP_STEP, c.y + dy * PIP_STEP, PIP_R, 12)
    }

    private fun drawEnemies(match: Match) {
        for (e in match.enemies) {
            val p = Layout.PATH.positionAt(e.distance)
            val r = Skin.enemyRadius(e.kind)
            if (e.poison.isNotEmpty()) {
                shapes.color = Color(0.3f, 0.85f, 0.3f, 1f)
                shapes.circle(p.x, p.y, r + 4f, 24)
            }
            shapes.color = tmp.set(Skin.color(e.kind))
            if (e.slow > 0f) shapes.color = tmp.lerp(0.5f, 0.8f, 1f, 1f, 0.5f)
            shapes.circle(p.x, p.y, r, 24)

            val w = r * 2
            val ratio = (e.hp / e.maxHp).coerceIn(0f, 1f)
            shapes.color = Color.DARK_GRAY
            shapes.rect(p.x - r, p.y + r + 6f, w, 6f)
            shapes.color = Color(0.9f, 0.2f, 0.2f, 1f)
            shapes.rect(p.x - r, p.y + r + 6f, w * ratio, 6f)
        }
    }

    private fun drawTraces() {
        for (t in traces) {
            shapes.color = Skin.color(t.shot.type)
            var from = t.shot.from
            for (to in t.shot.to) {
                shapes.rectLine(from.x, from.y, to.x, to.y, 4f)
                from = to
            }
        }
    }

    private companion object {
        const val SHOT_TTL = 0.1f
        const val PATH_WIDTH = 40f
        const val DIE = 104f
        const val PIP_STEP = 26f
        const val PIP_R = 9f

        private val C = 0 to 0
        private val TL = -1 to 1
        private val TR = 1 to 1
        private val BL = -1 to -1
        private val BR = 1 to -1
        private val ML = -1 to 0
        private val MR = 1 to 0

        /** Pip offsets (in PIP_STEP units) for 1..7 pips. */
        val PIPS = listOf(
            listOf(C),
            listOf(TL, BR),
            listOf(TL, C, BR),
            listOf(TL, TR, BL, BR),
            listOf(TL, TR, C, BL, BR),
            listOf(TL, TR, ML, MR, BL, BR),
            listOf(TL, TR, ML, C, MR, BL, BR),
        )
    }
}
