# Data Model — Manual de Usuário Completo

**Feature**: `002-manual-usuario` | **Data**: 2026-11-21 | **Spec**: [spec.md](./spec.md)
| **Research**: [research.md](./research.md)

Este documento tem duas partes:

1. **Modelo de informação do manual** — entidades que os documentos, a matriz e o índice
   usam, com campos e regras de consistência.
2. **Mapa das entidades reais do app** — o que o manual precisa explicar, conforme o código
   da versão documentada (`0.1.0`, commit `424e4fb`).

Nada aqui cria tabela, código ou dado no aplicativo: é modelo documental e referência de
conteúdo.

---

## Parte 1 — Modelo de informação do manual

### Tela

Destino navegável ou estado de tela com identidade própria.

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | rota estável do `Destination` (ex.: `catalog/variant/{variantId}`) |
| `titulo_visivel` | sim | texto exibido (título da tela), copiado de `strings.xml` |
| `rota` | sim | igual à rota declarada em `AppNavHost.kt`; nenhuma rota inventada |
| `argumentos` | não | nomes de `RouteArgs` e se são opcionais |
| `arquivo_tela` | sim | `feature/<área>/<Nome>Screen.kt` |
| `viewmodel` | sim | ViewModel/estado que sustenta a tela; “sem ViewModel” é valor válido |
| `finalidade` | sim | uma frase, em pt-BR, sobre o que a tela resolve |
| `acesso` | sim | de onde se chega (aba, botão, item de lista, Ajustes) |
| `estados` | sim | vazio, carregando, erro e sucesso — “não existe” precisa ser declarado |
| `capitulo` | sim | âncora do capítulo/seção no manual |
| `capturas` | sim | ids das evidências visuais; lista vazia exige lacuna registrada |

**Regras**
- Toda `Tela` corresponde a exatamente um `Destination` ou a um subestado explicitamente
  nomeado (ex.: diálogo de exclusão dentro do detalhe do exemplar).
- `titulo_visivel` nunca é o nome da função Composable nem a rota.
- `arquivo_tela` e `viewmodel` precisam existir no repositório no commit documentado.
- Toda `Tela` aparece em `MATRIZ_COBERTURA.md` com vínculo para `capitulo` (zero órfãos).

### Elemento de interface

Botão, campo, ícone, opção, filtro, seletor, indicador, diálogo ou aba.

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | `<tela.id>#<slug>`; slug estável e único na tela |
| `tipo` | sim | botão, campo, ícone, opção, filtro, seletor, indicador, diálogo, aba |
| `rotulo_visivel` | sim | texto exibido; se não houver texto, descrever forma/ícone |
| `content_description` | condicional | obrigatório quando existir; declarar ausência caso contrário |
| `posicao` | sim | topo, barra superior, corpo, barra inferior, diálogo, menu |
| `acao` | sim | o que acontece ao usar |
| `pre_condicao` | não | estado/dado necessário |
| `resultado` | sim | efeito observável (mensagem, navegação, alteração) |
| `mensagens` | não | ids de `Mensagem` associados |
| `evidencia` | condicional | captura que mostra o elemento |

**Regras**
- Cada elemento interativo visível na `Screen.kt` precisa de uma linha (FR-008/SC-002);
  itens não interativos só entram quando explicam leitura (indicadores, cabeçalhos, chips).
- `rotulo_visivel` é literal, inclusive plural, caixa e pontuação.
- Elemento sem rótulo acessível gera `Lacuna` de acessibilidade, não omissão.

### Procedimento

Sequência numerada que produz um resultado no app.

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | `proc-<slug>` |
| `titulo` | sim | começa com verbo (ex.: “Cadastrar o primeiro exemplar”) |
| `pre_condicoes` | sim | instalação limpa, dado existente ou “nenhuma” |
| `passos` | sim | um por linha, citando `Elemento.id` quando aplicável |
| `resultado_esperado` | sim | o que o usuário vê/obtém ao final |
| `validacoes` | não | regras que podem bloquear o passo |
| `mensagens` | não | mensagens previstas |
| `executado_em` | condicional | data + emulador/aparelho quando validado por execução |

