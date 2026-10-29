package com.iagothz.mergetd.logic

import kotlin.random.Random

/**
 * One fixed match per week (weeks since 1970-01-01, UTC): a map, five dice, two mutators and the seed, so
 * everyone faces the same run; the week's record is the mutator score.
 */
class WeeklyChallenge private constructor(val week: Int, val map: GameMap, val deck: List<DieType>, val mutators: Set<Mutator>) {
    val seed: Int get() = week * 1_000_033

    /** dd/MM/yyyy of the week's first day. */
    val dateLabel: String get() = DailyChallenge.of(week * 7).dateLabel

    fun newMatch(dieLevels: Map<DieType, Int> = emptyMap()): Match =
        Match(deck, Random(seed), map, mode = GameMode.NORMAL, seed = seed, locked = true, mutators = mutators, week = week, dieLevels = dieLevels)

    companion object {
        fun of(week: Int): WeeklyChallenge {
            val rng = Random(week * 104_729L + 29)
            return WeeklyChallenge(
                week,
                GameMap.entries.random(rng),
                DieType.entries.shuffled(rng).take(DeckCodec.SIZE),
                Mutator.entries.shuffled(rng).take(2).toSet(),
            )
        }

        fun weekOf(day: Int): Int = Math.floorDiv(day, 7)

        fun thisWeek(): Int = weekOf(DailyChallenge.today())
    }
}
