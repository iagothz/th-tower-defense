package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DieType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DieInfoTest {
    private fun row(t: DieType, key: String): String? = DieInfo.of(t).rows.firstOrNull { it.first == key }?.second
    private fun stat(t: DieType, kind: StatKind): Stat = DieInfo.of(t).stats.single { it.kind == kind }

    @Test
    fun fireShowsDamagePerPipAndRange() {
        assertEquals(Stat(StatKind.DAMAGE, "Dano", "20 → 140", "1 → 7 pips"), stat(DieType.FIRE, StatKind.DAMAGE))
        assertEquals(Stat(StatKind.TARGET, "Alvo", "Primeiro", "o mais avançado"), stat(DieType.FIRE, StatKind.TARGET))
        assertEquals("por mina, 1 → 7 pips", stat(DieType.TRAP, StatKind.DAMAGE).note)
    }

    @Test
    fun speedUsesDecimalCommaAndPipScaling() {
        assertEquals(Stat(StatKind.SPEED, "Velocidade", "1 → 0,53 s", "entre ataques"), stat(DieType.FIRE, StatKind.SPEED))
        assertEquals("0,35 → 0,09 s", stat(DieType.WIND, StatKind.SPEED).value)
    }

    @Test
    fun effectsQuoteBalanceNumbers() {
        val ice = row(DieType.ICE, "Efeito")!!
        assertTrue("20%" in ice && "50%" in ice && "2 s" in ice, ice)
        val poison = row(DieType.POISON, "Efeito")!!
        assertTrue("8 de dano/s por pip" in poison && "3 s" in poison && "5" in poison, poison)
        assertTrue("45 s" in row(DieType.GROWTH, "Efeito")!!)
        assertTrue("34,6" in row(DieType.GROWTH, "Power-up")!! && "18" in row(DieType.GROWTH, "Power-up")!!, row(DieType.GROWTH, "Power-up")!!)
        assertEquals("Primeiro", stat(DieType.LIGHT, StatKind.TARGET).value)
        assertEquals(Stat(StatKind.TARGET, "Alvo", "Mais vida", "o mais resistente"), stat(DieType.SNIPER, StatKind.TARGET))
        assertEquals("Todos", stat(DieType.QUAKE, StatKind.TARGET).value)
        assertEquals("Caminho", stat(DieType.TRAP, StatKind.TARGET).value)
    }

    @Test
    fun mineDoesNotAttack() {
        assertEquals(Stat(StatKind.TARGET, "Alvo", "Nenhum", "não ataca"), stat(DieType.MINE, StatKind.TARGET))
        assertEquals("-", stat(DieType.MINE, StatKind.DAMAGE).value)
        assertEquals("-", stat(DieType.MINE, StatKind.SPEED).value)
        assertNull(row(DieType.MINE, "Alvo"), "the three numbers live in the stats, not in the rows")
        assertTrue("10 SP por pip a cada 5 s" in row(DieType.MINE, "Efeito")!!)
    }

    @Test
    fun everyTypeHasSummaryEffectAndPowerUp() {
        for (t in DieType.entries) {
            val info = DieInfo.of(t)
            assertTrue(info.summary.isNotBlank(), "$t")
            assertEquals(StatKind.entries, info.stats.map { it.kind }, "$t: damage, speed, target in that order")
            assertTrue(row(t, "Efeito") != null, "$t effect")
            assertTrue(row(t, "Power-up") != null, "$t power-up")
        }
    }

    @Test
    fun wildcardsDescribeTheirMergeRules() {
        assertEquals("Primeiro", stat(DieType.BOOST, StatKind.TARGET).value)
        assertTrue("tipo do outro dado" in row(DieType.BOOST, "Efeito")!!)
        assertTrue("cópia" in row(DieType.CLONE, "Efeito")!!)
        assertTrue("sorteado" in row(DieType.MIRROR, "Efeito")!!)
        assertTrue("deck" in row(DieType.GROWTH, "Efeito")!!)
        assertTrue("7 pips" in row(DieType.HUNTER, "Efeito")!!)
    }

    @Test
    fun everyTypeExplainsItsSevenPipAbility() {
        for (t in DieType.entries) {
            val row = row(t, "7 pips")
            assertTrue(row != null && row.isNotBlank(), "$t")
        }
    }
}