**Regras**
- Passo cita rótulo visível, nunca rota interna.
- Procedimento não validado por execução fica marcado como “não executado” e justificado.
- O roteiro de primeiros passos e o exemplo Skyline são procedimentos de aceitação
  (SC-003/SC-004) e precisam de `executado_em` preenchido.

### Mensagem

Texto exibido pelo app (erro, confirmação, aviso, vazio).

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | chave da string, quando existir |
| `texto` | sim | literal de `strings.xml`; placeholders resolvidos em exemplo |
| `gatilho` | sim | quando aparece |
| `severidade` | sim | erro, aviso, sucesso, informação |
| `acao_recomendada` | sim | o que o usuário faz |

### Evidência visual

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | nome do arquivo em `docs/manual/capturas/` |
| `tela` / `estado` | sim | vínculo com `Tela.id` e estado representado |
| `origem` | sim | emulador `diecast_test` ou aparelho físico |
| `data` | sim | data da captura |
| `commit` | sim | commit instalado na captura |
| `legenda` | sim | legenda publicada junto da imagem |
| `anotacoes` | não | números/rótulos sobrepostos e sua legenda |
| `dados` | sim | confirmação de que são fictícios |

**Regras**
- Arquivo precisa existir; caminho relativo a partir do documento que o cita.
- Mockup não é evidência (FR-016). Diagrama é evidência de relacionamento/fluxo, não de tela.
- Captura ausente gera `Lacuna`, nunca imagem genérica.

### Lacuna documental

| Campo | Obrigatório | Regra |
|---|---|---|
| `id` | sim | `lac-<n>` |
| `item_afetado` | sim | `Tela`, `Elemento`, `Procedimento` ou `Entidade` |
| `motivo` | sim | inacessível, não executável, não capturável, parcial, legado divergente |
| `impacto` | sim | o que o leitor perde |
| `alternativa_real` | não | o que existe no lugar, quando existe |
| `destino` | sim | seção do manual + entrada em `LIMITACOES_DOCUMENTACAO.md` |

### Artefatos derivados

| Artefato | Deriva de | Chave de consistência |
|---|---|---|
| `MANUAL_USUARIO_COMPLETO.md` | Telas → capítulos, Elementos, Procedimentos, Mensagens | toda `Tela.capitulo` existe |
| `INDICE_FUNCIONALIDADES.md` | Telas, Elementos, Procedimentos, Entidades | todo item tem link de capítulo |
| `MATRIZ_COBERTURA.md` | 30 destinos + funcionalidades | nenhuma linha sem seção (contracts/coverage-contract.md) |
| `LIMITACOES_DOCUMENTACAO.md` | Lacunas | toda lacuna citada no ponto de uso |
| `capturas/` | Evidências | todo caminho citado existe |

### Relacionamentos

```text
Tela 1 ── N Elemento
Tela 1 ── N Procedimento (tela participa de N procedimentos)
Tela 1 ── N Evidência
Mensagem N ── N Elemento / Procedimento
Tela / Elemento / Procedimento 0..1 ── 0..N Lacuna
Entidade de domínio 1 ── N Tela (a entidade é explicada em várias telas)
```

### Validações do modelo documental

- `Tela.rota` ∈ rotas de `Destination`; `Tela.titulo_visivel` ∈ `strings.xml`.
- `Elemento.rotulo_visivel` idêntico ao literal exibido, sem tradução livre.
- `Procedimento.passos` não pode citar elemento inexistente na tela do passo.
- `Evidência.origem`/`data`/`commit` obrigatórios; `Lacuna.motivo` obrigatório.
- `MATRIZ_COBERTURA.md` sem linha vazia de vínculo; `INDICE` sem item sem capítulo.

---

## Parte 2 — Entidades reais do app (conteúdo a explicar)

Fonte: `core/domain/src/main/kotlin/.../domain/model/` e entidades Room em
`core/data/.../db/entity/`. O manual não reproduz o DER técnico; explica estas entidades
com nomes de colecionador (Art. I.1, FR-003).

