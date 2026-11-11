# ADR-0005: chave de release fora do git

Status: Aceito (2026-10-27)

## Contexto

O Android só atualiza um app instalado se o APK novo tiver a mesma assinatura. A chave de release é a identidade
do app no aparelho e na loja; vazá-la permite publicar atualizações falsas, e perdê-la impede atualizar o app já
instalado. O repositório é público no GitHub.

## Decisão

- A keystore (`android/keystore/merge-td.jks`) e as senhas (`android/keystore.properties`) ficam só na máquina de
  quem publica, listadas no `.gitignore`.
- O repositório tem só o modelo `android/keystore.properties.example`, sem senhas.
- O build de release lê `keystore.properties` quando existe; sem ele, o release sai sem assinatura e o debug não
  é afetado.
- A chave nunca é regerada: uma chave nova obriga a desinstalar o app antigo.

## Consequências

- Nenhum segredo no histórico do git; o CI gera só o APK de debug.
- A chave e as senhas precisam de backup fora do repositório, sob responsabilidade de quem publica.
- Para publicar na loja com assinatura gerenciada pelo Google Play, esta chave vira a chave de upload.
