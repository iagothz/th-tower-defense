# Padrão visual — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Toda tela do Merge TD usa o mesmo cabeçalho, a mesma hierarquia de botões, interruptores em Config, texto legível sobre cor e uma paleta só; o botão voltar do Android navega em vez de fechar o jogo.

**Architecture:** `Skin` ganha a paleta de interface e `inkOn`. `Button` passa a ter um `ButtonStyle` (com `color` opcional por cima). Três componentes novos e pequenos: `ScreenHeader` (título, subtítulo, seta), `Toggle` (linha com interruptor) e `BackAction.kt` (a tabela do voltar como função pura). `BaseScreen.onBack()` liga a tecla à tabela; cada tela monta seu `BackState`.

**Tech Stack:** Kotlin 2.1.20, libGDX 1.13.1 (ShapeRenderer, SpriteBatch, ExtendViewport 720×1280), testes `kotlin.test` com `./gradlew test` (sem Gdx nos testes: só `Color`, `Rectangle` e lógica pura).

**Spec:** `docs/superpowers/specs/2026-11-06-visual-standard-design.md`

## Global Constraints

- Mundo 720×1280; título em `bigFont` centrado em (360, 1222); subtítulo `smallFont` `Skin.MUTED` em (360, 1176); seta: círculo `Skin.PANEL` raio 30 em (60, 1222), área de toque 88×88 (16..104, 1178..1266).
- Cores (RGB, alfa 1 salvo indicado): `MUTED` 0,65/0,67/0,75 · `LOCKED` 0,22/0,23/0,29 · `LOCKED_INK` 0,32/0,33/0,40 · `LOCK_COLOR` 0,62/0,64/0,72 · `STAR_OFF` 0,26/0,27/0,34 · `ROW_DONE` 0,20/0,22/0,30 · `ROW_SELECTED` 0,22/0,24/0,33 · `DIM` preto alfa 0,72 · `DANGER` 0,70/0,25/0,25 · `SELECTED` 0,55/0,30/0,75 · `REWARD` 0,72/0,52/0,12 · `PRIMARY` 0,25/0,45/0,85.
- `inkOn`: tinta escura (0,15; 0,15; 0,20) quando a luminância do fundo passa de 0,55; branco senão. A luminância é 0,2126·R + 0,7152·G + 0,0722·B sobre os canais como estão (cortados em 0..1, sem linearizar). Ruling sobre a spec, que diz "luminância relativa": com os canais linearizados do WCAG, as molduras Ouro (0,45) e Prata (0,48) ficariam abaixo de 0,55 e manteriam o texto branco, justamente o bug que a seção 5 quer corrigir. Sem linearizar, elas ficam em 0,68 e 0,72 e recebem tinta escura, e os fundos escuros (painel, azul, vermelho) continuam com branco.
- `Toggle`: linha 600×72, trilho 76×40 (`Skin.CELL` desligado, `Skin.GOLD` ligado), bolinha branca raio 16, desliza em 0,15 s; instantâneo com `reduceMotion`.
- Textos de UI em português como chave, com EN em `Strings.kt` (`StringsTest` exige EN para toda chave).
- Commits em inglês, `feat:`/`docs:`/`refactor:`, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Trabalho direto na `main`. Push só quando o usuário disser "Sim, enviar".
- Preferências do desktop em `%USERPROFILE%\.prefs\merge-td` ficam intactas; a keystore de release nunca é regenerada.

## Review Focus

1. **Voltar durante a animação de derrota** (`MatchScreen.ending != null`): não pode abrir a pausa nem sair; trata como `ScreenKind.BLOCKING`. Teste em Task 4 (`blockingIgnoresEverything` cobre a regra; a tela só mapeia).
2. **Voltar com um dedo segurando um botão:** o toque em andamento é cancelado antes da ação, para o botão não ficar afundado nem disparar ao soltar na tela nova. Coberto em Task 4 (`BaseScreen` chama `onTouchCancelled()` antes de `onBack()`).
3. **Voltar com a janela do dado aberta e a tela sendo uma aba:** fecha a janela, não vai para o Início. Teste em Task 4 (`overlayWinsOverEveryKind`).
4. **Pausa aberta com confirmação:** voltar fecha só a confirmação, a pausa continua. Teste em Task 4 (`matchRules`).
5. **Fundos de cor acima de 1 (tintas de céu como 1,6/1,0/0,9):** `inkOn` corta os canais em 1 antes de linearizar e devolve tinta escura. Teste em Task 1 (`inkOnClampsTintsAboveOne`).