### Hierarquia do catálogo

| Entidade | Campos relevantes | Relacionamentos |
|---|---|---|
| **Fabricante** (`Manufacturer`) | `name`, `country`, `archivedAt` | 1→N modelos, séries, marcas |
| **Marca** (`Brand`) | `name`, `manufacturerId`, `archivedAt` | N→1 fabricante |
| **Veículo real** (`RealVehicle`) | `make`, `model`, `year`, `country` | referenciado pelo modelo |
| **Modelo** (`CarModel`) | `name`, `scale` (padrão `1:64`), `bodyType`, `material`, `countryOfManufacture`, `notes`, `archivedAt` | N→1 fabricante; 1→N variantes |
| **Série** (`Series`) | `name`, `parentId`, `kind`, `year`, `archivedAt` | N→1 fabricante; hierarquia por `parentId` |
| **Cor** (`ColorItem`) | `name`, `hex` | cor principal e secundárias da variante |
| **Variante** (`Variant`) | `fingerprint` (obrigatório), `primaryColorId`, cores secundárias, `wheelsType`, `baseType`, `decoration`, `releaseYear`, `collectionNumber`, `seriesNumber`, `edition`, `manufacturerCode`, `packagingType`, `archivedAt` | N→1 modelo; 1→N exemplares |
| **Exemplar** (`Exemplar`) | `internalCode` (único, obrigatório), `condition`, `packagingCondition`, `status`, `locationId`, `notes`, `rowVersion` | N→1 variante; 1→N fotos, aquisições, avaliações, movimentos |
| **Fotografia** (`Photo`) | `angle`, `path`, `checksum`, `isPrimary`, `sortOrder`, dimensões, `bytes`, `capturedAt` | N→1 exemplar |

**Regra central a explicar**: modelo ≠ variante ≠ exemplar. Duplicidade é por variante +
exemplar, nunca pelo nome do modelo (Art. I.3; FR-011/FR-012).

### Dinheiro e histórico

| Entidade | Campos relevantes | Regra exibida |
|---|---|---|
| **Aquisição** (`Acquisition`) | `date`, `currency` (`BRL`), `price`, `shipping`, `discount`, `store`, `seller`, `method`, `notes` | custo = preço + frete − desconto |
| **Avaliação** (`Valuation`) | `date`, `value`, `currency`, `source`, `notes` | histórico append-only; a mais recente compõe o estimado |
| **Negociação** (`Negotiation`) | `type` (venda/troca), `status`, `startedAt`, `closedAt`, `counterparty`, `channel`, `amount`, `currency`, `receivedExemplarId`, `notes` | fecha negociação e retira o exemplar do investido |
| **Evento de negociação** (`NegotiationEvent`) | `at`, `description`, `amount` | histórico da negociação |
| **Oferta de duplicado** (`DuplicateOffer`) | `availability`, `condition`, `askingPrice`, `notes` | disponibilidade: troca, venda, ambas, indisponível |
| **Movimento** (`Movement`) | `at`, `fromLocationId`, `toLocationId`, `note` | histórico de localização |
| **Histórico** (`HistoryEntry`) | `entityType`, `entityId`, `field`, `oldValue`, `newValue`, `source`, `at` | trilha auditável (Art. I.8) |

Todos os valores monetários são `MoneyCents` (inteiro de centavos, moeda `BRL`), nunca
ponto flutuante. A UI formata em pt-BR.

### Organização, desejos e sistema

| Entidade | Campos relevantes | Regra exibida |
|---|---|---|
| **Coleção** (`Collection`) | `name`, `description`, `parentId`, `manufacturerId`, `archivedAt` | hierarquia por `parentId` |
| **Item de coleção** (`CollectionItem`) | `variantId`/`modelId`, `expected`, `nonCollectible`, `notes` | progresso por unidades esperadas |
| **Item de desejo** (`WishlistItem`) | `priority` (1–3), `status`, vínculo com modelo/variante ou `freeText`, `maxPrice`, `foundPrice`, `store`, `referenceUrl`, `convertedExemplarId` | prioridade alta/média/baixa; conversão preserva histórico |
| **Localização** (`Location`) | `type`, `name`, `parentId`, `archivedAt` | árvore com contagem por subárvore |
| **Reconhecimento** (`RecognitionResult`) | `source`, `provider`, `payloadJson`, `confidence`, `appliedFields`, vínculo com exemplar/draft | sugestão, não identificação automática |
| **Configuração** (`AppSetting`) | `key`, `value` | preferências do app |

