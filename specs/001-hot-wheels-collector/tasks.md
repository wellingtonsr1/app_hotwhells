---
description: "Task list — Hot Wheels Collector (Android offline-first)"
---

# Tasks: Hot Wheels Collector — Aplicativo Android offline-first

**Input**: Design documents from `/specs/001-hot-wheels-collector/`

**Prerequisites**: [`plan.md`](./plan.md), [`spec.md`](./spec.md), [`research.md`](./research.md), [`data-model.md`](./data-model.md), [`contracts/`](./contracts/), [`quickstart.md`](./quickstart.md)

**Tests**: obrigatórios. A constituição v1.2.0 (Art. V.1–V.2) exige verificação automatizada e proíbe declarar conclusão por tela implementada. Cada história tem tarefas de teste antes da implementação.

**Organization**: uma fase por história de usuário, na ordem de prioridade da spec (P1 → P4), para que cada uma seja entregável e testável isoladamente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história da spec (`US1`…`US11`); setup, fundação e polish não levam rótulo
- Todo item traz o caminho de arquivo exato

## Path Conventions

- `app/src/main/java/br/com/hotwheelscollector/…` — UI, navegação, features, DI
- `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/…` — Kotlin puro (sem Android)
- `core/data/src/main/kotlin/br/com/hotwheelscollector/data/…` — Room, arquivos, import/export, ML Kit
- `core/ui/src/main/kotlin/br/com/hotwheelscollector/ui/…` — design system
- `core/data/src/test/kotlin/…` e `app/src/androidTest/java/…` — testes

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: esqueleto Gradle e ferramental do projeto

- [X] T001 Criar esqueleto multi-módulo Gradle (`:app`, `:core:domain`, `:core:data`, `:core:ui`) em `settings.gradle.kts` e `build.gradle.kts`, conforme `plan.md` (Structure Decision)
- [X] T002 Configurar Android Gradle Plugin e toolchain Kotlin 2.1/JVM 17 com `compileSdk 36` e `minSdk 26` em `app/build.gradle.kts`
- [X] T003 [P] Declarar dependências no version catalog (`compose BOM`, Material3, Navigation Compose, Hilt, Room 2.7 + KSP, Coil, ML Kit barcode/text, WorkManager) em `gradle/libs.versions.toml`
- [X] T004 [P] Configurar `HotWheelsApp` com `@HiltAndroidApp` em `app/src/main/java/br/com/hotwheelscollector/HotWheelsApp.kt`
- [X] T005 [P] Configurar lint Android, `detekt` e checagem de strings pt-BR em `build.gradle.kts`
- [X] T006 [P] Criar tema Material3 claro/escuro com paleta vermelho/laranja/preto/branco em `core/ui/src/main/kotlin/br/com/hotwheelscollector/ui/theme/Theme.kt`
- [X] T007 [P] Configurar exportação de schema Room (`exportSchema = true`, saída em `core/data/schemas/`) em `core/data/build.gradle.kts`
- [X] T008 [P] Definir regras de backup do sistema (incluir banco e preferências, excluir `images/`) em `app/src/main/res/xml/data_extraction_rules.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: banco, domínio e infraestrutura que **bloqueiam** todas as histórias

**⚠️ CRITICAL**: nenhuma história começa antes desta fase

- [X] T009 Criar `AppDatabase` v1 com todas as entidades de `data-model.md` e callback que executa `PRAGMA foreign_keys = ON` em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/db/AppDatabase.kt`
- [X] T010 Criar teste de migração v1 com `MigrationTestHelper` em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/db/MigrationTest.kt`
- [X] T011 [P] Implementar entidade `manufacturer` (`name` NOT NULL UNIQUE, `country` NULL) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/db/entity/ManufacturerEntity.kt`
- [X] T012 [P] Implementar entidades `brand`, `real_vehicle` e `color` (`name` NOT NULL UNIQUE em `color`) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/db/entity/`
- [X] T013 [P] Implementar `car_model` (`scale` NOT NULL DEFAULT `'1:64'`, único `(manufacturer_id, name, scale)`) em `core/data/.../db/entity/CarModelEntity.kt`
- [X] T014 [P] Implementar `variant` com `fingerprint` NOT NULL e índice único parcial `WHERE archived_at IS NULL` em `core/data/.../db/entity/VariantEntity.kt`
- [X] T015 [P] Implementar `exemplar` (`internal_code` NOT NULL UNIQUE, `condition` ∈ {SEALED, LOOSE, DAMAGED, RESTORED, INCOMPLETE}, `row_version` INTEGER DEFAULT 0) em `core/data/.../db/entity/ExemplarEntity.kt`
- [X] T016 [P] Implementar `photo` (`is_primary`, `sort_order`, `checksum` NOT NULL) em `core/data/.../db/entity/PhotoEntity.kt`
- [X] T017 [P] Implementar `location` e `movement` em `core/data/.../db/entity/`
- [X] T018 [P] Implementar `acquisition` com `price_cents`, `shipping_cents`, `discount_cents` INTEGER `>= 0` e `currency` DEFAULT `'BRL'` em `core/data/.../db/entity/AcquisitionEntity.kt`
- [X] T019 [P] Implementar `valuation` (`value_cents` >= 0, índice `(exemplar_id, date DESC)`) em `core/data/.../db/entity/ValuationEntity.kt`
- [X] T020 [P] Implementar `history_entry` appende-only em `core/data/.../db/entity/HistoryEntryEntity.kt`
- [X] T021 Implementar DAOs com consultas tipadas e `Flow` para as entidades de T011–T020 em `core/data/.../db/dao/`
- [X] T022 Criar modelos de domínio e mapeadores entidade↔domínio em `core/domain/.../domain/model/` e `core/data/.../mapper/`
- [X] T023 Criar interfaces de repositório em `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/repository/`
- [X] T024 [P] Implementar tipos `MoneyCents` e `IsoDate` com parsers pt-BR (vírgula e ponto) em `core/domain/.../domain/model/Money.kt`
- [X] T025 [P] Implementar `AuditLogWriter` e helper de transação Room em `core/data/.../db/AuditLogWriter.kt`
- [X] T026 [P] Implementar `ImageStore` (redimensionar, comprimir, remover EXIF, SHA-256, caminho `images/<exemplarId>/`) em `core/data/.../storage/ImageStore.kt`
- [X] T027 Mapear erros de domínio para mensagens pt-BR em `app/src/main/java/br/com/hotwheelscollector/ui/ErrorMessages.kt`
- [X] T028 Criar host de navegação com rotas tipadas e convenção de `SavedStateHandle` em `app/src/main/java/br/com/hotwheelscollector/navigation/AppNavHost.kt`

**Checkpoint**: fundação pronta — as histórias podem começar

### Evidências — Fase 2 (2026-11-16)

Validada em blocos, com compilação a cada bloco:

| Bloco | Tarefas | Comando | Resultado |
|---|---|---|---|
| 1. Domínio puro | T022 (modelos), T024 | `:core:domain:build` | `BUILD SUCCESSFUL in 13s` |
| 2. Entidades + banco | T011–T020, T009 | `:core:data:compileDebugKotlin` | `BUILD SUCCESSFUL in 1m 16s` (KSP gerou `schemas/1.json`) |
| 3. DAOs, auditoria, imagens | T021, T025, T026 | `:core:data:compileDebugKotlin` | `BUILD SUCCESSFUL in 23s` |
| 4. Mapeadores, repositórios, erros, navegação | T022, T023, T027, T028 | `:app:assembleDebug` | `BUILD SUCCESSFUL in 43s` |
| 5. Teste de schema/migração | T010 | `:core:data:testDebugUnitTest` | `tests="3" failures="0" errors="0"` |

Comando final de fechamento:

```text
$ ./gradlew :core:domain:build :core:data:testDebugUnitTest :app:assembleDebug :app:lint
BUILD SUCCESSFUL in 30s
EXIT=0

