package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NewEnemiesTest {
    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    private fun enemy(kind: EnemyKind, distance: Float, hp: Float = 1000f) = Enemy(kind, hp).also { it.distance = distance }

    private fun Match.advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            update(Balance.STEP)
            left -= Balance.STEP
        }
    }

    // --- Boss order ---

    @Test
    fun bossOrderIsAShuffleOfAllBossesAndWave10UsesIt() {
        val orders = (1..6).map { Match(DeckCodec.DEFAULT, Random(it.toLong())).bossOrder }
        for (o in orders) assertEquals(EnemyKind.entries.filter { it.isBoss }.toSet(), o.toSet())
        assertTrue(orders.toSet().size > 1, "different seeds should give different orders")
        val m = match()
        m.debugStartWave(10)
        m.update(Balance.STEP)
        assertEquals(m.bossOrder[0], m.enemies.single().kind)
        m.debugStartWave(20)
        m.update(Balance.STEP)
        assertEquals(m.bossOrder[1], m.enemies.single().kind)
    }

    // --- Healer ---

    @Test
    fun healerHealsNearbyEnemiesButNotFarOnes() {
        val m = match()
        val near = enemy(EnemyKind.NORMAL, 500f).also { it.hp = 100f }
        val far = enemy(EnemyKind.NORMAL, 800f).also { it.hp = 100f }
        listOf(near, far, enemy(EnemyKind.HEALER, 520f)).forEach { m.debugAddEnemy(it) }
        m.advance(1f)
        assertTrue(near.hp > 120f, "near ${near.hp}")
        assertEquals(100f, far.hp, 0.01f)
    }

    // --- Ghost ---

    @Test
    fun ghostIgnoresControlEffectsAndTraps() {
        val g = enemy(EnemyKind.GHOST, 500f)
        g.applySlow(0.5f)
        g.applyStun(1f)
        g.pushBack(100f)
        assertEquals(0f, g.slow)
        assertEquals(0f, g.stunTime)
        assertEquals(500f, g.distance)

        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.TRAP, 1)
        val ghost = enemy(EnemyKind.GHOST, 500f)
        m.debugAddEnemy(ghost)
        m.advance(3f)
        assertTrue(m.traps.isNotEmpty(), "the mines must not go off under a ghost")
        assertEquals(1000f, ghost.hp)
    }

    // --- Armored ---

    @Test
    fun armoredBossShrugsOffSmallHitsButNotPoison() {
        val m = match()
        m.debugStartWave(10)
        val boss = Enemy(EnemyKind.BOSS_ARMORED, 10_000f).also { it.distance = 500f; it.armor = 15f }
        m.debugAddEnemy(boss)
        assertEquals(0f, boss.takeDamage(10f, null))
        assertEquals(35f, boss.takeDamage(50f, null))
        val before = boss.hp
        boss.addPoison(10f)
        boss.tickEffects(1f)
        assertEquals(before - 10f, boss.hp, 0.01f)
    }

    @Test
    fun armorIsSetFromTheWaveOnSpawn() {
        val m = match()
        m.debugStartWave(10)
        m.debugSpawn(EnemyKind.BOSS_ARMORED)
        val armored = m.enemies.single { it.kind == EnemyKind.BOSS_ARMORED }
        assertEquals(Balance.ARMOR_FRACTION * WaveSpawner.enemyHp(10, EnemyKind.NORMAL), armored.armor, 0.01f)
        m.debugSpawn(EnemyKind.NORMAL)
        assertEquals(0f, m.enemies.first { it.kind == EnemyKind.NORMAL }.armor)
    }

    // --- Thief ---

    @Test
    fun thiefStealsSpAndGivesItBackWhenKilled() {
        val m = match()
        m.sp = 1000
        val thief = enemy(EnemyKind.BOSS_THIEF, 500f)
        m.debugAddEnemy(thief)
        m.advance(Balance.BOSS_ABILITY_INTERVAL + 0.1f)
        assertEquals(850, m.sp)
        assertEquals(150, thief.stolen)
        assertTrue(m.drainEvents().any { it is GameEvent.Stolen && it.amount == 150 })
        thief.hp = 0f
        m.update(Balance.STEP)
        assertEquals(850 + 150 + EnemyKind.BOSS_THIEF.reward, m.sp)
    }

    // --- Splitter ---

    @Test
    fun splitterLeavesTwoTanksBehind() {
        val m = match()
        val boss = enemy(EnemyKind.BOSS_SPLITTER, 600f, hp = 4000f)
        m.debugAddEnemy(boss)
        boss.hp = 0f
        m.update(Balance.STEP)
        val halves = m.enemies
        assertEquals(2, halves.size)
        assertTrue(halves.all { it.kind == EnemyKind.TANK && it.maxHp == 1000f && it.hp == 1000f })
        assertTrue(halves.all { kotlin.math.abs(it.distance - 600f) <= 30f })
    }

    // --- Commander ---

    @Test
    fun commanderHastesEnemiesAroundIt() {
        val m = match()
        val escorted = enemy(EnemyKind.NORMAL, 600f)
        val alone = enemy(EnemyKind.NORMAL, 100f)
        listOf(escorted, alone, enemy(EnemyKind.BOSS_COMMANDER, 650f)).forEach { m.debugAddEnemy(it) }
        m.advance(1f)
        val plain = alone.distance - 100f
        val hasted = escorted.distance - 600f
        assertEquals(plain * Balance.COMMANDER_HASTE, hasted, 1f)
        assertNotEquals(plain, hasted)
    }
}
