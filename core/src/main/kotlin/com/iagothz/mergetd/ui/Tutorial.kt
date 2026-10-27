package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.GameEvent

/** Three prompts on the first match, each cleared by doing the thing; then a goodbye that fades on its own. */
class Tutorial(active: Boolean = true) {
    /** Where the pulsing ring goes. */
    enum class Target { BUY, BOARD, POWER }

    enum class Step(val text: String, val target: Target?) {
        BUY("Toque em Comprar para colocar um dado no tabuleiro.", Target.BUY),
        MERGE("Arraste um dado sobre outro do mesmo tipo e com os mesmos pips para juntar os dois.", Target.BOARD),
        POWER("Gaste SP num power-up para fortalecer todos os dados daquele tipo.", Target.POWER),
        DONE("Pronto! Segure um dado para ver o que ele faz. Boa sorte.", null),
    }

    var step: Step? = if (active) Step.BUY else null
        private set
    private var goodbye = 0f

    /** True once the player has been through it (or skipped it). */
    val finished: Boolean get() = step == null

    fun consume(events: List<GameEvent>) {
        for (e in events) {
            val next = when {
                step == Step.BUY && e is GameEvent.DieBought -> Step.MERGE
                step == Step.MERGE && e is GameEvent.Merged -> Step.POWER
                step == Step.POWER && e is GameEvent.PoweredUp -> Step.DONE
                else -> continue
            }
            step = next
        }
    }

    fun update(dt: Float) {
        if (step != Step.DONE) return
        goodbye += dt
        if (goodbye >= GOODBYE_SECONDS) step = null
    }

    fun skip() {
        step = null
    }

    companion object {
        const val GOODBYE_SECONDS = 4f
    }
}
