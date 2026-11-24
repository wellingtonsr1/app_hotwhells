# Auditoria de responsividade — inventário, achados e matriz de diagnóstico

**Feature**: `005-interface-responsiva` | **Data**: 2026-11-24 | **Base**: código em `main` (commit `a06dd19`) e Constituição v1.3.0

**Como esta auditoria foi feita**: leitura do código de apresentação (`app/src/main/java/br/com/diecastcollector/**`), do `AppNavHost.kt`, do `MainActivity.kt`, do `app/build.gradle.kts`, do `gradle/libs.versions.toml` e da suíte `app/src/androidTest`. **Não** houve execução em tablet, dobrável ou emulador com outras densidades: os achados marcados **Comprovado** vêm do código (dimensão fixa, linha sem quebra, ausência de consulta à largura) ou de comentário do próprio autor registrando o incidente; os marcados **Risco** são consequência estrutural provável, ainda não reproduzida em aparelho. Nada foi medido em tela grande — esta etapa é só documentação.

## 1. Stack e arquitetura encontradas

| Item | Valor |
|---|---|
| Linguagem / UI | Kotlin 2.1.0, Jetpack Compose (BOM `2025.01.00`), Material 3 |
| Módulos | `app`, `core:domain`, `core:data`, `core:ui`, `benchmark` |
| Navegação | `androidx.navigation:navigation-compose` 2.8.5, rotas em `Destination` (32 rotas) com um `Scaffold` externo em `AppNavHost` |
| Persistência | Room 2.6.1 (SQLite), DataStore, WorkManager 2.10.0 — **fora do escopo** desta feature |
| SDK | `compileSdk`/`targetSdk` 36, `minSdk` 26, `applicationId br.com.diecastcollector` |
| Testes instrumentados | 30 classes em `app/src/androidTest` (Compose UI Test, Espresso), emulador API 36 |
| **APIs de tamanho de janela** | **nenhuma**: não há `WindowSizeClass`, `calculateWindowSizeClass`, `material3-window-size-class` nem `material3-adaptive` nas dependências |
| Uso de `LocalConfiguration` | `DashboardScreen` (fontScale) e `PhotoEditorScreen` (screenHeightDp × 0,40) — únicos dois pontos que consultam a configuração |

**Navegação (fato)**: `AppNavHost` desenha `NavigationBar` com cinco abas sempre que a rota atual é de primeiro nível (`dashboard`, `catalog`, `wishlist`, `finance`, `settings`); `contentWindowInsets = WindowInsets(0,0,0,0)` e o `NavHost` recebe o `padding` do `Scaffold`. As demais 27 rotas são telas de detalhe sem barra inferior.

**Componentes reutilizáveis existentes**: `ItemThumbnail` (`ui/components`), `PhotoGrid`/`PhotoTile`, `PhotoCaptureActions`, `ManufacturerAutocomplete`/`ModelAutocomplete`, `CatalogViews` (grade/lista/galeria da busca), `GroupedCatalogViews`, `ProgressBar` (duplicado em `CollectionsScreen` e `DashboardScreen`), `MetricCard` (duplicado em `FinanceScreen` e `DashboardScreen`).

## 2. Inventário de telas

Prioridade da coluna **Classe afetada**: `C` = compacta, `M` = média, `E` = expandida, `P` = paisagem (altura curta), `D` = dobrável.

