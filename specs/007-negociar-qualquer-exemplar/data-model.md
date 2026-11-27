# Data Model: negociar qualquer exemplar da coleção

**Feature**: `007-negociar-qualquer-exemplar` | **Data**: 2026-11-27

Este documento descreve o modelo **já existente** que a feature passa a acessar por mais origens, as **leituras derivadas** novas e a máquina de estados da disponibilidade. **Nenhuma** tabela, coluna, índice, migração ou entidade de domínio é criada ou alterada (D-06, NFR-501).

## 1. Tabelas envolvidas (inalteradas)

### `duplicate_offer` — disponibilidade atual de um exemplar

| Coluna | Tipo | Regra | Origem |
|---|---|---|---|
| `id` | TEXT (UUID) | PK | gerada no repositório |
| `exemplar_id` | TEXT | **UNIQUE**, FK → `exemplar` | uma linha por exemplar (FR-507) |
| `availability` | TEXT | `TRADE` \| `SALE` \| `BOTH` \| `UNAVAILABLE` | `DuplicateAvailability` |
| `condition` | TEXT? | `NULL` quando não informada (I.9) | condição da proposta, não do exemplar |
| `asking_price_cents` | INTEGER? | `NULL` quando não informado | valor pretendido |
| `notes` | TEXT? | `NULL` quando vazia | observações da negociação |
| `created_at`, `updated_at` | INTEGER | epoch ms | auditoria da linha |

**É estado atual, não histórico**: sobrescrever a oferta não gera trilha própria (BR-511); as negociações já realizadas permanecem.

### `negotiation` — tentativa de venda ou troca de um exemplar

| Coluna | Tipo | Regra |
|---|---|---|
| `id` | TEXT (UUID) | PK |
| `exemplar_id` | TEXT | FK → `exemplar`; índice existente |
| `type` | TEXT | `SALE` \| `TRADE` |
| `status` | TEXT | `OPEN` \| `RESERVED` \| `CLOSED` \| `CANCELLED`; índice existente |
| `counterparty`, `channel`, `notes` | TEXT? | `NULL` quando vazios |
| `started_at`, `closed_at` | INTEGER | epoch ms |
| `amount_cents`, `currency` | INTEGER?, TEXT? | valor discutido |
| `received_exemplar_id` | TEXT? | FK → `exemplar`, só na troca |

**Unicidade de negociação ativa**: garantida na transação de `open` (`findActiveForExemplar`), porque o SQLite não expressa índice parcial único de `OPEN`/`RESERVED` pelo Room. Nada muda aqui (FR-513, BR-505).

### `negotiation_event` — trilha append-only

`id`, `negotiation_id`, `at`, `description`, `amount_cents`. Aberta, reservada, concluída ou cancelada; nunca apagada (BR-507).

### `exemplar` — unidade física

Relevantes: `id`, `internal_code`, `variant_id`, `condition`, `status` (`OWNED`/`SOLD`/`TRADED`/`ARCHIVED`), `row_version`, `location_id`, `notes`.

## 2. Regra derivada: exemplar único

```text
ownedInVariant = COUNT(*) FROM exemplar
                 WHERE variant_id = :variantId AND status = 'OWNED'
```

Já disponível em `ExemplarRepository.countByVariant` (usado pelo cadastro rápido, FR-039). Nenhuma consulta nova no banco.

| `ownedInVariant` | Significado | Aviso possível? |
|---|---|---|
| 0 | variante sem unidade no acervo | não (não há `OWNED` para negociar) |
| **1** | **exemplar único** | **sim**, nas duas intenções |
| 2+ | variante repetida | não |

## 3. Tabela-verdade do aviso (FR-515)

