# Análise de Consistência e Rastreabilidade — Interface Responsiva

**Data**: 2026-11-24 | **Artefatos**: [spec](./spec.md) · [audit](./audit.md) · [plan](./plan.md) · [tasks](./tasks.md) · Constituição v1.3.0

**Comando**: `/speckit-analyze` (somente leitura) em 2026-11-24, seguido de dois lotes de remediação aprovados.

**Estado**: 47 requisitos analisados, 72 tarefas, **0 requisito órfão**, 1 achado crítico fechado, 2 lotes de remediação registrados (§4 e §5).

---

## 1. Veredito

| Dimensão | Situação |
|---|---|
| Constituição (Art. VII, V.10, R.4, R.17, R.18) | Conforme após remediação — VII.14 coberto por T313, T322–T324, T332–T333, T343–T344, T348, T352, T361–T364 |
| Cobertura FR → tarefa | 33/33 FRs com tarefa de implementação ou de verificação |
| Cobertura SC → evidência | 10/10 SCs com evidência declarada; 2 dependem da varredura de T356 |
| Consistência spec ↔ plano ↔ tarefas | Sem contradição; FR-302 alinhado à decisão Q3; contagem de telas unificada em 37 linhas |
| Escopo | Nenhuma tarefa toca domínio, dados, banco, `applicationId` ou `app_name` (T359 é o portão) |

---

## 2. Matriz FR ↔ tela ↔ teste

Legenda: **Impl.** = tarefa que altera a tela/componente; **Evidência** = tarefa que verifica. Classes: C/M/E/P/F conforme `tasks.md`.

