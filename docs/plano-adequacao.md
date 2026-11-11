# Plano de adequação ao padrão do projeto

Data: 2026-11-11. Base: checklist das instruções globais (`~/.claude/CLAUDE.md`).
Cada fase vira um commit ou PR próprio. Commits só quando solicitados.

## Situação atual

### Já atende

- `.gitignore` adequado à stack.
- README com objetivo, requisitos, execução, testes, geração de APK e estrutura.
- CI no GitHub Actions (`.github/workflows/ci.yml`): testes de `core` e `sim` e APK debug.
- Commits em Conventional Commits.
- 447 testes unitários.
- Separação entre domínio (`logic/`) e interface (`ui/`).
- Dependências com versão fixada: libGDX 1.13.1, Kotlin 2.1.20, AGP 8.10.1, play-services-games-v2 21.0.0.
- Segredos fora do git: `android/keystore/` e `android/keystore.properties` ignorados.

### Lacunas

| # | Lacuna | Onde |
|---|--------|------|
| 1 | Sem linter nem formatador | build |
| 2 | Sem logging | todo o projeto |
| 3 | Falhas silenciosas | `Match.restore` (devolve `null` sem registrar), `PlayGamesCloud.kt` (3× `runCatching` com `getOrNull`/`getOrDefault`), `AndroidLauncher.kt` (`catch (NullPointerException)`) |
| 4 | Sem CHANGELOG; versão duplicada | `MergeTdGame.VERSION` e `android/build.gradle.kts` |
| 5 | Sem exemplo versionado de configuração | `android/keystore.properties` |
| 6 | ~807 linhas de comentário em 82 arquivos | `core/src/main/kotlin` |
| 7 | Branch fora do padrão | `cl/...` em vez de `feature/`, `fix/`, `chore/` |
| 8 | Regras de aplicação dentro da UI | `MatchScreen.book()` (recordes, XP, missões, conquistas, cartas) |

### Não se aplica

SQL e queries parametrizadas, usuários de banco, plano de deploy e rollback de servidor. O jogo é offline; a nuvem é só o Play Games.

## Fases

### Fase 0: pendências (pequena)

- Commitar a correção da tela vermelha (blending religado após o passe de sprites).
- Novos trabalhos em branches `feature/`, `fix/` ou `chore/`.

### Fase 1: linter e formatador (pequena, alto retorno)

- Plugin `ktlint-gradle` e `detekt` com versões fixadas, mais `.editorconfig`.
- Um commit só de formatação, para isolar o diff.
- Baseline do detekt: congela os avisos atuais e barra apenas os novos.
- `ktlintCheck` e `detekt` no CI.
- Aceite: `./gradlew ktlintCheck detekt test` passa no PC e no CI.

### Fase 2: erros e logging (média)

- `Log` em `core` com níveis DEBUG/INFO/WARN/ERROR, timestamp, módulo e contexto, sobre `Gdx.app.log`/`Gdx.app.error` (vai ao Logcat no Android).
- No PC (`lwjgl3`), saída também em arquivo com rotação.
- Cada falha silenciosa da lacuna 3 passa a registrar com stack trace, mantendo o fallback atual (save corrompido não derruba o jogo).
- Nunca registrar o conteúdo do save nem dados da conta Play.
- Aceite: teste prova que um snapshot corrompido gera WARN e devolve `null`; nenhum `catch` ou `runCatching` sem log no código.

### Fase 3: versionamento e configuração (pequena)

- `CHANGELOG.md` no formato Keep a Changelog, reconstruído do histórico até a 0.7.0.
- Versão com fonte única em `gradle.properties`, gerando a constante do jogo no build.
- `android/keystore.properties.example` sem valores, explicado no README.
- Aceite: mudar a versão num lugar só altera o APK e o texto da tela inicial.

### Fase 4: camada de aplicação (média/grande, com plano próprio antes)

- Mover para serviços testáveis num pacote `app/`, entre `logic/` e `ui/`: o fechamento da partida (`book()`), a escolha da partida a iniciar e as regras do sandbox.
- Antes de mover, testes que cobrem o comportamento atual.
- Aceite: o fechamento da partida tem testes unitários; as telas só desenham e repassam o toque.

### Fase 5: comentários (grande, opcional)

- Código novo já segue a regra.
- No código existente, proposta: remover os comentários que repetem o óbvio e manter os KDoc que explicam regras de jogo. Alternativa: remover todos.
- Decisão em aberto: limpeza parcial ou total.

### Fase 6: documentação (pequena)

- ADRs curtos em `docs/adr/`: libGDX com Kotlin; save em texto; arte renderizada no Blender; chave de release fora do git.
- README atualizado com lint e CHANGELOG.

## Riscos

- A Fase 1 toca quase todos os arquivos; fazê-la antes de outras mudanças grandes para evitar conflitos.
- A Fase 4 mexe em recordes e save; só entra com testes cobrindo o comportamento atual.

## Decisões em aberto

- Fase 5: limpeza parcial (recomendada) ou total dos comentários.
