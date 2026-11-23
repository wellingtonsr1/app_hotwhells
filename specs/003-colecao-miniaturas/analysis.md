# Análise de Divergências — Spec 003 ↔ Implementação da tela Coleção

**Data**: 2026-11-23
**Spec**: [`spec.md`](./spec.md)
**Escopo da análise**: requisitos do spec 003 conferidos contra o código atual da tela
**Coleção** (aba «Catálogo», título «Coleção»), do componente de miniatura e da consulta
que fornece a foto representativa.
**Fora de escopo**: `plan.md` e `tasks.md` ainda não existem, portanto esta análise **não**
substitui o portão G5 completo (Constituição ↔ spec ↔ plano ↔ tarefas). Ela cobre apenas
spec ↔ código.

## Método

Leitura direta e conferência de comportamento em:

- `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt`
- `app/src/main/java/br/com/diecastcollector/ui/components/ItemThumbnail.kt`
- `core/data/src/main/kotlin/br/com/diecastcollector/data/db/dao/ItemDao.kt` (`VariantDao.observeSummaries`)
- `core/data/src/main/kotlin/br/com/diecastcollector/data/repository/SupportRepositories.kt`
- `app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt`
- `core/domain/src/main/kotlin/br/com/diecastcollector/domain/model/VariantSummary.kt`
- `core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/Theme.kt`
- testes existentes (`VariantRepositoryTest`, `PhotoRepositoryTest`, `CatalogFlowTest`, `SearchScreenTest`)

## Resumo

| ID | Requisito | Situação | Severidade | Correção |
|---|---|---|---|---|
| D1 | FR-012 (arquivo inválido → placeholder) | Corrigido | Média | Código |
| D2 | FR-004 / FR-015 (placeholder acessível) | Corrigido | Baixa–Média | Código |
| D3 | Edge case: exemplar vendido/arquivado | Aceito no spec | Média | Spec |
| D4 | FR-014 (altura consistente) | Corrigido | Baixa | Código |
| D5 | FR-007 (toque no cartão) | Aceito no spec | Média | Spec |
| D6 | FR-015 (descrição distingue o carrinho) | Corrigido | Baixa | Código |
| L1 | SC-003 (desempenho) | Em aberto | Média | Medição |
| L2–L4 | Testes do cartão, placeholder e subtítulo | Cobertos | Média | Testes |

Conformes sem ressalva: FR-001, FR-002, FR-003 (com ressalva O1), FR-005, FR-006, FR-008,
FR-009, FR-010, FR-011, FR-013.

---

## Divergências

### D1 — Arquivo de foto ausente ou ilegível não cai no placeholder (FR-012, edge case)

**O que o spec exige**: falha ao ler/decodificar um arquivo deve resultar no espaço
reservado com ícone de carrinho, nunca em área vazia.

**O que o código faz**: `ItemThumbnail.kt:77` usa `AsyncImage` sem `error` nem
`placeholder`. Quando o arquivo não existe ou está corrompido, o Coil não desenha nada e o
`Box` permanece apenas com o fundo `surfaceVariant` (`ItemThumbnail.kt:60-65`) — um
quadrado cinza vazio, sem o ícone de carrinho. O caminho `photoPath.isNullOrBlank()`
(`ItemThumbnail.kt:66-75`) só cobre ausência de registro, não falha de arquivo.

**Impacto**: 5% de arquivos inválidos (SC-005) produzem cartões com “buraco” visual, e o
usuário não distingue “sem foto” de “foto quebrada”.

**Correção mínima**: estado de erro no `AsyncImage` renderizando o mesmo ícone do
placeholder, reutilizando o bloco existente. Alteração dentro do componente compartilhado,
sem tocar em layout, dados ou navegação.

---

### D2 — Placeholder sem semântica de “sem foto” (FR-004, FR-015)

**O que o spec exige**: o espaço reservado deve ser anunciado como ausência de foto.

**O que o código faz**: o ícone do placeholder tem `contentDescription = null`
(`ItemThumbnail.kt:72`) e o `Box` não tem `semantics`; o TalkBack não anuncia a área. A
foto real recebe descrição (`catalog_thumbnail_photo`, `CatalogScreen.kt:193-196`), o
placeholder não recebe nada.

**Impacto**: requisito IV.6 da Constituição (rótulos semânticos em imagens) fica atendido
só no caminho com foto.

**Correção mínima**: uma string nova em `strings.xml` (ex.: «Sem foto») usada como
`contentDescription` do placeholder; nenhuma mudança de layout.

---

### D3 — Miniatura pode mostrar foto de exemplar que o usuário não possui mais

**O que o spec exige** (edge case): variante cujo único exemplar foi vendido ou arquivado
não deve ter a foto representando um item que não é mais possuído, se isso induzir a erro.

