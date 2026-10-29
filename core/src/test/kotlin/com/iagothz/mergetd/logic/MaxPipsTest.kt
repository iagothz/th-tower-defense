package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every die does something new at 7 pips. */
class MaxPipsTest {
    private val path = Layout.PATH
    private val origin = Layout.cellCenter(Cell(1, 2))

    private fun enemy(distance: Float, hp: Float = 100_000f, kind: EnemyKind = EnemyKind.NORMAL) = Enemy(kind, hp).also { it.distance = distance }

    private fun attack(die: Die, vararg enemies: Enemy, bonus: AttackBonus = AttackBonus.NONE, rng: Random = Random(1)) =
        Combat.attack(die, origin, 1f, enemies.toList(), path, bonus, rng)

    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    private fun Match.advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            update(Balance.STEP)
            left -= Balance.STEP
        }
    }

    @Test
    fun fireSplashIsWiderAndFullAtSeven() {
        val target = enemy(500f)
        val near = enemy(560f)
        attack(Die(DieType.FIRE, 7), target, near)
        assertEquals(100_000f - target.hp, 100_000f - near.hp, 0.01f)
        val far = enemy(500f + 100f)
        attack(Die(DieType.FIRE, 7), enemy(500f), far)
        assertTrue(far.hp < 100_000f)
    }

    @Test
    fun iceFreezesAtSeven() {
        val e = enemy(500f)
        attack(Die(DieType.ICE, 7), e)
        assertEquals(Balance.ICE_FREEZE, e.stunTime)
        val f = enemy(500f)
        attack(Die(DieType.ICE, 6), f)
        assertEquals(0f, f.stunTime)
    }

    @Test
    fun poisonLastsTwiceAsLongAtSeven() {
        val e = enemy(500f)
        attack(Die(DieType.POISON, 7), e)
        assertEquals(Balance.POISON_DURATION * 2, e.poison.single().remaining)
    }

    @Test
    fun electricChainsToFiveAtSeven() {
        val targets = List(7) { enemy(500f + it * 30f) }
        val shot = attack(Die(DieType.ELECTRIC, 7), *targets.toTypedArray())!!
        assertEquals(1 + Balance.CHAIN_JUMPS_MAX, shot.to.size)
    }

    @Test
    fun windDoublesItsSpeedAtSeven() {
        assertEquals(Combat.attackInterval(DieType.WIND, 6) / 2 * (1 + Balance.ATTACK_SPEED_PER_PIP * 5) / (1 + Balance.ATTACK_SPEED_PER_PIP * 6), Combat.attackInterval(DieType.WIND, 7), 1e-4f)
        assertEquals(Balance.stats(DieType.FIRE).baseInterval / (1 + Balance.ATTACK_SPEED_PER_PIP * 6), Combat.attackInterval(DieType.FIRE, 7), 1e-4f)
    }

    @Test
    fun ironQuadruplesOnBossesAtSeven() {
        val boss = enemy(500f, kind = EnemyKind.BOSS_REGEN)
        assertEquals(Combat.baseDamage(DieType.IRON, 7, 1f) * Balance.IRON_BOSS_MULT_MAX, Combat.hitDamage(DieType.IRON, 7, 1f, boss), 0.01f)
    }

    @Test
    fun mineDoublesIncomeAtSeven() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.MINE, 7)
        val before = m.sp
        m.advance(Balance.MINE_INTERVAL + 0.1f)
        assertEquals(before + 2 * Balance.MINE_SP_PER_PIP * 7, m.sp)
    }

    @Test
    fun sniperHitsTheTwoToughestAtSeven() {
        val a = enemy(300f, hp = 5000f)
        val b = enemy(400f, hp = 4000f)
        val c = enemy(500f, hp = 3000f)
        attack(Die(DieType.SNIPER, 7), a, b, c)
        assertTrue(a.hp < 5000f && b.hp < 4000f && c.hp == 3000f)
    }

    @Test
    fun mirrorBorrowsTheStrongestDamageAtSeven() {
        val e = enemy(500f)
        attack(Die(DieType.MIRROR, 7), e, bonus = AttackBonus(strongestDamage = 900f))
        assertEquals(900f, 100_000f - e.hp, 0.01f)
    }

    @Test
    fun growthAtSevenPlantsDice() {
        val m = match()
        m.lives = 1000
        m.board[Cell(0, 0)] = Die(DieType.GROWTH, 7)
        m.advance(Balance.GROWTH_INTERVAL + 0.2f)
        assertEquals(2, m.board.occupied().size)
        assertEquals(1, m.board.occupied().first { it.first != Cell(0, 0) }.second.pips)
    }

    @Test
    fun lightAtSevenReachesDiagonals() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.LIGHT, 7)
        assertEquals(1 + Balance.LIGHT_SPEED_PER_PIP * 7, m.speedMultiplier(Cell(1, 1)), 1e-4f)
        m.board[Cell(0, 0)] = Die(DieType.LIGHT, 6)
        assertEquals(1f, m.speedMultiplier(Cell(1, 1)), 1e-4f)
    }

    @Test
    fun critAtSevenRaisesTheMultiplierAndComboAtSevenCountsTwice() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.CRIT, 7)
        m.board[Cell(0, 1)] = Die(DieType.COMBO, 7)
        m.board[Cell(0, 2)] = Die(DieType.COMBO, 1)
        val b = m.attackBonus()
        assertEquals(Balance.CRIT_MULT_MAX, b.critMult)
        assertEquals(3, b.comboCount)
    }

    @Test
    fun curseAtSevenIsStrongerAndLonger() {
        val e = enemy(500f)
        attack(Die(DieType.CURSE, 7), e)
        assertEquals((Balance.CURSE_BASE + Balance.CURSE_PER_PIP * 7) * 1.5f, e.curse, 1e-4f)
        assertEquals(Balance.CURSE_DURATION * 2, e.curseTime)
    }

    @Test
    fun hunterStacksAreWorthMoreAtSeven() {
        val e = enemy(500f)
        attack(Die(DieType.HUNTER, 7).apply { stacks = 10 }, e)
        assertEquals(Combat.baseDamage(DieType.HUNTER, 7, 1f) * (1 + Balance.HUNTER_BONUS_PER_STACK_MAX * 10), 100_000f - e.hp, 0.01f)
    }

    @Test
    fun gravityAtSevenIgnoresThePushBudgetOnCommonEnemies() {
        val e = enemy(900f).also { it.pushed = Balance.GRAVITY_MAX_PUSH }
        attack(Die(DieType.GRAVITY, 7), e)
        assertTrue(e.distance < 900f)
        val boss = enemy(900f, kind = EnemyKind.BOSS_REGEN).also { it.pushed = Balance.GRAVITY_MAX_PUSH }
        attack(Die(DieType.GRAVITY, 7), boss)
        assertEquals(900f, boss.distance)
    }

    @Test
    fun timeAtSevenFreezesTheNeighbourhood() {
        val target = enemy(500f)
        val near = enemy(450f)
        val far = enemy(380f)
        attack(Die(DieType.TIME, 7), target, near, far)
        assertTrue(target.stunTime > 0f && near.stunTime > 0f && far.stunTime == 0f)
    }

    @Test
    fun trapAtSevenPlantsMoreAndBiggerMines() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.TRAP, 7)
        m.debugAddEnemy(enemy(300f).also { it.applyStun(100f) })
        m.advance(0.1f)
        assertEquals(Balance.TRAP_RADIUS * Balance.TRAP_MAX_RADIUS, m.traps.single().radius)
        m.advance(20f)
        assertEquals(Balance.TRAP_MAX_PER_DIE_MAX, m.traps.size)
    }

    @Test
    fun quakeAtSevenSlowsEveryone() {
        val a = enemy(500f)
        val b = enemy(300f)
        attack(Die(DieType.QUAKE, 7), a, b)
        assertEquals(Balance.QUAKE_SLOW, a.slow)
        assertEquals(Balance.QUAKE_SLOW, b.slow)
        assertEquals(Balance.QUAKE_SLOW_TIME, b.slowTime)
    }

    @Test
    fun reaperAtSevenExecutesBosses() {
        val boss = enemy(500f, hp = 10_000f, kind = EnemyKind.BOSS_REGEN).also { it.hp = 500f }
        attack(Die(DieType.REAPER, 7), boss)
        assertEquals(0f, boss.hp)
        val tough = enemy(500f, hp = 10_000f, kind = EnemyKind.BOSS_REGEN).also { it.hp = 2000f }
        attack(Die(DieType.REAPER, 7), tough)
        assertTrue(tough.hp > 0f)
    }

    @Test
    fun luckAtSevenNeverWhiffs() {
        val e = enemy(500f)
        repeat(30) { attack(Die(DieType.LUCK, 7), e, rng = Random(it)) }
        val perShot = (100_000f - e.hp) / 30
        assertTrue(perShot >= Combat.baseDamage(DieType.LUCK, 7, 1f) * 1.5f, "$perShot")
    }

    @Test
    fun cloneAtSevenCopiesAnyPips() {
        val board = Board()
        board[Cell(0, 0)] = Die(DieType.CLONE, 7)
        board[Cell(0, 1)] = Die(DieType.FIRE, 3)
        assertTrue(board.canMerge(Cell(0, 0), Cell(0, 1)))
        assertTrue(board.merge(Cell(0, 0), Cell(0, 1), DeckCodec.DEFAULT, Random(1)))
        assertEquals(DieType.FIRE, board[Cell(0, 0)]!!.type)
        assertEquals(3, board[Cell(0, 0)]!!.pips)
        board[Cell(1, 0)] = Die(DieType.CLONE, 6)
        assertTrue(!board.canMerge(Cell(1, 0), Cell(0, 1)))
    }

    @Test
    fun boostAtSevenHandsOutPowerLevels() {
        val m = match()
        m.lives = 1000
        m.board[Cell(0, 0)] = Die(DieType.BOOST, 7)
        m.advance(Balance.BOOST_INTERVAL + 0.2f)
        assertEquals(6, DeckCodec.DEFAULT.sumOf { m.powerLevel(it) })
        assertTrue(m.drainEvents().any { it is GameEvent.PoweredUp })
    }

    @Test
    fun vampireAtSevenGivesDoubleSpAndLivesEveryHundredKills() {
        val m = match()
        val v = Die(DieType.VAMPIRE, 7).apply { stacks = 99 }
        m.board[Cell(1, 2)] = v
        m.lives = 3
        m.debugAddEnemy(enemy(500f, hp = 1f))
        val before = m.sp
        m.update(Balance.STEP)
        assertEquals(before + EnemyKind.NORMAL.reward + 2 * Balance.VAMPIRE_SP_PER_PIP * 7, m.sp)
        assertEquals(4, m.lives)
        assertEquals(100, v.stacks)
    }

    @Test
    fun novaAtSevenExplodesWider() {
        val m = match()
        val nova = Die(DieType.NOVA, 7)
        m.board[Cell(1, 2)] = nova
        val victim = enemy(900f, hp = 1f)
        val far = enemy(900f - 120f, hp = 100_000f)
        m.debugAddEnemy(victim)
        m.debugAddEnemy(far)
        m.update(Balance.STEP)
        assertTrue(far.hp < 100_000f, "120 away is inside the 7-pip blast")
    }
}
