package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class CombatTest {
    private val path = Layout.PATH
    private val origin = Layout.cellCenter(Cell(1, 2))

    private fun enemy(distance: Float, hp: Float = 1000f, kind: EnemyKind = EnemyKind.NORMAL) =
        Enemy(kind, hp).also { it.distance = distance }

    @Test
    fun intervalScalesWithPips() {
        assertEquals(1.0f, Combat.attackInterval(DieType.FIRE, 1), 1e-4f)
        assertEquals(1.0f / 1.3f, Combat.attackInterval(DieType.FIRE, 3), 1e-4f)
    }

    @Test
    fun damageFormula() {
        assertEquals(20f * 3 * 1.3f, Combat.hitDamage(DieType.FIRE, 3, 2, enemy(0f)), 1e-3f)
    }

    @Test
    fun ironDoublesVsBoss() {
        assertEquals(36f, Combat.hitDamage(DieType.IRON, 1, 1, enemy(0f, kind = EnemyKind.BOSS_SWAPPER)), 1e-3f)
        assertEquals(18f, Combat.hitDamage(DieType.IRON, 1, 1, enemy(0f)), 1e-3f)
    }

    @Test
    fun targetsFurthest() {
        val far = enemy(500f)
        assertSame(far, Combat.selectTarget(DieType.FIRE, listOf(enemy(100f), far, enemy(300f))))
    }

    @Test
    fun sniperTargetsHighestHp() {
        val tough = enemy(100f, hp = 5000f)
        assertSame(tough, Combat.selectTarget(DieType.SNIPER, listOf(tough, enemy(500f), enemy(300f))))
    }

    @Test
    fun ignoresDead() {
        val dead = enemy(500f).also { it.hp = 0f }
        assertNull(Combat.attack(Die(DieType.FIRE, 1), origin, 1, listOf(dead), path))
    }

    @Test
    fun attackReturnsShot() {
        val target = enemy(500f)
        val shot = assertNotNull(Combat.attack(Die(DieType.WIND, 1), origin, 1, listOf(target), path))
        assertEquals(origin, shot.from)
        assertEquals(listOf(path.positionAt(500f)), shot.to)
        assertEquals(994f, target.hp, 1e-3f)
    }

    @Test
    fun fireSplash() {
        val target = enemy(500f)
        val near = enemy(480f)
        val far = enemy(100f)
        Combat.attack(Die(DieType.FIRE, 1), origin, 1, listOf(target, near, far), path)
        assertEquals(980f, target.hp, 1e-3f)
        assertEquals(990f, near.hp, 1e-3f)
        assertEquals(1000f, far.hp)
    }

    @Test
    fun shotReportsEveryDamagedEnemy() {
        val target = enemy(500f)
        val near = enemy(480f)
        val shot = assertNotNull(Combat.attack(Die(DieType.FIRE, 1), origin, 1, listOf(target, near), path))
        assertEquals(20f, shot.damage.getValue(target), 1e-3f)
        assertEquals(10f, shot.damage.getValue(near), 1e-3f)
        assertEquals(2, shot.damage.size)
    }

    @Test
    fun iceSlowByPips() {
        val e1 = enemy(500f)
        Combat.attack(Die(DieType.ICE, 1), origin, 1, listOf(e1), path)
        assertEquals(0.20f, e1.slow, 1e-4f)
        val e7 = enemy(500f)
        Combat.attack(Die(DieType.ICE, 7), origin, 1, listOf(e7), path)
        assertEquals(0.50f, e7.slow, 1e-4f)
    }

    @Test
    fun poisonAddsStack() {
        val e = enemy(500f)
        Combat.attack(Die(DieType.POISON, 2), origin, 2, listOf(e), path)
        assertEquals(1, e.poison.size)
        assertEquals(Balance.POISON_DPS * 2 * 1.3f, e.poison[0].dps, 1e-3f)
    }

    @Test
    fun electricChains() {
        val enemies = listOf(enemy(100f), enemy(200f), enemy(300f), enemy(400f))
        val shot = assertNotNull(Combat.attack(Die(DieType.ELECTRIC, 1), origin, 1, enemies, path))
        assertEquals(985f, enemies[3].hp, 1e-3f)
        assertEquals(1000f - 15f * 0.7f, enemies[2].hp, 1e-3f)
        assertEquals(1000f - 15f * 0.49f, enemies[1].hp, 1e-3f)
        assertEquals(1000f, enemies[0].hp)
        assertEquals(3, shot.to.size)
    }

    @Test
    fun electricRangeLimit() {
        val target = enemy(500f)
        val neighbour = enemy(350f)
        Combat.attack(Die(DieType.ELECTRIC, 1), origin, 1, listOf(target, neighbour), path)
        assertEquals(985f, target.hp, 1e-3f)
        assertEquals(1000f, neighbour.hp)
    }
}