| # | Tela (arquivo) | Rota | Estrutura atual | Classe afetada | Situação |
|---|---|---|---|---|---|
| 1 | `DashboardScreen` | `dashboard` | `Scaffold` + `TopAppBar` + `LazyColumn`; grade de 11 métricas em **3 colunas fixas** (`COLUMNS = 3`), reduzida a 2 só quando `fontScale ≥ 1,3` via `LocalConfiguration` | C (fonte grande), M, E | Esticada em E; só reage a fonte, não a largura |
| 2 | `CatalogScreen` | `catalog` | `LazyColumn` de uma coluna; atalhos em `Row` com `horizontalScroll`; `ExtendedFloatingActionButton` | C, M, E | Cartões de largura total em E; rolagem horizontal de atalhos |
| 3 | `SearchScreen` + `CatalogViews` + `GroupedCatalogViews` | `catalog/search` | Campo de busca, três `LazyRow` de chips, resultados em grade/lista/galeria; **grade e galeria com `GridCells.Fixed(2)`**; filtros em `ModalBottomSheet` | C, M, E, P | 2 colunas fixas em E; filtros sempre em folha |
| 4 | `CreateCatalogEntryScreen` | `catalog/create` | `Column` + `verticalScroll`, três campos `fillMaxWidth` + autocompletes | M, E | Campos de largura total |
| 5 | `VariantFormScreen` / `VariantEditScreen` | `catalog/variant/{id}`, `.../edit` | `Column` + `verticalScroll`; pares de campos em `Row` com `weight(1f)`; chips de condição em `Condition.entries.chunked(3)`; `PhotoGrid`; 3 `AlertDialog` (foto, arquivar, mesclar/dividir) | C (fonte ≥1,3), M, E, P | Chips em `Row` sem quebra; formulário sem largura máxima |
| 6 | `ModelFormScreen` | `catalog/model/{id}` | `Column` + `verticalScroll`; campos `fillMaxWidth`; **veículos reais em `Row` de `FilterChip` sem quebra/rolagem** (lista dinâmica) | C, M, E | Chips crescem para fora da tela |
| 7 | `AddExemplarScreen` | `exemplar/add/{id}` | `Column` + `verticalScroll`; **condições em dois `Row` fixos** (`take(3)`/`drop(3)`); pares de campos; botão `fillMaxWidth` | C (fonte ≥1,3), M, E | Chips sem quebra; formulário sem largura máxima |
| 8 | `IncompleteEntriesScreen` | `catalog/incomplete` | `LazyColumn`; cartão com `Row` título + badge `SpaceBetween`, título sem `weight`/`maxLines` | C (fonte grande) | Risco de colisão título/badge |
| 9 | `ExemplarListScreen` | `catalog/variant/{id}/exemplars` | `LazyColumn` de cartões full width; FAB | M, E | Lista de uma coluna em E |
| 10 | `ExemplarDetailScreen` | `exemplar/{id}` | `Column` + `verticalScroll` (ficha longa); **chips de condição e embalagem em 4 `Row` fixos**; pares de campos; `PhotoGrid`; histórico e `AlertDialog`s | C (fonte ≥1,3), M, E, P | Chips sem quebra; ficha esticada em E |
| 11 | `CollectionsScreen` | `catalog/collections` | `LazyColumn`; cabeçalho `Row` título + 2 `TextButton`; cartão com `ItemThumbnail` + percentual | C (fonte grande), M, E | Uma coluna em E; cabeçalho pode comprimir |
| 12 | `CollectionDetailScreen` | `collections/{id}` | `LazyColumn`; **cabeçalho com 3 `TextButton` em `Row`**; linhas com 2 `TextButton`; `AlertDialog`; `CollectionItemPicker` | C (fonte grande), M, E, P | Botões em linha sem quebra |
| 13 | `CollectionFormScreen` | `collections/form` | `Column` + `verticalScroll`, campos `fillMaxWidth`, `DropdownMenu`, `Row` de botões | M, E | Campos de largura total |
| 14 | `SeriesScreen` | `catalog/series` | `Column` + `LazyColumn`; cabeçalho seletor + voltar; `AlertDialog` com campo e menu | C, M, E | Uma coluna em E |
| 15 | `BarcodeScanScreen` | `catalog/barcode` | `Column` + `verticalScroll`; pares de botões `weight(1f)`; sugestões em `Row` (rótulo `weight(1f)` + valor sem limite); cartões de correspondência | C (fonte grande), M, E | Valor longo pode espremer o rótulo |
| 16 | `DuplicatesScreen` + `DuplicateActionsSheet` | `duplicates` | `LazyColumn`; linha com `Row` de ações; **folha com `Condition.entries` em um único `Row` (5 chips)** e disponibilidade em 2 `Row` | C, M, E, P | 5 chips em linha estouram em C |
| 17 | `NegotiationScreen` + `NegotiationHistoryScreen` | `duplicates/{id}/negotiation` | `Column` + `verticalScroll`; campos `fillMaxWidth`; `Row` de chips; `AlertDialog`s | C, M, E | Formulário sem largura máxima |
| 18 | `WishlistScreen` | `wishlist` | `Column` + `LazyColumn`; cabeçalho `Row` título + botão; cartão com **até 3 `TextButton` em `Row`** | C, M, E | Ações podem estourar em C |
| 19 | `WishlistFormScreen` | `wishlist/form` | `Column` + `verticalScroll`; campos `fillMaxWidth`; chips de prioridade em `Row`; pares de preço | C, M, E | Formulário sem largura máxima |
| 20 | `FinanceScreen` | `finance` | `Column` + `verticalScroll`; métricas em 2 colunas; **`SpendingRow` = `Row` `SpaceBetween` sem `weight`/`maxLines` no rótulo**; pares de campos de período | C (fonte grande), M, E | Rótulo longo colide com o valor |
| 21 | `LocationsScreen` | `locations` | `LazyColumn`; linha com `ItemThumbnail` + título; 2 `AlertDialog` com campo | C, M, E | Uma coluna em E |
| 22 | `LocationContentsScreen` | `locations/{id}` | `LazyColumn`; cabeçalho com título `weight(1f)` + voltar | M, E | Uma coluna em E |
| 23 | `SettingsScreen` (`SettingsRoute`) | `settings` | `Column` + `verticalScroll`; 8 cartões `SettingsRow`; `AlertDialog` de lembrete com 4 `TextButton` | C, E | Cartões de largura total em E |
| 24 | `BackupScreen` | `settings/backup` | `Column` + `verticalScroll`; botões full width; **`AlertDialog` com `confirmButton` = `Row` de 2 `TextButton`** | C (fonte grande), M, E | Botões podem estourar em C |
| 25 | `ReportsScreen` | `settings/reports` | `Column` + `verticalScroll`; 3 `FilterChip` em `Row`; botão full width | M, E | Esticada em E |
| 26 | `ImportScreen` | `settings/import` | `Column` + `verticalScroll`; cartões de prévia/resultado | M, E | Esticada em E |
| 27 | `DataControlScreen` | `settings/data` | `Column` + `verticalScroll`; 2 `AlertDialog` de confirmação em cascata | C, M, E | Esticada em E |
| 28 | `AboutScreen` | `settings/about` | `Column` + `verticalScroll` | M, E | Esticada em E |
| 29 | `PrivacyPolicyScreen` | `settings/privacy` | `Column` + `verticalScroll` | E | Linhas muito longas em E |
| 30 | `PhotoEditorDialog` (`feature/photoeditor`) | modal | `Dialog` (`decorFitsSystemWindows = false`), insets inferidos por 3 fontes + reserva; **`panelMaxHeight = screenHeightDp × 0,40` via `LocalConfiguration`** | P, M, E | Altura do painel não vem do contêiner |
| 31 | `PhotoGrid` + `PhotoViewerDialog` | componente | `photos.chunked(2)` em `Row` com `aspectRatio(3f/4f)`; **`IconButton`s de 32 dp**; visualizador `fillMaxSize` sem insets | C (alvo), M, E, P | 2 colunas fixas; alvos < 48 dp |
| 32 | `ItemThumbnail` | componente | `Box` de `size` fixo (64 dp padrão) + `Dialog` `fillMaxSize` sem insets | E, P | Miniatura pequena em E; visualizador sob insets |
| 33 | `CollectionItemPicker` | modal | `AlertDialog` com **`LazyColumn` de `height(320.dp)` fixo** | P (altura ~360 dp), C | Altura fixa em diálogo |
| 34 | `RecognitionReviewSheet`, `SearchScreen` (filtros), `DuplicateActionsSheet` | modal | `ModalBottomSheet` + `verticalScroll` | M, E | Folha cobre a tela em E |
| 35 | `PhotoEditDialog`, `PhotoReviewDialog`, `DeleteOrArchiveSheet`, `VariantTargetDialog`, `SplitExemplarsDialog`, `SeriesFormDialog`, `ArchiveExemplarDialog` | modais | `AlertDialog`/`Dialog`; listas de opções em `Column` sem rolagem | P, C | Conteúdo pode exceder a altura |
| 36 | `Autocomplete` (`SuggestionCard`) | componente | `Card` com linhas clicáveis `fillMaxWidth` | M, E | Esticada em E |
| 37 | `MainActivity` + `AppNavHost` | shell | `enableEdgeToEdge()`, `Scaffold` com `NavigationBar` inferior fixa e `contentWindowInsets = 0` | M, E, D | Sem trilho/gaveta |