app/build/reports/lint-results-debug.txt:  No issues found.
core/data/build/test-results/testDebugUnitTest: tests=3 skipped=0 failures=0 errors=0
core/data/schemas/.../AppDatabase/1.json: exportado
APK: app/build/outputs/apk/debug/app-debug.apk — 82 MB
```

Testes que passaram: `schemaV1CriaTabelasDoModelo`, `callbackCriaIndiceUnicoParcialDeFingerprint`, `foreignKeysSaoAplicadas`.

**Descobertas da validação (registradas porque voltam a aparecer):**

1. `MigrationTestHelper` cria as tabelas a partir do JSON e **não executa callbacks** de `RoomDatabase` — por isso o índice parcial é verificado abrindo o banco real do Room, não pelo helper.
2. Índice único parcial não é expressável em `@Index`: o de `variant.fingerprint` (só ativos) é criado no callback de `onCreate`; os de `collection_item` e de negociação ativa ficam como verificação transacional, documentada no código.
3. `RoomDatabase` não resolve `.use` em Kotlin; os testes fecham o banco em `try/finally`.
4. Lint acusou strings de navegação sem uso e uma sugestão de KTX — ambos corrigidos; o estado final é `No issues found`.

---

## Phase 3: User Story 1 - Registrar exemplar de variante existente (Priority: P1) 🎯 MVP

**Goal**: adicionar um exemplar físico a uma variante já cadastrada em menos de 20 segundos, sem redigitar o carro.

**Independent Test**: com uma variante e um exemplar existentes, adicionar o segundo exemplar e verificar 1 modelo distinto / 2 exemplares, com formulário recuperado após morte do processo.

### Tests for User Story 1

- [X] T029 [P] [US1] Teste unitário de contagem (modelo distinto vs exemplares) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/AddExemplarUseCaseTest.kt`
- [X] T030 [P] [US1] Teste de integração da transação (exemplar + aquisição + movimentação + auditoria) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/ExemplarRepositoryTest.kt`
- [X] T031 [P] [US1] Teste Compose do fluxo rápido de exemplar em `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/AddExemplarScreenTest.kt`

### Implementation for User Story 1

- [X] T032 [US1] Implementar `AddExemplarUseCase` (transação única, `internal_code` gerado, `status=OWNED`) em `core/domain/.../domain/usecase/AddExemplarUseCase.kt`
- [X] T033 [US1] Implementar `ExemplarRepository.add` com auditoria e `ON DELETE RESTRICT` nas FKs em `core/data/.../repository/ExemplarRepositoryImpl.kt`
- [X] T034 [US1] Criar `AddExemplarViewModel` herdando dados da variante e usando `SavedStateHandle` para o rascunho em `app/.../feature/catalog/AddExemplarViewModel.kt`
- [X] T035 [US1] Criar `AddExemplarScreen` com apenas condição, localização, preço/data e observações editáveis em `app/.../feature/catalog/AddExemplarScreen.kt`
- [X] T036 [US1] Exibir indicador "N exemplares desta variante" ao salvar o segundo exemplar (FR-039, sem bloquear) em `app/.../feature/catalog/AddExemplarScreen.kt`

**Checkpoint**: US1 funcional sozinha — MVP mínimo

### Evidências — Fase 3 / US1 (2026-11-16)

| Bloco | Tarefas | Comando | Resultado |
|---|---|---|---|
| Regra de negócio + teste | T032, T029 | `:core:domain:test` | `tests=6 failures=0` |
| Repositório transacional + teste | T033, T030 | `:core:data:testDebugUnitTest` | `tests=5 failures=0` (módulo: 8) |
| ViewModel, tela, aviso de repetição | T034, T035, T036 | `:app:assembleDebug :app:lint` | `BUILD SUCCESSFUL`, `No issues found` |

Fechamento: `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:assembleDebug :app:lint`
→ `BUILD SUCCESSFUL`, 14 testes, 0 falhas, `No issues found`.

**Defeitos encontrados na execução:** `DomainError` precisou estender `Exception` para caber em `Result.failure`; o `SavedStateHandle` do ViewModel precisou ser propriedade (`private val`) para ser lido nos métodos; `first(flow)` não existe como função estática — é extensão de `Flow`.

**Lacuna de alcance — fechada em 2026-11-17:** o app não abre mais em placeholder. O dashboard passou a ser o **catálogo de variantes** (`CatalogScreen`), com contagem de exemplares vinda da consulta; tocar numa variante abre `exemplar/add/{variantId}`; o FAB abre o **cadastro mínimo de miniatura** (`CreateCatalogEntryScreen`), que cria fabricante/modelo/variante reaproveitando o que já existe e calcula o `fingerprint`. Sem esse degrau, US1 ficava inalcançável numa instalação nova. Continua aberto o cadastro completo de US2 (T041, T043, T044 e fotos).

**Correções desta rodada:**

1. **Bug de data real:** `IsoDate.parsePtBrOrNull` usava resolução *smart* do `DateTimeFormatter`, e `31/02/2026` era aceito virando 28/02. Passou a `dd/MM/uuuu` com `ResolverStyle.STRICT`, e o teste do ViewModel cobre o caso.
2. **Relógio único:** o `AddExemplarViewModel` recebe `Clock` e deriva a data padrão do mesmo relógio que valida a data; antes, tela e regra podiam discordar sobre "hoje" (o teste flagrou isso com 24h de diferença).
3. **Seletor de localização:** o campo de texto livre virou `ExposedDropdownMenuBox` alimentado por `LocationRepository`; texto digitado criava localização fantasma e furava FR-088.
4. **Código morto removido:** `AddExemplarForm` (nunca usado) saiu de `AddExemplarUseCase.kt`.

**Evidência:** `:core:domain:test` 6 testes, `:core:data:testDebugUnitTest` 8 testes, `:app:testDebugUnitTest` **13 testes** (todos do `AddExemplarViewModel`), `:app:assembleDebug` + `:app:lint` → `No issues found`. O teste instrumentado (T031 e o fluxo P1) roda no emulador — evidência no bloco seguinte.

**Verificado em emulador (2026-11-17):** emulador API 36 (x86_64, WHPX) criado como `hw_test`. O fluxo é um **teste instrumentado**, não script externo: `app/src/androidTest/java/br/com/hotwheelscollector/CatalogFlowTest.kt` sobe o `MainActivity` real (grafo Hilt, `Room` em arquivo, navegação e ViewModels de produção), apaga o banco antes da activity — o fluxo começa como numa instalação nova — e conversa com as telas pela semântica do Compose, o mesmo caminho do toque:

1. o catálogo abre vazio (`Nenhuma miniatura ainda`);
2. `+ Mini` abre `Nova miniatura`;
3. fabricante `Mattel` e modelo `Twin Mill` digitados nos campos reais;
4. salvar a miniatura navega para `Novo exemplar`;
5. salvar o exemplar volta ao catálogo, que mostra `Mattel · 1 exemplar`;
6. tocar no cartão da variante abre o cadastro de exemplar (alcance a partir da variante, confirmado);
7. o aviso `Esta variante já tem 1 exemplar(es).` aparece e o segundo salvamento leva a `2 exemplares`.

**Evidência:** `:app:connectedDebugAndroidTest` → `Starting 4 tests on hw_test`, `tests=4 failures=0 errors=0`, fluxo completo em 8,7 s; `:app:lint` → `BUILD SUCCESSFUL`. Capturas da mesma tela em `docs/e2e-1..7-*.png`, feitas na execução anterior à conversão; o script `tools/e2e_flow.py` foi removido.

**Filtro de ABI no release:** `ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }` apenas no `buildType` release — no debug isso quebraria o emulador, que é x86_64. Resultado medido: `app-release-unsigned.apk` de **63 MB para 30 MB**, sem `lib/x86` nem `lib/x86_64` no pacote.

**Continua não verificado:** o release é *unsigned* (falta keystore, T136) e não foi executado em aparelho ARM — só compilado e inspecionado.


---

## Phase 4: User Story 2 - Cadastrar miniatura nova com fotos (Priority: P1)

**Goal**: cadastrar modelo, variante e exemplar do zero, com campos desconhecidos permitidos e até 8 ângulos de foto, funcionando em modo avião.

**Independent Test**: instalação vazia + modo avião → cadastrar modelo/variante/exemplar com duas fotos e encontrar o item na lista, marcado como incompleto se faltar campo.

### Tests for User Story 2

- [ ] T037 [P] [US2] Teste unitário de validação (escala `1:N`, condição, prioridade, campos desconhecidos como `NULL`) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/validation/CatalogValidationTest.kt`
- [ ] T038 [P] [US2] Teste do `ImageStore` (compressão, remoção de EXIF, checksum estável) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/ImageStoreTest.kt`
- [ ] T039 [P] [US2] Teste de criação de modelo duplicado (aviso, não bloqueio) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/CarModelRepositoryTest.kt`
- [ ] T040 [P] [US2] Teste Compose de cadastro completo com foto em `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/CreateCatalogEntryScreenTest.kt`

