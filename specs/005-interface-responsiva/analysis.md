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
| SC-301 | 100% das 37 linhas do inventário nas 3 classes + P | T356 (checklist), T361–T364, T322–T324, T343–T344, T348, T352 | **Parcial**: checklist com **37/37 linhas aprovadas por teste por classe** (T361–T364, T367); capturas por classe não geradas |
| SC-302 | E com > 1 coluna e cartão ≤ 70% | T315, T317, T323, T345, T356 | **Parcial**: colunas em E provadas por teste (2/4/5 na busca, 4 na grade de fotos); cartão ≤ 70% medido só em `FormColumn` (720 dp), não por cartão |
| SC-303 | Compacta + fonte 200% com ações alcançáveis | T332, T343, T362, T371 | **Parcial**: coberto por tela nas telas alteradas (`AddExemplar`, `Dashboard`, `CollectionDetail`, `Wishlist`); T356/T371 fecham a varredura |
| SC-304 | 100% dos alvos ≥ 48 × 48 dp | T333, T371 | **Parcial**: grade de fotos em 48 dp provada em T333; verificação global depende de T371 |
| SC-305 | Zero falhas de inset/corte | T312, T313, T350, T352, T372 | **Parcial**: insets do visualizador e do editor cobertos por asserção; dobra física não medida |
| SC-306 | ≤ 720 dp em E | T310, T343, T362 | **Parcial**: `FormColumn` medido em 360/1.000 dp (720 dp exatos) e aplicado às telas de ajuste/auxiliares em T369; falta a verificação de dobra |
| SC-307 | Suíte e detector de resíduos verdes | T358, T303 | **Atendido**: 148 testes instrumentados, 327 unitários e lint sem erros; varredura de T353 sem resíduos injustificados |
| SC-308 | Zero arquivos de domínio/dados | T359 | **Atendido**: `git diff --stat 000721b -- core/domain core/data app/.../data` vazio |
| SC-309 | Cadastro em tablet com ≤ toques de C | T324 | **Pendente**: sem teste de contagem de toques |
| SC-310 | < 1 s sem perder estado | T355, T372 | **Parcial**: reorganização da grade em 130 ms (NFR-301); redimensionamento em duas janelas depende de T372 |

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

1. T371 **não depende mais** de T356: o contraste é calculado das cores do tema em `ContrastAssert` (luminância relativa da WCAG), em vez de amostrar pixels das capturas. Isso mantém a verificação determinística, mas mede os pares de cor declarados — não o que cada tela renderiza com sobreposições e imagens de fundo.
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
- Cobertura SC: **10/10** com evidência declarada; 2 atendidos (SC-307, SC-308), 7 parciais (SC-301–SC-306, SC-310) e 1 pendente (SC-309)
- Achados da análise inicial: **18** — 1 crítica, 7 altas, 7 médias, 3 baixas
- Achados fechados: **15** (C1–C14, I1, A1, I2); 3 baixos registrados sem tarefa
- IDs duplicados em `tasks.md`: **0**

---

## 9. Execução do Polish (T353–T360, 2026-11-24)

### 9.1 T353 — varredura de resíduos

| Padrão procurado | Ocorrências | Situação |
|---|---|---|
| `GridCells.Fixed` | 0 em código (só o comentário de `AdaptiveGrid`) | Limpo |
| `chunked(` em grade | `DashboardScreen` (colunas por classe), `NegotiationHistoryScreen` (1/2 colunas), `PhotoGrid` (2/3/4) | Dinâmico, sem resíduo |
| `height(<dp>)` de conteúdo listável | Nenhuma; só `Spacer`, barra de progresso e marcadores | Limpo |
| `Row` de chips sem quebra | `PhotoGrid` (9 ângulos, rolagem horizontal) | **Exceção comentada no código** |
| `fillMaxWidth` de formulário sem `FormColumn` | Nenhuma nas telas de formulário | Limpo |

A varredura converteu três `Row` de chips restantes em `FlowChipGroup`
(`ReportsScreen`, `NegotiationScreen`, `CollectionItemPicker`). O seletor de ângulos da grade
de fotos mantém `horizontalScroll` com justificativa explícita: quebrar 9 ângulos em linhas
ocuparia mais altura que a própria miniatura.