| FR | Tema | Tela / arquivo | Impl. | Evidência | Janela |
|---|---|---|---|---|---|
| FR-301 | Classificação de janela | `MainActivity.kt`, `ui/components/WindowSizeClassLocal.kt` | T304, T305 | T313, T372 | C/M/E + duas janelas |
| FR-302 | Barra ↔ trilho | `navigation/AppNavHost.kt` | T306 | T322, T372 | C/M/E/P |
| FR-303 | Sem sobreposição, corte ou rolagem horizontal | `AppNavHost.kt` + listas | T306, T315–T321 | T322, T323 | C/M/E |
| FR-304 | Estado preservado | shell e telas (rolagem, filtros, texto, seleção) | T305, T306, T324 | T323 (3 telas críticas), T343, T348, T372 | C/M/E + rotação |
| FR-305 | Detalhe sem barra, voltar alcançável | 27 rotas de detalhe do `AppNavHost` | T306 | T322, T324 | C/M/E |
| FR-306 | Colunas por largura | `AdaptiveGrid.kt`, `DashboardScreen`, `CatalogScreen`, `Collections`, `Series`, `Locations`, `Duplicates`, `CatalogViews`, `PhotoGrid` | T308, T314, T315, T317, T319–T321, T345, T349 | T313, T323, T348, T352, T361 | C/M/E |
| FR-307 | Grade em M/E, sem lista-detalhe | `AdaptiveGrid` + telas de lista | T308, T315–T321 | T323, T348, T356 | C/M/E |
| FR-308 | Largura de leitura ≤ 70% | `AdaptiveGrid`, `FormColumn`, telas de formulário | T308, T310, T334–T339, T369 | T323, T343, T362 | C/M/E |
| FR-309 | Cabeçalho título + ações | `CatalogScreen`, `CollectionDetailScreen`, `WishlistScreen`, `IncompleteEntriesScreen` | T316, T318, T341, T369 | T323, T361, T362 | C/F, M/E |
| FR-310 | Texto não empurra valor | `FinanceScreen.SpendingRow`, `BarcodeScanScreen`, `NegotiationScreen` | T331, T337, T338 | T361, T363 | C/F, M/E |
| FR-311 | Formulário ≤ 720 dp centrado | `FormColumn.kt` + 11 telas de formulário/ajuste | T310, T334–T339, T369 | T343, T362 | C/M/E |
| FR-312 | Pares de campos empilham | `AddExemplar`, `VariantForm`, `ExemplarDetail`, `Finance` | T338 | T343, T322 | C/F/P, M/E |
| FR-313 | Chips quebram linha | `FlowChipGroup.kt`, `AddExemplar`, `ExemplarDetail`, `DuplicateActionsSheet`, `ModelForm`, `VariantForm`, `SearchScreen` | T309, T325–T329, T347 | T313, T332, T348, T363 | C/F, P, M/E |
| FR-314 | Ação primária com teclado | `FormColumn` (`imePadding`) + 4 formulários | T365 | T365 (teste com IME), T343 | C/F/P |
| FR-315 | Regra de negócio idêntica | formulários (sem mudança de lógica) | T334–T339 | T343, T358 | C/M/E |
| FR-316 | Diálogos sem altura fixa | `CollectionItemPicker`, `BackupScreen`, `VariantTargetDialog`, `SplitExemplarsDialog`, `SeriesFormDialog` | T339, T340, T367 | T344, T362, T367 | C/P/E |
| FR-317 | Folhas longas rolam | `DuplicateActionsSheet`, `SearchScreen` (filtros), `RecognitionReviewSheet` | T327, T346, T368 | T332, T348, T363 | C/F/P |
| FR-318 | Painel lateral em E, folha em C | `SearchScreen.kt` | T346 | T348 | C/M/E |
| FR-319 | Menus dentro da janela | `SeriesScreen`, `CollectionFormScreen` | T366 | T366, T362 | C/P/E |
| FR-320 | Editor pelo espaço real | `PhotoEditorScreen.kt` | T351 | T352 | C/P/M/E |
| FR-321 | Proporção preservada | `PhotoGrid.kt`, `ItemThumbnail.kt` | T349, T350 | T352 | C/M/E |
| FR-322 | Grade de fotos 3:4 | `PhotoGrid.kt` | T349 | T352 | C/M/E |
| FR-323 | Visualizador com insets | `MediaDialogScaffold.kt`, `PhotoGrid`, `ItemThumbnail` | T312, T350 | T313, T352 | C/P/E |
| FR-324 | Alvos ≥ 48 dp | `PhotoGrid.kt` e botões de ícone | T330 | T333, T371 | C (independe) |
| FR-325 | Insets e dobra | `MediaDialogScaffold`, `PhotoGrid`, `PhotoEditorScreen` | T312, T350, T351 | T313, T352, T372 | C/P/E + duas janelas |
| FR-326 | Contraste, semântica, foco, fonte | tokens existentes + `FlowChipGroup`/`AdaptableCardActions` | T309, T311 | T371, T332 | C/M/E + F |
| FR-327 | Rótulo essencial e descrição acessível | `SpendingRow`, cabeçalhos, chips | T331, T341 | T371, T361 | C/F/E |
| FR-328 | Identidade visual preservada | `core/ui` (extração sem mudança) | T354 | T356 (capturas), T358 | C/M/E |
| FR-329 | Só apresentação | — | T359 (portão) | T359, T358 | — |
| FR-330 | Nome, `applicationId`, identificadores | — | T359 (portão) | T359 | — |
| FR-331 | Nada removido ou escondido | todas as telas | T306–T351 | T358, T356 (checklist 37 linhas) | C/M/E |
| FR-332 | Componentes compartilhados | `ui/components/*` (5 componentes) | T304, T308–T312, T354 | T313, T353 | C/M/E |
| FR-333 | Sem bloqueio de orientação/resize | `AndroidManifest.xml` (inalterado) | — | T353, T359 (ausência de mudança) | — |
| NFR-301 | Reorganização < 1 s | classe de janela e composição | T304–T306 | T355, T372 | C/E |
| NFR-302 | Sem degradar fluxos da 003 | todas as telas adaptadas | T303 (baseline) | T355, T358 | C/E |
| NFR-303 | APK +≤ 1 MB | `app/build.gradle.kts` | T301, T303 | T355 | build |
| NFR-304 | Dependência com ADR | `gradle/libs.versions.toml`, `docs/adr-005-window-size-class.md` | T301, T302 | T301 (build), T302 (documento) | — |

**FRs sem tarefa de implementação** (por serem proibições ou portões, não lacunas): FR-329, FR-330, FR-333. Todos têm caminho de verificação declarado (T359, T353).

---

## 3. Cobertura de SC-301…SC-310

| SC | Critério | Tarefas de evidência | Situação |
|---|---|---|---|
| SC-301 | 100% das 37 linhas do inventário nas 3 classes + P | T356 (checklist), T361–T364, T322–T324, T343–T344, T348, T352 | Planejado; depende de T356 |
| SC-302 | E com > 1 coluna e cartão ≤ 70% | T315, T317, T323, T345, T356 | Planejado |
| SC-303 | Compacta + fonte 200% com ações alcançáveis | T332, T343, T362, T371 | Parcial: coberto por tela nas telas alteradas; T356 fecha a varredura |
| SC-304 | 100% dos alvos ≥ 48 × 48 dp | T333, T371 | Planejado |
| SC-305 | Zero falhas de inset/corte | T312, T313, T350, T352, T372 | Planejado (dobra física fica como limitação em T360) |
| SC-306 | ≤ 720 dp em E | T310, T343, T362 | Planejado |
| SC-307 | Suíte e detector de resíduos verdes | T358, T303 | Planejado |
| SC-308 | Zero arquivos de domínio/dados | T359 | Planejado |
| SC-309 | Cadastro em tablet com ≤ toques de C | T324 | Planejado |
| SC-310 | < 1 s sem perder estado | T355, T372 | Planejado |

---

