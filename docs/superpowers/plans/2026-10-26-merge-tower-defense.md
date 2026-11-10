# Merge Tower Defense — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tower defense de merge com dados, offline, que gera um APK Android jogável (deck de 5, merge, power-ups, ondas infinitas, recorde).

**Architecture:** Projeto Gradle multi-módulo libGDX (`core`, `lwjgl3`, `android`). Toda a regra fica em `core/.../logic` como Kotlin puro, sem import de `com.badlogic`, testada com JUnit 5 e RNG semeado. `core/.../ui` lê o estado de `Match` e desenha com `ShapeRenderer`/`BitmapFont` num viewport virtual 720×1280.

**Tech Stack:** Kotlin 2.1.x (JVM target 17), libGDX 1.13.x (backends lwjgl3 + android), Gradle wrapper 8.14.x, Android Gradle Plugin 8.10+ (compileSdk 36, minSdk 24), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-26-merge-tower-defense-design.md`

## Global Constraints

- Pacote raiz: `com.iagothz.mergetd`; lógica em `com.iagothz.mergetd.logic`, UI em `com.iagothz.mergetd.ui`.
- `logic` não importa nada de `com.badlogic.*` (vetor próprio `Vec2`).
- Mundo virtual 720×1280, retrato, `FitViewport`, origem embaixo à esquerda (y para cima).
- Grid 3 linhas × 5 colunas; pips de 1 a 7.
- 3 vidas; inimigo comum tira 1, chefe tira 2.
- SP inicial 100; compra 10 + 10 × (compras feitas).
- Power-up níveis 1–5; custo para subir: 100 / 200 / 400 / 700; `powerMult` = 1.0 / 1.3 / 1.6 / 2.0 / 2.5.
- Recompensa: normal 10, rápido 10, tanque 20, chefe 100.
- Onda `n`: `min(40, 8 + 2n)` inimigos a cada 0,6 s; vida `base × 1.12^(n−1)`; chefe sozinho nas ondas múltiplas de 10 com 40× a vida de um normal, na ordem Silenciador → Trocador → Regenerador; próxima onda 3 s depois de a anterior estar toda gerada e eliminada.
- Intervalo de ataque `baseInterval / (1 + 0.15 × (pips − 1))`; dano `baseDamage × pips × powerMult`.
- Deck padrão: FIRE, ICE, POISON, ELECTRIC, MINE.
- Toda constante numérica fica em `Balance.kt`.
- Passo fixo de simulação 1/60 s.
- Comandos rodam a partir da raiz do repositório, no Git Bash (`./gradlew`).

## Review Focus

1. **Deck salvo corrompido ou inválido** (string vazia, IDs desconhecidos, repetidos, quantidade ≠ 5) → usar o deck padrão em vez de travar. Teste: `DeckCodecTest` (Task 2).
2. **Comprar com o tabuleiro cheio e SP suficiente** → nada muda, nenhum SP é gasto. Teste: `MatchTest.buyOnFullBoardKeepsSp` (Task 6).
3. **`dt` enorme** (engasgo de frame, volta do segundo plano) → `update` limita a 0,25 s simulados por chamada, sem explosão de passos. Teste: `MatchTest.hugeDeltaIsClamped` (Task 6).
4. **Inimigo atingido por vários dados no mesmo passo** → SP pago uma única vez e inimigo removido. Teste: `MatchTest.killRewardPaidOnce` (Task 6).
5. **Chefe Trocador com menos de 2 dados / Silenciador sem dados** → habilidade não faz nada, sem exceção. Teste: `MatchTest.bossAbilitiesWithEmptyBoard` (Task 6).

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, local.properties (não versionado)
gradlew, gradlew.bat, gradle/wrapper/*
core/build.gradle.kts
core/src/main/kotlin/com/iagothz/mergetd/
  logic/Vec2.kt          vetor 2D puro
  logic/DieType.kt       enum dos 10 tipos
  logic/Balance.kt       todas as constantes e tabelas
  logic/Die.kt           dado + estado de runtime
  logic/Board.kt         Cell, grid 3×5, regras de merge/swap
  logic/DeckCodec.kt     serialização do deck + padrão
  logic/Layout.kt        geometria do grid e do caminho no mundo 720×1280
  logic/EnemyPath.kt     polilinha, posição por distância
  logic/Enemy.kt         EnemyKind + Enemy (vida, lentidão, veneno)
  logic/WaveSpawner.kt   composição e cronômetro das ondas
  logic/Combat.kt        fórmulas, mira, efeitos por tipo, ShotEvent
  logic/Match.kt         orquestra a partida
  ui/MergeTdGame.kt      Game + troca de telas, SpriteBatch/ShapeRenderer/fonte compartilhados
  ui/SaveData.kt         Preferences (bestWave, deck)
  ui/Skin.kt             cor/forma por DieType e EnemyKind
  ui/Button.kt           retângulo clicável + desenho
  ui/MenuScreen.kt, ui/DeckScreen.kt, ui/GameOverScreen.kt
  ui/MatchScreen.kt      loop, HUD, pausa
  ui/MatchRenderer.kt    desenho do caminho, grid, dados, inimigos, tiros
  ui/DragController.kt   arrastar e soltar para merge
core/src/test/kotlin/com/iagothz/mergetd/logic/*Test.kt
lwjgl3/build.gradle.kts, lwjgl3/src/main/kotlin/com/iagothz/mergetd/lwjgl3/Lwjgl3Launcher.kt
android/build.gradle.kts, android/AndroidManifest.xml, android/src/main/kotlin/com/iagothz/mergetd/android/AndroidLauncher.kt
```

