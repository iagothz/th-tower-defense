package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.Match
import com.iagothz.mergetd.logic.Vec2

/**
 * Free play for trying out dice and their looks: tap an empty cell to put down the chosen die at the chosen pips,
 * drag to merge or move as in a match, erase with the eraser, and send waves only when asked. Nothing is saved or booked.
 */
class SandboxScreen(game: MergeTdGame) : BaseScreen(game) {
    private val match = Match(game.save.playableDeck(), map = game.save.map, sandbox = true)
    private val drag = DragController(match)
    private val renderer = MatchRenderer(game.shapes, game.batch, game.sprites, game.font, game.smallFont, game.tinyFont)
    private val effects = Effects(path = match.path)
    private val sounds = SoundPlanner()
    private val buttonTap = Tap<Button>()
    private var clock = 0f
    private var speed = 1
    private var paused = false
    private var picking = false
    private var erasing = false

    /** The die a tap on an empty cell puts down. */
    var brush = DieType.FIRE
        private set
    var brushPips = 1
        private set

    private val speedButton = Button(Rectangle(528f, 1182f, 76f, 56f), "1x", style = ButtonStyle.SECONDARY)
    private val pauseButton = Button(Rectangle(616f, 1182f, 84f, 56f), "", style = ButtonStyle.SECONDARY)

    private val brushButton = Button(Rectangle(24f, ROW1, 220f, ROW_H), "", style = ButtonStyle.SECONDARY)
    private val minus = Button(Rectangle(256f, ROW1, 70f, ROW_H), "-", style = ButtonStyle.SECONDARY)
    private val plus = Button(Rectangle(446f, ROW1, 70f, ROW_H), "+", style = ButtonStyle.SECONDARY)
    private val eraser = Button(Rectangle(528f, ROW1, 168f, ROW_H), S["Apagar"], style = ButtonStyle.SECONDARY)

    private val waveButton = Button(Rectangle(24f, ROW2, 160f, ROW_H), S["Onda"])
    private val bossButton = Button(Rectangle(196f, ROW2, 160f, ROW_H), S["Chefe"], style = ButtonStyle.SECONDARY)
    private val stopButton = Button(Rectangle(368f, ROW2, 160f, ROW_H), S["Parar"], style = ButtonStyle.SECONDARY)
    private val clearButton = Button(Rectangle(540f, ROW2, 156f, ROW_H), S["Limpar"], style = ButtonStyle.DANGER)

    private val resume = Button(Rectangle(160f, 720f, 400f, 100f), S["Continuar"])
    private val quit = Button(Rectangle(160f, 590f, 400f, 100f), S["Sair"], style = ButtonStyle.DANGER)

    private val hudButtons = listOf(speedButton, pauseButton, brushButton, minus, plus, eraser, waveButton, bossButton, stopButton, clearButton)
    private val allButtons = hudButtons + listOf(resume, quit)

    /** The picker's cells: every die type in a 5-wide grid, top to bottom. */
    private val pickCells = DieType.entries.mapIndexed { i, t ->
        val col = i % PICK_COLS
        val row = i / PICK_COLS
        t to Rectangle(PICK.x + 16f + col * PICK_CELL, PICK.y + PICK.height - 90f - (row + 1) * PICK_CELL, PICK_CELL, PICK_CELL)
    }

    override val skyMap get() = match.map

    override fun pause() {
        paused = true
        onTouchCancelled()
    }

    override fun draw(delta: Float) {
        if (!paused && !picking) {
            clock += delta
            match.update(delta * speed * effects.timeScale())
            renderer.update(match, delta)
            for (s in match.lastShots) {
                val cell = Layout.cellAt(s.from) ?: continue
                val target = s.to.firstOrNull() ?: continue
                effects.kick(cell, Vec2(target.x - s.from.x, target.y - s.from.y))
            }
            val events = match.drainEvents()
            effects.consume(events)
            effects.update(delta)
            game.sfx.play(sounds.plan(events, clock))
        }
        speedButton.text = "${speed}x"
        minus.enabled = brushPips > 1
        plus.enabled = brushPips < Balance.MAX_PIPS
        eraser.style = if (erasing) ButtonStyle.SELECTED else ButtonStyle.SECONDARY
        stopButton.enabled = match.waveRunning
        allButtons.forEach { it.pressed = buttonTap.current === it }

        val shapes = game.shapes
        val batch = game.batch
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        renderer.drawWorldShapes(match, drag, effects)
        shapes.end()
        batch.begin()
        renderer.drawSprites(match, drag, effects)
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        renderer.drawOverlayShapes(match, drag, effects)
        for (b in hudButtons) b.drawShape(shapes)
        shapes.color = Color.WHITE
        val pc = pauseButton.rect
        shapes.rect(pc.x + pc.width / 2 - 12f, pc.y + 18f, 8f, 24f)
        shapes.rect(pc.x + pc.width / 2 + 4f, pc.y + 18f, 8f, 24f)
        val br = brushButton.rect
        shapes.die(brush, brushPips, br.x + 42f, br.y + br.height / 2, 56f)
        shapes.end()

        batch.begin()
        renderer.drawHud(match, effects, match.sp)
        effects.drawText(batch, game.smallFont, game.bigFont, game.font)
        game.tinyFont.color = Skin.GOLD
        batch.textCentered(game.tinyFont, S["Sandbox"], 360f, 1166f)
        game.tinyFont.color = Color.WHITE
        if (!match.waveRunning) {
            batch.textCentered(game.smallFont, S.f("Toque numa casa vazia para pôr %s", Skin.fullName(brush)), 360f, 1106f)
        }
        speedButton.drawText(batch, game.font)
        for (b in listOf(minus, plus, eraser, waveButton, bossButton, stopButton, clearButton)) b.drawText(batch, game.font)
        batch.textLeft(game.smallFont, Skin.fullName(brush), br.x + 80f, br.y + br.height / 2)
        batch.textCentered(game.font, S.f("%d %s", brushPips, S[if (brushPips == 1) "pip" else "pips"]), 386f, ROW1 + ROW_H / 2)
        batch.end()

        if (picking) drawPicker()
        if (paused) drawPause()
    }

