package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
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
    fun buyingAtACellPutsTheDieThere() {
        val m = match()
        val cell = Cell(2, 1)
        assertTrue(m.canBuyAt(cell))
        assertTrue(m.buyDie(cell))
        assertEquals(1, m.board[cell]?.pips)
        assertTrue(m.board[cell]!!.type in m.deck)
        assertEquals(150 - 10, m.sp)
        assertEquals(20, m.buyCost)
        assertEquals(GameEvent.DieBought(cell), m.drainEvents().last { it is GameEvent.DieBought })
    }

    @Test
    fun buyingAtAnOccupiedCellOrWithoutSpDoesNothing() {
        val m = match()
        val cell = Cell(0, 0)
        assertTrue(m.buyDie(cell))
        assertFalse(m.canBuyAt(cell))
        assertFalse(m.buyDie(cell))
        assertEquals(140, m.sp)
        m.sp = 5
        assertFalse(m.canBuyAt(Cell(1, 1)))
        assertFalse(m.buyDie(Cell(1, 1)))
        assertEquals(5, m.sp)
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
        m.advance(Balance.GROWTH_INTERVAL - 0.1f)
        assertEquals(1, m.board[Cell(0, 0)]!!.pips)
        m.advance(0.2f)
        assertEquals(2, m.board[Cell(0, 0)]!!.pips)
        assertEquals(7, m.board[Cell(0, 1)]!!.pips)
    }

    /** Each Growth power-up level shortens the wait: the interval is divided by the power multiplier. */
    @Test
    fun growthPowerUpsMakeItGrowFaster() {
        val m = Match(listOf(DieType.GROWTH, DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC), Random(5))
        m.lives = 1000
        m.sp = 100000
        assertTrue(m.powerUp(DieType.GROWTH))
        assertTrue(m.powerUp(DieType.GROWTH))
        assertEquals(3, m.powerLevel(DieType.GROWTH))
        val faster = Balance.GROWTH_INTERVAL / Balance.powerMult(3)
        assertEquals(faster, m.growthInterval())
        m.board[Cell(0, 0)] = Die(DieType.GROWTH, 1)
        m.advance(faster - 0.2f)
        assertEquals(1, m.board[Cell(0, 0)]!!.pips)
        m.advance(0.4f)
        assertEquals(2, m.board[Cell(0, 0)]!!.pips)
    }

    /** Gaining a pip rerolls the Growth into one of the deck's types, with the new pips. */
    @Test
    fun growthBecomesADeckTypeWhenItGainsAPip() {
        val deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.HUNTER)
        val types = (0 until 30).map { seed ->
            val m = Match(deck, Random(seed))
            m.lives = 1000
            m.board[Cell(0, 0)] = Die(DieType.GROWTH, 3)
            m.advance(Balance.GROWTH_INTERVAL + 0.1f)
            val die = m.board[Cell(0, 0)]!!
            assertEquals(4, die.pips)
            die.type
        }
        assertTrue(types.all { it in deck }, "$types")
        assertTrue(types.toSet().size > 1, "$types")
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
        m.debugAddEnemy(Enemy(EnemyKind.BOSS_SILENCER, 100_000f).also { it.distance = 100f })
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
        m.debugAddEnemy(Enemy(EnemyKind.BOSS_SWAPPER, 100_000f).also { it.distance = 100f })
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
    fun mirrorAttacks() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.MIRROR, 1)
        val enemy = Enemy(EnemyKind.NORMAL, 100f).also { it.distance = 500f }
        m.debugAddEnemy(enemy)
        m.update(Balance.STEP)
        assertEquals(100f - Balance.stats(DieType.MIRROR).baseDamage, enemy.hp)
        assertTrue(enemy.hp < 100f)
    }

    /** The match walks its own map's path, so the leak point moves with the map. */
    @Test
    fun matchFollowsItsMap() {
        val m = Match(DeckCodec.DEFAULT, Random(7), GameMap.STRAIGHT)
        assertEquals(GameMap.STRAIGHT.path, m.path)
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 100f).also { it.distance = GameMap.STRAIGHT.path.length - 0.1f })
        m.update(Balance.STEP)
        assertEquals(Balance.START_LIVES - 1, m.lives)
        assertTrue(m.enemies.isEmpty())
    }

    // --- Undo ---

    @Test
    fun undoWithinTheWindowPutsBothDiceBack() {
        val m = match()
        val a = Die(DieType.FIRE, 2).apply { disabledFor = 1.5f }
        val b = Die(DieType.FIRE, 2)
        m.board[Cell(0, 0)] = a
        m.board[Cell(0, 1)] = b
        assertTrue(m.merge(Cell(0, 0), Cell(0, 1)))
        assertTrue(m.canUndo)
        m.advance(Balance.UNDO_WINDOW - 0.2f)
        assertTrue(m.undoMerge())
        assertSame(a, m.board[Cell(0, 0)])
        assertSame(b, m.board[Cell(0, 1)])
        assertFalse(m.canUndo)
        assertTrue(m.drainEvents().any { it == GameEvent.Unmerged(Cell(0, 0), Cell(0, 1)) })
    }

    @Test
    fun undoExpiresAndIsRefusedAfterTheResultWasUsed() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 2)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 2)
        assertTrue(m.merge(Cell(0, 0), Cell(0, 1)))
        m.advance(Balance.UNDO_WINDOW + 0.1f)
        assertFalse(m.canUndo)
        assertFalse(m.undoMerge())

        val n = match()
        n.board[Cell(0, 0)] = Die(DieType.FIRE, 2)
        n.board[Cell(0, 1)] = Die(DieType.FIRE, 2)
        n.board[Cell(0, 2)] = Die(DieType.MIRROR, 3)
        assertTrue(n.merge(Cell(0, 0), Cell(0, 1)))
        assertTrue(n.merge(Cell(0, 2), Cell(0, 1)), "mirror merges with the 3-pip result")
        assertTrue(n.canUndo, "the latest merge can still be undone")
        assertTrue(n.undoMerge())
        assertEquals(3, n.board[Cell(0, 1)]!!.pips)
        assertEquals(DieType.MIRROR, n.board[Cell(0, 2)]!!.type)
        assertFalse(n.undoMerge(), "only the last merge is undoable")
    }

    @Test
    fun cloningCannotBeUndone() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.CLONE, 2)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 2)
        assertTrue(m.merge(Cell(0, 0), Cell(0, 1)))
        assertFalse(m.canUndo)
    }

    @Test
    fun undoIsRefusedIfTheSwapperMovedTheResult() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 2)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 2)
        assertTrue(m.merge(Cell(0, 0), Cell(0, 1)))
        m.board.swap(Cell(0, 1), Cell(2, 4))
        assertFalse(m.undoMerge())
    }
}
