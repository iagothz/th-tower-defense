package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieLevels
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Progression
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceSaveTest {
    @Test
    fun cardsAddUpAndALevelUpSpendsThem() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        assertEquals(0, save.cards(DieType.FIRE))
        assertEquals(1, save.dieLevel(DieType.FIRE))
        assertFalse(save.canLevelUp(DieType.FIRE))
        save.addCards(mapOf(DieType.FIRE to 4, DieType.ICE to 1))
        save.addCards(mapOf(DieType.FIRE to 3))
        assertEquals(7, save.cards(DieType.FIRE))
        assertEquals(1, save.cards(DieType.ICE))
        assertTrue(save.canLevelUp(DieType.FIRE))
        assertTrue(save.canLevelUpAny())
        assertTrue(save.levelUp(DieType.FIRE))
        assertEquals(2, save.dieLevel(DieType.FIRE))
        assertEquals(7 - DieLevels.cardsToNext(1)!!, save.cards(DieType.FIRE))
        assertFalse(save.levelUp(DieType.FIRE), "two cards are not enough for level 3")
        assertFalse(save.canLevelUpAny())
        assertEquals(mapOf(DieType.FIRE to 2), save.dieLevels())
    }

    @Test
    fun aMaxedDieTakesNoMoreLevels() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        save.addCards(mapOf(DieType.WIND to 1000))
        repeat(DieLevels.MAX - 1) { assertTrue(save.levelUp(DieType.WIND)) }
        assertEquals(DieLevels.MAX, save.dieLevel(DieType.WIND))
        assertFalse(save.canLevelUp(DieType.WIND))
        assertFalse(save.levelUp(DieType.WIND))
        assertEquals(1000 - 75, save.cards(DieType.WIND))
        assertFalse(DieType.WIND in save.cardCandidates(), "maxed dice draw no more cards")
    }

    @Test
    fun unlocksFollowTheAccountLevel() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        val second = DiceUnlocks.ORDER.first()
        assertEquals(1, save.accountLevel)
        assertFalse(save.isUnlocked(second))
        assertEquals(DiceUnlocks.STARTERS.toSet(), save.unlockedDice().toSet())
        assertEquals(DiceUnlocks.STARTERS.toSet(), save.cardCandidates().toSet())
        save.xp = Progression.costToNext(1)
        assertEquals(2, save.accountLevel)
        assertTrue(save.isUnlocked(second))
        assertTrue(second in save.cardCandidates())
        assertTrue(save.playableDeck().all { save.isUnlocked(it) })
    }

    @Test
    fun theDeckPlayedNeverHoldsALockedDie() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        val locked = DiceUnlocks.ORDER.last()
        save.deck = listOf(DieType.FIRE, DieType.ICE, locked, DieType.MINE, DieType.WIND)
        val deck = save.playableDeck()
        assertEquals(5, deck.toSet().size)
        assertFalse(locked in deck)
        assertEquals(DieType.FIRE, deck[0])
    }

    @Test
    fun veteransGetOneRetroactiveChestForTheirWaves() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        assertEquals(emptyMap(), save.retroCards(Random(1)), "a fresh save has nothing to pay for")
        save.recordMatch(GameMap.DEFAULT, save.deck, wave = 12, kills = 100, bosses = 1, merges = 30)
        assertEquals(emptyMap(), save.retroCards(Random(1)), "the chest was already settled for this save")

        val veteran = SaveData(SaveDataTest.memoryPrefs())
        veteran.recordMatch(GameMap.DEFAULT, veteran.deck, wave = 12, kills = 100, bosses = 1, merges = 30)
        veteran.recordMatch(GameMap.DEFAULT, veteran.deck, wave = 8, kills = 60, bosses = 0, merges = 20)
        val chest = veteran.retroCards(Random(1))
        assertEquals(12, chest.values.sum(), "(12 + 8) / 2 waves + 2 per boss")
        assertTrue(chest.keys.all { it in DiceUnlocks.STARTERS })
        assertEquals(12, DieType.entries.sumOf { veteran.cards(it) })
        assertEquals(emptyMap(), veteran.retroCards(Random(1)), "once only")

        val grinder = SaveData(SaveDataTest.memoryPrefs())
        repeat(30) { grinder.recordMatch(GameMap.DEFAULT, grinder.deck, wave = 20, kills = 1, bosses = 2, merges = 1) }
        assertEquals(SaveData.RETRO_CAP, grinder.retroCards(Random(1)).values.sum())
    }
}
