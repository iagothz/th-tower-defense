# Fase 4: camada de aplicação

Data: 2026-11-11. Branch: `refactor/app-layer`. Parte do [plano de adequação](plano-adequacao.md).

## Objetivo

As telas só desenham e repassam o toque. As regras de aplicação saem da UI para serviços testáveis no pacote
`com.iagothz.mergetd.app`, entre `logic/` (domínio) e `ui/` (telas). Nenhum comportamento do jogo muda.

## Regras de aplicação hoje na UI

| Regra | Onde está | Para onde vai |
|-------|-----------|---------------|
| Fechar a partida: limpar o save da partida; somar totais e recorde do modo, do diário ou do semanal; pontuação de mutadores; histórico com código; conquistas; XP; missões; dados liberados; baú de cartas | `MatchScreen.book()` | `app.MatchBooking.book()` |
| Gravar o recorde provisório e a partida ao ir para o fundo | `MatchScreen.pause()` | `app.MatchBooking.suspend()` |
| Decidir se é recorde (onda, ou pontuação no semanal) | `MatchScreen.finish()` | `app.MatchBooking`, devolvido no resultado |
| Montar a partida normal, diária, semanal, por código e a retomada, com o recorde anterior | `MergeTdGame.startMatch/startDaily/startWeekly/startFromCode/resumeSavedMatch` e o construtor de `MatchScreen` | `app.MatchLauncher` |
| Contar a partida e o progresso das missões | `ui.MatchStats`, `ui.MissionTracker` (já sem libGDX) | movidos para `app` |
| Sandbox: soltar um dado numa casa vazia move, sobre outro troca | `SandboxScreen.onTouchUp` | `Match.moveDie()` no domínio, só no sandbox |
| Fim de partida com 11 parâmetros soltos | `MergeTdGame.showGameOver(...)` | um `app.MatchOutcome` |

Fora do escopo desta fase: mover `SaveData` (persistência, hoje em `ui`) para uma camada de dados. Fica registrado
como próximo passo possível.

## Etapas

1. **Testes primeiro.** Testes de `MatchBooking` escritos a partir do comportamento atual de `book()`, `pause()` e
   `finish()`, usando o `SaveData` em memória que os testes já têm (`SaveDataTest.memoryPrefs()`):
   - partida normal: totais, recorde do mapa e modo, histórico com código, XP e nível;
   - diária: recorde do dia, sem recorde de mapa e sem código no histórico;
   - semanal: recorde pela pontuação;
   - mutadores: melhor pontuação fora do semanal;
   - modo dev: limpa a partida salva e não registra nada;
   - `suspend()`: recorde provisório e snapshot salvo; nada no dev e nada com a partida encerrada;
   - recorde: onda maior que a anterior, ou pontuação no semanal.
   Testes de `MatchLauncher` para cada tipo de partida e para a retomada com save corrompido.
   Teste de `Match.moveDie()`.
   Todos falham antes da extração.
2. **Extração.** Mover o código para `MatchBooking` e `MatchLauncher` sem mudar a lógica; `MatchStats` e
   `MissionTracker` mudam de pacote; `MatchScreen`, `SandboxScreen` e `MergeTdGame` passam a chamar os serviços.
3. **Conferência.** `ktlintCheck detekt test assembleDebug`; no emulador, uma partida normal até o fim (recorde, XP,
   baú) e a tela de fim igual à de antes.

## Critérios de aceite

- `MatchScreen` não chama mais `game.save` para registrar resultados.
- `MergeTdGame` não monta partidas; só troca de tela.
- Os testes novos cobrem cada caminho da tabela acima e passam.
- Lint, testes e build passam; a partida no emulador termina com a mesma tela de antes.

## Riscos

- Recordes e save: a ordem das gravações muda de lugar, não de sequência. Os testes da etapa 1 travam o resultado.
- As chaves do save não mudam, então não há migração.
- Diff grande em `MatchScreen`: a extração vai num commit só de movimentação, separado dos testes.
