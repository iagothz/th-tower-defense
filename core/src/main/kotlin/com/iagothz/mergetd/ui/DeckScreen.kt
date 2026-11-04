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
 * Tapping a die only shows what it does in the panel below; the deck changes by dragging (`DeckEditor`): a die
 * from the grid onto a slot, a slot's die out of the slots, slot onto slot to swap. Dice the account level hasn't
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
    private val editor = DeckEditor(selected, tiles, slots, ::isOpen)
    private val measure = com.badlogic.gdx.graphics.g2d.GlyphLayout()

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
                game.cloud.push()
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
        val target = editor.pointer?.let { editor.slotAt(it) }
        for ((i, r) in slots.withIndex()) {
            val t = selected.getOrNull(i)?.takeIf { i != editor.fromSlot }
            if (i == target) {
                // The slot the drag would land in.
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
        drawStatShapes()
        editor.dragging?.let { t -> editor.pointer?.let { p -> shapes.die(t, 1, p.x, p.y + DRAG_LIFT, 72f) } }
        shapes.end()

        game.batch.begin()
        game.batch.textShadowed(game.bigFont, S["Deck"], 360f, 1222f)
        if (selected.size < DeckCodec.SIZE) {
            game.smallFont.color = KEY_COLOR
            game.batch.textLeft(game.smallFont, S.f("arraste %d", DeckCodec.SIZE - selected.size), 556f, 1142f)
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
        if (toastLeft > 0f) game.batch.textShadowed(game.tinyFont, toast, 360f, TOAST_Y, Skin.GOLD)
        game.batch.end()
        // The font has no arrow glyph: the "→" in the stat texts is drawn here, in the gaps left for it.
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (a in arrows) {
            shapes.color = a.color
            shapes.rectLine(a.x - a.size * 0.45f, a.y, a.x + a.size * 0.2f, a.y, a.size * 0.16f)
            shapes.triangle(a.x + a.size * 0.05f, a.y + a.size * 0.3f, a.x + a.size * 0.05f, a.y - a.size * 0.3f, a.x + a.size * 0.5f, a.y)
        }
        shapes.end()
        arrows.clear()
    }

    /** Under the stars of an open die: its bonus and the cards towards the next level. */
    private fun levelText(): String {
        val lvl = game.save.dieLevel(shown)
        val bonus = ((DieLevels.mult(lvl) - 1f) * 100f).roundToInt()
        val cost = DieLevels.cardsToNext(lvl) ?: return S.f("máximo · +%d%%", bonus)
        return S.f("+%d%% · cartas %d/%d", bonus, game.save.cards(shown), cost)
    }

    /** Width the summary wraps at: it leaves room for the level-up button when that shows. */
    private fun summaryWidth() = panel.width - 48f - (if (levelUpShown()) 196f else 0f)

    /** Top of the damage/speed/target blocks, just under the summary (and the level-up button). */
    private fun statsTop(): Float {
        val summary = measure.apply { setText(game.smallFont, DieInfo.of(shown).summary, Color.WHITE, summaryWidth(), Align.left, true) }.height
        return panel.y + panel.height - 68f - maxOf(summary, if (levelUpShown()) 52f else 0f) - 18f
    }

    private fun statRect(i: Int, top: Float): Rectangle {
        val w = (panel.width - 48f - 2 * STAT_GAP) / 3
        return Rectangle(panel.x + 24f + i * (w + STAT_GAP), top - STAT_H, w, STAT_H)
    }

    /** The three blocks (damage, speed, target), each a card with its icon in its own colour. */
    private fun drawStatShapes() {
        val shapes = game.shapes
        val top = statsTop()
        for ((i, s) in DieInfo.of(shown).stats.withIndex()) {
            val r = statRect(i, top)
            shapes.color = Skin.CELL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 14f)
            shapes.color = statColor(s.kind)
            shapes.roundedRect(r.x, r.y + r.height - 5f, r.width, 5f, 2.5f)
            statIcon(shapes, s.kind, r.x + 24f, r.y + r.height - 28f)
        }
    }

    private fun statColor(k: StatKind): Color = when (k) {
        StatKind.DAMAGE -> Skin.HEART
        StatKind.SPEED -> SPEED_COLOR
        StatKind.TARGET -> Skin.GOLD
    }

    /** Sword, stopwatch or crosshair, about 28 px, centred on (cx, cy). */
    private fun statIcon(shapes: ShapeRenderer, k: StatKind, cx: Float, cy: Float) {
        shapes.color = statColor(k)
        when (k) {
            StatKind.DAMAGE -> {
                shapes.rectLine(cx - 9f, cy - 9f, cx + 11f, cy + 11f, 5f)
                shapes.triangle(cx + 9f, cy + 13f, cx + 14f, cy + 14f, cx + 13f, cy + 9f)
                shapes.rectLine(cx - 13f, cy - 3f, cx - 3f, cy - 13f, 4f)
                shapes.rectLine(cx - 10f, cy - 10f, cx - 15f, cy - 15f, 4f)
            }
            StatKind.SPEED -> {
                shapes.ring(cx, cy - 1f, 12f, 3.5f, 24)
                shapes.rect(cx - 3f, cy + 12f, 6f, 5f)
                shapes.rectLine(cx, cy - 1f, cx, cy + 7f, 3f)
                shapes.rectLine(cx, cy - 1f, cx + 6f, cy - 4f, 3f)
            }
            StatKind.TARGET -> {
                shapes.ring(cx, cy, 11f, 3.5f, 24)
                shapes.rectLine(cx - 16f, cy, cx - 7f, cy, 3f)
                shapes.rectLine(cx + 7f, cy, cx + 16f, cy, 3f)
                shapes.rectLine(cx, cy - 16f, cx, cy - 7f, 3f)
                shapes.rectLine(cx, cy + 7f, cx, cy + 16f, 3f)
                shapes.circle(cx, cy, 3f, 12)
            }
        }
    }

    private class Arrow(val x: Float, val y: Float, val size: Float, val color: Color)
    private val arrows = mutableListOf<Arrow>()

    /** Left-aligned text vertically centred on [y]; each "→" becomes a gap with an arrow drawn in it after the batch. */
    private fun textWithArrow(font: com.badlogic.gdx.graphics.g2d.BitmapFont, text: String, x: Float, y: Float, color: Color) {
        val parts = text.split("→")
        val arrowW = font.capHeight * 1.6f
        var cx = x
        font.color = color
        for ((i, part) in parts.withIndex()) {
            val p = if (i == 0) part.trimEnd() else part.trim()
            game.batch.textLeft(font, p, cx, y)
            cx += measure.apply { setText(font, p) }.width
            if (i < parts.size - 1) {
                arrows += Arrow(cx + arrowW / 2 + 2f, y, font.capHeight * 1.1f, color.cpy())
                cx += arrowW + 4f
            }
        }
        font.color = Color.WHITE
    }

    /** Labels of the detail rows in a colour each, so they read as separate things. */
    private fun rowColor(key: String): Color = when (key) {
        S["Efeito"] -> EFFECT_COLOR
        S["7 pips"] -> Skin.GOLD
        S["Sinergia"] -> SYNERGY_COLOR
        S["Power-up"] -> POWER_COLOR
        else -> KEY_COLOR
    }

    private fun drawInfo() {
        val info = DieInfo.of(shown)
        val left = panel.x + 24f
        val keyWidth = 112f
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
        game.smallFont.draw(game.batch, info.summary, left, y, summaryWidth(), Align.left, true)

        val top = statsTop()
        for ((i, s) in info.stats.withIndex()) {
            val r = statRect(i, top)
            game.tinyFont.color = statColor(s.kind)
            game.batch.textLeft(game.tinyFont, s.label.uppercase(), r.x + 46f, r.y + r.height - 28f)
            textWithArrow(game.smallFont, s.value, r.x + 14f, r.y + 38f, Color.WHITE)
            textWithArrow(game.tinyFont, s.note, r.x + 14f, r.y + 14f, KEY_COLOR)
        }
        game.smallFont.color = Color.WHITE
        y = top - STAT_H - 18f

        val locked = if (isOpen(shown)) emptyList() else listOf(S["Bloqueado"] to S.f("Abre no nível de conta %d", DiceUnlocks.unlockLevel(shown)))
        for ((key, value) in locked + info.rows) {
            game.tinyFont.color = rowColor(key)
            game.tinyFont.draw(game.batch, key.uppercase(), left, y)
            game.tinyFont.color = Color.WHITE
            y -= game.tinyFont.draw(game.batch, value, left + keyWidth, y, valueWidth, Align.left, true).height + 12f
        }
        game.tinyFont.color = Color.WHITE
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
        editor.down(p)
    }

    override fun onDrag(p: Vec2) {
        hold.move(p)
        val lifted = editor.dragging == null
        editor.move(p)
        if (lifted && editor.dragging != null) game.sfx.play(setOf("pick"))
    }

    override fun onTouchCancelled() {
        tap.cancel()
        holdingPreset = null
        hold.up()
        editor.cancel()
    }

    override fun onTouchUp(p: Vec2) {
        val wasDragging = editor.dragging != null
        val onSlot = slots.any { it.contains(p.x, p.y) }
        val release = editor.up(p)
        val die = release.shown
        when {
            die != null -> {
                shown = die
                when {
                    release.changed -> game.sfx.play(setOf("drop"))
                    wasDragging -> Unit
                    // A tap only shows the die; the toast teaches the gesture that would move it.
                    onSlot -> say(S["Arraste para fora para tirar do deck"])
                    isOpen(die) && die !in selected -> say(S["Arraste até um espaço do deck para usar"])
                }
            }
            else -> when (val b = tap.release(buttonAt(p))) {
                enemies -> game.showEnemies()
                random -> {
                    selected.clear()
                    selected.addAll(DeckCodec.random(rng, DiceUnlocks.unlocked(level)))
                    shown = selected.first()
                }
                levelUp -> if (game.save.levelUp(shown)) {
                    game.cloud.push()
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
        if (selected.size == DeckCodec.SIZE && selected != game.save.deck) {
            game.save.deck = selected.toList()
            game.cloud.push()
        }
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
        /** The dragged die floats this much above the finger so it isn't hidden under it. */
        const val DRAG_LIFT = 40f
        /** The toast sits in the gap between the title and the deck slots. */
        const val TOAST_Y = 1188f
        val KEY_COLOR = Color(0.65f, 0.67f, 0.75f, 1f)
        const val STAT_H = 92f
        const val STAT_GAP = 12f
        val SPEED_COLOR = Color(0.45f, 0.72f, 1f, 1f)
        val EFFECT_COLOR = Color(0.78f, 0.82f, 0.95f, 1f)
        val SYNERGY_COLOR = Color(0.45f, 0.85f, 0.55f, 1f)
        val POWER_COLOR = Color(0.78f, 0.55f, 1f, 1f)
    }
}
