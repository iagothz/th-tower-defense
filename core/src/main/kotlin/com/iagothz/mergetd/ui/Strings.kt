package com.iagothz.mergetd.ui

import java.util.Locale

enum class Language(val id: String, val title: String) {
    AUTO("auto", "Automático"),
    PT("pt", "Português"),
    EN("en", "English"),
    ;

    companion object {
        fun byId(id: String): Language? = entries.firstOrNull { it.id == id }

        /** The language to show: the setting, or the device's when it is AUTO (Portuguese stays, anything else is English). */
        fun resolve(setting: Language, deviceCode: String): Language = when (setting) {
            AUTO -> if (deviceCode.lowercase().startsWith("pt")) PT else EN
            else -> setting
        }
    }
}

/**
 * The string table. The Portuguese text is the key, so the code reads naturally and a missing translation
 * falls back to it. `S["Jogar"]` looks a text up; `S.f("Onda %d", n)` formats one.
 */
object S {
    var language: Language = Language.PT

    operator fun get(pt: String): String = if (language == Language.EN) EN[pt] ?: pt else pt

    fun f(pt: String, vararg args: Any?): String = String.format(Locale.ROOT, get(pt), *args)

    fun keys(): Set<String> = EN.keys

    fun has(lang: Language, key: String): Boolean = lang == Language.PT || EN.containsKey(key)

    fun english(key: String): String = EN[key] ?: ""

