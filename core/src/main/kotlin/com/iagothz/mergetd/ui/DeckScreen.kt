package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieLevels
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Synergy
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.roundToInt

/**
 * Tapping a die toggles it in the deck and shows what it does in the panel below. Dice the account level hasn't
 * opened yet sit grey with a padlock and the level that opens them; an open die shows its level and the cards
 * towards the next one, with a button to take it.
 */
class DeckScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.DECK

    private val level = game.save.accountLevel
    private val selected = game.save.playableDeck().toMutableList()
    private var shown = selected.firstOrNull() ?: DieType.FIRE
    private val tiles = DieType.entries.mapIndexed { i, t ->
        t to Rectangle(24f + (i % COLS) * 98f, 1010f - (i / COLS) * 116f, 84f, 84f)
    }
    private val panel = Rectangle(24f, 130f, 672f, 470f)
    private val enemies = Button(Rectangle(24f, 1190f, 190f, 56f), S["Inimigos"], color = Skin.PANEL)
    /** Three preset slots (tap loads, hold saves) and a random deck button. */
    private val presets = List(PRESETS) { Button(Rectangle(506f + it * 66f, 1190f, 60f, 56f), "${it + 1}", color = Skin.PANEL) }
    private val random = Button(Rectangle(440f, 1190f, 60f, 56f), "?", color = Skin.PANEL)
    /** Takes the shown die to its next level; drawn only while it has one to take. */
    private val levelUp = Button(Rectangle(panel.x + panel.width - 196f, panel.y + panel.height - 124f, 172f, 48f), S["Subir nível"], color = LEVEL_COLOR)
    private val hold = LongPress(holdSeconds = 0.6f)
    private var holdingPreset: Int? = null
    private var toast = ""
    private var toastLeft = 0f
    private val rng = kotlin.random.Random(System.nanoTime())
    private val tap = Tap<Button>()
    private val slots = List(DeckCodec.SIZE) { Rectangle(186f + it * 72f, 1112f, 60f, 60f) }

    private fun isOpen(t: DieType) = DiceUnlocks.isUnlocked(t, level)

    /** The level-up button belongs to an open die that isn't at the top yet. */
    private fun levelUpShown() = isOpen(shown) && DieLevels.cardsToNext(game.save.dieLevel(shown)) != null

    override fun draw(delta: Float) {
        random.pressed = tap.current === random
        levelUp.enabled = game.save.canLevelUp(shown)
        levelUp.pressed = tap.current === levelUp
        for ((i, b) in presets.withIndex()) {
            b.pressed = tap.current === b
            b.color = if (game.save.deckSlot(i) != null) PRESET_FULL else Skin.PANEL
        }
        toastLeft = maxOf(0f, toastLeft - delta)
        holdingPreset?.let { i ->
            if (hold.isHeld(game.clock) && selected.size == DeckCodec.SIZE) {
                game.save.saveDeckSlot(i, selected.toList())
                say(S.f("Deck salvo no slot %d", i + 1))
                holdingPreset = null
                hold.up()
                tap.cancel()
            }
        }
        enemies.pressed = tap.current === enemies
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((t, r) in tiles) {
            val cx = r.x + r.width / 2
            val cy = r.y + r.height / 2
            if (t == shown) {
                shapes.color = Color.WHITE
                shapes.roundedRect(r.x - 6, r.y - 6, r.width + 12, r.height + 12, 20f)
            }
            if (!isOpen(t)) {
                shapes.bevelRect(r.x, r.y, r.width, r.height, 16f, LOCKED)
                shapes.glyph(t, cx, cy + 2f, 25f, LOCKED_INK, LOCKED)
                // A padlock: the shackle's lower half hides behind the body.
                shapes.color = LOCK_COLOR
                shapes.ring(cx, cy + 4f, 9f, 4f, 20)
                shapes.roundedRect(cx - 13f, cy - 16f, 26f, 20f, 5f)
                shapes.color = LOCKED
                shapes.circle(cx, cy - 7f, 3f, 12)
                continue
            }
            shapes.bevelRect(r.x, r.y, r.width, r.height, 16f, Skin.color(t))
            shapes.glyph(t, cx, cy + 2f, 25f, Skin.glyphColor(t, 0.85f), Skin.color(t))
            if (t in selected) {
                shapes.color = Skin.GOLD
                shapes.circle(r.x + r.width - 2f, r.y + r.height - 2f, 14f, 20)
                shapes.color = Color(0.15f, 0.15f, 0.2f, 1f)
                shapes.rectLine(r.x + r.width - 9f, r.y + r.height - 2f, r.x + r.width - 4f, r.y + r.height - 8f, 3.5f)
                shapes.rectLine(r.x + r.width - 4f, r.y + r.height - 8f, r.x + r.width + 5f, r.y + r.height + 5f, 3.5f)
            }
            val dieLevel = game.save.dieLevel(t)
            if (dieLevel > 1) {
                // Level pips along the bottom edge, one per level above the first.
                shapes.color = Skin.GOLD
                for (i in 1 until dieLevel) shapes.circle(r.x + 10f + (i - 1) * 11f, r.y + 7f, 3.5f, 10)
            }
        }
        for ((i, r) in slots.withIndex()) {
            val t = selected.getOrNull(i)
            if (t == null) {
                shapes.color = Skin.CELL
                shapes.roundedRect(r.x, r.y, r.width, r.height, 12f)
            } else {
                shapes.die(t, 1, r.x + r.width / 2, r.y + r.height / 2, r.width)
            }
        }
        shapes.color = Skin.SHADOW
        shapes.roundedRect(panel.x + 3f, panel.y - 8f, panel.width, panel.height, 18f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(panel.x, panel.y, panel.width, panel.height, 18f)
        shapes.color = if (isOpen(shown)) Skin.color(shown) else LOCKED
        shapes.roundedRect(panel.x, panel.y + panel.height - 12f, panel.width, 12f, 6f)
        shapes.glyph(shown, panel.x + 48f, panel.y + panel.height - 42f, 18f, if (isOpen(shown)) Skin.color(shown) else LOCKED_INK, Skin.PANEL)
        if (isOpen(shown)) {
            // Five level stars on the title line, lit up to the die's level.
            val lit = game.save.dieLevel(shown)
            for (i in 0 until DieLevels.MAX) {
                shapes.color = if (i < lit) Skin.GOLD else STAR_OFF
                shapes.polygon(panel.x + panel.width - 24f - (DieLevels.MAX - 1 - i) * 26f, panel.y + panel.height - 42f, 10f, 5, Math.PI.toFloat() / 2)
            }
        }
        enemies.drawShape(shapes)
        random.drawShape(shapes)
        presets.forEach { it.drawShape(shapes) }
        if (levelUpShown()) levelUp.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Deck"], 360f, 1222f)
        if (selected.size < DeckCodec.SIZE) {
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, S.f("escolha %d", DeckCodec.SIZE - selected.size), 556f, 1142f)
            game.smallFont.color = Color.WHITE
        }
        for ((t, r) in tiles) {
            if (isOpen(t)) {
                game.batch.textCentered(game.tinyFont, Skin.label(t), r.x + r.width / 2, r.y - 16f)
            } else {
                game.tinyFont.color = KEY_COLOR
                game.batch.textCentered(game.tinyFont, S.f("Nv. %d", DiceUnlocks.unlockLevel(t)), r.x + r.width / 2, r.y - 16f)
                game.tinyFont.color = Color.WHITE
            }
        }
        drawInfo()
        val pairs = Synergy.forDeck(selected)
        game.tinyFont.color = Skin.GOLD
        val line = if (pairs.isEmpty()) S["Sinergias do deck: nenhuma ainda (veja os pares na descrição de cada dado)"] else S.f("Sinergias do deck: %s", pairs.joinToString(", ") { S[it.title] })
        game.batch.textCentered(game.tinyFont, line, 360f, panel.y + 15f)
        game.tinyFont.color = Color.WHITE
        enemies.drawText(game.batch, game.smallFont)
        random.drawText(game.batch, game.smallFont)
        presets.forEach { it.drawText(game.batch, game.smallFont) }
        if (levelUpShown()) levelUp.drawText(game.batch, game.smallFont)
        if (toastLeft > 0f) game.batch.textShadowed(game.tinyFont, toast, 360f, 1176f, Skin.GOLD)
        game.batch.end()
    }

    /** Under the stars of an open die: its bonus and the cards towards the next level. */
    private fun levelText(): String {
        val lvl = game.save.dieLevel(shown)
        val bonus = ((DieLevels.mult(lvl) - 1f) * 100f).roundToInt()
        val cost = DieLevels.cardsToNext(lvl) ?: return S.f("máximo · +%d%%", bonus)
        return S.f("+%d%% · cartas %d/%d", bonus, game.save.cards(shown), cost)
    }

    private fun drawInfo() {
        val info = DieInfo.of(shown)
        val left = panel.x + 24f
        val keyWidth = 150f
        val valueWidth = panel.width - 48f - keyWidth
        var y = panel.y + panel.height - 30f

        game.font.draw(game.batch, Skin.fullName(shown), left + 52f, y)
        if (isOpen(shown)) {
            game.tinyFont.color = KEY_COLOR
            game.batch.textRight(game.tinyFont, levelText(), panel.x + panel.width - 24f, y - 36f)
            game.tinyFont.color = Color.WHITE
        }
        y -= 38f
        // The level-up button sits on the right of the summary, which wraps before it.
        val summaryWidth = panel.width - 48f - (if (levelUpShown()) 196f else 0f)
        y -= maxOf(game.smallFont.draw(game.batch, info.summary, left, y, summaryWidth, Align.left, true).height, if (levelUpShown()) 52f else 0f) + 8f

        val locked = if (isOpen(shown)) emptyList() else listOf(S["Bloqueado"] to S.f("Abre no nível de conta %d", DiceUnlocks.unlockLevel(shown)))
        for ((key, value) in locked + info.rows) {
            game.smallFont.color = KEY_COLOR
            game.smallFont.draw(game.batch, key, left, y)
            game.smallFont.color = Color.WHITE
            y -= game.smallFont.draw(game.batch, value, left + keyWidth, y, valueWidth, Align.left, true).height + 8f
        }
    }

    private fun buttonAt(p: Vec2) = (listOf(enemies, random) + presets + listOfNotNull(levelUp.takeIf { levelUpShown() })).firstOrNull { it.contains(p.x, p.y) }

    private fun say(text: String) {
        toast = text
        toastLeft = 2f
    }

    override fun onTouchDown(p: Vec2) {
        tap.press(buttonAt(p))
        holdingPreset = presets.indexOfFirst { it.contains(p.x, p.y) }.takeIf { it >= 0 }
        if (holdingPreset != null) hold.down(p, game.clock)
    }

    override fun onDrag(p: Vec2) = hold.move(p)

    override fun onTouchCancelled() {
        tap.cancel()
        holdingPreset = null
        hold.up()
    }

    override fun onTouchUp(p: Vec2) {
        val tile = tiles.firstOrNull { it.second.contains(p.x, p.y) }?.first
        if (tile != null) shown = tile
        val slot = slots.indexOfFirst { it.contains(p.x, p.y) }
        when {
            slot in selected.indices -> {
                shown = selected[slot]
                selected.removeAt(slot)
                tap.cancel()
            }
            tile != null && !isOpen(tile) -> tap.cancel()
            tile != null && tile in selected -> selected.remove(tile)
            tile != null && selected.size < DeckCodec.SIZE -> selected.add(tile)
            tile != null -> tap.cancel()
            else -> when (val b = tap.release(buttonAt(p))) {
                enemies -> game.showEnemies()
                random -> {
                    selected.clear()
                    selected.addAll(DeckCodec.random(rng, DiceUnlocks.unlocked(level)))
                    shown = selected.first()
                }
                levelUp -> if (game.save.levelUp(shown)) {
                    say(S.f("%s subiu para o nível %d!", Skin.fullName(shown), game.save.dieLevel(shown)))
                    game.sfx.play(setOf("record"))
                }
                else -> presets.indexOf(b).takeIf { it >= 0 }?.let { i ->
                    val deck = game.save.deckSlot(i)
                    if (deck == null) {
                        say(S["Segure para salvar o deck aqui"])
                    } else {
                        selected.clear()
                        selected.addAll(DiceUnlocks.sanitize(deck, level))
                        shown = selected.first()
                    }
                }
            }
        }
        holdingPreset = null
        hold.up()
        // A full deck is the deck: no confirm step.
        if (selected.size == DeckCodec.SIZE && selected != game.save.deck) game.save.deck = selected.toList()
    }

    private companion object {
        const val PRESETS = 3
        val PRESET_FULL = Color(0.30f, 0.38f, 0.60f, 1f)
        val LEVEL_COLOR = Color(0.72f, 0.52f, 0.12f, 1f)
        val LOCKED = Color(0.22f, 0.23f, 0.29f, 1f)
        val LOCKED_INK = Color(0.32f, 0.33f, 0.40f, 1f)
        val LOCK_COLOR = Color(0.62f, 0.64f, 0.72f, 1f)
        val STAR_OFF = Color(0.26f, 0.27f, 0.34f, 1f)
        const val COLS = 7
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
    }
}
