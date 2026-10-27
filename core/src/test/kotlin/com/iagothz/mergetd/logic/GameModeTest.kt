package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameModeTest {
    private fun match(mode: GameMode) = Match(DeckCodec.DEFAULT, Random(7), mode = mode)

    @Test
    fun modesHaveIdsTitlesAndDescriptions() {
        assertEquals(4, GameMode.entries.size)
        assertEquals(GameMode.entries.size, GameMode.entries.map { it.id }.toSet().size)
        for (m in GameMode.entries) assertTrue(m.title.isNotBlank() && m.description.isNotBlank(), "$m")
        assertEquals(GameMode.NORMAL, GameMode.byId("x") ?: GameMode.NORMAL)
        assertEquals(GameMode.BOSS_RUSH, GameMode.byId("bossrush"))
    }

    @Test
    fun bossRushIsABossEveryWaveWithTrimmedHp() {
        val m = match(GameMode.BOSS_RUSH)
        m.debugStartWave(1)
        m.update(Balance.STEP)
        val boss = m.enemies.single()
        assertTrue(boss.kind.isBoss)
        assertEquals(m.bossOrder[0], boss.kind)
        assertEquals(WaveSpawner.enemyHp(1, boss.kind) * Balance.BOSS_RUSH_HP, boss.maxHp, 0.01f)
        m.debugStartWave(2)
        m.update(Balance.STEP)
        assertEquals(m.bossOrder[1], m.enemies.single().kind)
    }

    @Test
    fun survivalHasOneLifeAndDoubleRewards() {
        val m = match(GameMode.SURVIVAL)
        assertEquals(1, m.lives)
        assertEquals(Balance.START_SP * 2, m.sp)
        m.board[Cell(1, 2)] = Die(DieType.FIRE, 1)
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 1f).also { it.distance = 500f })
        val before = m.sp
        m.update(Balance.STEP)
        assertEquals(before + EnemyKind.NORMAL.reward * 2, m.sp)
    }

    @Test
    fun rushIsFasterAndFrailer() {
        val m = match(GameMode.RUSH)
        m.debugStartWave(1)
        m.update(Balance.STEP)
        val e = m.enemies.single()
        assertEquals(WaveSpawner.enemyHp(1, EnemyKind.NORMAL) * Balance.RUSH_HP, e.maxHp, 0.01f)
        assertEquals(Balance.BASE_SPEED * Balance.RUSH_SPEED, e.speed(), 0.01f)
        assertEquals(Balance.WAVE_DELAY * Balance.RUSH_DELAY, m.waveDelay)
    }

    @Test
    fun modeSurvivesTheSnapshot() {
        val m = match(GameMode.RUSH)
        assertEquals(GameMode.RUSH, Match.restore(m.snapshot())!!.mode)
        assertEquals(GameMode.NORMAL, Match(DeckCodec.DEFAULT).mode)
    }
}