---

### Task 1: Paleta e `Skin.inkOn`

**Files:**
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/Skin.kt`
- Modify (trocar constantes locais pelas de `Skin`): `AchievementsScreen.kt`, `ChallengeScreen.kt`, `CloudConflictScreen.kt`, `DeckScreen.kt`, `DieStatsView.kt`, `DieWindow.kt`, `EnemiesScreen.kt`, `GameOverScreen.kt`, `HistoryScreen.kt`, `MapScreen.kt`, `MatchScreen.kt`, `MissionsPanel.kt`, `SettingsScreen.kt`, `StatsScreen.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/SkinTest.kt`

**Interfaces:**
- Produces: `Skin.MUTED, LOCKED, LOCKED_INK, LOCK_COLOR, STAR_OFF, ROW_DONE, ROW_SELECTED, DIM, DANGER, SELECTED, REWARD, PRIMARY: Color`; `Skin.INK_DARK: Color`; `fun Skin.luminance(c: Color): Float`; `fun Skin.inkOn(background: Color): Color` (devolve `INK_DARK` ou `Color.WHITE`, nunca uma cópia).

- [ ] **Step 1: Write the failing tests** em `SkinTest`:

```kotlin
@Test fun inkOnPicksWhiteOnDarkFills() {
    for (c in listOf(Skin.PANEL, Skin.PRIMARY, Skin.DANGER)) assertEquals(Color.WHITE, Skin.inkOn(c), "$c")
}
@Test fun inkOnPicksDarkOnLightFills() {
    for (c in listOf(Skin.GOLD, Color.WHITE, Cosmetic.FRAME_GOLD.color, Cosmetic.FRAME_SILVER.color, Cosmetic.FRAME_DIAMOND.color)) assertEquals(Skin.INK_DARK, Skin.inkOn(c), "$c")
}
@Test fun inkOnSwitchesAroundTheThreshold() {
    assertEquals(Skin.INK_DARK, Skin.inkOn(Color(0.56f, 0.56f, 0.56f, 1f)))
    assertEquals(Color.WHITE, Skin.inkOn(Color(0.54f, 0.54f, 0.54f, 1f)))
}
@Test fun inkOnClampsTintsAboveOne() {
    for (c in listOf(Cosmetic.SKY_DUSK, Cosmetic.SKY_AURORA, Cosmetic.SKY_NEBULA, Cosmetic.SKY_GOLDEN)) assertEquals(Skin.INK_DARK, Skin.inkOn(c.color), "$c")
}
```

- [ ] **Step 2: Run** `./gradlew :core:test --tests "*SkinTest*"` — Expected: FAIL, `Unresolved reference: inkOn` / `INK_DARK`.

- [ ] **Step 3: Implement** em `Skin`: as doze cores da Global Constraints, `INK_DARK = Color(0.15f, 0.15f, 0.20f, 1f)`, `luminance(c)` = 0,2126·R + 0,7152·G + 0,0722·B com cada canal cortado em 0..1 (sem linearizar; ver Global Constraints), `inkOn(bg) = if (luminance(bg) > 0.55f) INK_DARK else Color.WHITE`. `pipColor` passa a devolver `INK_DARK` em vez de criar uma cor nova.

- [ ] **Step 4: Run** `./gradlew :core:test --tests "*SkinTest*"` — Expected: PASS.

- [ ] **Step 5: Trocar as duplicatas.** Apague cada `KEY_COLOR` local (13 arquivos da lista acima, inclusive `DieStatsView.KEY_COLOR`, que `DieWindow` usa) e use `Skin.MUTED`; o `KEY_COLOR` da partida (0,75/0,77/0,85) também vira `Skin.MUTED`. Apague `LOCKED`, `LOCKED_INK`, `LOCK_COLOR` de `DeckScreen` e `DieWindow`, `STAR_OFF` e `DIM` de `DieWindow`, `ROW_DONE` de `AchievementsScreen`, `ROW_SELECTED` de `EnemiesScreen`, `DANGER` da `MatchScreen`, e use as de `Skin`. O véu da pausa (`Color(0f, 0f, 0f, 0.7f)`) vira `Skin.DIM`. Texto escuro fixo `Color(0.15f, 0.15f, 0.2f, 1f)` em `AchievementsScreen` vira `Skin.INK_DARK`. Cores de identidade (dados, mapas, `MUTATOR_ON`, faixas dos desafios) ficam onde estão.

- [ ] **Step 6: Pílulas de Conquistas legíveis.** Em `AchievementsScreen.draw`, a cor do texto da pílula de cosmético passa a ser `Skin.inkOn(fundo)`, onde `fundo` é exatamente a cor desenhada atrás dela (`GOLD` equipado, `c.color` ganho, cinza bloqueado). `MissionsPanel` e `DieWindow`: todo texto ou selo desenhado sobre fundo colorido (pílula de XP, selo de check) usa `Skin.inkOn` do seu fundo.

- [ ] **Step 7: Run** `./gradlew test` — Expected: BUILD SUCCESSFUL, todos verdes. Run `grep -rn "KEY_COLOR\|Color(0.65f, 0.67f, 0.75f" core/src/main` — Expected: nenhuma linha.

- [ ] **Step 8: Commit**

```bash
git add core/src
git commit -m "refactor: one UI palette in Skin, inkOn picks readable text over colour"
```

### Task 2: `ButtonStyle`

**Files:**
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/Button.kt`
- Modify (call sites): `MenuScreen.kt`, `DeckScreen.kt`, `DieWindow.kt`, `MapScreen.kt`, `ChallengeScreen.kt`, `SettingsScreen.kt`, `StatsScreen.kt`, `AchievementsScreen.kt`, `HistoryScreen.kt`, `EnemiesScreen.kt`, `MatchScreen.kt`, `GameOverScreen.kt`, `CloudConflictScreen.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/ButtonTest.kt`