    private val EN: Map<String, String> = mapOf(
        "Início" to "Home", "Mapas" to "Maps", "Config" to "Settings",
        "Jogar" to "Play", "Deck" to "Deck", "Partida" to "Match", "Desafio" to "Daily", "Estatísticas" to "Stats",
        "Configurações" to "Settings", "Junte dados iguais, segure as ondas" to "Merge matching dice, hold the waves",
        "%s · %s: recorde %d" to "%s · %s: best %d",
        "Som" to "Sound", "Música" to "Music", "Vibrar" to "Vibration", "Vibração" to "Vibration", "Idioma" to "Language", "sim" to "on", "não" to "off",
        "Automático" to "Automatic", "Português" to "Portuguese", "English" to "English",
        "Privacidade: este jogo funciona sem internet, não coleta nem envia nenhum dado." to
            "Privacy: this game works offline and collects or sends no data at all.",
        "Privacidade: este jogo funciona sem internet e não coleta nada. Com a nuvem ligada, seu progresso fica na sua conta do Google Play Games." to
            "Privacy: this game works offline and collects nothing. With the cloud on, your progress is kept in your Google Play Games account.",
        "Nuvem: desligada" to "Cloud: off", "Nuvem: sem conta" to "Cloud: not signed in", "Nuvem: sincronizando..." to "Cloud: syncing...",
        "Nuvem: sincronizada hoje" to "Cloud: synced today", "Nuvem: sincronizada há %d dias" to "Cloud: synced %d days ago",
        "Nuvem: sem conexão" to "Cloud: no connection", "Nuvem: falhou" to "Cloud: failed",
        "Qual progresso manter?" to "Which progress to keep?", "Este aparelho" to "This device", "Nuvem" to "Cloud",
        "Manter este" to "Keep this one", "Manter a nuvem" to "Keep the cloud", "hoje" to "today", "há %d dias" to "%d days ago",
        "Ligar" to "Turn on", "Entrar" to "Sign in", "Desligar" to "Turn off", "Tentar" to "Retry",
        "Voltar" to "Back", "Confirmar" to "Confirm",
        "Partida" to "Match", "Modo" to "Mode", "Jogar código" to "Play a code", "Código inválido" to "Invalid code",
        "Código da partida" to "Match code", "recorde %d" to "best %d",
        "Normal" to "Normal", "Boss Rush" to "Boss Rush", "Sobrevivência" to "Survival", "Rápido" to "Rush",
        "Ondas infinitas, chefe a cada 10" to "Endless waves, a boss every 10",
        "Só chefes, um por onda, com menos vida" to "Bosses only, one per wave, with less HP",
        "Uma vida só; SP em dobro" to "One life; double SP",
        "Ondas emendadas, inimigos mais rápidos e mais frágeis" to "Back-to-back waves, faster and frailer enemies",
        "Ferradura" to "Horseshoe", "Serpente" to "Serpent", "Anel" to "Ring", "Reta" to "Straight",
        "Fácil" to "Easy", "Difícil" to "Hard",
        "nível %d · %d partidas" to "level %d · %d games", "Progresso da nuvem aplicado" to "Cloud progress applied",
        "arraste %d" to "drag %d",
        "Sinergias do deck: %s" to "Deck synergies: %s",
        "Sinergias do deck: nenhuma ainda (veja os pares na descrição de cada dado)" to
            "Deck synergies: none yet (each die's description lists its pairs)",
        "Continuar" to "Resume", "Reiniciar" to "Restart", "Sair" to "Quit",
        "Sim" to "Yes", "Não" to "No", "Pausa" to "Paused", "Reiniciar a partida?" to "Restart the match?",
        "Sair da partida?" to "Quit the match?", "O progresso desta partida será perdido." to "This match's progress will be lost.",
        "Pular" to "Skip", "MAX" to "MAX", "Nv%d" to "Lv%d",
        "Próxima onda em %d" to "Next wave in %d", "  ·  chefe em %d" to "  ·  boss in %d", "Chefe: %s" to "Boss: %s",
        "Desafio do dia" to "Daily challenge", "Desafio do dia: deck e mapa do dia" to "Daily challenge: the day's deck and map",
        "Chefe!" to "Boss!", "Máximo!" to "Max!", "Onda limpa!" to "Wave clear!", "Onda %d" to "Wave %d", "%s!" to "%s!", "-%d SP" to "-%d SP",
        "Horda" to "Horde", "Tanques" to "Tanks", "Corrida" to "Rush",
        "%d %s" to "%d %s", "soldados" to "soldiers", "corredores" to "runners", "tanques" to "tanks",
        "curandeiros" to "healers", "fantasmas" to "ghosts",
        "pip" to "pip", "pips" to "pips", " · %d stacks" to " · %d stacks", "%s · %d %s%s" to "%s · %d %s%s",
        "Dano %s  ·  a cada %s s  ·  Nv%d" to "Damage %s  ·  every %s s  ·  Lv%d", "7 pips: %s" to "7 pips: %s",
        "Toque numa célula vazia para comprar um dado ali." to "Tap an empty cell to buy a die there.",
        "Arraste um dado sobre outro do mesmo tipo e com os mesmos pips para juntar os dois." to
            "Drag a die onto another of the same type and pips to merge them.",
        "Gaste SP num power-up para fortalecer todos os dados daquele tipo." to "Spend SP on a power-up to strengthen every die of that type.",
        "Pronto! Segure um dado para ver o que ele faz. Boa sorte." to "Done! Hold a die to see what it does. Good luck.",
        "Fim de jogo" to "Game over", "Novo recorde!" to "New record!", "Menu" to "Menu", "Mapa" to "Map",
        "Onda alcançada" to "Wave reached", "Inimigos derrotados" to "Enemies defeated", "Chefes derrotados" to "Bosses defeated",
        "Merges" to "Merges", "Dados comprados" to "Dice bought", "Recorde" to "Record", "onda %d" to "wave %d",
        "Conquista: %s" to "Achievement: %s", "Código da partida: %s" to "Match code: %s",
        "Total" to "Total", "Partidas" to "Matches", "Ondas somadas" to "Waves in total", "Abates" to "Kills", "Chefes" to "Bosses",
        "Média de ondas" to "Average wave", "Recorde por mapa" to "Record per map", "Melhores decks" to "Best decks",
        "Jogue uma partida para aparecer aqui." to "Play a match to show up here.", "Nenhum desafio jogado ainda." to "No challenge played yet.",
        "Elite" to "Elite", "Tesouro" to "Treasure", "elites" to "elites", "tesouros" to "treasures",
        "Grande, cura a escolta e vale 30 SP." to "Big, heals its escort, worth 30 SP.",
        "Foge rápido; não tira vida, mas vale 60 SP." to "Runs for it; costs no life but is worth 60 SP.",
        "A Elite curava a escolta. Mate ela primeiro com Sniper ou Ferro; depois a fila cai fácil." to "The Elite kept healing its escort. Kill it first with Sniper or Iron; the rest falls easily.",
        "O Tesouro escapou: não custa vida, mas valia 60 SP. Vento e Sniper alcançam os rápidos." to "The Treasure got away: no life lost, but it was worth 60 SP. Wind and Sniper catch the fast ones.",
        "Velozes" to "Swift", "Resistentes" to "Tough", "Pobres" to "Poor", "Enxame" to "Swarm",
        "inimigos 30% mais rápidos" to "enemies 30% faster", "inimigos com 50% mais vida" to "enemies with 50% more HP",
        "inimigos rendem metade do SP" to "enemies pay half the SP", "40% mais inimigos por onda" to "40% more enemies per wave",
        "Mutadores ×%s" to "Mutators ×%s", "Mutadores ×%s: %s" to "Mutators ×%s: %s", "Sem mutadores: recorde normal" to "No mutators: normal record",
        "Pontuação %d (onda %d × mutadores)" to "Score %d (wave %d × mutators)", "pontuação %d" to "score %d",
        "recorde: pontuação %d" to "best: score %d", "Desafios" to "Challenges", "Desafio semanal" to "Weekly challenge",
        "Semana de %s" to "Week of %s",
        "Sandbox" to "Sandbox",
        "Monte o tabuleiro como quiser: qualquer dado, qualquer pip, ondas quando pedir. Não conta para recordes." to
            "Build the board any way you like: any die, any pips, waves when you ask. Doesn't count for records.",
        "Apagar" to "Erase", "Onda" to "Wave", "Chefe" to "Boss", "Parar" to "Stop", "Limpar" to "Clear",
        "Escolha o dado" to "Pick a die", "Toque numa casa vazia para pôr %s" to "Tap an empty cell to place %s",
        "Compartilhar" to "Share", "Copiado!" to "Copied!",
        "Cheguei à onda %d no mapa %s (%s) no Merge TD." to "I reached wave %d on %s (%s) in Merge TD.",
        "Nv. %d" to "Lv. %d", "Bloqueado" to "Locked", "Abre no nível de conta %d" to "Opens at account level %d",
        "+%d%% · cartas %d/%d" to "+%d%% · cards %d/%d", "máximo · +%d%%" to "max · +%d%%", "Subir nível" to "Level up", "%s subiu para o nível %d!" to "%s is now level %d!",
        "Novo dado: %s" to "New die: %s", "Baú retroativo: %d cartas pelas partidas antigas" to "Retro chest: %d cards for your old matches",
        "Deck salvo no slot %d" to "Deck saved to slot %d", "Segure para salvar o deck aqui" to "Hold to save the deck here",
        "Reduzir movimento" to "Reduce motion", "Texto grande" to "Large text",
        "Inimigos" to "Enemies", "Como vencer" to "How to beat it",
        "vida ×%s · vel. ×%s · %d SP · tira %d" to "HP ×%s · speed ×%s · %d SP · takes %d",
        "O básico: vem em grupo." to "The basic one: comes in groups.", "Rápido e frágil." to "Fast and fragile.",
        "Lento, com muita vida." to "Slow, with lots of HP.", "Cura os inimigos ao redor." to "Heals the enemies around it.",
        "Ignora lentidão, atordoamento, empurrão e armadilhas." to "Ignores slows, stuns, pushes and traps.",
        "Um coringa! Ele combina com qualquer dado de mesmos pips." to "A wildcard! It merges with any die of the same pips.",
        "Segure um dado parado para ver o que ele faz e quanto bate." to "Hold a die still to see what it does and how hard it hits.",
        "Sinergia ativa! Os dois tipos no tabuleiro ganham um bônus." to "Synergy on! Both types on the board get a bonus.",
        "Mais um merge e esse dado chega a 7 pips, com uma habilidade nova." to "One more merge and that die hits 7 pips, with a new ability.",
        "Missões" to "Missions", "Missões de hoje" to "Today's missions", "Renova amanhã" to "New ones tomorrow",
        "Nível %d · %s" to "Level %d · %s", "%d / %d XP" to "%d / %d XP", "+%d XP" to "+%d XP", "Concluída" to "Done",
        "Novo nível: %d!" to "Level up: %d!", "Missão: %s" to "Mission: %s",
        "Mate %d inimigos com %s" to "Kill %d enemies with %s", "Chegue à onda %d no mapa %s" to "Reach wave %d on %s",
        "Faça %d merges" to "Make %d merges", "Leve um dado a 7 pips" to "Take a die to 7 pips",
        "Derrote %d chefes" to "Defeat %d bosses", "Chegue à onda %d no modo %s" to "Reach wave %d in %s mode",
        "Recruta" to "Recruit", "Soldado" to "Soldier", "Veterano" to "Veteran", "Capitão" to "Captain",
        "Comandante" to "Commander", "General" to "General", "Lenda" to "Legend",
        "Conquistas" to "Achievements", "Histórico" to "History", "%d de %d" to "%d of %d",
        "Nenhuma partida terminada ainda." to "No finished match yet.", "Moldura" to "Frame", "Céu" to "Sky", "Pips" to "Pips",
        "Padrão" to "Default", "Bronze" to "Bronze", "Prata" to "Silver", "Ouro" to "Gold", "Diamante" to "Diamond",
        "Esmeralda" to "Emerald", "Rubi" to "Ruby", "Entardecer" to "Dusk", "Aurora" to "Aurora", "Nebulosa" to "Nebula",
        "Noite dourada" to "Golden night", "Redondos" to "Round", "Quadrados" to "Square", "Losangos" to "Diamonds", "Estrelas" to "Stars",
        "Primeiro sangue" to "First blood", "Derrote um inimigo" to "Defeat an enemy", "Dez ondas" to "Ten waves", "Chegue à onda 10" to "Reach wave 10",
        "Vinte ondas" to "Twenty waves", "Chegue à onda 20" to "Reach wave 20", "Trinta ondas" to "Thirty waves", "Chegue à onda 30" to "Reach wave 30",
        "Quarenta ondas" to "Forty waves", "Chegue à onda 40" to "Reach wave 40", "Caçador de chefes" to "Boss hunter", "Derrote um chefe" to "Defeat a boss",
        "Dez chefes" to "Ten bosses", "Derrote 10 chefes no total" to "Defeat 10 bosses in total", "Mil abates" to "A thousand kills",
        "Derrote 1000 inimigos no total" to "Defeat 1000 enemies in total", "Sete pips" to "Seven pips", "Tenha um dado de 7 pips" to "Have a 7-pip die",
        "Combinação" to "Combination", "Ative uma sinergia" to "Switch on a synergy", "Caçador 50" to "Hunter 50",
        "Tenha um Caçador com 50 stacks" to "Have a Hunter with 50 stacks", "Desafiante" to "Challenger", "Jogue um desafio do dia" to "Play a daily challenge",
        "Cinco chefes seguidos" to "Five bosses in a row", "Chegue à onda 5 no Boss Rush" to "Reach wave 5 in Boss Rush",
        "Sobrevivente" to "Survivor", "Chegue à onda 15 em Sobrevivência" to "Reach wave 15 in Survival", "Veloz" to "Speedster",
        "Chegue à onda 15 no Rápido" to "Reach wave 15 in Rush", "Explorador" to "Explorer", "Onda 10 em todos os mapas" to "Wave 10 on every map",
        "Economista" to "Economist", "Acumule 1000 SP numa partida" to "Hold 1000 SP in a match",
        "Veterano" to "Veteran", "Jogue 10 partidas" to "Play 10 matches", "Fundidor" to "Smelter", "Faça 100 merges no total" to "Make 100 merges in total",
        "Fogo" to "Fire", "Gelo" to "Ice", "Veneno" to "Poison", "Elétrico" to "Electric", "Vento" to "Wind", "Ferro" to "Iron", "Mina" to "Mine",
        "Sniper" to "Sniper", "Espelho" to "Mirror", "Cresc." to "Growth", "Crescimento" to "Growth", "Luz" to "Light", "Crítico" to "Critical",
        "Combo" to "Combo", "Maldição" to "Curse", "Caçador" to "Hunter", "Gravid." to "Gravity", "Gravidade" to "Gravity", "Tempo" to "Time",
        "Armad." to "Trap", "Armadilha" to "Trap", "Terrem." to "Quake", "Terremoto" to "Earthquake", "Ceifador" to "Reaper", "Nova" to "Nova",
        "Supernova" to "Supernova", "Sorte" to "Luck", "Vampiro" to "Vampire", "Clonador" to "Cloner", "Reforço" to "Boost",
        "Soldado" to "Soldier", "Corredor" to "Runner", "Tanque" to "Tank", "Curandeiro" to "Healer", "Fantasma" to "Ghost",
        "Silenciador" to "Silencer", "Trocador" to "Swapper", "Regenerador" to "Regenerator", "Divisor" to "Splitter",
        "Blindado" to "Armored", "Ladrão" to "Thief", "Comandante" to "Commander",
        "Cala um dado a cada 6 s" to "Silences a die every 6 s", "Troca dois dados de lugar a cada 6 s" to "Swaps two dice every 6 s",
        "Cura 2% da vida por segundo" to "Heals 2% HP per second", "Vira dois tanques ao morrer" to "Splits into two tanks on death",
        "Ignora golpes fracos; veneno passa" to "Shrugs off weak hits; poison gets through",
        "Rouba SP a cada 6 s; devolve ao morrer" to "Steals SP every 6 s; gives it back on death",
        "Acelera os inimigos perto dele" to "Hastes the enemies near it",
        "Incêndio" to "Blaze", "raio da explosão do Fogo +50%" to "Fire splash radius +50%",
        "Estilhaço" to "Shatter", "Terremoto dá dano ×2 em inimigos lentos" to "Earthquake deals ×2 to slowed enemies",
        "Praga" to "Plague", "veneno +50% de dano por segundo" to "poison +50% damage per second",
        "Ofuscar" to "Dazzle", "+10% de chance de crítico" to "+10% crit chance",
        "Choque" to "Shock", "cada salto do raio congela 0,2 s" to "each lightning jump freezes for 0.2 s",
        "Perfurante" to "Piercing", "Ferro e Sniper ignoram armadura e dão +25% em chefes" to "Iron and Sniper ignore armour and deal +25% to bosses",
        "Predador" to "Predator", "Caçador e Vampiro +25% de dano" to "Hunter and Vampire +25% damage",
        "Emboscada" to "Ambush", "raio de disparo das minas ×2" to "mine trigger radius ×2",
        "%s (com %s): %s" to "%s (with %s): %s",
        "Alvo" to "Target", "Dano" to "Damage", "Velocidade" to "Speed", "Efeito" to "Effect", "7 pips" to "7 pips", "Sinergia" to "Synergy", "Power-up" to "Power-up",
        "Toque num dado para ver tudo · arraste para montar o deck" to "Tap a die to see everything · drag to build the deck",
        "1 → %d pips" to "1 → %d pips", "por mina, %s" to "per mine, %s", "não ataca" to "doesn't attack", "entre ataques" to "between attacks",
        "Nenhum" to "None", "Mais vida" to "Most HP", "o mais resistente" to "the toughest one", "Todos" to "All", "de uma vez" to "at once",
        "Caminho" to "Path", "minas à frente" to "mines ahead", "Primeiro" to "Leader", "o mais avançado" to "the furthest along",
        "%s de SP gerado (níveis 2 a 5)" to "%s SP generated (levels 2 to 5)", "%s no dano e no veneno (níveis 2 a 5)" to "%s to damage and poison (levels 2 to 5)",
        "%s no dano das explosões (níveis 2 a 5)" to "%s to blast damage (levels 2 to 5)", "%s de dano (níveis 2 a 5)" to "%s damage (levels 2 to 5)",
    ) + DIE_TEXTS + TIPS

    /** Summaries, effects and 7-pip lines of the dice, plus the tips: long texts, kept apart for reading. */
    private val DIE_TEXTS: Map<String, String> get() = DieTextsEn.TABLE
    private val TIPS: Map<String, String> get() = TipsEn.TABLE
}
