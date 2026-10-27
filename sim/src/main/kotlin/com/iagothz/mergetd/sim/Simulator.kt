package com.iagothz.mergetd.sim

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameEvent
import com.iagothz.mergetd.logic.Match
import kotlin.random.Random

/** [wave] is the wave the game ended on (or the cap); [killedBy] is the kind of the last enemy that leaked. */
data class GameResult(val wave: Int, val killedBy: EnemyKind?, val deck: List<DieType>, val seed: Long)

object Simulator {
    const val MAX_WAVE = 100
    /** Game-time limit, so a stalemate (enemies held in place forever) can't hang a report. */
    const val MAX_SECONDS = 4 * 3600f
    private const val ACT_EVERY = 0.5f

    fun play(
        deck: List<DieType>,
        bot: (Long) -> Bot,
        seed: Long,
        maxWave: Int = MAX_WAVE,
        maxSeconds: Float = MAX_SECONDS,
    ): GameResult {
        val match = Match(deck, Random(seed))
        val player = bot(seed)
        val stepsPerAct = (ACT_EVERY / Balance.STEP).toInt()
        var killedBy: EnemyKind? = null
        var seconds = 0f
        while (!match.isGameOver && match.wave <= maxWave && seconds < maxSeconds) {
            seconds += ACT_EVERY
            player.act(match)
            repeat(stepsPerAct) { match.update(Balance.STEP) }
            for (e in match.drainEvents()) if (e is GameEvent.Leaked) killedBy = e.kind
        }
        return GameResult(minOf(match.wave, maxWave), if (match.isGameOver) killedBy else null, deck, seed)
    }
}

object Stats {
    /** Nearest-rank percentile of [values] (p in 0..100). */
    fun percentile(values: List<Int>, p: Int): Int {
        val sorted = values.sorted()
        val rank = maxOf(1, Math.ceil(p / 100.0 * sorted.size).toInt())
        return sorted[rank - 1]
    }

    /** Fraction of games that ended on or before [wave]. */
    fun deathRateBy(waves: List<Int>, wave: Int): Double = waves.count { it <= wave }.toDouble() / waves.size
}
