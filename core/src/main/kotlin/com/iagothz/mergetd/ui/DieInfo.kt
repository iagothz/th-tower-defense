package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Combat
import com.iagothz.mergetd.logic.DieType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/** Player-facing description of a die type. Every number is read from [Balance], so it can't drift. */
data class DieInfo(val summary: String, val rows: List<Pair<String, String>>) {
    companion object {
        fun of(t: DieType): DieInfo {
            val rows = mutableListOf("Alvo" to target(t))
            if (t.attacks) {
                val dmg = Balance.stats(t).baseDamage
                rows += "Dano" to "${num(dmg)} por pip (${num(dmg)} com 1 pip, ${num(dmg * Balance.MAX_PIPS)} com ${Balance.MAX_PIPS})"
                rows += "Velocidade" to "a cada ${num(Combat.attackInterval(t, 1))} s (1 pip) até " +
                    "${num(Combat.attackInterval(t, Balance.MAX_PIPS))} s (${Balance.MAX_PIPS} pips)"
            }
            rows += "Efeito" to effect(t)
            rows += "Power-up" to powerUp(t)
            return DieInfo(summary(t), rows)
        }

        private fun target(t: DieType) = when {
            !t.attacks -> "Não ataca"
            t == DieType.SNIPER -> "Inimigo com mais vida"
            else -> "Inimigo mais à frente"
        }

        private fun summary(t: DieType) = when (t) {
            DieType.FIRE -> "Explode no alvo e atinge os inimigos em volta."
            DieType.ICE -> "Deixa o alvo mais lento."
            DieType.POISON -> "Envenena o alvo, causando dano contínuo."
            DieType.ELECTRIC -> "Raio que salta para inimigos próximos."
            DieType.WIND -> "Ataques fracos, mas muito rápidos."
            DieType.IRON -> "Especialista em chefes."
            DieType.MINE -> "Não ataca: gera SP com o tempo."
            DieType.SNIPER -> "Tiro forte e lento em quem tem mais vida."
            DieType.MIRROR -> "Coringa de merge, com ataque fraco."
            DieType.GROWTH -> "Ganha pips sozinho com o tempo."
        }

        private fun effect(t: DieType) = when (t) {
            DieType.FIRE -> "${pct(Balance.FIRE_SPLASH_FRACTION)} do dano nos inimigos a até " +
                "${num(Balance.FIRE_SPLASH_RADIUS)} de distância do alvo"
            DieType.ICE -> "lentidão de ${pct(iceSlow(1))} (1 pip) a ${pct(iceSlow(Balance.MAX_PIPS))} " +
                "(${Balance.MAX_PIPS} pips) por ${num(Balance.SLOW_DURATION)} s; não acumula, vale a mais forte"
            DieType.POISON -> "${num(Balance.POISON_DPS)} de dano/s por pip durante ${num(Balance.POISON_DURATION)} s; " +
                "acumula até ${Balance.POISON_MAX_STACKS} vezes no mesmo inimigo"
            DieType.ELECTRIC -> "salta para mais ${Balance.CHAIN_JUMPS} inimigos próximos, " +
                "com ${pct(Balance.CHAIN_FALLOFF)} do dano a cada salto"
            DieType.WIND -> "nenhum; compensa com a velocidade"
            DieType.IRON -> "dano x${num(Balance.IRON_BOSS_MULT)} contra chefes"
            DieType.MINE -> "gera ${Balance.MINE_SP_PER_PIP} SP por pip a cada ${num(Balance.MINE_INTERVAL)} s"
            DieType.SNIPER -> "ignora a fila e mira o inimigo mais resistente"
            DieType.MIRROR -> "faz merge com qualquer dado de mesmos pips; o resultado vira o tipo do outro dado " +
                "(dois Espelhos viram um tipo aleatório)"
            DieType.GROWTH -> "ganha +1 pip a cada ${num(Balance.GROWTH_INTERVAL)} s, até ${Balance.MAX_PIPS}"
        }

        private fun powerUp(t: DieType): String {
            val bonus = Balance.POWER_MULT.drop(1).joinToString("/") { "+${pct(it - 1f)}" }
            return when (t) {
                DieType.MINE -> "$bonus de SP gerado (níveis 2 a 5)"
                DieType.POISON -> "$bonus no dano e no veneno (níveis 2 a 5)"
                else -> "$bonus de dano (níveis 2 a 5)"
            }
        }

        private fun iceSlow(pips: Int) =
            minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * pips)

        private fun pct(f: Float) = "${(f * 100).roundToInt()}%"

        /** Up to two decimals, decimal comma, no trailing zeros: 1 → "1", 0.35 → "0,35", 0.526 → "0,53". */
        private fun num(f: Float): String =
            BigDecimal(f.toDouble()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')
    }
}
