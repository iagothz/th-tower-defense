package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchTest {
    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    /** Advances in small frames, since a single update is clamped to MAX_FRAME. */
    private fun Match.advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.1f, left)
            update(dt)
            left -= dt
        }
    }

    private fun Match.fill() = board.cells().forEach { board[it] = Die(DieType.FIRE, 1) }

    @Test
    fun startsWith150SpAnd3Lives() {
        val m = match()
        assertEquals(150, m.sp)
        assertEquals(3, m.lives)
        assertEquals(0, m.wave)
    }

    @Test
    fun buyCostIncreases() {
        val m = match()
        assertEquals(10, m.buyCost)
        assertTrue(m.buyDie())
        assertEquals(20, m.buyCost)
        assertTrue(m.buyDie())
        assertEquals(30, m.buyCost)
        assertEquals(150 - 10 - 20, m.sp)
    }

    @Test
    fun buyPlacesOnePipDeckDie() {
        val m = match()
        m.buyDie()
        val dice = m.board.occupied().map { it.second }
        assertEquals(1, dice.size)
        assertEquals(1, dice[0].pips)
        assertTrue(dice[0].type in m.deck)
    }

    @Test
    fun buyOnFullBoardKeepsSp() {
        val m = match()
        m.fill()
        m.sp = 1000
        assertFalse(m.canBuy())
        assertFalse(m.buyDie())
        assertEquals(1000, m.sp)
        assertEquals(10, m.buyCost)
    }

    @Test
    fun cannotBuyWithoutSp() {
        val m = match()
        m.sp = 9
        assertFalse(m.buyDie())
        assertEquals(9, m.sp)
        assertTrue(m.board.occupied().isEmpty())
    }

    @Test
    fun powerUpCostsAndCaps() {
        val m = match()
        m.sp = 10_000
        val costs = mutableListOf<Int>()
        while (true) {
            val cost = m.powerUpCost(DieType.FIRE) ?: break
            costs += cost
            assertTrue(m.powerUp(DieType.FIRE))
        }
        assertEquals(listOf(100, 200, 400, 700), costs)
        assertEquals(10_000 - 1400, m.sp)
        assertEquals(5, m.powerLevel(DieType.FIRE))
        assertNull(m.powerUpCost(DieType.FIRE))
        assertFalse(m.powerUp(DieType.FIRE))
        assertEquals(2.5f, m.powerMult(DieType.FIRE))
        assertEquals(1, m.powerLevel(DieType.ICE))
    }

    @Test
    fun powerUpNeedsSp() {
        val m = match()
        m.sp = 99
        assertFalse(m.powerUp(DieType.FIRE))
        assertEquals(1, m.powerLevel(DieType.FIRE))
        assertEquals(99, m.sp)
    }

    @Test
    fun mergeUsesBoardRules() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 1)
        assertTrue(m.canMerge(Cell(0, 0), Cell(0, 1)))
        assertTrue(m.merge(Cell(0, 0), Cell(0, 1)))
        assertEquals(2, m.board[Cell(0, 1)]!!.pips)
    }

    @Test
    fun firstWaveStartsAfter3s() {
        val m = match()
        m.advance(2.9f)
        assertEquals(0, m.wave)
        m.advance(0.2f)
        assertEquals(1, m.wave)
        assertEquals(0f, m.waveCountdown)
        m.advance(0.6f * 10)
        assertEquals(10, m.enemies.size)
    }

    @Test
    fun leakingCostsLives() {
        val m = match()
        m.advance(60f)
        assertTrue(m.isGameOver)
        assertEquals(0, m.lives)
        assertFalse(m.buyDie())
    }

    @Test
    fun killRewardPaidOnce() {
        val m = match()
        listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)).forEach { m.board[it] = Die(DieType.FIRE, 1) }
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 1f).also { it.distance = 500f })
        m.update(Balance.STEP)
        assertEquals(160, m.sp)
        assertTrue(m.enemies.isEmpty())
        assertEquals(1, m.lastShots.size)
    }

    @Test
    fun nextWaveStartsAfterClear() {
        val m = match()
        m.sp = 0
        m.board.cells().forEach { m.board[it] = Die(DieType.WIND, 7) }
        m.advance(3.1f)
        assertEquals(1, m.wave)
        m.advance(6f)
        assertTrue(m.enemies.isEmpty())
        assertEquals(100, m.sp)
        assertEquals(1, m.wave)
        m.advance(3.2f)
        assertEquals(2, m.wave)
        assertEquals(3, m.lives)
    }

    @Test
    fun mineGeneratesSp() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.MINE, 2)
        m.advance(5.05f)
        assertEquals(170, m.sp)
    }

    @Test
    fun growthAddsPip() {
        val m = match()
        m.lives = 1000
        m.board[Cell(0, 0)] = Die(DieType.GROWTH, 1)
        m.board[Cell(0, 1)] = Die(DieType.GROWTH, 7)
        m.advance(59.9f)
        assertEquals(1, m.board[Cell(0, 0)]!!.pips)
        m.advance(0.2f)
        assertEquals(2, m.board[Cell(0, 0)]!!.pips)
        assertEquals(7, m.board[Cell(0, 1)]!!.pips)
    }

    @Test
    fun silencedDieDoesNotAttack() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1).apply { disabledFor = 10f }
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 1f).also { it.distance = 500f })
        m.update(Balance.STEP)
        assertEquals(1, m.enemies.size)
        assertEquals(150, m.sp)
    }

    @Test
    fun hugeDeltaIsClamped() {
        val m = match()
        m.update(10f)
        assertTrue(m.waveCountdown >= 2.75f - 1e-4f, "countdown ${m.waveCountdown}")
        assertTrue(m.waveCountdown < 3f)
    }

    @Test
    fun bossAbilitiesWithEmptyBoard() {
        listOf(10, 20, 30).forEach { n ->
            val m = match()
            m.lives = 1000
            m.debugStartWave(n)
            m.advance(13f)
            m.board[Cell(1, 1)] = Die(DieType.WIND, 1)
            m.advance(13f)
            assertEquals(n, m.wave)
        }
    }

    @Test
    fun silencerDisablesADie() {
        val m = match()
        m.lives = 1000
        m.board[Cell(0, 0)] = Die(DieType.MINE, 1)
        m.debugStartWave(10)
        m.advance(6.1f)
        assertTrue(m.board[Cell(0, 0)]!!.disabled)
    }

    @Test
    fun swapperMovesDice() {
        val m = match()
        m.lives = 1000
        val a = Die(DieType.MINE, 1)
        val b = Die(DieType.MINE, 2)
        m.board[Cell(0, 0)] = a
        m.board[Cell(2, 4)] = b
        m.debugStartWave(20)
        m.advance(6.1f)
        assertEquals(b, m.board[Cell(0, 0)])
        assertEquals(a, m.board[Cell(2, 4)])
    }

    @Test
    fun bossTakesTwoLives() {
        val m = match()
        m.debugAddEnemy(Enemy(EnemyKind.BOSS_SILENCER, 1000f).also { it.distance = Layout.PATH.length - 0.1f })
        m.update(Balance.STEP)
        assertEquals(1, m.lives)
        assertTrue(m.enemies.isEmpty())
    }

    @Test
    fun buysOnlyAttackersUntilThreeOnBoard() {
        val deck = listOf(DieType.MINE, DieType.MIRROR, DieType.FIRE, DieType.ICE, DieType.POISON)
        for (seed in 1..40) {
            val m = Match(deck, Random(seed))
            m.sp = 100_000
            repeat(3) { m.buyDie() }
            val types = m.board.occupied().map { it.second.type }
            assertTrue(DieType.MINE !in types, "seed $seed: $types")
        }
    }

    @Test
    fun buysWholeDeckOnceThreeAttackersAreOnBoard() {
        val deck = listOf(DieType.MINE, DieType.MIRROR, DieType.FIRE, DieType.ICE, DieType.POISON)
        val boughtMine = (1..10).count { seed ->
            val m = Match(deck, Random(seed))
            m.sp = 100_000
            repeat(3) { m.board[Cell(2, it)] = Die(DieType.FIRE, 1) }
            repeat(12) { m.buyDie() }
            m.board.occupied().any { it.second.type == DieType.MINE }
        }
        assertTrue(boughtMine >= 5, "only $boughtMine of 10 seeds bought a Mine")
    }

    @Test
    fun mirrorAttacks() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.MIRROR, 1)
        val enemy = Enemy(EnemyKind.NORMAL, 100f).also { it.distance = 500f }
        m.debugAddEnemy(enemy)
        m.update(Balance.STEP)
        assertEquals(100f - Balance.stats(DieType.MIRROR).baseDamage, enemy.hp)
        assertTrue(enemy.hp < 100f)
    }
}