---

### Task 1: Esqueleto Gradle (core + lwjgl3 + android)

**Files:**
- Create: arquivos Gradle da raiz, `core/build.gradle.kts`, `lwjgl3/**`, `android/**`, `.gitignore`, `core/src/main/kotlin/com/iagothz/mergetd/ui/MergeTdGame.kt` (stub), `core/src/test/kotlin/com/iagothz/mergetd/logic/SmokeTest.kt`

**Interfaces:**
- Produces: `class MergeTdGame : Game()` (stub que limpa a tela com cor escura); módulos `:core`, `:lwjgl3`, `:android`; tasks `:core:test`, `:lwjgl3:run`, `:android:assembleDebug`.

- [ ] **Step 1: Escolher o JDK.** Procure JDK 17 ou 21 (`ls "C:/Program Files/Java" "C:/Program Files/Eclipse Adoptium" "C:/Program Files/Microsoft"`). Se não houver, **pare e peça ao usuário** para instalar o Temurin 21 (`winget install EclipseAdoptium.Temurin.21.JDK`). O JDK 25 do Android Studio não roda o Gradle 8.14. Grave o caminho em `gradle.properties` como `org.gradle.java.home=<caminho com />`.
- [ ] **Step 2: Criar o wrapper.** Baixe `https://services.gradle.org/distributions/gradle-8.14.3-bin.zip` para o scratchpad, extraia e rode `<extraído>/bin/gradle wrapper --gradle-version 8.14.3` na raiz. Depois disso, use só `./gradlew`.
- [ ] **Step 3: Escrever os builds.**
  - `settings.gradle.kts` inclui `core`, `lwjgl3`, `android`, com repositórios `google()`, `mavenCentral()`.
  - `core`: plugin `kotlin("jvm")`, `api("com.badlogicgames.gdx:gdx:$gdxVersion")`, `testImplementation(kotlin("test"))`, `testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")`, `tasks.test { useJUnitPlatform() }`, `jvmToolchain(17)`.
  - `lwjgl3`: `application`, mainClass `com.iagothz.mergetd.lwjgl3.Lwjgl3LauncherKt`, dependências `gdx-backend-lwjgl3` + `gdx-platform:natives-desktop`. Janela 405×720, título "Merge TD".
  - `android`: `com.android.application`, `kotlin-android`, namespace/applicationId `com.iagothz.mergetd`, compileSdk 36, minSdk 24, targetSdk 36, `screenOrientation="portrait"`, `gdx-backend-android` + configuração `natives` copiando `natives-armeabi-v7a/arm64-v8a/x86/x86_64` para `src/main/jniLibs` (padrão gdx-liftoff). `local.properties` com `sdk.dir=C:/Users/iagoz/AppData/Local/Android/Sdk`.
  - `gdxVersion` em `gradle.properties` (1.13.1 ou o 1.13.x mais recente).
  - `.gitignore`: `build/`, `.gradle/`, `local.properties`, `*.iml`, `.idea/`, `android/src/main/jniLibs/`.
- [ ] **Step 4: Escrever um teste smoke** `SmokeTest.runs()` com `assertEquals(2, 1 + 1)`.
- [ ] **Step 5: Verificar.**
  - `./gradlew :core:test` → BUILD SUCCESSFUL, 1 teste.
  - `./gradlew :android:assembleDebug` → gera `android/build/outputs/apk/debug/android-debug.apk`.
  - `./gradlew :lwjgl3:run` (em background) abre uma janela escura; feche depois.
- [ ] **Step 6: Commit** — `git add -A && git commit -m "build: scaffold libGDX project (core, lwjgl3, android)"`