### Implementation for User Story 2

- [ ] T041 [US2] Implementar `CreateModelUseCase` (aviso de mesmo nome+fabricante, opção usar existente) em `core/domain/.../usecase/CreateModelUseCase.kt`
- [ ] T042 [US2] Implementar `CreateVariantUseCase` com cálculo de `fingerprint` e aviso de variante idêntica (FR-040) em `core/domain/.../usecase/CreateVariantUseCase.kt`
- [ ] T043 [US2] Implementar telas de cadastro de modelo e veículo real em `app/.../feature/catalog/ModelFormScreen.kt`
- [ ] T044 [US2] Implementar tela de cadastro de variante (cor principal, cores secundárias, rodas, base, decoração, série) em `app/.../feature/catalog/VariantFormScreen.kt`
- [ ] T045 [US2] Implementar captura por câmera e seleção via Photo Picker, com recusa de permissão tratada em `app/.../feature/catalog/PhotoCapture.kt`
- [ ] T046 [US2] Implementar grade de fotos por ângulo com definição de foto principal (transação única) em `app/.../feature/catalog/PhotoGrid.kt`
- [ ] T047 [US2] Implementar persistência de rascunho de cadastro no banco, recuperável após interrupção (FR-023) em `core/data/.../repository/DraftRepositoryImpl.kt`
- [ ] T048 [US2] Marcar cadastros incompletos e listá-los para complementação (FR-029) em `app/.../feature/catalog/IncompleteEntriesScreen.kt`
- [ ] T049 [US2] Implementar busca-assistida de modelo existente ao digitar (FR-024) em `app/.../feature/catalog/ModelAutocomplete.kt`

**Checkpoint**: US1 e US2 funcionam independentemente

---
## Phase 5: User Story 3 - Painel do estado da coleção (Priority: P2)

**Goal**: mostrar exemplares vs modelos distintos, investido vs estimado, últimas aquisições e coleções mais completas.

**Independent Test**: com dados de exemplo, comparar cada número do painel com contagem manual; todos coincidem.

### Tests for User Story 3

