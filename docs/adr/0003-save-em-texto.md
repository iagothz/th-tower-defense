# ADR-0003: save em texto sobre `Preferences`

Status: Aceito (2026-10-26, estendido para a nuvem em 2026-10-30)

## Contexto

O jogo guarda recordes, deck, progresso da conta, histórico e a partida em andamento, que precisa sobreviver ao
Android fechar o app. Depois veio o save na nuvem pelo Play Games, que guarda um único blob de bytes por conta. O
volume é pequeno (dezenas de chaves) e o formato muda a cada versão com recursos novos.

## Decisão

- `Preferences` do libGDX (SharedPreferences no Android, arquivo no PC) como armazenamento, encapsulado em
  `SaveData`. Valores compostos (histórico, decks, conquistas) são serializados como texto simples.
- A partida em andamento é um snapshot em texto, uma linha `chave=valor` por campo, com versão (`v=`). Um
  snapshot que não lê é descartado e registrado no log; a partida não é retomada.
- O save na nuvem é um export em texto das chaves sincronizáveis (`tipo:chave=valor` por linha), com hash e
  versão para decidir entre subir, adotar ou perguntar. Configurações do aparelho e a partida em andamento não
  sobem.
- Compatibilidade por ausência: uma chave ou linha que não existe num save antigo assume o valor de antes do
  recurso (por exemplo, sem `map` vale o mapa padrão; sem `levels`, todo dado no nível 1). As chaves nunca são
  renomeadas.

## Alternativas consideradas

- **SQLite/Room:** consultas e esquema que o jogo não precisa, sem build igual no PC.
- **JSON com biblioteca de serialização:** mais estrutura, mas mais uma dependência e o mesmo problema de
  migração; o texto por linha já é legível e fácil de comparar.

## Consequências

- O save é legível e depurável à mão; os testes usam `Preferences` em memória.
- Não há migração explícita: cada recurso novo precisa de um padrão para saves antigos, coberto por teste.
- Sem esquema, um erro de digitação numa chave passa sem aviso; as chaves ficam concentradas em `SaveData` e
  `CloudBlob` para reduzir esse risco.
