package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class EnemyTest {
    @Test
    fun slowTakesStrongestAndExpires() {
        val e = Enemy(EnemyKind.NORMAL, 100f)
        e.applySlow(0.2f)
        e.applySlow(0.1f)
        assertEquals(0.2f, e.slow)
        assertEquals(Balance.BASE_SPEED * 0.8f, e.speed(), 0.001f)
        e.tickEffects(2.1f)
        assertEquals(0f, e.slow)
    }

    @Test
    fun poisonCapsAt5Stacks() {
        val e = Enemy(EnemyKind.NORMAL, 1000f)
        repeat(6) { e.addPoison(10f) }
        assertEquals(5, e.poison.size)
    }

    @Test
    fun poisonDealsDpsAndExpires() {
        val e = Enemy(EnemyKind.NORMAL, 100f)
        e.addPoison(10f)
        e.tickEffects(1f)
        assertEquals(90f, e.hp, 0.001f)
        e.tickEffects(2.5f)
        assertEquals(0, e.poison.size)
    }

    @Test
    fun regenCapsAtMax() {
        val e = Enemy(EnemyKind.BOSS_REGEN, 1000f)
        e.hp = 990f
        e.tickEffects(1f)
        assertEquals(1000f, e.hp, 0.001f)
        e.hp = 500f
        e.tickEffects(1f)
        assertEquals(520f, e.hp, 0.001f)
    }

    @Test
    fun nonRegenBossDoesNotHeal() {
        val e = Enemy(EnemyKind.BOSS_SILENCER, 1000f)
        e.hp = 500f
        e.tickEffects(1f)
        assertEquals(500f, e.hp)
    }
}
