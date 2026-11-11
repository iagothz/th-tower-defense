# ADR-0001: libGDX com Kotlin, multi-módulo

Status: Aceito (2026-10-26)

## Contexto

O jogo é um tower defense 2D para Android, offline, mantido por uma pessoa. Precisa rodar também no PC para
desenvolver e testar rápido, e as regras de jogo (merge, combate, ondas) precisam de testes unitários
determinísticos. O balanceamento é ajustado com um simulador que joga milhares de partidas sem tela.

## Decisão

- libGDX 1.13 com Kotlin, no formato do gdx-liftoff: `core` (jogo), `android` e `lwjgl3` (PC) como launchers.
- Um módulo `sim` reusa as regras de `core` para simular partidas com bots.
- As regras ficam em Kotlin puro, sem libGDX, para rodar em JUnit e no simulador sem contexto gráfico.
- Passo fixo de 1/60 s e `Random` injetado, para partidas reproduzíveis por seed (base dos códigos de partida e
  dos desafios diário e semanal).

## Alternativas consideradas

- **Motores com editor (Unity, Godot):** mais ferramentas visuais, mas o fluxo de teste unitário e de simulação
  sem tela é mais pesado, e o projeto não precisa de editor de cena.
- **Android nativo com Canvas/Compose:** sem build para PC e sem loop de jogo pronto.

## Consequências

- O mesmo código roda no PC (modo dev com teclas de teste) e no Android.
- As regras são testadas sem emulador; o simulador mede a força dos dados com os mesmos números do jogo.
- A UI é desenhada à mão (`ShapeRenderer`, `SpriteBatch`), sem kit de widgets: cada tela cuida do próprio layout.
- O release passa pelo R8 e precisa das regras de reflexão do libGDX em `android/proguard-rules.pro`.
