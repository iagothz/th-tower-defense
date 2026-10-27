# Merge Tower Defense — Design

Data: 2026-10-26

## Objetivo

Um tower defense de merge no estilo Random Dice (111%), para jogar **offline no Android**, por diversão. Single-player, sem nenhum recurso online.

**Considero pronto quando:** você instala o APK, monta um deck de 5 dados, joga uma partida inteira de ondas infinitas com compra, merge e power-ups, perde e vê o recorde salvo.

### Escopo da v1

- Núcleo da partida: grid 3×5, compra de dado aleatório, merge, ondas, chefes, vidas e game over.
- Deck de 5 dados escolhidos entre 10 tipos.
- Power-up por tipo, pago com SP durante a partida.
- Modo de ondas infinitas, com recorde salvo.

### Fora do escopo da v1

Moeda ou progressão entre partidas, desbloqueios, PvP/IA, co-op, recursos online, sprites e som. A arte é feita por código, e a camada de skin fica isolada para trocar por sprites depois.

## Arquitetura

Kotlin + libGDX, projeto Gradle multi-módulo no formato do gdx-liftoff:

```
th-tower-defense/
├─ core/     jogo (Kotlin)
│  ├─ logic/ regras puras, sem dependência de libGDX
│  └─ ui/    telas, renderização, input, HUD
├─ android/  launcher Android → APK
└─ lwjgl3/   launcher desktop (desenvolvimento/testes no PC)
```

- **`logic`** é Kotlin puro. A classe `Match` expõe ações (`buyDie()`, `merge(from, to)`, `powerUp(type)`) e `update(dt)`, que avança em passos fixos de 1/60 s. O RNG é injetado (`Random` com seed) para os testes serem determinísticos. Todos os números ficam em `Balance.kt`.
- **`ui`** lê o estado de `Match` e desenha com `ShapeRenderer` (com MSAA 4x) e `BitmapFont`. As cores de cada tipo ficam em `Skin` (nenhum par de dados a menos de 0,2 de distância RGB, garantido por teste), os ícones em `DieGlyph` (primitivas num quadrado unitário) e o desenho do dado em `DieDraw`; tudo isso pode ser trocado por sprites depois. `Effects` cuida do feedback (números, anéis de explosão, partículas, banner), `Backdrop` do fundo e `Fader` da transição entre telas.
- **Persistência:** `Preferences` do libGDX, guardando o recorde por mapa (`best.<id>`; o antigo `bestWave` vale para a Ferradura), `map`, `deck` (lista de 5 IDs de tipo) e a partida em andamento.
- **Ambiente:** Android SDK 36, build-tools 35/36. O AGP exige JDK 17 ou 21; se o JDK 25 do Android Studio for rejeitado, configurar uma toolchain Gradle 17/21.

## Regras da partida

**Tabuleiro**
- Grid de 3 linhas × 5 colunas no centro da tela, em retrato.
- O caminho é um "U" invertido em volta do grid: entra embaixo à esquerda, sobe, cruza por cima e desce à direita até a saída.

**Vidas**
- 3 vidas.
- Um inimigo comum que chega à saída tira 1 vida; um chefe tira 2.
- Com 0 vidas, é game over.

**SP**
- Começa com 150 SP.
- Recompensa por inimigo morto: normal 10, rápido 10, tanque 20, chefe 100.

**Compra**
- Custa 10 SP na primeira compra e sobe +10 a cada compra.
- Gera um dado de tipo aleatório do deck, com 1 pip, numa casa vazia aleatória.
- Fica desabilitada se não houver SP suficiente ou casa vazia.

**Merge**
- Só funciona entre dados do mesmo tipo e com o mesmo número de pips, menor que 7.
- O resultado ocupa a casa de destino, com pips+1 e tipo aleatório do deck.
- A casa de origem fica vazia.
- Se um dos dados estava desativado pelo Silenciador, o resultado continua desativado pelo tempo restante maior.
- Um merge inválido não muda nada.

**Ataque**
- Cada dado que ataca dispara contra o inimigo mais avançado no caminho. O Sniper é a exceção (ver abaixo).
- Intervalo de ataque = `baseInterval(tipo) / (1 + 0.15 × (pips − 1))`.
- Dano = `baseDamage(tipo) × pips × powerMult(nível)`.
- Projéteis são instantâneos (hitscan). O traço visual varia por tipo (raio em zigue-zague, bala do Sniper, anel do Fogo), mas é só cosmético.