- [ ] T050 [P] [US3] Teste de agregações (exemplares possuídos, modelos distintos, investido, estimado, itens sem avaliação) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/DashboardQueryTest.kt`
- [ ] T051 [P] [US3] Teste Compose do painel verificando cartões separados e aviso de estimativa parcial em `app/src/androidTest/java/br/com/hotwheelscollector/feature/dashboard/DashboardScreenTest.kt`

### Implementation for User Story 3

- [ ] T052 [US3] Implementar `DashboardRepository` com as consultas derivadas de `data-model.md` (nada armazenado) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/repository/DashboardRepositoryImpl.kt`
- [ ] T053 [US3] Criar `DashboardViewModel` com estados de carregando, vazio e erro em `app/src/main/java/br/com/hotwheelscollector/feature/dashboard/DashboardViewModel.kt`
- [ ] T054 [US3] Criar `DashboardScreen` com os 11 indicadores, últimas aquisições e últimos cadastros (FR-001–FR-011) em `app/.../feature/dashboard/DashboardScreen.kt`
- [ ] T055 [US3] Implementar selo de estimativa com data, fonte e contagem de itens sem avaliação (FR-008, FR-086) em `app/.../feature/dashboard/EstimateBadge.kt`

**Checkpoint**: US3 entrega visão gerencial sem depender de relatórios

---

## Phase 6: User Story 4 - Busca, filtros e visualizações (Priority: P2)

**Goal**: encontrar "eu tenho esse?" e "onde está" com busca tolerante, 13 filtros combináveis e 6 formas de visualizar.

**Independent Test**: com 500 itens, buscar `skyline r34` acha o item; filtros combinados reduzem sem perder válidos.

### Tests for User Story 4

- [ ] T056 [P] [US4] Teste de FTS com acentos, caixa e ordem parcial (`Nissan Skyline GT-R (R34)` ← `skyline r34`) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/db/SearchIndexTest.kt`
- [ ] T057 [P] [US4] Teste de filtros combináveis (E entre categorias, múltipla escolha dentro da categoria) e contagem de resultados em `app/src/androidTest/java/br/com/hotwheelscollector/feature/search/SearchFiltersTest.kt`
- [ ] T058 [P] [US4] Teste de desempenho com 10.000 exemplares (busca < 300 ms, lista < 1 s) em `app/src/androidTest/java/br/com/hotwheelscollector/perf/SearchPerformanceTest.kt`

### Implementation for User Story 4

- [ ] T059 [US4] Migração v2 criando `search_index` FTS4 (`tokenizer=unicode61 remove_diacritics=1`) e gatilhos de sincronização em `core/data/.../db/migration/Migration_2_Search.kt`
- [ ] T060 [US4] Implementar `SearchRepository` com as 13 dimensões de filtro e ordenação por nome, ano, data de aquisição e valor (FR-116, FR-118) em `core/data/.../repository/SearchRepositoryImpl.kt`
- [ ] T061 [US4] Implementar estado de filtros com chips visíveis e ação de limpar (FR-119) em `app/.../feature/search/SearchFilterState.kt`
- [ ] T062 [US4] Implementar busca global com debounce e contagem de resultados (FR-114, FR-120) em `app/.../feature/search/SearchScreen.kt`
- [ ] T063 [US4] Implementar visualização em grade e em lista detalhada em `app/.../feature/search/CatalogViews.kt`
- [ ] T064 [US4] Implementar galeria fotográfica e agrupamentos por coleção, fabricante e série em `app/.../feature/search/GroupedCatalogViews.kt`

**Checkpoint**: US4 torna a coleção consultável; sem escrita de dados

---

## Phase 7: User Story 5 - Séries, coleções e progresso (Priority: P2)

**Goal**: acompanhar coleções com percentual, faltantes e variantes possuídas, sem presumir a taxonomia da Mattel.

**Independent Test**: coleção com 10 itens conhecidos e 6 possuídos mostra 60% e 4 faltantes.

### Tests for User Story 5

- [ ] T065 [P] [US5] Teste de progresso (`possuídos / (esperados − não colecionáveis)`) e lista de faltantes em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/CollectionProgressTest.kt`
- [ ] T066 [P] [US5] Teste Compose da tela de coleção (percentual, faltantes, adquiridos) em `app/src/androidTest/java/br/com/hotwheelscollector/feature/organization/CollectionDetailTest.kt`

### Implementation for User Story 5

- [ ] T067 [US5] Implementar entidades `series`, `collection`, `collection_item` com CHECK de exatamente um entre `variant_id`/`model_id` em `core/data/.../db/entity/OrganizationEntities.kt`
- [ ] T068 [US5] Migração v3 criando as três tabelas, hierarquias e índices únicos parciais em `core/data/.../db/migration/Migration_3_Organization.kt`
- [ ] T069 [US5] Implementar `CollectionProgressUseCase` e `MissingItemsUseCase` (FR-052–FR-054) em `core/domain/.../usecase/CollectionProgressUseCase.kt`
- [ ] T070 [US5] Implementar CRUD de série e coleção com hierarquia e proteção contra ciclo em `app/.../feature/organization/CollectionFormScreen.kt`
- [ ] T071 [US5] Implementar tela de coleção com percentual, faltantes e itens não colecionáveis em `app/.../feature/organization/CollectionDetailScreen.kt`
- [ ] T072 [US5] Implementar marcação "não colecionável" e renomear/reorganizar sem perder vínculos (FR-055, FR-056) em `app/.../feature/organization/CollectionItemsScreen.kt`

**Checkpoint**: US5 fecha o ciclo de "o que falta comprar"

---

## Phase 8: User Story 6 - Lista de desejos e conversão em aquisição (Priority: P3)

**Goal**: registrar o que se quer comprar e converter em aquisição sem perder o histórico do desejo.

**Independent Test**: converter desejo em aquisição e verificar data de criação, prioridade e valores originais preservados.

### Tests for User Story 6

- [ ] T073 [P] [US6] Teste de conversão transacional preservando prioridade, valor máximo e data de criação (FR-063) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/ConvertWishlistTest.kt`
- [ ] T074 [P] [US6] Teste de que desejo não entra em contagem de possuídos nem no patrimônio (FR-064) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/WishlistRepositoryTest.kt`

### Implementation for User Story 6

