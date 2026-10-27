package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.Layout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherTest {
    @Test
    fun eachMapHasItsOwnWeather() {
        assertEquals(GameMap.entries.size, GameMap.entries.map { Weather.kindFor(it) }.toSet().size)
        assertEquals(Weather.Kind.SNOW, Weather.kindFor(GameMap.RING))
        assertEquals(Weather.Kind.EMBERS, Weather.kindFor(GameMap.STRAIGHT))
    }

    @Test
    fun particlesKeepMovingAndStayInTheSky() {
        for (map in GameMap.entries) {
            val w = Weather(map, Random(3))
            val before = w.particles.map { it.x to it.y }
            repeat(600) { w.update(1f / 60f) }
            assertEquals(Weather.COUNT, w.particles.size, map.id)
            assertTrue(w.particles.zip(before).any { (p, b) -> p.x != b.first || p.y != b.second }, "${map.id} never moved")
            for (p in w.particles) {
                assertTrue(p.x >= -Weather.MARGIN && p.x <= Layout.WORLD_W + Weather.MARGIN, "${map.id} x=${p.x}")
                assertTrue(p.y >= -Weather.MARGIN && p.y <= Layout.WORLD_H + Weather.MARGIN, "${map.id} y=${p.y}")
            }
        }
    }
}
