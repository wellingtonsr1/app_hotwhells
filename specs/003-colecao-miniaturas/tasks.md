# Tasks: Miniatura dos carrinhos na tela Coleção

**Input**: Design documents from `/specs/003-colecao-miniaturas/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/collection-card.md](./contracts/collection-card.md)

**Tests**: solicitados pela seção 8 do pedido e cobertos pela Constituição V.1–V.3; todas
as tarefas de teste estão incluídas.

**Organização**: por história de usuário, para permitir implementação e teste
independentes.

**Estado**: `[x]` = implementado e verificado neste workspace (2026-11-23), com o comando
registrado em [analysis.md](./analysis.md); `[ ]` = pendente. A marcação de concluído não
antecipa o portão G6, que exige suíte verde e limitações declaradas.

## Formato: `[ID] [P?] [Story] Descrição`

- **[P]**: pode rodar em paralelo (arquivo diferente, sem dependência pendente)
- **[Story]**: história a que a tarefa pertence (US1, US2, US3)
- Todo caminho de arquivo é relativo à raiz do repositório.

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Propósito**: confirmar o terreno antes de tocar em código.

- [x] T001 Confirmar que a miniatura e a projeção já existem, para não criar componente paralelo: `app/src/main/java/br/com/diecastcollector/ui/components/ItemThumbnail.kt` e `core/domain/src/main/kotlin/br/com/diecastcollector/domain/model/VariantSummary.kt`
- [x] T002 [P] Confirmar toolchain de build (Gradle 8.11.1, JDK 17 em `.tools/jdk-17`) em `gradle/wrapper/gradle-wrapper.properties` e `gradle.properties`

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Propósito**: base de dados, componente e decisões das quais todas as histórias dependem.

**⚠️ CRÍTICO**: nenhuma história começa antes desta fase.

- [x] T003 [P] Adicionar a string `catalog_thumbnail_no_photo` = «Sem foto do carrinho» em `app/src/main/res/values/strings.xml`
- [x] T004 Implementar fallback de falha de carregamento no marcador (`onError` → estado local) em `app/src/main/java/br/com/diecastcollector/ui/components/ItemThumbnail.kt` (D1, FR-012)
- [x] T005 Usar a descrição da T003 no ícone do marcador em `app/src/main/java/br/com/diecastcollector/ui/components/ItemThumbnail.kt` (D2, FR-004/FR-015)
- [x] T006 Registrar na Assumptions que a foto pode vir de exemplar vendido/arquivado e que a contagem representa posse em `specs/003-colecao-miniaturas/spec.md` (D3, R2)
- [x] T007 Registrar no FR-007 e nas Assumptions que o toque na miniatura amplia a imagem sem navegar em `specs/003-colecao-miniaturas/spec.md` (D5)
- [x] T008 Garantir que a listagem recebe a foto representativa na mesma consulta, com prioridade `(is_primary, sort_order, captured_at)` e sem N+1, em `core/data/src/main/kotlin/br/com/diecastcollector/data/db/dao/ItemDao.kt` e `core/data/src/main/kotlin/br/com/diecastcollector/data/repository/SupportRepositories.kt` (FR-010)

**Checkpoint**: componente, strings, decisões e consulta prontos.

---

## Phase 3: User Story 1 - Reconhecer o carrinho pela imagem (Priority: P1) 🎯 MVP

**Goal**: cada cartão mostra a foto da variante (ou marcador), com as informações ao lado.

**Independent Test**: cadastrar variantes com foto principal, sem foto e com arquivo
inválido; abrir a Coleção e conferir imagem, marcador e descrição acessível — sem usar
outra tela.

### Testes da User Story 1

- [x] T009 [P] [US1] Cobrir no teste de dados os cenários de seleção que compõem a amostra fixa do SC-002: variante com foto principal, várias fotos sem principal, sem foto e exemplar vendido com contagem zero em `core/data/src/test/kotlin/br/com/diecastcollector/data/repository/VariantRepositoryTest.kt`
- [x] T010 [P] [US1] Criar teste Compose do cartão sobre a mesma amostra: marcador acessível, descrição «modelo · cor · ano», ano ausente, arquivo inválido e lista longa sob demanda em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CatalogScreenTest.kt`

