package com.iagothz.mergetd.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchEventsTest {
    private fun match() = Match(DeckCodec.DEFAULT, Random(7))

    private fun Match.advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.1f, left)
            update(dt)
            left -= dt
        }
    }

    @Test
    fun buyEmitsDieBoughtAtTheNewCell() {
        val m = match()
        m.buyDie()
        val cell = m.board.occupied().single().first
        assertEquals(listOf<GameEvent>(GameEvent.DieBought(cell)), m.drainEvents())
    }

    @Test
    fun failedActionsEmitNothing() {
        val m = match()
        m.sp = 0
        m.buyDie()
        m.powerUp(DieType.FIRE)
        m.merge(Cell(0, 0), Cell(0, 1))
        assertTrue(m.drainEvents().isEmpty())
    }

    @Test
    fun mergeEmitsMergedAtTarget() {
        val m = match()
        m.board[Cell(0, 0)] = Die(DieType.FIRE, 1)
        m.board[Cell(0, 1)] = Die(DieType.FIRE, 1)
        m.merge(Cell(0, 0), Cell(0, 1))
        assertEquals(listOf<GameEvent>(GameEvent.Merged(Cell(0, 1))), m.drainEvents())
    }

    @Test
    fun powerUpEmitsPoweredUp() {
        val m = match()
        m.powerUp(DieType.ICE)
        assertEquals(listOf<GameEvent>(GameEvent.PoweredUp(DieType.ICE)), m.drainEvents())
    }

    @Test
    fun drainClearsEvents() {
        val m = match()
        m.buyDie()
        m.drainEvents()
        assertTrue(m.drainEvents().isEmpty())
    }

    @Test
    fun eventsSurviveUpdateUntilDrained() {
        val m = match()
        m.buyDie()
        m.update(Balance.STEP)
        assertTrue(m.drainEvents().any { it is GameEvent.DieBought })
    }

    @Test
    fun killEmitsHitAndOneKilled() {
        val m = match()
        listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)).forEach { m.board[it] = Die(DieType.FIRE, 1) }
        val target = Enemy(EnemyKind.NORMAL, 1f).also { it.distance = 500f }
        m.debugAddEnemy(target)
        m.update(Balance.STEP)
        val events = m.drainEvents()
        val hits = events.filterIsInstance<GameEvent.Hit>()
        assertEquals(1, hits.size)
        assertTrue(hits[0].enemy === target)
        assertEquals(20f, hits[0].damage, 1e-3f)
        val kills = events.filterIsInstance<GameEvent.Killed>()
        assertEquals(listOf(GameEvent.Killed(EnemyKind.NORMAL, Layout.PATH.positionAt(target.distance))), kills)
    }

    @Test
    fun leakEmitsLeaked() {
        val m = match()
        m.debugAddEnemy(Enemy(EnemyKind.FAST, 100f).also { it.distance = Layout.PATH.length - 0.1f })
        m.update(Balance.STEP)
        assertEquals(listOf<GameEvent>(GameEvent.Leaked(EnemyKind.FAST)), m.drainEvents())
    }

    @Test
    fun waveStartEmitsWaveStarted() {
        val m = match()
        m.advance(3.1f)
        assertTrue(GameEvent.WaveStarted(1, boss = false) in m.drainEvents())
        m.debugStartWave(10)
        val started = m.drainEvents().single() as GameEvent.WaveStarted
        assertEquals(10, started.wave)
        assertTrue(started.boss)
        assertEquals(m.bossOrder[0], started.bossKind)
    }

    @Test
    fun supernovaKillEmitsBlast() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.NOVA, 1)
        val target = Enemy(EnemyKind.NORMAL, 1f).also { it.distance = 500f }
        m.debugAddEnemy(target)
        m.update(Balance.STEP)
        val blasts = m.drainEvents().filterIsInstance<GameEvent.Blast>()
        assertEquals(listOf(GameEvent.Blast(Layout.PATH.positionAt(target.distance), Balance.NOVA_RADIUS, DieType.NOVA)), blasts)
    }

    @Test
    fun trapExplosionEmitsBlast() {
        val m = match()
        m.board[Cell(1, 2)] = Die(DieType.TRAP, 1)
        m.debugAddEnemy(Enemy(EnemyKind.NORMAL, 1000f).also { it.distance = 500f })
        repeat(60 * 3) { m.update(Balance.STEP) }
        val blast = m.drainEvents().filterIsInstance<GameEvent.Blast>().first()
        assertEquals(DieType.TRAP, blast.type)
        assertEquals(Balance.TRAP_RADIUS, blast.radius)
    }

    @Test
    fun critsAreMarkedOnTheirHits() {
        val m = match()
        repeat(3) { m.board[Cell(0, it)] = Die(DieType.CRIT, 7) }
        m.debugAddEnemy(Enemy(EnemyKind.BOSS_REGEN, 1_000_000f).also { it.distance = 500f })
        var crits = 0
        var plain = 0
        repeat(120) {
            m.update(Balance.STEP)
            for (h in m.drainEvents().filterIsInstance<GameEvent.Hit>()) if (h.crit) crits++ else plain++
        }
        assertTrue(crits > 0 && plain > 0, "crits=$crits plain=$plain")
    }
}