- [ ] T075 [US6] Implementar `wishlist_item` (CHECK `model_id OR variant_id OR free_text`, `priority` 1..3) e migração v4 em `core/data/.../db/entity/WishlistItemEntity.kt`
- [ ] T076 [US6] Implementar formulário de desejo (prioridade, valor máximo, encontrado, loja, link, observações) em `app/.../feature/wishlist/WishlistFormScreen.kt`
- [ ] T077 [US6] Implementar lista de desejos ordenada por prioridade e status em `app/.../feature/wishlist/WishlistScreen.kt`
- [ ] T078 [US6] Implementar conversão desejo→aquisição com vínculo ao exemplar criado ou existente em `app/.../feature/wishlist/ConvertToAcquisition.kt`
- [ ] T079 [US6] Implementar abertura de link de referência em app externo com validação `http`/`https` em `app/.../feature/wishlist/ReferenceLink.kt`

**Checkpoint**: US6 conecta intenção de compra ao financeiro

---

## Phase 9: User Story 7 - Duplicados e negociação (Priority: P3)

**Goal**: identificar duplicados por variante, marcar disponibilidade e registrar negociação até a conclusão.

**Independent Test**: 3 exemplares da mesma variante + 1 de outra variante do mesmo modelo → só os 3 aparecem como duplicados.

### Tests for User Story 7

- [ ] T080 [P] [US7] Teste de agrupamento de duplicados por `variant_id` (nunca por nome do modelo) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/DuplicateQueryTest.kt`
- [ ] T081 [P] [US7] Teste de índice parcial único: um exemplar não pode ter duas negociações ativas (FR-068) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/NegotiationRepositoryTest.kt`

### Implementation for User Story 7

- [ ] T082 [US7] Implementar `negotiation` e `negotiation_event` com índice parcial único em `OPEN`/`RESERVED` e migração v5 em `core/data/.../db/entity/NegotiationEntities.kt`
- [ ] T083 [US7] Implementar consulta `GetDuplicatesUseCase` agrupada por variante com contagem (FR-065) em `core/domain/.../usecase/GetDuplicatesUseCase.kt`
- [ ] T084 [US7] Implementar marcação de disponibilidade (troca, venda, ambos, indisponível) com condição e valor pretendido em `app/.../feature/duplicates/DuplicateActionsSheet.kt`
- [ ] T085 [US7] Implementar reserva com bloqueio de segunda negociação ativa em `app/.../feature/duplicates/NegotiationScreen.kt`
- [ ] T086 [US7] Implementar conclusão de venda (`status=SOLD` + evento financeiro com valor e data) em `core/domain/.../usecase/CloseSaleUseCase.kt`
- [ ] T087 [US7] Implementar conclusão de troca (saída do negociado + entrada do recebido) em `core/domain/.../usecase/CloseTradeUseCase.kt`
- [ ] T088 [US7] Implementar cancelamento que devolve estado anterior e mantém `negotiation_event` imutável (FR-072–FR-073) em `app/.../feature/duplicates/NegotiationHistoryScreen.kt`

**Checkpoint**: US7 evita vender o exemplar errado e mantém rastro da negociação

---

## Phase 10: User Story 8 - Financeiro e avaliações (Priority: P3)

**Goal**: registrar aquisições e avaliações, calcular investimento, custo médio e gastos por período/série/fabricante, sem apresentar estimativa como preço oficial.

**Independent Test**: cinco aquisições conhecidas → total, custo médio e gasto por série coincidem com cálculo manual (incluindo frete e desconto).

### Tests for User Story 8

- [ ] T089 [P] [US8] Teste de custo (`preço + frete − desconto`), total investido e custo médio com exclusão de itens sem valor (FR-075, FR-081) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/FinanceTotalsTest.kt`
- [ ] T090 [P] [US8] Teste de "avaliação mais recente = valor atual" e histórico completo preservado (FR-083–FR-084) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/ValuationRepositoryTest.kt`
- [ ] T091 [P] [US8] Teste Compose do fluxo financeiro com rótulo de estimativa visível em `app/src/androidTest/java/br/com/hotwheelscollector/feature/finance/FinanceScreenTest.kt`

### Implementation for User Story 8

- [ ] T092 [US8] Implementar formulário de aquisição (data, preço, frete, desconto, loja, vendedor, forma, moeda) em `app/.../feature/finance/AcquisitionFormScreen.kt`
- [ ] T093 [US8] Implementar tela de avaliação com valor, data, fonte e histórico de avaliações em `app/.../feature/finance/ValuationScreen.kt`
- [ ] T094 [US8] Implementar agregações por período, série, fabricante e coleção (FR-078–FR-080) em `core/data/.../repository/FinanceRepositoryImpl.kt`
- [ ] T095 [US8] Implementar tela de finanças com investido, estimado, diferença e custo médio (FR-077, FR-085) em `app/.../feature/finance/FinanceScreen.kt`
- [ ] T096 [US8] Implementar rótulo obrigatório "estimativa do colecionador" com fonte e data em toda exibição de valor estimado (FR-086) em `app/.../feature/finance/EstimateLabel.kt`
- [ ] T097 [US8] Implementar histórico financeiro separando vendas/trocas do investimento (FR-087) em `core/domain/.../usecase/FinancialHistoryUseCase.kt`
- [ ] T098 [US8] Implementar formatação pt-BR de moeda (centavos) e data centralizada em `core/ui/src/main/kotlin/br/com/hotwheelscollector/ui/format/Formatters.kt`

**Checkpoint**: US8 entrega o módulo de maior valor de longo prazo

---

## Phase 11: User Story 9 - Localização física e movimentação (Priority: P3)

**Goal**: saber onde cada exemplar está, com hierarquia criada pelo usuário e histórico de movimentação.

**Independent Test**: atribuir exemplar a "Caixa 12", mover para "Estante A > Prateleira 3", ver as duas posições no histórico com datas.

### Tests for User Story 9

- [ ] T099 [P] [US9] Teste de hierarquia (ciclo impedido) e movimentação atômica (localização + histórico juntos) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/LocationTreeTest.kt`
- [ ] T100 [P] [US9] Teste de exclusão bloqueada quando há exemplares na localização (FR-093) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/LocationRepositoryTest.kt`

### Implementation for User Story 9