---

### Task 2: Dados, tabuleiro, merge e deck

**Files:**
- Create: `logic/Vec2.kt`, `logic/DieType.kt`, `logic/Balance.kt`, `logic/Die.kt`, `logic/Board.kt`, `logic/DeckCodec.kt`
- Test: `BoardTest.kt`, `DeckCodecTest.kt`

**Interfaces:**
- Produces:
  - `data class Vec2(val x: Float, val y: Float) { fun dst(o: Vec2): Float }`
  - `enum class DieType(val id: String, val attacks: Boolean) { FIRE, ICE, POISON, ELECTRIC, WIND, IRON, MINE(attacks=false), SNIPER, MIRROR(attacks=false), GROWTH }` (ids em minúsculas: `"fire"`…)
  - `object Balance` com, no mínimo: `ROWS=3, COLS=5, MAX_PIPS=7, START_SP=100, START_LIVES=3, BUY_BASE=10, BUY_STEP=10, POWER_COSTS=intArrayOf(100,200,400,700), POWER_MULT=floatArrayOf(1.0f,1.3f,1.6f,2.0f,2.5f), STEP=1f/60f, MAX_FRAME=0.25f` e `fun stats(t: DieType): DieStats` com `data class DieStats(val baseDamage: Float, val baseInterval: Float)`: FIRE 20/1.0, ICE 10/1.0, POISON 5/1.0, ELECTRIC 15/1.2, WIND 6/0.35, IRON 18/1.0, MINE 0/0, SNIPER 60/2.5, MIRROR 0/0, GROWTH 8/1.0.
  - `class Die(val type: DieType, var pips: Int) { var cooldown = 0f; var timer = 0f; var disabledFor = 0f; val disabled get() = disabledFor > 0f }` (`timer` serve a MINE e GROWTH)
  - `data class Cell(val row: Int, val col: Int)`
  - `class Board { operator fun get(c: Cell): Die?; operator fun set(c: Cell, d: Die?); fun cells(): List<Cell>; fun emptyCells(): List<Cell>; fun occupied(): List<Pair<Cell, Die>>; fun canMerge(from: Cell, to: Cell): Boolean; fun merge(from: Cell, to: Cell, deck: List<DieType>, rng: Random): Boolean; fun swap(a: Cell, b: Cell) }`
  - `object DeckCodec { val DEFAULT: List<DieType>; fun encode(deck: List<DieType>): String; fun decode(s: String?): List<DieType> }` — formato `"fire,ice,poison,electric,mine"`.

- [ ] **Step 1: Escrever os testes que falham.** `BoardTest` (deck `[FIRE, ICE, POISON, ELECTRIC, MIRROR]`, `Random(42)`):
  - `mergeSameTypeSamePips`: FIRE 2 em (0,0) e FIRE 2 em (0,1) → `merge` retorna true, `board[(0,0)] == null`, `board[(0,1)]!!.pips == 3` e o tipo pertence ao deck.
  - `rejectDifferentPips`: FIRE 2 + FIRE 3 → false, tabuleiro inalterado.
  - `rejectDifferentTypes`: FIRE 2 + ICE 2 → false.
  - `rejectSelfAndEmpty`: `canMerge(c, c)` false; origem ou destino vazio → false.
  - `rejectAtMaxPips`: FIRE 7 + FIRE 7 → false.
  - `mirrorCopiesOtherType`: MIRROR 3 → ICE 3 resulta em ICE 4; ICE 3 → MIRROR 3 também resulta em ICE 4.
  - `mirrorWithMirrorIsRandomDeckType`: resultado com pips 4 e tipo do deck.
  - `mergeResultIsFresh`: a origem tem `disabledFor = 5f` → o resultado tem `disabled == false`.
  - `swapMovesDice`: swap troca os conteúdos, inclusive com uma casa vazia.

  `DeckCodecTest`:
  - `roundTrip`: decode(encode(x)) == x.
  - `fallsBackToDefault` para `null`, `""`, `"fire,ice"`, `"fire,fire,ice,poison,mine"`, `"fire,ice,poison,electric,banana"` → `DEFAULT == listOf(FIRE, ICE, POISON, ELECTRIC, MINE)`.
- [ ] **Step 2: Rodar** `./gradlew :core:test` → FAIL (símbolos não resolvidos).
- [ ] **Step 3: Implementar** os arquivos acima. O resultado do merge é um `Die` novo: tipo do não-Espelho se exatamente um for MIRROR; senão `deck.random(rng)`.
- [ ] **Step 4: Rodar** `./gradlew :core:test` → PASS.
- [ ] **Step 5: Commit** — `feat(logic): dice, board merge rules and deck codec`