**Interfaces:**
- Consumes: `Skin.PRIMARY, PANEL, DANGER, SELECTED, REWARD`, `Skin.inkOn` (Task 1).
- Produces: `enum class ButtonStyle(val fill: Color) { PRIMARY, SECONDARY, DANGER, SELECTED, REWARD }` (no mesmo arquivo `Button.kt`); `class Button(rect, text, enabled = true, var style: ButtonStyle = ButtonStyle.PRIMARY, var color: Color? = null)`; `val Button.fill: Color` (= `color ?: style.fill`); `val Button.ink: Color` (= `Skin.inkOn(fill)`).

- [ ] **Step 1: Write the failing tests** em `ButtonTest`:

```kotlin
@Test fun styleSetsTheFill() {
    val r = Rectangle(0f, 0f, 100f, 50f)
    assertEquals(Skin.PRIMARY, Button(r, "x").fill)
    assertEquals(Skin.PANEL, Button(r, "x", style = ButtonStyle.SECONDARY).fill)
    assertEquals(Skin.DANGER, Button(r, "x", style = ButtonStyle.DANGER).fill)
    assertEquals(Skin.SELECTED, Button(r, "x", style = ButtonStyle.SELECTED).fill)
    assertEquals(Skin.REWARD, Button(r, "x", style = ButtonStyle.REWARD).fill)
}
@Test fun ownColourWinsOverTheStyle() {
    val b = Button(Rectangle(0f, 0f, 100f, 50f), "x", style = ButtonStyle.SECONDARY, color = Skin.GOLD)
    assertEquals(Skin.GOLD, b.fill)
    assertEquals(Skin.INK_DARK, b.ink)
}
@Test fun inkIsWhiteOnTheDarkStyles() {
    assertEquals(Color.WHITE, Button(Rectangle(0f, 0f, 100f, 50f), "x").ink)
}
```

- [ ] **Step 2: Run** `./gradlew :core:test --tests "*ButtonTest*"` — Expected: FAIL, `Unresolved reference: ButtonStyle` / `fill`.

- [ ] **Step 3: Implement.** `drawShape` usa `fill` quando habilitado (DISABLED senão). `drawText` usa `ink` quando habilitado e `Color.LIGHT_GRAY` desabilitado.

- [ ] **Step 4: Run** `./gradlew :core:test --tests "*ButtonTest*"` — Expected: PASS.

- [ ] **Step 5: Migrar os botões.** Todo `color = Skin.PANEL` vira `style = ButtonStyle.SECONDARY`. Mudanças de estilo além disso:

| Tela | Botão | Estilo |
|---|---|---|
| Início | Jogar | PRIMARY |
| Deck | Inimigos, `?`, presets | SECONDARY |
| Janela do dado | Subir nível | REWARD (apague `LEVEL_COLOR`) |
| Mapas | Jogar / Jogar código | PRIMARY / SECONDARY |
| Mapas | chips de modo | o escolhido `SELECTED`, os outros `Skin.PANEL` (troque `MODE_ON` por `Skin.SELECTED`) |
| Desafios | os dois Jogar | PRIMARY (a faixa no topo de cada cartão continua com a cor dele) |
| Config | idiomas | o escolhido `SELECTED`, os outros SECONDARY (`b.style = ...` em vez de `b.color`; apague `ON`) |
| Config | Estatísticas, Conquistas, Histórico, nuvem | SECONDARY |
| Estatísticas | Conquistas, Histórico | SECONDARY |
| Partida | velocidade, pausa, Pular, Reiniciar | SECONDARY |
| Partida | power-ups | `color = Skin.color(t)` (continua) |
| Partida | Continuar / Não | PRIMARY |
| Partida | Sair / Sim | DANGER |
| Fim de jogo | Início (texto `S["Início"]`, era `S["Menu"]`) / Compartilhar | PRIMARY / SECONDARY |
| Conflito da nuvem | Manter este / Manter a nuvem | SECONDARY / SECONDARY |

- [ ] **Step 6: Run** `./gradlew test` — Expected: BUILD SUCCESSFUL. Run `grep -rn "color = Skin.PANEL\|color = Color(" core/src/main/kotlin/com/iagothz/mergetd/ui/*Screen.kt core/src/main/kotlin/com/iagothz/mergetd/ui/DieWindow.kt | grep Button` — Expected: nenhuma linha.

- [ ] **Step 7: Commit**

```bash
git add core/src
git commit -m "feat: button styles (primary, secondary, danger, selected, reward); game over Menu becomes Início"
```

### Task 3: `ScreenHeader` e subtelas sem Voltar embaixo

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/ScreenHeader.kt`
- Modify: `DeckScreen.kt`, `MapScreen.kt`, `ChallengeScreen.kt`, `SettingsScreen.kt`, `StatsScreen.kt`, `AchievementsScreen.kt`, `HistoryScreen.kt`, `EnemiesScreen.kt`, `MergeTdGame.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/ScreenHeaderTest.kt`

**Interfaces:**
- Consumes: `Skin.MUTED` (Task 1).
- Produces: `class ScreenHeader(val title: String, var subtitle: String? = null, val back: Boolean = false)` com `var pressed: Boolean`, `fun backHit(p: Vec2): Boolean`, `fun drawShapes(shapes: ShapeRenderer)`, `fun drawText(batch: SpriteBatch, game: MergeTdGame)`. `MergeTdGame.showAchievements(back: () -> Unit)` e `showHistory(back: () -> Unit)`; `AchievementsScreen(game, back: () -> Unit)` e `HistoryScreen(game, back: () -> Unit)`, ambas com `val goBack: () -> Unit` público para a Task 4.

- [ ] **Step 1: Write the failing test** `ScreenHeaderTest`:

```kotlin
@Test fun backArrowHitsOnlyInsideItsSquare() {
    val h = ScreenHeader("x", back = true)
    assertTrue(h.backHit(Vec2(60f, 1222f)))
    assertTrue(h.backHit(Vec2(17f, 1179f)))
    assertTrue(h.backHit(Vec2(103f, 1265f)))
    assertFalse(h.backHit(Vec2(110f, 1222f)))
    assertFalse(h.backHit(Vec2(60f, 1170f)))
}
@Test fun noArrowNoHit() {
    assertFalse(ScreenHeader("x").backHit(Vec2(60f, 1222f)))
}
```

- [ ] **Step 2: Run** `./gradlew :core:test --tests "*ScreenHeaderTest*"` — Expected: FAIL, `Unresolved reference: ScreenHeader`.

- [ ] **Step 3: Implement `ScreenHeader`.** Área da seta = `Rectangle(16f, 1178f, 88f, 88f)`. `drawShapes`, só com seta: círculo `Skin.PANEL` raio 30 em (60, 1222), escurecido 18% quando `pressed` (como `Button`); seta branca para a esquerda com `rectLine` de (72, 1222) a (50, 1222) com 5 de espessura e `triangle` com ponta em (42, 1222) e base de (54, 1210) a (54, 1234). `drawText`: título com `textShadowed(game.bigFont, title, 360f, 1222f)`; subtítulo, se houver, `smallFont` em `Skin.MUTED` centrado em (360, 1176).

- [ ] **Step 4: Run** `./gradlew :core:test --tests "*ScreenHeaderTest*"` — Expected: PASS.

- [ ] **Step 5: Aplicar nas telas.**
  - Abas Deck, Mapas, Desafios e Configurações: `ScreenHeader(S[...])` no lugar da linha `textShadowed(game.bigFont, ...)`.
  - Estatísticas, Conquistas, Histórico e Inimigos: `ScreenHeader(S[...], back = true)`; Conquistas com `subtitle = S.f("%d de %d", done, total)` atualizado a cada frame no lugar do texto em 1176. O toque na seta usa o `Tap` da tela (pressionar marca `header.pressed`; soltar dentro chama a volta).
  - Apague os botões Voltar de Estatísticas, Conquistas, Histórico e Inimigos. Em Estatísticas, Conquistas e Histórico ficam SECONDARY lado a lado e centrados: `Rectangle(148f, 124f, 206f, 80f)` e `Rectangle(366f, 124f, 206f, 80f)`.
  - Voltas: Estatísticas → `game.showSettings()`; Inimigos → `game.showDeck()`; Conquistas e Histórico → `goBack()`. Config chama `showAchievements { showSettings() }` e `showHistory { showSettings() }`; Estatísticas chama `showAchievements { showStats() }` e `showHistory { showStats() }`.

- [ ] **Step 6: Run** `./gradlew test` — Expected: BUILD SUCCESSFUL. Run `grep -rn "S\[\"Voltar\"\]\|textShadowed(game.bigFont" core/src/main/kotlin/com/iagothz/mergetd/ui` — Expected: só `MenuScreen` ("Merge TD"), `MatchScreen` ("Pausa"), `GameOverScreen` ("Fim de jogo") e `CloudConflictScreen`.

- [ ] **Step 7: Commit**

```bash
git add core/src
git commit -m "feat: one screen header with a back arrow; subscreens lose the bottom Voltar button"
```

### Task 4: Botão voltar do Android

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/BackAction.kt`
- Modify: `BaseScreen.kt`, `MergeTdGame.kt`, `DeckScreen.kt`, `StatsScreen.kt`, `AchievementsScreen.kt`, `HistoryScreen.kt`, `EnemiesScreen.kt`, `MatchScreen.kt`, `GameOverScreen.kt`, `CloudConflictScreen.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/BackActionTest.kt`