## 3. Matriz de diagnóstico

Legenda de evidência: **C** = comprovado no código/registro do autor; **R** = risco estrutural; **O** = oportunidade de melhoria.

| Tela ou componente | Problema identificado | Classe afetada | Solução proposta | Prioridade |
|---|---|---|---|---|
| `AppNavHost` (shell) | Barra inferior de 5 abas em qualquer largura; nenhuma consulta à classe de janela (**C**) | M, E, D | Calcular `WindowSizeClass` no host; `NavigationBar` em C, `NavigationRail` em M/E (gaveta se necessário), mantendo os 5 destinos e o estado por aba | P1 |
| `AppNavHost` | Conteúdo recebe largura total da janela; nada limita área de leitura (**C**) | E | Aplicar coluna de conteúdo com largura máxima no nível do host/por tela | P2 |
| `CatalogScreen` | Lista de 1 coluna com cartões de largura total em E (**C**); atalhos em rolagem horizontal (**C**, comentário do autor) | M, E | Grade responsiva de cartões por largura mínima; atalhos que quebram linha em E | P1 |
| `SearchResultsGrid` / `SearchResultsGallery` | `GridCells.Fixed(2)` (**C**) | M, E | `GridCells.Adaptive(minSize)` com mínimo confortável | P1 |
| `SearchScreen` (filtros) | `ModalBottomSheet` sempre (**C**) e `LazyRow` de chips em E (**C**) | E | Painel lateral persistente em E; quebra de linha dos chips quando couber | P2 |
| `DashboardScreen` `IndicatorGrid` | 3 colunas fixas; reage só a `fontScale ≥ 1,3` via `LocalConfiguration` (**C**) | C (fonte), M, E | Colunas por largura mínima; manter regra de fonte como fator adicional | P2 |
| `AddExemplarScreen` | Condições em 2 `Row` fixos `take(3)`/`drop(3)` (**C**) | C (fonte ≥1,3) | Grupo de chips com quebra de linha (`FlowRow`/layout customizado) | P1 |
| `ExemplarDetailScreen` | Chips de condição e embalagem em 4 `Row` fixos (**C**) | C (fonte) | Mesmo componente de grupo de chips | P1 |
| `DuplicateActionsSheet` | `Condition.entries` (5 chips) em um único `Row` (**C**) | C | Grupo de chips com quebra | P1 |
| `ModelFormScreen` | Veículos reais em `Row` crescente, sem quebra/rolagem (**C**) | C, M | Chip group com quebra; opcional: campo de seleção com busca quando a lista crescer | P1 |
| `VariantFormScreen` | `chunked(3)` em `Row` sem quebra; pares de campos com `weight` (**C**) | C (fonte), P | Grupo de chips com quebra; pares que empilham quando não couberem | P1 |
| `PhotoGrid` | `chunked(2)` fixo com `aspectRatio(3:4)` (**C**) | M, E | Grade por largura mínima mantendo 3:4 | P2 |
| `PhotoGrid` (`PhotoItemCard`) | `IconButton`s de 32 dp/36 dp (**C**) | C, todas | Elevar alvo de toque a ≥ 48 dp (mantendo o ícone pequeno) | P1 |
| `ItemThumbnail` / `PhotoViewerDialog` | Diálogo `fillMaxSize` sem tratamento de insets (**C**); miniatura fixa de 64 dp (**C**) | P, E | Aplicar insets no visualizador; permitir tamanho maior por classe | P2 |
| `CollectionItemPicker` | `LazyColumn` com `height(320.dp)` fixo dentro de `AlertDialog` (**C**) | P | Altura relativa ao espaço disponível (`heightIn(max = …)`), rolagem própria | P1 |
| `PhotoEditorScreen` | `panelMaxHeight` por `screenHeightDp × 0,40` (**C**), não pelo contêiner | P, M, E | Calcular altura do painel pelo espaço real do diálogo (`BoxWithConstraints`), mantendo a folga do rodapé | P2 |
| `FinanceScreen` `SpendingRow` | `Row` `SpaceBetween` sem `weight`/`maxLines` no rótulo (**C**) | C (fonte) | Rótulo com `weight(1f)` + `maxLines`/ellipsis; valor alinhado à direita | P1 |
| `WishlistScreen` (ações do cartão) | Até 3 `TextButton` em `Row` sem quebra (**C**) | C (fonte) | Ações em linha que quebra ou menu "mais ações" | P2 |
| `CollectionDetailScreen` (cabeçalho e linhas) | 3 e 2 `TextButton` em `Row` sem quebra (**C**) | C (fonte) | Quebra de linha ou ícones com rótulo acessível | P2 |
| `IncompleteEntriesScreen` | Título + badge `SpaceBetween`, título sem `weight` (**C**) | C (fonte) | Título com `weight(1f)` e `maxLines` | P3 |
| `BackupScreen` (`AlertDialog` de restauração) | `confirmButton` com `Row` de 2 `TextButton` (**C**) | C (fonte) | Empilhar ações quando a largura não comportar duas | P3 |
| `BarcodeScanScreen` (sugestões) | `Row` rótulo `weight(1f)` + valor sem limite (**C**) | C (fonte) | Valor com `weight`/`maxLines` e quebra vertical opcional | P3 |
| Todas as telas de formulário (13, 4–7, 17, 19, 25–28) | `fillMaxWidth` sem largura máxima (**C**) | M, E | Componente de coluna de formulário com largura máxima e alinhamento central | P2 |
| Todas as listas (`LazyColumn`) em M/E | Uma coluna de cartões full width (**C** para 1, 2, 6, 9, 11, 14, 16, 18, 21, 22, 23) | M, E | Grade/lista responsiva compartilhada | P2 |
| `ModalBottomSheet` (filtros, duplicado, reconhecimento) | Folha ocupa a tela inteira em E (**C**) | M, E | Painel lateral/diálogo amplo em E, folha em C | P3 |
| Cabeçalhos com título + ações (11, 12, 15, 16, 20, 21, 23, 24, 26) | `Row` com `SpaceBetween` e botões sem quebra (**C**) | C (fonte) | Padrão de cabeçalho adaptável (ações viram ícones/menu) | P2 |
| `ProgressBar` e `MetricCard` | Componentes duplicados em dois arquivos (**C**) | — | Extrair para `core:ui`/`ui/components` ao adaptar (II.5) | P3 |
| Dobrável / duas janelas | Nenhum tratamento de dobra ou de janela dividida; app fixa um único `Scaffold` por atividade (**R**) | D, E | Deixar a classificação vir do contêiner; testar em janela redimensionada; evitar largura física da tela | P2 |
| Paisagem de celular | Telas longas com `verticalScroll` (ok), mas diálogos com altura fixa e editor com painel em 40% da altura (**C** para o editor, **R** para os diálogos) | P | Regras de altura relativa e rolagem nos diálogos | P1 |
| Suíte de testes | 30 classes instrumentadas, nenhuma verifica classe de janela (**C**) | — | Acrescentar verificação por classe (testes com tamanho fixo e asserções de coluna/insets) | P1 |
| Acessibilidade | Alvos de 32 dp na grade de fotos (**C**); ausência de teste de alvo mínimo global (**C**) | C | Corrigir alvos e cobrir com teste | P1 |

