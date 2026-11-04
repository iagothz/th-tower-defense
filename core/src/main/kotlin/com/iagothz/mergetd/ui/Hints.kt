package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Match

/**
 * One-line tips shown once ever, the first time each thing happens in a match: a wildcard bought,
 * a few buys without ever holding a die, a synergy lighting up, a die one merge from 7 pips.
 * [seen] is the persisted set; [onSeen] is called whenever it grows.
 */
class Hints(private val shown: MutableSet<String>, private val onSeen: (Set<String>) -> Unit = {}) {
    private val queue = ArrayDeque<String>()
    private var left = 0f
    private var buys = 0
    private var held = false

    /** Key of the hint on screen, or null. */
    var current: String? = null
        private set

    val seen: Set<String> get() = shown

    /** The hint's sentence in the current language. */
    val text: String? get() = current?.let { S[TEXTS.getValue(it)] }

    /** The player held a die to inspect it: no need to tell them about it. */
    fun noteHold() {
        held = true
    }

    fun update(dt: Float, events: List<GameEvent>, match: Match) {
        for (e in events) {
            when (e) {
                is GameEvent.DieBought -> {
                    buys++
                    val t = match.board[e.cell]?.type
                    if (t != null && (t.wildcard || t == DieType.CLONE)) want("wildcard")
                    if (buys >= HOLD_AFTER && !held) want("hold")
                }
                else -> Unit
            }
        }
        if (match.synergies().isNotEmpty()) want("synergy")
        if (match.board.occupied().any { it.second.pips == Balance.MAX_PIPS - 1 }) want("seven")
        if (current != null) {
            left -= dt
            if (left <= 0f) current = null
        }
        if (current == null) {
            queue.removeFirstOrNull()?.let {
                current = it
                left = SHOW_SECONDS
            }
        }
    }

    private fun want(key: String) {
        if (key in shown) return
        shown += key
        queue += key
        onSeen(shown)
    }

    companion object {
        const val SHOW_SECONDS = 5f
        /** Buys without a hold before the hold hint. */
        const val HOLD_AFTER = 3
        val TEXTS = linkedMapOf(
            "wildcard" to "Um coringa! Ele combina com qualquer dado de mesmos pips.",
            "hold" to "Segure um dado parado para ver o que ele faz e quanto bate.",
            "synergy" to "Sinergia ativa! Os dois tipos no tabuleiro ganham um bônus.",
            "seven" to "Mais um merge e esse dado chega a 7 pips, com uma habilidade nova.",
        )
    }
}