**Interfaces:**
- Consumes: `AchievementsScreen.goBack`, `HistoryScreen.goBack` (Task 3).
- Produces: `enum class ScreenKind { HOME, TAB, SUBSCREEN, MATCH, GAME_OVER, BLOCKING }`; `data class BackState(val kind: ScreenKind, val overlayOpen: Boolean = false, val paused: Boolean = false, val confirming: Boolean = false)`; `enum class BackAction { CLOSE_OVERLAY, OPEN_PAUSE, RESUME, CANCEL_CONFIRM, TO_PARENT, TO_HOME, EXIT, NOTHING }`; `fun backAction(s: BackState): BackAction`; `open fun BaseScreen.backState(): BackState`; `open fun BaseScreen.perform(a: BackAction)`; `fun BaseScreen.onBack(): Boolean`.

- [ ] **Step 1: Write the failing tests** `BackActionTest`, uma linha da tabela da spec por asserção:

```kotlin
@Test fun overlayWinsOverEveryKind() {
    for (k in listOf(ScreenKind.HOME, ScreenKind.TAB, ScreenKind.SUBSCREEN)) assertEquals(BackAction.CLOSE_OVERLAY, backAction(BackState(k, overlayOpen = true)))
}
@Test fun matchRules() {
    assertEquals(BackAction.OPEN_PAUSE, backAction(BackState(ScreenKind.MATCH)))
    assertEquals(BackAction.RESUME, backAction(BackState(ScreenKind.MATCH, paused = true)))
    assertEquals(BackAction.CANCEL_CONFIRM, backAction(BackState(ScreenKind.MATCH, paused = true, confirming = true)))
}
@Test fun screensWithoutOverlays() {
    assertEquals(BackAction.TO_HOME, backAction(BackState(ScreenKind.GAME_OVER)))
    assertEquals(BackAction.TO_PARENT, backAction(BackState(ScreenKind.SUBSCREEN)))
    assertEquals(BackAction.TO_HOME, backAction(BackState(ScreenKind.TAB)))
    assertEquals(BackAction.EXIT, backAction(BackState(ScreenKind.HOME)))
}
@Test fun blockingIgnoresEverything() {
    assertEquals(BackAction.NOTHING, backAction(BackState(ScreenKind.BLOCKING)))
    assertEquals(BackAction.NOTHING, backAction(BackState(ScreenKind.BLOCKING, overlayOpen = true, paused = true)))
}
```

