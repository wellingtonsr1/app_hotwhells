# Tasks: Interface responsiva e adaptável

**Input**: Design documents de `specs/005-interface-responsiva/`

**Prerequisites**: [`plan.md`](./plan.md), [`spec.md`](./spec.md), [`audit.md`](./audit.md), Constituição v1.3.0

**Tests**: **obrigatórios** — a constituição (V.1, V.10, VII.14) e a spec (SC-301–SC-310) exigem verificação por classe de janela. Cada tarefa de tela tem a sua verificação declarada; as tarefas de teste por classe estão no fim de cada fase de história.

**Decisões que restringem as tarefas** (spec, *Clarifications* 2026-11-24): trilho lateral a partir de 600 dp; grade responsiva em vez de lista-detalhe; trilho fixo com os cinco destinos, sem gaveta.

## Formato: `[ID] [P?] [Story] Descrição — arquivo`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1–US5, conforme `spec.md`
- Cada tarefa traz **Aceite** (critério verificável) e **Janela** (verificação por classe: C = compacta < 600 dp, M = média 600–839 dp, E = expandida ≥ 840 dp, P = paisagem compacta ~360 dp de altura, F = fonte 200%)

## Mapa plano → tarefas

| Fase do `plan.md` | Bloco de tarefas aqui |
|---|---|
| Fase 1 — estrutura, dependência, componentes | Setup (T301–T303) + Foundational (T304–T313) |
| Fase 2 — painel, acervo, coleções | US1 (T314–T322) |
| Fase 3 — fabricantes, modelos, variantes, exemplares | US1 (T323–T324) + US2 (T325–T330) + US3 (T331–T334) |
| Fase 4 — fotografias e editor | US5 (T352–T355) |
| Fase 5 — busca, financeiro, desejos, duplicados, locais | US1 (T321–T322) + US2 (T329, T330) + US3 (T335–T339) + US4 (T348–T351) |
| Fase 6 — configurações e auxiliares | US3 (T340–T343) |
| Remediação da análise (2026-11-24) | Phase 9 (T361–T372) |
| Fase 7 — fechamento | Polish (T353–T360) |

---

## Phase 1: Setup

**Propósito**: dependência, registro e baseline antes de qualquer mudança de tela.

- [ ] T301 [P] Adicionar `androidx.compose.material3:material3-window-size-class` em `gradle/libs.versions.toml` (versão vinda do BOM, sem versão explícita) e consumi-la em `app/build.gradle.kts`. **Aceite**: `./gradlew :app:assembleDebug` compila e o import `androidx.compose.material3.windowsizeclass.calculateWindowSizeClass` resolve; nenhuma outra dependência muda. **Janela**: build apenas (sem comportamento de tela).
- [ ] T302 [P] Criar o ADR da dependência em `docs/adr-005-window-size-class.md` com contexto, alternativas (`material3-window-size-class` × `material3-adaptive` × solução caseira sobre `BoxWithConstraints`), decisão, trade-offs e consequências (R.4), e atualizar `docs/dependencias.md` e `docs/revisao-dependencias.md` (finalidade, versão, licença, custo, plano de substituição). **Aceite**: R.4, R.5 e R.17 atendidos; o ADR registra por que lista-detalhe e gaveta ficaram fora do escopo e por que `material3-adaptive` não entrou. **Janela**: não se aplica (documentação).
- [ ] T303 [P] Registrar o baseline antes da mudança, em `build/responsive/baseline/`: tamanho dos APKs debug/release, tempo de startup do macrobenchmark existente e capturas compactas de painel, catálogo, busca, formulário de exemplar e editor. **Aceite**: 5 capturas + números registrados; servem de comparação para NFR-302/303 e SC-307. **Janela**: C (411×914 dp no `hw_test`).

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Propósito**: decisão de tamanho em um lugar e os cinco componentes adaptativos. Nenhuma tela é alterada nesta fase.

