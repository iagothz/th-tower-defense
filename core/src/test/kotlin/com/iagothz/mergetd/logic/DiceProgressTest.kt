package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiceProgressTest {
    @Test
    fun everyDieUnlocksExactlyOnceBetweenLevelsOneAndEighteen() {
        assertEquals(DieType.entries.toSet(), (DiceUnlocks.STARTERS + DiceUnlocks.ORDER).toSet())
        assertEquals(DieType.entries.size, DiceUnlocks.STARTERS.size + DiceUnlocks.ORDER.size, "no die listed twice")
        assertEquals(8, DiceUnlocks.STARTERS.size)
        assertEquals(1, DiceUnlocks.unlockLevel(DieType.FIRE))
        assertEquals(2, DiceUnlocks.unlockLevel(DiceUnlocks.ORDER.first()))
        assertEquals(18, DiceUnlocks.unlockLevel(DiceUnlocks.ORDER.last()))
        assertEquals(DiceUnlocks.STARTERS.toSet(), DiceUnlocks.unlocked(1).toSet())
        assertEquals(9, DiceUnlocks.unlocked(2).size)
        assertEquals(DieType.entries.size, DiceUnlocks.unlocked(18).size)
        assertEquals(DieType.entries.size, DiceUnlocks.unlocked(40).size)
    }

    @Test
    fun eachLevelFromTwoOpensOneDie() {
        assertNull(DiceUnlocks.openedAt(1))
        assertEquals(DiceUnlocks.ORDER[0], DiceUnlocks.openedAt(2))
        assertEquals(DiceUnlocks.ORDER[16], DiceUnlocks.openedAt(18))
        assertNull(DiceUnlocks.openedAt(19))
        assertTrue(DiceUnlocks.isUnlocked(DieType.MINE, 1))
        assertTrue(!DiceUnlocks.isUnlocked(DiceUnlocks.ORDER[3], 4))
        assertTrue(DiceUnlocks.isUnlocked(DiceUnlocks.ORDER[3], 5))
    }

    @Test
    fun sanitizeSwapsLockedDiceForUnlockedOnesAndKeepsTheRest() {
        val locked = DiceUnlocks.ORDER.takeLast(2)
        val deck = listOf(DieType.FIRE, locked[0], DieType.ICE, locked[1], DieType.MINE)
        val fixed = DiceUnlocks.sanitize(deck, level = 1)
        assertEquals(DeckCodec.SIZE, fixed.size)
        assertEquals(DeckCodec.SIZE, fixed.toSet().size)
        assertTrue(fixed.all { DiceUnlocks.isUnlocked(it, 1) }, "$fixed")
        assertEquals(DieType.FIRE, fixed[0])
        assertEquals(DieType.ICE, fixed[2])
        assertEquals(DieType.MINE, fixed[4])
        assertEquals(deck, DiceUnlocks.sanitize(deck, level = 18), "nothing to swap once everything is open")
    }

    @Test
    fun dieLevelsMultiplyLikeAPermanentPowerUp() {
        assertEquals(1f, DieLevels.mult(1), 1e-6f)
        assertEquals(1.1f, DieLevels.mult(2), 1e-6f)
        assertEquals(1.4f, DieLevels.mult(DieLevels.MAX), 1e-6f)
        assertEquals(5, DieLevels.cardsToNext(1))
        assertEquals(10, DieLevels.cardsToNext(2))
        assertEquals(40, DieLevels.cardsToNext(4))
        assertNull(DieLevels.cardsToNext(DieLevels.MAX))
    }

    @Test
    fun theChestGrowsWithWavesAndBosses() {
        assertEquals(1, CardDrop.count(wave = 0, bosses = 0))
        assertEquals(1, CardDrop.count(wave = 1, bosses = 0))
        assertEquals(5, CardDrop.count(wave = 10, bosses = 0))
        assertEquals(7, CardDrop.count(wave = 10, bosses = 1))
        assertEquals(14, CardDrop.count(wave = 20, bosses = 2))
    }

    @Test
    fun theDrawSpreadsOverTheCandidatesOnly() {
        val candidates = listOf(DieType.FIRE, DieType.ICE, DieType.WIND)
        val drop = CardDrop.draw(candidates, 30, Random(3))
        assertEquals(30, drop.values.sum())
        assertTrue(drop.keys.all { it in candidates }, "$drop")
        assertTrue(drop.size > 1, "thirty cards land on more than one die: $drop")
        assertEquals(emptyMap(), CardDrop.draw(emptyList(), 5, Random(1)))
        assertEquals(emptyMap(), CardDrop.draw(candidates, 0, Random(1)))
        assertEquals(CardDrop.draw(candidates, 12, Random(9)), CardDrop.draw(candidates, 12, Random(9)), "same seed, same chest")
    }
}