---

### Task 3: Layout e caminho

**Files:**
- Create: `logic/Layout.kt`, `logic/EnemyPath.kt`
- Test: `EnemyPathTest.kt`

**Interfaces:**
- Produces:
  - `object Layout { WORLD_W=720f, WORLD_H=1280f, CELL=112f, GAP=8f, GRID_X=64f, GRID_Y=464f; fun cellCenter(c: Cell): Vec2; fun cellAt(p: Vec2): Cell?; val PATH: EnemyPath }`. A linha 0 é a de cima. `cellCenter(Cell(r,c)) = Vec2(GRID_X + c*(CELL+GAP) + CELL/2, GRID_Y + (ROWS-1-r)*(CELL+GAP) + CELL/2)`. O caminho tem os pontos `(24,200) → (24,856) → (696,856) → (696,200)`.
  - `class EnemyPath(val points: List<Vec2>) { val length: Float; fun positionAt(distance: Float): Vec2 }` — limita a distância a `[0, length]`.

- [ ] **Step 1: Testes que falham:**
  - `lengthIs1984` (656+672+656).
  - `positionAtStart == (24,200)`; `positionAt(656f) == (24,856)`; `positionAt(656f+336f) == (360,856)`; `positionAt(9999f) == (696,200)`.
  - `cellAtRoundTrip`: para toda célula, `cellAt(cellCenter(c)) == c`.
  - `cellAt(Vec2(10f,10f)) == null`; um ponto no gap entre células → null.
- [ ] **Step 2: Rodar** → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar** → PASS.
- [ ] **Step 5: Commit** — `feat(logic): world layout and enemy path`

---

### Task 4: Inimigos e ondas

**Files:**
- Create: `logic/Enemy.kt`, `logic/WaveSpawner.kt`; adicionar constantes a `Balance.kt`
- Test: `WaveSpawnerTest.kt`, `EnemyTest.kt`

**Interfaces:**
- Consumes: `Balance`, `Layout.PATH.length`
- Produces:
  - `enum class EnemyKind(val hpMult: Float, val speedMult: Float, val reward: Int, val lifeDamage: Int, val isBoss: Boolean)`: NORMAL(1,1,10,1), FAST(0.5,2,10,1), TANK(3,0.6,20,1), BOSS_SILENCER/BOSS_SWAPPER/BOSS_REGEN(40,0.5,100,2,true).
  - `Balance`: `BASE_HP=100f, BASE_SPEED=120f, HP_GROWTH=1.12f, SPAWN_INTERVAL=0.6f, WAVE_DELAY=3f, MAX_WAVE_SIZE=40, BOSS_EVERY=10, SLOW_DURATION=2f, POISON_DURATION=3f, POISON_MAX_STACKS=5, BOSS_ABILITY_INTERVAL=6f, SILENCE_DURATION=5f, REGEN_PER_SEC=0.02f`.
  - `class Enemy(val kind: EnemyKind, val maxHp: Float) { var hp; var distance = 0f; var slow = 0f; var slowTime = 0f; val poison: MutableList<PoisonStack>; var abilityTimer = 0f; val alive get() = hp > 0f; fun speed(): Float = BASE_SPEED * kind.speedMult * (1 - slow); fun applySlow(amount: Float); fun addPoison(dps: Float); fun tickEffects(dt: Float) }`, com `data class PoisonStack(val dps: Float, var remaining: Float)`.
    - `applySlow` fica com o maior valor entre o atual e o novo e renova `slowTime = SLOW_DURATION`.
    - `addPoison`, quando já há 5 stacks, substitui o stack com menos tempo restante.
    - `tickEffects` aplica o dano do veneno, expira os stacks, expira a lentidão e, se o tipo for BOSS_REGEN, cura `REGEN_PER_SEC*maxHp*dt` limitado a `maxHp`.
  - `object WaveSpawner { fun waveSize(n: Int): Int; fun composition(n: Int): List<EnemyKind>; fun enemyHp(n: Int, kind: EnemyKind): Float }`
    - Composição sem chefe, por índice `i`: se `n>=5 && i%5==4` → TANK; senão se `n>=3 && i%3==2` → FAST; senão NORMAL.
    - Ondas de chefe: `listOf(boss)`, com `boss = [SILENCER, SWAPPER, REGEN][(n/10 - 1) % 3]`.
    - `enemyHp = BASE_HP * HP_GROWTH^(n-1) * kind.hpMult`.

