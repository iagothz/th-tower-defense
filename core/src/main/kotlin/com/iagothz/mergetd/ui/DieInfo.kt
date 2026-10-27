package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Balance
import com.iagothz.mergetd.logic.Combat
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.Synergy
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/**
 * Player-facing description of a die type. Every number is read from [Balance], so it can't drift;
 * every sentence goes through [S], so it follows the language setting.
 */
data class DieInfo(val summary: String, val rows: List<Pair<String, String>>) {
    companion object {
        private const val MAX = Balance.MAX_PIPS

        fun of(t: DieType): DieInfo {
            val rows = mutableListOf(S["Alvo"] to target(t))
            if (t.attacks) {
                val dmg = Balance.stats(t).baseDamage
                val key = if (t == DieType.TRAP) "%s por pip, por mina (%s com 1 pip, %s com %d)" else "%s por pip (%s com 1 pip, %s com %d)"
                rows += S["Dano"] to S.f(key, num(dmg), num(dmg), num(dmg * MAX), MAX)
                rows += S["Velocidade"] to S.f("a cada %s s (1 pip) até %s s (%d pips)", num(Combat.attackInterval(t, 1)), num(Combat.attackInterval(t, MAX)), MAX)
            }
            rows += S["Efeito"] to effect(t)
            rows += S["7 pips"] to maxed(t)
            synergy(t)?.let { rows += S["Sinergia"] to it }
            rows += S["Power-up"] to powerUp(t)
            return DieInfo(summary(t), rows)
        }

        private fun target(t: DieType) = S[
            when {
                !t.attacks -> "Não ataca"
                t == DieType.SNIPER -> "Inimigo com mais vida"
                t == DieType.QUAKE -> "Todos os inimigos"
                t == DieType.TRAP -> "O caminho à frente do primeiro inimigo"
                else -> "Inimigo mais à frente"
            },
        ]

        private fun summary(t: DieType) = S[
            when (t) {
                DieType.FIRE -> "Explode no alvo e atinge os inimigos em volta."
                DieType.ICE -> "Deixa o alvo mais lento."
                DieType.POISON -> "Envenena o alvo, causando dano contínuo."
                DieType.ELECTRIC -> "Raio que salta para inimigos próximos."
                DieType.WIND -> "Ataques fracos, mas muito rápidos."
                DieType.IRON -> "Especialista em chefes."
                DieType.MINE -> "Minerador: não ataca, gera SP com o tempo."
                DieType.SNIPER -> "Tiro forte e lento em quem tem mais vida."
                DieType.MIRROR -> "Coringa: combina com qualquer dado; o resultado é sorteado."
                DieType.GROWTH -> "Ganha pips sozinho e vira um dado do deck."
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
                DieType.CLONE -> "Vira uma cópia de outro dado."
                DieType.BOOST -> "Coringa garantido: o resultado mantém o tipo do outro dado."
            },
        ]

        private fun effect(t: DieType) = when (t) {
            DieType.FIRE -> S.f("%s do dano nos inimigos a até %s de distância do alvo", pct(Balance.FIRE_SPLASH_FRACTION), num(Balance.FIRE_SPLASH_RADIUS))
            DieType.ICE -> S.f("lentidão de %s (1 pip) a %s (%d pips) por %s s; não acumula, vale a mais forte", pct(iceSlow(1)), pct(iceSlow(MAX)), MAX, num(Balance.SLOW_DURATION))
            DieType.POISON -> S.f("%s de dano/s por pip durante %s s; acumula até %d vezes no mesmo inimigo", num(Balance.POISON_DPS), num(Balance.POISON_DURATION), Balance.POISON_MAX_STACKS)
            DieType.ELECTRIC -> S.f("salta para mais %d inimigos próximos, com %s do dano a cada salto", Balance.CHAIN_JUMPS, pct(Balance.CHAIN_FALLOFF))
            DieType.WIND -> S["nenhum; compensa com a velocidade"]
            DieType.IRON -> S.f("dano x%s contra chefes", num(Balance.IRON_BOSS_MULT))
            DieType.MINE -> S.f("gera %d SP por pip a cada %s s", Balance.MINE_SP_PER_PIP, num(Balance.MINE_INTERVAL))
            DieType.SNIPER -> S["ignora a fila e mira o inimigo mais resistente"]
            DieType.MIRROR -> S["faz merge com qualquer dado de mesmos pips; o resultado é sorteado entre os 5 tipos do deck, como num merge normal"]
            DieType.GROWTH -> S.f("a cada %s s ganha +1 pip e vira um dos 5 tipos do deck, sorteado (pode continuar Crescimento); para em %d pips", num(Balance.GROWTH_INTERVAL), MAX)
            DieType.LIGHT -> S.f("os dados de cima, de baixo e dos lados atacam +%s mais rápido por pip da Luz; várias Luzes somam", pct(Balance.LIGHT_SPEED_PER_PIP))
            DieType.CRIT -> S.f("cada pip dá +%s de chance de crítico (dano x%s) a todos os dados, até %s somando os Críticos", pct(Balance.CRIT_CHANCE_PER_PIP), num(Balance.CRIT_MULT), pct(Balance.CRIT_MAX))
            DieType.COMBO -> S.f("+%s de dano para cada outro Combo no tabuleiro", pct(Balance.COMBO_BONUS))
            DieType.CURSE -> S.f("o alvo recebe %s (1 pip) a %s (%d pips) a mais de dano de tudo, inclusive veneno, por %s s", pct(curse(1)), pct(curse(MAX)), MAX, num(Balance.CURSE_DURATION))
            DieType.HUNTER -> S.f(
                "+1 stack por abate; cada stack dá +%s de dano; limite de %d stacks por pip (%d com 1 pip, %d com %d) e sem limite com %d pips; merge de Caçadores soma os stacks se o resultado for Caçador",
                pct(Balance.HUNTER_BONUS_PER_STACK), Balance.HUNTER_STACKS_PER_PIP, Balance.hunterCap(1), Balance.hunterCap(MAX - 1), MAX - 1, MAX,
            )
            DieType.GRAVITY -> S.f("o alvo volta %s (1 pip) a %s (%d pips) no caminho; chefes voltam %s disso", num(push(1)), num(push(MAX)), MAX, pct(Balance.GRAVITY_BOSS_FACTOR))
            DieType.TIME -> S.f("o alvo fica parado de %s s (1 pip) a %s s (%d pips); chefes, %s disso", num(stun(1)), num(stun(MAX)), MAX, pct(Balance.TIME_BOSS_FACTOR))
            DieType.TRAP -> S.f("a mina fica %s à frente do primeiro inimigo e explode quando alguém pisa, atingindo quem estiver a até %s; máx. %d minas por dado", num(Balance.TRAP_AHEAD), num(Balance.TRAP_RADIUS), Balance.TRAP_MAX_PER_DIE)
            DieType.QUAKE -> S["sem alvo único: cada ataque acerta todos os inimigos na tela"]
            DieType.REAPER -> S.f("inimigos comuns que ficarem abaixo de %s (1 pip) a %s (%d pips) da vida morrem na hora; chefes levam só o dano", pct(reap(1)), pct(reap(MAX)), MAX)
            DieType.NOVA -> S.f("o inimigo morto explode com o dano do dado em quem estiver a até %s; quem morrer na explosão também explode", num(Balance.NOVA_RADIUS))
            DieType.LUCK -> S.f("cada ataque causa de 0x a %sx o dano normal (média %sx)", num(Balance.LUCK_MAX_MULT), num(Balance.LUCK_MAX_MULT / 2))
            DieType.VAMPIRE -> S.f("+%d SP por pip a cada inimigo que matar, além da recompensa normal", Balance.VAMPIRE_SP_PER_PIP)
            DieType.CLONE -> S["arrastado sobre um dado de mesmos pips (ou o contrário), vira uma cópia dele, tipo e pips, e o original continua no lugar; stacks do Caçador não são copiados; entre dois coringas vale o efeito do dado arrastado; dois Clonadores fazem merge normal"]
            DieType.BOOST -> S["o resultado é o tipo do outro dado com +1 pip; dois Reforços viram um Reforço; levado a um Clonador, sobe o Clonador"]
        }

        /** What the die gains at the pip cap. */
        fun maxed(t: DieType): String = when (t) {
            DieType.FIRE -> S.f("explosão com raio ×%s e dano cheio nos vizinhos", num(Balance.FIRE_MAX_RADIUS))
            DieType.ICE -> S.f("além da lentidão, congela o alvo por %s s", num(Balance.ICE_FREEZE))
            DieType.POISON -> S.f("o veneno dura o dobro (%s s)", num(Balance.POISON_DURATION * 2))
            DieType.ELECTRIC -> S.f("o raio salta para %d inimigos", Balance.CHAIN_JUMPS_MAX)
            DieType.WIND -> S["ataca duas vezes mais rápido"]
            DieType.IRON -> S.f("dano x%s contra chefes", num(Balance.IRON_BOSS_MULT_MAX))
            DieType.MINE -> S["gera o dobro de SP"]
            DieType.SNIPER -> S["acerta também o segundo inimigo mais resistente"]
            DieType.MIRROR -> S["ataca com o dano do dado mais forte do tabuleiro"]
            DieType.GROWTH -> S.f("a cada %s s planta um dado de 1 pip do deck numa célula vazia", num(Balance.GROWTH_INTERVAL))
            DieType.LIGHT -> S["acelera também os vizinhos das diagonais"]
            DieType.CRIT -> S.f("os críticos de todo o tabuleiro dão x%s", num(Balance.CRIT_MULT_MAX))
            DieType.COMBO -> S["conta como dois Combos"]
            DieType.CURSE -> S.f("maldição %s mais forte e durando o dobro", pct(Balance.CURSE_MAX_MULT - 1))
            DieType.HUNTER -> S.f("cada stack dá +%s (e os stacks não têm limite)", pct(Balance.HUNTER_BONUS_PER_STACK_MAX))
            DieType.GRAVITY -> S["empurra inimigos comuns sem limite"]
            DieType.TIME -> S.f("congela também quem está a até %s do alvo", num(Balance.TIME_AREA))
            DieType.TRAP -> S.f("%d minas por dado, com raio ×%s", Balance.TRAP_MAX_PER_DIE_MAX, num(Balance.TRAP_MAX_RADIUS))
            DieType.QUAKE -> S.f("cada tremor deixa todos %s mais lentos por %s s", pct(Balance.QUAKE_SLOW), num(Balance.QUAKE_SLOW_TIME))
            DieType.REAPER -> S["executa chefes também, abaixo de metade do limite"]
            DieType.NOVA -> S.f("explosões com raio e dano ×%s, inclusive em cadeia", num(Balance.NOVA_MAX))
            DieType.LUCK -> S.f("sorteia de %sx a %sx: nunca erra", num(Balance.LUCK_MIN_MULT_MAX), num(Balance.LUCK_MIN_MULT_MAX + Balance.LUCK_MAX_MULT))
            DieType.VAMPIRE -> S.f("SP em dobro e +1 vida a cada %d abates (máx. %d)", Balance.VAMPIRE_LIFE_KILLS, Balance.MAX_LIVES)
            DieType.CLONE -> S["clona qualquer dado, de qualquer quantidade de pips"]
            DieType.BOOST -> S.f("a cada %s s sobe de graça um nível de power-up de um tipo do deck", num(Balance.BOOST_INTERVAL))
        }

        /** The synergies this type takes part in, each with its partner. */
        private fun synergy(t: DieType): String? {
            val mine = Synergy.entries.filter { it.needs(t) }
            if (mine.isEmpty()) return null
            return mine.joinToString("; ") { s ->
                val other = if (s.a == t) s.b else s.a
                S.f("%s (com %s): %s", S[s.title], Skin.fullName(other), S[s.description])
            }
        }

        private fun powerUp(t: DieType): String {
            val bonus = Balance.POWER_MULT.drop(1).joinToString("/") { "+${pct(it - 1f)}" }
            return when (t) {
                DieType.GROWTH -> S.f("cresce mais rápido: %s s por pip (níveis 2 a 5)", Balance.POWER_MULT.drop(1).joinToString("/") { num(Balance.GROWTH_INTERVAL / it) })
                DieType.MINE -> S.f("%s de SP gerado (níveis 2 a 5)", bonus)
                DieType.POISON -> S.f("%s no dano e no veneno (níveis 2 a 5)", bonus)
                DieType.TRAP, DieType.NOVA -> S.f("%s no dano das explosões (níveis 2 a 5)", bonus)
                else -> S.f("%s de dano (níveis 2 a 5)", bonus)
            }
        }

        private fun iceSlow(pips: Int) =
            minOf(Balance.ICE_SLOW_MAX, Balance.ICE_SLOW_BASE + Balance.ICE_SLOW_PER_PIP * pips)

        private fun curse(pips: Int) = Balance.CURSE_BASE + Balance.CURSE_PER_PIP * pips

        private fun push(pips: Int) = Balance.GRAVITY_PUSH_BASE + Balance.GRAVITY_PUSH_PER_PIP * pips

        private fun stun(pips: Int) = Balance.TIME_STUN_BASE + Balance.TIME_STUN_PER_PIP * pips

        private fun reap(pips: Int) = Balance.REAPER_BASE + Balance.REAPER_PER_PIP * pips

        private fun pct(f: Float) = "${(f * 100).roundToInt()}%"

        /** Up to two decimals, decimal comma in Portuguese, no trailing zeros: 1 → "1", 0.35 → "0,35". */
        internal fun num(f: Float): String {
            val plain = BigDecimal(f.toDouble()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            return if (S.language == Language.EN) plain else plain.replace('.', ',')
        }
    }
}