    private fun drawPicker() {
        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Skin.DIM
        shapes.rect(-400f, -400f, Layout.WORLD_W + 800f, Layout.WORLD_H + 800f)
        shapes.color = Skin.SHADOW
        shapes.roundedRect(PICK.x + 3f, PICK.y - 8f, PICK.width, PICK.height, 22f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(PICK.x, PICK.y, PICK.width, PICK.height, 22f)
        for ((t, r) in pickCells) {
            if (t == brush) {
                shapes.color = Skin.GOLD
                shapes.ring(r.x + 4f, r.y + 4f, r.width - 8f, r.height - 8f, 4f)
            }
            shapes.die(t, brushPips, r.x + r.width / 2, r.y + r.height / 2 + 12f, 70f)
        }
        shapes.end()
        game.batch.begin()
        game.batch.textShadowed(game.font, S["Escolha o dado"], 360f, PICK.y + PICK.height - 44f)
        for ((t, r) in pickCells) game.batch.textCentered(game.tinyFont, Skin.label(t), r.x + r.width / 2, r.y + 16f)
        game.batch.end()
    }

    private fun drawPause() {
        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Skin.DIM
        shapes.rect(-400f, -400f, Layout.WORLD_W + 800f, Layout.WORLD_H + 800f)
        resume.drawShape(shapes)
        quit.drawShape(shapes)
        shapes.end()
        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Pausa"], 360f, 920f)
        resume.drawText(game.batch, game.font)
        quit.drawText(game.batch, game.font)
        game.batch.end()
    }

    private fun buttonAt(p: Vec2): Button? {
        val visible = when {
            paused -> listOf(resume, quit)
            picking -> emptyList()
            else -> hudButtons
        }
        return visible.firstOrNull { it.contains(p.x, p.y) }
    }

    override fun onTouchDown(p: Vec2) {
        if (picking) {
            pickCells.firstOrNull { it.second.contains(p.x, p.y) }?.let { brush = it.first }
            // Any tap closes the picker; one on a die also chooses it.
            picking = false
            return
        }
        if (!paused) {
            val cell = Layout.cellAt(p)
            if (cell != null) {
                when {
                    erasing -> if (match.remove(cell)) game.sfx.play(setOf("drop"))
                    match.board[cell] == null -> match.place(cell, brush, brushPips)
                    drag.down(p) -> game.sfx.play(setOf("pick"))
                }
                return
            }
        }
        buttonTap.press(buttonAt(p))
    }

    override fun onDrag(p: Vec2) = drag.move(p)

    override fun onTouchUp(p: Vec2) {
        val from = drag.dragging
        if (from != null) {
            val die = match.board[from]
            val to = drag.dropTarget(p)
            val result = if (to != null && match.canMerge(from, to)) match.board.resultCell(from, to) else null
            val merged = drag.up(p)
            when {
                merged && die != null && result != null && result != from -> effects.slide(from, result, die.type, die.pips)
                // In the sandbox a die dropped on an empty cell moves there, and one dropped on another die swaps with it.
                !merged && to != null && to != from -> match.board.swap(from, to)
                else -> game.sfx.play(setOf("drop"))
            }
            return
        }
        when (buttonTap.release(buttonAt(p))) {
            null -> Unit
            speedButton -> speed = if (speed >= 4) 1 else speed * 2
            pauseButton -> paused = true
            brushButton -> {
                picking = true
                erasing = false
            }
            minus -> brushPips = (brushPips - 1).coerceAtLeast(1)
            plus -> brushPips = (brushPips + 1).coerceAtMost(Balance.MAX_PIPS)
            eraser -> erasing = !erasing
            waveButton -> match.sendWave(match.wave + 1)
            bossButton -> match.sendWave((match.wave / Balance.BOSS_EVERY + 1) * Balance.BOSS_EVERY)
            stopButton -> match.stopWave()
            clearButton -> match.clearBoard()
            resume -> paused = false
            quit -> game.showChallenges()
        }
    }

    override fun onTouchCancelled() {
        drag.cancel()
        buttonTap.cancel()
    }

    override fun backState() = BackState(ScreenKind.MATCH, overlayOpen = picking, paused = paused)

    override fun perform(a: BackAction) {
        when (a) {
            BackAction.CLOSE_OVERLAY -> picking = false
            BackAction.OPEN_PAUSE -> paused = true
            BackAction.RESUME -> paused = false
            else -> Unit
        }
    }

    private companion object {
        const val ROW_H = 72f
        const val ROW1 = 104f
        const val ROW2 = 20f
        const val PICK_COLS = 5
        const val PICK_CELL = 128f
        val PICK = Rectangle(24f, 280f, 672f, 770f)
    }
}