**Power-up por tipo**
- Níveis 1 a 5; custo para subir: 100 / 200 / 400 / 700 SP.
- `powerMult` = 1.0 / 1.3 / 1.6 / 2.0 / 2.5.
- Afeta todos os dados do tipo, inclusive os que vierem depois.

**Ondas**
- A onda `n` tem `8 + 2n` inimigos (máximo 40), surgindo a cada 0,6 s.
- Composição: rápidos a partir da onda 3, tanques a partir da onda 5.
- Vida = `base × 1.09^(n−1)`.
- A próxima onda começa 3 s depois que a anterior foi totalmente gerada e eliminada.
- **Chefe** a cada 10 ondas (10, 20, …), sozinho. Vida = 40× a de um normal da mesma onda.
- Os chefes alternam na ordem Silenciador → Trocador → Regenerador.

**Recorde:** maior onda alcançada.

## Dados (25 tipos)

| Tipo | Cor | Efeito |
|---|---|---|
| Fogo | vermelho | Dano em área (raio pequeno) em volta do alvo |
| Gelo | azul claro | Lentidão de 15% + 5% × pips (20% com 1 pip, 50% com 7); não acumula, vale o efeito mais forte |
| Veneno | verde | Dano por segundo; acumula até 5 aplicações |
| Elétrico | amarelo | Salta para mais 2 inimigos próximos, com 70% do dano a cada salto |
| Vento | ciano | Dano baixo e intervalo de ataque muito curto |
| Ferro | cinza | Dano ×2 contra chefes |
| Mina | dourado | Minerador: não ataca; gera `10 × pips × powerMult` SP a cada 5 s |
| Sniper | roxo | Dano alto e ataque lento; mira o inimigo com mais vida |
| Espelho | branco | Ataque fraco. Coringa: faz merge com qualquer dado de mesmos pips; o resultado é sorteado do deck, como num merge normal |
| Crescimento | rosa | A cada 45 s (dividido pelo multiplicador do power-up: 34,6 / 28,1 / 22,5 / 18 s nos níveis 2 a 5, `Match.growthInterval`) ganha +1 pip e vira um tipo sorteado do deck (pode continuar Crescimento); para em 7 pips; fraco nos outros atributos |
| Luz | amarelo-claro | Ataque fraco; os vizinhos ortogonais atacam +15% mais rápido por pip da Luz (somam) |
| Crítico | laranja | Ataque fraco; cada pip dá +3% de chance de crítico (dano x3) a todos os dados, até 60% |
| Combo | azul | +30% de dano para cada outro Combo no tabuleiro |
| Maldição | roxo-escuro | O alvo recebe 15% + 3% × pips a mais de dano de tudo (inclusive veneno) por 3 s; não acumula |
| Caçador | marrom | +1 stack por abate, +5% de dano por stack; limite de 10 stacks por pip (60 com 6) e sem limite com 7 pips; merge de Caçadores soma os stacks se o resultado for Caçador |
| Gravidade | azul-marinho | Empurra o alvo 40 + 10 × pips para trás; cada inimigo pode ser empurrado no máximo 300 no total (chefes: 25% de tudo) |
| Tempo | verde-água | Congela o alvo 0,3 + 0,1 × pips s (chefes: metade); não renova um congelamento ativo e deixa o inimigo imune por 1 s depois |
| Armadilha | oliva | Planta mina 150 à frente do primeiro inimigo; explode quando alguém pisa (raio 60); máx. 3 por dado |
| Terremoto | bege | Cada ataque acerta todos os inimigos |
| Ceifador | grafite | Inimigo comum abaixo de 5% + 2% × pips da vida morre na hora |
| Supernova | magenta | Inimigo morto por ela explode (raio 90) com o dano do dado; mortes na explosão também explodem |
| Sorte | verde-limão | Dano sorteado de 0x a 4x a cada ataque |
| Vampiro | vinho | +1 SP por pip a cada abate, além da recompensa |
| Clonador | lilás | Ataque fraco. Arrastado sobre um dado de mesmos pips (ou o contrário), vira uma cópia dele (tipo e pips, sem stacks) e o original fica; funciona até com 7 pips; dois Clonadores fazem merge normal |
| Reforço | verde-petróleo | Ataque fraco. Coringa garantido: faz merge com qualquer dado de mesmos pips e o resultado é o tipo do outro dado com +1 pip; dois Reforços viram um Reforço |

