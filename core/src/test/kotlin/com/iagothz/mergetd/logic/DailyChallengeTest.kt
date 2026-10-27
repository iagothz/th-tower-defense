package com.iagothz.mergetd.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DailyChallengeTest {
    @Test
    fun theSameDayAlwaysGivesTheSameChallenge() {
        val a = DailyChallenge.of(20_750)
        val b = DailyChallenge.of(20_750)
        assertEquals(a.map, b.map)
        assertEquals(a.deck, b.deck)
        assertEquals(a.seed, b.seed)
        assertEquals(DeckCodec.SIZE, a.deck.toSet().size)
    }

    @Test
    fun differentDaysDiffer() {
        val days = (20_750 until 20_760).map { DailyChallenge.of(it) }
        assertTrue(days.map { it.deck }.toSet().size > 5)
        assertTrue(days.map { it.map }.toSet().size > 1)
        assertNotEquals(days[0].seed, days[1].seed)
    }

    @Test
    fun labelShowsTheDate() {
        // Day 20 750 since the epoch is 2026-10-24.
        assertEquals("24/10/2026", DailyChallenge.of(20_750).dateLabel)
        assertTrue(DailyChallenge.today() >= 20_750)
    }

    @Test
    fun aChallengeMatchRemembersItsDay() {
        val c = DailyChallenge.of(20_750)
        val m = c.newMatch()
        assertEquals(c.map, m.map)
        assertEquals(c.deck, m.deck)
        assertEquals(20_750, m.challengeDay)
        val restored = Match.restore(m.snapshot())!!
        assertEquals(20_750, restored.challengeDay)
        assertEquals(null, Match(DeckCodec.DEFAULT).challengeDay)
    }
}
