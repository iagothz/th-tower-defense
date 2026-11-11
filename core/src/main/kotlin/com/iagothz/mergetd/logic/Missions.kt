package com.iagothz.mergetd.logic

import kotlin.random.Random

enum class MissionKind { KILLS_WITH, REACH_WAVE, MERGES, MAX_PIPS, BOSSES, PLAY_MODE }

/**
 * One of the day's three goals. [target] is the count to reach; counters add up across the day's matches,
 * wave goals take the best wave reached on the right map or mode.
 */
data class Mission(val kind: MissionKind, val target: Int, val xp: Int, val type: DieType? = null, val map: GameMap? = null, val mode: GameMode? = null) {
    /** Portuguese sentence with a %d for the target (except the fixed 7-pip one) and, when it applies, a %s for the die, map or mode. */
    val template: String
        get() = when (kind) {
            MissionKind.KILLS_WITH -> "Mate %d inimigos com %s"
            MissionKind.REACH_WAVE -> "Chegue à onda %d no mapa %s"
            MissionKind.MERGES -> "Faça %d merges"
            MissionKind.MAX_PIPS -> "Leve um dado a 7 pips"
            MissionKind.BOSSES -> "Derrote %d chefes"
            MissionKind.PLAY_MODE -> "Chegue à onda %d no modo %s"
        }

    /** Whether progress is a running count (true) or the best wave reached (false). */
    val counts: Boolean get() = kind != MissionKind.REACH_WAVE && kind != MissionKind.PLAY_MODE
}

/** How far one match moved a mission: [delta] for counters, [absolute] (the wave reached) for wave goals. */
data class MissionProgress(val delta: Int = 0, val absolute: Int = 0)

/** Three missions per day, the same for everyone, drawn from the day number. */
object DailyMissions {
    const val COUNT = 3

    fun of(day: Int): List<Mission> {
        val rng = Random(day * 7919L + 13)
        val kinds = MissionKind.entries.shuffled(rng).take(COUNT)
        return kinds.map { kind ->
            when (kind) {
                MissionKind.KILLS_WITH -> Mission(kind, 30 + 10 * rng.nextInt(4), 60, type = KILLERS.random(rng))
                MissionKind.REACH_WAVE -> Mission(kind, 8 + 2 * rng.nextInt(4), 80, map = GameMap.entries.random(rng))
                MissionKind.MERGES -> Mission(kind, 20 + 10 * rng.nextInt(4), 50)
                MissionKind.MAX_PIPS -> Mission(kind, 1, 90)
                MissionKind.BOSSES -> Mission(kind, 1 + rng.nextInt(3), 70)
                MissionKind.PLAY_MODE -> Mission(kind, 5 + rng.nextInt(4), 70, mode = GameMode.entries.filter { it != GameMode.NORMAL }.random(rng))
            }
        }
    }

    /** Dice a kill can be credited to: attackers that aren't wildcards or the Clone. */
    private val KILLERS = DieType.entries.filter { it.attacks && !it.wildcard && it != DieType.CLONE }
}

/** Account level from accumulated XP: every match pays by wave, missions pay on completion. */
object Progression {
    const val XP_PER_WAVE = 10
    const val XP_PER_BOSS = 20

    fun xpForMatch(wave: Int, bosses: Int): Int = wave * XP_PER_WAVE + bosses * XP_PER_BOSS

    fun costToNext(level: Int): Int = 150 + 50 * (level - 1)

    fun level(xp: Int): Int {
        var level = 1
        var left = xp
        while (left >= costToNext(level)) {
            left -= costToNext(level)
            level++
        }
        return level
    }

    /** XP gathered inside the current level, for the bar. */
    fun intoLevel(xp: Int): Int {
        var level = 1
        var left = xp
        while (left >= costToNext(level)) {
            left -= costToNext(level)
            level++
        }
        return left
    }

    /** Level → title, Portuguese; the UI translates. */
    val TITLES = listOf(1 to "Recruta", 3 to "Soldado", 5 to "Veterano", 8 to "Capitão", 12 to "Comandante", 16 to "General", 20 to "Lenda")

    fun title(level: Int): String = TITLES.last { it.first <= level }.second
}