Os valores base (dano e intervalo) ficam em `Balance.kt`. O dado que deu o último golpe recebe o crédito do abate (Vampiro, Caçador, Supernova). Gravidade e Tempo têm limites para nenhum inimigo ficar preso para sempre.

## Mapas

O tabuleiro, o HUD e os botões nunca mudam de lugar; cada mapa é um caminho diferente nas faixas livres em volta (x=44 e x=676 ao lado do tabuleiro, y=880..1020 acima, y=245 abaixo) com um tema de chão e céu. O jogador escolhe na tela **Mapa**; o recorde é guardado por mapa, e o recorde de antes dos mapas vale para a Ferradura. A partida salva guarda o mapa; um save sem mapa abre na Ferradura. A dificuldade é medida no simulador (bot smart, deck padrão, 1000 partidas), não pelo comprimento.

| Mapa | Traçado | Comprimento | Mediana | Dificuldade | Tema |
|---|---|---|---|---|---|
| Ferradura | sobe pela esquerda, cruza em cima, desce pela direita | 1984 | 22 | Normal | floresta |
| Serpente | duas passagens pela área de cima, desce pela esquerda | 2104 | 22 | Normal | deserto |
| Anel | do topo pelo meio, desce pela direita, passa por baixo do tabuleiro, sobe pela esquerda | 2674 | 25 | Fácil | gelo |
| Reta | cruza em cima e desce pela direita | 1332 | 16 | Difícil | vulcão |

## Sinergias, 7 pips e modos

- **Sinergias** (`Synergy`): oito pares ativos enquanto os dois tipos estão no tabuleiro sem silêncio: Incêndio (Fogo+Vento, raio ×1,5), Estilhaço (Gelo+Terremoto, ×2 em lentos), Praga (Veneno+Maldição, veneno ×1,5), Ofuscar (Luz+Crítico, +10% crítico), Choque (Elétrico+Tempo, saltos congelam 0,2 s), Perfurante (Ferro+Sniper, ignoram armadura, +25% em chefes), Predador (Caçador+Vampiro, +25%), Emboscada (Gravidade+Armadilha, disparo das minas ×2). Etiquetas na borda do tabuleiro; lista na tela Deck e na descrição do dado.
- **7 pips**: cada tipo ganha uma habilidade própria (anel dourado no dado; linha "7 pips" em `DieInfo.maxed`). Constantes em `Balance` (`*_MAX`, `ICE_FREEZE`, `TIME_AREA`, `BOOST_INTERVAL`...).
- **Modos** (`GameMode`), escolhidos na tela Partida e guardados no save: Normal; Boss Rush (um chefe por onda, 35% da vida); Sobrevivência (1 vida, SP ×2); Rápido (intervalo 1 s, spawn 2× mais rápido, inimigos ×1,3 de velocidade e ×0,6 de vida). Recorde por mapa e modo (`best.<mapa>.<modo>`). Simulador com deck padrão: Normal 20, Boss Rush 14, Sobrevivência 20, Rápido 21.

## Conquistas e cosméticos

- 20 conquistas (`Achievement`), avaliadas no fim de cada partida a partir de `AchievementProgress` (resultado, modo, flags observadas no tabuleiro por `MatchStats.observe`, totais do save). Guardadas como `ach.<id>`.
- 13 delas destravam um cosmético só visual (`Cosmetic`): 6 molduras do tabuleiro, 4 tons de céu (tinta multiplicada no gradiente), 3 formas de pip. Equipado por slot em `cosmetic.<slot>`, lido pelos renderers via `Cosmetics`. Tela Conquistas (botão em Estatísticas): toque numa linha destravada para equipar; de novo para voltar ao padrão.

## Histórico e código de partida

- Toda partida terminada entra no **histórico** (`history`, últimas 20): dia, mapa, modo, deck, onda, quem matou e o código. Tela Histórico (botão em Estatísticas).
- Toda partida normal nasce de uma seed guardada (`Match.seeded`, linha `seed` no save). `MatchCode` escreve mapa, modo, deck e seed em 14 letras base32 (Crockford, sem I/L/O/U) com letra de verificação: "XXXX-XXXX-XXXX-XX". O fim de jogo mostra o código; "Jogar código" na tela Partida reproduz a mesma partida (`locked`: conta só para os totais, não para recordes). O desafio do dia não gera código.

## Meta, tutorial e áudio