- [ ] T101 [US9] Implementar `location` (tipos ENVIRONMENT…CUSTOM) e `movement` + migração v6 em `core/data/.../db/entity/LocationEntities.kt`
- [ ] T102 [US9] Implementar árvore de localização com criação de níveis e nomes personalizados em `app/.../feature/locations/LocationTreeScreen.kt`
- [ ] T103 [US9] Implementar atribuição e movimentação com histórico em transação única (FR-090–FR-091) em `app/.../feature/locations/MoveExemplarSheet.kt`
- [ ] T104 [US9] Implementar diálogo de exclusão com decisão explícita (mover ou arquivar), sem cascata em `app/.../feature/locations/DeleteLocationDialog.kt`
- [ ] T105 [US9] Implementar consulta e filtro por localização em qualquer nível da hierarquia (FR-094) em `app/.../feature/locations/LocationContentsScreen.kt`
- [ ] T106 [US9] Implementar inventário por localização (FR-095) em `app/.../feature/locations/LocationInventoryScreen.kt`

**Checkpoint**: US9 resolve "não sei onde guardei"

---
## Phase 12: User Story 10 - Relatórios, importação, exportação e backup (Priority: P4)

**Goal**: extrair relatórios em CSV/XLSX/PDF gerados no aparelho e garantir que a coleção sobrevive à troca de celular.

**Independent Test**: gerar backup, apagar os dados do app, restaurar e verificar contagens e fotos vinculadas; gerar PDF em modo avião.

### Tests for User Story 10

- [ ] T107 [P] [US10] Teste de contrato do CSV de importação (colunas, enums, datas, `E001`–`E008`) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/CsvImportContractTest.kt`
- [ ] T108 [P] [US10] Teste de contrato do pacote de backup (layout, `manifest.json`, SHA-256 por arquivo) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/BackupPackageContractTest.kt`
- [ ] T109 [P] [US10] Teste de formato dos relatórios: PDF (páginas, cabeçalho, totais) e XLSX aberto por leitor independente em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/ReportFormatTest.kt`
- [ ] T110 [P] [US10] Teste de ida e volta backup→restauração em banco novo com fotos e relacionamentos (SC-004) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/RestoreRoundTripTest.kt`

### Implementation for User Story 10

- [ ] T111 [US10] Implementar leitor CSV (UTF-8 com/sem BOM, delimitador `,`/`;` detectado, cabeçalho obrigatório) em `core/data/.../importexport/CsvReader.kt`
- [ ] T112 [US10] Implementar leitor XLSX (primeira aba, abas extras ignoradas com aviso) em `core/data/.../importexport/XlsxReader.kt`
- [ ] T113 [US10] Implementar pipeline de importação nas 7 fases do contrato com aplicação transacional por lote (FR-136–FR-139) em `core/domain/.../usecase/ImportDataUseCase.kt`
- [ ] T114 [US10] Implementar relatório de erros por linha com códigos `E001`–`E008` em `core/data/.../importexport/ImportErrorReport.kt`
- [ ] T115 [US10] Implementar tela de importação com pré-visualização, contagens e escolha por duplicata (pular/criar/mesclar) em `app/.../feature/transfer/ImportScreen.kt`
- [ ] T116 [US10] Implementar escritores CSV e XLSX (OOXML por `ZipOutputStream`) com nome de arquivo datado em `core/data/.../importexport/ReportWriters.kt`
- [ ] T117 [US10] Implementar gerador de PDF com `PdfDocument`, cabeçalho, filtros, totais e paginação (FR-126–FR-128) em `core/data/.../importexport/PdfReportWriter.kt`
- [ ] T118 [US10] Implementar tela de relatórios com os 11 tipos, progresso e cancelamento seguro (FR-123–FR-132) em `app/.../feature/reports/ReportsScreen.kt`
- [ ] T119 [US10] Implementar `BackupService` (manifesto, hashes, streaming) e restauração com validação prévia e modos substituir/mesclar em `core/data/.../storage/BackupService.kt`
- [ ] T120 [US10] Implementar tela de backup/restauração com lembrete por intervalo e aviso de cota do Auto Backup em `app/.../feature/transfer/BackupScreen.kt`

**Checkpoint**: US10 garante que a coleção não fica presa ao app

---

## Phase 13: User Story 11 - Reconhecimento assistido por foto e código de barras (Priority: P4)

**Goal**: sugerir dados por OCR e código de barras, sempre com aceite humano e "não identificado" quando inconclusivo.

**Independent Test**: foto de item já cadastrado → sugestões; recusar todas mantém o cadastro exatamente como estava.

### Tests for User Story 11

- [ ] T121 [P] [US11] Teste de que nenhuma sugestão grava dado sem aceite explícito (SC-010) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/RecognitionProposalTest.kt`
- [ ] T122 [P] [US11] Teste de "não identificado" quando a confiança fica abaixo do limiar, sem preenchimento por inferência (FR-105) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/recognition/RecognitionProviderTest.kt`

### Implementation for User Story 11

- [ ] T123 [US11] Definir `RecognitionProvider` (sugestões com confiança e origem, sem efeito colateral) em `core/domain/.../domain/recognition/RecognitionProvider.kt`
- [ ] T124 [US11] Implementar provedor ML Kit `barcode-scanning` on-device em `core/data/.../recognition/MlKitBarcodeProvider.kt`
- [ ] T125 [US11] Implementar provedor ML Kit `text-recognition` (OCR de embalagem) on-device em `core/data/.../recognition/MlKitTextProvider.kt`
- [ ] T126 [US11] Implementar busca de correspondências no catálogo local (até 5, ordenadas por confiança) em `core/domain/.../usecase/FindCatalogMatchesUseCase.kt`
- [ ] T127 [US11] Implementar tela de revisão com aceite/rejeição campo a campo (FR-104) em `app/.../feature/recognition/RecognitionReviewSheet.kt`
- [ ] T128 [US11] Implementar fluxo de código de barras: correspondência, conferência, aproveitar dados e guardar código sem criar exemplar (FR-110–FR-113) em `app/.../feature/recognition/BarcodeScanScreen.kt`
- [ ] T129 [US11] Persistir `recognition_result` com `applied_fields` e garantir degradação sem permissão de câmera (FR-108) em `app/.../feature/recognition/RecognitionViewModel.kt`

**Checkpoint**: US11 reduz atrito de cadastro sem violar Art. VI.4

---

## Phase 14: Polish & Cross-Cutting Concerns

**Purpose**: requisitos transversais que atravessam todas as histórias

