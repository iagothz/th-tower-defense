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
        private const val MAX = Balance.MAX_PIPS

        fun of(t: DieType): DieInfo {
            val rows = mutableListOf("Alvo" to target(t))
            if (t.attacks) {
                val dmg = Balance.stats(t).baseDamage
                val what = if (t == DieType.TRAP) "por pip, por mina" else "por pip"
                rows += "Dano" to "${num(dmg)} $what (${num(dmg)} com 1 pip, ${num(dmg * MAX)} com $MAX)"
                rows += "Velocidade" to "a cada ${num(Combat.attackInterval(t, 1))} s (1 pip) até " +
                    "${num(Combat.attackInterval(t, MAX))} s ($MAX pips)"
            }
            rows += "Efeito" to effect(t)
            rows += "Power-up" to powerUp(t)
            return DieInfo(summary(t), rows)
        }

        private fun target(t: DieType) = when {
            !t.attacks -> "Não ataca"
            t == DieType.SNIPER -> "Inimigo com mais vida"
            t == DieType.QUAKE -> "Todos os inimigos"
            t == DieType.TRAP -> "O caminho à frente do primeiro inimigo"
            else -> "Inimigo mais à frente"
        }

        private fun summary(t: DieType) = when (t) {
            DieType.FIRE -> "Explode no alvo e atinge os inimigos em volta."
            DieType.ICE -> "Deixa o alvo mais lento."
            DieType.POISON -> "Envenena o alvo, causando dano contínuo."
            DieType.ELECTRIC -> "Raio que salta para inimigos próximos."
            DieType.WIND -> "Ataques fracos, mas muito rápidos."
            DieType.IRON -> "Especialista em chefes."
            DieType.MINE -> "Minerador: não ataca, gera SP com o tempo."
            DieType.SNIPER -> "Tiro forte e lento em quem tem mais vida."
            DieType.MIRROR -> "Coringa de merge, com ataque fraco."
            DieType.GROWTH -> "Ganha pips sozinho com o tempo."
            DieType.LIGHT -> "Ataque fraco; acelera os dados vizinhos."
            DieType.CRIT -> "Dá chance de crítico a todo o tabuleiro."
            DieType.COMBO -> "Fraco sozinho, forte em grupo."
            DieType.CURSE -> "Amaldiçoa o alvo para receber mais dano."
            DieType.HUNTER -> "Fica mais forte a cada abate."
            DieType.GRAVITY -> "Empurra o inimigo para trás."
            DieType.TIME -> "Congela o alvo por um instante."
            DieType.TRAP -> "Planta minas no caminho."
            DieType.QUAKE -> "Atinge todos os inimigos de uma vez."
            DieType.REAPER -> "Executa inimigos com pouca vida."
            DieType.NOVA -> "Inimigos mortos por ela explodem."
            DieType.LUCK -> "Dano sorteado a cada ataque."
            DieType.VAMPIRE -> "Ganha SP extra ao matar."
        }

        private fun effect(t: DieType) = when (t) {
            DieType.FIRE -> "${pct(Balance.FIRE_SPLASH_FRACTION)} do dano nos inimigos a até " +
                "${num(Balance.FIRE_SPLASH_RADIUS)} de distância do alvo"
            DieType.ICE -> "lentidão de ${pct(iceSlow(1))} (1 pip) a ${pct(iceSlow(MAX))} " +
                "($MAX pips) por ${num(Balance.SLOW_DURATION)} s; não acumula, vale a mais forte"
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
            DieType.GROWTH -> "ganha +1 pip a cada ${num(Balance.GROWTH_INTERVAL)} s, até $MAX"
            DieType.LIGHT -> "os dados de cima, de baixo e dos lados atacam +${pct(Balance.LIGHT_SPEED_PER_PIP)} " +
                "mais rápido por pip da Luz; várias Luzes somam"
            DieType.CRIT -> "cada pip dá +${pct(Balance.CRIT_CHANCE_PER_PIP)} de chance de crítico " +
                "(dano x${num(Balance.CRIT_MULT)}) a todos os dados, até ${pct(Balance.CRIT_MAX)} somando os Críticos"
            DieType.COMBO -> "+${pct(Balance.COMBO_BONUS)} de dano para cada outro Combo no tabuleiro"
            DieType.CURSE -> "o alvo recebe ${pct(curse(1))} (1 pip) a ${pct(curse(MAX))} ($MAX pips) a mais " +
                "de dano de tudo, inclusive veneno, por ${num(Balance.CURSE_DURATION)} s"
            DieType.HUNTER -> "+1 stack por abate; cada stack dá +${pct(Balance.HUNTER_BONUS_PER_STACK)} de dano " +
                "(máx. ${Balance.HUNTER_MAX_STACKS}); merge de Caçadores soma os stacks se o resultado for Caçador"
            DieType.GRAVITY -> "o alvo volta ${num(push(1))} (1 pip) a ${num(push(MAX))} ($MAX pips) no caminho; " +
                "chefes voltam ${pct(Balance.GRAVITY_BOSS_FACTOR)} disso"
            DieType.TIME -> "o alvo fica parado de ${num(stun(1))} s (1 pip) a ${num(stun(MAX))} s ($MAX pips); " +
                "chefes, ${pct(Balance.TIME_BOSS_FACTOR)} disso"
            DieType.TRAP -> "a mina fica ${num(Balance.TRAP_AHEAD)} à frente do primeiro inimigo e explode quando " +
                "alguém pisa, atingindo quem estiver a até ${num(Balance.TRAP_RADIUS)}; " +
                "máx. ${Balance.TRAP_MAX_PER_DIE} minas por dado"
            DieType.QUAKE -> "sem alvo único: cada ataque acerta todos os inimigos na tela"
            DieType.REAPER -> "inimigos comuns que ficarem abaixo de ${pct(reap(1))} (1 pip) a ${pct(reap(MAX))} " +
                "($MAX pips) da vida morrem na hora; chefes levam só o dano"
            DieType.NOVA -> "o inimigo morto explode com o dano do dado em quem estiver a até " +
                "${num(Balance.NOVA_RADIUS)}; quem morrer na explosão também explode"
            DieType.LUCK -> "cada ataque causa de 0x a ${num(Balance.LUCK_MAX_MULT)}x o dano normal " +
                "(média ${num(Balance.LUCK_MAX_MULT / 2)}x)"
            DieType.VAMPIRE -> "+${Balance.VAMPIRE_SP_PER_PIP} SP por pip a cada inimigo que matar, " +
                "além da recompensa normal"
        }

        private fun powerUp(t: DieType): String {
            val bonus = Balance.POWER_MULT.drop(1).joinToString("/") { "+${pct(it - 1f)}" }
            return when (t) {
                DieType.MINE -> "$bonus de SP gerado (níveis 2 a 5)"
                DieType.POISON -> "$bonus no dano e no veneno (níveis 2 a 5)"
                DieType.TRAP, DieType.NOVA -> "$bonus no dano das explosões (níveis 2 a 5)"
                else -> "$bonus de dano (níveis 2 a 5)"
            }
        }

        private fun iceSlow(pips: Int) =
            minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * pips)

        private fun curse(pips: Int) = Balance.CURSE_BASE + Balance.CURSE_PER_PIP * pips

        private fun push(pips: Int) = Balance.GRAVITY_PUSH_BASE + Balance.GRAVITY_PUSH_PER_PIP * pips

        private fun stun(pips: Int) = Balance.TIME_STUN_BASE + Balance.TIME_STUN_PER_PIP * pips

        private fun reap(pips: Int) = Balance.REAPER_BASE + Balance.REAPER_PER_PIP * pips

        private fun pct(f: Float) = "${(f * 100).roundToInt()}%"

        /** Up to two decimals, decimal comma, no trailing zeros: 1 → "1", 0.35 → "0,35", 0.526 → "0,53". */
        private fun num(f: Float): String =
            BigDecimal(f.toDouble()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')
    }
}