- **Tutorial** na primeira partida (`Tutorial`): Comprar → juntar → power-up, cada balão some ao fazer; Pular; `tutorialDone` no save.
- **Dica de fim de jogo** (`Tips`) conforme o último inimigo que vazou (`MatchStats.killedBy`).
- **Estatísticas** (`StatsScreen`): totais (`total.*`), recorde por mapa, melhores decks (`deckbest.<deck>`) e últimos desafios.
- **Desafio do dia** (`DailyChallenge.of(dia)`): o dia desde 1970 sorteia mapa e 5 dados e fixa a seed (`dia × 1 000 003`); `Match.challengeDay` vai no save (`daily`), e o resultado conta só para `daily.<dia>` e os totais.
- **Animações**: o dado comprado cai de 90 unidades acima, quica uma vez e aterrissa achatado (`Effects.dropOffset`, `dieScale`: squash de 28% que oscila e assenta em 0,22 s); no merge o dado arrastado desliza até o alvo por 0,14 s e o resultado só aparece depois (`Effects.slide`).
- **Toque do merge** (`Effects`): a partida congela por 0,06 s (`timeScale`, `HIT_STOP`), a célula pisca branca por 0,18 s (`cellFlash`) e o dado novo aterrissa com squash. Chegar a **7 pips** soma 30 fagulhas douradas, um anel, tremor curto, o texto "Máximo!" e o som `max`. O arrasto desenha o dado 12% maior com sombra mais baixa, os alvos compatíveis pulsam e uma célula compatível a até 64 unidades do dedo puxa o dado (`DragController.snapTarget`/`dropTarget`, contorno dourado); soltar no vão entre células ainda faz o merge.
- **Impacto do combate**: o dado recua 5 unidades contra o alvo ao atirar e volta em 0,12 s (`Effects.kick`); golpes pesados (Ferro, Sniper, Gravidade e críticos) jogam o inimigo 7 unidades para trás no caminho e o incham 18% por 0,12 s (`jolt`); crítico aparece 60% maior e encolhe; a bala de Ferro deixa três fantasmas; o raio tremula; o Fogo deixa 6 brasas que sobem por 0,8 s; morte de chefe: 60 partículas, dois anéis, tremor 9 e câmera lenta a 30% por 0,45 s (`SLOWMO`), com som `bosskill` e vibração de 60 ms. Inimigo morto incha 30% antes de encolher.
- **Tensão das ondas**: nos últimos 3 s da contagem a barra vai do azul ao vermelho, o texto pula e tocam `tick_3`/`tick_2`/`tick_1` (`SoundPlanner.countdown`). Vinheta nas bordas (`Effects.vignetteAlpha`/`vignetteColor`, faixas de 190 unidades que escurecem para fora): vermelha por 0,4 s ao vazar, escura enquanto o cartão do chefe está na tela, e um pulso vermelho lento (0,06..0,34, 4 rad/s) enquanto resta uma vida fora de Sobrevivência (`lastLife`). Ao vazar o coração do HUD incha 7 unidades e treme (`pulse("lives")`); entrada de chefe abaixa a música a 20% e ela volta em ~1 s (`Sfx.DUCK`); a barra do chefe enche a partir do vazio quando ele aparece.
- **HUD vivo**: SP rola até o valor real (`Counter`, aproximação exponencial, encaixe a < 0,5); onda, vidas e SP roubado pulam 35% (`pulse`); Comprar respira 2% enquanto dá para comprar; tocar num botão sem SP faz ele tremer de lado por 0,25 s com o som `deny` (`Button.deny`); o afundar do botão é suavizado (`Button.press`); power-up acende 45% e dá um pulo de 10% em todo dado daquele tipo por 0,5 s (`typeGlow`) e o botão do tipo infla 8%.
- Tecla de teste **H** perde uma vida (`Match.devLeak`) para ver o aviso de vazamento.
- **Momentos-chave**: a última vida congela a partida por 1,1 s (`MatchScreen.ending`), com tremor 16, vinheta vermelha cheia (`Effects.gameOver`) e o som `gameover`, e só então vai para o fim de jogo; lá o título entra 2,2× maior e assenta em 0,3 s, os números contam de zero um após o outro (`Counter`, a partir de 0,3 s, 0,12 s entre linhas) e um recorde toca `record` além do confete. **Onda limpa**: quando o último inimigo morre antes da próxima onda o `Match` emite `WaveCleared`, com faixa "Onda limpa!" por 0,9 s e o som `clear`. A barra do chefe pisca abaixo de 20%.
- **Micro-detalhes**: Sniper, Ferro e críticos soltam 5 faíscas no ponto de impacto; a câmera aproxima 5% no pico da câmera lenta do chefe (`BaseScreen.cameraZoom`); Desfazer aparece crescendo de 70% (`pulse("undo")`); golpes no mesmo inimigo dentro de 0,35 s somam no mesmo número com um "×N" ao lado (`STACK_WINDOW`); o congelamento, o flash e o squash do merge esperam o dado deslizante chegar na célula (`Effects.land`, agendado pelo slide).
- **Desfazer**: o último merge que consumiu dois dados pode ser desfeito por 3 s (`Match.undoMerge`), desde que o resultado ainda esteja na célula e a origem vazia; clonar não desfaz. Botão Desfazer ao lado de Comprar enquanto vale.
- **Clima por mapa** (`Weather`): vagalumes na Ferradura, areia na Serpente, neve no Anel, brasas na Reta, atrás de todas as telas. Cartão do chefe no banner (nome e o que ele faz, 2,6 s), corpo encolhendo ao morrer, balanço ao andar e sombra no chão.
- O tutorial não aparece no Desafio do dia; a partida começa com o aviso "deck e mapa do dia".
- **Áudio**: som de ataque por família (`SoundPlanner.family`: fogo, gelo, raio, vento, pesado, mágico) e um loop de 19 s (`sounds/music.wav`) gerado por `tools/gen_sounds.py`. O merge toca `merge_2`..`merge_6` (o mesmo arpejo, dois semitons mais alto por pip) e `max` em 7 pips; hits e kills variam o pitch em ±10% (`Sfx.PITCH_SPREAD`); vibração por evento (abaixo).
- **Música em camadas** (`MusicMix`, `Sfx.updateMusic`): a base (`music.wav`) e uma camada de bateria (`music_drums.wav`, bumbo nos tempos 1 e 3, caixa no 2 e 4) tocam juntas e sincronizadas (ressincronia se desviarem mais de 0,08 s); a bateria sobe a 1 por segundo enquanto há chefe vivo e desce a 0,7 por segundo quando ele morre. O modo Rápido usa o par `music_fast`/`music_drums_fast` a 118 BPM em vez de 100. Toda tela fora da partida volta ao loop calmo (`Sfx.calm` ao trocar de tela). Pegar um dado toca `pick`; soltar sem merge toca `drop`.
- **Vibração** (`Haptics.pulse`, com amplitude no Android 8+ e só duração nos outros): merge 15 ms/60, negar 20 ms/90, chefe 50 ms/130, recorde 60 ms/160, 7 pips 40 ms/180, matar chefe 70 ms/255, vazar 80 ms/255, fim de jogo 120 ms/255; num mesmo quadro vale o pulso mais forte.
- **Configurações** (`SettingsScreen`, botão Configurações no menu): controles deslizantes de volume do som e da música (`soundVolume`/`musicVolume`, 0..1, música desligada em 0), Vibrar e idioma (`language`: automático, português, inglês), mais a linha de privacidade.
- **Idiomas** (`Strings.kt`): o texto em português é a chave; `S["texto"]` devolve o inglês da tabela `EN` quando o idioma é inglês e o próprio texto caso contrário, e `S.f(formato, args)` formata depois de traduzir. Automático escolhe português quando o aparelho está em `pt` e inglês para o resto. Descrições dos dados e dicas ficam em `DieTextsEn`/`TipsEn`; `StringsTest` garante que toda chave tem tradução.
- **Loja**: release com R8 (`android/proguard-rules.pro` mantém backend, freetype, utils e os construtores sem argumentos que o `Pools` cria por reflexão), AAB por `bundleRelease` e `PRIVACY.md` (pt/en) como política de privacidade.

