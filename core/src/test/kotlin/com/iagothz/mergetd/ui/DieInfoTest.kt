package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DieType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DieInfoTest {
    private fun row(t: DieType, key: String): String? = DieInfo.of(t).rows.firstOrNull { it.first == key }?.second

    @Test
    fun fireShowsDamagePerPipAndRange() {
        assertEquals("20 por pip (20 com 1 pip, 140 com 7)", row(DieType.FIRE, "Dano"))
        assertEquals("Inimigo mais à frente", row(DieType.FIRE, "Alvo"))
    }

    @Test
    fun speedUsesDecimalCommaAndPipScaling() {
        assertEquals("a cada 1 s (1 pip) até 0,53 s (7 pips)", row(DieType.FIRE, "Velocidade"))
        assertEquals("a cada 0,35 s (1 pip) até 0,09 s (7 pips)", row(DieType.WIND, "Velocidade"))
    }

    @Test
    fun effectsQuoteBalanceNumbers() {
        val ice = row(DieType.ICE, "Efeito")!!
        assertTrue("20%" in ice && "50%" in ice && "2 s" in ice, ice)
        val poison = row(DieType.POISON, "Efeito")!!
        assertTrue("8 de dano/s por pip" in poison && "3 s" in poison && "5" in poison, poison)
        assertTrue("75 s" in row(DieType.GROWTH, "Efeito")!!)
        assertEquals("Inimigo mais à frente", row(DieType.LIGHT, "Alvo"))
        assertEquals("Inimigo com mais vida", row(DieType.SNIPER, "Alvo"))
    }

    @Test
    fun mineDoesNotAttack() {
        assertEquals("Não ataca", row(DieType.MINE, "Alvo"))
        assertNull(row(DieType.MINE, "Dano"))
        assertNull(row(DieType.MINE, "Velocidade"))
        assertTrue("10 SP por pip a cada 5 s" in row(DieType.MINE, "Efeito")!!)
    }

    @Test
    fun everyTypeHasSummaryEffectAndPowerUp() {
        for (t in DieType.entries) {
            val info = DieInfo.of(t)
            assertTrue(info.summary.isNotBlank(), "$t")
            assertTrue(row(t, "Efeito") != null, "$t effect")
            assertTrue(row(t, "Power-up") != null, "$t power-up")
        }
    }

    @Test
    fun wildcardsDescribeTheirMergeRules() {
        assertEquals("Inimigo mais à frente", row(DieType.BOOST, "Alvo"))
        assertTrue("mesmos pips" in row(DieType.BOOST, "Efeito")!!)
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
