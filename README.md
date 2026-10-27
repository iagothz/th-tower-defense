# Merge TD

Tower defense de merge no estilo Random Dice, para jogar offline no Android. Feito em Kotlin + libGDX.

## Como jogar

- Compre dados com SP. Cada dado comprado é um tipo aleatório do seu deck, com 1 pip.
- Arraste um dado sobre outro do **mesmo tipo e mesmo número de pips** para gerar um dado com +1 pip de tipo aleatório. O Espelho combina com qualquer tipo.
- Gaste SP nos botões de baixo para aumentar o dano de um tipo (níveis 1 a 5).
- As ondas são infinitas, com um chefe a cada 10 ondas. Você tem 3 vidas; o recorde é a maior onda alcançada.
- Se você trocar de app no meio da partida, o jogo pausa e salva. Mesmo que o Android feche o jogo, ao abrir de novo a partida volta de onde parou.
- Na tela **Deck**, escolha 5 dos 10 tipos de dado. Tocar num dado mostra o que ele faz: alvo, dano, velocidade, efeito e power-up.

## Requisitos

- JDK 17 ou 21 (o caminho fica em `org.gradle.java.home`, no `gradle.properties`).
- Android SDK, com o caminho em `local.properties` (`sdk.dir=...`). Esse arquivo não é versionado.

## Rodar no PC

```bash
./gradlew :lwjgl3:run
```

## Testes

```bash
./gradlew :core:test
```

## Modo de teste (só no PC)

```bash
./gradlew :lwjgl3:run --args=--dev
```

Teclas na partida:

- **N:** próxima onda.
- **B:** próximo chefe.
- **S:** +500 SP.
- **L:** +1 vida.
- **K:** mata todos os inimigos.
- **F:** velocidade 1×/2×/4×.

No modo de teste, o recorde não é salvo.

## Simulador de balanceamento

```bash
./gradlew :sim:run --args="--games 300 --bot all"
```

Joga partidas sem tela com bots (`--bot simple|smart|all`) e mostra em que onda as partidas terminam. Com `--decks random`, mostra também a força de cada tipo de dado. O CSV com os resultados vai para `sim/build/sim-results.csv`.

## Gerar e instalar o APK

```bash
./gradlew :android:assembleDebug
```

O APK fica em `android/build/outputs/apk/debug/android-debug.apk`. Para instalar, há dois caminhos:

- Copiar o arquivo para o celular e abrir. Será preciso permitir a instalação de apps de fontes desconhecidas.
- Com o celular em modo desenvolvedor e conectado por USB:

```bash
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

## Estrutura

- `core/.../logic`: regras puras (sem libGDX), cobertas por testes JUnit. Os números de balanceamento ficam em `Balance.kt`.
- `core/.../ui`: telas, renderização e input.
- `assets/`: arquivos usados pelo PC e pelo Android.
  - `fonts/`: Roboto Medium, licença Apache 2.0 (ver `LICENSE-Roboto.txt`).
  - `sounds/`: efeitos sintetizados. Para regenerar, rode `python tools/gen_sounds.py`.
- `lwjgl3/`: launcher desktop.
- `sim/`: simulador de balanceamento (bots e relatório).
- `android/`: launcher Android.