- [ ] T304 Criar `app/src/main/java/br/com/diecastcollector/ui/components/WindowSizeClassLocal.kt` com a `CompositionLocal`, o helper de instalação e os derivados `isMediumOrWider`, `isExpanded` e `gridMinSize` (largura mínima de item). **Aceite**: nenhuma consulta a `DisplayMetrics`/`screenWidthDp`; valor padrão seguro (compacta) para previews e testes. **Janela**: C/M/E resolvidas pelo helper, coberto em T313.
- [ ] T305 Instalar a classificação em `app/src/main/java/br/com/diecastcollector/MainActivity.kt` a partir do contêiner da janela e publicá-la no `CompositionLocal`. **Aceite**: girar, redimensionar em janela livre e dobrar/desdobrar recalculam a classe sem recriar a atividade além do já existente; estado de tela preservado. **Janela**: C → compacta; M → média; E → expandida; verificação manual em `wm size`.
- [ ] T306 Adaptar o shell em `app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt`: `NavigationBar` em C, `NavigationRail` em M/E, mantendo as cinco abas, as rotas, `popUpTo`/`saveState`/`restoreState` e as 27 rotas de detalhe sem navegação principal. **Aceite**: os cinco destinos navegam igual; nenhuma rota nova; seleção de aba correta em ambas as formas. **Janela**: C barra inferior; M e E trilho; P trilho estreito sem cobrir conteúdo.
- [ ] T307 [P] Criar `app/src/androidTest/java/br/com/diecastcollector/testing/WindowSizeRule.kt` (renderiza o conteúdo em `Box` de 360/700/1000 dp de largura e fornece a classe correspondente). **Aceite**: usado por ≥ 3 testes; falha explícita quando a classe esperada não corresponde à largura. **Janela**: C/M/E.
- [ ] T308 [P] Criar `app/src/main/java/br/com/diecastcollector/ui/components/AdaptiveGrid.kt` (grade com `GridCells.Adaptive(minSize)` e espaçamento padrão). **Aceite**: 360 dp → 1–2 colunas; 700 dp → 2–3; 1000 dp → 3–4; nenhuma célula com largura < mínimo. **Janela**: C/M/E.
- [ ] T309 [P] Criar `app/src/main/java/br/com/diecastcollector/ui/components/FlowChipGroup.kt` (grupo de opções que quebra linha, mantendo ordem, seleção e rótulos). **Aceite**: grupo de 5 chips não sai da tela em 360 dp com fonte 200%; todos tocáveis. **Janela**: C, F, M/E.
- [ ] T310 [P] Criar `app/src/main/java/br/com/diecastcollector/ui/components/FormColumn.kt` (coluna de leitura com largura máxima e alinhamento central). **Aceite**: em 1000 dp o conteúdo fica ≤ 720 dp centralizado; em 360 dp ocupa a largura toda. **Janela**: C/M/E.
- [ ] T311 [P] Criar `app/src/main/java/br/com/diecastcollector/ui/components/AdaptableCardActions.kt` (ações em linha que quebram ou viram menu quando não couberem). **Aceite**: 3 ações de texto cabem inteiras ou passam a menu; nenhum botão cortado. **Janela**: C, F, E.
- [ ] T312 Criar `app/src/main/java/br/com/diecastcollector/ui/components/MediaDialogScaffold.kt` (contêiner de tela cheia com `WindowInsets.safeDrawing` para visualizadores de foto). **Aceite**: imagem e botão fechar nunca sob status bar/nav/câmera. **Janela**: C, P, E.
- [ ] T313 [P] Testes dos componentes em `app/src/androidTest/java/br/com/diecastcollector/ui/components/ResponsiveComponentsTest.kt`: classe recebida, colunas por largura, quebra de chips, largura de formulário e insets do contêiner. **Aceite**: 8 asserções verdes; nenhum componente é validado por captura apenas. **Janela**: C/M/E + F.

**Checkpoint**: shell e componentes prontos; nenhuma tela ainda alterada.

---

## Phase 3: US1 (P1) — Navegar e ler o acervo com conforto em qualquer tela 🎯 MVP

**Goal**: navegação adaptável funcionando de ponta a ponta e listas do acervo aproveitando a largura.

**Independent Test**: em 360, 700 e 1000 dp, percorrer as cinco abas, catálogo, coleções, locais e duplicados; verificar barra ↔ trilho, mais de uma coluna onde houver espaço e ausência de cartão de largura total em E.