- [ ] **Step 1: Testes que falham:**
  - `waveSize`: 1→10, 5→18, 16→40, 50→40.
  - `earlyWavesOnlyNormal`: a onda 1 é toda NORMAL; a 3 tem FAST e nenhum TANK; a 5 tem TANK.
  - `bossWaves`: a onda 10 é `[BOSS_SILENCER]`, a 20 é `[BOSS_SWAPPER]`, a 30 `[BOSS_REGEN]` e a 40 `[BOSS_SILENCER]`.
  - `hpScaling`: `enemyHp(1, NORMAL)==100f`; `enemyHp(2, NORMAL)==112f` (±0.01); `enemyHp(10, BOSS_SILENCER) == 40 * enemyHp(10, NORMAL)`.
  - `EnemyTest`:
    - `slowTakesStrongestAndExpires`: aplicar 0.2 e depois 0.1 → `slow==0.2`; após `tickEffects(2.1f)` → 0.
    - `poisonCapsAt5Stacks`: 6 aplicações → 5 stacks.
    - `poisonDealsDps`: 1 stack de 10 dps, tick de 1 s → hp −10.
    - `regenCapsAtMax`.
- [ ] **Step 2: Rodar** → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar** → PASS.
- [ ] **Step 5: Commit** — `feat(logic): enemies, status effects and wave composition`

---

### Task 5: Combate

**Files:**
- Create: `logic/Combat.kt`; adicionar constantes a `Balance.kt`
- Test: `CombatTest.kt`

**Interfaces:**
- Consumes: `Die`, `DieType`, `Enemy`, `EnemyPath`, `Vec2`, `Balance`
- Produces:
  - `Balance`: `FIRE_SPLASH_RADIUS=70f, FIRE_SPLASH_FRACTION=0.5f, ICE_SLOW_BASE=0.15f, ICE_SLOW_PER_PIP=0.05f, ICE_SLOW_MAX=0.5f, POISON_DPS=8f, CHAIN_JUMPS=2, CHAIN_RADIUS=120f, CHAIN_FALLOFF=0.7f, IRON_BOSS_MULT=2f, ATTACK_SPEED_PER_PIP=0.15f`.
  - `data class ShotEvent(val from: Vec2, val to: List<Vec2>, val type: DieType)`
  - `object Combat`:
    - `fun attackInterval(t: DieType, pips: Int): Float`
    - `fun hitDamage(t: DieType, pips: Int, powerLevel: Int, target: Enemy): Float` (IRON ×2 contra chefe)
    - `fun selectTarget(t: DieType, enemies: List<Enemy>): Enemy?`: só vivos; SNIPER → maior `hp`; demais → maior `distance`.
    - `fun attack(die: Die, origin: Vec2, powerLevel: Int, enemies: List<Enemy>, path: EnemyPath): ShotEvent?`: aplica o dano principal e o efeito do tipo, retorna null se não houver alvo. Efeitos:
      - FIRE: 50% do dano a outros vivos a ≤70 do alvo.
      - ICE: `applySlow(min(0.5, 0.15 + 0.05*(pips-1)))`.
      - POISON: `addPoison(POISON_DPS*pips*powerMult)`.
      - ELECTRIC: até 2 saltos para o vivo mais próximo ainda não atingido a ≤120 do último, com dano ×0.7 a cada salto.
      - WIND, IRON, SNIPER, GROWTH: só o dano.

    Posições dos inimigos vêm de `path.positionAt(enemy.distance)`. `attack` não remove mortos nem paga SP (isso é do `Match`).

- [ ] **Step 1: Testes que falham:**
  - `intervalScalesWithPips`: `attackInterval(FIRE,1)==1.0f`; `attackInterval(FIRE,3)==1.0f/1.3f` (±1e-4).
  - `damageFormula`: `hitDamage(FIRE,3,2,normal)==20*3*1.3f`.
  - `ironDoublesVsBoss`: `hitDamage(IRON,1,1,boss)==36f`; contra NORMAL dá 18f.
  - `targetsFurthest`: inimigos com distance 100/500/300 → o de 500.
  - `sniperTargetsHighestHp`.
  - `ignoresDead`: só há mortos → `attack` retorna null.
  - `fireSplash`: alvo a d=500, vizinho a d=540 (mesmo segmento, 40 de distância) e longe a d=900 → o vizinho perde `0.5*dano` e o longe não perde nada.
  - `iceSlowByPips`: pips 1 → slow 0.15; pips 7 → 0.45.
  - `poisonAddsStack`.
  - `electricChains`: 4 inimigos em fila no primeiro segmento, espaçados 100 → exatamente 3 atingidos (limite de 2 saltos), com danos d, 0.7d, 0.49d; o 4º não é atingido.
  - `electricRangeLimit`: alvo e um único vizinho a 150 → só o alvo é atingido.