## Inimigos

| Inimigo | Vida (relativa) | Velocidade | Observação |
|---|---|---|---|
| Normal | 1.0 | 1.0 | — |
| Rápido | 0.5 | 2.0 | — |
| Tanque | 3.0 | 0.6 | — |
| Chefe Silenciador | 40 | 0.5 | A cada 6 s, desativa um dado aleatório por 5 s |
| Chefe Trocador | 40 | 0.5 | A cada 6 s, troca a posição de 2 dados aleatórios |
| Chefe Regenerador | 40 | 0.5 | Cura 2% da vida máxima por segundo |
| Curandeiro (onda 8+) | 1.2 | 0.9 | Cura 2% da vida máxima por segundo dos inimigos a até 90 dele no caminho |
| Fantasma (onda 12+) | 0.8 | 1.3 | Ignora lentidão, congelamento, empurrão e armadilhas (a Supernova ainda pega) |
| Chefe Divisor | 40 | 0.5 | Ao morrer deixa dois tanques com 25% da vida dele cada |
| Chefe Blindado | 40 | 0.5 | Ignora os primeiros 15% da vida de um soldado daquela onda em cada golpe direto; veneno passa |
| Chefe Ladrão | 40 | 0.5 | A cada 6 s rouba 15% do SP; matá-lo devolve tudo |
| Chefe Comandante | 40 | 0.5 | Inimigos a até 150 dele no caminho andam 40% mais rápido |

