package com.iagothz.mergetd.logic

import kotlin.random.Random

/** Everything that makes two matches play out the same given the same moves. */
data class MatchSetup(val map: GameMap, val mode: GameMode, val seed: Int, val deck: List<DieType>, val mutators: Set<Mutator> = emptySet()) {
    fun newMatch(dieLevels: Map<DieType, Int> = emptyMap()): Match =
        Match(deck, Random(seed), map, mode = mode, seed = seed, locked = true, mutators = mutators, dieLevels = dieLevels)
}

/**
 * A [MatchSetup] as 15 letters ("XXXXX-XXXXX-XXXXX"): a 7-letter head (mutators 4 bits, map 2, mode 2, five dice
 * 5 each), a 7-letter seed (32 bits), then a check letter. Crockford base32: no I, L, O or U, so it survives being
 * read out loud. Codes from before mutators (14 letters) still decode.
 */
object MatchCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val DIE_BITS = 5
    private const val HEAD_LETTERS = 7
    private const val SEED_LETTERS = 7
    private const val PAYLOAD_LETTERS = HEAD_LETTERS + SEED_LETTERS
    private const val LEGACY_PAYLOAD_LETTERS = 13

    fun encode(s: MatchSetup): String {
        require(s.deck.size == DeckCodec.SIZE)
        var head = Mutator.mask(s.mutators).toLong() and 0xF
        head = (head shl 2) or (s.map.ordinal.toLong() and 0x3)
        head = (head shl 2) or (s.mode.ordinal.toLong() and 0x3)
        for (d in s.deck) head = (head shl DIE_BITS) or d.ordinal.toLong()
        val letters = CharArray(PAYLOAD_LETTERS)
        for (i in 0 until HEAD_LETTERS) letters[i] = ALPHABET[((head shr (5 * (HEAD_LETTERS - 1 - i))) and 0x1F).toInt()]
        val seed = s.seed.toLong() and 0xFFFF_FFFFL
        for (i in 0 until SEED_LETTERS) letters[HEAD_LETTERS + i] = ALPHABET[((seed shr (5 * (SEED_LETTERS - 1 - i))) and 0x1F).toInt()]
        val raw = String(letters) + ALPHABET[check(letters)]
        return raw.chunked(5).joinToString("-")
    }

    /** Null for anything that isn't a code this version (or the previous one) wrote, including a single wrong letter. */
    fun decode(text: String): MatchSetup? {
        val clean = text.uppercase().filter { it != '-' && it != ' ' }
        if (clean.any { it !in ALPHABET }) return null
        if (clean.length == LEGACY_PAYLOAD_LETTERS + 1) return decodeLegacy(clean)
        if (clean.length != PAYLOAD_LETTERS + 1) return null
        val letters = clean.substring(0, PAYLOAD_LETTERS).toCharArray()
        if (ALPHABET.indexOf(clean.last()) != check(letters)) return null
        var head = 0L
        for (i in 0 until HEAD_LETTERS) head = (head shl 5) or ALPHABET.indexOf(letters[i]).toLong()
        var seedBits = 0L
        for (i in 0 until SEED_LETTERS) seedBits = (seedBits shl 5) or ALPHABET.indexOf(letters[HEAD_LETTERS + i]).toLong()
        val seed = (seedBits and 0xFFFF_FFFFL).toInt()
        return unpackHead(head, seed)
    }

    private fun unpackHead(packed: Long, seed: Int): MatchSetup? {
        var bits = packed
        val dice = ArrayList<DieType>(DeckCodec.SIZE)
        repeat(DeckCodec.SIZE) {
            dice += DieType.entries.getOrNull((bits and 0x1F).toInt()) ?: return null
            bits = bits ushr DIE_BITS
        }
        dice.reverse()
        val mode = GameMode.entries.getOrNull((bits and 0x3).toInt()) ?: return null
        bits = bits ushr 2
        val map = GameMap.entries.getOrNull((bits and 0x3).toInt()) ?: return null
        bits = bits ushr 2
        val mutators = Mutator.fromMask((bits and 0xF).toInt())
        if (dice.toSet().size != DeckCodec.SIZE) return null
        return MatchSetup(map, mode, seed, dice, mutators)
    }

    /** The 14-letter layout from before mutators: map, mode, dice and seed packed into one number. */
    private fun decodeLegacy(clean: String): MatchSetup? {
        val letters = clean.substring(0, LEGACY_PAYLOAD_LETTERS).toCharArray()
        if (ALPHABET.indexOf(clean.last()) != check(letters)) return null
        var bits = 0L
        for (c in letters) bits = (bits shl 5) or ALPHABET.indexOf(c).toLong()
        val seed = (bits and 0xFFFF_FFFFL).toInt()
        return unpackHead(bits ushr 32, seed)
    }

    /** Weighted sum of the letters, so swapped or mistyped letters change it. */
    private fun check(letters: CharArray): Int =
        letters.withIndex().sumOf { (i, c) -> (i + 1) * ALPHABET.indexOf(c) } % ALPHABET.length
}