- [ ] **Step 2: Rodar** → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar** → PASS.
- [ ] **Step 5: Commit** — `feat(logic): combat formulas, targeting and die effects`

---

### Task 6: Partida (Match)

**Files:**
- Create: `logic/Match.kt`; adicionar `MINE_INTERVAL=5f, MINE_SP_PER_PIP=5, GROWTH_INTERVAL=20f` a `Balance.kt`
- Test: `MatchTest.kt`

**Interfaces:**
- Consumes: tudo das Tasks 2–5
- Produces: `class Match(val deck: List<DieType>, private val rng: Random = Random.Default)` com:
  - Estado:
    - `val board: Board`
    - `var sp: Int` (100)
    - `var lives: Int` (3)
    - `var wave: Int` (0 antes da primeira onda; vira 1 após 3 s)
    - `val enemies: List<Enemy>`
    - `val isGameOver: Boolean`
    - `val buyCost: Int`
    - `val lastShots: List<ShotEvent>` (tiros da última chamada de `update`)
    - `val waveCountdown: Float` (segundos até a próxima onda, 0 durante a onda)
  - Poder:
    - `fun powerLevel(t: DieType): Int`
    - `fun powerUpCost(t: DieType): Int?` (null no nível 5)
    - `fun powerMult(t: DieType): Float`
  - Ações:
    - `fun canBuy(): Boolean`
    - `fun buyDie(): Boolean`
    - `fun canMerge(from: Cell, to: Cell): Boolean`
    - `fun merge(from: Cell, to: Cell): Boolean`
    - `fun powerUp(t: DieType): Boolean`
    - `fun update(dt: Float)`
  - Todas as ações retornam false e não mudam nada se `isGameOver`.

  `update` limita `dt` a `MAX_FRAME`, acumula e roda `step()` de 1/60 s. Ordem dentro de `step()`:
  1. **Spawner:** controla a contagem regressiva entre ondas e a fila de spawn a cada 0,6 s.
  2. **Inimigos:** `tickEffects`, depois `distance += speed*dt`; quem chega a `PATH.length` sai e tira `lifeDamage` vidas (mínimo 0).
  3. **Habilidades de chefe:** a cada 6 s. Silenciador: dado não desativado aleatório recebe `disabledFor=5`. Trocador: `swap` de 2 casas ocupadas aleatórias, se houver ≥2. Regenerador: já tratado em `tickEffects`.
  4. **Dados:** `disabledFor -= dt`.
     - MINE: `timer += dt`; a cada 5 s, `sp += round(5*pips*powerMult)`.
     - GROWTH: `timer += dt`; a cada 20 s, `pips = min(7, pips+1)`.
     - Atacantes não desativados: `cooldown -= dt`; com `cooldown <= 0`, chamam `Combat.attack(...)`. Se houve alvo, `cooldown += attackInterval`; sem alvo, `cooldown = 0`.
  5. **Mortos:** remover os inimigos com `hp <= 0` e pagar `reward` uma única vez.

  A próxima onda começa 3 s depois de a fila de spawn esvaziar e não haver inimigos vivos.

- [ ] **Step 1: Testes que falham** (`Random(7)`, deck padrão):
  - `startsWith100SpAnd3Lives`; `buyCostIncreases`: 10, depois 20, depois 30; `sp` cai 10+20.
  - `buyPlacesOnePipDeckDie`.
  - `buyOnFullBoardKeepsSp`: encher 15 casas com `board[c] = Die(...)`, `sp=1000` → `buyDie()==false`, sp continua 1000 e `buyCost` não muda.
  - `cannotBuyWithoutSp`.
  - `powerUpCostsAndCaps`: com sp alto, os custos são 100, 200, 400, 700 e depois `powerUpCost==null` e `powerUp==false`; `powerMult` no nível 5 é 2.5.
  - `firstWaveStartsAfter3s`: `update(2.9f)` → wave 0; mais 0,2 s → wave 1; após 0,6×10 s, os 10 inimigos da onda 1 nasceram.
  - `leakingCostsLives`: sem dados, rodar até a onda 1 acabar → lives 0 e `isGameOver`; depois, `buyDie()==false`.
  - `killRewardPaidOnce`: inimigo com hp 1 e 3 dados FIRE prontos (`cooldown=0`) → depois de um passo, sp aumentou exatamente 10 e `enemies` está vazio.
  - `mineGeneratesSp`: MINE 2 pips, 5 s sem inimigos → +10 SP.
  - `growthAddsPip`: GROWTH 1 → 20 s → pips 2; parado em 7.
  - `silencedDieDoesNotAttack`.
  - `hugeDeltaIsClamped`: `update(10f)` avança no máximo 0,25 s (ex.: `waveCountdown` cai de 3.0 para ≥2.75).
  - `bossAbilitiesWithEmptyBoard`: forçar a onda 10 (`internal fun debugStartWave(n: Int)`) com tabuleiro vazio e com 1 dado, rodar 13 s → sem exceção; o mesmo para a onda 20 (Trocador).
  - `bossTakesTwoLives`.
