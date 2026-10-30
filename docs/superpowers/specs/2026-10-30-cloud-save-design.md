# Salvamento em nuvem (Play Games) — Design

Data: 2026-10-30

## Objetivo

Jogar o Merge TD em dois aparelhos com a mesma conta Google sem perder nível, dados desbloqueados, cartas, níveis dos dados, recordes e histórico. O jogador entra no Google Play Games automaticamente ao abrir; o progresso sobe ao fim de cada partida e desce ao abrir o jogo. Sem conta, sem rede ou com a nuvem desligada, o jogo funciona exatamente como hoje, offline.

**Considero pronto quando:** com o projeto do Play Games Services configurado no Play Console, um aparelho joga uma partida, outro aparelho com a mesma conta abre o jogo e vê o nível, as cartas e o recorde novos; e quando os dois jogaram separados, o segundo mostra a tela de conflito e a escolha prevalece nos dois.

### Decisões tomadas

- **Serviço:** Google Play Games Services v2, recurso Saved Games (snapshots). Sem servidor próprio.
- **Login:** automático ao abrir (comportamento padrão do SDK v2), com botão **Entrar** em Config para quem recusou o popup. O v2 não tem "sair": o jogo oferece **Desligar a nuvem** (interruptor local) no lugar.
- **Conflito:** o save com mais progresso vence em silêncio quando só um dos dois avançou; quando os dois avançaram separados, uma tela pergunta qual manter. Nunca sobrescreve progresso sem avisar.
- **Fora do escopo:** conquistas e placares do Play Games, mesclar campo a campo, nuvem no desktop, sincronizar configurações do aparelho, sincronizar a partida em andamento.

## Camadas

```
core/ui/CloudSave.kt      interface CloudSave + NoCloud (desktop, sim, testes)
core/ui/CloudSync.kt      regra pura: o que fazer com o save local e o da nuvem
core/ui/CloudBlob.kt      export/import do SaveData em texto, hash, resumo (nível, partidas, data)
core/ui/CloudController.kt  liga tudo ao jogo: quando baixar, quando subir, quando perguntar
core/ui/CloudConflictScreen.kt  a tela "este aparelho × nuvem"
core/ui/SettingsScreen.kt   linha "Nuvem" com estado e botão
android/.../PlayGamesCloud.kt  CloudSave com play-services-games-v2
android/.../AndroidLauncher.kt  passa PlayGamesCloud ao MergeTdGame
```

`MergeTdGame` ganha o parâmetro `cloud: CloudSave = NoCloud` ao lado de `shareText`. O desktop e o simulador não passam nada.

### `CloudSave` (core)

```kotlin
interface CloudSave {
    val available: Boolean                       // false no NoCloud: a linha some de Config
    fun signIn(done: (Boolean) -> Unit)          // abre o fluxo do Google; false se recusou ou falhou
    fun isSignedIn(done: (Boolean) -> Unit)
    fun download(done: (CloudResult<String?>) -> Unit)   // null quando ainda não há snapshot
    fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit)
}

sealed interface CloudResult<out T> {
    data class Ok<T>(val value: T) : CloudResult<T>
    data class Failed(val reason: String) : CloudResult<Nothing>   // "offline", "signed-out", "error:<msg>"
}
```

Toda callback é entregue **na thread do GL** (`Gdx.app.postRunnable` no Android). O `core` nunca bloqueia esperando a nuvem.

## O que sincroniza (`CloudBlob`)

`SaveData.export(): String` escreve o mapa de `Preferences` (`prefs.get()`) uma chave por linha, em ordem alfabética, com o tipo na frente:

```
i:xp=1310
s:deck=fire,ice,poison,electric,mine
b:tutorialDone=true
f:… (nenhum hoje; o formato aceita)
```

Valores são escapados (`\n` → `\\n`, `\\` → `\\\\`). `SaveData.import(blob)` apaga todas as chaves sincronizáveis e aplica as do blob; chaves locais ficam intactas. Blob inválido (linha sem `tipo:chave=`) é rejeitado inteiro sem tocar no save.

**Fica fora do blob (local do aparelho):** `soundVolume`, `musicVolume`, `sound`, `music`, `vibration`, `reduceMotion`, `largeText`, `language`, `match` (partida em andamento) e todas as chaves `sync.*`. Tudo o mais entra: `xp`, `deck`, `deckslot.*`, `cards.*`, `dielevel.*`, `best.*`, `bestWave`, `deckbest.*`, `matchBest`, `score.best`, `total.*`, `history`, `missions.*`, `daily.*`, `weekly.*`, `ach.*`, `cosmetic.*`, `hints`, `map`, `mode`, `mutators`, `tutorialDone`, `cards.retro`.

O blob carrega ainda duas linhas de controle, geradas na hora de subir: `i:sync.version=N` e `i:sync.at=<dia>` (`DailyChallenge.today()` do aparelho que subiu). Ao importar, `sync.version` vira o `sync.version` local e `sync.at` não é gravado.

