package com.iagothz.mergetd.logic

import kotlin.random.Random

/** Everything that makes two matches play out the same given the same moves. */
data class MatchSetup(val map: GameMap, val mode: GameMode, val seed: Int, val deck: List<DieType>) {
    fun newMatch(): Match = Match(deck, Random(seed), map, mode = mode, seed = seed, locked = true)
}

/**
 * A [MatchSetup] as 14 letters ("XXXX-XXXX-XXXX-XX"): map (2 bits), mode (2), five dice (5 each), seed (32),
 * then a check letter. Crockford base32: no I, L, O or U, so it survives being read out loud.
 */
object MatchCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val DIE_BITS = 5
    private const val PAYLOAD_LETTERS = 13

    fun encode(s: MatchSetup): String {
        require(s.deck.size == DeckCodec.SIZE)
        var bits = s.map.ordinal.toLong() and 0x3
        bits = (bits shl 2) or (s.mode.ordinal.toLong() and 0x3)
        for (d in s.deck) bits = (bits shl DIE_BITS) or d.ordinal.toLong()
        bits = (bits shl 32) or (s.seed.toLong() and 0xFFFF_FFFFL)
        val letters = CharArray(PAYLOAD_LETTERS) { i ->
            ALPHABET[((bits shr (5 * (PAYLOAD_LETTERS - 1 - i))) and 0x1F).toInt()]
        }
        val raw = String(letters) + ALPHABET[check(letters)]
        return raw.chunked(4).joinToString("-")
    }

    /** Null for anything that isn't a code this version wrote, including a single wrong letter. */
    fun decode(text: String): MatchSetup? {
        val clean = text.uppercase().filter { it != '-' && it != ' ' }
        if (clean.length != PAYLOAD_LETTERS + 1 || clean.any { it !in ALPHABET }) return null
        val letters = clean.substring(0, PAYLOAD_LETTERS).toCharArray()
        if (ALPHABET.indexOf(clean.last()) != check(letters)) return null
        var bits = 0L
        for (c in letters) bits = (bits shl 5) or ALPHABET.indexOf(c).toLong()
        val seed = (bits and 0xFFFF_FFFFL).toInt()
        bits = bits ushr 32
        val dice = ArrayList<DieType>(DeckCodec.SIZE)
        repeat(DeckCodec.SIZE) {
            dice += DieType.entries.getOrNull((bits and 0x1F).toInt()) ?: return null
            bits = bits ushr DIE_BITS
        }
        dice.reverse()
        val mode = GameMode.entries.getOrNull((bits and 0x3).toInt()) ?: return null
        bits = bits ushr 2
        val map = GameMap.entries.getOrNull((bits and 0x3).toInt()) ?: return null
        if (dice.toSet().size != DeckCodec.SIZE) return null
        return MatchSetup(map, mode, seed, dice)
    }

    /** Weighted sum of the letters, so swapped or mistyped letters change it. */
    private fun check(letters: CharArray): Int =
        letters.withIndex().sumOf { (i, c) -> (i + 1) * ALPHABET.indexOf(c) } % ALPHABET.length
}
