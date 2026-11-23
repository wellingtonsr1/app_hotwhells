# Data Model — Miniatura dos carrinhos na tela Coleção

**Data**: 2026-11-23
**Regra geral**: esta feature **não altera o esquema**. O documento descreve as entidades já
existentes, a projeção usada pela listagem e as regras de derivação da miniatura.

## Entidades de origem (já existentes, sem alteração)

### Modelo (`car_model`)

| Atributo | Papel na feature |
|---|---|
| `id` | Vínculo com a variante |
| `name` | Nome exibido no cartão e base da descrição da foto |
| `manufacturer_id` | Origem do fabricante exibido |
| `archived_at` | Modelo arquivado não entra na listagem |

### Variante (`variant`)

| Atributo | Papel na feature |
|---|---|
| `id` | Identidade do cartão; chave do `LazyColumn` |
| `model_id` | Vínculo com o modelo |
| `primary_color_id` | Cor exibida na descrição da miniatura |
| `release_year` | Ano exibido no subtítulo e na descrição |
| `archived_at` | Variante arquivada não entra na listagem |

### Exemplar (`exemplar`)

| Atributo | Papel na feature |
|---|---|
| `id` | Vínculo das fotos |
| `variant_id` | Agrupa os exemplares da variante |
| `status` | `OWNED` conta para «N exemplares»; os demais não contam, mas **podem** fornecer a foto (decisão R2) |

### Fotografia (`photo`)

| Atributo | Papel na feature |
|---|---|
| `exemplar_id` | Vínculo com o exemplar |
| `path` | Caminho relativo em `filesDir`; é o valor exibido pela miniatura |
| `is_primary` | Prioridade máxima na escolha |
| `sort_order` | Segunda chave de ordenação |
| `captured_at` | Terceira chave de ordenação |

Nenhuma entidade, coluna, índice ou migração é criada. O índice
`photo(exemplar_id, angle, sort_order)` já existente sustenta a subconsulta.

## Projeção de leitura (`VariantSummary`)

Usada pela tela; não é entidade persistida.

| Campo | Origem | Obrigatório | Regra |
|---|---|---|---|
| `variantId` | `variant.id` | sim | Chave estável do cartão |
| `modelName` | `car_model.name` | sim | Texto do título |
| `manufacturerName` | `manufacturer.name` | sim | Texto do subtítulo |
| `colorName` | `color.name` via `primary_color_id` | não | `NULL` quando o cadastro está incompleto |
| `releaseYear` | `variant.release_year` | não | `NULL` não vira texto nem valor padrão |
| `exemplarCount` | contagem de `exemplar` com `status = 'OWNED'` | sim | Zero é válido e exibido como «nenhum exemplar» |
| `primaryPhotoPath` | `photo.path` da foto escolhida | não | `NULL` ou vazio ⇒ marcador |

## Regras de derivação

1. **Escopo**: somente variantes com `archived_at IS NULL`, ordenadas por fabricante, modelo
   e criação (comportamento atual preservado).
2. **Contagem**: `COUNT` de exemplares `OWNED` da variante — nunca armazenada (Art. I.2).
3. **Foto representativa**: uma única linha por variante, escolhida por
   `(1 - is_primary), sort_order, captured_at`, restrita às fotos de exemplares da própria
   variante.
4. **Estabilidade**: enquanto os dados não mudarem, a mesma foto é escolhida em toda
   abertura. Empate absoluto nas três chaves é raro; a ordem residual é do SQLite.
5. **Degradação**: caminho nulo, vazio, arquivo ausente ou falha de decodificação ⇒ marcador
   de carrinho com a mesma dimensão.
6. **Posse**: a foto não é filtrada por `status`; a contagem é que expressa posse (R2).

## Invariantes verificáveis

- Nenhum cartão sem área de imagem (foto ou marcador).
- A foto exibida pertence a um exemplar da variante do cartão — nunca a outra variante.
- Nenhum arquivo é copiado, renomeado ou apagado pela feature.
- A contagem não muda por causa da miniatura.
- Modelos e exemplares existentes continuam válidos sem conversão de dados.

## Transições de estado

Não há transição de domínio. Há apenas estado de apresentação da miniatura:

```text
SEM_CAMINHO ─────────────► MARCADOR
CAMINHO_VALIDO ─ carrega ─► FOTO
CAMINHO_VALIDO ─ onError ─► MARCADOR
CAMINHO_VALIDO ─ toque ───► AMPLIACAO ─ fecha ─► FOTO
```
