# Padrão visual (parte 1 da revisão de UI/UX)

**Data:** 2026-11-06 · **Status:** implementado (plano `docs/superpowers/plans/2026-11-06-visual-standard.md`)

## Contexto

Auditoria das 14 telas (Início, Deck e janela do dado, Mapas, Desafios, Config, Estatísticas, Conquistas, Histórico, Inimigos, partida, pausa, fim de jogo) agrupou os problemas em quatro partes: **padrão visual**, tela da partida, fim de jogo e organização das telas. Esta spec cobre só a primeira; as outras terão spec própria e vão usar o que esta cria.

**Objetivo:** toda tela fala a mesma língua visual. Quem aprende uma tela já sabe usar as outras: onde fica o título, como voltar, qual é a ação principal, o que é um interruptor.

**Considero pronto quando:** todas as telas usam o mesmo cabeçalho, nenhuma subtela tem botão Voltar embaixo, o botão voltar do Android navega em vez de fechar o jogo, os três ajustes de Config são interruptores, nenhum texto sobre fundo colorido fica ilegível, e as cores de interface vêm de um lugar só.

**Fora desta parte:** a faixa vazia acima dos títulos em celulares altos (é a altura extra do `ExtendViewport`; aproveitá-la exige refazer o layout de cada tela, o que fica nas partes "organização das telas" e "tela da partida"); mudanças de layout da partida e do fim de jogo; mover Estatísticas/Conquistas/Histórico de lugar.

## 1. Cabeçalho único (`ScreenHeader`)

Novo componente em `ui/ScreenHeader.kt`, usado por toda tela que hoje desenha `textShadowed(game.bigFont, título, 360f, 1222f)`:

- **Título:** `bigFont` centrado em (360, 1222), com sombra, como hoje.
- **Subtítulo opcional:** `smallFont` cor `Skin.MUTED`, centrado em (360, 1176) (Conquistas usa para "7 de 19").
- **Seta de voltar opcional:** círculo `Skin.PANEL` de raio 30 centrado em (60, 1222) com uma seta branca para a esquerda (haste e ponta desenhadas com `rectLine`/`triangle`); área de toque 88×88 em volta. Afunda ao pressionar como um `Button`.
- API: `ScreenHeader(title: String, subtitle: String? = null, back: Boolean = false)`, com `drawShapes(shapes)`, `drawText(batch, game)` e `backHit(p: Vec2): Boolean` (+ estado de pressionado via `Tap`).

Telas com seta (subtelas): **Estatísticas, Conquistas, Histórico, Inimigos**. Abas (Início não tem título; Deck, Mapas, Desafios, Config) usam o cabeçalho sem seta. Partida, pausa, fim de jogo e conflito da nuvem ficam como estão nesta parte.

Os botões **Voltar** de baixo saem de Estatísticas, Conquistas, Histórico e Inimigos. Em Estatísticas ficam só Conquistas e Histórico, como botões secundários lado a lado, centrados.

## 2. Botão voltar do Android

Hoje o libGDX não captura a tecla BACK e o Android fecha o jogo de qualquer tela. Passa a valer:

| Situação | Voltar faz |
|---|---|
| Janela do dado aberta (Deck) | fecha a janela |
| Partida rodando | abre a pausa |
| Partida pausada (sem confirmação) | continua a partida |
| Pausa com confirmação de Reiniciar/Sair | fecha a confirmação |
| Fim de jogo | vai para o Início |
| Subtela (Estatísticas, Conquistas, Histórico, Inimigos) | volta para a tela de onde veio |
| Aba que não é o Início (Deck, Mapas, Desafios, Config) | vai para o Início |
| Conflito da nuvem | nada (a escolha é obrigatória) |
| Início | sai do jogo (`Gdx.app.exit()`) |

- `Gdx.input.setCatchKey(Input.Keys.BACK, true)` uma vez no `create()` do `MergeTdGame`; `Input.Keys.ESCAPE` faz o mesmo no desktop.
- `BaseScreen` ganha `open fun onBack(): Boolean` (true = tratou). O padrão usa `backAction` com `ScreenKind.TAB` para telas com `tab` diferente de `HOME` e `ScreenKind.HOME` para o Início; as outras telas sobrescrevem. A tecla chega pelo `keyDown` que o `BaseScreen` já tem; quando `onBack()` devolve false, o jogo chama `Gdx.app.exit()`.
- "Tela de onde veio": Conquistas e Histórico são abertos por Config e por Estatísticas, e hoje sempre voltam para Estatísticas. Os construtores passam a receber `back: () -> Unit` (`showAchievements(back)`, `showHistory(back)`): Config passa `{ showSettings() }`, Estatísticas passa `{ showStats() }`. Estatísticas volta para Config e Inimigos para Deck, fixos.
- A regra da tabela vira uma função pura testável em `ui/BackAction.kt`:
  - `enum class ScreenKind { HOME, TAB, SUBSCREEN, MATCH, GAME_OVER, BLOCKING }` (`BLOCKING` é o conflito da nuvem);
  - `data class BackState(val kind: ScreenKind, val overlayOpen: Boolean = false, val paused: Boolean = false, val confirming: Boolean = false)`;
  - `enum class BackAction { CLOSE_OVERLAY, OPEN_PAUSE, RESUME, CANCEL_CONFIRM, TO_PARENT, TO_HOME, EXIT, NOTHING }`;
  - `fun backAction(s: BackState): BackAction`. Cada tela monta seu `BackState` em `onBack()` e executa a ação.

