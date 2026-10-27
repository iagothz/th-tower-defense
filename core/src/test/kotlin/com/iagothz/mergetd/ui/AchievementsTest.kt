package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementsTest {
    private fun progress(
        wave: Int = 0,
        mode: GameMode = GameMode.NORMAL,
        kills: Int = 0,
        bosses: Int = 0,
        merges: Int = 0,
        games: Int = 1,
        maxPips: Int = 1,
        synergy: Boolean = false,
        hunterStacks: Int = 0,
        daily: Boolean = false,
        undo: Boolean = false,
        rich: Boolean = false,
        mapsAtTen: Int = 0,
    ) = AchievementProgress(wave, mode, kills, bosses, merges, games, maxPips, synergy, hunterStacks, daily, undo, rich, mapsAtTen)

    @Test
    fun twentyAchievementsWithUniqueIdsAndRealCosmetics() {
        assertEquals(20, Achievement.entries.size)
        assertEquals(20, Achievement.entries.map { it.id }.toSet().size)
        for (a in Achievement.entries) assertTrue(a.title.isNotBlank() && a.description.isNotBlank(), "$a")
        val cosmetics = Achievement.entries.mapNotNull { it.unlocks }
        assertEquals(cosmetics.size, cosmetics.toSet().size, "each cosmetic is unlocked by one achievement")
        assertEquals(Cosmetic.entries.filter { it.unlockedByDefault.not() }.toSet(), cosmetics.toSet())
    }

    @Test
    fun nothingIsEarnedByAnEmptyMatch() {
        assertEquals(emptyList(), Achievements.earned(progress(games = 0)))
    }

    @Test
    fun wavesKillsAndBossesEarnTheirBadges() {
        val e = Achievements.earned(progress(wave = 30, kills = 1200, bosses = 3))
        assertTrue(Achievement.FIRST_BLOOD in e && Achievement.WAVE_10 in e && Achievement.WAVE_20 in e && Achievement.WAVE_30 in e)
        assertTrue(Achievement.WAVE_40 !in e)
        assertTrue(Achievement.BOSS_1 in e && Achievement.KILLS_1000 in e && Achievement.BOSSES_10 !in e)
    }

    @Test
    fun modesCountOnlyInTheirOwnMode() {
        assertTrue(Achievement.BOSS_RUSH_5 in Achievements.earned(progress(wave = 5, mode = GameMode.BOSS_RUSH)))
        assertTrue(Achievement.BOSS_RUSH_5 !in Achievements.earned(progress(wave = 5)))
        assertTrue(Achievement.SURVIVOR in Achievements.earned(progress(wave = 15, mode = GameMode.SURVIVAL)))
        assertTrue(Achievement.SPEEDSTER in Achievements.earned(progress(wave = 15, mode = GameMode.RUSH)))
    }

    @Test
    fun flagsFromTheMatchEarnTheRest() {
        val e = Achievements.earned(progress(maxPips = 7, synergy = true, hunterStacks = 50, daily = true, undo = true, rich = true, mapsAtTen = GameMap.entries.size, games = 10, merges = 100))
        for (a in listOf(Achievement.SEVEN_PIPS, Achievement.SYNERGY, Achievement.HUNTER_50, Achievement.DAILY, Achievement.UNDO, Achievement.RICH, Achievement.EXPLORER, Achievement.VETERAN, Achievement.MERGES_100)) {
            assertTrue(a in e, "$a")
        }
    }

    @Test
    fun saveRemembersUnlocksAndEquippedCosmetics() {
        val save = SaveData(SaveDataTest.memoryPrefs())
        assertEquals(setOf(Achievement.FIRST_BLOOD), save.unlock(listOf(Achievement.FIRST_BLOOD)))
        assertEquals(emptySet(), save.unlock(listOf(Achievement.FIRST_BLOOD)), "already had it")
        assertTrue(save.hasAchievement(Achievement.FIRST_BLOOD))
        assertTrue(save.hasCosmetic(Cosmetic.FRAME_BRONZE))
        assertTrue(!save.hasCosmetic(Cosmetic.FRAME_GOLD))
        assertEquals(Cosmetic.FRAME_DEFAULT, save.equipped(Cosmetic.Slot.FRAME))
        save.equip(Cosmetic.FRAME_BRONZE)
        assertEquals(Cosmetic.FRAME_BRONZE, save.equipped(Cosmetic.Slot.FRAME))
    }
}