## 4. O que **não** é problema (verificado)

- **Não há distorção de imagem**: todas as imagens usam `ContentScale.Crop` (miniaturas, grade, galeria) ou `ContentScale.Fit` (visualizadores) — proporção preservada.
- **Não há bloqueio de orientação** nem restrição de redimensionamento declarada no manifesto.
- **Formulários longos já rolam** (`verticalScroll`) e listas já usam `LazyColumn`/`LazyVerticalGrid` — não há lista preguiçosa dentro de `verticalScroll` (o `PhotoGrid` usa `Row`/`Column` justamente por isso, conforme comentário no código).
- **Telas de detalhe não exibem a barra inferior**, o que já é o padrão esperado para navegação adaptável.
- **`enableEdgeToEdge()` está aplicado** e o editor de fotografia já trata insets com três fontes independentes + reserva.
- **Textos de listas já têm `maxLines` + `TextOverflow.Ellipsis`** na maioria dos cartões (catálogo, busca, exemplares, desejos), evitando estouro vertical; o problema está na linha com valor à direita em finanças/coleções, não no cartão inteiro.

## 5. Limitações desta auditoria

1. **Nenhuma execução em tablet, dobrável, paisagem ou densidade diferente** — os itens "P" e "D" são riscos derivados do código, não reproduções.
2. **Não foi medido** o tamanho de fonte real em 200% nem o comportamento do teclado; a análise de fonte é estrutural (ausência de quebra de linha).
3. **Não existe baseline visual** para telas grandes; a verificação futura precisa registrar capturas antes/depois por classe de janela.
4. **`GroupedCatalogViews`, `PhotoReviewDialog`, `NegotiationScreen` e `LocationsScreen`** foram lidos por busca estrutural (linhas de layout), não integralmente; nenhum achado desta matriz depende de detalhe não verificado.
5. **Dobra física** não pode ser simulada no emulador atual (`hw_test`, API 36, celular): a validação de dobrável depende de emulador de dobrável ou aparelho.
6. A auditoria **não** avalia desempenho por classe de janela (NFR-301/302 serão medidos na implementação).

