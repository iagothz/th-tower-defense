# ADR-0002: camadas `logic`, `app` e `ui`

Status: Aceito (2026-11-11)

## Contexto

As regras de jogo já viviam em `logic`, sem libGDX. As regras de aplicação ficavam nas telas: fechar a partida
(recordes, histórico, XP, missões, baú), salvar ao ir para o fundo e montar cada tipo de partida. Elas não tinham
testes. O padrão do time pede camadas com responsabilidades separadas.

## Decisão

Três camadas em `core`, com dependência só para baixo:

- **`logic` (domínio):** regras puras da partida (`Match`, `Board`, `Combat`, `WaveSpawner`, `Balance`...). Não
  conhece save nem tela.
- **`app` (aplicação):** casos de uso que combinam domínio e save. `MatchLauncher` monta partidas normal, diária,
  semanal, por código e retomada (`MatchSession`); `MatchBooking` suspende e fecha a partida e devolve um
  `MatchOutcome`; `MatchStats` e `MissionTracker` acompanham a partida.
- **`ui` (apresentação):** telas, desenho e input. Chama `app`, não grava resultados no save direto.

`log` é transversal. O plano e os critérios estão em [fase4-camada-aplicacao.md](../fase4-camada-aplicacao.md).

## Consequências

- O fechamento da partida e a montagem das partidas têm testes unitários com save em memória.
- `MatchScreen` e `MergeTdGame` só trocam de tela e desenham.
- `SaveData` (persistência) ainda está em `ui` e é usado por `app`: é a dívida que sobra. O próximo passo
  possível é movê-lo para uma camada de dados atrás de uma interface.
