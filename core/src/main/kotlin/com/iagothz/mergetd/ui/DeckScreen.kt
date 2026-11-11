package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Synergy
import com.iagothz.mergetd.logic.Vec2

/**
 * The 25 dice four to a row (seven rows, all on screen). Tapping one opens its sheet (`DieWindow`), where it can also level up; the deck changes
 * by dragging (`DeckEditor`): a die from the grid onto a slot, a slot's die out of the slots, slot onto slot to swap.
 * Dice the account level has not opened yet sit grey with a padlock and the level that opens them; a die with
 * enough cards for its next level carries an arrow badge.
 */
class DeckScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.DECK

    private val header = ScreenHeader(S["Deck"])

    private val level = game.save.accountLevel
    private val selected = game.save.playableDeck().toMutableList()
    private val tiles = DieType.entries.mapIndexed { i, t ->
        t to Rectangle(GRID_X + (i % COLS) * STEP_X, GRID_TOP - TILE - (i / COLS) * STEP_Y, TILE, TILE)
    }
    private val enemies = Button(Rectangle(24f, 1190f, 190f, 56f), S["Inimigos"], style = ButtonStyle.SECONDARY)

    /** Three preset slots (tap loads, hold saves) and a random deck button. */
    private val presets = List(PRESETS) { Button(Rectangle(506f + it * 66f, 1190f, 60f, 56f), "${it + 1}", style = ButtonStyle.SECONDARY) }
    private val random = Button(Rectangle(440f, 1190f, 60f, 56f), "?", style = ButtonStyle.SECONDARY)
    private val hold = LongPress(holdSeconds = 0.6f)
    private var holdingPreset: Int? = null
    private var toast = ""
    private var toastLeft = 0f
    private val rng = kotlin.random.Random(System.nanoTime())
    private val tap = Tap<Button>()
    private val slots = List(DeckCodec.SIZE) { Rectangle(186f + it * 72f, 1112f, 60f, 60f) }
    private val editor = DeckEditor(selected, tiles, slots, ::isOpen)
    private val window = DieWindow(game, level) { t ->
        if (game.save.levelUp(t)) {
            game.cloud.push()
            say(S.f("%s subiu para o nível %d!", Skin.fullName(t), game.save.dieLevel(t)))
            game.sfx.play(setOf("record"))
        }
    }

    private fun isOpen(t: DieType) = DiceUnlocks.isUnlocked(t, level)

    override fun draw(delta: Float) {
        random.pressed = tap.current === random
        for ((i, b) in presets.withIndex()) {
            b.pressed = tap.current === b
            b.color = if (game.save.deckSlot(i) != null) PRESET_FULL else Skin.PANEL
        }
        toastLeft = maxOf(0f, toastLeft - delta)
        holdingPreset?.let { i ->
            if (hold.isHeld(game.clock) && selected.size == DeckCodec.SIZE) {
                game.save.saveDeckSlot(i, selected.toList())
                game.cloud.push()
                say(S.f("Deck salvo no slot %d", i + 1))
                holdingPreset = null
                hold.up()
                tap.cancel()
            }
        }
        enemies.pressed = tap.current === enemies
        val shapes = game.shapes
        val k = TILE / 84f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((t, r) in tiles) {
            val cx = r.x + r.width / 2
            val cy = r.y + r.height / 2
            if (t == window.shown) {
                shapes.color = Color.WHITE
                shapes.roundedRect(r.x - 6, r.y - 6, r.width + 12, r.height + 12, 22f)
            }
            if (!isOpen(t)) {
                shapes.bevelRect(r.x, r.y, r.width, r.height, 18f, Skin.LOCKED)
                shapes.glyph(t, cx, cy + 2f, 25f * k, Skin.LOCKED_INK, Skin.LOCKED)
                shapes.color = Skin.LOCK_COLOR
                shapes.ring(cx, cy + 4f * k, 9f * k, 4f * k, 20)
                shapes.roundedRect(cx - 13f * k, cy - 16f * k, 26f * k, 20f * k, 5f * k)
                shapes.color = Skin.LOCKED
                shapes.circle(cx, cy - 7f * k, 3f * k, 12)
                continue
            }
            shapes.bevelRect(r.x, r.y, r.width, r.height, 18f, Skin.color(t))
            shapes.glyph(t, cx, cy + 2f, 25f * k, Skin.glyphColor(t, 0.85f), Skin.color(t))
            if (t in selected) {
                shapes.color = Skin.GOLD
                shapes.circle(r.x + r.width - 2f, r.y + r.height - 2f, 15f, 20)
                shapes.color = BADGE_INK
                shapes.rectLine(r.x + r.width - 9f, r.y + r.height - 2f, r.x + r.width - 4f, r.y + r.height - 8f, 3.5f)
                shapes.rectLine(r.x + r.width - 4f, r.y + r.height - 8f, r.x + r.width + 5f, r.y + r.height + 5f, 3.5f)
            }
            val dieLevel = game.save.dieLevel(t)
            if (dieLevel > 1) {
                // Level pips along the bottom edge, one per level above the first.
                shapes.color = Skin.GOLD
                for (i in 1 until dieLevel) shapes.circle(r.x + 12f + (i - 1) * 12f, r.y + 9f, 4f, 10)
            }
            if (game.save.canLevelUp(t)) {
                // Enough cards for the next level: an arrow badge on the bottom-right corner.
                val bx = r.x + r.width - 2f
                val by = r.y + 4f
                shapes.color = LEVEL_BADGE
                shapes.circle(bx, by, 15f, 20)
                shapes.color = Color.WHITE
                shapes.triangle(bx - 8f, by - 1f, bx + 8f, by - 1f, bx, by + 9f)
                shapes.rect(bx - 3f, by - 9f, 6f, 9f)
            }
        }
        val target = editor.pointer?.let { editor.slotAt(it) }
        for ((i, r) in slots.withIndex()) {
            val t = selected.getOrNull(i)?.takeIf { i != editor.fromSlot }
            if (i == target) {
                shapes.color = Skin.GOLD
                shapes.roundedRect(r.x - 5f, r.y - 5f, r.width + 10f, r.height + 10f, 14f)
            }
            if (t == null) {
                shapes.color = Skin.CELL
                shapes.roundedRect(r.x, r.y, r.width, r.height, 12f)
            } else {
                shapes.die(t, 1, r.x + r.width / 2, r.y + r.height / 2, r.width)
            }
        }
        enemies.drawShape(shapes)
        random.drawShape(shapes)
        presets.forEach { it.drawShape(shapes) }
        editor.dragging?.let { t -> editor.pointer?.let { p -> shapes.die(t, 1, p.x, p.y + DRAG_LIFT, 72f) } }
        shapes.end()

        game.batch.begin()
        header.drawText(game.batch, game)
        if (selected.size < DeckCodec.SIZE) {
            game.smallFont.color = Skin.MUTED
            game.batch.textLeft(game.smallFont, S.f("arraste %d", DeckCodec.SIZE - selected.size), 556f, 1142f)
            game.smallFont.color = Color.WHITE
        }
        for ((t, r) in tiles) {
            if (isOpen(t)) {
                game.batch.textCentered(game.smallFont, Skin.label(t), r.x + r.width / 2, r.y - 18f)
            } else {
                game.smallFont.color = Skin.MUTED
                game.batch.textCentered(game.smallFont, S.f("Nv. %d", DiceUnlocks.unlockLevel(t)), r.x + r.width / 2, r.y - 18f)
                game.smallFont.color = Color.WHITE
            }
        }
        val pairs = Synergy.forDeck(selected)
        game.tinyFont.color = Skin.GOLD
        val line = if (pairs.isEmpty()) S["Sinergias do deck: nenhuma ainda (veja os pares na descrição de cada dado)"] else S.f("Sinergias do deck: %s", pairs.joinToString(", ") { S[it.title] })
        game.batch.textCentered(game.tinyFont, line, 360f, SYNERGY_Y)
        game.tinyFont.color = Skin.MUTED
        game.batch.textCentered(game.tinyFont, S["Toque num dado para ver tudo · arraste para montar o deck"], 360f, SYNERGY_Y - 26f)
        game.tinyFont.color = Color.WHITE
        enemies.drawText(game.batch, game.smallFont)
        random.drawText(game.batch, game.smallFont)
        presets.forEach { it.drawText(game.batch, game.smallFont) }
        if (toastLeft > 0f && !window.isOpen) game.batch.textShadowed(game.tinyFont, toast, 360f, TOAST_Y, Skin.GOLD)
        game.batch.end()

        window.draw(viewport.worldWidth, viewport.worldHeight)
        if (toastLeft > 0f && window.isOpen) {
            // Over the window: a level-up says so while the sheet stays open.
            game.batch.begin()
            game.batch.textShadowed(game.smallFont, toast, 360f, TOAST_Y, Skin.GOLD)
            game.batch.end()
        }
    }

    private fun buttonAt(p: Vec2) = (listOf(enemies, random) + presets).firstOrNull { it.contains(p.x, p.y) }

    private fun say(text: String) {
        toast = text
        toastLeft = 2f
    }

    override fun onTouchDown(p: Vec2) {
        if (window.isOpen) return window.touchDown(p)
        tap.press(buttonAt(p))
        holdingPreset = presets.indexOfFirst { it.contains(p.x, p.y) }.takeIf { it >= 0 }
        if (holdingPreset != null) hold.down(p, game.clock)
        editor.down(p)
    }

    override fun onDrag(p: Vec2) {
        if (window.isOpen) return
        hold.move(p)
        val lifted = editor.dragging == null
        editor.move(p)
        if (lifted && editor.dragging != null) game.sfx.play(setOf("pick"))
    }

    override fun onTouchCancelled() {
        window.touchCancelled()
        tap.cancel()
        holdingPreset = null
        hold.up()
        editor.cancel()
    }

    override fun onTouchUp(p: Vec2) {
        if (window.isOpen) return window.touchUp(p)
        val wasDragging = editor.dragging != null
        val release = editor.up(p)
        val die = release.shown
        when {
            die != null -> when {
                release.changed -> game.sfx.play(setOf("drop"))
                wasDragging -> Unit
                // A plain tap opens the die's sheet; the deck only changes by dragging.
                else -> window.open(die)
            }
            else -> when (val b = tap.release(buttonAt(p))) {
                enemies -> game.showEnemies()
                random -> {
                    selected.clear()
                    selected.addAll(DeckCodec.random(rng, DiceUnlocks.unlocked(level)))
                }
                else -> presets.indexOf(b).takeIf { it >= 0 }?.let { i ->
                    val deck = game.save.deckSlot(i)
                    if (deck == null) {
                        say(S["Segure para salvar o deck aqui"])
                    } else {
                        selected.clear()
                        selected.addAll(DiceUnlocks.sanitize(deck, level))
                    }
                }
            }
        }
        holdingPreset = null
        hold.up()
        // A full deck is the deck: no confirm step.
        if (selected.size == DeckCodec.SIZE && selected != game.save.deck) {
            game.save.deck = selected.toList()
            game.cloud.push()
        }
    }

    private companion object {
        const val PRESETS = 3
        val PRESET_FULL = Color(0.30f, 0.38f, 0.60f, 1f)
        val LEVEL_BADGE = Color(0.25f, 0.70f, 0.35f, 1f)
        val BADGE_INK = Skin.INK_DARK

        /** Four dice to a row, spread across the width; seven rows fit between the deck slots and the bottom bar. */
        const val COLS = 4
        const val TILE = 92f
        const val STEP_X = 168f
        const val STEP_Y = 132f
        const val GRID_X = 62f
        const val GRID_TOP = 1080f
        const val SYNERGY_Y = 150f

        /** The dragged die floats this much above the finger so it isn't hidden under it. */
        const val DRAG_LIFT = 40f

        /** The toast sits in the gap between the title and the deck slots. */
        const val TOAST_Y = 1188f
    }

    override fun backState() = BackState(ScreenKind.TAB, overlayOpen = window.isOpen)

    override fun perform(a: BackAction) {
        if (a == BackAction.CLOSE_OVERLAY) window.close() else super.perform(a)
    }
}
