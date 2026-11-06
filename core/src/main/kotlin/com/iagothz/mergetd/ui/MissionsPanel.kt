package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DailyChallenge
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind

/**
 * The day's missions as a panel of cards: on the home tab and in the match's pause menu. Each card has an icon for
 * its kind on the left, the sentence on top, a thick bar with the count inside, and the XP reward as a pill on the
 * right; a finished one gets a gold rim, a check badge on the icon and a gold "Concluída" pill.
 */
class MissionsPanel(private val game: MergeTdGame, val rect: Rectangle, private val day: Int, private val missions: List<Mission>) {
    private val cards = missions.mapIndexed { i, _ ->
        Rectangle(rect.x + 16f, rect.y + rect.height - 74f - (i + 1) * 112f, rect.width - 32f, 102f)
    }
    private val tmp = Color()

    fun drawShapes(progress: List<Int>) {
        val shapes = game.shapes
        shapes.color = Skin.SHADOW
        shapes.roundedRect(rect.x + 3f, rect.y - 8f, rect.width, rect.height, 20f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(rect.x, rect.y, rect.width, rect.height, 20f)
        for ((i, m) in missions.withIndex()) {
            val c = cards[i]
            val done = progress[i] >= m.target
            if (done) {
                shapes.color = Skin.GOLD
                shapes.roundedRect(c.x - 3f, c.y - 3f, c.width + 6f, c.height + 6f, 19f)
            }
            shapes.color = if (done) CARD_DONE else Skin.CELL
            shapes.roundedRect(c.x, c.y, c.width, c.height, 16f)
            icon(shapes, m, c.x + ICON_X + ICON / 2, c.y + c.height / 2, done)
            // Progress bar with room for the count inside it.
            val bx = c.x + TEXT_X
            val bw = c.width - TEXT_X - PILL_W - 28f
            shapes.color = BAR_BACK
            shapes.roundedRect(bx, c.y + BAR_Y, bw, BAR_H, BAR_H / 2)
            val f = (progress[i].toFloat() / m.target).coerceIn(0f, 1f)
            if (f > 0f) {
                shapes.color = if (done) Skin.GOLD else accent(m)
                shapes.roundedRect(bx, c.y + BAR_Y, maxOf(BAR_H, bw * f), BAR_H, BAR_H / 2)
            }
            // Reward pill.
            val px = c.x + c.width - PILL_W - 14f
            shapes.color = if (done) Skin.GOLD else PILL
            shapes.roundedRect(px, c.y + BAR_Y - 3f, PILL_W, BAR_H + 6f, (BAR_H + 6f) / 2)
        }
    }

    fun drawText(progress: List<Int>) {
        val batch = game.batch
        game.font.color = Skin.GOLD
        batch.textLeft(game.font, S["Missões de hoje"], rect.x + 24f, rect.y + rect.height - 32f)
        game.font.color = Color.WHITE
        game.tinyFont.color = Skin.MUTED
        batch.textRight(game.tinyFont, "${DailyChallenge.of(day).dateLabel} · ${S["Renova amanhã"]}", rect.x + rect.width - 24f, rect.y + rect.height - 32f)
        for ((i, m) in missions.withIndex()) {
            val c = cards[i]
            val done = progress[i] >= m.target
            game.smallFont.color = Color.WHITE
            batch.textLeft(game.smallFont, m.text(), c.x + TEXT_X, c.y + c.height - 30f)
            val bw = c.width - TEXT_X - PILL_W - 28f
            game.tinyFont.color = if (done) Skin.inkOn(Skin.GOLD) else Color.WHITE
            batch.textCentered(game.tinyFont, "${minOf(progress[i], m.target)}/${m.target}", c.x + TEXT_X + bw / 2, c.y + BAR_Y + BAR_H / 2)
            val px = c.x + c.width - PILL_W - 14f
            game.tinyFont.color = if (done) Skin.inkOn(Skin.GOLD) else Skin.GOLD
            batch.textCentered(game.tinyFont, if (done) S["Concluída"] else S.f("+%d XP", m.xp), px + PILL_W / 2, c.y + BAR_Y + BAR_H / 2)
        }
        game.tinyFont.color = Color.WHITE
        game.smallFont.color = Color.WHITE
    }

    /** The colour that stands for a mission: the die's, the map's, or a fixed one per kind. */
    private fun accent(m: Mission): Color = when (m.kind) {
        MissionKind.KILLS_WITH -> Skin.color(m.type!!)
        MissionKind.REACH_WAVE -> tmp.set(Skin.theme(m.map!!).ground).lerp(Color.WHITE, 0.2f)
        MissionKind.MERGES -> Skin.color(DieType.ICE)
        MissionKind.MAX_PIPS -> Skin.GOLD
        MissionKind.BOSSES -> Skin.HEART
        MissionKind.PLAY_MODE -> Skin.color(DieType.ELECTRIC)
    }

    /** A 64-px icon centred on (cx, cy). */
    private fun icon(shapes: ShapeRenderer, m: Mission, cx: Float, cy: Float, done: Boolean) {
        val h = ICON / 2
        when (m.kind) {
            MissionKind.KILLS_WITH -> shapes.die(m.type!!, 3, cx, cy, ICON - 6f)
            MissionKind.MAX_PIPS -> shapes.die(DieType.ELECTRIC, 7, cx, cy, ICON - 12f)
            MissionKind.MERGES -> {
                shapes.die(DieType.ICE, 2, cx - 9f, cy - 8f, 40f)
                shapes.die(DieType.ICE, 2, cx + 9f, cy + 9f, 40f)
            }
            MissionKind.BOSSES -> {
                shapes.bevelRect(cx - h, cy - h, ICON, ICON, 14f, TILE_BOSS)
                // Crown: band and three points.
                shapes.color = Skin.GOLD
                shapes.rect(cx - 20f, cy - 14f, 40f, 10f)
                shapes.triangle(cx - 20f, cy - 4f, cx - 20f, cy + 18f, cx - 6f, cy - 4f)
                shapes.triangle(cx - 10f, cy - 4f, cx, cy + 22f, cx + 10f, cy - 4f)
                shapes.triangle(cx + 6f, cy - 4f, cx + 20f, cy + 18f, cx + 20f, cy - 4f)
            }
            MissionKind.REACH_WAVE -> {
                shapes.bevelRect(cx - h, cy - h, ICON, ICON, 14f, accent(m).cpy())
                flag(shapes, cx, cy, Color.WHITE)
            }
            MissionKind.PLAY_MODE -> {
                shapes.bevelRect(cx - h, cy - h, ICON, ICON, 14f, TILE_MODE)
                shapes.color = Color.WHITE
                shapes.triangle(cx - 11f, cy - 16f, cx - 11f, cy + 18f, cx + 17f, cy + 1f)
            }
        }
        if (done) {
            // Check badge on the icon's top-right corner.
            val bx = cx + h - 4f
            val by = cy + h - 4f
            shapes.color = Skin.SHADOW
            shapes.circle(bx + 1f, by - 2f, 15f, 20)
            shapes.color = Skin.GOLD
            shapes.circle(bx, by, 14f, 20)
            shapes.color = Skin.inkOn(Skin.GOLD)
            shapes.rectLine(bx - 7f, by, bx - 2f, by - 6f, 4f)
            shapes.rectLine(bx - 2f, by - 6f, bx + 8f, by + 6f, 4f)
        }
    }

    private fun flag(shapes: ShapeRenderer, cx: Float, cy: Float, ink: Color) {
        shapes.color = ink
        shapes.rectLine(cx - 12f, cy - 18f, cx - 12f, cy + 18f, 4f)
        shapes.triangle(cx - 11f, cy + 18f, cx - 11f, cy + 2f, cx + 16f, cy + 10f)
    }

    companion object {
        /** Height that fits the header and three cards. */
        const val HEIGHT = 440f
        private const val ICON = 64f
        private const val ICON_X = 16f
        private const val TEXT_X = 98f
        private const val BAR_Y = 20f
        private const val BAR_H = 24f
        private const val PILL_W = 118f
        private val CARD_DONE = Color(0.22f, 0.21f, 0.18f, 1f)
        private val BAR_BACK = Color(0.11f, 0.12f, 0.16f, 1f)
        private val PILL = Color(0.11f, 0.12f, 0.16f, 1f)
        private val TILE_BOSS = Color(0.45f, 0.14f, 0.20f, 1f)
        private val TILE_MODE = Color(0.22f, 0.38f, 0.72f, 1f)
    }
}