### Implementação da User Story 1

- [x] T011 [P] [US1] Exibir a miniatura de 64 dp à esquerda, quadrada, cantos de 10 dp e 12 dp de afastamento, dentro do `LazyColumn` e do `AsyncImage`/Coil existentes (carregamento sob demanda + cache padrão), em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (FR-001, FR-011)
- [x] T012 [US1] Limitar título e subtítulo a 2 linhas com reticências para altura consistente em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (D4, FR-014)
- [x] T013 [US1] Compor a descrição da miniatura com modelo, cor e ano em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (D6, FR-015)
- [x] T014 [US1] Executar `CatalogScreenTest` no emulador API 36 e registrar o resultado em `specs/003-colecao-miniaturas/analysis.md`

**Checkpoint**: US1 funcional e testável isoladamente; é o MVP.

---

## Phase 4: User Story 2 - Manter informações e navegação (Priority: P1)

**Goal**: nada do cartão some e os caminhos existentes continuam iguais.

**Independent Test**: comparar o conteúdo do cartão e percorrer cartão → exemplares para
variante com e sem foto.

### Testes da User Story 2

- [x] T015 [US2] Cobrir no teste Compose o atalho «Ver exemplares» acionando a variante do próprio cartão e o toque no título abrindo o cadastro em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CatalogScreenTest.kt`
- [x] T016 [US2] Rodar a regressão do fluxo P1 (`CatalogFlowTest`) no emulador e registrar em `specs/003-colecao-miniaturas/analysis.md` (FR-005–FR-007)

### Implementação da User Story 2

- [x] T017 [US2] Manter no subtítulo fabricante, ano opcional e contagem no plural, sem separador sobrando, em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (FR-005/FR-006)
- [x] T018 [US2] Confirmar que as rotas do cartão e do atalho permanecem inalteradas em `app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt` (FR-007)

**Checkpoint**: US1 e US2 funcionam juntas sem regressão.

---

## Phase 5: User Story 3 - Preservar busca, atalhos e inclusão (Priority: P2)

**Goal**: os caminhos de uso diário da tela continuam iguais.

**Independent Test**: abrir busca, incompletos, coleções, duplicados, código de barras e
«+ Mini».

### Testes da User Story 3

- [x] T019 [US3] Rodar a regressão de busca (`SearchScreenTest`) no emulador, que usa o mesmo `ItemThumbnail`, e registrar em `specs/003-colecao-miniaturas/analysis.md` (FR-008)
- [x] T020 [US3] Verificar no cartão/testes que os atalhos e o FAB continuam acionáveis em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (FR-008/FR-009)

**Checkpoint**: as três histórias são independentemente verificáveis.

---

## Phase 6: Polish e preocupações transversais

**Propósito**: fechar verificação, documentação e a pendência de desempenho.

- [x] T021 [P] Atualizar `specs/003-colecao-miniaturas/analysis.md` com decisões, correções e resultados de teste
- [x] T022 [P] Validar o roteiro de `specs/003-colecao-miniaturas/quickstart.md` (build, testes de dados e de UI)
- [x] T023 Medir SC-003 (500 variantes em < 1 s, rolagem fluida): `benchmark/src/main/java/br/com/diecastcollector/benchmark/CatalogBenchmark.kt` (abertura e rolagem), semente de 500 variantes em `tools/benchmark_seed.py` e instalação/semeadura em `tools/seed-benchmark-device.sh`; 2 testes verdes, abertura mediana 83 ms (Constituição V.8)
- [x] T024 [P] Registrar o resultado medido e atualizar o status de SC-003 em `specs/003-colecao-miniaturas/plan.md` e `specs/003-colecao-miniaturas/analysis.md` (depende de T023)
- [x] T025 [P] Verificar que cartão e miniatura usam apenas tokens do tema escuro (`InkBlack`, `SmokyGrey`, `FlameRedDark`) e tipografia Material 3, sem cor ou fonte nova, em `core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/Theme.kt` e `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` (FR-013; evidência em `analysis.md`)
- [x] T026 [P] [US1] Cobrir que uma lista longa (60 variantes) não compõe cartões fora da tela: o último título não existe até a rolagem em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CatalogScreenTest.kt` (FR-011; 8/8 verdes no emulador API 36)
- [x] T027 [P] [US1] Cobrir em fonte 200% (`fontScale = 2f`) que a miniatura não encolhe e o subtítulo não parte palavra ao quebrar linha, reconferindo o defeito 5 de `docs/validacao.md`, em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CatalogScreenTest.kt` (FR-014; 9/9 verdes no emulador API 36), com `maxLines = 2` e ellipsis (FR-014)
- [x] T028 [P] Fechar o portão G5 com a matriz requisito ↔ código ↔ prova ↔ princípio da Constituição em `specs/003-colecao-miniaturas/analysis.md`

---

## Dependencies & Execution Order

### Dependências de fase

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende do Setup; bloqueia todas as histórias.
- **User Stories (Phase 3+)**: dependem da Phase 2. US1, US2 e US3 podem ser feitas em paralelo, mas a validação visual compartilha o mesmo arquivo de teste, então na prática US1 → US2 → US3 em sequência.
- **Polish (Phase 6)**: T023 depende das histórias estarem completas; T024 depende de T023.

### Dependências entre histórias

- **US1 (P1)**: base do MVP; não depende de outra história.
- **US2 (P1)**: usa o cartão da US1, mas o critério (informações + navegação) é verificável sozinho.
- **US3 (P2)**: independente; usa apenas atalhos e telas já existentes.

### Dentro de cada história

- Testes antes da implementação quando possível; na prática deste workspace, T009/T010 foram escritos junto do delta e executados depois.
- Componente/consulta (Phase 2) antes do layout do cartão.
- Layout antes de rodar a suíte instrumentada.

### Oportunidades de paralelismo

- T003 e T008 em arquivos diferentes.
- T009 (core:data) e T010 (androidTest) em módulos diferentes.
- T011, T012 e T013 no mesmo arquivo — **não** paralelizar entre si.
- T021 e T022 (documentação) podem rodar em paralelo com T023.
- T025 e T026 tocam arquivos diferentes e podem rodar em paralelo com T023.

---

## Parallel Example: User Story 1

```bash
# Testes em módulos diferentes, em paralelo:
Task: "Teste de dados da foto representativa em VariantRepositoryTest.kt"
Task: "Teste Compose do cartão em CatalogScreenTest.kt"

