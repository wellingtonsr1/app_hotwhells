# Research — Miniatura dos carrinhos na tela Coleção

**Data**: 2026-11-23
**Contexto**: a tela Coleção lista variantes do acervo. A foto não é atributo do modelo nem
da variante: existe por exemplar, com indicação de principal e ordem.

## R1 — Qual foto representa cada variante

- **Decisão**: usar a primeira foto na ordem `(is_primary DESC, sort_order, captured_at)` da
  subconsulta por `variant_id`; o resultado é NULL quando não há foto.
- **Racional**: prioriza a escolha explícita do usuário (principal) e cai para uma ordem
  estável quando ele não marcou nenhuma. A foto continua vindo de um exemplar real da
  própria variante, sem cópia e sem geração.
- **Alternativas consideradas**:
  - Foto no modelo/variante: exigiria coluna nova e cadastro paralelo — fora do escopo.
  - Escolher em memória após carregar todas as fotos: N+1 e payload maior.
  - Foto do exemplar mais antigo: ignora a principal marcada pelo usuário.

## R2 — Foto de exemplar vendido/arquivado (decisão de produto)

- **Decisão**: a miniatura pode usar foto de qualquer exemplar da variante, inclusive
  `SOLD`/`TRADED`/`ARCHIVED`. A contagem permanece só de `OWNED`.
- **Racional**: manter a continuidade visual da variante quando o colecionador registra a
  venda; sem isso, o cartão volta ao marcador e o acervo “perde” a referência visual.
- **Consequência registrada**: cartão pode mostrar foto de item não possuído; a contagem é
  o indicador de posse. Decisão registrada no spec (Assumptions) e coberta por teste.

## R3 — Carregamento e cache da imagem

- **Decisão**: reutilizar `ItemThumbnail` com Coil `AsyncImage` (2.7.0, já no projeto), cache
  de memória e disco padrão, carga sob demanda pelo `LazyColumn`.
- **Racional**: zero dependência nova; o componente já é usado em busca, wishlist, coleções
  e lista de exemplares, e resolve o caminho relativo em `filesDir`.
- **Alternativas consideradas**:
  - Glide/Picasso: dependência nova sem ganho.
  - Bitmap manual com `FileInputStream`: repetiria cache e decodificação fora do escopo.
  - `painterResource`/asset: violaria R.2 (foto vem do acervo do usuário).

## R4 — Falha de arquivo (D1)

- **Decisão**: estado local `loadFailed` alimentado por `onError` do `AsyncImage`; o
  componente passa a renderizar o mesmo marcador de “sem foto”.
- **Racional**: um caminho no banco sem arquivo (restauração parcial, limpeza do SO) não pode
  virar quadrado vazio; o cartão precisa continuar legível (FR-012/SC-005).
- **Alternativas consideradas**:
  - `error` painter com drawable estático: mais um recurso para manter.
  - Deixar em branco: foi o comportamento encontrado e é justamente a divergência.

## R5 — Acessibilidade (D2/D6)

- **Decisão**: descrição da foto composta por modelo, cor e ano; marcador com descrição
  própria («Sem foto do carrinho»); nova string em `strings.xml`.
- **Racional**: atende IV.6 e diferencia variantes do mesmo modelo no TalkBack. A composição
  fica na apresentação, como o subtítulo — o domínio não carrega idioma (Art. IV).
- **Alternativas consideradas**:
  - Reutilizar a descrição da foto no marcador: anunciaria “Foto de…” sem foto.
  - `contentDescription = null`: é a omissão atual; falha o requisito.

## R6 — Altura e leitura do cartão (D4)

- **Decisão**: nome e subtítulo com `maxLines = 2` e `TextOverflow.Ellipsis`; miniatura fixa
  em 64 dp; o atalho «Ver exemplares» permanece abaixo do bloco de texto.
- **Racional**: nomes longos e fonte ampliada esticavam o cartão; duas linhas preservam a
  identificação e estabilizam o ritmo da lista.
- **Alternativas consideradas**:
  - Altura fixa do cartão: cortaria informação em fonte grande (IV.6).
  - `singleLine`: truncaria modelos com nome composto, comuns em diecast.

## R7 — Toque na miniatura (decisão de produto)

- **Decisão**: tocar na foto amplia a imagem (diálogo já existente); tocar no restante do
  cartão mantém o caminho atual (cadastro rápido) e «Ver exemplares» abre a lista.
- **Racional**: a foto é o alvo natural para conferir detalhes; o texto e o atalho continuam
  levando aos mesmos destinos. O comportamento fica documentado no spec (FR-007).
- **Alternativas consideradas**:
  - Desligar a ampliação só na Coleção: iguala alvos, mas remove um recurso útil e já
    testado na lista de exemplares.

## R8 — Consulta e ausência de N+1

- **Decisão**: manter a foto na mesma instrução de `observeSummaries`, como subconsulta
  correlacionada; o índice `photo(exemplar_id, angle, sort_order)` sustenta o acesso.
- **Racional**: uma ida ao banco por emissão da lista; sem carregar fotos de cartões fora da
  tela.
- **Alternativas consideradas**:
  - Consulta separada por variante: N+1 explícito, proibido pelo requisito de desempenho.
  - `LEFT JOIN` com agregação: mais complexo e sem ganho medido.
- **Ressalva**: o custo por linha é teórico; SC-003 continua sem medição (L1).

## R9 — Nenhuma alteração estrutural

- **Decisão**: sem migração, sem tabela, sem coluna, sem mudança em backup, financeiro ou
  regras de cadastro. A única alteração de dados é leitura.
- **Racional**: a Constituição (R.8, I.10) e o pedido exigem a menor alteração possível;
  `VariantSummary.primaryPhotoPath` já existia como projeção.
