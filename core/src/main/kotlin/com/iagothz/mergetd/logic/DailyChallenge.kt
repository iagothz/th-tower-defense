package com.iagothz.mergetd.logic

import kotlin.random.Random

/**
 * One fixed match per calendar day: the day picks the map, five dice out of all of them and the RNG seed,
 * so everyone who plays that day faces the same rolls (given the same moves).
 */
class DailyChallenge private constructor(val day: Int, val map: GameMap, val deck: List<DieType>) {
    val seed: Long get() = day * 1_000_003L

    /** dd/MM/yyyy of [day], counted in days since 1970-01-01 (UTC). */
    val dateLabel: String
        get() {
            val (y, m, d) = civil(day)
            return "%02d/%02d/%04d".format(d, m, y)
        }

    fun newMatch(): Match = Match(deck, Random(seed), map, day)

    companion object {
        fun of(day: Int): DailyChallenge {
            val rng = Random(day * 7919L + 17)
            return DailyChallenge(day, GameMap.entries.random(rng), DieType.entries.shuffled(rng).take(DeckCodec.SIZE))
        }

        fun today(): Int = (System.currentTimeMillis() / 86_400_000L).toInt()

        /** Days since the epoch to (year, month, day); Howard Hinnant's algorithm, so no java.time on old Android. */
        private fun civil(days: Int): Triple<Int, Int, Int> {
            val z = days + 719_468
            val era = Math.floorDiv(z, 146_097)
            val doe = z - era * 146_097
            val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            val y = yoe + era * 400 + if (m <= 2) 1 else 0
            return Triple(y, m, d)
        }
    }
}