- [ ] T130 [P] Auditar acessibilidade (TalkBack, alvos 48 dp, fonte do sistema até 200%, contraste 4.5:1) e corrigir desvios em `app/src/main/res/values/dimens.xml`
- [ ] T131 [P] Externalizar 100% das strings em pt-BR e remover texto fixo de Composable, com lint limpo em `app/src/main/res/values/strings.xml`
- [ ] T132 [P] Implementar política de privacidade offline dentro do app (FR-172) em `app/.../feature/settings/PrivacyPolicyScreen.kt`
- [ ] T133 [P] Implementar tela "Sobre" com versão, data de build e identificador do pacote (FR-171) em `app/.../feature/settings/AboutScreen.kt`
- [ ] T134 Implementar `WorkManager` para importação, backup e restauração com progresso e retomada após segundo plano (FR-150) em `app/.../work/TransferWorkers.kt`
- [ ] T135 Otimizar desempenho: índices faltantes, consultas N+1, Baseline Profile e startup < 2 s em `app/src/main/baseline-prof.txt`
- [ ] T136 [P] Configurar assinatura de release, AAB e `keystore.properties` fora do versionamento (R.15) em `app/build.gradle.kts`
- [ ] T137 [P] Escrever documentação operacional (instalação, backup, restauração, solução de problemas) em `docs/operacao.md`
- [ ] T138 Executar os 14 cenários do `quickstart.md` no aparelho e registrar evidências em `docs/validacao.md`, com verificação explícita e assinada de SC-003, SC-005, SC-009, SC-011, SC-012 e SC-014
- [ ] T139 Revisar segurança e privacidade: zero chamadas de rede, nenhum segredo, permissões mínimas, nenhuma telemetria (Art. III) em `docs/revisao-seguranca.md`

---

## Phase 15: Remediação dos achados CRITICAL do `/speckit-analyze` (G5)

**Purpose**: fechar as lacunas que a análise cruzada apontou como violação de princípio constitucional. Não renumeram tarefas existentes.

- [ ] T140 Implementar `AppLogger` com log estruturado (nível, timestamp UTC, identificador de correlação por operação, sem dado sensível) integrado ao handler global de erros e exportável no diagnóstico (Art. V.7) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/logging/AppLogger.kt`
- [ ] T141 [US2] Implementar exclusão e arquivamento de exemplar/variante com confirmação que descreve o efeito, bloqueio quando há dependentes (`ON DELETE RESTRICT`) e oferta de arquivamento como alternativa (FR-028, Art. I.6) em `app/src/main/java/br/com/hotwheelscollector/feature/catalog/DeleteOrArchiveSheet.kt`
- [ ] T142 [P] [US2] Teste de bloqueio de exclusão com dependentes e de arquivamento preservando histórico (Art. I.6) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/DeletionGuardTest.kt`
- [ ] T143 [US2] Implementar `MergeVariantsUseCase`: mover todos os exemplares, itens de coleção e desejos para a variante de destino, recalcular `fingerprint`, registrar `history_entry` e arquivar a origem sem apagar nada (FR-041) em `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/usecase/MergeVariantsUseCase.kt`
- [ ] T144 [US2] Implementar `SplitVariantUseCase`: criar nova variante a partir dos exemplares selecionados preservando aquisição, avaliações, fotos e movimentações (FR-042) em `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/usecase/SplitVariantUseCase.kt`
- [ ] T145 [P] [US2] Teste de domínio de união e separação de variantes com múltiplos exemplares e históricos (Art. I.3, V.3) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/VariantMergeSplitTest.kt`
- [ ] T146 Implementar exportação integral em formato aberto (JSON + CSV + imagens) e exclusão total dos dados do usuário com dupla confirmação e oferta de exportação prévia (FR-142, FR-157, FR-163, Art. III.7) em `app/src/main/java/br/com/hotwheelscollector/feature/settings/DataControlScreen.kt`
- [ ] T147 [P] Registrar todas as dependências externas com finalidade, versão, licença, custo e plano de substituição (R.5) em `docs/dependencias.md`
- [ ] T148 Configurar verificação de vulnerabilidades de dependências no build e bloquear release com vulnerabilidade crítica sem correção disponível (Art. III.10) em `build.gradle.kts` e `docs/revisao-dependencias.md`
- [ ] T149 Testar cadastro com foto em aparelho sem espaço livre: erro acionável, cadastro preservado e nenhuma foto parcial gravada (FR-153) em `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/StorageFullTest.kt`
- [ ] T150 Criar a matriz SC→teste cobrindo os 14 critérios de sucesso, com a tarefa ou teste responsável por cada um, e falhar o build quando algum SC ficar órfão (FR-133, Art. V.1–V.2) em `docs/matriz-sc-testes.md`
- [ ] T151 Testar importação de 1.000 linhas com pré-visualização completa e relatório de erro por linha dentro do tempo aceitável (SC-006) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/ImportScaleTest.kt`
- [ ] T152 Testar geração de PDF de 1.000 exemplares em menos de 10 segundos, com totais iguais aos do painel (SC-007) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/PdfPerformanceTest.kt`
- [ ] T153 Testar restauração completa de 1.000 exemplares e 2.000 fotos em menos de 5 minutos, com vínculos de imagem preservados (SC-013) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/RestorePerformanceTest.kt`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — começa imediatamente
- **Foundational (Phase 2)**: depende do Setup — **bloqueia todas as histórias**
- **User Stories (Phases 3–13)**: dependem da Fase 2; podem seguir em paralelo ou em ordem de prioridade
- **Polish (Phase 14)**: depende das histórias que se pretende entregar
- **Remediação G5 (Phase 15)**: depende de US2 e US10; MUST concluir antes de qualquer release
- **Nota de ordenação (H6)**: US6 executa antes de US8 por prioridade de produto, mas `acquisition` já existe desde a fundação (T018); nenhuma tarefa de US6 importa tela, ViewModel ou rota de US8

### User Story Dependencies

| História | Prioridade | Depende de | Observação |
|---|---|---|---|
| US1 | P1 | Fundação | entrega o MVP mínimo |
| US2 | P1 | Fundação | independente de US1, compartilha entidades de catálogo |
| US3 | P2 | US1, US2 (dados) | só leitura; sem escrita própria |
| US4 | P2 | US2 | FTS alimentado por modelo/variante |
| US5 | P2 | US2 | usa `collection_item` e variantes |
| US6 | P3 | US5 + entidade `acquisition` da fundação (T018) | a conversão grava `acquisition` direto no repositório; não depende de nenhuma tela de US8 |
| US7 | P3 | US1, US2 | duplicados exigem variante+exemplar |
| US8 | P3 | US1, US6 | aquisição é pré-requisito do financeiro |
| US9 | P3 | US1 | localização aplica-se ao exemplar |
| US10 | P4 | todas | exporta o que existe |
| US11 | P4 | US2, US5 | sugestões comparam com catálogo |