## 6. Dúvidas da auditoria — fechadas em 2026-11-24

As três dúvidas abertas na primeira versão desta auditoria foram resolvidas pela proposta padrão de cada uma. As decisões estão registradas também em `spec.md` (seção *Clarifications*, sessão 2026-11-24) e são a base dos requisitos FR-302, FR-303 e FR-307.

| ID | Dúvida | Decisão | Consequência na implementação |
|---|---|---|---|
| Q1 | Em janela média (600–839 dp), trilho lateral já na média ou barra inferior até 840 dp? | **Trilho lateral a partir da classe média**; barra inferior apenas na compacta (< 600 dp). | O corte de navegação acontece em 600 dp, não em 840 dp. Os cinco destinos, as rotas e o estado por aba ficam inalterados; o shell passa a ter duas formas (barra ↔ trilho). |
| Q2 | Em janela expandida, listas viram grade de 2–3 colunas ou lista-detalhe (lista + painel ao lado)? | **Grade responsiva** para acervo, busca, coleções, locais, duplicados e fotos. **Lista-detalhe fica fora desta feature.** | Nenhum painel duplo novo: as telas de detalhe continuam sendo destinos próprios (catálogo → exemplar, coleção → itens). A adaptação usa apenas colunas adicionais com largura mínima confortável, o que preserva o fluxo de navegação atual e a regra de não reconstruir telas. Adotar lista-detalhe depois exige especificação própria, porque muda navegação, não só apresentação. |
| Q3 | Barra inferior em janela expandida pode virar gaveta, ou o trilho fica fixo com os cinco destinos? | **Trilho fixo com os cinco destinos atuais.** | Nenhum destino novo e nenhuma gaveta nesta feature. Gaveta só entra se o número de destinos crescer, com decisão registrada e especificação posterior. |

**Impacto nos achados da matriz**: nenhum achado muda de prioridade. A decisão Q2 remove a alternativa de painel duplo do escopo, o que reduz o risco de reescrita de tela apontado no plano; a decisão Q1 antecipa o trilho para 600 dp, o que faz a classe média ser a mais afetada pela Fase 1; a decisão Q3 mantém `AppNavHost` com a mesma lista de abas já existente.
