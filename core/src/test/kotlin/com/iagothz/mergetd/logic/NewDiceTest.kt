package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NewDiceTest {
    private val path = Layout.PATH
    private val origin = Layout.cellCenter(Cell(1, 2))

    private fun enemy(distance: Float, hp: Float = 1000f, kind: EnemyKind = EnemyKind.NORMAL) =
        Enemy(kind, hp).also { it.distance = distance }

    private fun attack(die: Die, vararg enemies: Enemy, bonus: AttackBonus = AttackBonus.NONE, rng: Random = Random(1)) =
        Combat.attack(die, origin, 1, enemies.toList(), path, bonus, rng)

    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    // --- Combat ---

    @Test
    fun hitsRememberTheDie() {
        val die = Die(DieType.FIRE, 1)
        val e = enemy(300f)
        attack(die, e)
        assertSame(die, e.lastHit)
    }

    @Test
    fun curseAppliesAndAmplifiesLaterDamage() {
        val e = enemy(300f)
        attack(Die(DieType.CURSE, 2), e)
        assertEquals(0.21f, e.curse, 1e-4f)
        assertEquals(Balance.CURSE_DURATION, e.curseTime)
        val before = e.hp
        attack(Die(DieType.FIRE, 1), e)
        assertEquals(20f * 1.21f, before - e.hp, 1e-3f)
    }

    @Test
    fun curseAmplifiesPoisonAndExpires() {
        val e = enemy(300f).apply { curse = 0.5f; curseTime = 1f }
        e.addPoison(10f)
        e.tickEffects(0.5f)
        assertEquals(1000f - 7.5f, e.hp, 1e-3f)
        e.tickEffects(0.6f)
        assertEquals(0f, e.curse)
    }

    @Test
    fun gravityPushesBackLessForBosses() {
        val normal = enemy(300f)
        attack(Die(DieType.GRAVITY, 2), normal)
        assertEquals(240f, normal.distance, 1e-3f)
        val boss = enemy(300f, 100_000f, EnemyKind.BOSS_REGEN)
        attack(Die(DieType.GRAVITY, 2), boss)
        assertEquals(285f, boss.distance, 1e-3f)
        val atStart = enemy(10f)
        attack(Die(DieType.GRAVITY, 7), atStart)
        assertEquals(0f, atStart.distance)
    }

    @Test
    fun timeStunsAndStunnedEnemiesDontMove() {
        val e = enemy(300f)
        attack(Die(DieType.TIME, 2), e)
        assertEquals(0.5f, e.stunTime, 1e-4f)
        assertEquals(0f, e.speed())
        e.tickEffects(0.6f)
        assertTrue(e.speed() > 0f)
        val boss = enemy(300f, 100_000f, EnemyKind.BOSS_SILENCER)
        attack(Die(DieType.TIME, 2), boss)
        assertEquals(0.25f, boss.stunTime, 1e-4f)
    }

    @Test
    fun gravityHasATotalPushBudgetPerEnemy() {
        val e = enemy(1000f)
        repeat(5) { attack(Die(DieType.GRAVITY, 7), e) }
        assertEquals(1000f - Balance.GRAVITY_MAX_PUSH, e.distance, 1e-3f)
        val boss = enemy(1000f, 1_000_000f, EnemyKind.BOSS_REGEN)
        repeat(20) { attack(Die(DieType.GRAVITY, 7), boss) }
        assertEquals(1000f - Balance.GRAVITY_MAX_PUSH * Balance.GRAVITY_BOSS_FACTOR, boss.distance, 1e-3f)
    }

    @Test
    fun stunsCantBeChained() {
        val e = enemy(300f)
        attack(Die(DieType.TIME, 2), e)
        e.tickEffects(0.3f)
        attack(Die(DieType.TIME, 7), e)
        assertEquals(0.2f, e.stunTime, 1e-4f, "a running stun is not refreshed")
        e.tickEffects(0.3f)
        attack(Die(DieType.TIME, 7), e)
        assertEquals(0f, e.stunTime, "immune right after a stun")
        e.tickEffects(Balance.STUN_IMMUNITY + 0.01f)
        attack(Die(DieType.TIME, 1), e)
        assertEquals(0.4f, e.stunTime, 1e-4f)
    }

    @Test
    fun quakeHitsEveryEnemy() {
        val enemies = listOf(enemy(100f), enemy(500f), enemy(1500f))
        val shot = assertNotNull(Combat.attack(Die(DieType.QUAKE, 1), origin, 1, enemies, path))
        enemies.forEach { assertEquals(1000f - 6f, it.hp, 1e-3f) }
        assertEquals(3, shot.damage.size)
    }

    @Test
    fun reaperExecutesOnlyCommonEnemies() {
        val low = enemy(300f, 1000f).apply { hp = 80f }
        attack(Die(DieType.REAPER, 1), low)
        assertFalse(low.alive, "65 hp left is under 7% of 1000")
        val healthy = enemy(300f, 1000f).apply { hp = 200f }
        attack(Die(DieType.REAPER, 1), healthy)
        assertEquals(185f, healthy.hp, 1e-3f)
        val boss = enemy(300f, 1000f, EnemyKind.BOSS_SWAPPER).apply { hp = 50f }
        attack(Die(DieType.REAPER, 1), boss)
        assertEquals(35f, boss.hp, 1e-3f)
    }

    @Test
    fun luckRollsBetweenZeroAndFourTimes() {
        val rng = Random(3)
        val rolls = List(2000) {
            val e = enemy(300f, 1_000_000f)
            attack(Die(DieType.LUCK, 1), e, rng = rng)
            1_000_000f - e.hp
        }
        assertTrue(rolls.all { it in 0f..40f })
        assertEquals(20f, rolls.average().toFloat(), 1f)
    }

    @Test
    fun critTriplesDamage() {
        val e = enemy(300f)
        val shot = assertNotNull(attack(Die(DieType.FIRE, 1), e, bonus = AttackBonus(critChance = 1f)))
        assertTrue(shot.crit)
        assertEquals(60f, 1000f - e.hp, 1e-3f)
        val plain = enemy(300f)
        assertFalse(assertNotNull(attack(Die(DieType.FIRE, 1), plain)).crit)
    }

    @Test
    fun comboScalesWithComboCountAndHunterWithStacks() {
        val e = enemy(300f)
        attack(Die(DieType.COMBO, 1), e, bonus = AttackBonus(comboCount = 3))
        assertEquals(12f * 1.6f, 1000f - e.hp, 1e-3f)
        val h = enemy(300f)
        attack(Die(DieType.HUNTER, 1).apply { stacks = 10 }, h)
        assertEquals(12f * 1.5f, 1000f - h.hp, 1e-3f)
    }

    // --- Board bonuses ---

    @Test
    fun lightSpeedsUpOnlyNeighbours() {
        val m = match()
        m.board[Cell(1, 1)] = Die(DieType.FIRE, 1)
        m.board[Cell(1, 2)] = Die(DieType.LIGHT, 2)
        m.board[Cell(0, 2)] = Die(DieType.LIGHT, 1)
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        assertEquals(1.3f, m.speedMultiplier(Cell(1, 1)), 1e-4f)
        assertEquals(1f, m.speedMultiplier(Cell(0, 0)))
        assertEquals(1.15f, m.speedMultiplier(Cell(1, 2)), 1e-4f)
        m.board[Cell(1, 2)]!!.disabledFor = 5f
        assertEquals(1f, m.speedMultiplier(Cell(1, 1)))
    }

    @Test
    fun critChanceSumsAndCaps() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.CRIT, 3)
        m.board[Cell(0, 1)] = Die(DieType.CRIT, 2)
        assertEquals(0.15f, m.attackBonus().critChance, 1e-4f)
        m.board.cells().forEach { m.board[it] = Die(DieType.CRIT, 7) }
        assertEquals(Balance.CRIT_MAX, m.attackBonus().critChance)
    }

    @Test
    fun comboCountsComboDice() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.COMBO, 1)
        m.board[Cell(2, 3)] = Die(DieType.COMBO, 4)
        m.board[Cell(1, 1)] = Die(DieType.FIRE, 1)
        assertEquals(2, m.attackBonus().comboCount)
    }

    // --- Kills ---

    @Test
    fun vampirePaysExtraSpPerPip() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.VAMPIRE, 3)
        m.debugAddEnemy(enemy(500f, 1f))
        m.update(Balance.STEP)
        assertEquals(150 + 10 + 3, m.sp)
    }

    @Test
    fun hunterStacksOnKillUpToTheCap() {
        val m = match()
        val hunter = Die(DieType.HUNTER, 1)
        m.board[Cell(1, 2)] = hunter
        m.debugAddEnemy(enemy(500f, 1f))
        m.update(Balance.STEP)
        assertEquals(1, hunter.stacks)
        hunter.stacks = Balance.HUNTER_MAX_STACKS
        hunter.cooldown = 0f
        m.debugAddEnemy(enemy(500f, 1f))
        m.update(Balance.STEP)
        assertEquals(Balance.HUNTER_MAX_STACKS, hunter.stacks)
    }

    @Test
    fun supernovaKillsChainExplosions() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.NOVA, 1)
        val a = enemy(900f, 10f)
        val b = enemy(880f, 5f)
        val c = enemy(800f, 5f)
        val d = enemy(600f, 5f)
        listOf(a, b, c, d).forEach { m.debugAddEnemy(it) }
        m.update(Balance.STEP)
        assertEquals(listOf(d), m.enemies)
        assertEquals(150 + 30, m.sp)
    }

    @Test
    fun trapIsPlantedAheadAndExplodesWhenReached() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.TRAP, 1)
        val e = enemy(500f)
        m.debugAddEnemy(e)
        m.update(Balance.STEP)
        assertEquals(1, m.traps.size)
        assertEquals(500f + Balance.TRAP_AHEAD, m.traps[0].distance, 5f)
        repeat(60 * 2) { m.update(Balance.STEP) }
        assertTrue(m.traps.isEmpty() || m.traps[0].distance > e.distance, "first trap should have gone off")
        assertTrue(e.hp <= 1000f - 30f, "hp ${e.hp}")
    }

    @Test
    fun trapsPerDieAreCapped() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.TRAP, 1)
        m.debugAddEnemy(enemy(500f).apply { stunTime = 100f })
        // Traps go down at 0, 2.5 and 5 s; the one due at 7.5 s is over the cap. Wave 1 (from 3 s) is still short of them.
        repeat(60 * 8) { m.update(Balance.STEP) }
        assertEquals(Balance.TRAP_MAX_PER_DIE, m.traps.size)
    }

    // --- Merge ---

    @Test
    fun hunterMergeKeepsStacksWhenResultIsHunter() {
        val board = Board()
        board[Cell(0, 0)] = Die(DieType.HUNTER, 2).apply { stacks = 7 }
        board[Cell(0, 1)] = Die(DieType.HUNTER, 2).apply { stacks = 5 }
        assertTrue(board.merge(Cell(0, 0), Cell(0, 1), listOf(DieType.HUNTER), Random(1)))
        assertEquals(12, board[Cell(0, 1)]!!.stacks)

        board[Cell(1, 0)] = Die(DieType.MIRROR, 3)
        board[Cell(1, 1)] = Die(DieType.HUNTER, 3).apply { stacks = 39 }
        board[Cell(1, 2)] = Die(DieType.HUNTER, 3).apply { stacks = 30 }
        assertTrue(board.merge(Cell(1, 0), Cell(1, 1), listOf(DieType.FIRE), Random(1)))
        assertEquals(39, board[Cell(1, 1)]!!.stacks)

        board[Cell(2, 0)] = Die(DieType.HUNTER, 1).apply { stacks = 30 }
        board[Cell(2, 1)] = Die(DieType.HUNTER, 1).apply { stacks = 30 }
        assertTrue(board.merge(Cell(2, 0), Cell(2, 1), listOf(DieType.HUNTER), Random(1)))
        assertEquals(Balance.HUNTER_MAX_STACKS, board[Cell(2, 1)]!!.stacks)

        board[Cell(2, 2)] = Die(DieType.HUNTER, 4).apply { stacks = 30 }
        board[Cell(2, 3)] = Die(DieType.HUNTER, 4).apply { stacks = 30 }
        assertTrue(board.merge(Cell(2, 2), Cell(2, 3), listOf(DieType.FIRE), Random(1)))
        assertEquals(0, board[Cell(2, 3)]!!.stacks)
    }
}