`CloudBlob.hash(blob)` é o hash das linhas sem `sync.*`. `CloudBlob.summary(blob)` devolve `(nível, partidas, dia)` lendo `xp`, `total.games` e `sync.at`, para a tela de conflito.

Tamanho: poucos KB; o limite do snapshot é 3 MB.

## Regra de sincronização (`CloudSync`)

Chaves locais `sync.version` (versão da nuvem vista por último; 0 quando nunca sincronizou) e `sync.hash` (hash do export na última sincronização). **Local mudou** = `hash(export()) != sync.hash`.

```kotlin
object CloudSync {
    sealed interface Action {
        data object Nothing : Action
        data class Upload(val version: Int) : Action      // sobe o local como esta versão
        data object Adopt : Action                        // aplica a nuvem em silêncio
        data object Ask : Action                          // tela de conflito
    }
    fun decide(localVersion: Int, localChanged: Boolean, cloudVersion: Int?): Action
}
```

| nuvem | local mudou | ação |
|---|---|---|
| não existe (`null`) | qualquer | `Upload(1)` |
| `== localVersion` | não | `Nothing` |
| `== localVersion` | sim | `Upload(localVersion + 1)` |
| `> localVersion` | não | `Adopt` |
| `> localVersion` | sim | `Ask` |
| `< localVersion` | qualquer | `Upload(localVersion + 1)` (a nuvem foi apagada ou voltou atrás; o local é a referência) |

Depois de `Upload(v)`: `sync.version = v`, `sync.hash = hash atual`. Depois de `Adopt` ou de escolher a nuvem na tela: `import(blob)`, `sync.version = versão da nuvem`, `sync.hash = hash(blob)`. Escolher o local na tela: `Upload(cloudVersion + 1)`.

## Quando (`CloudController`)

- **Ao abrir** (`MergeTdGame.create`, depois de `retroCards`): se `cloud.available` e `sync.enabled` (padrão `true`), `isSignedIn` → se sim, `download` → `decide` → executa. Enquanto isso o jogo abre normalmente no Início; o resultado chega depois.
- **Adopt** só é aplicado se a tela atual não é a partida (`MatchScreen`) nem o fim de jogo; caso contrário fica pendente e aplica quando o jogo volta ao Início. Ao aplicar, as telas são recriadas (`showTab(HOME)`) e o Início mostra o aviso "Progresso da nuvem aplicado" por 3 s. A partida salva em andamento (`match`) é descartada ao adotar, porque pertence ao save antigo.
- **Ask** abre `CloudConflictScreen` por cima do Início (também só fora da partida).
- **Subir:** ao fim de cada partida (depois de `book()` no `MatchScreen`), ao subir nível de dado (`DeckScreen.levelUp`) e ao salvar o deck (`DeckScreen`, slot ou deck atual): `CloudController.push()`. Se logado, sobe `Upload(sync.version + 1)`; se falhou por rede, nada muda (o hash já diverge, então a próxima abertura sobe). Subidas são enfileiradas: nunca duas ao mesmo tempo; se chegar outra durante uma, roda uma só depois.
- **Entrar** (botão em Config): `signIn` → se ok, mesmo fluxo de abertura. **Desligar/Ligar:** grava `sync.enabled`; desligado, nada é baixado nem subido; ligar roda o fluxo de abertura.
- **Resumo visível** (`CloudController.status`): `UNAVAILABLE`, `OFF`, `SIGNED_OUT`, `WORKING`, `SYNCED(at: dia)`, `OFFLINE`, `ERROR(reason)`. Guarda `sync.lastOk` (dia) para "sincronizado hoje / há N dias".

## Telas

**Config** ganha, abaixo dos idiomas e acima da linha de privacidade, a linha **Nuvem** (`smallFont`, cor de chave à esquerda, estado em branco) e um botão à direita:

| estado | texto | botão |
|---|---|---|
| `UNAVAILABLE` | linha oculta | — |
| `OFF` | Nuvem: desligada | Ligar |
| `SIGNED_OUT` | Nuvem: sem conta | Entrar |
| `WORKING` | Nuvem: sincronizando… | — |
| `SYNCED` | Nuvem: sincronizada hoje / há N dias | Desligar |
| `OFFLINE` | Nuvem: sem conexão | Desligar |
| `ERROR` | Nuvem: falhou (toque para tentar) | Tentar |

A linha de privacidade passa a dizer que, com a nuvem ligada, o progresso é guardado na conta Google do jogador via Google Play Games, e nada mais; [PRIVACY.md](../../../PRIVACY.md) ganha o mesmo parágrafo.

**`CloudConflictScreen`** (sem barra inferior, `tab = null`): título "Qual progresso manter?", dois cartões empilhados, cada um com título, duas linhas (`nível %d · %d partidas`, `há %d dias` ou `hoje`) e um botão: "Este aparelho" / "Nuvem". Não tem Voltar nem fecha com o botão físico: a escolha é obrigatória, mas o jogo não trava enquanto o jogador decide (é só uma tela). Escolher aplica a regra acima e volta ao Início.

