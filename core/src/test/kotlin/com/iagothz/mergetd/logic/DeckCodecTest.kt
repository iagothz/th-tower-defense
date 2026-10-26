package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
