package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Mutator
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.MatchCode
import com.iagothz.mergetd.logic.Vec2

/** Picks the match: four map cards (miniature, difficulty, record) above a row of modes. */
class MapScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.MAPS

    private var selected = game.save.map
    private var mode = game.save.mode
    private val mutators = game.save.mutators.toMutableSet()
    private val mutatorChips = Mutator.entries.mapIndexed { i, m -> m to Rectangle(24f + i * 170f, 330f, 162f, 56f) }
    private val cards = GameMap.entries.mapIndexed { i, m ->
        m to Rectangle(24f + (i % 2) * 340f, 870f - (i / 2) * 300f, 332f, 280f)
    }
    private val modes = GameMode.entries.mapIndexed { i, m -> m to Rectangle(24f + i * 170f, 440f, 162f, 64f) }
    private val play = Button(Rectangle(40f, 124f, 400f, 76f), S["Jogar"])
    private val playCode = Button(Rectangle(456f, 124f, 240f, 76f), S["Jogar código"], color = Color(0.55f, 0.30f, 0.75f, 1f))
    private var codeError = 0f
    private val tap = Tap<Button>()

    /** The menu behind the cards already shows the chosen map's sky. */
    override val skyMap get() = selected

    override fun draw(delta: Float) {
        play.pressed = tap.current === play
        playCode.pressed = tap.current === playCode
        codeError = maxOf(0f, codeError - delta)
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((m, r) in cards) {
            if (m == selected) {
                shapes.color = Skin.GOLD
                shapes.roundedRect(r.x - 6f, r.y - 6f, r.width + 12f, r.height + 12f, 26f)
            }
            shapes.color = Skin.SHADOW
            shapes.roundedRect(r.x + 3f, r.y - 8f, r.width, r.height, 22f)
            shapes.color = Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 22f)
            drawMiniature(m, r.x + r.width / 2, r.y + 18f, PREVIEW_SCALE)
        }
        for ((m, r) in modes) {
            if (m == mode) {
                shapes.color = Skin.GOLD
                shapes.roundedRect(r.x - 4f, r.y - 4f, r.width + 8f, r.height + 8f, 18f)
            }
            shapes.bevelRect(r.x, r.y, r.width, r.height, 14f, if (m == mode) MODE_ON else Skin.PANEL)
        }
        for ((m, r) in mutatorChips) shapes.bevelRect(r.x, r.y, r.width, r.height, 14f, if (m in mutators) MUTATOR_ON else Skin.PANEL)
        play.drawShape(shapes)
        playCode.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Mapas"], 360f, 1222f)
        for ((m, r) in cards) {
            game.batch.textCentered(game.font, S[m.title], r.x + r.width / 2, r.y + r.height - 30f)
            game.smallFont.color = Skin.difficultyColor(m.difficulty)
            game.batch.textCentered(game.smallFont, S[m.difficulty.label], r.x + r.width / 2 - 70f, r.y + r.height - 62f)
            game.smallFont.color = KEY_COLOR
            game.batch.textCentered(game.smallFont, S.f("recorde %d", game.save.bestWave(m, mode)), r.x + r.width / 2 + 60f, r.y + r.height - 62f)
            game.smallFont.color = Color.WHITE
        }
        game.smallFont.color = KEY_COLOR
        game.batch.textLeft(game.smallFont, S["Modo"], 24f, 532f)
        game.smallFont.color = Color.WHITE
        for ((m, r) in modes) game.batch.textCentered(game.smallFont, S[m.title], r.x + r.width / 2, r.y + r.height / 2 + 2f)
        game.tinyFont.color = Skin.GOLD
        game.batch.textCentered(game.tinyFont, S[mode.description], 360f, 414f)
        game.smallFont.color = KEY_COLOR
        game.batch.textLeft(game.smallFont, S.f("Mutadores ×%s", DieInfo.num(Mutator.multiplier(mutators))), 24f, 400f)
        game.smallFont.color = Color.WHITE
        for ((m, r) in mutatorChips) game.batch.textCentered(game.tinyFont, S[m.title], r.x + r.width / 2, r.y + r.height / 2 + 2f)
        game.tinyFont.color = Skin.GOLD
        game.batch.textCentered(game.tinyFont, if (mutators.isEmpty()) S["Sem mutadores: recorde normal"] else mutators.joinToString(" · ") { S[it.description] }, 360f, 304f)
        game.tinyFont.color = Color.WHITE
        play.drawText(game.batch, game.font)
        playCode.drawText(game.batch, game.smallFont)
        if (codeError > 0f) {
            game.smallFont.color = Skin.HEART
            game.batch.textCentered(game.smallFont, S["Código inválido"], 360f, 222f)
            game.smallFont.color = Color.WHITE
        }
        game.batch.end()
    }

    /** The map at [s] scale with its bottom-centre at (cx, y): sky, board frame, ground, start and end. */
    private fun drawMiniature(m: GameMap, cx: Float, y: Float, s: Float) {
        val shapes = game.shapes
        val t = Skin.theme(m)
        val x0 = cx - Layout.WORLD_W * s / 2
        fun wx(x: Float) = x0 + x * s
        fun wy(wy: Float) = y + wy * s
        shapes.rect(x0, y, Layout.WORLD_W * s, Layout.WORLD_H * s, t.skyBottom, t.skyBottom, t.skyTop, t.skyTop)
        shapes.color = Skin.BOARD_RIM
        val bw = Balance.COLS * (Layout.CELL + Layout.GAP) - Layout.GAP
        val bh = Balance.ROWS * (Layout.CELL + Layout.GAP) - Layout.GAP
        shapes.roundedRect(wx(Layout.GRID_X - 18f), wy(Layout.GRID_Y - 18f), (bw + 36f) * s, (bh + 36f) * s, 5f)
        val pts = m.path.points
        for ((c, w) in listOf(t.edge to 48f * s, t.ground to 40f * s)) {
            shapes.color = c
            for ((a, b) in pts.zipWithNext()) shapes.rectLine(wx(a.x), wy(a.y), wx(b.x), wy(b.y), w)
            pts.forEach { shapes.rect(wx(it.x) - w / 2, wy(it.y) - w / 2, w, w) }
        }
        shapes.color = Color(0.3f, 0.75f, 0.35f, 1f)
        shapes.circle(wx(pts.first().x), wy(pts.first().y), 5f, 12)
        shapes.color = Skin.HEART
        shapes.circle(wx(pts.last().x), wy(pts.last().y), 5f, 12)
    }

    private fun buttonAt(p: Vec2) = listOf(play, playCode).firstOrNull { it.contains(p.x, p.y) }

    private fun askForCode() {
        Gdx.input.getTextInput(
            object : Input.TextInputListener {
                override fun input(text: String) {
                    val setup = MatchCode.decode(text)
                    Gdx.app.postRunnable { if (setup != null) game.startFromCode(setup) else codeError = 2.5f }
                }

                override fun canceled() = Unit
            },
            S["Código da partida"], "", "XXXXX-XXXXX-XXXXX",
        )
    }

    override fun onTouchDown(p: Vec2) = tap.press(buttonAt(p))

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        val card = cards.firstOrNull { it.second.contains(p.x, p.y) }?.first
        val chip = modes.firstOrNull { it.second.contains(p.x, p.y) }?.first
        val mutator = mutatorChips.firstOrNull { it.second.contains(p.x, p.y) }?.first
        if (card != null || chip != null || mutator != null) {
            if (card != null) {
                selected = card
                game.save.map = card
            }
            if (chip != null) {
                mode = chip
                game.save.mode = chip
            }
            if (mutator != null) {
                if (!mutators.remove(mutator)) mutators += mutator
                game.save.mutators = mutators
            }
            tap.cancel()
            return
        }
        when (tap.release(buttonAt(p))) {
            play -> game.startMatch()
            playCode -> askForCode()
        }
    }

    private companion object {
        val MUTATOR_ON = Color(0.70f, 0.30f, 0.30f, 1f)
        const val PREVIEW_SCALE = 0.15f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        val MODE_ON = Color(0.55f, 0.30f, 0.75f, 1f)
    }
}
