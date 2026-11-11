package com.iagothz.mergetd.ui

/** English for the dice texts in [DieInfo], keyed by the Portuguese format strings. */
object DieTextsEn {
    val TABLE: Map<String, String> = mapOf(
        "Explode no alvo e atinge os inimigos em volta." to "Explodes on the target and hits the enemies around it.",
        "Deixa o alvo mais lento." to "Slows the target.",
        "Envenena o alvo, causando dano contínuo." to "Poisons the target for damage over time.",
        "Raio que salta para inimigos próximos." to "Lightning that jumps to nearby enemies.",
        "Ataques fracos, mas muito rápidos." to "Weak but very fast attacks.",
        "Especialista em chefes." to "Boss specialist.",
        "Minerador: não ataca, gera SP com o tempo." to "Miner: doesn't attack, makes SP over time.",
        "Tiro forte e lento em quem tem mais vida." to "Strong, slow shot at whoever has the most HP.",
        "Coringa: combina com qualquer dado; o resultado é sorteado." to "Wildcard: merges with any die; the result is rolled.",
        "Ganha pips sozinho e vira um dado do deck." to "Earns pips on its own and turns into a deck die.",
        "Ataque fraco; acelera os dados vizinhos." to "Weak attack; speeds up neighbouring dice.",
        "Dá chance de crítico a todo o tabuleiro." to "Gives the whole board a crit chance.",
        "Fraco sozinho, forte em grupo." to "Weak alone, strong in numbers.",
        "Amaldiçoa o alvo para receber mais dano." to "Curses the target to take more damage.",
        "Fica mais forte a cada abate." to "Grows stronger with every kill.",
        "Empurra o inimigo para trás." to "Pushes the enemy back.",
        "Congela o alvo por um instante." to "Freezes the target for a moment.",
        "Planta minas no caminho." to "Plants mines on the path.",
        "Atinge todos os inimigos de uma vez." to "Hits every enemy at once.",
        "Executa inimigos com pouca vida." to "Executes enemies on low HP.",
        "Inimigos mortos por ela explodem." to "Enemies it kills explode.",
        "Dano sorteado a cada ataque." to "Damage rolled on every attack.",
        "Ganha SP extra ao matar." to "Earns extra SP on kills.",
        "Vira uma cópia de outro dado." to "Becomes a copy of another die.",
        "Coringa garantido: o resultado mantém o tipo do outro dado." to "Guaranteed wildcard: the result keeps the other die's type.",
        "%s do dano nos inimigos a até %s de distância do alvo" to "%s of the damage to enemies within %s of the target",
        "lentidão de %s (1 pip) a %s (%d pips) por %s s; não acumula, vale a mais forte" to "slow of %s (1 pip) to %s (%d pips) for %s s; doesn't stack, the strongest wins",
        "%s de dano/s por pip durante %s s; acumula até %d vezes no mesmo inimigo" to "%s damage/s per pip for %s s; stacks up to %d times on one enemy",
        "salta para mais %d inimigos próximos, com %s do dano a cada salto" to "jumps to %d more nearby enemies, with %s of the damage per jump",
        "nenhum; compensa com a velocidade" to "none; it makes up for it with speed",
        "dano x%s contra chefes" to "x%s damage to bosses",
        "gera %d SP por pip a cada %s s" to "makes %d SP per pip every %s s",
        "ignora a fila e mira o inimigo mais resistente" to "ignores the queue and aims at the toughest enemy",
        "faz merge com qualquer dado de mesmos pips; o resultado é sorteado entre os 5 tipos do deck, como num merge normal" to
            "merges with any die of the same pips; the result is rolled from the deck's 5 types, like a normal merge",
        "a cada %s s ganha +1 pip e vira um dos 5 tipos do deck, sorteado (pode continuar Crescimento); para em %d pips" to
            "every %s s it gains +1 pip and turns into one of the deck's 5 types at random (it may stay Growth); stops at %d pips",
        "os dados de cima, de baixo e dos lados atacam +%s mais rápido por pip da Luz; várias Luzes somam" to
            "the dice above, below and beside attack +%s faster per pip of Light; several Lights add up",
        "cada pip dá +%s de chance de crítico (dano x%s) a todos os dados, até %s somando os Críticos" to
            "each pip gives +%s crit chance (x%s damage) to every die, up to %s across all Criticals",
        "+%s de dano para cada outro Combo no tabuleiro" to "+%s damage for every other Combo on the board",
        "o alvo recebe %s (1 pip) a %s (%d pips) a mais de dano de tudo, inclusive veneno, por %s s" to
            "the target takes %s (1 pip) to %s (%d pips) more damage from everything, poison included, for %s s",
        "+1 stack por abate; cada stack dá +%s de dano; limite de %d stacks por pip (%d com 1 pip, %d com %d) e sem limite com %d pips; merge de Caçadores soma os stacks se o resultado for Caçador" to
            "+1 stack per kill; each stack gives +%s damage; cap of %d stacks per pip (%d at 1 pip, %d at %d) and no cap at %d pips; merging Hunters adds the stacks if the result is a Hunter",
        "o alvo volta %s (1 pip) a %s (%d pips) no caminho; chefes voltam %s disso" to "the target goes back %s (1 pip) to %s (%d pips) along the path; bosses only %s of that",
        "o alvo fica parado de %s s (1 pip) a %s s (%d pips); chefes, %s disso" to "the target stands still for %s s (1 pip) to %s s (%d pips); bosses, %s of that",
        "a mina fica %s à frente do primeiro inimigo e explode quando alguém pisa, atingindo quem estiver a até %s; máx. %d minas por dado" to
            "the mine sits %s ahead of the leading enemy and goes off when stepped on, hitting anyone within %s; max. %d mines per die",
        "sem alvo único: cada ataque acerta todos os inimigos na tela" to "no single target: every attack hits every enemy on screen",
        "inimigos comuns que ficarem abaixo de %s (1 pip) a %s (%d pips) da vida morrem na hora; chefes levam só o dano" to
            "common enemies left under %s (1 pip) to %s (%d pips) of their HP die at once; bosses just take the damage",
        "o inimigo morto explode com o dano do dado em quem estiver a até %s; quem morrer na explosão também explode" to
            "the killed enemy explodes for the die's damage on anyone within %s; whoever dies in the blast explodes too",
        "cada ataque causa de 0x a %sx o dano normal (média %sx)" to "every attack deals 0x to %sx the normal damage (%sx on average)",
        "+%d SP por pip a cada inimigo que matar, além da recompensa normal" to "+%d SP per pip for every enemy it kills, on top of the normal reward",
        "arrastado sobre um dado de mesmos pips (ou o contrário), vira uma cópia dele, tipo e pips, e o original continua no lugar; stacks do Caçador não são copiados; entre dois coringas vale o efeito do dado arrastado; dois Clonadores fazem merge normal" to
            "dropped on a die of the same pips (or the other way round), it becomes a copy of it, type and pips, and the original stays; Hunter stacks aren't copied; between two wildcards the dragged die's effect wins; two Cloners merge normally",
        "o resultado é o tipo do outro dado com +1 pip; dois Reforços viram um Reforço; levado a um Clonador, sobe o Clonador" to
            "the result is the other die's type with +1 pip; two Boosts make a Boost; taken to a Cloner, it grows the Cloner",
        "explosão com raio ×%s e dano cheio nos vizinhos" to "blast radius ×%s with full damage on the neighbours",
        "além da lentidão, congela o alvo por %s s" to "besides the slow, freezes the target for %s s",
        "o veneno dura o dobro (%s s)" to "poison lasts twice as long (%s s)",
        "o raio salta para %d inimigos" to "the lightning jumps to %d enemies",
        "ataca duas vezes mais rápido" to "attacks twice as fast",
        "gera o dobro de SP" to "makes twice the SP",
        "acerta também o segundo inimigo mais resistente" to "also hits the second toughest enemy",
        "ataca com o dano do dado mais forte do tabuleiro" to "attacks with the damage of the strongest die on the board",
        "cresce mais rápido: %s s por pip (níveis 2 a 5)" to "grows faster: %s s per pip (levels 2 to 5)",
        "a cada %s s planta um dado de 1 pip do deck numa célula vazia" to "every %s s it plants a 1-pip deck die in an empty cell",
        "acelera também os vizinhos das diagonais" to "also speeds up the diagonal neighbours",
        "os críticos de todo o tabuleiro dão x%s" to "every crit on the board deals x%s",
        "conta como dois Combos" to "counts as two Combos",
        "maldição %s mais forte e durando o dobro" to "curse %s stronger and lasting twice as long",
        "cada stack dá +%s (e os stacks não têm limite)" to "each stack gives +%s (and there is no stack cap)",
        "empurra inimigos comuns sem limite" to "pushes common enemies with no limit",
        "congela também quem está a até %s do alvo" to "also freezes anyone within %s of the target",
        "%d minas por dado, com raio ×%s" to "%d mines per die, with radius ×%s",
        "cada tremor deixa todos %s mais lentos por %s s" to "every quake slows everyone by %s for %s s",
        "executa chefes também, abaixo de metade do limite" to "executes bosses too, under half the threshold",
        "explosões com raio e dano ×%s, inclusive em cadeia" to "blasts with ×%s radius and damage, chains included",
        "sorteia de %sx a %sx: nunca erra" to "rolls %sx to %sx: never whiffs",
        "SP em dobro e +1 vida a cada %d abates (máx. %d)" to "double SP and +1 life every %d kills (max. %d)",
        "clona qualquer dado, de qualquer quantidade de pips" to "clones any die, whatever its pips",
        "a cada %s s sobe de graça um nível de power-up de um tipo do deck" to "every %s s it raises a deck type's power-up level for free",
    )
}
