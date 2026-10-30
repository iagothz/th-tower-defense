# Salvamento em nuvem (Play Games) — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O progresso do Merge TD (nível, dados, cartas, recordes, histórico) sobe para o Google Play Games ao fim de cada partida e desce ao abrir o jogo, com uma tela de escolha quando dois aparelhos avançaram separados.

**Architecture:** No `core`, `SaveData` ganha export/import do mapa de preferências em texto tipado; `CloudSync` é a tabela de decisão pura; `CloudController` liga abertura, fim de partida, nível de dado e deck a uma interface `CloudSave`. O módulo `android` implementa `CloudSave` com Play Games Services v2 (Saved Games); desktop e sim usam `NoCloud`. Config mostra o estado; `CloudConflictScreen` resolve o conflito.

**Tech Stack:** Kotlin 2.1.20, libGDX 1.13.1, `com.google.android.gms:play-services-games-v2:21.0.0` (só no módulo android), testes com `kotlin.test` via `./gradlew test`.

**Spec:** `docs/superpowers/specs/2026-10-30-cloud-save-design.md`

## Global Constraints

- Nada de Play Services no `core`: o `core` só conhece `CloudSave`, e `./gradlew test` continua rodando sem Android.
- Toda callback de `CloudSave` chega na thread do GL (`Gdx.app.postRunnable` no Android); o `core` nunca bloqueia esperando a nuvem.
- Chaves locais, nunca no blob: `soundVolume`, `musicVolume`, `sound`, `music`, `vibration`, `reduceMotion`, `largeText`, `language`, `match`, `matchBest` e qualquer `sync.*`. (`matchBest` acompanha `match`; a spec é corrigida na Task 7.)
- A nuvem nunca é aplicada enquanto a tela atual é `MatchScreen` ou `GameOverScreen`.
- Textos de UI em português como chave, com tradução EN em `Strings.kt` (o `StringsTest` exige tradução para toda chave usada).
- Commits: mensagem em inglês, estilo `feat:`/`docs:`, com a linha `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. Push só quando o usuário pedir.
- Versão final 0.7.0 / versionCode 7 (`android/build.gradle.kts` e `MergeTdGame.VERSION`).

## Review Focus

1. Blob da nuvem escrito por uma versão futura do jogo com chaves desconhecidas: `import` deve aplicá-las (são só preferências), não rejeitar. Teste em Task 1.
2. Valor de preferência contendo `=` (códigos de partida, histórico): a linha `tipo:chave=valor` deve cortar só no primeiro `=`. Teste em Task 1.
3. Resposta da nuvem chegando depois que o jogador já começou uma partida: `Adopt`/`Ask` ficam pendentes e nunca trocam o save no meio da partida. Teste em Task 3.
4. `push()` chamado enquanto outro upload está no ar (fim de partida seguido de subir nível no Deck): uma única subida extra depois da atual, com a versão certa. Teste em Task 3.
5. Jogador desliga a nuvem no meio de um download: o resultado que chega depois é ignorado. Teste em Task 3.

---

### Task 1: Export e import do save (`CloudBlob`)

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudBlob.kt`
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/SaveData.kt` (fim da classe)
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/CloudBlobTest.kt`

**Interfaces:**
- Consumes: `SaveDataTest.memoryPrefs()` (fake de `Preferences` já existente; ele implementa `get`, `put*`, `remove`, `flush`).
- Produces:
  - `object CloudBlob { val LOCAL_KEYS: Set<String>; fun isLocal(key: String): Boolean; fun encode(values: Map<String, Any?>): String; fun decode(blob: String): Map<String, Any?>?; fun hash(blob: String): String; fun version(blob: String): Int?; data class Summary(val level: Int, val games: Int, val day: Int?); fun summary(blob: String): Summary }`
  - `SaveData.export(): String` e `SaveData.import(blob: String): Boolean`.