### Within Each User Story

- Testes primeiro, falhando, antes da implementação (Art. V.1)
- Entidades e migração antes de repositório; repositório antes de use case; use case antes de tela
- Migração nova só entra depois de `MigrationTestHelper` cobrindo a versão

### Parallel Opportunities

- Fase 1: T003–T008 em paralelo após T001/T002
- Fase 2: T011–T020 (entidades) em paralelo; T024–T026 em paralelo
- Fase 3 em diante: cada fase tem 2–4 tarefas de teste marcadas `[P]` para rodar juntas
- US3, US4, US5, US9 podem rodar em paralelo depois da fundação, por não compartilharem arquivos de escrita
- US10 e US11 não devem rodar em paralelo com migrações abertas de outras histórias (conflito em `AppDatabase`)

---

## Parallel Example: User Story 2

```text
# Testes em paralelo (arquivos distintos):
T037 validação de catálogo em core/domain/src/test/.../CatalogValidationTest.kt
T038 ImageStore em core/data/src/test/.../ImageStoreTest.kt
T039 modelo duplicado em core/data/src/test/.../CarModelRepositoryTest.kt
T040 fluxo Compose de cadastro em app/src/androidTest/.../CreateCatalogEntryScreenTest.kt

# Depois, implementação sequencial de formulários:
T043 modelo → T044 variante → T045 câmera/Photo Picker → T046 grade de fotos
```

---

## Implementation Strategy

### MVP First (US1 apenas)

1. Fase 1 (Setup) + Fase 2 (Fundação) — obrigatórias
2. Fase 3 (US1) — adicionar exemplar a variante existente
3. **Parar e validar**: SC-001 (20 s) e SC-008 (sem perda de formulário)
4. Só então avançar

### Incremental Delivery

1. Fundação pronta → app abre, banco íntegro, migração v1 testada
2. +US1 +US2 → catálogo utilizável (cadastro e fotos) → primeiro APK instalável de uso real
3. +US3 +US4 +US5 → coleção consultável e com progresso
4. +US6 +US7 +US8 +US9 → gestão completa (desejos, negociações, dinheiro, localização)
5. +US10 → relatórios e backup: **só aqui a coleção está protegida**
6. +US11 → reconhecimento assistido
7. Fase 14 → acessibilidade, desempenho, assinatura e validação final

### Regras de parada (herdadas da constituição)

- Nenhuma fase é "concluída" com teste vermelho ou pulado (Art. V.4)
- Nenhuma migração destrutiva, em nenhuma hipótese (Art. I.10, R.8)
- Nenhuma tarefa fora do escopo da fase entra no mesmo commit (Art. V, proibições)
- Ao final de cada fase: listar arquivos alterados, testes executados, resultado e limitações conhecidas

---

## Notes

- `[P]` = arquivos diferentes, sem dependência pendente
- `[USn]` mapeia a tarefa à história da spec para rastreabilidade
- Tarefas T130–T139 (transversais) e T140, T146, T147, T148 (remediação G5) não pertencem a uma história: são exigidas pela constituição e pela análise cruzada
- Evitar: tarefas vagas, dois agentes no mesmo arquivo, dependência cruzada que quebre a independência de uma história

---

## Evidências — Fase 1 verificada (2026-11-16)

**Toolchain instalada em diretório de usuário, sem elevação:**

| Componente | Versão | Caminho |
|---|---|---|
| JDK (Temurin) | 17.0.21+9 | `%LOCALAPPDATA%\Android\Jdk\jdk-17.0.21+9` |
| Gradle | 8.11.1 | `%LOCALAPPDATA%\Android\Gradle\gradle-8.11.1` (+ wrapper no projeto) |
| Android SDK | platform 36, build-tools 36.0.0, platform-tools | `%LOCALAPPDATA%\Android\Sdk` |

**Saídas dos comandos:**

```text
$ ./gradlew :app:assembleDebug :app:lint
BUILD SUCCESSFUL in 10s
EXIT=0

app/build/reports/lint-results-debug.txt:
No issues found.        (0 errors, 0 warnings)

APK: app/build/outputs/apk/debug/app-debug.apk — 81 MB (debug)
```

**Correções exigidas pela validação:**

1. AGP `8.7.3` → `8.9.2`: o `compileSdk 36` não é suportado por AGP 8.7.x.
2. Regras de backup: `FullBackupContent` recusou `<exclude domain="file" path="images"/>` sem um `include` do mesmo domínio — corrigido em `backup_rules.xml` e `data_extraction_rules.xml` (3 erros de lint).
3. `android:label` redundante removido da `MainActivity` (aviso `RedundantLabel`).

**Ícone do aplicativo (antecipado de T136):** ícone adaptativo próprio em `res/mipmap-anydpi` — `ic_launcher` e `ic_launcher_round` apontados no manifesto via `android:icon` e `android:roundIcon`, mais variante `monochrome` para ícones temáticos (Android 13+). Fundo FlameRed `#D3242B`, silhueta da miniatura em Chalk `#F4F1EE` e faixa HotOrange `#F26B21`; desenho vetorial próprio, sem ativo de terceiros (A9). O `aapt2 dump badging` confirma o ícone empacotado em `res/mipmap-anydpi-v21/ic_launcher.xml`.

Revisão visual em `docs/icone-preview.html` (máscara circular, squircle, 48/72/96 px, monochrome): a silhueta não é cortada pela máscara (ponto mais distante do centro a 28dp, contra 33dp de raio seguro), mas o desenho estava 5dp abaixo do centro — corrigido com `translateY="-5"` no grupo dos dois vetores. Limite assumido: a faixa laranja tem 2dp e só aparece de 72px para cima; em 48px ela cai abaixo de 1 pixel.

**Armadilha de ambiente encontrada:** a pasta precisou nascer como `mipmap-anydpi` (o qualificador `-v26` é redundante com `minSdk 26` e o lint reprova com `ObsoleteSdkInt`), mas o merge incremental do AGP e o build cache local continuaram servindo a saída antiga e vazia — só passou com `--stop` nos daemons, remoção de `build/` e `caches/build-cache-1`, e build com `--no-build-cache`.
