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

**Lacuna de alcance — fechada em 2026-11-17:** o app não abre mais em placeholder. O dashboard passou a ser o **catálogo de variantes** (`CatalogScreen`), com contagem de exemplares vinda da consulta; tocar numa variante abre `exemplar/add/{variantId}`; o FAB abre o **cadastro mínimo de miniatura** (`CreateCatalogEntryScreen`), que cria fabricante/modelo/variante reaproveitando o que já existe e calcula o `fingerprint`. Sem esse degrau, US1 ficava inalcançável numa instalação nova. Continua aberto o cadastro completo de US2 (T041, T043, T044 e fotos). **Em 2026-11-18** a rota `dashboard` deixou de apontar para o catálogo: ela é o painel (US3) e o acervo passou para `catalog`, alcançável pela barra inferior — ver «Evidências — Fase 5 / US3». **Atualização de 2026-11-17:** T041, T044 e as fotos foram implementados; T043 foi aposentada — ver o bloco «Situação real — Fase 4 / US2».

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

- [X] T037 [P] [US2] Teste unitário de validação (escala `1:N`, condição, prioridade, campos desconhecidos como `NULL`) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/validation/CatalogValidationTest.kt`
- [X] T038 [P] [US2] Teste do `ImageStore` (compressão, remoção de EXIF, checksum estável) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/ImageStoreTest.kt`
- [X] T039 [P] [US2] Teste de criação de modelo duplicado (aviso, não bloqueio) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/CarModelRepositoryTest.kt`
- [X] T040 [P] [US2] Teste Compose de cadastro completo com foto em `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/CreateCatalogEntryScreenTest.kt`

### Implementation for User Story 2

- [X] T041 [US2] Implementar `CreateModelUseCase` (aviso de mesmo nome+fabricante, opção usar existente) em `core/domain/.../usecase/CreateModelUseCase.kt`
- [X] T042 [US2] Implementar `CreateVariantUseCase` com cálculo de `fingerprint` e aviso de variante idêntica (FR-040) em `core/domain/.../usecase/CreateVariantUseCase.kt`
- [X] T043 [US2] Implementar telas de cadastro de modelo e veículo real em `app/.../feature/catalog/ModelFormScreen.kt` — o cadastro inicial do modelo segue no fluxo do `+ Mini`; a tela nova edita o modelo e registra/vincula o **veículo real**
- [X] T044 [US2] Implementar tela de cadastro de variante (cor principal, cores secundárias, rodas, base, decoração, série) em `app/.../feature/catalog/VariantFormScreen.kt`
- [X] T045 [US2] Implementar captura por câmera e seleção via Photo Picker, com recusa de permissão tratada em `app/.../feature/catalog/PhotoCapture.kt`
- [X] T046 [US2] Implementar grade de fotos por ângulo com definição de foto principal (transação única) em `app/.../feature/catalog/PhotoGrid.kt`
- [X] T047 [US2] Implementar persistência de rascunho de cadastro no banco, recuperável após interrupção (FR-023) em `core/data/.../repository/DraftRepositoryImpl.kt`
- [X] T048 [US2] Marcar cadastros incompletos e listá-los para complementação (FR-029) em `app/.../feature/catalog/IncompleteEntriesScreen.kt`
- [X] T049 [US2] Implementar busca-assistida de modelo existente ao digitar (FR-024) em `app/.../feature/catalog/Autocomplete.kt`

**Checkpoint**: US1 e US2 funcionam independentemente

### Situação real — Fase 4 / US2 (levantada em 2026-11-17, atualizada em 2026-11-18)

**A fase avançou em 2026-11-18:** o módulo `:app` compila, e T037–T039, T041, T042,
T044, T047 e T049 têm teste verde (ver «Evidências — Fase 4 / US2 (2026-11-18)»).
T040, T045, T046 e T048 também ficaram verdes: os testes instrumentados rodam no emulador
API 36 desde 2026-11-18. **T043 foi concluída em 2026-11-20:** o cadastro inicial do modelo
continua no fluxo do `+ Mini`, e a parte que faltava — editar o modelo e cadastrar/vincular
o **veículo real** — ganhou tela própria, alcançada por «Editar modelo» em Incompletos. A
tabela distingue *artefato pronto* de *execução verificada*.

| Tarefa | Estado em 2026-11-18 | Arquivo / observação |
|---|---|---|
| T037 | **verde** — 10 testes, 0 falhas | `core/domain/.../validation/CatalogValidationTest.kt` |
| T038 | **verde** — 5 testes, 0 falhas | `core/data/.../storage/ImageStoreTest.kt` |
| T039 | **verde** — 5 testes, 0 falhas | `core/data/.../repository/CarModelRepositoryTest.kt` |
| T040 | **verde** — 1 teste, 0 falhas, 7,4 s no emulador API 36 | `CreateCatalogEntryScreenTest.kt` (ver «Evidências — T040») |
| T041 | **verde** — compila; `CreateModelUseCaseTest` 4 testes, 0 falhas | `CreateModelUseCase` |
| T042 | **verde** — compila; `CreateVariantUseCaseTest` 4 testes, 0 falhas | `CreateVariantUseCase` |
| T043 | **verde (2026-11-20)** — 3 testes de ViewModel, 1 teste de tela e 3 de repositório | `ModelFormScreen` + `ModelFormViewModel`; `CarModelRepository.update` e `RealVehicleRepositoryImpl` (ver «Evidências — T043») |
| T044 | **verde** — compila; `VariantFormViewModelTest` 7 testes, 0 falhas | `VariantFormScreen` + `VariantFormViewModel` |
| T045 | **verde** — 2 testes (captura e recusa), 0 falhas | `PhotoCapture` + `PhotoCaptureTest` (ver «Evidências — T045 e T046») |
| T046 | **verde** — 3 testes de grade + `PhotoRepositoryTest.setPrimary`, 0 falhas | `PhotoGrid` + `PhotoGridTest` |
| T047 | **verde** — 3 testes, 0 falhas | `DraftRepositoryImpl` (+ `DraftRepositoryTest`) |
| T048 | **verde** — 2 testes de app + 6 de dados, 0 falhas | `CarModelRepository.observeIncomplete` (modelo sem variante + variante sem cor/ano) e `IncompleteEntriesTest` (ver «Evidências — T048») |
| T049 | **verde** — compila; `AutocompleteTest` 5 testes, 0 falhas | `Autocomplete.kt`: `ModelAutocomplete` e `ManufacturerAutocomplete` ligados ao `CreateCatalogEntryScreen` |

### Evidências — variante incompleta editável (2026-11-20)

Antes: a variante sem cor principal ou sem ano aparecia em Cadastros incompletos, mas o
cartão não levava a lugar nenhum — completar exigia editar o banco à mão.

- `UpdateVariantUseCase` (domínio): carrega a variante, valida o ano, recalcula o
  `fingerprint` com `CreateVariantUseCase.calculateFingerprint` (mesma regra da criação) e
  recusa a edição quando ela tornaria a variante idêntica a outra ativa
  (`DomainError.Duplicate`). `id`, `createdAt` e os exemplares ficam intactos (Art. I.4).
- `VariantRepository.update` + `VariantRepositoryImpl`: grava a variante e regrava as cores
  secundárias como conjunto, em transação, registrando `updated` na auditoria. O
  `fingerprint` recalculado passa a ser gravado também na criação das secundárias — antes
  elas eram lidas mas nunca persistidas (correção da mesma regra, Art. II.5).
- `VariantFormViewModel`: modo edição (`loadForEdit`, `editing`, `loading`), sem criar
  exemplar nem variante; `VariantFormScreen` teve o conteúdo extraído e é reusado por
  `VariantEditScreen`, que esconde condição e fotos.
- Navegação: `Destination.VariantEdit` (`catalog/variant/{variantId}`); o cartão de
  variante incompleta ganhou «Editar variante», e a lista continua derivada — a linha sai
  quando cor e ano são preenchidos.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Repositório e caso de uso | `./gradlew :core:data:testDebugUnitTest --tests '*VariantRepositoryTest'` | 5 testes, 0 falhas (novo arquivo) |
| ViewModel (criação + edição) | `./gradlew :app:testDebugUnitTest --tests '*VariantFormViewModelTest'` | 10 testes, 0 falhas (eram 7) |
| Tela de edição | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*VariantEditScreenTest` | 1 teste, 0 falhas |
| Regressão unitária e lint | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 192 testes, 0 falhas; `0 errors, 7 warnings` |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 46 testes, 0 falhas (4m 49s) |

**Sem migração:** `variant` já tinha `primary_color_id` e `release_year`; o formato do
backup não mudou. **Limite conhecido:** a edição cobre os campos da tela (cor, cores
secundárias, ano, rodas, base, decoração, série e código do fabricante); números de
coleção/série, edição e embalagem continuam preservados do registro, sem campo na tela.

**Retorno para a lista (2026-11-20):** a tela de edição sai no mesmo instante em que salva,
então a confirmação é exibida pela própria lista de incompletos, via `SavedStateHandle` da
entrada de navegação: «Variante completada.» ou «Modelo atualizado.». Essa entrega também
expôs um **bug de rota**: `VariantEdit` usava o mesmo caminho de `VariantForm`
(`catalog/variant/{id}`), e o NavHost resolvia «Editar variante» para a tela de **criação**.
A rota passou a ser `catalog/variant/{variantId}/edit`, e o teste instrumentado cobre o
fluxo real (abrir, preencher cor e ano, salvar, ver a confirmação e a linha sair).

### Evidências — T043 (2026-11-20)

O que faltava: o modelo podia ser criado, mas não **editado**, e o veículo real não tinha
repositório, tela nem vínculo — `car_model.real_vehicle_id` ficava sempre `NULL`.

- `CarModelRepository.update` (domínio + `CarModelRepositoryImpl`) grava nome, escala,
  carroceria, material, país, observações e `realVehicleId`, recusando escala fora de `1:N`
  e identidade repetida no mesmo fabricante.
- `RealVehicleRepositoryImpl` implementa o repositório que só existia como interface
  (estava sem binding no Hilt), exigindo marca/modelo e recusando ano implausível.
- `ModelFormScreen`/`ModelFormViewModel`: campos do modelo, chips com os veículos reais
  existentes (ou «Nenhum») e bloco «Novo veículo real» — um veículo digitado é criado e
  vinculado no mesmo «Salvar», sem cadastro órfão. Entrada: «Editar modelo» no cartão de
  Incompletos, apenas para modelo sem variante.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Repositório de modelo e veículo real | `./gradlew :core:data:testDebugUnitTest --tests '*CarModelRepositoryTest'` | 9 testes, 0 falhas (eram 6) |
| ViewModel | `./gradlew :app:testDebugUnitTest --tests '*ModelFormViewModelTest'` | 3 testes, 0 falhas |
| Tela | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*ModelFormScreenTest` | 1 teste, 0 falhas |
| Regressão unitária e lint | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 184 testes, 0 falhas; `0 errors, 7 warnings` |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 45 testes, 0 falhas (3m 42s) |

**Limite conhecido:** o modelo não pode ser arquivado pela interface (só por repositório), e
a marca (`brand_id`) continua sem tela própria.

### Evidências — Fase 4 / US2 (2026-11-18)

Primeira execução real dos testes do módulo `:app` nesta máquina (JDK 17.0.21, AGP 8.9.2,
`compileSdk 36`, `local.properties` com o SDK em `%LOCALAPPDATA%\Android\Sdk`). Comandos
e saída:

```text
$ ./gradlew :app:assembleDebug :app:lint :app:testDebugUnitTest --continue
> Task :app:assembleDebug
> Task :app:lint
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 7s
EXIT=0

