package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SynergyTest {
    @Test
    fun pairsAreDistinctAndDescribed() {
        assertEquals(8, Synergy.entries.size)
        assertEquals(Synergy.entries.size, Synergy.entries.map { setOf(it.a, it.b) }.toSet().size)
        for (s in Synergy.entries) {
            assertTrue(s.title.isNotBlank() && s.description.isNotBlank(), "$s")
        }
    }

    @Test
    fun activeNeedsBothTypesAndIgnoresSilencedDice() {
        assertEquals(emptySet(), Synergy.active(listOf(DieType.FIRE)))
        assertEquals(setOf(Synergy.BLAZE), Synergy.active(listOf(DieType.FIRE, DieType.WIND, DieType.ICE)))
        assertEquals(setOf(Synergy.PLAGUE, Synergy.PIERCE), Synergy.forDeck(listOf(DieType.POISON, DieType.CURSE, DieType.IRON, DieType.SNIPER, DieType.MINE)))

        val m = Match(DeckCodec.DEFAULT, Random(1))
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.WIND, 1).apply { disabledFor = 2f }
        assertEquals(emptySet(), m.synergies())
        m.board[Cell(0, 1)]!!.disabledFor = 0f
        assertEquals(setOf(Synergy.BLAZE), m.synergies())
    }

    @Test
    fun blazeWidensFireSplashAndShatterDoublesQuakeOnSlowedEnemies() {
        val path = Layout.PATH
        val origin = Layout.cellCenter(Cell(1, 2))
        val target = Enemy(EnemyKind.NORMAL, 1000f).also { it.distance = 500f }
        val far = Enemy(EnemyKind.NORMAL, 1000f).also { it.distance = 500f - 90f }
        Combat.attack(Die(DieType.FIRE, 1), origin, 1, listOf(target, far), path)
        assertEquals(1000f, far.hp, "90 away is outside the plain splash")
        Combat.attack(Die(DieType.FIRE, 1), origin, 1, listOf(target, far), path, AttackBonus(synergies = setOf(Synergy.BLAZE)))
        assertTrue(far.hp < 1000f, "Blaze reaches 90 away")

        val slowed = Enemy(EnemyKind.NORMAL, 1000f).also { it.distance = 400f; it.applySlow(0.3f) }
        val plain = Enemy(EnemyKind.NORMAL, 1000f).also { it.distance = 420f }
        Combat.attack(Die(DieType.QUAKE, 1), origin, 1, listOf(slowed, plain), path, AttackBonus(synergies = setOf(Synergy.SHATTER)))
        assertEquals(2f * (1000f - plain.hp), 1000f - slowed.hp, 0.01f)
    }

    @Test
    fun dazzleAddsCritChanceOnTheBoard() {
        val m = Match(DeckCodec.DEFAULT, Random(1))
        m.board[Cell(0, 0)] = Die(DieType.LIGHT, 1)
        m.board[Cell(0, 1)] = Die(DieType.CRIT, 1)
        assertEquals(Balance.CRIT_CHANCE_PER_PIP + Balance.DAZZLE_CRIT, m.attackBonus().critChance, 1e-4f)
    }
}
