package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Layout
import com.iagothz.mergetd.logic.SpecialWave
import com.iagothz.mergetd.logic.Vec2
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectsTest {
    private val fx = Effects(Random(1))
    private val enemy = Enemy(EnemyKind.NORMAL, 100f).also { it.distance = 300f }

    @Test
    fun hitsOnSameEnemyAggregateIntoOneNumber() {
        fx.consume(listOf(GameEvent.Hit(enemy, 10f), GameEvent.Hit(enemy, 5.4f)))
        assertEquals(listOf("15"), fx.texts.map { it.text })
    }

    @Test
    fun hitFlashesEnemyBriefly() {
        fx.consume(listOf(GameEvent.Hit(enemy, 1f)))
        assertTrue(fx.isFlashing(enemy))
        fx.update(0.1f)
        assertFalse(fx.isFlashing(enemy))
    }

    @Test
    fun numbersRiseAndExpire() {
        fx.consume(listOf(GameEvent.Hit(enemy, 7f)))
        val y0 = fx.texts.single().y
        fx.update(0.3f)
        assertTrue(fx.texts.single().y > y0)
        fx.update(0.4f)
        assertTrue(fx.texts.isEmpty())
    }

    @Test
    fun killSpawnsParticlesThatExpire() {
        fx.consume(listOf(GameEvent.Killed(EnemyKind.TANK, Vec2(100f, 100f))))
        assertEquals(Effects.PARTICLES_PER_KILL, fx.particles.size)
        fx.update(0.5f)
        assertTrue(fx.particles.isEmpty())
    }

    @Test
    fun boughtAndMergedDicePop() {
        val cell = Cell(1, 2)
        assertEquals(1f, fx.popScale(cell))
        fx.consume(listOf(GameEvent.Merged(cell)))
        assertTrue(fx.popScale(cell) < 1f)
        fx.update(0.3f)
        assertEquals(1f, fx.popScale(cell))
    }

    @Test
    fun leakShakesThenSettles() {
        assertEquals(Vec2(0f, 0f), fx.shakeOffset())
        fx.consume(listOf(GameEvent.Leaked(EnemyKind.NORMAL)))
        fx.update(0.01f)
        val o = fx.shakeOffset()
        assertTrue(sqrt(o.x * o.x + o.y * o.y) <= Effects.LEAK_SHAKE * sqrt(2f))
        fx.update(0.3f)
        assertEquals(Vec2(0f, 0f), fx.shakeOffset())
    }

    @Test
    fun waveBannerShowsThenHides() {
        fx.consume(listOf(GameEvent.WaveStarted(3, boss = false)))
        assertEquals("Onda 3", fx.banner)
        fx.consume(listOf(GameEvent.WaveStarted(10, boss = true)))
        assertEquals("Chefe!", fx.banner)
        fx.update(Effects.BOSS_BANNER_TIME + 0.1f)
        assertNull(fx.banner)
    }

    @Test
    fun critNumbersAreMarked() {
        fx.consume(listOf(GameEvent.Hit(enemy, 30f, crit = true)))
        assertTrue(fx.texts.single().crit)
        assertEquals("30", fx.texts.single().text)
    }

    @Test
    fun killShowsTheReward() {
        fx.consume(listOf(GameEvent.Killed(EnemyKind.TANK, Vec2(100f, 100f))))
        assertEquals(listOf("+${EnemyKind.TANK.reward}"), fx.texts.map { it.text })
        assertTrue(fx.texts.single().reward)
    }

    @Test
    fun mergeSparkles() {
        fx.consume(listOf(GameEvent.Merged(Cell(0, 0))))
        assertTrue(fx.particles.isNotEmpty())
    }

    @Test
    fun blastRingsExpandThenFade() {
        fx.consume(listOf(GameEvent.Blast(Vec2(50f, 50f), 90f, DieType.NOVA)))
        val r0 = fx.rings.single().radius
        fx.update(0.1f)
        assertTrue(fx.rings.single().radius > r0)
        fx.update(Effects.RING_TIME)
        assertTrue(fx.rings.isEmpty())
    }

    @Test
    fun specialWavesGetTheirOwnBanner() {
        fx.consume(listOf(GameEvent.WaveStarted(5, boss = false, special = SpecialWave.HORDE)))
        assertEquals("Horda!", fx.banner)
    }

    @Test
    fun stolenSpShowsAsARedNumber() {
        fx.consume(listOf(GameEvent.Stolen(150)))
        assertEquals(listOf("-150 SP"), fx.texts.map { it.text })
    }

    @Test
    fun boughtDiceDropInFromAbove() {
        val cell = Cell(1, 1)
        assertEquals(0f, fx.dropOffset(cell))
        fx.consume(listOf(GameEvent.DieBought(cell)))
        assertTrue(fx.dropOffset(cell) > 30f)
        fx.update(Effects.DROP_TIME + 0.01f)
        assertEquals(0f, fx.dropOffset(cell))
    }

    @Test
    fun mergedDieSlidesToItsTargetThenTheResultAppears() {
        val from = Cell(0, 0)
        val to = Cell(0, 1)
        fx.slide(from, to, DieType.FIRE, 2)
        assertTrue(fx.slidingTo(to))
        assertEquals(1, fx.slides.size)
        fx.update(Effects.SLIDE_TIME / 2)
        val mid = fx.slides.single()
        assertTrue(mid.x > Layout.cellCenter(from).x && mid.x < Layout.cellCenter(to).x, "x=${mid.x}")
        fx.update(Effects.SLIDE_TIME)
        assertFalse(fx.slidingTo(to))
        assertTrue(fx.slides.isEmpty())
    }

    @Test
    fun bossWaveShowsTheBossNameAndWhatItDoes() {
        fx.consume(listOf(GameEvent.WaveStarted(10, boss = true, bossKind = EnemyKind.BOSS_THIEF)))
        assertEquals("Chefe: Ladrão", fx.banner)
        assertEquals(Skin.bossAbility(EnemyKind.BOSS_THIEF), fx.bannerNote)
        fx.update(Effects.BOSS_BANNER_TIME + 0.1f)
        assertNull(fx.banner)
        assertNull(fx.bannerNote)
    }

    @Test
    fun killedEnemiesLeaveAShrinkingBody() {
        fx.consume(listOf(GameEvent.Killed(EnemyKind.TANK, Vec2(100f, 100f))))
        assertEquals(1, fx.dying.size)
        val r0 = fx.dying.single().scale
        fx.update(Effects.DYING_TIME / 2)
        assertTrue(fx.dying.single().scale < r0)
        fx.update(Effects.DYING_TIME)
        assertTrue(fx.dying.isEmpty())
    }

    @Test
    fun unmergePopsBothCells() {
        fx.consume(listOf(GameEvent.Unmerged(Cell(0, 0), Cell(0, 1))))
        assertTrue(fx.popScale(Cell(0, 0)) < 1f)
        assertTrue(fx.popScale(Cell(0, 1)) < 1f)
    }

    @Test
    fun announcementsUseTheBanner() {
        fx.announce("Desafio do dia")
        assertEquals("Desafio do dia", fx.banner)
    }

    @Test
    fun mergeFreezesTheSimulationForAnInstant() {
        assertEquals(1f, fx.timeScale())
        fx.consume(listOf(GameEvent.Merged(Cell(0, 0), pips = 3)))
        assertEquals(0f, fx.timeScale())
        fx.update(Effects.HIT_STOP + 0.01f)
        assertEquals(1f, fx.timeScale())
    }

    @Test
    fun mergedCellFlashesWhiteThenFades() {
        val cell = Cell(1, 1)
        assertEquals(0f, fx.cellFlash(cell))
        fx.consume(listOf(GameEvent.Merged(cell, pips = 2)))
        assertEquals(1f, fx.cellFlash(cell))
        fx.update(Effects.CELL_FLASH_TIME / 2)
        assertTrue(fx.cellFlash(cell) in 0.01f..0.99f)
        fx.update(Effects.CELL_FLASH_TIME)
        assertEquals(0f, fx.cellFlash(cell))
    }

    @Test
    fun landingDiceSquashWideThenSettle() {
        val cell = Cell(2, 2)
        assertEquals(Vec2(1f, 1f), fx.dieScale(cell))
        fx.consume(listOf(GameEvent.DieBought(cell)))
        fx.update(Effects.DROP_TIME + 0.02f)
        val s = fx.dieScale(cell)
        assertTrue(s.x > 1f && s.y < 1f, "squash $s")
        fx.update(Effects.SQUASH_TIME)
        assertEquals(Vec2(1f, 1f), fx.dieScale(cell))
    }

    @Test
    fun boughtDiceBounceOnLanding() {
        val cell = Cell(1, 1)
        fx.consume(listOf(GameEvent.DieBought(cell)))
        var minSeen = Float.MAX_VALUE
        var rose = false
        repeat(25) {
            fx.update(Effects.DROP_TIME / 25)
            val o = fx.dropOffset(cell)
            if (o > minSeen + 0.5f) rose = true
            minSeen = minOf(minSeen, o)
        }
        assertTrue(rose, "the die should bounce back up once")
        fx.update(0.01f)
        assertEquals(0f, fx.dropOffset(cell))
    }

    @Test
    fun sevenPipsCelebrateWithShakeAndMoreSparkles() {
        fx.consume(listOf(GameEvent.Merged(Cell(0, 0), pips = 7)))
        assertTrue(fx.particles.size > Effects.PARTICLES_PER_MERGE)
        assertTrue(fx.rings.isNotEmpty())
        fx.update(0.01f)
        assertTrue(fx.shakeOffset() != Vec2(0f, 0f))
    }

    @Test
    fun firingDiceKickBackThenReturn() {
        val cell = Cell(0, 0)
        assertEquals(Vec2(0f, 0f), fx.kickOffset(cell))
        fx.kick(cell, Vec2(1f, 0f))
        val k = fx.kickOffset(cell)
        assertTrue(k.x < 0f && k.y == 0f, "kick $k")
        fx.update(Effects.KICK_TIME + 0.01f)
        assertEquals(Vec2(0f, 0f), fx.kickOffset(cell))
    }

    @Test
    fun heavyHitsJoltTheEnemyBackwards() {
        fx.consume(listOf(GameEvent.Hit(enemy, 5f, type = DieType.FIRE)))
        assertEquals(0f, fx.jolt(enemy))
        fx.consume(listOf(GameEvent.Hit(enemy, 5f, type = DieType.IRON)))
        assertTrue(fx.jolt(enemy) > 0f)
        fx.update(Effects.JOLT_TIME + 0.01f)
        assertEquals(0f, fx.jolt(enemy))
        fx.consume(listOf(GameEvent.Hit(enemy, 5f, crit = true)))
        assertTrue(fx.jolt(enemy) > 0f)
    }

    @Test
    fun fireBlastsLeaveEmbersThatOutliveSparks() {
        fx.consume(listOf(GameEvent.Blast(Vec2(50f, 50f), 90f, DieType.FIRE)))
        assertTrue(fx.particles.isNotEmpty())
        fx.update(Effects.PARTICLE_TIME + 0.01f)
        assertTrue(fx.particles.isNotEmpty(), "embers last longer than sparks")
        fx.update(Effects.EMBER_TIME)
        assertTrue(fx.particles.isEmpty())
    }

    @Test
    fun bossDeathGoesSlowMotionWithABigBurst() {
        fx.consume(listOf(GameEvent.Killed(EnemyKind.BOSS_REGEN, Vec2(100f, 100f))))
        assertTrue(fx.particles.size > Effects.PARTICLES_PER_KILL * 2)
        assertTrue(fx.timeScale() < 1f && fx.timeScale() > 0f, "slow motion: ${fx.timeScale()}")
        fx.update(Effects.SLOWMO_TIME + 0.01f)
        assertEquals(1f, fx.timeScale())
    }

    @Test
    fun critNumbersPopInBig() {
        fx.consume(listOf(GameEvent.Hit(enemy, 30f, crit = true)))
        val t = fx.texts.single()
        assertTrue(t.scale > 1.2f)
        fx.update(Effects.TEXT_POP_TIME + 0.01f)
        assertEquals(1f, t.scale)
    }
}