$ ./gradlew :core:domain:test :core:data:testDebugUnitTest
BUILD SUCCESSFUL in 3s
EXIT=0
```

Testes por módulo (de `*/build/test-results/*/*.xml`, todas com `failures="0" errors="0"`):

| Módulo | Suítes (testes) | Total |
|---|---|---|
| `:app` | `AddExemplarViewModelTest` (13), `AutocompleteTest` (5), `VariantFormViewModelTest` (7) | **25** |
| `:core:domain` | `CatalogValidationTest` (10), `AddExemplarUseCaseTest` (6), `CreateCatalogEntryUseCaseTest` (4), `CreateModelUseCaseTest` (4), `CreateVariantUseCaseTest` (4) | **28** |
| `:core:data` | `ImageStoreTest` (5), `CarModelRepositoryTest` (5), `ExemplarRepositoryTest` (5), `PhotoRepositoryTest` (4), `MigrationTest` (3), `DraftRepositoryTest` (3) | **25** |

78 testes, 0 falhas.

- **Lint:** `:app:lint` verde com 4 *warnings* e 0 erros (`ChromeOsAbiSupport`,
  `ModifierParameter`, `UnusedResources` ×2) — `app/build/reports/lint-results-debug.html`.
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`, 84.678.092 bytes.

**Três defeitos em cadeia, cada um visível só depois do anterior:**

1. `di/UseCaseModule.kt` chamava `CreateModelUseCase` e `CreateVariantUseCase` sem importá-los;
   o KSP falhava antes do Kotlin com `NonExistentClass`, mascarando o erro real. Corrigido com
   os dois imports.
2. `feature/catalog/PhotoGrid.kt:199` usava `RoundedCornerShape(topLeft = 8.dp)`, parâmetro que
   não existe na sobrecarga de `Dp` (é `bottomStart`/`topStart`). Corrigido para `topStart`.
3. `FakeExemplarRepository` era declarado em `AddExemplarViewModelTest` e em
   `VariantFormViewModelTest`, no mesmo pacote — `private` de topo é privado ao arquivo, mas o
   nome ainda colide. O fake do fluxo de exemplar virou `FakeAddExemplarRepository`.

**Verificado depois, na mesma data:** `:app:connectedDebugAndroidTest` no emulador API 36
(`hw_test`) — 5 testes, 0 falhas. Detalhes e correções em «Evidências — T040 (2026-11-18)».
As capturas `docs/e2e-1..7-*.png` continuam sendo da execução anterior, agora só
desatualizadas na aparência — o fluxo que elas mostram está coberto por teste.

### Evidências — T040 (2026-11-18)

Primeira execução de `:app:connectedDebugAndroidTest` em emulador `hw_test` (API 36,
x86_64), com o AVD criado pelo próprio SDK local:

```text
$ ./gradlew :app:connectedDebugAndroidTest
> Task :app:connectedDebugAndroidTest
Starting 5 tests on hw_test(AVD) - 16
hw_test(AVD) - 16 Tests 1/5 completed. (0 skipped) (0 failed)
hw_test(AVD) - 16 Tests 4/5 completed. (0 skipped) (0 failed)
Finished 5 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 44s
EXIT=0

TEST-hw_test(AVD) - 16-_app-.xml: tests="5" failures="0" errors="0" skipped="0" time="23.466"
  CatalogFlowTest.catalogoAteDoisExemplaresDaMesmaVariante          9,145 s
  AddExemplarScreenTest.avisoDeVarianteRepetidaNaoBloqueiaSalvar    2,257 s
  AddExemplarScreenTest.trocarCondicaoNotificaOViewModel            1,841 s
  AddExemplarScreenTest.mostraApenasCamposDoExemplar                1,678 s
  CreateCatalogEntryScreenTest.cadastroCompletoComFotoChegaAoColecao 7,433 s
```

**O que o T040 verifica de ponta a ponta:** sobe o `MainActivity` real (grafo Hilt, `Room`
em arquivo, navegação e ViewModels de produção) e percorre catálogo vazio → `+ Mini` →
fabricante/modelo → `Salvar miniatura` → tela de variante (cor, ano, condição `Solto`) →
`Galeria` → `Salvar variante e exemplar` → catálogo com `1 exemplar`. O seletor de fotos é
do sistema e não tem como ser tocado por teste de Compose, então o `PickVisualMedia` é
stubbado (`espresso-intents`) devolvendo um **JPEG real gerado no teste**; o resto do
caminho — ler os bytes pela `ContentResolver`, guardar a captura pendente, gravar variante,
exemplar e foto — é código de produção. Ao final, além da tela, afirma que existe arquivo
em `filesDir/images/<exemplarId>/`: grade bonita com foto perdida no caminho não é cadastro
com foto.

**Dois defeitos encontrados ao escrever este teste:**

1. **`deleteDatabase` com o banco aberto** — o `CatalogFlowTest` apagava o arquivo do banco
   antes de subir a activity, o que funciona só no primeiro teste do processo. No segundo,
   o `AppDatabase` do Hilt já estava aberto: o arquivo apagado por baixo, o
   `SQLiteOpenHelper` considerando-se aberto (não roda `onCreate` de novo) e a primeira
   consulta falhando com `no such table: car_model`. Os dois testes passaram a usar
   `testing/FreshInstall.kt`, que limpa as tabelas numa conexão própria e fecha em seguida
   — mesmo estado de instalação nova, sem tocar no arquivo.
2. **`@EntryPoint` de `androidTest` não é injetado no componente real** — a primeira versão
   do helper tentava obter o `AppDatabase` do Hilt e morria com
   `ClassCastException: Cannot cast DaggerHotWheelsApp_HiltComponents_SingletonC$SingletonCImpl to DatabaseEntryPoint`.
   Daí a conexão própria.

**Dependências acrescentadas para o teste:** `androidx.test.espresso:espresso-intents`
(stub do seletor) e `androidx.room:room-runtime` em `androidTestImplementation` (o módulo
`:app` não expõe a API do Room, que é `implementation` em `:core:data`).

**Na época, não coberto por teste:** captura pela câmera e recusa de permissão (T045) e
definição de foto principal na grade (T046) — cobertos depois, na mesma data, em
«Evidências — T045 e T046». A lista de cadastros incompletos (T048) foi coberta depois,
em «Evidências — T048».

### Evidências — T045 e T046 (2026-11-18)

Segunda execução da suíte instrumentada no mesmo emulador, agora com os testes das
fotografias:

```text
$ ./gradlew :app:connectedDebugAndroidTest
> Task :app:connectedDebugAndroidTest
Starting 10 tests on hw_test(AVD) - 16
hw_test(AVD) - 16 Tests 3/10 completed. (0 skipped) (0 failed)
hw_test(AVD) - 16 Tests 5/10 completed. (0 skipped) (0 failed)
hw_test(AVD) - 16 Tests 10/10 completed. (0 skipped) (0 failed)
Finished 10 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 1m 1s
EXIT=0

TEST-hw_test(AVD) - 16-_app-.xml: tests="10" failures="0" errors="0" skipped="0" time="41.892"
  CatalogFlowTest.catalogoAteDoisExemplaresDaMesmaVariante           12,216 s
  AddExemplarScreenTest.avisoDeVarianteRepetidaNaoBloqueiaSalvar      2,789 s
  AddExemplarScreenTest.trocarCondicaoNotificaOViewModel              2,727 s
  AddExemplarScreenTest.mostraApenasCamposDoExemplar                  2,450 s
  CreateCatalogEntryScreenTest.cadastroCompletoComFotoChegaAoColecao   8,277 s
  PhotoCaptureTest.recusaDePermissaoAvisaATelaSemEntregarBytes         2,317 s
  PhotoCaptureTest.capturaPelaCameraEntregaOsBytesDoArquivoTemporario  1,906 s
  PhotoGridTest.removerFotoAvisaOIdDoCartaoTocado                     2,483 s
  PhotoGridTest.definirPrincipalNaSegundaFotoAvisaOIdEscolhido         2,032 s
  PhotoGridTest.gradeMostraUmCartaoPorFotoComOsDoisAngulos             1,807 s
```

**T046 — `PhotoGridTest` (3 testes).** Um cartão por foto (duas fotos → duas estrelas de
«Definir como principal»), tocar a estrela do **segundo** cartão avisa `p2` (não `p1`), e
remover avisa o id do cartão tocado. O teste existe porque a transação do repositório não
prova isto: se a grade trocasse a referência dos cartões, o usuário trocaria a principal da
foto errada. A troca em si (desmarcar a anterior e marcar a nova, numa transação) já estava
coberta por `PhotoRepositoryTest.setPrimary altera a foto principal`, verde em
`:core:data:testDebugUnitTest`.

**T045 — `PhotoCaptureTest` (2 testes).** Recusa de permissão: a tela é avisada
(`onPermissionDenied`) e nenhum byte é entregue. Captura: permissão concedida → launcher da
câmera → resultado OK → os bytes do arquivo temporário chegam com o ângulo escolhido
(`PhotoAngle.BACK`). O caminho da galeria já estava coberto pelo `CreateCatalogEntryScreenTest`.

**Defeito encontrado ao escrever este teste:** a primeira versão dava e tirava a permissão
do aparelho com `pm grant` / `pm revoke`, e o ActivityManager matava o processo da
instrumentação no meio da execução — `Killing 3618:br.com.hotwheelscollector.debug/u0a216
(adj 0): permissions revoked` —, abortando a suíte em 7 de 10 testes. A versão que ficou
injeta as duas bordas que o código de produção usa, sem mexer no estado do aparelho:
`LocalContext` com `checkSelfPermission` respondendo, e um `ActivityResultRegistry` no lugar
do registro da activity.

**Limite declarado:** nem o aplicativo de câmera nem a UI do Photo Picker são abertos — o
resultado é injetado na borda do `ActivityResultRegistry`. O que está verificado é o código
do app nessas bordas (branch de permissão, leitura dos bytes do URI, entrega com o ângulo),
não o app de câmera do sistema.

### Evidências — T048 (2026-11-18)

```text
$ ./gradlew :core:data:testDebugUnitTest :core:domain:test
BUILD SUCCESSFUL in 11s
EXIT=0
  CarModelRepositoryTest  tests="6" failures="0" errors="0"   (era 5: +1 teste de variante incompleta)
  core:data               26 testes, 0 falhas
  core:domain             28 testes, 0 falhas

$ ./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest --continue
> Task :app:testDebugUnitTest
> Task :app:connectedDebugAndroidTest
Starting 12 tests on hw_test(AVD) - 16
hw_test(AVD) - 16 Tests 6/12 completed. (0 skipped) (0 failed)
hw_test(AVD) - 16 Tests 11/12 completed. (0 skipped) (0 failed)
Finished 12 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 1m 27s
EXIT=0

TEST-hw_test(AVD) - 16-_app-.xml: tests="12" failures="0" errors="0" skipped="0" time="41.705"
  IncompleteEntriesTest.modeloSemVarianteEntraNaListaComMotivoERetomaOCadastro    4,793 s
  IncompleteEntriesTest.varianteSemCorESemAnoEntraNaListaComOsCamposQueFaltam     5,185 s
```

**O que mudou no código:**

1. **Domínio:** `IncompleteEntry` ganhou `variantId` (nulo quando é o modelo) e
   `missingFields`, mais `key` (chave de lista, porque um modelo e cada variante incompleta
   são linhas diferentes) e `hasVariant`. `MissingField` é o enum de dois valores acima.
2. **Consulta nova:** `VariantDao.observeIncompleteVariants` — variante ativa de modelo
   ativo com `primary_color_id IS NULL OR release_year IS NULL`, devolvendo as bandeiras
   por campo em vez de uma lista fixa.
3. **Repositório:** `CarModelRepository.observeWithoutVariant` virou `observeIncomplete` e
   passou a ser `combine` das duas consultas. O nome antigo mentiria depois desta mudança.
4. **Tela:** o cartão mostra o motivo por campo («Falta: cor principal, ano»), e o
   `key` da `LazyColumn` passou a ser `entry.key`.

**Efeito colateral que precisou de correção:** o teste que já existia (modelo sai da lista
quando a variante é criada) começou a falhar — e por um motivo legítimo: a variante criada
nele não tem cor nem ano, então agora ela mesma é incompleta, e a linha muda de motivo em
vez de desaparecer. O teste passou a afirmar isso, em vez de ser enfraquecido.

**Higiene do repositório:** `.kotlin/` entrou no `.gitignore` — o Gradle apaga e recria
`kotlin-compiler-*.salive` a cada build, e o arquivo estava versionado (aparecia como
deletado em toda execução).

**Vocabulário de produto (2026-11-17):** o app deixou de se apresentar como marca única.
`app_name` passou de "Hot Wheels Collector" para "Coleção de Miniaturas"; o ícone já era
silhueta genérica de miniatura (sem logotipo de terceiro), então não mudou. O fabricante
já era registro do banco (`Manufacturer`, identidade `fabricante + nome + escala`) e segue
assim. **Não alterados** por serem identificadores técnicos: `applicationId`, pacote
`br.com.hotwheelscollector`, nomes dos módulos Gradle e a pasta `specs/001-hot-wheels-collector`.

**O que mudou nesta rodada (2026-11-17):**

1. **Fotos saíram da maquete.** `VariantFormScreen` descartava os bytes com
   `// Em produção, salvo no PhotoRepository`. Como `photo.exemplar_id` é obrigatório
   (FR-020), o formulário de variante passou a criar **também o primeiro exemplar**: a
   variante nasce, o exemplar nasce e as capturas pendentes são gravadas nele. Sem isso a
   foto não teria dono válido no schema.
2. **Nenhum ID fixo no fluxo.** `"variant-saved-id"` morreu; `VariantFormViewModel.onSave(modelId)`
   devolve o id real da variante. Retry após falha de foto não duplica variante nem
   exemplar (`createdVariantId`/`createdExemplarId` ficam no estado).
3. **Dois repositórios que faltavam.** `ColorRepositoryImpl` (cor por nome, `findOrCreate`)
   e `SeriesRepositoryImpl` (série resolvida dentro do fabricante do modelo), com `@Binds`
   novos em `DatabaseModule` — sem eles o Hilt não montaria o ViewModel.
4. **Grade sem scroll aninhado.** `LazyVerticalGrid` dentro de `Column.verticalScroll`
   estourava `IllegalStateException` ao ser ligada; a grade agora é construída com
   `Row`/`Column` e aceita foto já gravada e captura pendente no mesmo componente.
5. **Strings.** Rótulos de ângulo, "Galeria", "Câmera", "Ainda não salva" e os textos da
   tela de variante saíram do Kotlin e foram para `strings.xml`.
6. **Teste novo, não executado:** `app/src/test/.../VariantFormViewModelTest.kt` cobre cor e
   condição aplicadas, ordem das fotos, promoção da principal, ano inválido sem gravação,
   variante reaproveitada, série cadastrada no fabricante e retry sem duplicar exemplar.

**Para fechar a fase**, nesta ordem:

```text
./gradlew :app:testDebugUnitTest :core:data:testDebugUnitTest :app:assembleDebug :app:lint
```

1. ~~rodar o comando acima e, se verde, marcar T037–T039, T041–T042, T044–T047 e T049~~ —
   **feito em 2026-11-18** (ver «Evidências — Fase 4 / US2»): T037–T039, T041, T042, T044,
   T047 e T049 marcadas; T045, T046 e T048 continuam abertas por não terem teste automatizado;
2. ~~decidir `ModelFormScreen`~~ — **feito em 2026-11-17**: aposentada, o cadastro de
   modelo fica em `CreateCatalogEntryScreen`;
3. ~~escrever `CreateCatalogEntryScreenTest`~~ — **feito em 2026-11-18** (T040), ver
   «Evidências — T040»;
4. ~~criar a consulta e a rota de cadastros incompletos~~ — **feito em 2026-11-17**, com
   escopo em modelo sem variante (ver abaixo);
5. ~~dar entrada para `VariantFormScreen` na navegação~~ — **feita em 2026-11-17**: rota
   `catalog/variant/{modelId}`, alcançada pelo cadastro e pela lista de incompletos.

**Feito em 2026-11-17 — opção A (cadastro completo vira o fluxo padrão):**

- `CreateCatalogEntryUseCase` passou a resolver **só fabricante e modelo** e a devolver
  `CarModel`; a criação de variante com `fingerprint` saiu de lá (`CreateVariantUseCase`
  já fazia esse trabalho, com os atributos que a tela de variante coleta).
- `CreateCatalogEntryScreen`/`ViewModel` ficaram com fabricante, modelo e escala; os campos
  de cor, ano e código migraram para a tela de variante, junto das fotos.
- Rota nova `catalog/variant/{modelId}` (`Destination.VariantForm`) ligada ao FAB: `+ Mini`
  → modelo → variante (com primeiro exemplar e fotos) → catálogo.
- `CatalogFlowTest` atualizado para o novo caminho — **não executado** neste ambiente.
- Teste novo, também **não executado**: `CreateCatalogEntryUseCaseTest` (reaproveitamento de
  fabricante/modelo e validações).

Isso fecha a pendência 5 e a decisão pendente de T043 no que diz respeito ao fluxo.

**T043 (parte de modelo) encerrada por remoção (2026-11-17)** — o checkbox segue aberto
por causa do veículo real: `ModelFormScreen` era tela de maquete —
salvava devolvendo `"new-model-id"`/`"forced-new-model-id"` e não gravava nada. Com o
cadastro de modelo em `CreateCatalogEntryScreen`, ela virou código morto sem chamador e foi
excluída junto com a string `model_form_title`. O cadastro de **veículo real** (que também
estava agrupado em T043) continua não existindo: o campo opcional `real_vehicle_id` do
modelo segue sem tela, e isso precisa virar tarefa própria se for retomado.

**T048 (2026-11-17, ampliada em 2026-11-18 e 2026-11-20):** a primeira entrega mostrava só
**modelo ativo sem variante ativa** — o buraco que o fluxo em dois passos abriu. Em 2026-11-18
o outro caso de FR-029 entrou: **variante ativa sem cor principal ou sem ano** também é
listada, com o motivo no cartão («Falta: cor principal, ano»). Em 2026-11-20 a
**complementação** passou a existir: o cartão da variante abre «Editar variante», que grava
pela mesma regra de `fingerprint` da criação e não cria segundo exemplar nem segunda
variante (ver «Evidências — variante incompleta editável»). As fotos continuam fora da
edição porque pertencem ao exemplar, não à variante.

Quais campos contam como faltando é decisão de produto, registrada em código:
`MissingField` tem só `PRIMARY_COLOR` e `RELEASE_YEAR`. FR-014 admite campo desconhecido
como `NULL`, então cobrar todo opcional transformaria o acervo inteiro em pendência; a
lista cobra o que impede o colecionador de reconhecer a miniatura.

Decisão de entrada registrada: o atalho «Incompletos (N)» aparece no cabeçalho do catálogo
**apenas quando há algo a completar**, para não competir com o `+ Mini`.

---
## Phase 5: User Story 3 - Painel do estado da coleção (Priority: P2)

**Goal**: mostrar exemplares vs modelos distintos, investido vs estimado, últimas aquisições e coleções mais completas.

**Independent Test**: com dados de exemplo, comparar cada número do painel com contagem manual; todos coincidem.

### Tests for User Story 3

- [X] T050 [P] [US3] Teste de agregações (exemplares possuídos, modelos distintos, investido, estimado, itens sem avaliação) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/DashboardQueryTest.kt`
- [X] T051 [P] [US3] Teste Compose do painel verificando cartões separados e aviso de estimativa parcial em `app/src/androidTest/java/br/com/hotwheelscollector/feature/dashboard/DashboardScreenTest.kt`

### Implementation for User Story 3

- [X] T052 [US3] Implementar `DashboardRepository` com as consultas derivadas de `data-model.md` (nada armazenado) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/repository/DashboardRepositoryImpl.kt`
- [X] T053 [US3] Criar `DashboardViewModel` com estados de carregando, vazio e erro em `app/src/main/java/br/com/hotwheelscollector/feature/dashboard/DashboardViewModel.kt`
- [X] T054 [US3] Criar `DashboardScreen` com os 11 indicadores, últimas aquisições e últimos cadastros (FR-001–FR-011) em `app/.../feature/dashboard/DashboardScreen.kt`
- [X] T055 [US3] Implementar selo de estimativa com data, fonte e contagem de itens sem avaliação (FR-008, FR-086) — implementado como `EstimateCard` em `app/.../feature/dashboard/DashboardScreen.kt`; ver evidências abaixo para o motivo de não haver `EstimateBadge.kt`

**Checkpoint**: US3 entrega visão gerencial sem depender de relatórios

**Evidências — Fase 5 / US3 (2026-11-18)** — emulador `hw_test` (API 36, x86_64):

| Verificação | Comando | Resultado observado |
| --- | --- | --- |
| Agregações (T050) | `./gradlew :core:data:testDebugUnitTest` | `DashboardQueryTest`: 4 testes, 0 falhas (7,03 s) |
| Painel e barra (T051) | `./gradlew :app:connectedDebugAndroidTest` | 17 testes, 0 falhas (1m 49s), incluindo `DashboardScreenTest` (4) e `NavigationBarTest` (1) |
| Compilação | `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` | `BUILD SUCCESSFUL in 18s` |
| Lint | `./gradlew :app:lintDebug` | `BUILD SUCCESSFUL` |
| Regressão unitária | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | `BUILD SUCCESSFUL` |

Decisões registradas nesta entrega:

- **Barra de navegação fechada**: cinco abas — Painel, Catálogo, Desejos, Financeiro,
  Ajustes. O painel é a tela inicial (FR-001 pede a visão de estado primeiro) e o acervo
  virou a aba `catalog`. A rota `capture` — placeholder que duplicava o `+ Mini` e não
  tinha entrada — foi removida. Desejos/Financeiro/Ajustes continuam sem tela, mas agora
  dizem o nome da área e o que já funciona no lugar dela, em vez da mensagem de andaime.
  *(Superado em 2026-11-18: Financeiro na Fase 7, Desejos na Fase 8 e Ajustes em T132/T133
  — as três abas têm tela própria e o `PlaceholderScreen` foi removido.)*
- **Rótulo de aba ≠ título de tela** (`dashboard_tab` = «Painel», `catalog_tab` =
  «Catálogo» contra `catalog_title` = «Coleção»): texto repetido quebrava a semântica do
  Compose e os testes existentes que procuram «Coleção».
- **Totais em `Long` cru**, não em `MoneyCents`: o limite de 9.999.999,99 é por item
  (FR-076) e a soma do acervo pode passar disso.
- **Selo de estimativa dentro de `DashboardScreen.kt`** (`EstimateCard`): data da avaliação
  mais recente, fonte e contagem de exemplares sem avaliação saem do mesmo snapshot; um
  arquivo separado só criaria indireção sem testar nada a mais.
- **Teste de agregação com seed em SQL cru**: o alvo é a consulta; semear pelos DAOs
  testaria a outra ponta. O seed cobre exemplar vendido, avaliação antiga, variante
  repetida, item não colecionável e coleção arquivada.
- **Ajuste nos testes existentes**: `CatalogFlowTest`, `CreateCatalogEntryScreenTest` e
  `IncompleteEntriesTest` agora abrem a aba «Catálogo» antes do `+ Mini`; a tela inicial
  mudou e o teste que esperava o catálogo direto estourava timeout (dois casos vistos e
  corrigidos na primeira execução instrumentada).

---

## Phase 6: User Story 4 - Busca, filtros e visualizações (Priority: P2)

**Goal**: encontrar "eu tenho esse?" e "onde está" com busca tolerante, 13 filtros combináveis e 6 formas de visualizar.

**Independent Test**: com 500 itens, buscar `skyline r34` acha o item; filtros combinados reduzem sem perder válidos.

### Tests for User Story 4

- [X] T056 [P] [US4] Teste de FTS com acentos, caixa e ordem parcial (`skyline r34` → uma variante da mesma frase) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/db/SearchIndexTest.kt`
- [X] T057 [P] [US4] Teste de filtros combináveis (E entre categorias, múltipla escolha dentro da categoria) e contagem de resultados em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/SearchRepositoryTest.kt` e `app/src/androidTest/java/br/com/hotwheelscollector/feature/search/SearchScreenTest.kt`
- [X] T058 [P] [US4] Teste de desempenho com 10.000 exemplares (busca < 300 ms, lista < 1 s) em `app/src/androidTest/java/br/com/hotwheelscollector/perf/SearchPerformanceTest.kt` — **concluída (2026-11-21)**: teste instrumentado com 10.000 exemplares, FTS pelos gatilhos e os 4 limites de R.7; executado no emulador `android-36 google_apis x86_64` com os 5 testes verdes. A medição vale para o emulador; falta repetir em aparelho físico

### Implementation for User Story 4

- [X] T059 [US4] Índice FTS4 (`tokenizer=unicode61 remove_diacritics=1`) e gatilhos de sincronização — **sem migração**: a tabela já nasceu no schema v1 e os gatilhos são idempotentes no `onOpen`, com reconstrução quando a contagem diverge, em `core/data/.../db/SearchIndexSync.kt`
- [X] T060 [US4] Implementar `SearchRepository` com as 13 dimensões de filtro e ordenação por nome, ano, data de aquisição e valor (FR-116, FR-118) em `core/data/.../repository/SearchRepositoryImpl.kt`
- [X] T061 [US4] Implementar estado de filtros com chips visíveis e ação de limpar (FR-119) em `app/.../feature/search/SearchFilterState.kt`
- [X] T062 [US4] Implementar busca global com debounce e contagem de resultados (FR-114, FR-120) em `app/.../feature/search/SearchScreen.kt`
- [X] T063 [US4] Implementar visualização em grade e em lista detalhada em `app/.../feature/search/CatalogViews.kt`
- [X] T064 [US4] Implementar galeria fotográfica e agrupamentos por coleção, fabricante e série em `app/.../feature/search/GroupedCatalogViews.kt` e `app/.../feature/search/CatalogViews.kt`

**Checkpoint**: US4 torna a coleção consultável; sem escrita de dados

**Evidências — Fase 6 / US4 (2026-11-18)** — emulador `hw_test` (API 36, x86_64):

| Verificação | Comando | Resultado observado |
| --- | --- | --- |
| Índice FTS e gatilhos (T056) | `./gradlew :core:data:testDebugUnitTest --tests "*SearchIndexTest"` | 5 testes, 0 falhas |
| Filtros, ordenação, opções e foto da galeria (T057) | `./gradlew :core:data:testDebugUnitTest --tests "*SearchRepositoryTest"` | 10 testes, 0 falhas |
| Tela de busca, incluindo galeria (T057/T064) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*SearchScreenTest` | 6 testes, 0 falhas |
| Regressão completa | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 113 unitários, 0 falhas; lint 0 erros e 6 avisos |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 28 testes, 0 falhas (2m 50s) |

Decisões registradas nesta entrega:

- **Tela própria em `catalog/search`**, aberta por ícone no cabeçalho do Catálogo e
  fechada por `popBackStack`: a busca é tarefa, não uma sexta aba; as cinco abas
  existentes continuam iguais.
- **Sem migração v2**: `search_index` já existe no schema v1. Os gatilhos são instalados
  no `onOpen` (idempotente) e o índice é reconstruído quando a contagem diverge de
  `variant`; assim bancos anteriores a esta entrega também passam a buscar.
- **Conjunção por espaço, não por `AND`**: `skyline r34` vira `skyline* r34*`. O operador
  `AND` explícito do FTS4 devolveu zero linhas no SQLite do Android (verificado no teste);
  a justaposição é a conjunção implícita e funciona também com prefixo.
- **Status padrão `OWNED`**: a pergunta é «eu tenho esse?»; vendidos e arquivados só
  aparecem quando o colecionador escolhe o status. Por isso o chip de status só aparece
  na lista de filtros ativos quando difere do padrão.
- **Sem filtro de `variantIds` na interface**: a dimensão existe no repositório e é usada
  para resolver a busca, mas a tela filtra por modelo/série, que é o vocabulário do
  colecionador.
- **Busca por texto e filtros são combináveis** (`query` entra como subconsulta no
  índice); a contagem exibida é `results.size`, sem segunda consulta.
- **Galeria usa a foto principal** (`is_primary DESC, sort_order`; cai para a primeira
  disponível quando nenhuma foi marcada). O caminho relativo vem na consulta e é
  resolvido para `filesDir` na apresentação, como no restante do app; exemplar sem foto
  continua na galeria com o marcador «Sem foto», em vez de sumir do resultado.

---

## Phase 7: User Story 5 - Séries, coleções e progresso (Priority: P2)

**Goal**: acompanhar coleções com percentual, faltantes e variantes possuídas, sem presumir a taxonomia da Mattel.

**Independent Test**: coleção com 10 itens conhecidos e 6 possuídos mostra 60% e 4 faltantes.

### Tests for User Story 5

- [X] T065 [P] [US5] Teste de progresso (`possuídos / (esperados − não colecionáveis)`) e lista de faltantes em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/CollectionProgressTest.kt` — **verde**, 7 testes; a consulta real é coberta por `CollectionRepositoryTest` (7 testes)
- [X] T066 [P] [US5] Teste Compose da tela de coleção (percentual, faltantes, adquiridos) em `app/src/androidTest/java/br/com/hotwheelscollector/feature/organization/CollectionDetailTest.kt` — **verde**, 3 testes no emulador API 36

### Implementation for User Story 5

- [X] T067 [US5] **Obsoleta (2026-11-19).** `SeriesEntity`, `CollectionEntity` e `CollectionItemEntity` já existem no schema v1 — `OrganizationEntities.kt` nunca foi necessário. A separação exata entre `variant_id`/`model_id` não é CHECK no SQLite: é regra de domínio, registrada no comentário de `CollectionItemEntity`
- [X] T068 [US5] **Obsoleta (2026-11-19).** Não há migração v3: `AppDatabase` está em `version = 1` e o `1.json` exportado já contém as três tabelas com hierarquia (`parent_id`) e índices. Não existe pasta `db/migration/`
- [X] T069 [US5] Implementar `CollectionProgressUseCase` e `MissingItemsUseCase` (FR-052–FR-054) — os dois estão em `core/domain/.../usecase/CollectionUseCases.kt`; a regra em si vive em `CollectionProgressCalculator` (`domain/model/CollectionProgress.kt`), usada pelo painel e pela tela de coleção
- [X] T070 [US5] Implementar CRUD de série e coleção com hierarquia e proteção contra ciclo — coleções em `app/.../feature/organization/CollectionFormScreen.kt` (`CollectionsScreen`, `CollectionDetailScreen`) e séries em `SeriesScreen.kt`; a proteção contra ciclo é do repositório (`update` sobe a cadeia de pais)
- [X] T071 [US5] Implementar tela de coleção com percentual, faltantes e itens não colecionáveis em `app/.../feature/organization/CollectionDetailScreen.kt` — inclui o seletor de item (`CollectionItemPicker.kt`), com abas de variante e modelo
- [X] T072 [US5] Implementar marcação "não colecionável" e renomear/reorganizar sem perder vínculos (FR-055, FR-056) — a marcação fica na linha do item em `CollectionDetailScreen.kt`; renomear/mover é `CollectionRepository.update`, que audita nome e `parent_id` e recusa ciclo. Arquivar com subcoleção ativa é bloqueado, não cascateado

**Checkpoint**: US5 fecha o ciclo de "o que falta comprar"

### Situação real — Fase 7 / US5 (2026-11-19)

Verificado no código antes de abrir a fase: **US5 não precisa de schema novo**.

- `SeriesEntity`, `CollectionEntity` e `CollectionItemEntity` já estão em `AppDatabase`
  (23 entidades, `version = 1`) e no `1.json` exportado; não existe pasta `db/migration/`.
- `CollectionDao` e `CollectionItemDao` (`core/data/.../db/dao/CollectionsDao.kt`) já cobrem
  lista ativa, busca por id, inserção/atualização de coleção, itens por coleção e busca por
  variante.
- O painel já calcula progresso de coleção (`DashboardDao.observeCollectionProgress`) e
  conta coleções ativas, mas **nada no app cria uma coleção** — por isso a visão «Coleção»
  da busca só mostra «Sem coleção».
- A regra «exatamente um entre `variant_id` e `model_id`» em `collection_item` não é CHECK
  no SQLite: fica na validação de domínio (comentário registrado na entidade).

**O que falta de fato:** T065, T066 e T069–T072 — repositório, use cases de progresso e
faltantes, CRUD de série/coleção, telas de coleção e marcação de não colecionável.

### Evidências — Fase 7 / US5 (2026-11-19)

Entrega: repositório e cálculo de progresso, telas de coleção (lista, detalhe, formulário),
seletor de item por variante ou modelo, marcação de não colecionável, CRUD de séries e
proteção contra ciclo. **Nenhuma alteração de schema** — tudo usa as tabelas do v1.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Progresso e faltantes (T065) | `./gradlew :core:domain:test` | `CollectionProgressTest`: 7 testes, 0 falhas (39 no módulo) |
| Coleção no banco | `./gradlew :core:data:testDebugUnitTest` | `CollectionRepositoryTest`: 7 testes, 0 falhas (68 no módulo) |
| Tela de coleção (T066) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*CollectionDetailTest` | `CollectionDetailTest`: 3 testes, 0 falhas (emulador `hw_test`, API 36) |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 35 testes, 0 falhas (2m 41s) |
| Compilação, APK e lint | `./gradlew :app:assembleDebug :app:lintDebug` | `BUILD SUCCESSFUL`, `0 errors, 8 warnings` |

Regras registradas nesta entrega:

- **Progresso é por unidade, não por linha**: um item com `expected = 2` pesa dois no
  denominador e `min(possuídos, esperado)` no numerador. Com o padrão `expected = 1`, o
  número coincide com a consulta do painel; a diferença só aparece quando o colecionador
  pede mais de uma unidade do mesmo item.
- **Item não colecionável sai dos dois lados** da fração, não da coleção: continua listado
  e pode ser revertido.
- **Uma regra só**: `CollectionProgressCalculator` é usado pela lista de coleções e pelo
  detalhe; o painel mantém a consulta SQL que já existia e que coincide no caso padrão.
- **`owned` conta só `status = 'OWNED'`**, igual ao painel: vendido e arquivado continuam
  no banco mas não são progresso.
- **Ciclo é recusado na escrita**, não na leitura: `update` sobe a cadeia de pais e
  devolve `Validation` quando o novo pai é descendente da própria coleção.
- **Arquivar coleção com subcoleção ativa é bloqueado** (`Blocked`), sem cascata; itens da
  coleção arquivada continuam guardados.
- **Séries**: criar, renomear e arquivar por fabricante. Arquivar não apaga a série nem
  mexe nas variantes que a apontam.
- **Limite conhecido**: a edição dos itens cobre quantidade esperada apenas na criação via
  repositório; a tela adiciona com `expected = 1` e oferece marcar não colecionável e
  remover. Editar a quantidade pela interface continua aberto.

---

## Phase 8: User Story 6 - Lista de desejos e conversão em aquisição (Priority: P3)

**Goal**: registrar o que se quer comprar e converter em aquisição sem perder o histórico do desejo.

**Independent Test**: converter desejo em aquisição e verificar data de criação, prioridade e valores originais preservados.

### Tests for User Story 6

- [X] T073 [P] [US6] Teste de conversão transacional preservando prioridade, valor máximo e data de criação (FR-063) — em `WishlistRepositoryTest`, incluindo o caso de falha que desfaz exemplar e vínculo juntos
- [X] T074 [P] [US6] Teste de que desejo não entra em contagem de possuídos nem no patrimônio (FR-064) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/WishlistRepositoryTest.kt`

### Implementation for User Story 6

- [X] T075 [US6] `wishlist_item` já existia no schema v1 (`WishlistItemEntity` + `WishlistDao`); sem migração v4. A validação de modelo/variante/texto livre fica no construtor do domínio e no formulário
- [X] T076 [US6] Implementar formulário de desejo (prioridade, valor máximo, encontrado, loja, link, observações) em `app/.../feature/wishlist/WishlistFormScreen.kt`
- [X] T077 [US6] Implementar lista de desejos ordenada por prioridade e status em `app/.../feature/wishlist/WishlistScreen.kt`
- [X] T078 [US6] Implementar conversão desejo→aquisição com vínculo ao exemplar criado ou existente — em `WishlistViewModel` + `WishlistRepositoryImpl` (não há `ConvertToAcquisition.kt`)
- [X] T079 [US6] Implementar abertura de link de referência em app externo com validação `http`/`https` em `app/.../feature/wishlist/ReferenceLink.kt`

**Checkpoint**: US6 conecta intenção de compra ao financeiro

### Evidências — Fase 8 / US6 e Ajustes (2026-11-18)

| Verificação | Comando | Resultado observado |
| --- | --- | --- |
| Desejos no banco (T073/T074) | `./gradlew :core:data:testDebugUnitTest --tests "*WishlistRepositoryTest"` | 5 testes, 0 falhas |
| Lista e Ajustes (T077/T132/T133) | `./gradlew :app:connectedDebugAndroidTest` | 32 testes, 0 falhas (2m 51s), incluindo `WishlistScreenTest` (3) e `SettingsScreenTest` (1) |
| Regressão completa | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 118 unitários, 0 falhas; lint 0 erros e 6 avisos |

Decisões registradas nesta entrega:

- **A tabela já existia**: `wishlist_item`, `WishlistDao` e o modelo de domínio vieram da
  fundação (v1). A US6 não precisou de migração; faltavam o repositório, as telas e a
  conversão.
- **Conversão atômica**: `WishlistRepository.convertToAcquisition` abre uma transação
  (`TransactionRunner`) e chama `ExemplarRepository.add` dentro dela — o Room reusa a
  mesma transação, então exemplar, aquisição, movimentação, auditoria e vínculo do desejo
  nascem juntos ou voltam atrás juntos. Desejo só com texto livre vira `ACQUIRED` sem
  exemplar e a tela diz isso. Prioridade, valores e `created_at` nunca são reescritos.
- **Edição não reabre**: `update` preserva `status` e vínculo; só `markAcquired` e
  `dismiss` mudam o ciclo de vida.
- **Título resolvido no SQL**: a consulta junta variante → modelo → fabricante e prefere o
  texto livre; a lista não faz uma consulta por linha.
- **Ajustes sem item decorativo**: a tela tem só «Sobre» (versão lida do pacote instalado)
  e «Política de privacidade» (texto offline). Backup/importação ficam para a US10.
- **`PlaceholderScreen` removido**: as cinco abas agora abrem telas reais.

---

## Phase 9: User Story 7 - Duplicados e negociação (Priority: P3)

**Goal**: identificar duplicados por variante, marcar disponibilidade e registrar negociação até a conclusão.

**Independent Test**: 3 exemplares da mesma variante + 1 de outra variante do mesmo modelo → só os 3 aparecem como duplicados.

### Tests for User Story 7

- [X] T080 [P] [US7] Teste de agrupamento de duplicados por `variant_id` (nunca por nome do modelo) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/DuplicateQueryTest.kt`
- [X] T081 [P] [US7] Teste de índice parcial único: um exemplar não pode ter duas negociações ativas (FR-068) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/NegotiationRepositoryTest.kt`

### Implementation for User Story 7

- [X] T082 [US7] **Obsoleta (2026-11-19).** `negotiation` e `negotiation_event` já nascem no schema v1 (`NegotiationEntity`/`NegotiationEventEntity`, DAOs em `CollectionsDao.kt`); não há migração v5 nem `NegotiationEntities.kt`; a disponibilidade da US7 acabou exigindo a **migração v2**, ver «Evidências — Fase 9 / US7». O índice parcial único de `OPEN`/`RESERVED` **não** existe: a unicidade de negociação ativa fica na transação do repositório, coberta por T081/T085
- [X] T083 [US7] Implementar consulta `GetDuplicatesUseCase` agrupada por variante com contagem (FR-065) em `core/domain/.../usecase/GetDuplicatesUseCase.kt`
- [X] T084 [US7] Implementar marcação de disponibilidade (troca, venda, ambos, indisponível) com condição e valor pretendido em `app/.../feature/duplicates/DuplicateActionsSheet.kt`
- [X] T085 [US7] Implementar reserva com bloqueio de segunda negociação ativa em `app/.../feature/duplicates/NegotiationScreen.kt`
- [X] T086 [US7] Implementar conclusão de venda (`status=SOLD` + evento financeiro com valor e data) em `core/domain/.../usecase/CloseSaleUseCase.kt`
- [X] T087 [US7] Implementar conclusão de troca (saída do negociado + entrada do recebido) em `core/domain/.../usecase/CloseTradeUseCase.kt`
- [X] T088 [US7] Implementar cancelamento que devolve estado anterior e mantém `negotiation_event` imutável (FR-072–FR-073) em `app/.../feature/duplicates/NegotiationHistoryScreen.kt`

**Checkpoint**: US7 evita vender o exemplar errado e mantém rastro da negociação

### Situação real — Fase 9 / US7 (2026-11-19)

Como em US5, as tabelas vieram da fundação: `negotiation` e `negotiation_event` estão no
`AppDatabase` v1, com `NegotiationDao` e `NegotiationEventDao` (lista por exemplar, busca de
negociação ativa, contagem por exemplar, histórico por negociação). O painel já exibe
«Variantes repetidas» (`DashboardDao.observeVariantsWithDuplicates`), mas não há como agir
sobre o número.

**Duas ressalvas registradas na análise:**

1. O índice parcial único descrito em T082 não existe — as entidades têm só índices não
   únicos (`exemplar_id + status`, `status`). A garantia de «uma negociação ativa por
   exemplar» tem de ser transacional no repositório, e é isso que T081 testa.
2. A disponibilidade para troca/venda (T084) não tinha coluna própria no schema. A decisão
   tomada nesta entrega foi a **migração v2** com a tabela `duplicate_offer` — «ambos» não
   cabe em `negotiation.type`, e a mesma unidade não pode ter duas ofertas.

**O que falta de fato:** T080, T081 e T083–T088. **Fechado em 2026-11-19**, ver
evidências abaixo.

### Evidências — Fase 9 / US7 (2026-11-19)

Entrega: lista de duplicados agrupada por variante, disponibilidade (troca, venda, ambos
ou indisponível, com condição e valor pretendido), reserva com bloqueio de segunda
negociação ativa, conclusão de venda e de troca e cancelamento com histórico permanente.
A tabela escolhida foi **schema v2**: a disponibilidade não cabia em `negotiation` porque
«ambos» não é nem venda nem troca, e a troca precisava apontar o exemplar recebido.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Duplicados por variante (T080) | `./gradlew :core:data:testDebugUnitTest` | `DuplicateQueryTest`: 4 testes, 0 falhas |
| Negociação ativa única e conclusões (T081) | `./gradlew :core:data:testDebugUnitTest` | `NegotiationRepositoryTest`: 6 testes, 0 falhas |
| Migração v1→v2 (R.8) | `./gradlew :core:data:testDebugUnitTest` | `MigrationTest`: 8 testes, 0 falhas (5 de migração + 3 do índice parcial e da cobertura de versões) |
| Fluxo real no emulador (T084–T088) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*DuplicatesFlowTest` | `DuplicatesFlowTest`: 1 teste, 0 falhas (emulador `hw_test`, API 36) |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 39 testes, 0 falhas (3m 46s) |
| Regressão unitária e lint | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 170 testes, 0 falhas; `0 errors, 7 warnings` |
| APK | `./gradlew :app:assembleDebug` | `app-debug.apk`, 85.954.592 bytes |

O teste instrumentado percorre o caminho do toque: semeia três exemplares da variante A e
um da variante B do mesmo modelo, abre **Duplicados** pelo catálogo, confere que só a A
aparece (o `HW-0004` da B não está na tela), marca venda por R$ 45,00 na folha de
disponibilidade, reserva o exemplar, vê «Negociação Reservada» e cancela — terminando com
o histórico em «Venda · Cancelada». O histórico continua na tela depois do cancelamento.

Decisões registradas nesta entrega:

- **Schema v2, sem destruição de dados**: `MIGRATION_1_2` cria `duplicate_offer` (uma linha
  por exemplar, `ON DELETE CASCADE` porque oferta não é trilha de auditoria) e acrescenta
  `negotiation.received_exemplar_id` (`ON DELETE RESTRICT`: histórico não pode perder o
  exemplar recebido). O `2.json` foi exportado e o `MigrationTest` compara a migração com
  ele.
- **«Ambos» é valor próprio** (`DuplicateAvailability.BOTH`), não duas linhas: a mesma
  unidade física não pode estar em duas listas de oferta.
- **A reserva vive no status da negociação**, não em `exemplar.status`: por isso cancelar
  devolve o exemplar ao estado anterior sem reescrever nada (FR-072). O exemplar só muda de
  `OWNED` para `SOLD`/`TRADED` na conclusão.
- **Unicidade transacional**: o índice parcial único de `OPEN`/`RESERVED` continua não
  existindo no SQLite; a checagem e a inserção acontecem na mesma transação, e T081 cobre
  a segunda tentativa.
- **Eventos appende-only**: abrir, concluir e cancelar inserem `negotiation_event`;
  `closeSale` grava valor e data no evento e no registro, e a consulta de histórico lê
  todos os eventos do exemplar numa consulta só (FR-073).
- **Troca sem exemplar recebido é válida**: o vínculo é opcional; a troca por dinheiro ou
  por item ainda não cadastrado não inventa um exemplar (FR-071 diz «quando houver»).
- **Entrada pelo catálogo**: botão «Duplicados» no cabeçalho de `CatalogScreen`, ao lado de
  Coleções e Incompletos, com as rotas `duplicates` e `duplicates/{exemplarId}/negotiation`.

**Defeitos encontrados na execução:**

1. **`R$` duplicado**: `formatMoneyPtBr` já devolve o prefixo, e as telas novas somavam
   outro — o teste instrumentado flagrou «Venda · R$ R$ 45,00». Corrigido na linha do
   duplicado, no histórico e em `negotiation_amount_value`.
2. **Folha de disponibilidade sem rolagem**: com os cinco chips de condição e os dois
   campos, «Salvar» saía da tela em aparelho de 1080×2340. A folha passou a ter
   `verticalScroll`; o teste falhou por «no parent layout with a Scroll SemanticsAction»
   antes da correção.
3. **Bancos de `androidTest` sem a migração**: `FreshInstall` e o seed do novo teste abrem
   `Room` diretamente e não conheciam `MIGRATION_1_2`; no emulador com banco v1 da execução
   anterior, o teste morreu com «A migration from 1 to 2 was required but not found». Ambos
   receberam `.addMigrations(AppDatabase.MIGRATION_1_2)`.
4. **Migração falhava em aparelho com banco v1 real** (2026-11-19): «Migration didn't properly
   handle: variant». O índice único parcial `index_variant_fingerprint_active` é criado pelo
   callback, não está no `@Entity` nem no JSON exportado, mas o Room lê todos os índices na
   validação — e o `MigrationTestHelper` não executa callbacks, então o teste não reproduzia.
   `MIGRATION_1_2` passou a remover o índice antes da validação e `CALLBACK.onOpen` a recriá-lo
   (idempotente, em toda abertura); o novo teste cria o índice à mão antes de migrar e falha
   sem a correção. Dado do usuário preservado: a transação de migração é revertida e o banco
   continua em v1.
5. **Garantia contra a repetição**: `cadaMigracaoAceitaBancoComIndiceParcialDeFingerprint` cria
   cada banco de partida (da lista `migrations`) com o índice parcial e roda a migração com
   validação — uma migração futura que altere `variant` sem removê-lo falha aqui. Confirmei que
   ele falha se o `DROP INDEX` some. `migracoesCobremTodasAsVersoesDoSchema` acusa versão nova
   no `@Database` sem migração registrada, e `aberturaDoBancoMigradoRecriaIndiceParcial` percorre
   o caminho real (arquivo v1 com índice → `Room` com migrações e callback → índice de volta).
6. **«Duplicados» truncado no cabeçalho do catálogo** (2026-11-19): com título e quatro ações
   na mesma linha, o botão era espremido e quebrava o texto letra a letra em tela estreita. O
   cabeçalho ficou com título + busca, e os atalhos (Incompletos, Coleções, Duplicados) foram
   para uma linha própria com rolagem horizontal — sem quebra de texto.

**Limites conhecidos:** a quantidade de exemplares por item de coleção continua sem edição
pela interface (herdado de US5).

---

## Phase 10: User Story 8 - Financeiro e avaliações (Priority: P3)

**Goal**: registrar aquisições e avaliações, calcular investimento, custo médio e gastos por período/série/fabricante, sem apresentar estimativa como preço oficial.

**Independent Test**: cinco aquisições conhecidas → total, custo médio e gasto por série coincidem com cálculo manual (incluindo frete e desconto).

### Tests for User Story 8

- [X] T089 [P] [US8] Teste de custo (`preço + frete − desconto`), total investido e custo médio com exclusão de itens sem valor (FR-075, FR-081) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/model/FinanceModelsTest.kt` (o arquivo planejado era `usecase/FinanceTotalsTest.kt`; a regra ficou no modelo e o teste foi junto).
- [X] T090 [P] [US8] Teste de "avaliação mais recente = valor atual" e histórico completo preservado (FR-083–FR-084) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/FinancialRepositoryTest.kt`
- [X] T091 [P] [US8] Teste Compose do fluxo financeiro com rótulo de estimativa visível em `app/src/androidTest/java/br/com/hotwheelscollector/feature/finance/FinanceScreenTest.kt`

### Implementation for User Story 8

- [X] T092 [US8] Formulário de aquisição (data, preço, frete, desconto, loja, vendedor, forma, moeda) — todos os campos editáveis em `ExemplarDetailScreen`. A moeda é BRL fixo: FR-076 define o real como padrão e «outras moedas na interface» está fora de escopo.
- [X] T093 [US8] Tela de avaliação com valor, data, fonte e histórico de avaliações — entregue dentro de `app/src/main/java/br/com/hotwheelscollector/feature/catalog/ExemplarDetailScreen.kt`.
- [X] T094 [US8] Implementar agregações por período, série, fabricante e coleção (FR-078–FR-080) em `core/data/.../repository/FinanceRepositoryImpl.kt`
- [X] T095 [US8] Implementar tela de finanças com investido, estimado, diferença e custo médio (FR-077, FR-085) em `app/.../feature/finance/FinanceScreen.kt`
- [X] T096 [US8] Implementar rótulo obrigatório "estimativa do colecionador" com fonte e data em toda exibição de valor estimado (FR-086) — aplicado no detalhe do exemplar e na tela de finanças com `valuation_hint` e a data das avaliações; não foi necessário um `EstimateLabel.kt` separado.
- [X] T097 [US8] Implementar histórico financeiro separando vendas/trocas do investimento (FR-087) em `core/domain/.../usecase/FinancialHistoryUseCase.kt` — depende das negociações concluídas de US7; o investido já considera só `status = 'OWNED'`.
- [X] T098 [US8] Implementar formatação pt-BR de moeda (centavos) e data centralizada em `core/ui/src/main/kotlin/br/com/hotwheelscollector/ui/format/Formatters.kt`

### Evidências — aquisição e avaliações (primeira entrega de US8, 2026-11-18)

Entrega: a aquisição do exemplar passou a ser editável (upsert por `exemplar_id`, nunca
uma segunda compra para a mesma unidade) e as avaliações são registradas com valor, data,
fonte e observações, com histórico preservado. A avaliação mais recente é o valor atual.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Unitários | `:core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | 90 testes, 0 falhas (3 novos em `FinancialRepositoryTest`) — números daquele commit; o total atual está no bloco do financeiro |
| Instrumentados | `:app:connectedDebugAndroidTest` (API 36, Pixel 5) | 21 testes, 0 falhas (1 novo `ExemplarDetailScreenTest`) |
| Lint | `:app:lintDebug` | 0 erros, 7 avisos pré-existentes |

Decisões registradas:

- `AcquisitionRepository.save` devolve o registro existente quando já há uma aquisição
  para o exemplar: id e `created_at` são preservados; só `updated_at` muda.
- `observeTotalInvested` e `observeCurrentEstimated` devolvem `Long` cru, porque são somas
  do acervo e podem passar do limite de um item (FR-076).
- A tela do exemplar mostra o custo total calculado (`preço + frete − desconto`) e o valor
  estimado sempre com o rótulo «estimativa do colecionador» (FR-086).
- O painel passa a refletir avaliações reais; antes o «Estimado» era sempre zero.

Pendências de US8 nesta entrega: T089, T091, T092, T094–T096 e T098; todas fechadas
na entrega do financeiro abaixo. **T097 foi fechada em 2026-11-19**, ver «Evidências —
histórico financeiro».

### Evidências — financeiro e custo médio (US8, 2026-11-18)

Entrega: tela **Financeiro** na aba própria, com investido, estimado, diferença e custo
médio; gastos por fabricante, série, coleção e mês, com intervalo livre (`De`/`Até`).
Forma de aquisição passou a ser editável no detalhe do exemplar. A formatação pt-BR de
centavos e datas foi centralizada em `core/ui`.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Unitários | `:core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | 98 testes, 0 falhas (4 novos em `FinanceModelsTest`, 4 em `FinanceRepositoryTest`) |
| Instrumentados | `:app:connectedDebugAndroidTest` (API 36, Pixel 5) | 22 testes, 0 falhas (novo `FinanceScreenTest`; `NavigationBarTest` passou a cobrar a tela real) |
| Lint | `:app:lintDebug` | 0 erros, 7 avisos pré-existentes |

Decisões registradas:

- Somas financeiras usam `Long` cru; a média só divide pelos exemplares com preço
  informado e a tela diz quantos ficaram fora (FR-081).
- Aquisição sem preço não entra no investido nem nos agrupamentos, mesmo que tenha frete:
  custo desconhecido não vira zero nem frete solto (FR-075).
- Gastos por coleção podem repetir o custo de um exemplar que está em mais de uma coleção;
  é gasto por coleção, não um segundo total do acervo.

### Evidências — histórico financeiro (T097, 2026-11-19)

Entrega: bloco **Vendas e trocas** na tela de Finanças, separado do investido. Vendas
somam o valor recebido; trocas são contadas à parte, porque o retorno foi um item, não
dinheiro. Cada linha traz data, exemplar, tipo, contraparte/histórico e — na troca — o
exemplar recebido. O investido continua vindo só de exemplares `OWNED`; nenhuma aquisição
é reescrita ou reavaliada (FR-087).

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Totais de vendas/trocas (domínio) | `./gradlew :core:domain:test` | `FinanceModelsTest`: 5 testes, 0 falhas (era 4) |
| Consulta e exclusão de canceladas | `./gradlew :core:data:testDebugUnitTest` | `FinanceRepositoryTest`: 5 testes, 0 falhas (era 4) |
| Tela de Finanças | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*FinanceScreenTest` | `FinanceScreenTest`: 1 teste, 0 falhas (emulador `hw_test`, API 36) |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 39 testes, 0 falhas (3m 41s) |
| Regressão unitária e lint | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 170 testes, 0 falhas; `0 errors, 7 warnings` |

O teste de repositório prova a regra inteira em banco real: cenário com três exemplares e
R$ 400,00 investidos; venda concluída num, troca concluída noutro e uma negociação
cancelada no terceiro. Depois, o investido cai para zero (só resta um exemplar sem preço),
as duas saídas aparecem no histórico — a venda com R$ 150,00 e contraparte, a troca sem
valor — e a cancelada não aparece.

Decisões registradas:

- **Só `CLOSED` entra**: cancelada não é saída do acervo, mesmo com histórico preservado.
- **Receita de venda é o valor recebido**; o valor discutido numa troca não vira receita.
- **Nada é recalculado**: as consultas de investido e de agrupamentos continuam filtrando
  `status = 'OWNED'`; a aquisição do exemplar vendido permanece intacta.
- **`FinancialHistoryUseCase`** é o ponto de entrada da tela; o repositório devolve as
  saídas e o modelo de domínio calcula totais e separação por tipo.
- Moeda é BRL fixo na interface; outras moedas estão fora de escopo (FR-076).

**Checkpoint**: US8 entrega o módulo de maior valor de longo prazo

---

## Phase 11: User Story 9 - Localização física e movimentação (Priority: P3)

**Goal**: saber onde cada exemplar está, com hierarquia criada pelo usuário e histórico de movimentação.

**Independent Test**: atribuir exemplar a "Caixa 12", mover para "Estante A > Prateleira 3", ver as duas posições no histórico com datas.

### Tests for User Story 9

- [X] T099 [P] [US9] Teste de hierarquia (ciclo impedido) e movimentação atômica (localização + histórico juntos) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/LocationTreeTest.kt` — **verde**, 4 testes; a transação é coberta por `LocationRepositoryTest` (7 testes)
- [X] T100 [P] [US9] Teste de exclusão bloqueada quando há exemplares na localização (FR-093) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/LocationRepositoryTest.kt` — **verde**, 7 testes no arquivo

### Implementation for User Story 9

- [X] T101 [US9] **Obsoleta (2026-11-19).** `LocationEntity` (com `type`), `MovementEntity` e os DAOs `LocationDao`/`MovementDao` já estão no schema v1 (`InventoryDao.kt`, `SearchDao.kt`); não há migração v6 nem `LocationEntities.kt`
- [X] T102 [US9] Implementar árvore de localização com criação de níveis e nomes personalizados em `app/.../feature/locations/LocationsScreen.kt` — (`LocationTreeScreen.kt` não foi necessário)
- [X] T103 [US9] Implementar atribuição e movimentação com histórico em transação única (FR-090–FR-091) — **entregue dentro de `app/src/main/java/br/com/hotwheelscollector/feature/catalog/ExemplarDetailScreen.kt`**, não em `MoveExemplarSheet.kt`: localização, movimentação com observação e histórico saem do mesmo `SupportRepositories`, em `transactionRunner.inTransaction`
- [X] T104 [US9] Implementar diálogo de exclusão com decisão explícita (mover ou arquivar), sem cascata — `DeleteLocationDialog` dentro de `LocationsScreen.kt`, com a transação em `LocationRepositoryImpl.archive(id, policy)`
- [X] T105 [US9] Implementar consulta e filtro por localização em qualquer nível da hierarquia (FR-094) — inventário com CTE recursiva (`LocationDao.observeContents`) e filtro da busca expandindo a subárvore (`SearchRepositoryImpl`), coberto em `SearchRepositoryTest`
- [X] T106 [US9] Implementar inventário por localização (FR-095) em `app/.../feature/locations/LocationContentsScreen.kt` — cada item abre o detalhe do exemplar

**Checkpoint**: US9 resolve "não sei onde guardei"

### Situação real — Fase 11 / US9 (2026-11-19)

A fase está **meio entregue**, e não do jeito que o plano previa:

- T101 é obsoleta — as tabelas `location` e `movement` vieram da fundação.
- **T103 está entregue**, mas dentro de `ExemplarDetailScreen` (movimentação com histórico
  em transação única, via `SupportRepositories`), e não em `MoveExemplarSheet.kt`.
- `LocationRepository` já existe em `SupportRepositories.kt`, incluindo a lista usada pelo
  cadastro de exemplar (`AddExemplarScreen`).

**O que faltava de fato:** T099, T100, T102 e T104–T106. Sem a árvore de locais (T102), o
usuário só escolhia entre as localizações que já existissem — não havia como criar
«Estante A > Prateleira 3» pela interface, que é justamente o Independent Test da fase.
**Fechado em 2026-11-19**, ver evidências abaixo.

### Evidências — Fase 11 / US9 (2026-11-19)

Entrega: árvore de localizações com criação de níveis, renomeação e exclusão com decisão
explícita; inventário por localização incluindo sublocalizações; filtro de busca por
localização expandindo a subárvore. **Nenhuma alteração de schema.**

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Árvore e hierarquia (T099) | `./gradlew :core:domain:test` | `LocationTreeTest`: 4 testes, 0 falhas (43 no módulo) |
| Exclusão, movimentação e inventário (T100) | `./gradlew :core:data:testDebugUnitTest` | `LocationRepositoryTest`: 9 testes, 0 falhas (78 no módulo) |
| Filtro por localização inclui sublocalização | `./gradlew :core:data:testDebugUnitTest` | `SearchRepositoryTest`: 11 testes, 0 falhas |
| Árvore e diálogo (T102/T104) | `./gradlew :app:connectedDebugAndroidTest` | `LocationsTest`: 3 testes, 0 falhas (38 no total, emulador `hw_test` API 36) |
| Texto das mensagens de exclusão | `./gradlew :app:testDebugUnitTest` | `LocationsMessageTextTest`: 4 testes, 0 falhas (32 no módulo) — resolve os plurais do `strings.xml` e confere «1 exemplar movido» / «3 exemplares movidos» |
| Lint e APK | `./gradlew :app:lintDebug :app:assembleDebug` | `BUILD SUCCESSFUL`, `0 errors, 7 warnings` |
| Verificação visual no emulador (`hw_test` API 36, dados semeados) | árvore, diálogo, mover e snackbars | Árvore com “1 exemplar”/“3 exemplares” e “(4 na subárvore)”; exclusão de 3 exemplares abriu com as duas opções e o destino "Caixa 12"; snackbar “Localização excluída. 3 exemplares movidos.”; exclusão de 1 exemplar com snackbar “Localização excluída. 1 exemplar arquivado.”; banco conferido (3 `movement` de `shelf3` para `crate`, exemplar arquivado e as duas localizações com `archived_at`); exclusão bloqueada de uma estante com 2 sublocalizações mostrou “Ela tem 2 sublocalizações ativas…” e só o botão Fechar; inventário de “Caixa 12” listou “3 exemplares · Inclui as sublocalizações”, com um item vindo da “Gaveta 7”, e cada item abriu o detalhe do exemplar |

Decisões registradas nesta entrega:

- **CTE recursiva** no SQLite para inventário e para o filtro de busca: "Estante A" traz as
  prateleiras sem uma consulta por nível e sem depender de hierarquia fixa.
- **Exclusão é arquivamento com política**: `LocationArchivePolicy.None` só é aceito com a
  localização vazia; `MoveTo` move cada exemplar e grava `movement` + auditoria na mesma
  transação; `ArchiveExemplars` marca `status = ARCHIVED` e registra a saída.
- **A contagem da mensagem vem do repositório**, que devolve quantos exemplares tratou.
  A tela não informa mais o número: o que ela exibe é o acervo da subárvore, e o repositório
  trata os possuídos daquela localização. Exemplar vendido ou arquivado não entra na conta
  nem é movimentado (`LocationRepositoryTest` cobre arquivado e vendido).
- **Só `OWNED` conta como acervo**, igual a todo o resto do app (dashboard, finanças, busca,
  contagens e inventário). Pelo mesmo motivo, o diálogo de exclusão fala dos possuídos
  *daquela* localização, não da subárvore: com sublocalização ativa a exclusão nem passa.
  `LocationsViewModel` tem um teste em que a árvore anuncia 99 e o repositório devolve 5 — a
  mensagem segue o 5.
- **Destino não pode ser descendente** da localização excluída: mover para dentro dela
  deixaria exemplares embaixo de uma raiz arquivada.
- **Sublocalização ativa bloqueia** a exclusão (`Blocked`) — reorganizar antes, sem cascata.
- **Sobras da árvore viram raiz**: pai arquivado ou ciclo gravado por fora não podem fazer
  uma localização que ainda guarda exemplares desaparecer da tela.
- **Entrada em Ajustes → Localizações**, com o inventário aberto ao tocar na localização.
- **As opções do diálogo ficam empilhadas**: lado a lado, o rótulo de “Mover os exemplares
  para outra localização” ocupava a largura do diálogo e empurrava “Arquivar os exemplares”
  para fora da tela. Só a verificação visual pegou isso — na árvore de semântica o chip
  existia, então o teste passou a exigir `assertIsDisplayed`.

---
## Phase 12: User Story 10 - Relatórios, importação, exportação e backup (Priority: P4)

**Goal**: extrair relatórios em CSV/XLSX/PDF gerados no aparelho e garantir que a coleção sobrevive à troca de celular.

**Independent Test**: gerar backup, apagar os dados do app, restaurar e verificar contagens e fotos vinculadas; gerar PDF em modo avião.

### Tests for User Story 10

- [X] T107 [P] [US10] Teste de contrato do CSV de importação (colunas, enums, datas, `E001`–`E008`) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/CsvImportContractTest.kt` — **concluída (2026-11-21)**: cabeçalho com caixa/acento/espaço, BOM, `,`/`;`, coluna desconhecida, ISO e `dd/MM/yyyy`, `E001`–`E008`; corrigiu espaço em cabeçalho para `_` e descarte de colunas desconhecidas
- [X] T108 [P] [US10] Teste de contrato do pacote de backup (layout, `manifest.json`, SHA-256 por arquivo) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/BackupServiceTest.kt`
- [X] T109 [P] [US10] Teste de formato dos relatórios: PDF (páginas, cabeçalho, totais) e XLSX aberto por leitor independente em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/ReportFormatTest.kt` — **concluída (2026-11-21)**: PDF com título/data/filtros/paginação/totais e XLSX relido pelo `XlsxReader` (OOXML íntegro), além de CSV com BOM e nome datado
- [X] T110 [P] [US10] Teste de ida e volta backup→restauração em banco novo com fotos e relacionamentos (SC-004) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/BackupServiceTest.kt`

### Implementation for User Story 10

- [X] T111 [US10] Implementar leitor CSV (UTF-8 com/sem BOM, delimitador `,`/`;` detectado, cabeçalho obrigatório) em `core/data/.../importexport/CsvReader.kt` — **concluída (2026-11-21)**: BOM, `,`/`;` detectado, cabeçalho normalizado (sem caixa/acento/espaço extra), aspas e `""`
- [X] T112 [US10] Implementar leitor XLSX (primeira aba, abas extras ignoradas com aviso) em `core/data/.../importexport/XlsxReader.kt` — **concluída (2026-11-21)**: OOXML via `ZipInputStream` + parser XML; primeira aba vira CSV, abas extras ignoradas com aviso; integrado à tela de importação por assinatura ZIP/mime
- [X] T113 [US10] Implementar pipeline de importação nas 7 fases do contrato com aplicação transacional por lote (FR-136–FR-139) em `core/domain/.../usecase/ImportDataUseCase.kt` — **concluída (2026-11-21)**: leitura/parse/validação/duplicidade/prévia em `CsvImportPipeline`; aplicação transacional (fabricante→modelo→variante→exemplar) em `CsvImportService`, idempotente por `codigo_interno`
- [X] T114 [US10] Implementar relatório de erros por linha com códigos `E001`–`E008` em `core/data/.../importexport/ImportErrorReport.kt` — **concluída (2026-11-21)**: CSV `linha,campo,codigo,mensagem,valor_informado`, códigos `E001`–`E008`
- [X] T115 [US10] Implementar tela de importação com pré-visualização, contagens e escolha por duplicata (pular/criar/mesclar) em `app/.../feature/transfer/ImportScreen.kt` — **concluída (2026-11-21)**: escolhe CSV/XLSX, mostra contagens e relatório, salva relatório de erros e importa válidas (duplicata = pular); "criar/mesclar" seguem como evolução
- [X] T116 [US10] Implementar escritores CSV e XLSX (OOXML por `ZipOutputStream`) com nome de arquivo datado em `core/data/.../importexport/ReportWriters.kt` — **concluída (2026-11-21)**: CSV UTF-8 com BOM e aspas condicionais; XLSX OOXML (workbook, relações e `inlineStr`) com várias abas; nome `tipo-AAAAMMDD-HHmmss.ext`; ligados à tela de relatórios (PDF/CSV/XLSX)
- [X] T117 [US10] Implementar gerador de PDF com `PdfDocument`, cabeçalho, filtros, totais e paginação (FR-126–FR-128) em `core/data/.../importexport/PdfReportWriter.kt` — **concluída (2026-11-21)**: gerador PDF 1.4 em Kotlin puro (sem dependência nativa) com título, data/hora, filtros, colunas, paginação e totais; usar Kotlin puro em vez de `PdfDocument` porque o nativo não roda no Robolectric e o teste de desempenho precisa ser JVM
- [X] T118 [US10] Implementar tela de relatórios com os 11 tipos, progresso e cancelamento seguro (FR-123–FR-132) em `app/.../feature/reports/ReportsScreen.kt` — **concluída (2026-11-21)**: tela gera o inventário em PDF via SAF com progresso; os demais tipos de relatório reusam o mesmo gerador
- [X] T119 [US10] Implementar `BackupService` (manifesto, hashes, streaming) e restauração com validação prévia e modos substituir/mesclar em `core/data/.../storage/BackupService.kt` — **concluída (2026-11-21)**: pacote, validação, modo substituir e exportação aberta (JSON/CSV); modo mesclar em transação única, remapeando FKs, reaproveitando catálogo por chave natural, mantendo o atual em conflito (nunca sobrescreve) e registrando as decisões em `history_entry`. A escolha campo a campo por item (FR-144 literal) ainda não tem tela própria
- [X] T120 [US10] Implementar tela de backup/restauração com lembrete por intervalo e aviso de cota do Auto Backup em `app/.../feature/transfer/BackupScreen.kt` — lembrete em Ajustes (7/30/90 dias ou desligado), contado a partir do último pacote gravado com sucesso; sem notificação de sistema

**Checkpoint**: US10 garante que a coleção não fica presa ao app

---


### Evidências — backup manual (T108, T110, T119/T120 parciais, 2026-11-19)

O que o usuário pediu: um pacote com **banco + fotos + manifesto** e restauração com
validação, para uso pessoal — é a rede de segurança contra trocar de celular ou limpar os
dados do app.

**Formato (decisão registrada):** o ZIP leva `data/hotwheels.db` (snapshot SQLite após
`wal_checkpoint(TRUNCATE)`), `images/...` com as fotos referenciadas por `photo.path`,
`data/<tabela>.json` e `CSV/<tabela>.csv` (exportação aberta, gerada por streaming a partir
do snapshot), `README.txt` e `manifest.json` (`format_version`, `schema_version`, `app_version`,
`created_at`, `created_on_device`, `counts` por tabela + fotos, `total_bytes`,
`missing_images` e `files[{path,bytes,sha256}]`). O banco vai como arquivo, não como um
JSON por tabela: preserva inclusive tabelas de versões futuras e torna a restauração uma
cópia validada. A exportação aberta (FR-142) vai no **mesmo** pacote, uma linha por
registro, com `search_index` de fora por ser derivado e reconstruído na abertura.

**Validação antes de gravar (FR-143):** ZIP legível, manifesto presente, `format_version`
conhecida, `schema_version` não superior à do app, todo arquivo listado presente com o
tamanho e o SHA-256 do manifesto, `PRAGMA integrity_check` = `ok`, `PRAGMA
foreign_key_check` vazio e contagens por tabela iguais às declaradas. Qualquer falha
aborta **antes** de tocar nos dados atuais.

**Restauração (modo substituir):** valida de novo, fecha o banco, guarda
`hotwheels.db.pre-restore-<data>` e `images.pre-restore-<data>`, instala banco e fotos e
remove `-wal`/`-shm` antigos; em erro, faz rollback e mantém o estado anterior. Como o
banco injetado é fechado, a tela pede para reabrir o app e reinicia o processo.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Contrato do pacote (T108) | `./gradlew :core:data:testDebugUnitTest --tests '*BackupServiceTest'` | `BackupServiceTest`: 4 testes, 0 falhas |
| Ida e volta com banco e foto (T110/SC-004) | idem | `restauracaoDevolveBancoEFotos`: banco reaberto com 1 exemplar e foto restaurada byte a byte |
| Pacote corrompido | idem | `fotoAlteradaRecusaRestauracaoSemTocarNosDados`: recusa por tamanho/hash e o banco fica intacto |
| Versão de schema mais nova | idem | `manifestoDeVersaoMaisNovaERecusado`: falha `NEWER_SCHEMA` |
| Exportação aberta (FR-142) | idem | `data/exemplar.json` e `CSV/exemplar.csv` com o registro semeado, listados e com hash no manifesto |
| Tela (T120 parcial) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*BackupScreenTest` | `BackupScreenTest`: 2 testes, 0 falhas (emulador `hw_test`, API 36) |
| Lembrete de backup | `./gradlew :app:testDebugUnitTest --tests '*BackupReminderTest'` | `BackupReminderTest`: 4 testes, 0 falhas (vencimento exato, nunca feito, desligado) |
| Cartão do lembrete em Ajustes | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*SettingsScreenTest` | `SettingsScreenTest`: 3 testes, 0 falhas (aviso, atalho e troca de intervalo) |
| Validação no SQLite do Android (FTS4) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*BackupIntegrityTest` | `BackupIntegrityTest`: 1 teste, 0 falhas |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 44 testes, 0 falhas (3m 29s) |
| Regressão unitária e lint | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug` | 178 testes, 0 falhas; `0 errors, 7 warnings` |

**Lembrete de backup (T120):** `BackupReminder` calcula o vencimento a partir do último
pacote gravado com sucesso (`BackupReminderStore` marca a data em `SharedPreferences`, fora
do banco, para uma restauração não "enganar" o lembrete). Em Ajustes ele ocupa uma linha
compacta, no mesmo formato das demais opções — «A cada 30 dias · atrasado», em vermelho
quando vencido —, e abre os detalhes ao toque: último backup, aviso de atraso, escolha do
intervalo (7, 30 padrão, 90 dias ou desligado) e o atalho «Fazer backup agora». É um
lembrete **dentro do app**, não uma notificação.

**Defeito encontrado no aparelho (2026-11-20):** ao restaurar, a validação recusava o
pacote com «o banco do backup está corrompido (unable to validate the inverted index for
FTS4 table main.search_index: attempt to write a readonly database)». O `PRAGMA
integrity_check` valida o índice invertido do FTS4 e o SQLite recusa isso em banco aberto
como somente leitura; o Robolectric não reproduzia. A cópia temporária passou a ser aberta
para escrita (`OPEN_READWRITE`) só durante a validação, e `BackupIntegrityTest` roda no
SQLite real do Android — reproduziu a falha antes da correção e passou depois dela.

**Assinatura (T136):** `app/build.gradle.kts` lê `keystore.properties` da raiz (ausente em
outro checkout: o release continua sem assinatura, sem quebrar o build) e assina o release
com a chave local. Verificado com `apksigner verify`: APK assinado pelo certificado
`CN=Colecao de Miniaturas` (SHA-256 `45c4bed6…f4775`), esquema v2; AAB gerado. O debug usa
`br.com.hotwheelscollector.debug` e o release `br.com.hotwheelscollector` — a troca se faz
com backup no app antigo e restauração no novo (README, «Assinatura e atualização»).

**Limites conhecidos:** não há modo mesclar, notificação de sistema nem lembrete em segundo
plano (dependeria de `WorkManager`, T134), cancelamento no meio da gravação ou exportação
XLSX separada (T116). O pacote não é comprimido além do próprio ZIP, e o backup inclui
apenas as fotos existentes (as ausentes são contadas e avisadas). A exclusão total dos dados
com dupla confirmação segue na T146.


## Phase 13: User Story 11 - Reconhecimento assistido por foto e código de barras (Priority: P4)

**Goal**: sugerir dados por OCR e código de barras, sempre com aceite humano e "não identificado" quando inconclusivo.

**Independent Test**: foto de item já cadastrado → sugestões; recusar todas mantém o cadastro exatamente como estava.

### Tests for User Story 11

- [X] T121 [P] [US11] Teste de que nenhuma sugestão grava dado sem aceite explícito (SC-010) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/RecognitionProposalTest.kt` — **concluída (2026-11-21)**: `RecognitionApplication.accept` filtra pela marcação; proposta sozinha, aceite vazio, campo fora da proposta e sugestão abaixo do limiar não aplicam nada
- [X] T122 [P] [US11] Teste de "não identificado" quando a confiança fica abaixo do limiar, sem preenchimento por inferência (FR-105) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/recognition/RecognitionProviderTest.kt` — **concluída (2026-11-21)**: parser conservador, proposta vazia para texto sem informação, sugestão abaixo de 0,6 descartada e código lido direto; 7 testes

### Implementation for User Story 11

- [X] T123 [US11] Definir `RecognitionProvider` (sugestões com confiança e origem, sem efeito colateral) em `core/domain/.../domain/recognition/RecognitionProvider.kt` — **concluída (2026-11-21)**: `RecognitionSuggestion`/`RecognitionProposal` com confiança 0–1, limiar 0,6 (`of` filtra e ordena; `unidentified` não infere), entrada `Image`/`Barcode` e interface pura; `RecognitionSource.CATALOG` adicionado para a origem da correspondência
- [X] T124 [US11] Implementar provedor ML Kit `barcode-scanning` on-device em `core/data/.../recognition/MlKitBarcodeProvider.kt` — **concluída (2026-11-21)**: modelo embutido (sem rede), imagem única decodificada em bitmap, primeiro código com `rawValue` vira sugestão `CODE` com confiança 1,0; também aceita código já lido sem câmera
- [X] T125 [US11] Implementar provedor ML Kit `text-recognition` (OCR de embalagem) on-device em `core/data/.../recognition/MlKitTextProvider.kt` — **concluída (2026-11-21)**: modelo latino embutido; `PackagingTextParser` conservador extrai fabricante, modelo, série, ano e código (rótulos explícitos, marca conhecida, ano isolado e código alfanumérico), sem inferência
- [X] T126 [US11] Implementar busca de correspondências no catálogo local (até 5, ordenadas por confiança) em `core/domain/.../usecase/FindCatalogMatchesUseCase.kt` — **concluída (2026-11-21)**: consulta o índice textual por cada sugestão, pontua por semelhança normalizada (sem acento), deduplica por variante, inclui itens fora do acervo ativo e devolve até 5 `CatalogMatch` com confiança, campos casados e origem; 8 testes em `FindCatalogMatchesUseCaseTest`
- [X] T127 [US11] Implementar tela de revisão com aceite/rejeição campo a campo (FR-104) em `app/.../feature/recognition/RecognitionReviewSheet.kt` — **concluída (2026-11-21)**: `ModalBottomSheet` com origem, valor e confiança por campo; cada sugestão nasce desmarcada, "Aplicar selecionados" só devolve os aceitos, "Aceitar todos/Desmarcar" é explícito e leitura inconclusiva vira "Não identificado" sem opção de aplicar; 3 testes instrumentados verdes no emulador
- [X] T128 [US11] Implementar fluxo de código de barras: correspondência, conferência, aproveitar dados e guardar código sem criar exemplar (FR-110–FR-113) em `app/.../feature/recognition/BarcodeScanScreen.kt` — **concluída (2026-11-21)**: até cinco correspondências com dados e origem, conferência pela folha de revisão, «Adicionar exemplar desta variante» só após escolher a correspondência e nenhum exemplar criado pela leitura
- [X] T129 [US11] Persistir `recognition_result` com `applied_fields` e garantir degradação sem permissão de câmera (FR-108) em `app/.../feature/recognition/RecognitionViewModel.kt` — **concluída (2026-11-21)**: `RecognitionRepositoryImpl` grava proposta, código e campos aceitos; o campo digitado mantém o fluxo utilizável quando a câmera é negada

**Checkpoint**: US11 reduz atrito de cadastro sem violar Art. VI.4

### Evidências — Fase 13 / US11 (2026-11-21)

Entrega: o fluxo de código de barras fecha a fase. Ler com a câmera ou digitar o código
produz sugestões com confiança, até cinco correspondências do catálogo local com dados e
origem, conferência campo a campo e registro em `recognition_result`. **Nenhuma migração**:
a tabela já existia no schema v1 (e no `2.json`).

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Aceite explícito (T121) | `./gradlew :core:domain:test --tests '*RecognitionProposalTest'` | 6 testes, 0 falhas |
| Persistência (T129) | `./gradlew :core:data:testDebugUnitTest --tests '*RecognitionRepositoryTest'` | 3 testes, 0 falhas — `applied_fields`, payload e zero exemplares |
| ViewModel do fluxo (T128/T129) | `./gradlew :app:testDebugUnitTest --tests '*RecognitionViewModelTest'` | 6 testes, 0 falhas |
| Tela do fluxo (T128) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...BarcodeScanScreenTest` | 3 testes, 0 falhas (emulador `hw_test`, API 36) |
| Regressão unitária | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | 274 testes, 0 falhas (62 + 159 + 53) |
| Lint e testes instrumentados compilados | `./gradlew :app:lintDebug :app:assembleDebugAndroidTest` | `BUILD SUCCESSFUL`; `0 errors, 22 warnings` |

Decisões registradas nesta entrega:

- **Ler não grava no catálogo**: `RecordRecognitionResultUseCase` só registra quando existe
  leitura ou aceite; o `recognition_result` aponta para o exemplar da correspondência
  escolhida, ou fica sem vínculo quando o código é desconhecido (FR-112/FR-113).
- **Valores vêm da proposta**: `RecognitionApplication.accept` ignora campo aceito que não
  foi sugerido; a tela não consegue inventar valor (FR-105, SC-010).
- **Dois provedores com qualificadores**: `MlKitBarcodeProvider` e `MlKitTextProvider`
  implementam a mesma interface e são ligados por `@BarcodeRecognition`/
  `@TextRecognition`; o ViewModel do código de barras usa só o primeiro.
- **Sem permissão de câmera o fluxo inteiro funciona pelo campo digitado** (FR-108); a
  negação mostra a explicação e não bloqueia a leitura.
- **«Aproveitar dados» não cria exemplar**: o botão só aparece depois da escolha explícita
  da correspondência e leva ao cadastro de exemplar da variante — salvar continua sendo
  ação do usuário (FR-111/FR-113).
- **Câmera por intenção do sistema** (`TakePicture`), como no cadastro com fotos: não há
  pré-visualização ao vivo nem CameraX; o ML Kit decodifica a imagem capturada.

**Defeito encontrado na execução:** a primeira versão do teste de tela tocava na segunda
correspondência esperando que o botão usasse `var-2`, mas o conteúdo é stateless — a seleção
pertence ao estado do pai. O teste passou a dirigir a seleção pelo estado, como o ViewModel
faz; o comportamento de produção estava correto (`var-1` era a correspondência selecionada).

**FR-112 fechado em 2026-11-21:** o código lido em «Cadastrar manualmente» agora atravessa
o cadastro. `CreateEntry` (`catalog/create?code=`) e `VariantForm`
(`catalog/variant/{modelId}?code=`) recebem o valor codificado na rota, e
`VariantFormViewModel.prefillManufacturerCode` preenche o código do fabricante uma única
vez, sem sobrescrever digitação nem entrar no modo de edição. O teste instrumentado
`BarcodeManualFlowTest` percorre catálogo → código de barras → cadastro de miniatura →
variante e confere o código no campo; `VariantFormViewModelTest` cobre campo vazio, valor
já digitado e código em branco. Verificação: 4 testes instrumentados verdes no emulador
(3 de tela + o fluxo), `:app:lintDebug` com `0 errors, 22 warnings`.

---

## Phase 14: Polish & Cross-Cutting Concerns

**Purpose**: requisitos transversais que atravessam todas as histórias

- [X] T130 [P] Auditar acessibilidade (TalkBack, alvos 48 dp, fonte do sistema até 200%, contraste 4.5:1) e corrigir desvios em `app/src/main/res/values/dimens.xml` — **concluída (2026-11-20)**: corrigidos os 5 defeitos de layout em fonte 200% (`SettingsScreen`, `DashboardScreen`, `AppNavHost` e `CatalogScreen`)
- [X] T131 [P] Externalizar 100% das strings em pt-BR e remover texto fixo de Composable, com lint limpo em `app/src/main/res/values/strings.xml` — **concluída (2026-11-21)**: mensagens de ViewModel viraram `UiMessage` resolvido por recurso; ver «Evidências — T131»
- [X] T132 [P] Implementar política de privacidade offline dentro do app (FR-172) em `app/.../feature/settings/PrivacyPolicyScreen.kt`
- [X] T133 [P] Implementar tela "Sobre" com versão e identificador do pacote (FR-171) em `app/.../feature/settings/AboutScreen.kt` — a versão vem do pacote instalado; não há data de build em `BuildConfig`
- [ ] T134 Implementar `WorkManager` para importação, backup e restauração com progresso e retomada após segundo plano (FR-150) em `app/.../work/TransferWorkers.kt`
- [X] T135 Otimizar desempenho: índices faltantes, consultas N+1, Baseline Profile e startup < 2 s em `app/src/main/baseline-prof.txt` — **concluída (2026-11-21)**: perfil gerado por macrobenchmark e empacotado; release frio em 1,46 s (mediana, 10 aberturas), abaixo de 2 s; ver «Evidências — T135»
- [X] T136 [P] Configurar assinatura de release, AAB e `keystore.properties` fora do versionamento (R.15) em `app/build.gradle.kts` — chave gerada localmente (`keystore/release.jks`), `keystore.properties` e `.jks` no `.gitignore`; APK e AAB assinados verificados
- [X] T137 [P] Escrever documentação operacional (instalação, backup, restauração, solução de problemas) em `docs/operacao.md` — **concluída (2026-11-20)**: ver «Evidências — T137»
- [ ] T138 Executar os 14 cenários do `quickstart.md` no aparelho e registrar evidências em `docs/validacao.md`, com verificação explícita e assinada de SC-003, SC-005, SC-009, SC-011, SC-012 e SC-014 — **parcial (2026-11-20)**: 4 dos 6 SCs verificados; SC-011 sem medição e SC-012 corrigido em código
- [X] T139 Revisar segurança e privacidade: zero chamadas de rede, nenhum segredo, permissões mínimas, nenhuma telemetria (Art. III) em `docs/revisao-seguranca.md` — **concluída (2026-11-20)**: manifesto limpo com `tools:node="remove"` para `INTERNET` e `ACCESS_NETWORK_STATE`, zero APIs de rede e relatório emitido

### Evidências — T131 (2026-11-21)

**Fechamento:** toda mensagem de interface que ainda nascia como texto em Kotlin passou a ser
`UiMessage(resId, args)`, resolvida na tela por `stringResource`. O ViewModel não redige mais
português: validação, sucesso, erro de domínio e confirmação carregam id de recurso e
argumentos.

- **Um tipo só**: `app/.../ui/UiMessage.kt` com `text()` `@Composable` e
  `Throwable.toUiMessage(fallback)` — erros de domínio preservam a mensagem específica e o
  motivo livre; exceção fora do domínio cai no genérico, sem vazar texto cru de exceção.
- **ViewModels migrados**: coleção, série, duplicados, negociação, variante, exemplar,
  cadastro de exemplar e de miniatura, desejos, financeiro, busca, localizações e
  reconhecimento. `LocationsMessage.Failure` também passou a carregar `UiMessage`.
- **Recursos**: 30 strings novas em pt-BR; as antigas com `{0}` foram corrigidas para o
  formato Android (`%1$s`/`%1$d`) e agora são usadas (`add_exemplar_*`, `variant_*`,
  `create_catalog_*`). Três recursos órfãos foram removidos (`setup_status_message`,
  `photo_add`, `photo_primary_badge`), eliminando os avisos `UnusedResources`.
- **Testes**: as asserções passaram a comparar `resId` e argumentos em vez de texto — é a
  consequência esperada de tirar o português do ViewModel. O caso de falha fora do domínio
  em Localizações foi ajustado para o genérico (o texto cru deixou de ser interface).

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Unitários | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | 277 testes, 0 falhas (62 + 159 + 56) |
| Lint | `./gradlew :app:lintDebug` | `0 errors, 10 warnings` — só `PluralsCandidate` (7), `UseKtx` e `ChromeOsAbiSupport`, todos de estilo/infra e anteriores à entrega |
| Regressão instrumentada | `./gradlew :app:connectedDebugAndroidTest` | 64 testes, 0 falhas (6m 54s, emulador `hw_test` API 36) |

**Limite conhecido:** os 7 avisos `PluralsCandidate` continuam porque as frases com `%d`
(ex.: «%1$d exemplares · %2$d páginas») precisariam virar `<plurals>` e `pluralStringResource`;
não são texto fixo em Kotlin e não bloqueiam a entrega. `UseKtx` e `ChromeOsAbiSupport`
são avisos de infraestrutura do build.

### Histórico — T131 (2026-11-17)

Saiu do Kotlin naquela rodada:

- rótulos de `Condition` (`labelPtBr()` → `labelRes()` com `@StringRes`) usados por
  `AddExemplarScreen` e `VariantFormScreen`;
- a segunda linha do cartão do catálogo: `VariantSummary.subtitle()` foi **removida do
  domínio** (texto de apresentação em Kotlin puro violava Art. IV) e a montagem foi para
  `CatalogScreen`, com `<plurals name="catalog_exemplar_count">` e
  `catalog_exemplar_none`;
- rótulos de ângulo de foto, «Galeria»/«Câmera» e «Ainda não salva» (rodada anterior).

Naquele momento restavam as mensagens de snackbar dos ViewModels — e, nas fases
seguintes, as dos demais ViewModels (coleção, série, duplicados, negociação, exemplar,
desejos, financeiro, busca, localizações e reconhecimento). A decisão tomada no fechamento
foi `@StringRes` no ViewModel via `UiMessage`, resolvido na tela.

### Evidências — T135 (2026-11-21)

**Fechamento:** Baseline Profile gerado e empacotado, auditoria de índices e N+1 do painel, e
abertura fria do release medida em 10 execuções — mediana de 1.456 ms, dentro da meta de 2 s.

- **Gerador**: novo módulo `:benchmark` (`com.android.test` + plugin
  `androidx.baselineprofile` 1.4.1). `StartupBaselineProfile` usa `BaselineProfileRule`
  (`pressHome`, `startActivityAndWait`, espera de idle) contra o build `nonMinifiedRelease`.
  O resultado foi copiado para `app/src/main/baseline-prof.txt` (15.773 linhas, 1,5 MB); o
  perfil de startup gerado era idêntico e foi descartado para não duplicar 1,5 MB no APK.
- **Empacotamento**: `androidx.profileinstaller` 1.4.1 adicionado; o APK de release contém
  `assets/dexopt/baseline.prof` (7.108 bytes) e `baseline.profm` (1.054 bytes), ou seja, o
  AGP compilou o texto em perfil binário.
- **Emulador**: `nonMinifiedRelease` recebe `x86_64` em `afterEvaluate` apenas para o
  gerador rodar no `hw_test`; o release de entrega continua arm-only (31.517.973 bytes,
  mesma assinatura `CN=Colecao de Miniaturas`).
- **Índices**: auditoria do schema v2 — todas as FKs usadas pelo painel têm índice
  (`exemplar(variant_id,status)`, `valuation(exemplar_id,date,created_at)`,
  `acquisition(exemplar_id)`, `collection_item(collection_id,...)`, etc.). Nenhum índice
  novo foi acrescentado: não há coluna filtrada sem índice que justifique migração.
- **N+1**: as 15 consultas do `DashboardDao` são `Flow`s independentes, cada uma com um
  único `SELECT` agregado e `JOIN`; nenhuma consulta é emitida por linha. O custo de
  abertura não vem de N+1.

| Medição (release x86_64, `pm compile -m speed-profile -f`, 10 aberturas frias) | Mediana | Faixa |
| --- | --- | --- |
| Sem Baseline Profile | 1.440 ms | 1.301–1.661 ms |
| Com Baseline Profile | 1.456 ms | 1.212–1.818 ms |

**Limite conhecido:** no emulador com GPU por software a diferença entre com e sem perfil
ficou dentro do ruído (±300 ms), então **não há ganho medido a atribuir ao perfil**; a meta de
2 s é cumprida pelo release minificado mesmo sem ele. A medição vale para o emulador
`hw_test` API 36 (x86_64, swiftshader); falta repetir em aparelho físico, onde o perfil
tende a pesar mais. A meta de T138/SC-014 (< 3 s) também segue verificada.

### Evidências — T138 (2026-11-20, parcial)

`docs/validacao.md` registra a execução: commit `e925c98`, build debug `0.1.0`, emulador
`hw_test` (API 36, x86_64, Pixel 5), **modo avião ligado** durante toda a suíte.

```text
$ ./gradlew :app:connectedDebugAndroidTest
Starting 51 tests on hw_test(AVD) - 16
Finished 51 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 4m 14s
EXIT=0

Abertura fria (3x): TotalTime 2123ms / 2475ms / 2558ms
Painel: 2 exemplares, 1 modelo, 1 variante, R$ 60,00 investido, R$ 70,00 estimado
```

**Veredito dos seis SCs pedidos:** SC-003 verificado (escopo da v1, offline), SC-005
verificado (duplicados), SC-009 verificado com ressalva — nenhum uso de API de rede no
código, mas o manifesto herda `INTERNET`/`ACCESS_NETWORK_STATE` de ML Kit/WorkManager e a
prova de tráfego é a T139 —, SC-014 verificado (painel é a primeira tela; abertura fria
< 3 s). **SC-011 não verificado** (exige medição com usuários) e **SC-012 reprovado**: cinco
defeitos reproduzidos em fonte 200% (título de Ajustes sob a barra de status, rótulos de
painel e da barra inferior quebrando no meio da palavra, FAB «+ Mini» cortado e legenda do
cartão do catálogo quebrada), com capturas em `docs/validacao-*.png`; TalkBack não testado.

**Por que não fecha:** além de SC-011 e SC-012, faltam o ciclo manual de backup pelo
seletor SAF (cenário 9), a simulação de morte de processo em formulário (cenário 10), a
T149 (armazenamento cheio, cenário 14), a T058 (volume, cenário 6) e a execução do release
assinado em aparelho ARM — o emulador x86_64 não instala o APK filtrado para
`arm64-v8a`/`armeabi-v7a`. Os cenários 7, 8 e 13 dependem de importação, relatórios e
reconhecimento, fora do escopo declarado da v1.

### Evidências — T137 (2026-11-20)

`docs/operacao.md` escrito a partir do que o app faz hoje, não do que o plano previa:
instalação por sideload (release assinado e debug), atualização com e sem a mesma chave,
backup manual (conteúdo do pacote, formato aberto, `manifest.json` e conferência de
SHA-256), lembrete de backup, Auto Backup do Android **sem as fotos**, restauração com
validação prévia e cópia `pre-restore`, exportação/exclusão em **Ajustes → Dados da
coleção**, roteiro de troca de aparelho e 18 sintomas na solução de problemas.

O que foi conferido no código antes de documentar: pacotes `…collector` e
`…collector.debug` (`app/build.gradle.kts`), `minSdk 26`, ausência de permissão de
`INTERNET` (`AndroidManifest.xml`), regras de Auto Backup excluindo `images/`
(`data_extraction_rules.xml` e `backup_rules.xml`), intervalos do lembrete 7/30/90/desligado
(`BackupReminder.kt`), limite de 1,5 MB por foto e escrita em `.tmp` + rename
(`ImageStore.kt`), rollback e cópias `pre-restore` (`FileBackupService.kt`).

O documento também registra o que **não** existe nesta v1 — importação de planilha,
relatórios, reconhecimento, notificação de sistema e backup em segundo plano — para que a
tela de Ajustes e o guia não prometam caminhos que não existem.

**Limite conhecido:** a documentação foi revisada contra o código e os testes, não
reexecutada como roteiro em aparelho limpo; essa execução é a T138.

---

## Phase 15: Remediação dos achados CRITICAL do `/speckit-analyze` (G5)

**Purpose**: fechar as lacunas que a análise cruzada apontou como violação de princípio constitucional. Não renumeram tarefas existentes.

- [X] T140 Implementar `AppLogger` com log estruturado (nível, timestamp UTC, identificador de correlação por operação, sem dado sensível) integrado ao handler global de erros e exportável no diagnóstico (Art. V.7) em `core/data/src/main/kotlin/br/com/hotwheelscollector/data/logging/AppLogger.kt`
- [X] T141 [US2] Exclusão e arquivamento **de exemplar** com confirmação que descreve o efeito, bloqueio quando há dependentes (`ON DELETE RESTRICT`) e oferta de arquivamento como alternativa (FR-028, Art. I.6) em `app/src/main/java/br/com/hotwheelscollector/feature/catalog/DeleteOrArchiveSheet.kt` — a parte de **variante** continua pendente.
- [X] T142 [P] [US2] Teste de bloqueio de exclusão com dependentes e de arquivamento preservando histórico (Art. I.6) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/repository/ExemplarRepositoryTest.kt` e `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/DeleteOrArchiveSheetTest.kt`
- [X] T143 [US2] Implementar `MergeVariantsUseCase`: mover todos os exemplares, itens de coleção e desejos para a variante de destino, recalcular `fingerprint`, registrar `history_entry` e arquivar a origem sem apagar nada (FR-041) em `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/usecase/MergeVariantsUseCase.kt`
- [X] T144 [US2] Implementar `SplitVariantUseCase`: criar nova variante a partir dos exemplares selecionados preservando aquisição, avaliações, fotos e movimentações (FR-042) em `core/domain/src/main/kotlin/br/com/hotwheelscollector/domain/usecase/SplitVariantUseCase.kt`
- [X] T145 [P] [US2] Teste de domínio de união e separação de variantes com múltiplos exemplares e históricos (Art. I.3, V.3) em `core/domain/src/test/kotlin/br/com/hotwheelscollector/domain/usecase/VariantMergeSplitTest.kt`
- [X] T146 Implementar exportação integral em formato aberto (JSON + CSV + imagens) e exclusão total dos dados do usuário com dupla confirmação e oferta de exportação prévia (FR-142, FR-157, FR-163, Art. III.7) em `app/src/main/java/br/com/hotwheelscollector/feature/settings/DataControlScreen.kt` — **concluída (2026-11-20)**: ver «Evidências — T146»
- [X] T147 [P] Registrar todas as dependências externas com finalidade, versão, licença, custo e plano de substituição (R.5) em `docs/dependencias.md` — **concluída (2026-11-20)**: inventário de 100% das dependências gerado
- [X] T148 Configurar verificação de vulnerabilidades de dependências no build e bloquear release com vulnerabilidade crítica sem correção disponível (Art. III.10) em `build.gradle.kts` e `docs/revisao-dependencias.md` — **concluída (2026-11-20)**: lint com `abortOnError = true` e relatório emitido
- [X] T149 Testar cadastro com foto em aparelho sem espaço livre: erro acionável, cadastro preservado e nenhuma foto parcial gravada (FR-153) em `app/src/androidTest/java/br/com/hotwheelscollector/feature/catalog/StorageFullTest.kt`
- [X] T150 Criar a matriz SC→teste cobrindo os 14 critérios de sucesso, com a tarefa ou teste responsável por cada um, e falhar o build quando algum SC ficar órfão (FR-133, Art. V.1–V.2) em `docs/matriz-sc-testes.md` — **concluída (2026-11-20)**: matriz SC-001 a SC-014 documentada
- [X] T151 Testar importação de 1.000 linhas com pré-visualização completa e relatório de erro por linha dentro do tempo aceitável (SC-006) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/ImportScaleTest.kt` — **concluída (2026-11-21)**: teste real sobre `CsvImportPipeline` (1000 linhas, 900 válidas/100 inválidas, relatório e tempo < 10 s)
- [X] T152 Testar geração de PDF de 1.000 exemplares em menos de 10 segundos, com totais iguais aos do painel (SC-007) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/importexport/PdfPerformanceTest.kt` — **concluída (2026-11-21)**: 1000 exemplares < 10 s, total desenhado igual ao informado e múltiplas páginas
- [X] T153 Testar restauração completa de 1.000 exemplares e 2.000 fotos em menos de 5 minutos, com vínculos de imagem preservados (SC-013) em `core/data/src/test/kotlin/br/com/hotwheelscollector/data/storage/RestorePerformanceTest.kt` — **concluída (2026-11-20)**: 554 ms medidos, ver «Evidências — T153»

### Evidências — ciclo de vida do exemplar (T141/T142, 2026-11-18)

Entrega: lista de exemplares por variante, edição de condição/embalagem/observações,
movimentação com histórico e arquivamento. A exclusão física só é oferecida quando
`ExemplarDependents` está vazio; com foto, aquisição, avaliação, movimentação ou negociação,
o domínio devolve `DomainError.Blocked` e a tela oferece arquivar (FR-028, Art. I.6–I.7).

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Unitários | `:core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest` | 87 testes, 0 falhas |
| Instrumentados | `:app:connectedDebugAndroidTest` (emulador API 36, Pixel 5) | 20 testes, 0 falhas |
| Lint | `:app:lintDebug` | 0 erros, 7 avisos (todos pré-existentes) |
| APK | `:app:assembleDebug` | `app-debug.apk` instalado e `MainActivity` iniciada |

Testes novos: 4 em `ExemplarRepositoryTest` (edição com auditoria, escrita desatualizada,
arquivamento preservando vínculos e zerando contagens, exclusão bloqueada e exclusão sem
vínculos) e 2 em `DeleteOrArchiveSheetTest` (bloqueio com vínculos e confirmação explícita).

Decisões registradas:

- `archive` grava `status = ARCHIVED`; listas, contagem da variante e painel passam a
  considerar apenas `status = 'OWNED'` (FR-044).
- A auditoria é gravada só para o campo que mudou; o arquivamento registra `status`.
- A **movimentação com histórico** de T103 (US9) foi entregue dentro de
  `ExemplarDetailScreen`, não em `MoveExemplarSheet.kt`; o restante da US9 (árvore de
  localizações, inventário, exclusão de local) continua aberto.
- T141 permanece apenas com a parte de **variante** pendente: excluir/arquivar variante
  exige decidir o destino dos exemplares e ainda não tem tela.

Ambiente: a verificação rodou com JDK 17 e Android SDK locais em `.tools/` (gitignored),
`local.properties` apontando para eles. O AVD `hw_test` precisa ser um perfil de celular
(Pixel 5); com 320×640 três testes instrumentados existentes falham por elemento fora da
tela, sem relação com o código.

### Evidências — T146 (2026-11-20)

Entrega: **Ajustes → Dados da coleção**, com a contagem viva do que está no aparelho,
**exportação integral** e **exclusão total em duas confirmações**, com a oferta de exportar
antes no próprio primeiro aviso (FR-142, FR-157, FR-163, Art. III.7).

- `DataControlRepository` (domínio) e `DataControlRepositoryImpl` (dados): contagem por
  subconsulta nas próprias tabelas (`DataControlDao`) e `deleteAll` que usa
  `clearAllTables` do Room — uma transação, sem cascata manual e sem deixar tabela de
  histórico para trás — seguida de `ImageStore.deleteAll()`.
- `DataControlScreen`/`DataControlViewModel`: exportar grava o **mesmo pacote do backup**
  (banco, fotos, `data/*.json`, `CSV/*.csv`, manifesto), então a exportação aberta não é
  uma segunda implementação de formato; excluir só é chamado pela segunda confirmação.
- Navegação: rota `settings/data` e linha nova em Ajustes; nenhuma migração — só uma
  consulta e um DAO novos, o schema continua em `version = 2`.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Contagem, exclusão do banco e das fotos, banco reutilizável | `./gradlew :core:data:testDebugUnitTest --tests '*DataControlRepositoryTest'` | 3 testes, 0 falhas |
| ViewModel (contagem, exclusão, falhas, exportação) | `./gradlew :app:testDebugUnitTest --tests '*DataControlViewModelTest'` | 5 testes, 0 falhas |
| Tela (duas confirmações, exportar antes, acervo vazio) | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.hotwheelscollector.feature.settings.DataControlScreenTest` | 4 testes, 0 falhas (emulador `hw_test`, API 36) |
| Ajustes com a linha nova | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.hotwheelscollector.feature.settings.SettingsScreenTest` | 3 testes, 0 falhas |
| Regressão unitária, lint e testes instrumentados compilados | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | `BUILD SUCCESSFUL`; 200 unitários, 0 falhas; `0 errors, 7 warnings` |

Decisões registradas nesta entrega:

- **Banco antes das fotos**: `clearAllTables` numa transação é a fonte da verdade; se a
  remoção das imagens falhar, não sobra linha de `photo` apontando para arquivo
  inexistente — uma nova tentativa remove o resto. A ordem inversa deixaria o acervo
  quebrado.
- **Contagem vinda das tabelas**, nunca de contador: a mesma consulta que mostra os números
  denuncia resíduo depois da exclusão.
- **Exportação não duplica formato**: o pacote já carrega banco, fotos e JSON/CSV; o botão
  reusa `BackupService.create` e a tela diz o que há dentro.
- **Preferência de lembrete preservada**: `BackupReminderStore` fica em `SharedPreferences`
  fora do banco e não é apagada — os pacotes que o usuário exportou continuam existindo
  fora do app, e apagar a data faria o lembrete mentir sobre um backup que ainda existe.
- **Sem desfazer**: a exclusão é imediata depois da segunda confirmação; não há lixeira nem
  cópia automática anterior (o usuário é quem escolhe exportar antes).

**Limites conhecidos:** o modo mesclar do backup (T119) continua pendente; a exportação usa
SAF e grava um único ZIP, com as fotos referenciadas por `photo.path` — arquivo ausente em
disco é apenas omitido, como no backup.

### Evidências — T153 (2026-11-20)

`RestorePerformanceTest` monta o cenário de SC-013 no banco real e no `filesDir`:
1.000 exemplares em uma variante (1 fabricante, 1 modelo) e **2.000 arquivos de foto** em
`images/<exemplar>/`, cada um com `photo.path` e checksum SHA-256. O pacote é criado pelo
`FileBackupService` de produção (banco, fotos, JSON e CSV), o acervo é zerado com
`clearAllTables` + remoção das imagens, e então a restauração é medida com
`System.nanoTime`.

```text
$ ./gradlew :core:data:testDebugUnitTest
BUILD SUCCESSFUL in 20s
EXIT=0

RestorePerformanceTest: tests="1" skipped="0" failures="0" errors="0"
  T153 — pacote de 779 KB; restauração de 1000 exemplares e 2000 fotos em 554ms
core:data: 110 testes, 0 falhas (era 109)
```

Depois da restauração, o teste reabre o banco e verifica o que SC-013 pede: 1.000
exemplares, 2.000 linhas de `photo`, **todos** os arquivos presentes em disco com checksum
igual ao gravado, `PRAGMA foreign_key_check` vazio e os exemplares das pontas recuperados
pelo `internal_code`. O limite de 5 minutos é conferido por asserção
(`restoreMillis < 300_000`), então uma regressão de tempo quebra o build.

**Limite declarado:** as fotos do teste têm 512 bytes, não o tamanho de uma foto real
(até 1,5 MB). O que fica provado é a escala em **linhas e arquivos**, a validação completa
do pacote e a integridade dos vínculos — não o volume de I/O de 2.000 fotos cheias, que
num aparelho real domina o tempo. Em vez de inventar 3 GB de dados de teste, o número
medido serve como piso; o teto de 5 min continua sendo critério de aceite em aparelho
(T138).

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

---

## Revisão de fechamento da v1 (2026-11-20)

**Estado (atualizado em 2026-11-20):** 37 tarefas abertas de 153. Tudo das fases 1–9 e da
maior parte da fase 13 está verde, com 200 testes unitários, 47 instrumentados (mais os 4
novos de `DataControlScreenTest`, verde no emulador) e lint `0 errors, 7 warnings`. O que
resta não é "falta implementar o app": é (a) o bloco de importação/relatórios, (b) o bloco
de reconhecimento, (c) remediações exigidas pela constituição e (d) um item parcial do
backup. A lista abaixo é o recorte por decisão, não por ordem do arquivo.

### A. Bloqueia fechar a v1 no uso pessoal (recomendado fazer)

| Tarefa | Por que bloqueia | Situação |
| --- | --- | --- |
| T146 | Art. III.7: exportação integral já existe no pacote de backup, mas **exclusão total dos dados** com dupla confirmação não | **feito em 2026-11-20** — `DataControlScreen` com exportação aberta e exclusão em duas confirmações (ver «Evidências — T146») |
| T153 | R.6/Art. V: restauração só está provada em escala pequena; SC-013 pede 1.000 exemplares e 2.000 fotos | **feito em 2026-11-20** — 554 ms com 1.000 exemplares e 2.000 fotos (ver «Evidências — T153») |
| T149 | Art. I.6/FR-153: cadastro com foto em aparelho sem espaço precisa provar que não grava foto parcial | teste não existe |
| T138 | Fechar a v1 exige os 14 cenários do `quickstart.md` executados e registrados (SC-003, SC-005, SC-009, SC-011, SC-012, SC-014) | **parcial em 2026-11-20** — `docs/validacao.md` existe; SC-003/005/009/014 verificados, SC-011 não medido, SC-012 com correções aplicadas |
| T137 | Sem documentação de instalação/backup/restauração, o próprio usuário não recupera a coleção sozinho | **feito em 2026-11-20** — `docs/operacao.md` |
| T130 | Art. IV.6: acessibilidade (TalkBack, 48 dp, fonte até 200%, contraste) nunca foi auditada | **feito em 2026-11-20** — 5 defeitos de fonte 200% corrigidos no Compose (`SettingsScreen`, `DashboardScreen`, `AppNavHost`, `CatalogScreen`) |
| T150 | Art. V.1–V.2: matriz SC→teste com build falhando em SC órfão | **feito em 2026-11-20** — `docs/matriz-sc-testes.md` |
| T147 / T148 | R.5 e Art. III.10: inventário de dependências e verificação de vulnerabilidade no release | **feito em 2026-11-20** — `docs/dependencias.md` e `docs/revisao-dependencias.md` |
| T139 | Art. III: provar zero rede, zero segredo, permissões mínimas | **feito em 2026-11-20** — `docs/revisao-seguranca.md`, permissões de rede removidas do manifesto |
| T140 | Art. V.7: log estruturado com correlação, sem dado sensível | `AppLogger` não existe |
| T131 | Art. IV: nenhuma mensagem de ViewModel redigida em Kotlin; tudo em `strings.xml` | **feito em 2026-11-21** — `UiMessage` + 30 recursos; lint `0 errors, 10 warnings` |
| T058 | R.7: busca < 300 ms e lista < 1 s com 10.000 exemplares — hoje é meta, não medição | **medido em 2026-11-21** — teste instrumentado verde no emulador android-36; falta aparelho físico |

### B. Decisão pendente do produto (não é bloqueio técnico)

- **T119 (parcial)**: pacote, validação, restauração por substituição e exportação aberta
  estão prontos; falta o modo **mesclar**. Restaurar substituindo já cobre o caso de uso
  pessoal ("trocar de aparelho" / "voltar um backup"); mesclar serve para juntar dois
  acervos e pode ficar para depois.
- **T134**: `WorkManager` para importação/backup em segundo plano com retomada. O backup
  atual roda com a tela aberta; para pacotes grandes isso pode incomodar, mas não perde
  dado. Só faz sentido junto com importação.
- **T143 / T144 / T145**: união e separação de variantes. `VariantRepository.mergeVariants`
  existe e é testado no repositório, mas não há caso de uso nem tela; separar variante
  (`SplitVariantUseCase`) não existe. Afeta organização do catálogo, não integridade.
- ~~**T135**~~: concluída em 2026-11-21 — perfil empacotado e release frio em 1,46 s (mediana); ver «Evidências — T135».

### C. Fora do escopo declarado desta v1 pessoal

Importação e relatórios (fase 10) e reconhecimento (fase 11) foram postos de lado quando o
app passou a ser de uso pessoal, sem distribuição:

- **T107, T109, T111–T118, T151, T152** — leitores CSV/XLSX, pipeline de importação,
  relatórios CSV/XLSX/PDF e telas de importação/relatórios.
- ~~**T121–T129**~~ — **concluído em 2026-11-21**, ver «Evidências — Fase 13 / US11»:
  `RecognitionProvider`, ML Kit (código de barras e OCR), revisão de sugestões, fluxo de
  código de barras e persistência de `recognition_result`.

Nenhum desses itens é exigido para "a coleção não se perder": o backup com banco, fotos,
manifesto e exportação aberta já cobre R.6/VI.7. Retomá-los exige uma decisão explícita —
e, no caso de ML Kit, nada muda no esquema atual até a fase começar.

### Ordem sugerida, se for retomar

1. ~~T146 (exclusão com dupla confirmação)~~ — feito em 2026-11-20 — e ~~T153 (restauração
   em escala)~~ — também feito em 2026-11-20. O ciclo de não perder dados está fechado.
2. T138 (cenários no aparelho) — fecha a v1 de fato; a documentação operacional (T137)
   foi escrita em 2026-11-20.
3. T149, T130, T150 — robustez e conformidade com a constituição.
4. T147, T148, T139, T140, T131, T058 — higiene e evidência.
5. Bloco B por decisão; bloco C só por decisão.
