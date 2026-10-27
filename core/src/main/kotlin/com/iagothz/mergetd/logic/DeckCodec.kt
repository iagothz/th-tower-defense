package com.iagothz.mergetd.logic

object DeckCodec {
    const val SIZE = 5
    val DEFAULT = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.MINE)

    fun encode(deck: List<DieType>): String = deck.joinToString(",") { it.id }

    /** Five distinct types, any of them. */
    fun random(rng: kotlin.random.Random): List<DieType> = DieType.entries.shuffled(rng).take(SIZE)

    /** Returns [DEFAULT] for anything that is not exactly [SIZE] distinct known ids. */
    fun decode(s: String?): List<DieType> {
        if (s.isNullOrBlank()) return DEFAULT
        val types = s.split(",").map { DieType.byId(it.trim()) ?: return DEFAULT }
        return if (types.size == SIZE && types.toSet().size == SIZE) types else DEFAULT
    }
}
