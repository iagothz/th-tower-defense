package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Cell
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Enemy
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
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
        fx.update(Effects.BANNER_TIME + 0.1f)
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
}
