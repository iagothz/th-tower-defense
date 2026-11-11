package com.iagothz.mergetd.logic

import kotlin.random.Random

/** Which dice an account level has opened: the starters at level 1, then one more per level in [ORDER]. */
object DiceUnlocks {
    val STARTERS = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MINE, DieType.WIND, DieType.IRON, DieType.SNIPER)

    /** The other dice, simplest first: the i-th opens at level i + 2. */
    val ORDER = listOf(
        DieType.MIRROR, DieType.GROWTH, DieType.LIGHT, DieType.CRIT, DieType.COMBO, DieType.HUNTER, DieType.CURSE, DieType.GRAVITY,
        DieType.TIME, DieType.TRAP, DieType.QUAKE, DieType.LUCK, DieType.VAMPIRE, DieType.REAPER, DieType.NOVA, DieType.CLONE, DieType.BOOST,
    )

    fun unlockLevel(t: DieType): Int = if (t in STARTERS) 1 else ORDER.indexOf(t) + 2

    fun isUnlocked(t: DieType, level: Int): Boolean = unlockLevel(t) <= level

    /** In enum order, so the deck screen keeps its grid. */
    fun unlocked(level: Int): List<DieType> = DieType.entries.filter { isUnlocked(it, level) }

    /** The die that reaching exactly [level] opens, if any. */
    fun openedAt(level: Int): DieType? = ORDER.getOrNull(level - 2)

    /** [deck] with every locked die swapped, in place, for the first unlocked one it doesn't already hold. */
    fun sanitize(deck: List<DieType>, level: Int): List<DieType> {
        val out = deck.toMutableList()
        for (i in out.indices) {
            if (isUnlocked(out[i], level)) continue
            out[i] = unlocked(level).first { it !in out }
        }
        return out
    }
}

/** A die's permanent level, paid with its own cards; it multiplies the die like a power-up does. */
object DieLevels {
    const val MAX = 5
    const val MULT_PER_LEVEL = 0.1f

    /** Cards to go from level i+1 to level i+2. */
    val CARDS_TO_NEXT = intArrayOf(5, 10, 20, 40)

    fun mult(level: Int): Float = 1f + MULT_PER_LEVEL * (level - 1)

    /** Null at [MAX]. */
    fun cardsToNext(level: Int): Int? = CARDS_TO_NEXT.getOrNull(level - 1)
}

/** The chest a finished match opens: a few cards spread over the dice that can still grow. */
object CardDrop {
    const val PER_BOSS = 2

    fun count(wave: Int, bosses: Int): Int = maxOf(1, (wave + 1) / 2 + PER_BOSS * bosses)

    /** [n] cards, each on a uniformly drawn candidate; empty when there is nobody to give them to. */
    fun draw(candidates: List<DieType>, n: Int, rng: Random): Map<DieType, Int> {
        if (candidates.isEmpty() || n <= 0) return emptyMap()
        val out = sortedMapOf<DieType, Int>()
        repeat(n) {
            val t = candidates[rng.nextInt(candidates.size)]
            out[t] = (out[t] ?: 0) + 1
        }
        return out
    }
}