`isLocal` é verdadeiro para as chaves da lista de Global Constraints e para qualquer chave que comece com `sync.`. `encode` escreve uma linha por chave não local, ordem alfabética, `i:`/`s:`/`b:`/`f:` + chave + `=` + valor; no valor, `\` vira `\\` e quebra de linha vira `\n`. `decode` devolve `null` se qualquer linha não tiver o formato `tipo:chave=valor` com tipo entre os quatro ou se um `i:`/`f:`/`b:` não converter; chaves locais presentes no blob são ignoradas. `hash` é o SHA-256 em hex das linhas que não começam com `i:sync.`. `version` lê `i:sync.version`. `summary` lê `xp` (→ `Progression.level`), `total.games` e `sync.at`.

- [ ] **Step 1: Escrever os testes**

```kotlin
class CloudBlobTest {
    @Test fun encodeSkipsLocalKeysAndSortsTheRest() {
        val blob = CloudBlob.encode(mapOf("xp" to 140, "soundVolume" to 0.5f, "deck" to "fire,ice", "sync.version" to 3, "tutorialDone" to true))
        assertEquals("b:tutorialDone=true\ns:deck=fire,ice\ni:xp=140", blob)
    }
    @Test fun decodeRoundTripsEveryTypeAndEscapes() {
        val values = mapOf("xp" to 7, "history" to "a=b\nc\\d", "tutorialDone" to false, "ratio" to 0.25f)
        assertEquals(values, CloudBlob.decode(CloudBlob.encode(values)))
    }
    @Test fun decodeKeepsEverythingAfterTheFirstEquals() {
        assertEquals(mapOf("code" to "AB=CD=E"), CloudBlob.decode("s:code=AB=CD=E"))
    }
    @Test fun decodeAcceptsUnknownKeysButRejectsBadLines() {
        assertEquals(mapOf("future.thing" to 1), CloudBlob.decode("i:future.thing=1"))
        assertNull(CloudBlob.decode("xp=1"))
        assertNull(CloudBlob.decode("i:xp=abc"))
        assertNull(CloudBlob.decode("z:xp=1"))
    }
    @Test fun decodeDropsLocalKeys() {
        assertEquals(mapOf("xp" to 1), CloudBlob.decode("i:xp=1\nf:soundVolume=0.5\ni:sync.at=3"))
    }
    @Test fun hashIgnoresSyncLines() {
        assertEquals(CloudBlob.hash("i:xp=1"), CloudBlob.hash("i:xp=1\ni:sync.version=4\ni:sync.at=9"))
        assertNotEquals(CloudBlob.hash("i:xp=1"), CloudBlob.hash("i:xp=2"))
    }
    @Test fun versionAndSummaryReadTheBlob() {
        val blob = "i:sync.at=20000\ni:sync.version=5\ni:total.games=42\ni:xp=1310"
        assertEquals(5, CloudBlob.version(blob))
        assertNull(CloudBlob.version("i:xp=1"))
        assertEquals(CloudBlob.Summary(Progression.level(1310), 42, 20000), CloudBlob.summary(blob))
        assertEquals(CloudBlob.Summary(1, 0, null), CloudBlob.summary(""))
    }
    @Test fun saveDataExportsAndImportsWithoutTouchingLocalKeys() {
        val prefs = SaveDataTest.memoryPrefs()
        val save = SaveData(prefs)
        save.xp = 300; save.soundVolume = 0.3f; save.deck = listOf(DieType.FIRE, DieType.ICE, DieType.POISON, DieType.ELECTRIC, DieType.WIND)
        val blob = save.export()
        assertFalse("soundVolume" in blob)
        val other = SaveData(SaveDataTest.memoryPrefs()).apply { soundVolume = 0.9f; xp = 1 }
        assertTrue(other.import(blob))
        assertEquals(300, other.xp); assertEquals(0.9f, other.soundVolume); assertEquals(save.deck, other.deck)
    }
    @Test fun importReplacesTheWholeSyncedSetAndRejectsBadBlobs() {
        val save = SaveData(SaveDataTest.memoryPrefs()).apply { xp = 50; recordWave(GameMap.DEFAULT, 12) }
        assertTrue(save.import("i:xp=9"))
        assertEquals(9, save.xp); assertEquals(0, save.bestWave(GameMap.DEFAULT))
        assertFalse(save.import("garbage"))
        assertEquals(9, save.xp)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :core:test --tests 'com.iagothz.mergetd.ui.CloudBlobTest' -q`
Expected: falha de compilação, `Unresolved reference 'CloudBlob'`.

- [ ] **Step 3: Implementar `CloudBlob` e `SaveData.export/import`**

`export()` = `CloudBlob.encode(prefs.get())`. `import(blob)`: `decode` → se `null`, devolve `false`; senão remove toda chave não local de `prefs.get()`, aplica o mapa com `putInteger/putString/putBoolean/putFloat` por tipo, `flush()`, `true`. O fake de teste responde a `get`, `put*`, `remove` e `flush`; não use `clear()`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew test -q`
Expected: sem falhas (384+ testes).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/com/iagothz/mergetd/ui/CloudBlob.kt core/src/main/kotlin/com/iagothz/mergetd/ui/SaveData.kt core/src/test/kotlin/com/iagothz/mergetd/ui/CloudBlobTest.kt
git commit -m "feat: export and import the save as a typed text blob"
```

---

### Task 2: Regra de sincronização (`CloudSync`)

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudSync.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/CloudSyncTest.kt`

**Interfaces:**
- Produces: `object CloudSync { sealed interface Action { data object Nothing; data class Upload(val version: Int); data object Adopt; data object Ask }; fun decide(localVersion: Int, localChanged: Boolean, cloudVersion: Int?): Action }`

- [ ] **Step 1: Escrever o teste** (uma asserção por célula da tabela da spec)

```kotlin
class CloudSyncTest {
    @Test fun noCloudUploadsVersionOne() { assertEquals(Upload(1), decide(0, false, null)); assertEquals(Upload(1), decide(3, true, null)) }
    @Test fun sameVersionUploadsOnlyWhenLocalChanged() { assertEquals(Nothing, decide(4, false, 4)); assertEquals(Upload(5), decide(4, true, 4)) }
    @Test fun cloudAheadAdoptsWhenLocalUnchangedElseAsks() { assertEquals(Adopt, decide(4, false, 6)); assertEquals(Ask, decide(4, true, 6)) }
    @Test fun cloudBehindIsOverwritten() { assertEquals(Upload(5), decide(4, false, 2)); assertEquals(Upload(5), decide(4, true, 0)) }
}
```

- [ ] **Step 2: Rodar e ver falhar** — Run: `./gradlew :core:test --tests 'com.iagothz.mergetd.ui.CloudSyncTest' -q` — Expected: `Unresolved reference 'CloudSync'`.
- [ ] **Step 3: Implementar `decide`** como um `when` sobre a tabela.
- [ ] **Step 4: Rodar e ver passar** — Run: `./gradlew :core:test -q` — Expected: sem falhas.
- [ ] **Step 5: Commit** — `git commit -m "feat: cloud sync decision table"`

---

### Task 3: `CloudSave`, `CloudController` e os pontos de disparo

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudSave.kt` (interface, `CloudResult`, `NoCloud`)
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudController.kt`
- Create: `core/src/test/kotlin/com/iagothz/mergetd/ui/FakeCloud.kt`
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/SaveData.kt` (chaves `sync.*`)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/MergeTdGame.kt:21` (parâmetro), `:37-47` (`create`), `apply`/`switchTo` (flag de partida)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/MatchScreen.kt:502-510` (`finish`)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/DeckScreen.kt` (`levelUp`, salvar deck e slot)
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/CloudControllerTest.kt`

**Interfaces:**
- Consumes: `CloudBlob`, `SaveData.export/import`, `CloudSync.decide` (Tasks 1–2).
- Produces:

```kotlin
sealed interface CloudResult<out T> { data class Ok<T>(val value: T) : CloudResult<T>; data class Failed(val reason: String) : CloudResult<Nothing> }
interface CloudSave {
    val available: Boolean
    fun signIn(done: (Boolean) -> Unit)
    fun isSignedIn(done: (Boolean) -> Unit)
    fun download(done: (CloudResult<String?>) -> Unit)
    fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit)
}
object NoCloud : CloudSave   // available=false; signIn/isSignedIn → false; download/upload → Failed("unavailable")

class CloudController(private val save: SaveData, private val cloud: CloudSave, private val today: () -> Int = DailyChallenge::today) {
    sealed interface Status { data object Unavailable; data object Off; data object SignedOut; data object Working; data class Synced(val day: Int); data object Offline; data class Error(val reason: String) }
    val status: Status
    var inMatch: Boolean            // o jogo liga em MatchScreen/GameOverScreen; ao desligar, aplica o pendente
    var notice: String?             // "Progresso da nuvem aplicado"; a tela que mostra zera
    val conflict: String?           // blob da nuvem esperando a tela de conflito, ou null
    fun open()                      // fluxo de abertura (isSignedIn → download → decide → executa)
    fun signIn()                    // botão Entrar: signIn → open()
    fun setEnabled(on: Boolean)     // Ligar/Desligar; ligar chama open()
    fun push()                      // sobe o local como sync.version+1 (enfileira se já há upload)
    fun keepLocal()                 // escolha na tela de conflito → Upload(cloudVersion+1)
    fun keepCloud()                 // escolha na tela de conflito → import + versão da nuvem
    var onAdopted: () -> Unit       // o jogo recria as telas (showTab(HOME))
}
```

- `SaveData` ganha `var syncEnabled: Boolean` (`sync.enabled`, padrão `true`), `var syncVersion: Int` (`sync.version`, 0), `var syncHash: String` (`sync.hash`, ""), `var syncLastOk: Int?` (`sync.lastOk`), `fun localChanged() = CloudBlob.hash(export()) != syncHash`.
- Subir: blob = `export()` + `"\ni:sync.version=$v\ni:sync.at=${today()}"`; descrição `"nível %d · %d partidas"`. Sucesso → `syncVersion = v`, `syncHash = hash(blob)`, `syncLastOk = today()`, status `Synced`. `Failed("offline")` → `Offline`; `Failed("signed-out")` → `SignedOut`; outro → `Error`.
- Adotar: `import(blob)` (se falhar → `Error("blob")` e tratar como `cloudVersion = 0` na próxima decisão) → `syncVersion = version(blob)`, `syncHash = hash(blob)`, `save.clearMatch()`, `notice = "Progresso da nuvem aplicado"`, `onAdopted()`.
- Uma "geração" (`Int`) incrementada em `setEnabled(false)` e em cada `open()`: callbacks de uma geração antiga são descartadas.

`FakeCloud` (teste): `var available = true`, `var signedIn = true`, `var offline = false`, `var blob: String? = null`, `val uploads = mutableListOf<String>()`, `var deliverLater = false` com `fun deliver()` que entrega as callbacks presas (para simular resposta chegando depois).

- [ ] **Step 1: Escrever os testes**

```kotlin
class CloudControllerTest {
    private fun save() = SaveData(SaveDataTest.memoryPrefs()).apply { xp = 100 }
    @Test fun openingWithNoCloudUploadsVersionOne() { val c = CloudController(save(), fake); c.open(); assertEquals(1, fake.uploads.size); assertEquals(1, CloudBlob.version(fake.blob!!)); assertEquals(Synced(today), c.status) }
    @Test fun openingWithCloudAheadAdoptsSilently() { fake.blob = "i:sync.at=1\ni:sync.version=3\ni:xp=900"; c.open(); assertEquals(900, save.xp); assertEquals(3, save.syncVersion); assertEquals("Progresso da nuvem aplicado", c.notice); assertTrue(adopted) }
    @Test fun openingWithBothChangedAsks() { save.syncVersion = 2; save.syncHash = "stale"; fake.blob = "...version=3..."; c.open(); assertNotNull(c.conflict); assertEquals(100, save.xp) }
    @Test fun keepLocalUploadsAboveTheCloudVersion() { ...Ask...; c.keepLocal(); assertEquals(4, CloudBlob.version(fake.blob!!)); assertNull(c.conflict) }
    @Test fun keepCloudImportsIt() { ...Ask...; c.keepCloud(); assertEquals(900, save.xp); assertNull(c.conflict) }
    @Test fun cloudResultDuringAMatchWaitsForTheMenu() { fake.deliverLater = true; c.open(); c.inMatch = true; fake.deliver(); assertEquals(100, save.xp); c.inMatch = false; assertEquals(900, save.xp) }
    @Test fun pushWhileUploadingRunsOnceMoreAfterwards() { fake.deliverLater = true; c.push(); c.push(); c.push(); fake.deliver(); fake.deliver(); assertEquals(2, fake.uploads.size); assertEquals(2, save.syncVersion) }
    @Test fun offlinePushLeavesHashDivergentSoNextOpenUploads() { fake.offline = true; c.push(); assertEquals(Offline, c.status); fake.offline = false; c.open(); assertEquals(1, fake.uploads.size) }
    @Test fun disabledDoesNothingAndDropsLateResults() { fake.deliverLater = true; c.open(); c.setEnabled(false); fake.deliver(); assertEquals(100, save.xp); assertEquals(Off, c.status); c.push(); assertTrue(fake.uploads.isEmpty()) }
    @Test fun signedOutShowsSignedOutAndSignInOpens() { fake.signedIn = false; c.open(); assertEquals(SignedOut, c.status); fake.signedIn = true; c.signIn(); assertEquals(1, fake.uploads.size) }
    @Test fun unavailableCloudIsUnavailable() { fake.available = false; assertEquals(Unavailable, c.status); c.open(); assertTrue(fake.uploads.isEmpty()) }
    @Test fun corruptCloudBlobIsOverwrittenByLocal() { fake.blob = "garbage"; c.open(); assertEquals(1, CloudBlob.version(fake.blob!!)) }
}
```

(Os `...` são o mesmo arranjo do teste anterior; escreva cada um completo.)

- [ ] **Step 2: Rodar e ver falhar** — Run: `./gradlew :core:test --tests 'com.iagothz.mergetd.ui.CloudControllerTest' -q` — Expected: `Unresolved reference 'CloudController'`.
- [ ] **Step 3: Implementar `CloudSave.kt`, `CloudController.kt`, as chaves `sync.*` em `SaveData`**
- [ ] **Step 4: Rodar e ver passar** — Run: `./gradlew :core:test -q` — Expected: sem falhas.
- [ ] **Step 5: Ligar ao jogo**
  - `MergeTdGame(val devMode: Boolean = false, private val shareText: ((String) -> Unit)? = null, cloudSave: CloudSave = NoCloud)`; `lateinit var cloud: CloudController`; em `create()`, depois de `save.retroCards(...)`: `cloud = CloudController(save, cloudSave).also { it.onAdopted = { showTab(Tab.HOME) } }` e, após `showMenu()`/`resumeSavedMatch()`, `cloud.open()`.
  - Em `apply(screen)` (o ponto único onde a tela muda): `cloud.inMatch = screen is MatchScreen || screen is GameOverScreen`.
  - `MatchScreen.finish()`: depois de `book()`, `if (!game.devMode) game.cloud.push()`.
  - `DeckScreen`: após `game.save.levelUp(shown)` bem-sucedido, após `game.save.saveDeckSlot(...)` e após `game.save.deck = ...`: `game.cloud.push()`.
- [ ] **Step 6: Rodar tudo** — Run: `./gradlew test -q` — Expected: sem falhas.
- [ ] **Step 7: Commit** — `git commit -m "feat: cloud controller wired to startup, match end and deck changes"`

---

### Task 4: Linha "Nuvem" em Config e aviso no Início

**Files:**
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/SettingsScreen.kt` (linha entre idiomas e privacidade; a linha de privacidade sobe ou encurta)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/MenuScreen.kt` (aviso `cloud.notice` por 3 s, `tinyFont` dourado acima do botão Jogar)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/Strings.kt` (EN)
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudText.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/CloudTextTest.kt`

**Interfaces:**
- Consumes: `CloudController.Status`, `.signIn()`, `.setEnabled()`, `.open()`, `.notice`.
- Produces: `object CloudText { fun key(status: Status): String; fun line(status: Status, today: Int): String; fun button(status: Status): String? }`. `key` é a chave PT da tabela (com `%d` no caso de dias); `line` é o texto já traduzido e formatado (`S[...]`/`S.f`); `button` é a chave PT do botão ou null (sem botão); `Unavailable` → `key` e `line` vazias.

Textos (PT → EN): "Nuvem: desligada" → "Cloud: off"; "Nuvem: sem conta" → "Cloud: not signed in"; "Nuvem: sincronizando…" → "Cloud: syncing…"; "Nuvem: sincronizada hoje" → "Cloud: synced today"; "Nuvem: sincronizada há %d dias" → "Cloud: synced %d days ago"; "Nuvem: sem conexão" → "Cloud: no connection"; "Nuvem: falhou" → "Cloud: failed"; botões "Entrar" → "Sign in", "Ligar" → "Turn on", "Desligar" → "Turn off", "Tentar" → "Retry"; "Progresso da nuvem aplicado" → "Cloud progress applied". Linha de privacidade nova: "Privacidade: este jogo funciona sem internet e não coleta nada. Com a nuvem ligada, seu progresso fica na sua conta do Google Play Games." → EN equivalente.

- [ ] **Step 1: Teste**

```kotlin
class CloudTextTest {
    @Test fun eachStatusHasALineAndTheRightButton() {
        assertEquals("" to null, CloudText.line(Unavailable, 10) to CloudText.button(Unavailable))
        assertEquals("Nuvem: desligada" to "Ligar", CloudText.line(Off, 10) to CloudText.button(Off))
        assertEquals("Nuvem: sem conta" to "Entrar", CloudText.line(SignedOut, 10) to CloudText.button(SignedOut))
        assertEquals("Nuvem: sincronizando…" to null, CloudText.line(Working, 10) to CloudText.button(Working))
        assertEquals("Nuvem: sincronizada hoje" to "Desligar", CloudText.line(Synced(10), 10) to CloudText.button(Synced(10)))
        assertEquals("Nuvem: sincronizada há 3 dias", CloudText.line(Synced(7), 10))
        assertEquals("Nuvem: sem conexão" to "Desligar", CloudText.line(Offline, 10) to CloudText.button(Offline))
        assertEquals("Nuvem: falhou" to "Tentar", CloudText.line(Error("x"), 10) to CloudText.button(Error("x")))
    }
    @Test fun everyTextIsTranslated() {
        for (s in listOf(Off, SignedOut, Working, Synced(1), Synced(0), Offline, Error("x"))) {
            assertTrue(S.has(Language.EN, CloudText.key(s)), CloudText.key(s))
            CloudText.button(s)?.let { assertTrue(S.has(Language.EN, it), it) }
        }
    }
}
```

(Os testes de `line` rodam com `S.language = Language.PT`, o padrão; `CloudText.key(Synced(7))` é `"Nuvem: sincronizada há %d dias"`.)

- [ ] **Step 2: Rodar e ver falhar** — Expected: `Unresolved reference 'CloudText'`.
- [ ] **Step 3: Implementar `CloudText`, a linha em Config (`smallFont`, y entre 460 e 480, botão `Button(Rectangle(468f, 440f, 192f, 56f))` à direita, cor `Skin.PANEL`), o aviso no Início (3 s) e as strings EN.** Botão: `SignedOut` → `cloud.signIn()`; `Off` → `setEnabled(true)`; `Synced`/`Offline` → `setEnabled(false)`; `Error` → `open()`. Com `Unavailable`, nada é desenhado e a privacidade mantém o texto antigo.
- [ ] **Step 4: Rodar tudo** — Run: `./gradlew test -q` — Expected: sem falhas.
- [ ] **Step 5: Commit** — `git commit -m "feat: cloud status row in settings and adoption notice on the home tab"`

---

### Task 5: `CloudConflictScreen`

**Files:**
- Create: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudConflictScreen.kt`
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/MergeTdGame.kt` (`showCloudConflict(blob)`; `CloudController` chama por `var onConflict: (String) -> Unit`, chamado quando `conflict` fica não nulo e `inMatch` é falso)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/CloudController.kt` (`onConflict`)
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/Strings.kt`
- Test: `core/src/test/kotlin/com/iagothz/mergetd/ui/CloudControllerTest.kt` (um teste a mais)

Tela sem barra (`tab = null`): título "Qual progresso manter?" (`bigFont`, y 1100), dois painéis `Rectangle(40f, 700f, 640f, 300f)` e `Rectangle(40f, 360f, 640f, 300f)`, cada um com título ("Este aparelho" / "Nuvem"), `CloudBlob.summary` do `save.export()` e do blob (`"nível %d · %d partidas"`, e `"hoje"`/`"há %d dias"` só no da nuvem, a partir de `sync.at`), e um `Button` de 560×80 na base ("Manter este" / "Manter a nuvem"). Toque → `cloud.keepLocal()` / `cloud.keepCloud()` → `showTab(Tab.HOME)`. `onKey` ignora voltar. Strings EN: "Qual progresso manter?" → "Which progress to keep?", "Este aparelho" → "This device", "Manter este" → "Keep this one", "Manter a nuvem" → "Keep the cloud", "nível %d · %d partidas" → "level %d · %d games", "hoje" → "today", "há %d dias" → "%d days ago".

- [ ] **Step 1: Teste em `CloudControllerTest`**: `conflictCallbackFiresOnlyOutsideAMatch` — com `inMatch = true` e `Ask`, `onConflict` não é chamado; ao `inMatch = false`, é chamado uma vez com o blob.
- [ ] **Step 2: Rodar e ver falhar** — Expected: `Unresolved reference 'onConflict'`.
- [ ] **Step 3: Implementar `onConflict`, a tela e `showCloudConflict`.**
- [ ] **Step 4: Rodar tudo** — Run: `./gradlew test -q` — Expected: sem falhas (o `StringsTest` cobre as chaves novas).
- [ ] **Step 5: Verificar no desktop** — Run: `./gradlew :lwjgl3:run --args="--dev"` não mostra a tela (NoCloud). Para vê-la uma vez, em dev mode a tecla `C` no Início chama `showCloudConflict(save.export())` (só `devMode`). Tirar screenshot mental: dois painéis legíveis, botões dentro.
- [ ] **Step 6: Commit** — `git commit -m "feat: cloud conflict screen"`

---

### Task 6: Android — `PlayGamesCloud`

**Files:**
- Modify: `android/build.gradle.kts` (dependência; versão 0.7.0 / 7)
- Modify: `android/AndroidManifest.xml` (meta-data APP_ID)
- Create: `android/res/values/games.xml` (`<string name="game_services_project_id" translatable="false">0</string>`)
- Create: `android/src/main/kotlin/com/iagothz/mergetd/android/PlayGamesCloud.kt`
- Modify: `android/src/main/kotlin/com/iagothz/mergetd/android/AndroidLauncher.kt`
- Modify: `core/src/main/kotlin/com/iagothz/mergetd/ui/MergeTdGame.kt` (`VERSION = "0.7.0"`)

**Interfaces:**
- Consumes: `CloudSave`, `CloudResult`.
- Produces: `class PlayGamesCloud(private val activity: Activity) : CloudSave`.

- `available = true`.
- `signIn`: `PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener { post { done(it.isSuccessful && it.result.isAuthenticated) } }`.
- `isSignedIn`: `isAuthenticated()` idem.
- `download`: `PlayGames.getSnapshotsClient(activity).open("save", true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)`; do `DataOrConflict`, `data` (conflito do Google já resolvido pela política) → `snapshotContents.readFully()`; array vazio → `Ok(null)`; depois `discardAndClose(snapshot)`.
- `upload`: `open` igual, `snapshotContents.writeBytes(blob.toByteArray())`, `commitAndClose(snapshot, SnapshotMetadataChange.Builder().setDescription(description).build())`.
- Falhas: `ApiException.statusCode == CommonStatusCodes.NETWORK_ERROR` → `Failed("offline")`; `SIGN_IN_REQUIRED` ou `GamesClientStatusCodes.SIGN_IN_REQUIRED`... → `Failed("signed-out")`; resto → `Failed("error:$code")`. `post` = `Gdx.app.postRunnable`.
- `AndroidLauncher.onCreate`: `PlayGamesSdk.initialize(this)` antes de `initialize(game, config)`; `MergeTdGame(shareText = ..., cloudSave = PlayGamesCloud(this))`.

- [ ] **Step 1: Dependência, manifesto, `games.xml`, versão** e compilar: Run: `./gradlew :android:assembleDebug -q` — Expected: sem erro.
- [ ] **Step 2: Implementar `PlayGamesCloud` e ligar no launcher.** Run: `./gradlew :android:assembleRelease -q` — Expected: sem erro e sem aviso novo do R8 (se houver `-dontwarn` necessário, adicionar em `proguard-rules.pro` com um comentário).
- [ ] **Step 3: Emulador (caminho de falha, sem projeto no console)** seguindo a memória `emulator-verification.md`: instalar o debug, abrir, Config mostra "Nuvem: sem conta · Entrar"; tocar Entrar não trava (volta a "sem conta" ou "falhou"); jogar uma partida até o fim de jogo e voltar ao Início sem erro no `adb logcat -s AndroidRuntime`. Capturar a tela de Config.
- [ ] **Step 4: Rodar tudo** — Run: `./gradlew test -q` — Expected: sem falhas.
- [ ] **Step 5: Commit** — `git commit -m "feat: Play Games Saved Games implementation on Android"`

---

### Task 7: Documentação

**Files:**
- Modify: `README.md:20` (nova bullet "Nuvem" após "Dados por nível"; e em "Gerar e instalar o APK" um parágrafo sobre `games.xml`)
- Modify: `PRIVACY.md` (PT e EN: parágrafo da nuvem; "contas ou serviços de terceiros" passa a "nenhum serviço além do Google Play Games, opcional, para guardar o progresso na sua conta")
- Modify: `store/README.md:43` ("Novidades": "Salvamento na nuvem com o Google Play Games." no início, PT e EN)
- Modify: `docs/superpowers/specs/2026-10-30-cloud-save-design.md` (`matchBest` passa para a lista de chaves locais; a linha do estado `ERROR` vira "Nuvem: falhou" com botão Tentar)
- Modify: `docs/superpowers/specs/2026-10-26-merge-tower-defense-design.md` (bullet "Nuvem" apontando para a spec nova; "sem nenhum recurso online" vira "online só para o salvamento em nuvem, opcional")
- Modify: `docs/TODO.md` (riscar "Salvamento na nuvem"; acrescentar "Configurar o Play Console e testar a nuvem em dois aparelhos" como pendência do usuário)

- [ ] **Step 1: Editar os seis arquivos.**
- [ ] **Step 2: Rodar tudo** — Run: `./gradlew test -q` — Expected: sem falhas.
- [ ] **Step 3: Commit** — `git commit -m "docs: cloud save in README, privacy, store listing and specs"`
- [ ] **Step 4: Gerar o APK release** — Run: `./gradlew :android:assembleRelease -q` — e enviar `android/build/outputs/apk/release/android-release.apk` ao usuário.