| `ownedInVariant` | `status` do exemplar | Oferta sendo salva | Negociação ativa? | Momento | Aviso? |
|---|---|---|---|---|---|
| 1 | `OWNED` | `SALE`/`TRADE`/`BOTH` | não | disponibilidade | **sim** |
| 1 | `OWNED` | `UNAVAILABLE` | não | disponibilidade | não |
| 1 | `OWNED` | qualquer | **sim** | disponibilidade | não (já avisado na abertura; estado dominante é **Em negociação**) |
| 1 | `OWNED` | — | não | negociação | **sim** |
| 1 | `OWNED` | — | **sim** | negociação | não (a tela mostra a ativa) |
| 2+ | `OWNED` | qualquer | qualquer | qualquer | não |
| qualquer | `SOLD`/`TRADED`/`ARCHIVED` | qualquer | qualquer | qualquer | não (ação bloqueada por FR-506) |

A tabela é o contrato de `UniqueExemplarPolicy`; o teste de domínio é a tabela em forma de código.

## 4. Leituras derivadas novas (sem persistência)

| Leitura | Grão | Fonte | Uso |
|---|---|---|---|
| `DuplicateRepository.observeOffer(exemplarId)` | 1 exemplar | `duplicate_offer` por `exemplar_id` | ficha |
| `DuplicateRepository.observeOffersByVariant(variantId)` | variante | `duplicate_offer` ⋈ `exemplar` | lista |
| `NegotiationRepository.observeActiveByVariant(variantId)` | variante | `negotiation` (`OPEN`/`RESERVED`) ⋈ `exemplar` | lista |

Assinaturas: `observeOffer(exemplarId): Flow<DuplicateOffer?>`; `observeOffersByVariant(variantId): Flow<Map<String, DuplicateOffer>>`; `observeActiveByVariant(variantId): Flow<Map<String, Negotiation>>`. O DAO emite `List` e o repositório indexa por `exemplarId` (T505/T506).

Todas são `Flow` reativos pela mesma tabela que o Duplicados já observa: **uma fonte de verdade** (FR-505, BR-510). Nenhum dado é copiado para `UiState` de forma persistente — os `Map` por `exemplar_id` são montados a cada emissão.

## 5. Estado de apresentação

```text
OfferSheetSubject(internalCode, title, condition, offer?)
        │
        ▼
Editando(availability, condition, priceText, notes)   ← estado local da folha
        │  Salvar
        ▼
PendingOffer(availability, condition, askingPrice, notes)   ← memória do ViewModel
        │  warnsOnAvailability == true
        ▼
Avisando(uniqueExemplar)   ── Cancelar ──▶ Editando (mesma seleção, nada gravado)
        │  Continuar
        ▼
Gravando ── ok ──▶ Fechada + mensagem
        └── erro ──▶ Editando + mensagem (valores mantidos, FR-512)
```

`PendingOffer` nunca é serializado, persistido ou restaurado: rotação ou morte de processo durante o aviso simplesmente não produz gravação (caso de borda da spec).

## 6. Invariantes

- **I-1**: no máximo um `duplicate_offer` por `exemplar_id` (UNIQUE do esquema).
- **I-2**: no máximo uma `negotiation` com `status IN ('OPEN','RESERVED')` por `exemplar_id`.
- **I-3**: `availability = UNAVAILABLE` **não** apaga a linha nem encerra negociação (FR-510, BR-506).
- **I-4**: concluir venda → `exemplar.status = SOLD`; troca → `TRADED`; cancelar **não** altera `exemplar.status` (BR-508).
- **I-5**: disponibilidade e negociação nunca alteram a composição de grupos de duplicados, que depende só de `exemplar.status = OWNED` por `variant_id`.
- **I-6**: nenhum campo opcional é preenchido com sentinela; ausência é `NULL` (I.9).
- **I-7**: toda escrita de oferta continua transacional e validando `status = OWNED` (FR-506/FR-513).

## 7. O que **não** muda

- `AppDatabase`, versão **4** e `MigrationTest`.
- Entidades `ExemplarEntity`, `DuplicateOfferEntity`, `NegotiationEntity`, `NegotiationEventEntity`.
- `DuplicateDao.observeDuplicates` (critério `HAVING COUNT(*) > 1` por `variant_id`).
- Formato do pacote de backup (`FileBackupService`) — as tabelas já são exportadas.
- `applicationId`, `app_name`, rotas de navegação.
