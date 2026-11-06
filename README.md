# Merge TD

[![CI](https://github.com/iagothz/th-tower-defense/actions/workflows/ci.yml/badge.svg)](https://github.com/iagothz/th-tower-defense/actions/workflows/ci.yml)

Tower defense de merge no estilo Random Dice, para jogar offline no Android. Feito em Kotlin + libGDX.

## Como jogar

- Compre dados com SP. Cada dado comprado é um tipo aleatório do seu deck, com 1 pip.
- Arraste um dado sobre outro do **mesmo tipo e mesmo número de pips** para gerar um dado com +1 pip de tipo aleatório. O Espelho combina com qualquer tipo (resultado sorteado), o Reforço também, mas garante o tipo do outro dado, e o Clonador vira uma cópia do dado em que for solto. Entre dois coringas vale o efeito do dado que você arrasta: Reforço levado ao Clonador sobe o Clonador; Clonador levado ao Reforço clona o Reforço.
- Gaste SP nos botões de baixo para aumentar o dano de um tipo (níveis 1 a 5).
- Para comprar um dado, toque numa célula vazia: cada uma mostra o preço (dourado quando dá, cinza quando falta SP) e o dado nasce ali. Soltar o dado perto de um alvo compatível já vale: ele é puxado para a célula.
- **Segure** um dado no tabuleiro para ver o que ele faz e quanto está batendo agora. O botão **2x** acelera a partida; o de pausa tem Continuar, Reiniciar e Sair (os dois últimos pedem confirmação) e mostra as missões do dia com o que esta partida já fez.
- Entre as ondas, o topo mostra o que vem na próxima (quantos de cada inimigo) e em quantas ondas chega o chefe.
- As ondas são infinitas, com um chefe a cada 10 ondas (7 chefes, em ordem sorteada por partida) e uma onda especial a cada 5 (Horda, Tanques, Corrida). Na primeira partida um tutorial de 3 passos mostra o básico. Você tem 3 vidas; o recorde é a maior onda alcançada.
- Se você trocar de app no meio da partida, o jogo pausa e salva. Mesmo que o Android feche o jogo, ao abrir de novo a partida volta de onde parou.
- **Histórico** das últimas 20 partidas e **código de partida** no fim de jogo: mande o código para alguém jogar exatamente a mesma partida (botão Jogar código na tela Partida).
- **Nível e missões**: cada partida dá XP pelas ondas, e três missões por dia (as mesmas para todo mundo) pagam XP extra; o nível, o título e as missões aparecem no **Início**.
- **Barra inferior** com cinco abas: Início, Deck, Mapas, Desafios e Config (Estatísticas, Conquistas e Histórico ficam dentro de Config).
- **Dados por nível**: você começa com 8 dados e cada nível de conta abre mais um (até o 18). Cada partida abre um baú de **cartas**; com 5/10/20/40 cartas de um dado ele sobe de nível (até 5), ganhando +10% permanentes por nível. Tudo na tela Deck.
- **Nuvem** (Android): com uma conta do Google Play Games, o progresso (nível, dados, cartas, recordes, histórico) sobe ao fim de cada partida e desce ao abrir o jogo, então dá para jogar em dois aparelhos. Se os dois avançaram separados, o jogo pergunta qual progresso manter. Em Config dá para entrar, ligar e desligar; sem conta, tudo fica no aparelho como antes.
- **Conquistas**: 19 medalhas; 13 destravam molduras, céus e formas de pip (só visual), equipados na tela Conquistas.
- **Elite** (grande, cura a escolta, vale 30 SP) a cada três ondas e **Tesouro** (rápido, não tira vida, vale 60 SP) a cada quatro, a partir da onda 6.
- **Mutadores** na tela Partida (Velozes, Resistentes, Pobres, Enxame): cada um multiplica a pontuação (onda × mutadores). O **Desafio semanal** fixa mapa, deck e dois mutadores para a semana.
- **Desafio do dia**: o dia sorteia mapa, 5 dados e a seed; o recorde é por dia e os últimos ficam em **Estatísticas**, junto com totais, recorde por mapa e melhores decks.
- **Sinergias**: oito pares de dados (Fogo+Vento, Gelo+Terremoto, Veneno+Maldição, Luz+Crítico, Elétrico+Tempo, Ferro+Sniper, Caçador+Vampiro, Gravidade+Armadilha) ligam um bônus enquanto os dois estão no tabuleiro. Todo dado ganha uma habilidade própria em **7 pips**.
- **Modos** na tela Partida: Normal, Boss Rush, Sobrevivência e Rápido, com recorde por mapa e modo.
- Na tela **Partida**, escolha entre 4 caminhos (Ferradura, Serpente, Anel e Reta), com dificuldade e recorde próprios.
- Todas as telas têm o mesmo cabeçalho; as subtelas (Estatísticas, Conquistas, Histórico, Inimigos) voltam pela seta no canto de cima, e o botão voltar do Android navega pelo jogo (fecha janelas, pausa, sobe um nível) e só fecha o jogo no Início. Em Config, Vibração, Reduzir movimento e Texto grande são interruptores.
- Na tela **Deck**, o botão **Inimigos** abre a enciclopédia: o que cada inimigo e chefe faz e como vencer. Tocar num dado abre a ficha completa dele numa janela (dano, velocidade, alvo, efeito, 7 pips, sinergia e power-up), onde também dá para subir o nível; uma seta verde marca os dados que já podem subir. Na partida, dicas de uma linha aparecem na primeira vez que algo novo acontece (coringa, sinergia, 7 pips).
- Na tela **Deck**, três slots guardam decks (toque carrega, segurar salva) e o **?** sorteia um deck. No fim de jogo, **Compartilhar** manda onda, mapa e código para qualquer app.
- Na tela **Deck**, escolha 5 dos 25 tipos de dado arrastando: do grid para um espaço do deck entra (no lugar do que estava), de um espaço para fora sai, espaço sobre espaço troca de lugar. Tocar num dado só mostra o que ele faz: alvo, dano, velocidade, efeito e power-up.
- **Configurações**: volume do som e da música, vibração, idioma (automático, português ou inglês), **Reduzir movimento** (sem tremor, vinheta nem câmera lenta) e **Texto grande**. A música ganha bateria enquanto um chefe está vivo e acelera no modo Rápido. O jogo funciona sem internet e não coleta nada ([PRIVACY.md](PRIVACY.md)).

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

Para a loja, o pacote AAB sai de `./gradlew :android:bundleRelease` (em `android/build/outputs/bundle/release/`). O release passa pelo R8 (código reduzido e ofuscado); as regras que mantêm o que o libGDX usa por reflexão ficam em `android/proguard-rules.pro`, e o `mapping.txt` de cada build em `android/build/outputs/mapping/release/`.

O material da ficha da loja (textos em pt/en, capturas 1080×2160 e a arte de destaque) fica em `store/`, com um `README.md` explicando cada item.

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
- **H:** perde 1 vida (testa o aviso de vazamento).
- **K:** mata todos os inimigos.
- **F:** velocidade 1×/2×/4×.

No modo de teste, o recorde não é salvo.

## Simulador de balanceamento

```bash
./gradlew :sim:run --args="--games 300 --bot all"
```

Joga partidas sem tela com bots (`--bot simple|smart|all`) e mostra em que onda as partidas terminam. Com `--decks random`, mostra também a força de cada tipo de dado; `--levels 5` põe todos os dados no nível 5. O CSV com os resultados vai para `sim/build/sim-results.csv`.

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

Para a nuvem funcionar, o jogo precisa de um projeto do Play Games Services no Play Console (credencial Android com o pacote e o SHA‑1 de cada chave, Saved Games ativado); o ID do projeto vai em `android/res/values/games.xml`. Sem isso, Config mostra "Nuvem: sem conta" e o jogo segue offline.

## Estrutura

- `core/.../logic`: regras puras (sem libGDX), cobertas por testes JUnit. Os números de balanceamento ficam em `Balance.kt`.
- `core/.../ui`: telas, renderização e input.
- `assets/`: arquivos usados pelo PC e pelo Android.
  - `fonts/`: Roboto Medium, licença Apache 2.0 (ver `LICENSE-Roboto.txt`).
  - `sounds/`: efeitos sintetizados. Para regenerar, rode `python tools/gen_sounds.py`.
- `lwjgl3/`: launcher desktop.
- `sim/`: simulador de balanceamento (bots e relatório).
- `android/`: launcher Android.