## 4. Histórico de remediação — primeiro lote (C1–C8, I1, A1, I2)

| ID | Severidade | Achado | Resolução |
|---|---|---|---|
| C1 | **Crítica** | VII.14/V.10: verificação por classe só em ~15 telas | **T361** (4 listas), **T362** (6 telas auxiliares), **T363** (barcode + folha), **T364** (PhotoReviewDialog) |
| C2 | Alta | FR-314 sem tarefa (teclado) | **T365**: `imePadding` em `FormColumn` + 4 formulários, com teste |
| C3 | Alta | FR-319 sem tarefa (menus) | **T366**: `SeriesScreen`/`CollectionFormScreen` |
| C4 | Alta | FR-316 incompleto (diálogos de variante e série) | **T367** |
| C5 | Alta | FR-317 sem `RecognitionReviewSheet` | **T368** |
| C6 | Alta | Fase 6 do plano só com Backup | **T369**: 6 telas de configuração/auxiliares |
| C7 | Alta | `PhotoCaptureActions` sem tarefa | **T370** |
| C8 | Alta | Plano exigia ADR; T302 registrava só em `dependencias.md` | T302 reescrita: ADR em `docs/adr-005-window-size-class.md` (R.4) |
| I1 | Alta | FR-302 admitia gaveta, contra a decisão Q3 | FR-302 reescrita: "Gaveta MUST NOT" |
| I2 | Média | Contagem divergente (25/30/37) | Key Entities e SC-301 padronizados |
| A1 | Média | "largura confortável" sem número | FR-306 (160–180 dp) e FR-311 (720 dp) |

## 5. Histórico de remediação — segundo lote (C9–C14)

| ID | Achado | Resolução | Onde |
|---|---|---|---|
| C9 | FR-303 sem verificação de rolagem horizontal | Asserção `maxValue == 0` no eixo X | T322, T323 |
| C10 | FR-304 sem teste por tela | 3 telas críticas nomeadas com o estado a preservar | T323 |
| C11 | VII.10 sem verificação de contraste/semântica/foco | `ResponsiveAccessibilityTest` + `ContrastAssert` | T371 |
| C12 | FR-327 sem descrição acessível | Asserção de rótulo truncado com descrição completa | T371 |
| C13 | FR-325 sem teste de dobra/duas janelas | `MultiWindowTest` em split-screen 4/5 | T372; T360 registra a limitação física |
| C14 | SC-301 sem varredura global | Checklist das 37 linhas com captura por classe | T356; SC-301 e Key Entities ajustados |

**Achados baixos não convertidos em tarefa** (registrados, sem bloqueio): C15 (FR-331 depende da suíte + checklist), C16 (FR-333 verificado por ausência de mudança no manifesto em T353/T359), C17 (T360 é governança, não requisito).

**Correções de consistência adicionais**: mapa do plano → tarefas com a linha da Phase 9 (T361–T372) e correção da linha da Fase 7 para `Polish (T353–T360)`.

---

## 6. Dependências críticas da rastreabilidade

1. T371 depende de T356 (capturas para o cálculo de contraste).
2. T372 depende de T306 (shell adaptável) e valida FR-301/FR-304/FR-325/NFR-301 em duas janelas.
3. T361–T364 executam antes de T358, para que a suíte final já inclua a verificação por classe de todas as telas.
4. T359 fecha FR-329, FR-330 e SC-308 por comando; sem ele, a matriz fica sem prova de não intrusão.

---

## 7. Limitações da verificação

Registradas aqui e atualizadas por T360 após a execução:

| Limitação | Efeito | Mitigação |
|---|---|---|
| Sem hardware dobrável | Dobra física de FR-325 não medida | T372 cobre duas janelas emuladas; T360 registra a pendência |
| Emulador de tablet é `wm size`/`wm density` | Densidade e recorte podem diferir de aparelho real | T303/T356 registram as dimensões usadas |
| Janela livre (freeform) não é o padrão do `hw_test` | Redimensionamento contínuo testado só em cenários discretos | T305 + T372 registram os tamanhos exercitados |
| Sem teste de acessibilidade com TalkBack real | Semântica e foco verificados por asserção, não por audição | T371 usa asserções de semântica/ordem de foco |

---

## 8. Métricas

- Requisitos: **47** (33 FR + 4 NFR + 10 SC)
- Tarefas: **72** (T301–T372), todas com Aceite e Janela
- Cobertura FR: **33/33** (30 com tarefa de implementação + 3 com portão de verificação)
- Cobertura SC: **10/10** planejada (2 dependem de T356)
- Achados da análise inicial: **18** — 1 crítica, 7 altas, 7 médias, 3 baixas
- Achados fechados: **15** (C1–C14, I1, A1, I2); 3 baixos registrados sem tarefa
- IDs duplicados em `tasks.md`: **0**
