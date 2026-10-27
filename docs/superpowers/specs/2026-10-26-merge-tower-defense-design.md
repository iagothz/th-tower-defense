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
- **`ui`** lê o estado de `Match` e desenha com `ShapeRenderer` e `BitmapFont`. As cores e formas de cada tipo ficam numa `Skin`, que pode ser substituída por sprites depois.
- **Persistência:** `Preferences` do libGDX, guardando `bestWave` e `deck` (lista de 5 IDs de tipo).
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
- Projéteis são instantâneos (hitscan), com um traço visual curto.

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

## Dados (10 tipos)

| Tipo | Cor | Efeito |
|---|---|---|
| Fogo | vermelho | Dano em área (raio pequeno) em volta do alvo |
| Gelo | azul claro | Lentidão de 15% + 5% × pips (20% com 1 pip, 50% com 7); não acumula, vale o efeito mais forte |
| Veneno | verde | Dano por segundo; acumula até 5 aplicações |
| Elétrico | amarelo | Salta para mais 2 inimigos próximos, com 70% do dano a cada salto |
| Vento | ciano | Dano baixo e intervalo de ataque muito curto |
| Ferro | cinza | Dano ×2 contra chefes |
| Mina | dourado | Não ataca; gera `10 × pips × powerMult` SP a cada 5 s |
| Sniper | roxo | Dano alto e ataque lento; mira o inimigo com mais vida |
| Espelho | branco | Ataque fraco. Pode fazer merge com qualquer dado de mesmos pips, e o resultado tem o tipo do outro dado. Dois Espelhos geram um tipo aleatório |
| Crescimento | rosa | A cada 60 s, ganha +1 pip (máx. 7); fraco nos outros atributos |

Os valores base (dano e intervalo) ficam em `Balance.kt`.

## Inimigos

| Inimigo | Vida (relativa) | Velocidade | Observação |
|---|---|---|---|
| Normal | 1.0 | 1.0 | — |
| Rápido | 0.5 | 2.0 | — |
| Tanque | 3.0 | 0.6 | — |
| Chefe Silenciador | 40 | 0.5 | A cada 6 s, desativa um dado aleatório por 5 s |
| Chefe Trocador | 40 | 0.5 | A cada 6 s, troca a posição de 2 dados aleatórios |
| Chefe Regenerador | 40 | 0.5 | Cura 2% da vida máxima por segundo |

## Deck

- Escolha de exatamente 5 dos 10 tipos.
- Deck padrão no primeiro uso: Fogo, Gelo, Veneno, Elétrico, Mina.
- O deck fica salvo em `Preferences`.

## Telas e controles

**Fluxo:** Menu (Jogar / Deck / Recorde) → Deck → Partida → Game over → Menu.
- **Deck:** grade com os 10 tipos; tocar alterna a seleção e mostra, num painel, o que o dado faz: alvo, dano, velocidade de ataque, efeito e power-up (números lidos do `Balance.kt`). O botão Confirmar só fica ativo com 5 tipos escolhidos.
- **Game over:** mostra a onda alcançada e "Novo recorde!" quando for o caso.

**Partida**
- Viewport virtual de 720×1280 (`FitViewport`), em retrato.
- Topo: onda, vidas e SP.
- Centro: o caminho em volta do grid.
- Embaixo: botão Comprar (com o preço) e 5 botões de power-up mostrando o nível e o custo.
- Botão de pausa. O jogo pausa sozinho quando o app vai para segundo plano.

**Arrasto**
- Arrastar um dado destaca os alvos compatíveis.
- Soltar sobre um alvo válido faz o merge; soltar em qualquer outro lugar devolve o dado.

## Testes

- **JUnit 5 no pacote `logic`,** com RNG semeado:
  - custo de compra e casa cheia;
  - merge válido e inválido, máximo de 7 pips;
  - Espelho (com outro tipo e com outro Espelho);
  - Crescimento;
  - SP de abates e da Mina;
  - power-up (custo e multiplicador);
  - fórmulas de ataque;
  - efeito de cada tipo (área, lentidão, veneno, corrente, chefe, mira do Sniper);
  - ondas (contagem, composição, escala de vida, chefes a cada 10);
  - vidas e game over.
- **Verificação manual** pelo launcher desktop: jogar uma partida até o game over.
- **Entrega:** `./gradlew android:assembleDebug` gera o APK debug para instalar no celular.