A ordem dos 7 chefes é sorteada por partida (onda 10, 20, 30... seguem a lista; fica no save). Ondas 5, 15, 25... são especiais, rodando **Horda** (40 soldados com metade da vida), **Tanques** (um terço da onda, só tanques) e **Corrida** (só corredores); o banner e a prévia dizem qual. Com o deck padrão no simulador, a mediana caiu de 22 para 20 com tudo isso.

## Deck

- Escolha de exatamente 5 dos 10 tipos.
- Deck padrão no primeiro uso: Fogo, Gelo, Veneno, Elétrico, Mina.
- O deck fica salvo em `Preferences`.

## Telas e controles

**Fluxo:** Menu (Jogar / Deck / Partida / Desafio / Estatísticas / Configurações) → Deck → Partida → Game over → Menu.
- **Deck:** grade com os 10 tipos; tocar alterna a seleção e mostra, num painel, o que o dado faz: alvo, dano, velocidade de ataque, efeito e power-up (números lidos do `Balance.kt`). O botão Confirmar só fica ativo com 5 tipos escolhidos.
- **Game over:** mostra a onda alcançada e "Novo recorde!" quando for o caso.

**Partida**
- Viewport virtual de 720×1280 (`FitViewport`), em retrato.
- Topo: onda, vidas e SP.
- Centro: o caminho em volta do grid.
- Embaixo: botão Comprar (com o preço) e 5 botões de power-up mostrando o nível e o custo.
- Botão de pausa. O jogo pausa sozinho quando o app vai para segundo plano e salva a partida em `Preferences`. Se o Android fechar o app nesse meio-tempo, ao reabrir a partida volta pausada de onde parou. Sair da partida ou perder apaga esse save.

**Arrasto**
- Arrastar um dado destaca os alvos compatíveis.
- Soltar sobre um alvo válido faz o merge; soltar em qualquer outro lugar devolve o dado.
- Segurar um dado parado por 0,45 s abre um cartão com nome, pips, dano e intervalo atuais (com power-up e Luz) e o efeito; mexer o dedo vira arraste e fecha o cartão.
- HUD: botão 2x (velocidade da simulação), pausa com Continuar / Reiniciar / Sair, e os dois últimos pedem confirmação. Entre ondas, a prévia da próxima (quantos de cada tipo) e em quantas ondas vem o chefe.
- Vibração curta ao vazar (80 ms) e no início de um chefe (40 ms), desligável no menu.
- O mundo 720×1280 usa `ExtendViewport`: telas mais altas ganham céu acima e abaixo em vez de barras pretas; HUD e tabuleiro ficam onde estão.

## Testes

- **JUnit 5 no pacote `logic`,** com RNG semeado:
  - custo de compra e casa cheia;
  - merge válido e inválido, máximo de 7 pips;
  - Entre dois coringas vale o efeito do dado arrastado (`Board.actor`): Reforço sobre Clonador sobe o Clonador, Clonador sobre Reforço clona o Reforço, Espelho sobre Reforço sorteia; dois Clonadores fazem merge normal.
  - Espelho (sorteio), Reforço (tipo garantido) e Clonador (cópia, original fica);
  - Crescimento (vira tipo do deck ao ganhar pip);
  - SP de abates e da Mina;
  - power-up (custo e multiplicador);
  - fórmulas de ataque;
  - efeito de cada tipo (área, lentidão, veneno, corrente, chefe, mira do Sniper);
  - ondas (contagem, composição, escala de vida, chefes a cada 10);
  - vidas e game over.
- **Verificação manual** pelo launcher desktop: jogar uma partida até o game over.
- **Entrega:** `./gradlew android:assembleDebug` gera o APK debug para instalar no celular.