- [ ] **Step 2: Rodar** → FAIL.
- [ ] **Step 3: Implementar.** `debugStartWave` é `internal` e serve só para testes.
- [ ] **Step 4: Rodar** `./gradlew :core:test` → PASS (todos os testes de logic).
- [ ] **Step 5: Commit** — `feat(logic): match loop with economy, waves, boss abilities`

---

### Task 7: Shell do jogo, salvamento e telas de menu/deck/game over

**Files:**
- Create: `ui/MergeTdGame.kt` (substitui o stub), `ui/SaveData.kt`, `ui/Skin.kt`, `ui/Button.kt`, `ui/MenuScreen.kt`, `ui/DeckScreen.kt`, `ui/GameOverScreen.kt`

**Interfaces:**
- Consumes: `DeckCodec`, `DieType`, `EnemyKind`
- Produces:
  - `class MergeTdGame : Game()` com `lateinit var shapes: ShapeRenderer; lateinit var batch: SpriteBatch; lateinit var font: BitmapFont; lateinit var save: SaveData` e as funções `fun showMenu()`, `fun showDeck()`, `fun startMatch()`, `fun showGameOver(wave: Int, isRecord: Boolean)`.
  - `class SaveData(prefs: Preferences) { var bestWave: Int; var deck: List<DieType> }`, com chaves `"bestWave"` e `"deck"`, usando `DeckCodec` e `flush()` ao gravar. Prefs com nome `"merge-td"`.
  - `object Skin { fun color(t: DieType): Color; fun color(k: EnemyKind): Color; fun label(t: DieType): String }`, com as cores da tabela da spec. Os rótulos em PT: "Fogo", "Gelo", "Veneno", "Elétrico", "Vento", "Ferro", "Mina", "Sniper", "Espelho", "Cresc.".
  - `class Button(val rect: Rectangle, var text: String, var enabled: Boolean = true) { fun contains(x: Float, y: Float): Boolean; fun draw(shapes, batch, font) }`.
  - Cada tela tem seu próprio `FitViewport(720f, 1280f)`, e o toque é convertido com `viewport.unproject`.
  - A fonte padrão `BitmapFont()` é escalada (`font.data.setScale(2.5f)`). Se ela não renderizar acentos (conferir no Step 6), todos os textos da UI ficam sem acento.

- [ ] **Step 1: Implementar o `MergeTdGame`.** No `create()`, ele inicializa os recursos e chama `showMenu()`. No `dispose()`, libera os recursos.
- [ ] **Step 2: Implementar o `MenuScreen`.** Título, botões "Jogar" e "Deck", e o texto "Recorde: onda N".
- [ ] **Step 3: Implementar o `DeckScreen`.** Grade 2×5 com os 10 tipos (quadrado na cor do tipo e o rótulo). Tocar alterna a seleção, com no máximo 5 selecionados (um 6º toque é ignorado). O botão "Confirmar" só fica ativo com exatamente 5; ao confirmar, salva `save.deck` e volta ao menu. Começa com o deck salvo.
- [ ] **Step 4: Implementar o `GameOverScreen`.** Mostra "Fim de jogo", "Onda N", "Novo recorde!" se for o caso, e o botão "Menu".
- [ ] **Step 5: Implementar `startMatch()` temporário,** que mostra o `GameOverScreen` com a onda 0 até a Task 8 existir.
- [ ] **Step 6: Verificar.** `./gradlew :core:test` passa; rodar `./gradlew :lwjgl3:run` em background e, com o computer-use (screenshot), conferir que dá para navegar Menu → Deck (escolher 5 e confirmar) → Menu, e que o deck fica salvo ao reabrir.
- [ ] **Step 7: Commit** — `feat(ui): game shell, save data, menu/deck/game-over screens`

---

### Task 8: Tela da partida (render, HUD, arrasto, pausa)

**Files:**
- Create: `ui/MatchScreen.kt`, `ui/MatchRenderer.kt`, `ui/DragController.kt`
- Modify: `ui/MergeTdGame.kt` (`startMatch` cria `Match(save.deck)`)