### 9.2 T354 — componentes extraídos

`ProgressBar` e `MetricCard` agora existem uma única vez, em
`core/ui/src/main/kotlin/br/com/diecastcollector/ui/components/Metric.kt`, e são reutilizados
por `DashboardScreen`, `FinanceScreen`, `CollectionsScreen` e `CollectionDetailScreen`.
`MetricCard` recebeu `valueFirst` (painel mostra número antes do rótulo) e `tag` (o painel
mantém `DashboardTags.METRIC`). A suíte cobre os quatro consumidores
(`DashboardScreenTest`, `FinanceScreenTest`, `ListsResponsiveTest`, `CollectionDetailTest`).

### 9.3 T355 — medições

Registradas em `docs/validacao.md` §9:

- **NFR-301**: reorganização da grade 1 → 5 colunas em **130 ms** (limite 1 s).
- **NFR-303**: APK debug **+138.356 B (+0,13 MB)**; release **+12.288 B (+0,01 MB)** contra
  o commit `000721b`.
- **NFR-302**: abertura da Coleção com 500 variantes em **147 ms** de mediana (baseline 83 ms;
  diferença dentro do ruído do emulador por software). Rolagem não medida: o `swiftshader`
  não produz os slices de renderização exigidos pelo `FrameTimingMetric`.

### 9.4 T356 — checklist das 37 linhas

`build/responsive/checklist.md`: **37/37 linhas aprovadas por teste por classe** depois do
lote da Phase 9 (T361–T364, T367), sem nenhuma linha aprovada sem teste que exercite a
classe ou o contêiner. **As capturas
por classe não foram geradas**; é a lacuna que mantém SC-301/SC-302 parciais e que também
bloqueia o cálculo de contraste de T371 sobre imagens.

### 9.5 T358 — suíte completa

| Comando | Resultado |
|---|---|
| `./gradlew :app:testDebugUnitTest :core:domain:test :core:data:test` | 76 + 74 + 177 = **327 testes**, 0 falhas, 0 ignorados |
| `./gradlew :app:connectedDebugAndroidTest` | **167 testes**, 0 falhas, 0 ignorados |
| `./gradlew :app:lintDebug` | **0 erros**, 23 avisos |

Nenhum teste foi removido, marcado como `@Ignore` ou teve asserção afrouxada: o número de
testes instrumentados subiu (68 em 2026-11-21 → 167) e as três classes alteradas por este
lote (`PhotoGridTest`, `PhotoEditorScreenTest`, `ResponsiveComponentsTest`) só ganharam casos.

### 9.6 T359 — portão de não intrusão

```bash
git diff --stat 000721b -- core/domain core/data app/src/main/java/br/com/diecastcollector/data
# (vazio)
git diff 000721b -- app/build.gradle.kts app/src/main/res/values/strings.xml | grep -E "^[+-].*(applicationId|app_name)"
# (vazio)
```

`applicationId = "br.com.diecastcollector"` e `app_name = Diecast Collector` seguem os mesmos;
nenhum arquivo de domínio, dados ou Room foi tocado.

### 9.7 T360 — limitações acrescentadas

- **Capturas por classe ausentes (T356)**: o checklist prova por asserção de limites, não por
  imagem. Bloqueia SC-301/SC-302 e o contraste medido sobre capturas de T371.
- **Rolagem de 500 variantes**: falha no emulador com `swiftshader`
  (`Observed no renderthread slices in trace`); a metade "rolagem fluida" de NFR-302 e SC-003
  continua pendente de aparelho físico.
- **Dobra física**: nenhum aparelho dobrável; T372 cobre duas janelas emuladas, não a dobra
  de hardware (FR-325).
- **Janela livre (freeform)**: não é o modo padrão do AVD; só tamanhos discretos foram
  exercitados.
- **TalkBack**: semântica e foco são verificados por asserção; não há audição real (T371).
- **Abertura em emulador por software**: números de abertura fria (4,3–7,9 s no debug) não
  representam o build de release com Baseline Profile (1,456 s medido em 2026-11-21).
