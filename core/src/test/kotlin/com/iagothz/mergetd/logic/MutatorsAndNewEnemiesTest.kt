package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutatorsAndNewEnemiesTest {
    private fun Match.advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.1f, left)
            update(dt)
            left -= dt
        }
    }

    // --- Elite and Treasure in the waves ---

    @Test
    fun elitesLeadEveryThirdWaveFromWaveSix() {
        assertFalse(EnemyKind.ELITE in WaveSpawner.composition(4))
        assertEquals(EnemyKind.ELITE, WaveSpawner.composition(6).first())
        assertEquals(1, WaveSpawner.composition(6).count { it == EnemyKind.ELITE })
        assertFalse(EnemyKind.ELITE in WaveSpawner.composition(7))
        assertEquals(EnemyKind.ELITE, WaveSpawner.composition(9).first())
        assertFalse(EnemyKind.ELITE in WaveSpawner.composition(10), "never on a boss wave")
        assertFalse(EnemyKind.ELITE in WaveSpawner.composition(15), "never on a special wave")
    }

    @Test
    fun aTreasureRunsAtTheBackEveryFourthWaveFromWaveSix() {
        assertFalse(EnemyKind.TREASURE in WaveSpawner.composition(2))
        assertEquals(EnemyKind.TREASURE, WaveSpawner.composition(6).last())
        assertEquals(1, WaveSpawner.composition(6).count { it == EnemyKind.TREASURE })
        assertFalse(EnemyKind.TREASURE in WaveSpawner.composition(7))
        assertEquals(EnemyKind.TREASURE, WaveSpawner.composition(14).last())
        assertEquals(0, EnemyKind.TREASURE.lifeDamage)
        assertTrue(EnemyKind.TREASURE.reward >= 5 * EnemyKind.NORMAL.reward)
    }

    @Test
    fun swarmAddsFortyPercentMoreOnOrdinaryWaves() {
        val plain = WaveSpawner.composition(7)
        val swarm = WaveSpawner.composition(7, swarm = true)
        assertEquals(plain.size + plain.size * 2 / 5, swarm.size)
        assertEquals(WaveSpawner.composition(10), WaveSpawner.composition(10, swarm = true), "bosses come alone")
        assertEquals(swarm.groupingBy { it }.eachCount(), WaveSpawner.preview(7, swarm = true))
    }

    @Test
    fun treasureEscapesWithoutCostingALife() {
        val m = Match(DeckCodec.DEFAULT, Random(1))
        val t = m.devSpawn(EnemyKind.TREASURE)
        t.distance = m.path.length - 0.5f
        m.drainEvents()
        m.update(0.5f)
        assertEquals(Balance.START_LIVES, m.lives)
        assertTrue(m.drainEvents().none { it is GameEvent.Leaked }, "no red flash for a treasure that got away")
        assertTrue(m.enemies.isEmpty())
    }

    @Test
    fun eliteHealsItsNeighbours() {
        val m = Match(DeckCodec.DEFAULT, Random(1))
        m.devSpawn(EnemyKind.ELITE)
        val hurt = m.devSpawn(EnemyKind.NORMAL)
        hurt.hp = hurt.maxHp * 0.5f
        m.update(1f)
        assertTrue(hurt.hp > hurt.maxHp * 0.5f, "${hurt.hp} of ${hurt.maxHp}")
    }

    // --- Mutators ---

    @Test
    fun scoreMultipliesTheWaveByEveryMutator() {
        assertEquals(10, Mutator.score(10, emptySet()))
        assertEquals(13, Mutator.score(10, setOf(Mutator.SWIFT)))
        assertEquals(Math.round(10 * Mutator.SWIFT.multiplier * Mutator.TOUGH.multiplier), Mutator.score(10, setOf(Mutator.SWIFT, Mutator.TOUGH)))
        assertTrue(Mutator.entries.all { it.multiplier > 1f })
        assertEquals(Mutator.SWIFT, Mutator.byId("swift"))
        assertNull(Mutator.byId("nope"))
    }

    @Test
    fun toughSwiftAndPoorChangeTheEnemies() {
        fun first(mutators: Set<Mutator>): Pair<Match, Enemy> {
            val m = Match(DeckCodec.DEFAULT, Random(1), mutators = mutators)
            return m to m.devSpawn(EnemyKind.NORMAL)
        }
        val (_, plain) = first(emptySet())
        val (_, tough) = first(setOf(Mutator.TOUGH))
        assertEquals(plain.maxHp * Balance.TOUGH_HP, tough.maxHp, 0.01f)
        val (_, swift) = first(setOf(Mutator.SWIFT))
        assertEquals(Balance.SWIFT_SPEED, swift.speedScale)

        val poor = Match(DeckCodec.DEFAULT, Random(1), mutators = setOf(Mutator.POOR))
        poor.devSpawn(EnemyKind.NORMAL)
        val before = poor.sp
        poor.devKillAll()
        poor.update(0.1f)
        assertEquals(before + Math.round(EnemyKind.NORMAL.reward * Balance.POOR_REWARD), poor.sp)
    }

    @Test
    fun swarmMatchesSpawnBiggerWaves() {
        val m = Match(DeckCodec.DEFAULT, Random(1), mutators = setOf(Mutator.SWARM))
        m.devNextWave()
        assertEquals(WaveSpawner.composition(1, swarm = true).groupingBy { it }.eachCount(), m.previewOfCurrentWave())
    }

    @Test
    fun mutatorsSurviveTheSnapshotAndTheCode() {
        val setup = MatchSetup(GameMap.RING, GameMode.RUSH, 77, DeckCodec.DEFAULT, setOf(Mutator.TOUGH, Mutator.SWARM))
        assertEquals(setup, MatchCode.decode(MatchCode.encode(setup)))
        // A code the previous version wrote (14 letters): Horseshoe, Normal, the default deck, no mutators.
        val legacy = MatchCode.decode("0004-8CV8-Q49R-VP")!!
        assertEquals(GameMap.DEFAULT, legacy.map)
        assertEquals(GameMode.NORMAL, legacy.mode)
        assertEquals(DeckCodec.DEFAULT, legacy.deck)
        assertTrue(legacy.mutators.isEmpty())
        val plain = MatchSetup(GameMap.RING, GameMode.RUSH, 77, DeckCodec.DEFAULT)
        assertEquals(plain, MatchCode.decode(MatchCode.encode(plain)))
        assertNotEquals(MatchCode.encode(plain), MatchCode.encode(setup))

        val m = setup.newMatch()
        val back = Match.restore(m.snapshot(), Random(1))!!
        assertEquals(setup.mutators, back.mutators)
        assertEquals(setup, back.setup)
    }

    // --- Weekly challenge ---

    @Test
    fun weeklyChallengeIsFixedPerWeekWithTwoMutators() {
        val a = WeeklyChallenge.of(2_900)
        assertEquals(a.map, WeeklyChallenge.of(2_900).map)
        assertEquals(a.deck, WeeklyChallenge.of(2_900).deck)
        assertEquals(2, a.mutators.size)
        assertEquals(DeckCodec.SIZE, a.deck.toSet().size)
        assertNotEquals(a.deck, WeeklyChallenge.of(2_901).deck)
        val m = a.newMatch()
        assertEquals(a.mutators, m.mutators)
        assertEquals(2_900, m.week)
        assertNull(m.challengeDay)
        assertEquals(2_900, WeeklyChallenge.weekOf(2_900 * 7 + 3))
    }
}