**Interfaces:**
- Consumes: `Match` (Task 6), `Layout`, `Skin`, `Button`, `SaveData`
- Produces:
  - `class DragController(private val match: Match) { val dragging: Cell?; val pointer: Vec2?; fun down(p: Vec2): Boolean; fun move(p: Vec2); fun up(p: Vec2): Boolean; fun highlights(): List<Cell> }`, onde `highlights()` são as células com `match.canMerge(dragging, c)`. `up` sobre uma célula válida faz o merge; em qualquer outro caso, cancela.
  - `class MatchRenderer(shapes, batch, font) { fun render(match: Match, drag: DragController) }`

O que a tela desenha:
- **Caminho:** faixa de 40 px seguindo `Layout.PATH`.
- **Grid:** casas vazias em cinza escuro; casas destacadas com contorno branco.
- **Dados:** quadrado arredondado de 104 px na cor do tipo, com pips (pontos brancos em layout de dado; 7 = 6 + centro) e escurecido se `disabled`. O dado arrastado é desenhado no ponteiro.
- **Inimigos:** círculos (raio 16; chefe 32; tanque 22) com barra de vida acima, tom azulado se `slow>0` e borda verde se houver veneno.
- **Tiros:** `lastShots` desenhados como linhas finas na cor do tipo, que somem em 0,1 s (guarde-os com tempo de vida no renderer).
- **HUD no topo:** "Onda N", "♥ x3" ou "Vidas 3", "SP N" e, entre ondas, "Próxima onda em X".
- **Barra inferior:** botão "Comprar (custo)" desativado quando `!canBuy()`; abaixo, 5 botões de power-up com a cor do tipo, "Nv X" e o custo (ou "MAX").
- **Botão "II":** pausa e mostra uma sobreposição com "Continuar" e "Sair" (Sair vai para o game over com a onda atual).

Comportamento:
- `pause()` do libGDX (app em segundo plano) ativa a pausa.
- Pausado, não chama `match.update`.
- Quando `match.isGameOver`: `isRecord = wave > save.bestWave`; se for recorde, salva; depois chama `showGameOver`.

- [ ] **Step 1: Teste do `DragController`** em `core/src/test/.../ui/DragControllerTest.kt`, sem dependência gráfica:
  - `dropOnCompatibleMerges`.
  - `dropOnIncompatibleCancels`: tabuleiro inalterado.
  - `dropOutsideCancels`.
  - `downOnEmptyCellDoesNothing`: retorna false.

  Use `Layout.cellCenter` como coordenadas e coloque dados em `match.board` diretamente.
- [ ] **Step 2: Rodar** → FAIL.
- [ ] **Step 3: Implementar `DragController`** → teste PASS.
- [ ] **Step 4: Implementar `MatchRenderer` e `MatchScreen`** e ligar `startMatch()`.
- [ ] **Step 5: Verificar jogando** com `:lwjgl3:run` e o computer-use:
  - comprar até encher;
  - arrastar para fazer merge (os destaques aparecem);
  - power-up;
  - inimigos andando, tomando dano e dando SP;
  - pausa;
  - deixar perder → tela de game over com recorde;
  - menu → jogar de novo.

  Anotar e corrigir o que estiver quebrado.
- [ ] **Step 6: Commit** — `feat(ui): match screen with rendering, HUD, drag-to-merge and pause`

---

### Task 9: APK final e verificação

**Files:**
- Modify: `android/AndroidManifest.xml` (label "Merge TD", `screenOrientation="portrait"`, `configChanges` padrão do libGDX), `android/build.gradle.kts` (versionName "0.1.0")
- Create: `README.md` (como rodar no PC, como gerar e instalar o APK)

- [ ] **Step 1: Rodar** `./gradlew :core:test` → todos passam (anexe a contagem ao relatório).
- [ ] **Step 2: Gerar o APK.** `./gradlew :android:assembleDebug` → `android/build/outputs/apk/debug/android-debug.apk` existe.
- [ ] **Step 3: Jogar uma partida completa no desktop** até o game over, passando de pelo menos uma onda com tanques. Se der tempo, ir até a onda 10 com `debugStartWave` exposto por uma flag de desenvolvimento só no lwjgl3 (opcional, não entra no APK).
- [ ] **Step 4: README.** Explicar `./gradlew :lwjgl3:run`, `./gradlew :android:assembleDebug` e a instalação: copiar o APK para o celular e permitir apps de fontes desconhecidas, ou `adb install -r android/build/outputs/apk/debug/android-debug.apk` com o celular em modo desenvolvedor.
- [ ] **Step 5: Commit** — `chore: android packaging and README`
