package com.iagothz.mergetd.logic

import kotlin.math.roundToInt

/**
 * Optional handicaps picked before a match. Each one multiplies the final score (wave × product), so a run with
 * mutators competes on its own scale. Titles and descriptions are Portuguese; the UI translates.
 */
enum class Mutator(val id: String, val title: String, val description: String, val multiplier: Float) {
    SWIFT("swift", "Velozes", "inimigos 30% mais rápidos", 1.3f),
    TOUGH("tough", "Resistentes", "inimigos com 50% mais vida", 1.5f),
    POOR("poor", "Pobres", "inimigos rendem metade do SP", 1.4f),
    SWARM("swarm", "Enxame", "40% mais inimigos por onda", 1.4f),
    ;

    companion object {
        fun byId(id: String): Mutator? = entries.firstOrNull { it.id == id }

        fun multiplier(mutators: Set<Mutator>): Float = mutators.fold(1f) { acc, m -> acc * m.multiplier }

        /** The wave reached, scaled by every active mutator. */
        fun score(wave: Int, mutators: Set<Mutator>): Int = (wave * multiplier(mutators)).roundToInt()

        /** Bit mask for codes and saves: bit i = entries[i]. */
        fun mask(mutators: Set<Mutator>): Int = mutators.fold(0) { acc, m -> acc or (1 shl m.ordinal) }

        fun fromMask(mask: Int): Set<Mutator> = entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
    }
}