### Progresso de coleção (`CollectionProgress`)

- `units` = 0 se `nonCollectible`; senão `expected`.
- `ownedUnits` = `min(ownedExemplars, expected)`; item não colecionável conta 0.
- `missingUnits` = `max(units − ownedUnits, 0)`.
- `isMissing` = `missingUnits > 0`; `isOwned` = `ownedUnits > 0`.
- O percentual exibido deriva de unidades, não do número de linhas.

### Valores de estado (enums)

| Enum | Valores |
|---|---|
| Condição do exemplar | `SEALED`, `LOOSE`, `DAMAGED`, `RESTORED`, `INCOMPLETE` |
| Condição da embalagem | `SEALED`, `OPENED`, `DAMAGED`, `NONE` |
| Status do exemplar | `OWNED`, `SOLD`, `TRADED`, `ARCHIVED` |
| Tipo de localização | `LocationType` (definido em `Enums.kt`) |
| Negociação | tipo `SALE`/`TRADE`; status `OPEN`, `RESERVED`, `CLOSED`, `CANCELLED` |
| Desejo | `WANTED`, `ACQUIRED`, `DISMISSED` |
| Ângulo da foto | `FRONT`, `BACK`, `LEFT`, `RIGHT`, `TOP`, `BASE`, `PACKAGING`, `DETAIL` |
| Origem do reconhecimento | `PHOTO`, `OCR`, `BARCODE`, `CATALOG` |
| Disponibilidade de duplicado | `TRADE`, `SALE`, `BOTH`, `UNAVAILABLE` |

### Regras de validação confirmadas (`CatalogValidation`)

| Regra | Efeito exibido |
|---|---|
| Escala no formato `1:N`, N de 1 a 999 | mensagem “Escala deve estar no formato 1:N, como 1:64.” |
| Nome obrigatório (fabricante/modelo) não vazio e ≤ 120 caracteres | “Informe o …” / “O … excede 120 caracteres.” |
| Ano entre 1886 e ano atual + 2, ou desconhecido | “Ano deve estar entre 1886 e <atual+2>.” |
| Prioridade do desejo entre 1 e 3 | “Prioridade deve estar entre 1 (alta) e 3 (baixa).” |
| Texto opcional vazio/sentinela (`N/A`, `NA`, `null`, `none`, “não informado”) | vira desconhecido, não string literal |
| `internalCode` do exemplar obrigatório e único | cadastro bloqueado sem código |
| `rowVersion` ≥ 0 | proteção contra escrita concorrente (Art. I.12) |
| Foto exige `path` e `checksum` | falha de gravação se ausente |

### Regras de cálculo financeiro (a documentar com exemplo numérico)

- **Investido** = Σ(preço + frete − desconto) de exemplares `OWNED`; sem preço, contribui 0.
- **Estimado** = Σ da avaliação mais recente de cada exemplar `OWNED`.
- **Diferença** = estimado − investido.
- **Custo médio** = investido ÷ exemplares com valor; nulo se não houver.
- **Gastos por fabricante/série/coleção/mês** usam a mesma fórmula de custo.
- Vendas e trocas fechadas aparecem no histórico financeiro, separadas do investido.

### O que o manual NÃO deve afirmar

- Que existem “11 relatórios”: o recurso gera o inventário em PDF, CSV e XLSX.
- Que importação aceita planilha XLSX: é CSV, com pré-visualização e relatório de erros.
- Que código de barras identifica o carrinho sozinho.
- Que o app usa nuvem, conta ou sincronização.
- Que uma tela ausente do `AppNavHost` existe por haver arquivo `*Screen.kt`.