# Depois, a implementação no cartão (mesmo arquivo, sequencial):
Task: "Miniatura 64 dp em CatalogScreen.kt"
Task: "maxLines/ellipsis em CatalogScreen.kt"
Task: "Descrição composta em CatalogScreen.kt"
```

---

## Implementation Strategy

### MVP primeiro (User Story 1)

1. Phase 1 e Phase 2 — concluídas.
2. Phase 3 — implementada e testada: miniatura, marcador e acessibilidade.
3. **Parar e validar** com `CatalogScreenTest` + `VariantRepositoryTest`.

### Entrega incremental

1. US1 → valor visual (foto ou marcador em todo cartão).
2. US2 → garante que informação e navegação não regrediram.
3. US3 → garante atalhos, busca e inclusão.
4. T023/T024 → fecha SC-003 com medição; até lá, o critério é meta.

---

## Notes

- `[P]` = arquivos diferentes, sem dependência pendente.
- `[Story]` mantém a rastreabilidade com o spec.
- Marcar uma tarefa como concluída exige evidência de execução, não apenas código alterado (Constituição V.1/V.8).
- Nenhuma tarefa cria tabela, migração, dependência ou altera backup/financeiro.
- Evitar: tarefas vagas, dois donos no mesmo arquivo, dependência cruzada que quebre a independência das histórias.
