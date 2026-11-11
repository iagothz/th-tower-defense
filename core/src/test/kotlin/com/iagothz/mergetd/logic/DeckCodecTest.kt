package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeckCodecTest {
    @Test
    fun roundTrip() {
        val deck = listOf(DieType.WIND, DieType.SNIPER, DieType.MIRROR, DieType.GROWTH, DieType.IRON)
        assertEquals(deck, DeckCodec.decode(DeckCodec.encode(deck)))
    }

    @Test
    fun defaultDeck() {
        assertEquals(
            listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MINE),
            DeckCodec.DEFAULT,
        )
    }

    @Test
    fun fallsBackToDefault() {
        listOf(
            null,
            "",
            "fire,ice",
            "fire,fire,ice,poison,mine",
            "fire,ice,poison,electric,banana",
        ).forEach { assertEquals(DeckCodec.DEFAULT, DeckCodec.decode(it), "input: $it") }
    }

    @Test
    fun randomDeckHasFiveDistinctTypes() {
        val a = DeckCodec.random(Random(1))
        assertEquals(DeckCodec.SIZE, a.size)
        assertEquals(DeckCodec.SIZE, a.toSet().size)
        val b = DeckCodec.random(Random(2))
        assertTrue(a != b, "$a")
    }
}