## 3. Hierarquia de botões (`ButtonStyle`)

`Button` troca o parâmetro de cor solta por um estilo, mantendo um `color` opcional para os botões que precisam de cor própria (power-ups da partida, na cor do dado):

| Estilo | Cor | Uso |
|---|---|---|
| `PRIMARY` | azul atual (0,25; 0,45; 0,85) | a ação principal da tela; no máximo uma por tela (Jogar, Continuar, Início no fim de jogo) |
| `SECONDARY` | `Skin.PANEL` | todas as outras ações |
| `DANGER` | vermelho (0,70; 0,25; 0,25) | ações que perdem progresso (Sair, Sim nas confirmações) |
| `SELECTED` | roxo (0,55; 0,30; 0,75) | a opção escolhida num grupo (modo, idioma) |
| `REWARD` | dourado (0,72; 0,52; 0,12) | Subir nível |

Mudanças visíveis: os dois Jogar de Desafios (hoje roxo e vermelho) e o Jogar código de Mapas (roxo) passam a `PRIMARY` e `SECONDARY`; a faixa colorida no topo de cada cartão de desafio continua marcando qual é qual. O botão "Menu" do fim de jogo passa a se chamar **Início**, como a aba.

## 4. Interruptores em Config (`Toggle`)

Novo componente `ui/Toggle.kt`: uma linha da largura do painel (600×72) com o nome à esquerda (`smallFont`) e um interruptor à direita (trilho 76×40 arredondado, `Skin.CELL` desligado e `Skin.GOLD` ligado; bolinha branca de raio 16 que desliza em 0,15 s). Tocar em qualquer ponto da linha alterna.

- Config troca os três botões "Vibrar: sim", "Reduzir movimento: não" e "Texto grande: não" por três `Toggle`: **Vibração**, **Reduzir movimento**, **Texto grande**, empilhados.
- Estado testável: `Toggle.on`, `toggle()`, `update(dt)` com `knob` de 0 a 1 que segue `on` (sem animação quando `reduceMotion`).

## 5. Texto legível sobre cor (`Skin.inkOn`)

`Skin.inkOn(background: Color): Color` devolve tinta escura (0,15; 0,15; 0,20) quando a luminância relativa do fundo passa de 0,55 e branco abaixo disso. Usado em todo texto desenhado sobre um fundo de cor variável:

- pílulas de cosmético em Conquistas (o bug: céus e molduras claros com texto branco);
- botões de estilo (o `Button` escolhe a tinta pelo fundo);
- selos e pílulas já existentes que hoje fixam branco ou escuro (`MissionsPanel`, janela do dado).

## 6. Paleta num lugar só

As cores de interface repetidas em 16 arquivos passam a viver em `Skin`:

- `Skin.MUTED` (0,65; 0,67; 0,75): textos secundários (hoje `KEY_COLOR` em 13 arquivos; a partida usa 0,75; 0,77; 0,85, que vira `MUTED` também).
- `Skin.LOCKED`, `Skin.LOCKED_INK`, `Skin.LOCK_COLOR`, `Skin.STAR_OFF`, `Skin.ROW_DONE`, `Skin.ROW_SELECTED`, `Skin.DIM` (fundo escurecido de janelas e pausa), `Skin.DANGER`, `Skin.SELECTED`, `Skin.REWARD`, `Skin.PRIMARY`.
- Cores que são identidade de um elemento (cada dado, cada mapa, mutador ligado, cor do desafio) continuam onde estão.

## Testes

- `BackActionTest`: cada linha da tabela da seção 2.
- `SkinTest.inkOn`: branco em `PANEL`, `PRIMARY`, `DANGER`; escuro em `GOLD`, branco puro e nas cores claras de céu; um caso de fronteira de cada lado de 0,55.
- `ToggleTest`: `toggle()` alterna; `update` leva `knob` a 1 em 0,15 s e para; com movimento reduzido salta direto.
- `StringsTest` continua cobrindo os textos novos (Vibração, Início no fim de jogo).
- Emulador: captura de antes e depois das 14 telas, conferindo cabeçalho, ausência de Voltar embaixo, interruptores, pílulas de Conquistas legíveis, e o botão voltar do Android em cada linha da tabela.

## Riscos

- **Toque na seta x elementos já no topo:** Deck tem o botão Inimigos em (24..214, 1190..1246), mas Deck é aba e não tem seta. Nas subtelas, a área da seta (16..104, 1178..1266) não cruza nada hoje.
- **Sair com o voltar no Início:** quem aperta voltar por engano no Início sai do jogo, como em qualquer app Android; o progresso já é salvo a cada mudança.
