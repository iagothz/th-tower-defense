package com.iagothz.mergetd.ui

import com.badlogic.gdx.math.Rectangle
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeckEditorTest {
    private val tiles = DieType.entries.mapIndexed { i, t -> t to Rectangle(24f + (i % 7) * 98f, 1010f - (i / 7) * 116f, 84f, 84f) }
    private val slots = List(5) { Rectangle(186f + it * 72f, 1112f, 60f, 60f) }
    private val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MINE)
    private val nowhere = Vec2(360f, 400f)

    private fun editor(deck: List<DieType> = this.deck, locked: Set<DieType> = emptySet()) = DeckEditor(deck.toMutableList(), tiles, slots) { it !in locked }

    private fun centre(r: Rectangle) = Vec2(r.x + r.width / 2, r.y + r.height / 2)
    private fun tile(t: DieType) = centre(tiles.first { it.first == t }.second)
    private fun slot(i: Int) = centre(slots[i])

    private fun DeckEditor.drag(from: Vec2, to: Vec2): DeckEditor.Release {
        down(from)
        move(Vec2(from.x + 40f, from.y + 40f))
        move(to)
        return up(to)
    }

    @Test
    fun tappingATileOnlyShowsIt() {
        val e = editor()
        e.down(tile(DieType.WIND))
        val r = e.up(tile(DieType.WIND))
        assertEquals(DieType.WIND, r.shown)
        assertFalse(r.changed)
        assertEquals(deck, e.deck)
    }

    @Test
    fun tappingASelectedTileKeepsItInTheDeck() {
        val e = editor()
        e.down(tile(DieType.ICE))
        val r = e.up(tile(DieType.ICE))
        assertEquals(DieType.ICE, r.shown)
        assertEquals(deck, e.deck)
    }

    @Test
    fun tappingASlotShowsItsDieAndKeepsIt() {
        val e = editor()
        e.down(slot(2))
        val r = e.up(slot(2))
        assertEquals(DieType.POISON, r.shown)
        assertFalse(r.changed)
        assertEquals(deck, e.deck)
    }

    @Test
    fun aSmallWobbleIsStillATap() {
        val e = editor()
        val p = tile(DieType.WIND)
        e.down(p)
        e.move(Vec2(p.x + 5f, p.y - 5f))
        assertNull(e.dragging)
        val r = e.up(Vec2(p.x + 5f, p.y - 5f))
        assertFalse(r.changed)
        assertEquals(deck, e.deck)
    }

    @Test
    fun theDraggedDieFollowsTheFinger() {
        val e = editor()
        e.down(tile(DieType.WIND))
        e.move(nowhere)
        assertEquals(DieType.WIND, e.dragging)
        assertEquals(nowhere, e.pointer)
    }

    @Test
    fun draggingATileToAnEmptySlotAddsIt() {
        val e = editor(deck.take(4))
        val r = e.drag(tile(DieType.WIND), slot(4))
        assertTrue(r.changed)
        assertEquals(deck.take(4) + DieType.WIND, e.deck)
        assertNull(e.dragging)
    }

    @Test
    fun draggingATileOntoAnOccupiedSlotReplacesThatDie() {
        val e = editor()
        val r = e.drag(tile(DieType.WIND), slot(1))
        assertTrue(r.changed)
        assertEquals(DieType.WIND, r.shown)
        assertEquals(listOf(DieType.FIRE, DieType.WIND, DieType.POISON, DieType.ELECTRIC, DieType.MINE), e.deck)
    }

    @Test
    fun droppingBetweenTwoSlotsLandsOnTheNearest() {
        val e = editor()
        val between = Vec2(slots[2].x - 4f, slots[2].y + 30f)
        e.drag(tile(DieType.WIND), between)
        assertEquals(DieType.WIND, e.deck[2])
    }

    @Test
    fun draggingATileNowhereChangesNothing() {
        val e = editor(deck.take(4))
        val r = e.drag(tile(DieType.WIND), nowhere)
        assertFalse(r.changed)
        assertEquals(DieType.WIND, r.shown)
        assertEquals(deck.take(4), e.deck)
    }

    @Test
    fun draggingASlotOutRemovesItsDie() {
        val e = editor()
        val r = e.drag(slot(1), nowhere)
        assertTrue(r.changed)
        assertEquals(DieType.ICE, r.shown)
        assertEquals(listOf(DieType.FIRE, DieType.POISON, DieType.ELECTRIC, DieType.MINE), e.deck)
    }

    @Test
    fun draggingASlotOntoAnotherSwapsThem() {
        val e = editor()
        val r = e.drag(slot(0), slot(3))
        assertTrue(r.changed)
        assertEquals(listOf(DieType.ELECTRIC, DieType.ICE, DieType.POISON, DieType.FIRE, DieType.MINE), e.deck)
    }

    @Test
    fun draggingASlotBackOntoItselfChangesNothing() {
        val e = editor()
        val r = e.drag(slot(2), slot(2))
        assertFalse(r.changed)
        assertEquals(deck, e.deck)
    }

    @Test
    fun draggingASelectedTileOntoASlotMovesItThere() {
        val e = editor()
        e.drag(tile(DieType.FIRE), slot(3))
        assertEquals(listOf(DieType.ELECTRIC, DieType.ICE, DieType.POISON, DieType.FIRE, DieType.MINE), e.deck)
    }

    @Test
    fun aLockedTileNeverDrags() {
        val e = editor(deck.take(4), locked = setOf(DieType.WIND))
        e.down(tile(DieType.WIND))
        e.move(slot(4))
        assertNull(e.dragging)
        val r = e.up(slot(4))
        assertEquals(DieType.WIND, r.shown, "it still shows what the die does")
        assertFalse(r.changed)
        assertEquals(deck.take(4), e.deck)
    }

    @Test
    fun anEmptySlotHasNothingToDrag() {
        val e = editor(deck.take(3))
        assertTrue(e.down(slot(4)))
        e.move(nowhere)
        assertNull(e.dragging)
        val r = e.up(nowhere)
        assertNull(r.shown)
        assertFalse(r.changed)
    }

    @Test
    fun pressingOutsideTilesAndSlotsIsNotAPress() {
        val e = editor()
        assertFalse(e.down(nowhere))
        assertEquals(DeckEditor.Release(null, false), e.up(nowhere))
    }

    @Test
    fun cancellingDropsTheDieBackWithoutAChange() {
        val e = editor(deck.take(4))
        e.down(tile(DieType.WIND))
        e.move(slot(4))
        e.cancel()
        assertNull(e.dragging)
        assertEquals(deck.take(4), e.deck)
    }
}
