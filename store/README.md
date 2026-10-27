# Material da loja

Tudo o que a ficha do Google Play pede, pronto para colar.

- `feature.png`: arte de destaque 1024×500 (`python tools/make_feature.py` gera de novo).
- `screenshots/pt/` e `screenshots/en/`: sete capturas 1080×2160 por idioma, na ordem em que devem aparecer (menu, partida, deck, inimigos, missões, desafios, fim de jogo). Geradas no emulador Pixel 8 e cortadas pelo `tools/crop_screenshots.py`.
- Ícone: `android/res/mipmap-*` (adaptativo), gerado por `tools/make_icons.py`.
- Política de privacidade: `PRIVACY.md` na raiz (publique o texto numa página e cole o link).
- Pacote: `./gradlew :android:bundleRelease` → `android/build/outputs/bundle/release/android-release.aab`.

Categoria: Jogos › Estratégia. Classificação: sem violência realista, sem compras, sem anúncios, sem coleta de dados (questionário IARC: "Todos").

---

## Português (pt-BR)

**Título** (30): Merge TD: dados e ondas

**Descrição curta** (80): Junte dados, segure as ondas. Tower defense offline, sem anúncios.

**Descrição completa**:

Merge TD é um tower defense de juntar dados, para jogar sem internet.

Compre dados com SP, arraste dois iguais um sobre o outro e eles viram um dado mais forte de tipo sorteado do seu deck. Cada tipo ataca de um jeito: Fogo explode em área, Gelo congela, Elétrico salta entre inimigos, Sniper mira o mais resistente, Veneno acumula, e por aí vai. Chegue a 7 pips e o dado ganha uma habilidade só dele.

• 25 tipos de dado e 8 sinergias entre pares.
• 4 mapas com caminhos e climas diferentes.
• 4 modos: Normal, Boss Rush, Sobrevivência e Rápido.
• 7 chefes com truques próprios, ondas especiais, elites e tesouros.
• Mutadores para quem quer mais: inimigos mais rápidos, mais resistentes, mais numerosos.
• Desafio do dia e desafio da semana, iguais para todo mundo.
• Nível de conta com XP, três missões por dia e 20 conquistas que destravam molduras, céus e pips.
• Código de partida: mande para um amigo jogar exatamente a mesma partida.
• Histórico, estatísticas e enciclopédia de inimigos.
• Configurações de som, música, vibração, idioma, reduzir movimento e texto grande.

Sem anúncios, sem compras, sem conta. O jogo não coleta nem envia nenhum dado.

**Novidades** (500):

Nível com XP e missões diárias, enciclopédia de inimigos, dicas na partida, inimigos Elite e Tesouro, mutadores com pontuação, desafio semanal, decks salvos, compartilhar partida, reduzir movimento e texto grande.

---

## English (en-US)

**Title** (30): Merge TD: dice and waves

**Short description** (80): Merge dice, hold the waves. Offline tower defense, no ads.

**Full description**:

Merge TD is a dice-merging tower defense you can play offline.

Buy dice with SP, drag two matching dice together and they become a stronger die of a random type from your deck. Every type attacks its own way: Fire splashes, Ice freezes, Electric chains between enemies, Sniper picks the toughest one, Poison stacks, and so on. Reach 7 pips and the die gets an ability of its own.

• 25 dice types and 8 synergies between pairs.
• 4 maps with their own paths and weather.
• 4 modes: Normal, Boss Rush, Survival and Rush.
• 7 bosses with their own tricks, special waves, elites and treasures.
• Mutators for more: faster, tougher, more numerous enemies.
• Daily and weekly challenges, the same for everyone.
• Account level with XP, three missions a day and 20 achievements that unlock frames, skies and pip shapes.
• Match codes: send one to a friend to play the exact same match.
• History, stats and an enemy encyclopedia.
• Settings for sound, music, vibration, language, reduce motion and large text.

No ads, no purchases, no account. The game collects and sends no data.

**What's new** (500):

Account level with XP and daily missions, enemy encyclopedia, in-match hints, Elite and Treasure enemies, mutators with scores, weekly challenge, saved decks, share your match, reduce motion and large text.
