# Changelog

Todas as mudanças relevantes do Merge TD. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o projeto usa [Versionamento Semântico](https://semver.org/lang/pt-BR/).

A versão fica em `gradle.properties` (`appVersion` e `appVersionCode`); o APK e a tela inicial leem de lá.

## [Unreleased]

### Adicionado
- Modo sandbox na aba Desafios: qualquer dado com qualquer pip, ondas sob demanda, nada salvo nem contado.
- Gatinho de fogo desenhado para todos os níveis (pips 1 a 7), com efeitos de fogo leves e visão 3/4 do campo.
- Compra de dado tocando na casa vazia escolhida, com o preço em cada casa.
- Menu de pausa com as missões do dia e o progresso da partida.
- Ficha completa do dado numa janela ao tocar nele no deck; grade do deck com quatro dados por linha.
- Padrão visual: paleta única, estilos de botão, cabeçalho com voltar, chaves em Config.
- Voltar do Android navega entre telas e só sai do jogo no Início, inclusive pelo gesto do Android 16.
- Logging estruturado (Logcat no Android; console e arquivo com rotação no PC).
- ktlint e detekt no build e no CI.
- Decisões de arquitetura registradas em `docs/adr/`.

### Alterado
- Cartões de missão com ícone, barra com contagem e selo de XP.
- O Play Games só inicializa com a nuvem ligada.
- Montagem e fechamento da partida saíram das telas para a camada de aplicação, com testes.
- Comentários que só repetiam o código foram removidos.

### Removido
- Desfazer merge, junto com a conquista correspondente.

### Corrigido
- A borda vermelha de última vida aparecia como blocos sólidos sobre a partida.
- A sincronização da nuvem nunca sobrescreve o save da conta sem lê-lo antes.
- Falhas da nuvem e de partida salva corrompida agora são registradas em vez de ignoradas.
- Selo de recorde no Início dimensionado pelo texto; barra de abas na borda da tela.

## [0.7.0] - 2026-10-30

### Adicionado
- Save na nuvem pelo Play Games (Saved Games), com exportação do save em texto, tabela de decisão de sincronização,
  status em Config, aviso de adoção e tela de conflito.

## [0.6.0] - 2026-10-29

### Adicionado
- Dados liberados pelo nível da conta, níveis de dado pagos com cartas e baú no fim da partida.
- Barra de navegação inferior com Início, Deck, Mapas, Desafios e Config.

### Alterado
- O deck muda só arrastando; tocar apenas mostra o dado.

## [0.5.0] - 2026-10-27

### Adicionado
- Tela de configurações, textos em português e inglês, release com R8 e AAB.
- Sensação de jogo no merge e no combate (hit-stop, squash, snap, recuo, câmera lenta).
- Tensão de onda e HUD vivo; momentos-chave e microdetalhes; música adaptativa em camadas e vibração.
- Nível de conta com XP e três missões diárias; enciclopédia de inimigos e dicas contextuais.
- Acessibilidade (reduzir movimento, texto grande); presets de deck, deck aleatório e compartilhar.
- Inimigos Elite e Tesouro, mutadores com pontuação e desafio semanal.
- Página da loja, capturas e arte de destaque.

### Corrigido
- Dois Reforços fundem num Reforço; o coringa arrastado decide o merge.

## [0.4.0] - 2026-10-27

### Adicionado
- Tela de estatísticas, desafio diário, animações de merge e compra, música e sons por família de ataque.
- Clima por mapa, cartão de entrada do chefe, animação de morte e sombras.
- Sinergias, uma habilidade de 7 pips para cada dado e modos de jogo (Boss Rush, Sobrevivência, Rápido).
- Vinte conquistas com cosméticos; histórico de partidas e códigos de partida compartilháveis.

## [0.3.0] - 2026-10-27

### Adicionado
- Qualidade de vida na partida: 2x, segurar para inspecionar, prévia da onda, confirmações, ícone, splash,
  vibração, assinatura de release e CI.
- Quatro chefes novos, inimigos Curandeiro e Fantasma, ondas especiais e ordem de chefes embaralhada.
- Tutorial da primeira partida e dica no fim da partida sobre quem a encerrou.

## [0.2.0] - 2026-10-27

### Adicionado
- Fundo estrelado, moldura do tabuleiro, telas de menu e fim de partida, slots de deck e transições.
- Dados Clonador e Reforço; Espelho que rola; Crescimento que sorteia do deck; limite do Caçador por pip.
- Quatro mapas com temas, tela de mapas e recordes por mapa.

## [0.1.0] - 2026-10-26

### Adicionado
- Projeto libGDX (core, desktop, Android) com regras puras testadas: dados, merge, deck, caminho, inimigos,
  efeitos, combate, economia, ondas e chefes.
- Telas de menu, deck, partida e fim de partida; arrastar para fundir; retomar a partida ao voltar ao app.
- Fonte com acentos, números de dano, partículas, tremor, sons retrô e 13 dados novos.
- Modo dev no PC e simulador de balanceamento.

[Unreleased]: https://github.com/iagothz/th-tower-defense/compare/23ac2da...HEAD
[0.7.0]: https://github.com/iagothz/th-tower-defense/commit/23ac2da
[0.6.0]: https://github.com/iagothz/th-tower-defense/commit/0fe0e3d
[0.5.0]: https://github.com/iagothz/th-tower-defense/commit/69a9950
[0.4.0]: https://github.com/iagothz/th-tower-defense/commit/40db4f5
[0.3.0]: https://github.com/iagothz/th-tower-defense/commit/723a451
[0.2.0]: https://github.com/iagothz/th-tower-defense/commit/78a52c2
[0.1.0]: https://github.com/iagothz/th-tower-defense/commit/392cd4a
