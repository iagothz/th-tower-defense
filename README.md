# Merge TD

[![CI](https://github.com/iagothz/th-tower-defense/actions/workflows/ci.yml/badge.svg)](https://github.com/iagothz/th-tower-defense/actions/workflows/ci.yml)

Tower defense de merge no estilo Random Dice, para jogar offline no Android. Feito em Kotlin + libGDX.

## Como jogar

- Compre dados com SP. Cada dado comprado é um tipo aleatório do seu deck, com 1 pip.
- Arraste um dado sobre outro do **mesmo tipo e mesmo número de pips** para gerar um dado com +1 pip de tipo aleatório. O Espelho combina com qualquer tipo (resultado sorteado), o Reforço também, mas garante o tipo do outro dado, e o Clonador vira uma cópia do dado em que for solto.
- Gaste SP nos botões de baixo para aumentar o dano de um tipo (níveis 1 a 5).
- Errou um merge? **Desfazer** aparece por 3 s ao lado de Comprar.
- **Segure** um dado no tabuleiro para ver o que ele faz e quanto está batendo agora. O botão **2x** acelera a partida; o de pausa tem Continuar, Reiniciar e Sair (os dois últimos pedem confirmação).
- Entre as ondas, o topo mostra o que vem na próxima (quantos de cada inimigo) e em quantas ondas chega o chefe.
- As ondas são infinitas, com um chefe a cada 10 ondas (7 chefes, em ordem sorteada por partida) e uma onda especial a cada 5 (Horda, Tanques, Corrida). Na primeira partida um tutorial de 3 passos mostra o básico. Você tem 3 vidas; o recorde é a maior onda alcançada.
- Se você trocar de app no meio da partida, o jogo pausa e salva. Mesmo que o Android feche o jogo, ao abrir de novo a partida volta de onde parou.
- **Histórico** das últimas 20 partidas e **código de partida** no fim de jogo: mande o código para alguém jogar exatamente a mesma partida (botão Jogar código na tela Partida).
- **Conquistas**: 20 medalhas; 13 destravam molduras, céus e formas de pip (só visual), equipados na tela Conquistas.
- **Desafio do dia**: o dia sorteia mapa, 5 dados e a seed; o recorde é por dia e os últimos ficam em **Estatísticas**, junto com totais, recorde por mapa e melhores decks.
- **Sinergias**: oito pares de dados (Fogo+Vento, Gelo+Terremoto, Veneno+Maldição, Luz+Crítico, Elétrico+Tempo, Ferro+Sniper, Caçador+Vampiro, Gravidade+Armadilha) ligam um bônus enquanto os dois estão no tabuleiro. Todo dado ganha uma habilidade própria em **7 pips**.
- **Modos** na tela Partida: Normal, Boss Rush, Sobrevivência e Rápido, com recorde por mapa e modo.
- Na tela **Partida**, escolha entre 4 caminhos (Ferradura, Serpente, Anel e Reta), com dificuldade e recorde próprios.
- Na tela **Deck**, escolha 5 dos 25 tipos de dado. Tocar num dado mostra o que ele faz: alvo, dano, velocidade, efeito e power-up.

## Requisitos

- JDK 17 ou 21 (o caminho fica em `org.gradle.java.home`, no `gradle.properties`).
- Android SDK, com o caminho em `local.properties` (`sdk.dir=...`). Esse arquivo não é versionado.

## Rodar no PC

```bash
./gradlew :lwjgl3:run
```

## Testes

```bash
./gradlew :core:test :sim:test
```

O GitHub Actions (`.github/workflows/ci.yml`) roda os testes e gera o APK de debug a cada push em `main`; o APK fica nos artefatos da execução.

## APK

```bash
./gradlew :android:assembleDebug
```

Para o APK de release assinado, crie uma chave uma única vez e aponte para ela em `android/keystore.properties` (os dois ficam fora do git):

```bash
keytool -genkeypair -keystore android/keystore/merge-td.jks -alias mergetd -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=keystore/merge-td.jks
storePassword=...
keyPassword=...
keyAlias=mergetd
```

```bash
./gradlew :android:assembleRelease
```

Guarde a chave e as senhas: atualizar um app instalado exige a mesma assinatura. O ícone é gerado por `python tools/make_icons.py` (precisa do Pillow).

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