**O que o código faz**: em `ItemDao.kt:62` a contagem filtra `e.status = 'OWNED'`, mas a
subconsulta da foto (`ItemDao.kt:63-68`) não filtra status algum. Uma variante com todos
os exemplares `SOLD`/`TRADED`/`ARCHIVED` aparece com «nenhum exemplar» **e** a foto de um
exemplar que saiu do acervo.

**Impacto**: contradição visível dentro do mesmo cartão; pode levar o colecionador a crer
que ainda possui o item.

**Correção possível (código)**: adicionar `AND e.status = 'OWNED'` à subconsulta da foto.
Efeito colateral: `observeSummaries` alimenta também `CollectionDetailViewModel`,
`NegotiationViewModel` e `WishlistFormViewModel`; a mudança é coerente para todos (mesma
regra de “possuído”), mas precisa ser aceita explicitamente, pois altera a miniatura
dessas telas.

**Alternativa (spec)**: aceitar a foto de qualquer exemplar da variante como continuidade
visual e registrar essa decisão em Assumptions, removendo a ressalva do edge case.

**Decisão necessária antes da implementação** — é a única divergência que muda o que o
usuário vê em mais de uma tela.

---

### D4 — Altura do cartão não é uniforme (FR-014)

**O que o spec exige**: altura consistente entre cartões.

**O que o código faz**: `VariantCard` (`CatalogScreen.kt:179-218`) não define altura mínima
nem `maxLines`; nome e subtítulo (`CatalogScreen.kt:200-207`) podem quebrar em 2+ linhas, e
o `TextButton` «Ver exemplares» (`CatalogScreen.kt:212-216`) fica sempre abaixo, somando
altura que varia com o conteúdo.

**Impacto**: listas com nomes longos alternam cartões altos e baixos; a miniatura de 64 dp
permanece estável, mas o ritmo visual não.

**Correção mínima**: `maxLines` + `overflow = TextOverflow.Ellipsis` no nome e no subtítulo
(ou altura mínima comum). Não exige mudança de dados.

---

### D5 — Tocar na miniatura abre a ampliação, não o cartão (FR-007)

**O que o spec exige**: tocar no cartão mantém o comportamento atual; a miniatura não pode
interferir na identificação da variante.

**O que o código faz**: quando há foto, `ItemThumbnail` aplica `Modifier.clickable {
expanded = true }` (`ItemThumbnail.kt:64`), abrindo o diálogo de imagem cheia
(`ItemThumbnail.kt:85-140`). Como o `clickable` interno consome o toque, a área da foto
**não** aciona o cartão, enquanto o restante do cartão abre o cadastro rápido
(`AppNavHost.kt:314-320`).

**Impacto**: alvo de toque inconsistente dentro do mesmo cartão e comportamento não
previsto no spec 003. Em fonte ampliada, o alvo mais óbvio (a imagem) faz outra coisa.

**Correção possível (código)**: no cartão da Coleção, manter o toque na miniatura como
toque do cartão (desligar a ampliação nesse uso). Isso exige uma opção no componente — por
exemplo, não habilitar a ampliação quando `onOpenGallery == null` e o chamador pedir
comportamento passivo.

**Alternativa (spec)**: documentar a ampliação como comportamento desejado da Coleção e
ajustar FR-007/Assumptions, mantendo o restante do cartão como está.

---

### D6 — Descrição acessível não distingue variantes do mesmo modelo (FR-015)

**O que o spec exige**: a descrição deve permitir identificar o carrinho a que a imagem
pertence.

**O que o código faz**: `catalog_thumbnail_photo` = «Foto de %1$s» preenchido com
`variant.modelName` (`CatalogScreen.kt:193-196`). Duas variantes do mesmo modelo (cores ou
edições diferentes) recebem a mesma descrição.

**Impacto**: navegação por TalkBack fica ambígua entre variantes do mesmo modelo.

**Correção mínima**: compor a descrição com cor e ano já disponíveis em `VariantSummary`,
sem tocar no domínio (a composição é da apresentação, como o subtítulo).

---

## Lacunas de verificação (não são divergências de código)

### L1 — SC-003 sem medição

Nenhum teste ou registro mede “500 variantes em menos de 1 segundo”. Pela Constituição V.8
o critério não pode ser declarado atendido sem medição reproduzível; hoje ele é meta, não
resultado.

### L2 — Sem teste do placeholder e do arquivo inválido

`VariantRepositoryTest` cobre a foto principal e a consulta; `PhotoRepositoryTest` cobre o
mapa de capas por variante. Não há teste para: variante sem nenhuma foto, arquivo apontado
mas inexistente, ou falha de decodificação.

### L3 — Sem teste da escolha determinística sem principal, nem teste de UI do cartão

A regra `(1 - is_primary), sort_order, captured_at` (`ItemDao.kt:66`) não tem teste com
várias fotos **sem** principal. `CatalogFlowTest` percorre o fluxo e clica no cartão «Twin
Mill», mas não verifica miniatura, placeholder, descrição acessível nem o atalho «Ver
exemplares» a partir do cartão.