Textos novos em `Strings.kt` com tradução EN.

## Android (`PlayGamesCloud`)

- Dependência `com.google.android.gms:play-services-games-v2:21.0.0` no módulo `android`. Nada no `core`.
- `AndroidManifest.xml`: `<meta-data android:name="com.google.android.gms.games.APP_ID" android:value="@string/game_services_project_id"/>`; o valor em `android/res/values/games.xml` (não é segredo; entra no git com um placeholder `0` até o console existir).
- `AndroidLauncher.onCreate`: `PlayGamesSdk.initialize(this)` antes de criar o jogo; passa `PlayGamesCloud(this)`.
- `signIn` → `PlayGames.getGamesSignInClient(activity).signIn()`; `isSignedIn` → `isAuthenticated()`.
- `download` → `getSnapshotsClient(activity).open("save", true, RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)`, lê `snapshotContents.readFully()` como UTF‑8 e fecha com `discardAndClose`. Snapshot recém-criado e vazio conta como `null`.
- `upload` → `open` igual, `writeBytes`, `commitAndClose` com `SnapshotMetadataChange` cuja descrição é `"nível %d · %d partidas"`. O SDK limita uma gravação a cada poucos segundos; o `CloudController` já serializa as subidas.
- Falhas: sem rede (`ApiException` com `NETWORK_ERROR`) → `Failed("offline")`; não autenticado → `Failed("signed-out")`; o resto → `Failed("error:<código>")`. Tudo entregue com `Gdx.app.postRunnable`.
- `proguard-rules.pro`: as regras do Play Services vêm com a biblioteca; nada a acrescentar salvo aviso do R8 no build.

### Fora do código: Play Console (você)

1. Conta de desenvolvedor no Google Play Console (taxa única).
2. Criar o app com o pacote `com.iagothz.mergetd`.
3. **Play Games Services → Configuração e gerenciamento → Configuração**: criar um projeto de jogo ligado a um projeto do Google Cloud (o console cria um).
4. **Credenciais**: adicionar credencial do tipo Android com o pacote e um cliente OAuth para cada SHA‑1: a chave de debug local (`keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android`), a chave de release do `android/keystore/merge-td.jks` e, se usar o Play App Signing, a chave de assinatura do app mostrada no console.
5. Ativar **Saved Games** nas propriedades do jogo (não dá para ligar depois de publicar o projeto de jogo).
6. **Testadores**: adicionar as contas Google que vão testar antes de publicar o projeto de jogo.
7. Copiar o **ID do projeto** (número) para `android/res/values/games.xml`.

Até isso existir, `isSignedIn` devolve `false`, Config mostra "Nuvem: sem conta · Entrar", e o jogo segue offline.

## Tratamento de erro

- Qualquer falha da nuvem é silenciosa fora de Config: o estado aparece lá, nunca interrompe o jogo.
- Blob corrompido na nuvem: `import` rejeita, o estado vira `ERROR("blob")`, e o próximo `push()` sobrescreve a nuvem com o local (tratado como `cloudVersion < localVersion`).
- Dois aparelhos subindo quase ao mesmo tempo: a política `MOST_RECENTLY_MODIFIED` resolve o conflito do Google; o próximo que abrir vê a versão maior e aplica a regra. Com `sync.version` igual nos dois, mas conteúdo diferente, o hash diverge e vira `Upload`; o outro aparelho ao abrir vê versão maior e adota ou pergunta.

## Testes

- `CloudSyncTest`: uma linha por célula da tabela de decisão.
- `SaveDataTest`: export → import ida e volta reproduz todas as chaves sincronizáveis; chaves locais não entram no export e sobrevivem ao import; blob inválido não altera nada; escape de `\n` em valores (`history` tem vírgulas e pode ter quebras? não tem, mas o teste garante).
- `CloudBlobTest`: `hash` ignora `sync.*`; `summary` lê nível, partidas e dia; blob sem `total.games` dá 0 partidas.
- `CloudControllerTest` com um `CloudSave` falso em memória (lista de blobs por versão, flags de offline/signed-out): abrir sem nuvem sobe; abrir com nuvem à frente adota; adotar durante partida fica pendente e aplica ao voltar; subir enquanto offline deixa o hash divergente e a próxima abertura sobe; duas subidas seguidas viram uma; `sync.enabled=false` não faz nada.
- Android: só no aparelho, depois do console. No emulador sem projeto: build de release instala, Config mostra "sem conta", Entrar falha de forma limpa, partida e fim de jogo normais.

## Documentação

README (seção de progressão: "Nuvem"), `store/README.md` (lista "Novidades"), `PRIVACY.md` e esta spec. Versão sobe para 0.7.0 / versionCode 7.
