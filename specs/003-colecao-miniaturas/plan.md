# Implementation Plan: Miniatura dos carrinhos na tela Coleção

**Branch**: `main` (spec-kit sem branch própria; diretório `003-colecao-miniaturas`) | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-colecao-miniaturas/spec.md`

**Note**: plano preenchido a partir do spec aprovado e do código já existente. A maior parte
da feature estava implementada antes deste plano (commits `899ba85` e `32a09b4`); o plano
registra o desenho vigente, as decisões tomadas na análise e o delta aplicado.

## Summary

Exibir, em cada cartão da tela Coleção, a miniatura de uma fotografia já cadastrada da
variante, ao lado de nome, fabricante, ano e quantidade de exemplares, preservando busca,
atalhos, cadastro e navegação. A foto representativa vem na mesma consulta da listagem
(sem N+1), no mesmo componente de miniatura usado pelas demais listas, com marcador de
carrinho quando não há foto e com ampliação ao toque na imagem.

## Technical Context

**Language/Version**: Kotlin 2.1.0 (JVM 17 para o build; `jvmTarget` do projeto)

**Primary Dependencies**: Jetpack Compose (BOM do projeto) + Material 3, Coil 2.7.0
(`coil-compose`), Room 2.6.1, Hilt, Navigation Compose

**Storage**: Room/SQLite local; imagens em `filesDir/images/<exemplarId>/`, banco guarda só
caminho relativo (Constituição R.2)

**Testing**: JUnit4 + Robolectric/Truth no módulo `core:data`; Compose UI Test
(`createComposeRule`) em `app/src/androidTest`

**Target Platform**: Android `minSdk 26`, `compileSdk`/`targetSdk` 36

**Project Type**: aplicativo Android nativo, multi-módulo (`app`, `core:data`, `core:domain`,
`core:ui`)

**Performance Goals**: abrir a listagem em < 1 s com 500 variantes; rolagem fluida;
miniaturas carregadas sob demanda (SC-003)

**Constraints**: offline-first; nenhuma migração; nenhuma dependência nova; leitura de
arquivo fora da thread principal; foto só de caminho já existente

**Scale/Scope**: 10.000 exemplares / 50.000 fotos (R.7); uma tela e um componente
compartilhado

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Artigo / restrição | Como o plano atende | Status |
|---|---|---|
| I — Integridade dos dados | Projeção de leitura (`VariantSummary`); nenhuma escrita, exclusão ou migração; modelo → variante → exemplar preservado | PASSA |
| II — Modularidade | Regra de seleção na consulta do DAO; texto e idioma na apresentação (`CatalogScreen`); componente reutilizado `ItemThumbnail` | PASSA |
| III — Segurança e privacidade | Nenhum segredo, nenhuma exportação de dado pessoal; foto lida do armazenamento privado | PASSA |
| IV.5/IV.6 — Usabilidade e acessibilidade | Alvos ≥ 48 dp (cartão inteiro), descrição na foto e no marcador, tema escuro preservado, 360 dp | PASSA |
| V — Testes e verificabilidade | Testes de dados e teste Compose dos cartões; SC-003 **não** declarado atendido sem medição | PASSA com ressalva (L1) |
| VI.10/VI.11 — Offline e fonte de verdade local | Nenhuma chamada externa; caminho relativo + UUID existentes | PASSA |
| R.2 — Imagens fora do banco | `photo.path` relativo; arquivo em `filesDir` | PASSA |
| R.7 — Metas de volume/latência | Consulta única; carregamento sob demanda; medição pendente | PASSA com ressalva (L1) |
| R.8 — Migrações | Nenhuma alteração de esquema | PASSA |
| R.13/R.14 — Dependências e stack | Coil já presente; nenhuma dependência adicionada | PASSA |

Nenhuma violação que exija justificativa em Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/003-colecao-miniaturas/
├── spec.md
├── plan.md              # este arquivo
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── contracts/
│   └── collection-card.md
├── quickstart.md        # Phase 1
├── analysis.md          # auditoria spec ↔ código
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
app/src/main/java/br/com/diecastcollector/
├── feature/catalog/
│   ├── CatalogScreen.kt          # cartão, subtítulo, descrição da miniatura
│   └── CatalogViewModel.kt       # combine(observeSummaries, observeIncomplete)
├── navigation/AppNavHost.kt      # rotas preservadas (cartão → exemplar; atalho → lista)
└── ui/components/ItemThumbnail.kt # miniatura compartilhada (64 dp, fallback, ampliação)

app/src/main/res/values/strings.xml # rótulos pt-BR, incluindo «Sem foto do carrinho»

core/domain/src/main/kotlin/br/com/diecastcollector/domain/model/VariantSummary.kt
core/data/src/main/kotlin/br/com/diecastcollector/data/
├── db/dao/ItemDao.kt             # VariantDao.observeSummaries + subconsulta da foto
└── repository/SupportRepositories.kt # VariantRepositoryImpl.observeSummaries

core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/ # tema escuro

app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CatalogScreenTest.kt
core/data/src/test/kotlin/br/com/diecastcollector/data/repository/VariantRepositoryTest.kt
```

**Structure Decision**: manter a estrutura existente. A feature não cria módulo, tela,
tabela nem serviço; mexe em um cartão (`CatalogScreen`), no componente compartilhado de
miniatura, em uma consulta de leitura e em testes.

## Phase 0 — Research

Ver [research.md](./research.md). Decisões fechadas: origem e ordem da foto, carregamento
com Coil, fallback em erro, ausência de N+1, acessibilidade, limite de linhas do cartão e
as duas decisões de produto (foto de exemplar vendido; toque na miniatura amplia).

## Phase 1 — Design & Contracts

- [data-model.md](./data-model.md): projeção `VariantSummary`, entidades de origem e regras
  de derivação; sem migração.
- [contracts/collection-card.md](./contracts/collection-card.md): contrato visível do
  cartão (elementos, estados, rótulos, interações).
- [quickstart.md](./quickstart.md): comandos e roteiro de validação.

## Phase 2 — Implementation status

| Item | Arquivo | Estado |
|---|---|---|
| Miniatura à esquerda, 64 dp, cantos 10 dp | `CatalogScreen.kt`, `ItemThumbnail.kt` | Já existia |
| Foto representativa na consulta da listagem | `ItemDao.kt`, `SupportRepositories.kt` | Já existia |
| Marcador sem foto | `ItemThumbnail.kt` | Já existia |
| Fallback para arquivo inválido (D1) | `ItemThumbnail.kt` | Aplicado |
| Descrição do marcador (D2) | `ItemThumbnail.kt`, `strings.xml` | Aplicado |
| Limite de linhas do cartão (D4) | `CatalogScreen.kt` | Aplicado |
| Descrição composta modelo · cor · ano (D6) | `CatalogScreen.kt` | Aplicado |
| Testes de dados e de cartão | `VariantRepositoryTest`, `CatalogScreenTest` | Aplicado |
| Medição de SC-003 | — | **Pendente** |

## Re-check pós-design

Após o desenho, os gates continuam passando: nenhuma entidade nova, nenhum contrato externo
alterado, nenhuma dependência adicionada. O único ponto aberto é a medição de desempenho
(L1), que não bloqueia a correção funcional mas impede declarar SC-003 atendido (Art. V.8).

## Complexity Tracking

Sem violações. Nenhuma tabela de exceção necessária.