- [ ] T314 [US1] Trocar a grade fixa de 3 colunas em `app/src/main/java/br/com/diecastcollector/feature/dashboard/DashboardScreen.kt` por colunas derivadas da largura (mantendo a regra de fonte ≥ 1,3× como fator adicional). **Aceite**: nenhum cartão de métrica com valor truncado; 11 indicadores distribuídos sem buraco no fim da linha. **Janela**: C 2–3; M 3–4; E 4–6; F 2.
- [ ] T315 [US1] Trocar a `LazyColumn` de cartões por `AdaptiveGrid` em `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt`. **Aceite**: cartão ≤ 70% da largura útil em E; FAB e atalhos continuam acessíveis. **Janela**: C 1; M 2; E 3.
- [ ] T316 [US1] Fazer os atalhos do cabeçalho de `app/src/main/java/br/com/diecastcollector/feature/catalog/CatalogScreen.kt` quebrarem linha em M/E e manterem rolagem horizontal apenas em C. **Aceite**: 4 atalhos visíveis sem rolagem em 700 dp; nenhum rótulo quebrado letra a letra. **Janela**: C rolagem; M/E quebra.
- [ ] T317 [US1] Aplicar `AdaptiveGrid` em `app/src/main/java/br/com/diecastcollector/feature/organization/CollectionsScreen.kt`. **Aceite**: percentual e nome continuam legíveis; nenhum cartão esticado. **Janela**: C 1; M 2; E 2–3.
- [ ] T318 [US1] Ajustar cabeçalho e ações de `app/src/main/java/br/com/diecastcollector/feature/organization/CollectionDetailScreen.kt` com `AdaptableCardActions` (3 botões no cabeçalho, 2 por linha). **Aceite**: ações inteiras e tocáveis em C/F; lista de itens em 1 coluna em C e 2 em M/E. **Janela**: C/F, M, E.
- [ ] T319 [US1] Ajustar `app/src/main/java/br/com/diecastcollector/feature/organization/SeriesScreen.kt` para usar `AdaptiveGrid` e cabeçalho que quebra. **Aceite**: criar/renomear/arquivar continuam acessíveis; lista não estica em E. **Janela**: C 1; M 2; E 2–3.
- [ ] T320 [US1] Aplicar `AdaptiveGrid` em `app/src/main/java/br/com/diecastcollector/feature/locations/LocationsScreen.kt` e em `LocationContentsScreen.kt`. **Aceite**: hierarquia de locais legível; inventário em colunas em M/E. **Janela**: C 1; M 2; E 2–3.
- [ ] T321 [US1] Aplicar `AdaptiveGrid` na lista de `app/src/main/java/br/com/diecastcollector/feature/duplicates/DuplicatesScreen.kt` e no histórico de `NegotiationHistoryScreen.kt`. **Aceite**: grupos de duplicados e histórico em colunas em E; ações por linha continuam visíveis. **Janela**: C 1; M 2; E 2–3.
- [ ] T322 [P] [US1] Testes por classe: ajustar `app/src/androidTest/java/br/com/diecastcollector/NavigationBarTest.kt` e criar `app/src/androidTest/java/br/com/diecastcollector/AppShellResponsiveTest.kt`. **Aceite**: em 360 dp existe `NavigationBar` e não existe trilho; em 700 e 1000 dp existe trilho e não existe `NavigationBar`; troca de aba preserva estado; o nó raiz de cada tela não tem rolagem horizontal (`maxValue == 0` no eixo X) nas três classes, fechando FR-303/FR-312. **Janela**: C/M/E.
- [ ] T323 [P] [US1] Testes de lista por classe em `DashboardScreenTest.kt`, `CatalogScreenTest.kt`, `CollectionDetailTest.kt` e novo `CollectionsScreenTest.kt`/`SeriesScreenTest.kt` (em `app/src/androidTest/java/br/com/diecastcollector/feature/...`). **Aceite**: contagem de colunas afirmada por largura; nenhum nó fora dos limites do contêiner; nenhuma lista/grade com rolagem horizontal no eixo X (FR-303); estado preservado ao girar e ao redimensionar em três telas críticas — `CatalogScreen` com filtro ativo, `CollectionDetail` com aba rolada e `DashboardScreen` com período selecionado (FR-304). **Janela**: C/M/E + rotação.
- [ ] T324 [P] [US1] Fluxo de cadastro em janela expandida em `app/src/androidTest/java/br/com/diecastcollector/CatalogFlowTest.kt`. **Aceite**: cadastro completo (modelo → variante → exemplar) com o mesmo número de toques ou menos que em C (SC-309). **Janela**: E principal; C como regressão.

**Checkpoint**: US1 entregue — shell e leitura adaptáveis; nada de formulário foi tocado ainda.

---

## Phase 4: US2 (P1) — Nenhum controle cortado, sobreposto ou fora de alcance

**Goal**: grupos de opções que quebram, alvos ≥ 48 dp e linhas que não empurram valores para fora.

**Independent Test**: com fonte 200% em 360 dp, abrir cadastro rápido, ficha do exemplar, folha de duplicado, formulário de modelo e de variante; todos os chips visíveis e tocáveis; alvos da grade de fotos medindo ≥ 48 dp.