### L4 — Sem teste da montagem do subtítulo

`variantSubtitle` (`CatalogScreen.kt:224-241`) não tem teste de unidade/instrumentado:
ano ausente, fabricante presente, «nenhum exemplar» e plural são regras de apresentação
testáveis sem banco.

---

## Observações (conformidade, com ressalva)

- **O1 — FR-010**: a foto vem na mesma instrução SQL da listagem, sem segunda consulta por
  cartão; porém é uma subconsulta correlacionada executada por linha. O índice
  `photo(exemplar_id, angle, sort_order)` (`PhotoEntity.kt:26`) sustenta o acesso. Atende o
  requisito; o custo por linha só deve ser tratado se a medição de L1 indicar problema.
- **FR-003 — empate residual**: `sort_order` e `captured_at` podem empatar; nesse caso a
  ordem entre fotos empatadas não é totalmente determinada. Risco baixo, mas a garantia de
  “mesma foto em toda abertura” não é absoluta.
- **FR-008**: a pesquisa e os filtros vivem na tela de busca (`SearchScreen`), que tem
  miniatura própria via `CatalogViews.kt:190`; a Coleção em si não tem filtros, apenas os
  atalhos (incompletos, coleções, duplicados, código de barras), todos preservados.
- **FR-013**: tema escuro confirmado (`Theme.kt:26-38`: `InkBlack`, `SmokyGrey`,
  `FlameRedDark`), componentes Material 3; nenhuma cor nova introduzida.
- **FR-001**: 64 dp, cantos de 10 dp, quadrada, `ContentScale.Crop`, centralizada na `Row`
  e 12 dp de espaçamento (`CatalogScreen.kt:190-198`) — dentro da faixa de 64–88 dp.

---

## Conjunto mínimo sugerido (se todas as divergências de código forem aceitas)

1. `ItemThumbnail.kt`: fallback de erro reutilizando o placeholder (D1) e descrição do
   placeholder (D2).
2. `ItemDao.kt`: filtro de status na subconsulta da foto (D3) — **requer decisão de produto**.
3. `CatalogScreen.kt`: `maxLines`/ellipsis (D4), descrição composta com cor/ano (D6) e
   política de toque na miniatura (D5).
4. `strings.xml`: uma string para «sem foto» e o formato de descrição ampliado.
5. Testes: `VariantRepositoryTest` (sem foto, sem principal, exemplar vendido),
   teste Compose do cartão (placeholder, descrição, «Ver exemplares») e teste do subtítulo.
6. Medição de L1 antes de declarar SC-003 atendido.

Nenhum item exige migração, alteração de esquema, tabela nova, mudança de backup ou de
regras de cadastro. D3 e D5 podem ser resolvidos no spec em vez do código; nesse caso não
há alteração de comportamento no app.

## Resolução adotada (2026-11-23)

**Decisões de produto**

- **D3 — aceito no spec**: a miniatura continua usando a foto de qualquer exemplar da
  variante, inclusive vendido ou arquivado. A contagem é que informa o que é possuído.
  Nenhuma mudança na consulta; comportamento coberto por teste.
- **D5 — aceito no spec**: tocar na miniatura amplia a imagem; tocar no restante do cartão
  mantém o caminho atual. FR-007 e as Assumptions registram a decisão.

**Correções de código aplicadas**

- `ItemThumbnail.kt`: estado de falha de carregamento cai no marcador (D1) e o marcador
  ganhou descrição «Sem foto do carrinho» (D2). Vale para todas as listas que usam o
  componente.
- `CatalogScreen.kt`: nome e subtítulo limitados a duas linhas com reticências (D4);
  descrição da miniatura composta por modelo, cor e ano (D6).
- `strings.xml`: nova string `catalog_thumbnail_no_photo`.
- `VariantRepositoryTest`: variante sem foto, escolha sem principal e foto de exemplar
  vendido com contagem zero.
- `CatalogScreenTest` (novo, instrumentado): marcador acessível, descrição com cor e ano,
  ano ausente, «Ver exemplares» na variante certa, toque no cartão, arquivo inválido,
  lista longa sem compor cartões fora da tela (FR-011) e amostra fixa de 20 variantes do
  SC-002.

**Verificação executada**

| Comando | Resultado |
|---|---|
| `:core:data:testDebugUnitTest --tests "*VariantRepositoryTest*"` | 10 testes, 0 falhas |
| `:app:compileDebugKotlin` + `:app:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `:app:connectedDebugAndroidTest` — `CatalogScreenTest` | 8 testes, 0 falhas (emulador API 36) |
| `:app:connectedDebugAndroidTest` — `CatalogFlowTest` + `SearchScreenTest` | 7 testes, 0 falhas |

**Continua em aberto**: L1 — nenhuma medição de SC-003 (500 variantes em menos de 1 s).
Pela Constituição V.8, o critério permanece meta até haver medição reproduzível.