- [ ] **Step 2: Run** `./gradlew :core:test --tests "*BackActionTest*"` — Expected: FAIL, `Unresolved reference: backAction`.

- [ ] **Step 3: Implement `backAction`** em `BackAction.kt` com um `when`, nesta ordem: `BLOCKING` → `NOTHING`; `overlayOpen` → `CLOSE_OVERLAY`; `MATCH` → `confirming` `CANCEL_CONFIRM`, `paused` `RESUME`, senão `OPEN_PAUSE`; `GAME_OVER` → `TO_HOME`; `SUBSCREEN` → `TO_PARENT`; `TAB` → `TO_HOME`; `HOME` → `EXIT`.

- [ ] **Step 4: Run** `./gradlew :core:test --tests "*BackActionTest*"` — Expected: PASS.

- [ ] **Step 5: Ligar a tecla.**
  - `MergeTdGame.create()`: `Gdx.input.setCatchKey(Input.Keys.BACK, true)`.
  - `BaseScreen`: `keyDown` trata `Input.Keys.BACK` e `Input.Keys.ESCAPE` chamando `onBack()` (as outras teclas continuam indo para `onKey`). `onBack()`: cancela o toque em andamento (`gesture.cancel()`, `nav?.pressed = null`, `onBar = false`, `onTouchCancelled()`), calcula `backAction(backState())`, chama `perform(a)` e devolve `a != EXIT`; quando devolve false, `keyDown` chama `Gdx.app.exit()`.
  - `backState()` padrão: `HOME` quando `tab == Tab.HOME`, `TAB` para outra aba, `BLOCKING` sem aba. `perform` padrão: `TO_HOME` → `game.showTab(Tab.HOME)`, o resto não faz nada.
  - Sobrescritas: `DeckScreen` (`TAB`, `overlayOpen = window.isOpen`; `CLOSE_OVERLAY` → `window.close()`); `StatsScreen`, `EnemiesScreen`, `AchievementsScreen`, `HistoryScreen` (`SUBSCREEN`; `TO_PARENT` → a mesma volta da seta da Task 3); `MatchScreen` (`BLOCKING` enquanto `ending != null`, senão `MATCH` com `paused` e `confirming = confirm != null`; `OPEN_PAUSE` → `paused = true; confirm = null`, `RESUME` → `paused = false`, `CANCEL_CONFIRM` → `confirm = null`); `GameOverScreen` (`GAME_OVER`; `TO_HOME` → `game.showMenu()`, o mesmo do botão Início); `CloudConflictScreen` herda o padrão (`BLOCKING`).

- [ ] **Step 6: Run** `./gradlew test` — Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add core/src
git commit -m "feat: Android back navigates (closes, pauses, goes up) and only exits from Início"
```

### Task 5: Interruptores em Config

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/Toggle.kt`
- Modify: `SettingsScreen.kt`, `Strings.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/ToggleTest.kt`

**Interfaces:**
- Produces: `class Toggle(val rect: Rectangle, val label: String, on: Boolean, private val instant: () -> Boolean = { false })` com `var on: Boolean` (só leitura de fora: `private set`), `var knob: Float` (`private set`, começa em 1 se `on`, 0 senão), `fun toggle()`, `fun update(dt: Float)`, `fun contains(x: Float, y: Float): Boolean`, `fun drawShapes(shapes: ShapeRenderer)`, `fun drawText(batch: SpriteBatch, font: BitmapFont)`; `const val Toggle.SLIDE = 0.15f`.

- [ ] **Step 1: Write the failing tests** `ToggleTest`:

```kotlin
private fun t(on: Boolean = false, instant: Boolean = false) = Toggle(Rectangle(60f, 700f, 600f, 72f), "x", on) { instant }

@Test fun toggleFlips() {
    val a = t()
    a.toggle(); assertTrue(a.on)
    a.toggle(); assertFalse(a.on)
}
@Test fun knobStartsWhereTheStateIs() {
    assertEquals(1f, t(on = true).knob)
    assertEquals(0f, t().knob)
}
@Test fun knobSlidesOverASlideAndStops() {
    val a = t()
    a.toggle()
    a.update(Toggle.SLIDE / 2)
    assertEquals(0.5f, a.knob, 0.01f)
    a.update(Toggle.SLIDE)
    assertEquals(1f, a.knob)
}
@Test fun reducedMotionJumps() {
    val a = t(instant = true)
    a.toggle()
    a.update(0.001f)
    assertEquals(1f, a.knob)
}
@Test fun wholeRowIsTheTarget() {
    assertTrue(t().contains(70f, 710f))
    assertTrue(t().contains(650f, 760f))
    assertFalse(t().contains(70f, 780f))
}
```

- [ ] **Step 2: Run** `./gradlew :core:test --tests "*ToggleTest*"` — Expected: FAIL, `Unresolved reference: Toggle`.

- [ ] **Step 3: Implement.** `update` move `knob` em direção a `on` a `dt / SLIDE` por chamada, sem passar do alvo; com `instant()` vai direto. Desenho: fundo da linha `Skin.PANEL` arredondado (raio 16); trilho 76×40 alinhado à direita com 24 de margem e centrado na altura, raio 20, cor `Skin.CELL` interpolada até `Skin.GOLD` por `knob`; bolinha branca raio 16 de `trilho.x + 20` a `trilho.x + 56` por `knob`. Nome em `font` à esquerda, 24 de margem, centrado na altura. `drawShapes` chama `update(Gdx.graphics.deltaTime)`, como o `Button`.

- [ ] **Step 4: Run** `./gradlew :core:test --tests "*ToggleTest*"` — Expected: PASS.

- [ ] **Step 5: Usar em Config.** Troque `vibration`, `motion` e `largeText` por três `Toggle` de 600×72 em x 60, empilhados em y 748, 664 e 580 (8 de espaço), rótulos `S["Vibração"]`, `S["Reduzir movimento"]`, `S["Texto grande"]`, `instant = { game.save.reduceMotion }`. Tocar alterna o `Toggle` e grava o mesmo efeito de hoje (vibrar ao ligar a vibração; reconstruir as fontes e reabrir Config no texto grande). Desça "Idioma" para y 540 e os botões de idioma para y 432, e a linha da nuvem e a privacidade o quanto for preciso para não encostar (a nuvem em 352 e privacidade em 330 continuam se couberem; confira no emulador). Em `Strings.kt`, adicione `"Vibração" to "Vibration"`.

- [ ] **Step 6: Run** `./gradlew test` — Expected: BUILD SUCCESSFUL (inclui `StringsTest`).

- [ ] **Step 7: Commit**

```bash
git add core/src
git commit -m "feat: Config uses switches for vibration, reduced motion and large text"
```

### Task 6: Documentação, emulador e APK

**Files:**
- Modify: `README.md`, `docs/superpowers/specs/2026-10-26-merge-tower-defense-design.md`, `docs/superpowers/specs/2026-11-06-visual-standard-design.md` (Status: implementado)

- [ ] **Step 1: Docs.** README e a spec principal citam o cabeçalho com seta, o botão voltar do Android, os estilos de botão e os interruptores de Config, no lugar das menções a botões Voltar nas subtelas e a "Menu" no fim de jogo.

- [ ] **Step 2: Build** `./gradlew test :android:assembleRelease` — Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Emulador** (fluxo da memória emulator-verification: Pixel_8 headless, `cap.sh` do scratchpad). Instale o APK e capture as 14 telas. Confira: cabeçalho igual nas abas e subtelas, nenhum Voltar embaixo, os três interruptores, pílulas de Conquistas legíveis, Desafios com os Jogar azuis, e "Início" no fim de jogo. Teste o voltar com `adb shell input keyevent 4` em cada linha da tabela: janela do dado, partida rodando, pausa, confirmação, fim de jogo, as quatro subtelas (Conquistas e Histórico abertos por Config e por Estatísticas), uma aba e o Início.

- [ ] **Step 4: Commit**

```bash
git add README.md docs
git commit -m "docs: visual standard implemented"
```

- [ ] **Step 5:** Envie o APK de release com SendUserFile e peça o "Sim, enviar" antes do push.
