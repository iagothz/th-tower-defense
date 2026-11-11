package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchCodeTest {
    private val deck = listOf(DieType.HUNTER, DieType.BOOST, DieType.FIRE, DieType.MINE, DieType.VAMPIRE)

    @Test
    fun roundTripsEverything() {
        val setup = MatchSetup(GameMap.RING, GameMode.RUSH, -123_456_789, deck)
        val code = MatchCode.encode(setup)
        assertTrue(Regex("[0-9A-Z]{5}-[0-9A-Z]{5}-[0-9A-Z]{5}").matches(code), code)
        assertEquals(setup, MatchCode.decode(code))
        assertEquals(setup, MatchCode.decode(code.lowercase().replace("-", " ")), "lenient about case, dashes and spaces")
    }

    @Test
    fun garbageAndTyposAreRefused() {
        assertNull(MatchCode.decode("hello"))
        assertNull(MatchCode.decode(""))
        val code = MatchCode.encode(MatchSetup(GameMap.HORSESHOE, GameMode.NORMAL, 42, deck))
        val typo = code.replaceRange(2, 3, if (code[2] == 'A') "B" else "A")
        assertNull(MatchCode.decode(typo))
    }

    @Test
    fun aMatchFromACodeIsReproducible() {
        val setup = MatchSetup(GameMap.SERPENT, GameMode.NORMAL, 777, deck)
        val a = setup.newMatch()
        val b = setup.newMatch()
        repeat(4) {
            a.buyDie()
            b.buyDie()
        }
        assertEquals(a.board.occupied().map { it.first to it.second.type }, b.board.occupied().map { it.first to it.second.type })
        assertEquals(777, a.seed)
        assertTrue(a.locked)
        val restored = Match.restore(a.snapshot())!!
        assertEquals(777, restored.seed)
        assertTrue(restored.locked)
        assertEquals(setup, restored.setup)
    }

    @Test
    fun ordinaryMatchesCarryASeedToo() {
        val m = Match.seeded(DeckCodec.DEFAULT, GameMap.RING, GameMode.SURVIVAL, rng = Random(9))
        assertTrue(m.seed != null)
        assertTrue(!m.locked)
        assertEquals(m.seed, Match.restore(m.snapshot())!!.seed)
    }
}