- [ ] T325 [P] [US2] Trocar os dois `Row` fixos de condição por `FlowChipGroup` em `app/src/main/java/br/com/diecastcollector/feature/catalog/AddExemplarScreen.kt`. **Aceite**: 5 condições visíveis e tocáveis em C/F; seleção preservada. **Janela**: C, F, M, E.
- [ ] T326 [P] [US2] Trocar os quatro `Row` de condição/embalagem por `FlowChipGroup` em `app/src/main/java/br/com/diecastcollector/feature/catalog/ExemplarDetailScreen.kt`. **Aceite**: nenhum chip fora da tela; "não informado" de embalagem visível. **Janela**: C, F, M, E.
- [ ] T327 [P] [US2] Trocar o `Row` único de 5 condições por `FlowChipGroup` em `app/src/main/java/br/com/diecastcollector/feature/duplicates/DuplicateActionsSheet.kt`. **Aceite**: 5 chips em 2–3 linhas em C; folha rola e botões alcançáveis. **Janela**: C, F, P.
- [ ] T328 [P] [US2] Trocar o `Row` crescente de veículos reais por `FlowChipGroup` em `app/src/main/java/br/com/diecastcollector/feature/catalog/ModelFormScreen.kt`. **Aceite**: com 8 veículos cadastrados, todos alcançáveis em 360 dp; nenhum chip cortado na borda. **Janela**: C, F, M/E.
- [ ] T329 [P] [US2] Trocar `Condition.entries.chunked(3)` por `FlowChipGroup` em `app/src/main/java/br/com/diecastcollector/feature/catalog/VariantFormScreen.kt`. **Aceite**: 5 condições visíveis em C/F; pares de campos continuam alinhados. **Janela**: C, F, P.
- [ ] T330 [US2] Elevar a área de toque dos `IconButton` de 32/36 dp para ≥ 48 dp em `app/src/main/java/br/com/diecastcollector/feature/catalog/PhotoGrid.kt`, mantendo o ícone pequeno. **Aceite**: 3 botões por miniatura medindo ≥ 48×48 dp; disposição não sobrepõe a imagem de forma inaceitável. **Janela**: C, F, E (medido no teste de T337).
- [ ] T331 [US2] Corrigir `SpendingRow` em `app/src/main/java/br/com/diecastcollector/feature/finance/FinanceScreen.kt` (rótulo com `weight(1f)` + `maxLines`/ellipsis). **Aceite**: fabricante de 40 caracteres não empurra o valor para fora em 360 dp/F. **Janela**: C, F, M/E.
- [ ] T332 [P] [US2] Teste de controles em fonte 200% em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/AddExemplarScreenTest.kt` e `ExemplarDetailScreenTest.kt`. **Aceite**: todos os chips com `assertIsDisplayed` e bounds dentro da tela; 0 falhas. **Janela**: C + F.
- [ ] T333 [P] [US2] Teste de alvo mínimo em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/PhotoGridTest.kt`. **Aceite**: asserção de ≥ 48 dp de altura e largura nos três botões; SC-304. **Janela**: C (alvo independe da classe).

**Checkpoint**: US2 entregue — nenhuma opção inacessível nas telas com grupos de chips.

---

## Phase 5: US3 (P2) — Formulários e diálogos confortáveis em tablet e paisagem

**Goal**: largura de leitura, pares de campos que empilham, diálogos sem altura fixa e ações de diálogo que caibam.

**Independent Test**: abrir os 8 formulários em 1000 dp e em paisagem compacta; conteúdo ≤ 720 dp centralizado, diálogos roláveis, ações inteiras.

