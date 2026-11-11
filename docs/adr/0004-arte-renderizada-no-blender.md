# ADR-0004: arte dos dados renderizada no Blender

Status: Aceito (2026-11-10)

## Contexto

Os dados eram desenhados em código (`ShapeRenderer`), com formas e pips. Para dar personalidade às torres, cada
tipo de dado ganha um personagem que evolui a cada pip, em oito direções e com animações de espera e ataque. O
estilo pedido é de ilustração desenhada à mão, consistente entre níveis, e sem custo com ferramentas pagas.

## Decisão

- Modelar e renderizar os personagens no Blender, por scripts Python: contorno com Freestyle (traço de caneta),
  sombreado com hachura, câmera ortográfica a 35°, fundo transparente.
- Cada nível de pip vira spritesheets em `assets/sprites/` (8 direções; espera com 6 quadros, ataque com 8), mais
  os efeitos do projétil. Um script monta as folhas e informa as métricas usadas em `Sprites.kt`.
- Antes de renderizar um nível inteiro, um teste com poucos quadros é aprovado.
- O jogo troca o desenho em código pelo sprite só nos níveis que já têm arte (`Sprites.CAT_PIPS`); os demais
  dados seguem desenhados em código até ganharem a sua.

## Alternativas consideradas

- **Imagens geradas por IA:** rejeitadas pelo estilo inconsistente entre quadros e níveis, e pelo custo.
- **Desenho vetorial no código:** mantido para os dados sem arte, mas não chega ao nível de detalhe pedido.

## Consequências

- O visual é reproduzível: mudar um detalhe é rodar o script de novo, não redesenhar quadros.
- Renderizar um nível leva cerca de 10 minutos (512 px por quadro); a arte de todos os 25 dados é trabalho longo.
- Os scripts do Blender ficam numa pasta interna fora do git. Regenerar a arte depende dessa pasta, que precisa
  de backup próprio; os PNG gerados estão versionados em `assets/sprites/`.
