package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.EnemyKind

/** One sentence of advice for the game-over screen, keyed by whoever took the last life. */
object Tips {
    fun forKiller(k: EnemyKind?): String = S[forKillerPt(k)]

    private fun forKillerPt(k: EnemyKind?): String = when (k) {
        null -> "Junte dados cedo: um dado de 3 pips vale mais que três de 1."
        EnemyKind.NORMAL -> "Os soldados passaram em massa. Fogo e Terremoto acertam vários de uma vez; Elétrico salta entre eles."
        EnemyKind.FAST -> "Os corredores escaparam. Gelo, Tempo e Gravidade seguram eles; Vento atira rápido o bastante para acompanhar."
        EnemyKind.TANK -> "Os tanques passaram. Ferro, Sniper e Maldição derrubam os de muita vida; veneno acumula bem neles."
        EnemyKind.HEALER -> "O curandeiro manteve a fila viva. Mate ele primeiro: Sniper mira o mais resistente, Fogo pega o grupo."
        EnemyKind.ELITE -> "A Elite curava a escolta. Mate ela primeiro com Sniper ou Ferro; depois a fila cai fácil."
        EnemyKind.TREASURE -> "O Tesouro escapou: não custa vida, mas valia 60 SP. Vento e Sniper alcançam os rápidos."
        EnemyKind.GHOST -> "Fantasmas ignoram lentidão e armadilhas. Dano puro: Ferro, Sniper, Crítico e Combo."
        EnemyKind.BOSS_SILENCER -> "O Silenciador cala um dado por vez. Espalhe o dano em vários dados em vez de um só gigante."
        EnemyKind.BOSS_SWAPPER -> "O Trocador embaralha o tabuleiro. Luz e Combo dependem de posição; prefira dados que valem em qualquer lugar."
        EnemyKind.BOSS_REGEN -> "O Regenerador cura 2% por segundo. Precisa de dano constante alto: Ferro, veneno acumulado e Caçador."
        EnemyKind.BOSS_SPLITTER -> "O Divisor vira dois tanques ao morrer. Deixe dano de área pronto: Fogo, Supernova e Terremoto."
        EnemyKind.BOSS_ARMORED -> "O Blindado ignora golpes fracos. Veneno passa direto pela armadura; Sniper e Ferro batem forte."
        EnemyKind.BOSS_THIEF -> "O Ladrão rouba SP a cada 6 s, mas devolve tudo ao morrer. Foque nele com Ferro e Sniper."
        EnemyKind.BOSS_COMMANDER -> "O Comandante acelera quem está perto dele. Gelo e Tempo anulam a pressa; mate-o antes da escolta."
    }
}