- [ ] T334 [P] [US3] Aplicar `FormColumn` em `app/src/main/java/br/com/diecastcollector/feature/catalog/CreateCatalogEntryScreen.kt` e `ModelFormScreen.kt`. **Aceite**: campos ≤ 720 dp centralizados em E; autocompletes alinhados à coluna. **Janela**: C largura total; M/E coluna limitada.
- [ ] T335 [P] [US3] Aplicar `FormColumn` em `app/src/main/java/br/com/diecastcollector/feature/catalog/AddExemplarScreen.kt` e `VariantFormScreen.kt`. **Aceite**: nenhum campo de 1.000 dp; `PhotoGrid` acompanha a coluna. **Janela**: C, M, E, P.
- [ ] T336 [P] [US3] Aplicar `FormColumn` em `app/src/main/java/br/com/diecastcollector/feature/organization/CollectionFormScreen.kt` e `feature/wishlist/WishlistFormScreen.kt`. **Aceite**: ações de salvar visíveis sem rolagem lateral. **Janela**: C, M, E.
- [ ] T337 [P] [US3] Aplicar `FormColumn` em `app/src/main/java/br/com/diecastcollector/feature/duplicates/NegotiationScreen.kt` e `feature/recognition/BarcodeScanScreen.kt`. **Aceite**: sugestões e formulários com largura de leitura; valores longos sem espremer rótulo. **Janela**: C/F, M, E.
- [ ] T338 [US3] Empilhar pares de campos quando a largura não comportar dois (helper aplicado em `app/src/main/java/br/com/diecastcollector/feature/catalog/AddExemplarScreen.kt`, `.../feature/catalog/VariantFormScreen.kt`, `.../feature/catalog/ExemplarDetailScreen.kt` e `.../feature/finance/FinanceScreen.kt`). **Aceite**: em 360 dp/F e em P os pares viram coluna; em ≥ 600 dp continuam lado a lado. **Janela**: C, F, P, M/E.
- [ ] T339 [US3] Trocar `Modifier.height(320.dp)` por altura relativa ao espaço do diálogo com rolagem em `app/src/main/java/br/com/diecastcollector/feature/organization/CollectionItemPicker.kt`. **Aceite**: em paisagem com 360 dp de altura o diálogo cabe, a lista rola e os botões ficam visíveis. **Janela**: P (crítico), C, E.
- [ ] T340 [US3] Ajustar as ações do diálogo de restauração em `app/src/main/java/br/com/diecastcollector/feature/transfer/BackupScreen.kt` (duas ações que empilham quando não couberem). **Aceite**: "Mesclar" e "Restaurar" inteiros em C/F. **Janela**: C, F, E.
- [ ] T341 [US3] Ajustar cabeçalhos com título + ações em `app/src/main/java/br/com/diecastcollector/feature/wishlist/WishlistScreen.kt` e `feature/catalog/IncompleteEntriesScreen.kt` (título com `weight`, badge alinhado). **Aceite**: nenhum título truncado a uma letra; badge/título sem sobreposição em C/F. **Janela**: C, F, E.
- [ ] T342 [US3] Ajustar as ações do cartão de desejo em `app/src/main/java/br/com/diecastcollector/feature/wishlist/WishlistScreen.kt` com `AdaptableCardActions`. **Aceite**: comprar/abrir link/desistir alcançáveis em C/F. **Janela**: C, F, M/E.
- [ ] T343 [P] [US3] Testes por classe dos formulários em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/CreateCatalogEntryScreenTest.kt`, `ModelFormScreenTest.kt`, `VariantEditScreenTest.kt`, `feature/wishlist/WishlistScreenTest.kt`. **Aceite**: largura ≤ 720 dp em E; campos empilhados em C/F; regressão de preenchimento preservada (IV.7). **Janela**: C, M, E, F.
- [ ] T344 [P] [US3] Teste do seletor em paisagem em `app/src/androidTest/java/br/com/diecastcollector/feature/organization/CollectionDetailTest.kt`. **Aceite**: diálogo dentro da tela e lista rolável em P. **Janela**: P.

**Checkpoint**: US3 entregue — formulários e diálogos confortáveis em tablet e paisagem.

---

## Phase 6: US4 (P2) — Busca, filtros e coleções adaptados ao espaço

**Goal**: grades com colunas responsivas e filtros em painel lateral na janela expandida.

**Independent Test**: em C/M/E, alternar lista/grade/galeria e abrir filtros; colunas coerentes com a largura, folha em C/M e painel lateral em E, filtros preservados ao girar.

- [ ] T345 [US4] Trocar `GridCells.Fixed(2)` por `AdaptiveGrid` em `app/src/main/java/br/com/diecastcollector/feature/search/CatalogViews.kt` (grade e galeria). **Aceite**: 2 colunas em C, 3–4 em M, 4–5 em E; proporção 1:1 e legenda preservadas. **Janela**: C/M/E.
- [ ] T346 [US4] Apresentar os filtros como painel lateral persistente em E e folha modal em C/M em `app/src/main/java/br/com/diecastcollector/feature/search/SearchScreen.kt`. **Aceite**: em E resultados visíveis ao lado dos filtros; em C/M a folha cobre a largura e rola. **Janela**: C, M, E.
- [ ] T347 [US4] Aplicar `FlowChipGroup` nas opções de filtro e nas linhas de ordenação/visão de `app/src/main/java/br/com/diecastcollector/feature/search/SearchScreen.kt`. **Aceite**: chips de filtro sem sair da tela; em E as linhas de ordenação quebram em vez de rolar. **Janela**: C/F, M, E.
- [ ] T348 [P] [US4] Testes de busca por classe em `app/src/androidTest/java/br/com/diecastcollector/feature/search/SearchScreenTest.kt`. **Aceite**: contagem de colunas por largura; painel lateral em E e folha em C; filtros/consulta preservados na rotação. **Janela**: C, M, E.

**Checkpoint**: US4 entregue — busca e filtros adaptados.

---

## Phase 7: US5 (P3) — Fotografias e editor proporcionais

**Goal**: grade de fotos adaptativa, visualizador com insets e editor calculando o painel pelo espaço real.

**Independent Test**: em E e em P, abrir grade de fotos e editor; fotos em 3:4 sem distorção, painel rolável, rodapé acima da barra.

- [ ] T349 [US5] Trocar `photos.chunked(2)` por grade adaptativa em `app/src/main/java/br/com/diecastcollector/feature/catalog/PhotoGrid.kt`, mantendo `aspectRatio(3f/4f)`. **Aceite**: colunas por largura; nenhuma foto distorcida; ordem dos ângulos preservada. **Janela**: C 2; M 3; E 4.
- [ ] T350 [US5] Usar `MediaDialogScaffold` no visualizador de `PhotoGrid.kt` e em `app/src/main/java/br/com/diecastcollector/ui/components/ItemThumbnail.kt`. **Aceite**: imagem e botão fechar dentro dos insets em P e E. **Janela**: C, P, E.
- [ ] T351 [US5] Derivar a altura do painel do espaço real do diálogo em `app/src/main/java/br/com/diecastcollector/feature/photoeditor/PhotoEditorScreen.kt` (substituir `screenHeightDp × 0,40`), preservando a folga do rodapé da spec 004. **Aceite**: em P o painel rola e o rodapé fica acima da barra; em E a prévia aproveita o espaço. **Janela**: C, P, M, E.
- [ ] T352 [P] [US5] Testes de foto por classe em `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/PhotoGridTest.kt` e `feature/photoeditor/PhotoEditorScreenTest.kt`. **Aceite**: colunas por largura; sem distorção (proporção assertada); rodapé com folga ≥ 8 dp da barra. **Janela**: C/M/E + P.

**Checkpoint**: US5 entregue — fotos e editor proporcionais.

---

## Phase 8: Polish e verificação final

- [ ] T353 Varredura de resíduos no `app/src/main/java/br/com/diecastcollector`: nenhum `GridCells.Fixed`, `height(<dp>)` de conteúdo listável, `chunked(` em grade, `Row` de chips sem quebra e `fillMaxWidth` de formulário sem `FormColumn`. **Aceite**: busca retorna 0 ocorrências justificadas; exceções registradas com comentário no código. **Janela**: C/M/E revisados por grep + inspeção.
- [ ] T354 [P] Extrair `ProgressBar` (duplicado em `CollectionsScreen.kt`/`DashboardScreen.kt`) e `MetricCard` (`DashboardScreen.kt`/`FinanceScreen.kt`) para `core/ui` e reutilizar. **Aceite**: uma única definição de cada; testes existentes verdes. **Janela**: sem mudança visual em C/M/E (comparar capturas).
- [ ] T355 [P] Medir NFR-301/302/303 e registrar em `docs/validacao.md`: reorganização < 1 s, perfil de lista/busca e tamanho do APK (+≤ 1 MB). **Aceite**: números antes/depois com comando usado. **Janela**: C (medição) e E (reorganização).
- [ ] T356 [P] Preencher o checklist de varredura das **37 linhas** de `audit.md` §2 (**30 telas/rotas + 11 diálogos/folhas** agrupados em 7 linhas) em `build/responsive/checklist.md`, com captura de antes/depois por classe em `build/responsive/` (painel, catálogo, busca, formulário, ficha, fotos, editor em C/M/E/P). **Aceite**: 37 linhas com classe × resultado × arquivo de captura; nenhum recorte/sobreposição visível; SC-301 e SC-302 comprovados por checklist. **Janela**: C/M/E/P.
- [ ] T357 [P] Preencher `specs/005-interface-responsiva/analysis.md` com a matriz de rastreabilidade FR ↔ tela ↔ teste (Anexo B) e a cobertura de SC-301…SC-310. **Aceite**: todo FR com teste ou evidência; nenhum requisito órfão. **Janela**: cobertura das 4 classes registrada.
- [ ] T358 Rodar a suíte completa e registrar: `./gradlew :app:testDebugUnitTest :app:lintDebug` e `./gradlew :app:connectedDebugAndroidTest`. **Aceite**: suíte verde; nenhum teste removido, ignorado ou afrouxado (SC-307). **Janela**: C/M/E nos testes instrumentados.
- [ ] T359 Confirmar ausência de mudança fora da apresentação: `git diff --stat -- core/domain core/data app/src/main/java/br/com/diecastcollector/data` vazio e `applicationId`/`app_name` inalterados. **Aceite**: SC-308 e FR-329/330 verificados por comando. **Janela**: não se aplica.
- [ ] T360 Registrar limitações da verificação (dobra física — o multi-window de T372 cobre duas janelas, não hardware dobrável —, emulador de tablet, janela livre) em `specs/005-interface-responsiva/analysis.md`. **Aceite**: cada limitação com o motivo e o que ficou não verificado, citando T372 como evidência parcial de FR-325. **Janela**: D e janela livre listadas como pendência.

---

## Phase 9: Remediação da análise (2026-11-24)

**Propósito**: fechar as lacunas encontradas em `/speckit-analyze` — verificação por classe das telas restantes (VII.14/V.10), FR-314, FR-317, FR-319, diálogos sem rolagem, as telas da fase 6 do plano, acessibilidade (VII.10/FR-326/FR-327) e a dobra (FR-325). Cada tarefa executa junto da história indicada pelo seu rótulo, antes de T358.

- [ ] T361 [P] [US1] Testes por classe das listas restantes em `app/src/androidTest/java/br/com/diecastcollector/feature/locations/LocationsTest.kt`, `.../feature/duplicates/DuplicatesFlowTest.kt`, `.../feature/catalog/IncompleteEntriesTest.kt` e `.../feature/finance/FinanceScreenTest.kt`. **Aceite**: 1 coluna em C, ≥ 2 em M/E; nenhum nó fora dos limites do contêiner; fecha VII.14 para as quatro telas. **Janela**: C/M/E.
- [ ] T362 [P] [US3] Testes por classe das telas auxiliares em `.../feature/settings/SettingsScreenTest.kt`, `.../feature/settings/DataControlScreenTest.kt`, `.../feature/transfer/BackupScreenTest.kt` e novos `ImportScreenTest.kt`, `ReportsScreenTest.kt`, `AboutPrivacyScreenTest.kt`. **Aceite**: conteúdo ≤ 720 dp em E; ações visíveis; fecha VII.14 para as seis telas. **Janela**: C/M/E + P nos diálogos.
- [ ] T363 [P] [US2] Testes por classe de reconhecimento em `.../feature/recognition/BarcodeScanScreenTest.kt` e `RecognitionReviewSheetTest.kt`. **Aceite**: valor longo não espreme o rótulo; folha rola e mantém ações visíveis em C/F/P. **Janela**: C, F, P.
- [ ] T364 [P] [US5] Teste por classe de `app/src/androidTest/java/br/com/diecastcollector/feature/photoeditor/PhotoReviewDialogTest.kt`. **Aceite**: as quatro ações de revisão inteiras e tocáveis em C/F/P; fecha VII.14 para o diálogo. **Janela**: C, F, P.
- [ ] T365 [US3] Tratar teclado (IME) com `imePadding` no contêiner de `app/src/main/java/br/com/diecastcollector/ui/components/FormColumn.kt` e aplicar em `.../feature/catalog/CreateCatalogEntryScreen.kt`, `AddExemplarScreen.kt`, `VariantFormScreen.kt` e `.../feature/wishlist/WishlistFormScreen.kt`. **Aceite**: com o IME aberto a ação primária continua visível e o conteúdo rola, sem esconder o campo em edição (FR-314). **Janela**: C, F, P.
- [ ] T366 [US3] Manter menus dentro da janela em `.../feature/organization/SeriesScreen.kt` e `CollectionFormScreen.kt` (limite de altura e rolagem no seletor). **Aceite**: menu com 30 séries abre inteiro na tela e rola (FR-319). **Janela**: C, P, E.
- [ ] T367 [US3] Dar rolagem e altura relativa ao espaço a `VariantTargetDialog`/`SplitExemplarsDialog` em `.../feature/catalog/VariantFormScreen.kt` e a `SeriesFormDialog` em `.../feature/organization/SeriesScreen.kt`. **Aceite**: em P o conteúdo rola e confirmar/cancelar ficam visíveis (FR-316). **Janela**: C, P, F.
- [ ] T368 [US3] Ajustar `.../feature/recognition/RecognitionReviewSheet.kt`: rolagem própria, ações fixas no fim e largura de leitura em E. **Aceite**: folha com 6 sugestões rola e "Aplicar" alcançável em C/P (FR-317). **Janela**: C, P, E.
- [ ] T369 [US3] Aplicar `FormColumn` e cabeçalho adaptável em `.../feature/settings/SettingsScreen.kt`, `.../feature/reports/ReportsScreen.kt`, `.../feature/transfer/ImportScreen.kt`, `.../feature/settings/DataControlScreen.kt`, `AboutScreen.kt` e `PrivacyPolicyScreen.kt`. **Aceite**: conteúdo ≤ 720 dp em E; os 8 cartões de ajuste não esticam; a política mantém linha de leitura (FR-308, FR-311). **Janela**: C/M/E.
- [ ] T370 [US3] Fazer `PhotoCaptureActions` em `.../feature/catalog/PhotoCapture.kt` empilhar os dois botões quando não couberem. **Aceite**: "Galeria" e "Câmera" inteiros e tocáveis em C/F. **Janela**: C, F, M/E.
- [ ] T371 [P] [US2] Verificar acessibilidade por classe com `app/src/androidTest/java/br/com/diecastcollector/ResponsiveAccessibilityTest.kt` e um helper de contraste (`app/src/androidTest/java/br/com/diecastcollector/ContrastAssert.kt`, razão calculada sobre as capturas de T356). **Aceite**: contraste ≥ 4,5:1 em texto normal e ≥ 3:1 em texto grande; todo ícone com `contentDescription` significativo ou `null` explícito em ação decorativa; ordem de foco igual à ordem de leitura; rótulo truncado carrega descrição completa (FR-326, FR-327, Art. VII.10). **Janela**: C/M/E + F.
- [ ] T372 [P] [US1] Testar duas janelas e dobra com `app/src/androidTest/java/br/com/diecastcollector/MultiWindowTest.kt`, executando os cenários com o app em split-screen primário e secundário (`adb shell am start --windowingMode 4`/`5`). **Aceite**: cada janela recebe a classe pela largura do próprio contêiner (não da tela); nenhum conteúdo sob barras do sistema ou sob a divisória; troca de classe reordena em < 1 s sem recarregar dados (FR-301, FR-304, FR-325, NFR-301). **Janela**: duas janelas C+C, C+M e M+E.

---

## Dependencies & Execution Order

### Fases

- **Setup (T301–T303)**: sem dependência; pode começar imediatamente.
- **Foundational (T304–T313)**: depende do Setup; **bloqueia todas as histórias**. T306 depende de T304/T305; T313 depende de T307–T312.
- **US1 (T314–T324)**: depende de T304–T308, T310, T311, T306. Entrega o MVP.
- **US2 (T325–T333)**: depende de T309 (FlowChipGroup) e T306; independente de US1.
- **US3 (T334–T344)**: depende de T310 e T311; T338 depende de T334–T337.
- **US4 (T345–T348)**: depende de T308 e T309.
- **US5 (T349–T352)**: depende de T308 e T312.
- **Polish (T353–T360)**: depende das histórias incluídas na entrega.
- **Phase 9 (T361–T372)**: executa junto da história do seu rótulo (T361/T372 em US1; T363/T371 em US2; T362/T365–T370 em US3; T364 em US5) e **antes de T358** (suíte final), para que a verificação por classe cubra todas as telas alteradas; T371 depende de T356 (capturas), T372 depende de T306.

### Histórias

- US1, US2, US3, US4 e US5 são independentes entre si depois da fase Foundational.
- Ordem sugerida de entrega: US1 (MVP) → US2 → US3 → US4 → US5.
- Nenhuma história altera domínio, banco ou offline; portanto nenhuma depende de migração.

### Paralelismo

- T301/T302/T303 em paralelo.
- T307–T312 em paralelo (arquivos distintos), depois T313.
- Dentro de US1: T314–T321 em paralelo por arquivo; T322–T324 em paralelo.
- Dentro de US2: T325–T329 em paralelo; T330/T331 em arquivos distintos; T332/T333 em paralelo.
- Dentro de US3: T334–T337 em paralelo; T339/T340/T341/T342 em arquivos distintos.
- Polish: T354–T357 e T359 em paralelo; T358 por último.

## Parallel Example: US2

```bash
# grupos de chips em arquivos diferentes
Task: "FlowChipGroup em AddExemplarScreen.kt (T325)"
Task: "FlowChipGroup em ExemplarDetailScreen.kt (T326)"
Task: "FlowChipGroup em DuplicateActionsSheet.kt (T327)"
Task: "FlowChipGroup em ModelFormScreen.kt (T328)"
Task: "FlowChipGroup em VariantFormScreen.kt (T329)"
```

## Implementation Strategy

### MVP (Setup + Foundational + US1)

1. T301–T303 (Setup) → T304–T313 (Foundational).
2. T314–T321 (telas de leitura) → T322–T324 (testes por classe).
3. **PARAR e VALIDAR** em C/M/E: navegação, colunas, estado preservado.
4. Entregar: já resolve "app esticado no tablet".

### Entrega incremental

1. US2 → valida em C/F que nenhum controle fica cortado.
2. US3 → valida formulários em M/E e paisagem.
3. US4 → valida busca e filtros.
4. US5 → valida fotos e editor.
5. Polish → evidências, medições e rastreabilidade antes de fechar a feature.

## Notes

- Nenhuma tarefa toca `core/domain`, `core/data`, Room, migrações, `applicationId` ou `app_name`.
- Testes são obrigatórios nesta feature (V.10): cada tela alterada tem ao menos uma asserção por classe tocada.
- Ajustes de asserção existentes são permitidos apenas onde a forma de navegação mudou (barra → trilho); remover cobertura é proibido (V.4).
- Cada tarefa deve caber em um commit pequeno; seguir a disciplina de alteração da constituição (identificar arquivos, dependências, risco, alteração mínima, testes).
