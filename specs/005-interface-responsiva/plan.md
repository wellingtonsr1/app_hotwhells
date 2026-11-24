# Implementation Plan: Interface responsiva e adaptável

**Branch**: `005-interface-responsiva` | **Date**: 2026-11-24 | **Spec**: [`spec.md`](./spec.md) | **Auditoria**: [`audit.md`](./audit.md)

**Input**: especificação da feature `005-interface-responsiva`, derivada da Constituição v1.3.0 (Artigo VII) e da auditoria do código atual.

> **Natureza desta fase**: planejamento. Nenhum arquivo de código, banco ou configuração do aplicativo é alterado aqui. As tarefas detalhadas serão geradas por `/speckit-tasks`; este documento fixa abordagem técnica, ordem, arquivos e critérios.

## Summary

Adaptar a camada de apresentação do Diecast Collector às classes de janela compacta, média e expandida sem tocar em domínio, banco ou offline. A abordagem é **centralizar a decisão de tamanho** (uma `WindowSizeClass` calculada no shell e distribuída por `CompositionLocal`), **extrair cinco componentes adaptativos** reutilizáveis e **aplicá-los em fases**, começando pela navegação e terminando nas telas auxiliares. Cada fase é entregável isoladamente e verificada por classe de janela.

## Technical Context

**Language/Version**: Kotlin 2.1.0, JVM 17

**Primary Dependencies atuais**: Compose BOM `2025.01.00` (material3, ui), `navigation-compose 2.8.5`, Hilt 2.53.1, Coil 2.7.0, Room 2.6.1, ML Kit on-device

**Dependência pretendida**: `androidx.compose.material3:material3-window-size-class` (versão gerenciada pelo BOM do Compose já usado, `androidx.compose.material3.windowsizeclass`). `androidx.compose.material3:material3-adaptive` / `material3-adaptive-navigation-suite` são **opcionais** e só entram com ADR (R.4, R.17) — o plano funciona sem elas.

**Storage**: inalterado (Room/SQLite via `core:data`); nenhuma migração

**Testing**: JUnit4 + Robolectric (unitários), Compose UI Test + Espresso (instrumentados, 30 classes existentes), Macrobenchmark (`benchmark`)

**Target Platform**: Android `minSdk 26`, `targetSdk 36`; celular, tablet e dobrável

**Project Type**: aplicativo Android multi-módulo (`app`, `core:domain`, `core:data`, `core:ui`, `benchmark`)

**Performance Goals**: reorganizar layout ao trocar de classe de janela em < 1 s; manter as metas de R.7 (lista < 1 s, busca < 300 ms) e o perfil de rolagem medido na spec 003

**Constraints**: sem mudança de regra de negócio, banco, offline, `applicationId` ou nome comercial; artefato +≤ 1 MB; sem `screenOrientation` fixo; alterações cirúrgicas (nada de redesenho)

**Scale/Scope**: 30 telas/rotas de UI, 11 diálogos/folhas, 7 componentes compartilhados, 30 classes de teste instrumentado

## Constitution Check

*GATE: deve passar antes da Fase 0 e ser revalidado após o desenho.*

| Princípio | Como o plano atende | Situação |
|---|---|---|
| VII.1–VII.3 (classes de janela, dispositivos, orientação) | `WindowSizeClass` calculada do contêiner no shell; nenhum `DisplayMetrics`/`screenWidthDp`; sem bloqueio de orientação | PASSA |
| VII.4–VII.7 (adaptação de layout, formulários, insets) | 5 componentes adaptativos compartilhados; insets via `WindowInsets.safeDrawing`; coluna de leitura máxima | PASSA |
| VII.8 (dobra e insets) | Classificação pelo contêiner; `WindowInsets.safeDrawing` + teste em janela dividida | PASSA |
| VII.9–VII.10 (identidade, acessibilidade) | Sem mudança de tokens/tema; alvos ≥ 48 dp corrigidos na grade de fotos; fonte do sistema mantida | PASSA |
| VII.11 (não alterar negócio) | Fases tocam apenas `feature/**` (composição) e `ui/components`; `core:domain`/`core:data` intocados | PASSA |
| VII.12–VII.14 (planejamento, arquitetura, testes) | Spec declara comportamento por classe; plano por fases; V.10 atendido com testes por tamanho | PASSA |
| II.1–II.6 (modularidade, teste sem UI) | Componentes em `ui/components`/`core:ui`; domínio testável sem UI permanece | PASSA |
| V.1, V.4, V.10 (testes, sem afrouxar) | Suíte existente mantida; novas asserções por classe; nenhum teste removido | PASSA |
| R.4/R.5/R.17 (dependência justificada) | Uma dependência (`material3-window-size-class`) com ADR obrigatório antes da Fase 1; adaptativas só com ADR | PASSA com ADR |
| R.13 (tamanho/startup) | Componentes pequenos, sem nova biblioteca de layout; medido antes/depois da Fase 1 | PASSA |
| IV.5 / IV.6 (Android, 360 dp, acessibilidade) | Comportamento compacto preservado; verificação em 360 dp e fonte 200% | PASSA |

Sem violações a justificar em *Complexity Tracking*.

## Project Structure

### Documentação (esta feature)

```text
specs/005-interface-responsiva/
├── spec.md                     # requisitos e critérios de aceitação
├── audit.md                    # inventário, achados e matriz de diagnóstico
├── plan.md                     # este documento
└── checklists/requirements.md  # validação de qualidade da spec
```

### Código (estrutura existente, sem reorganização)

```text
app/src/main/java/br/com/diecastcollector/
├── MainActivity.kt                        # fornece WindowSizeClass ao conteúdo
├── navigation/AppNavHost.kt               # shell adaptável (barra ↔ trilho)
├── ui/
│   ├── components/                        # componentes compartilhados novos
│   │   ├── WindowSizeClassLocal.kt        # CompositionLocal + helper de instalação
│   │   ├── AdaptiveGrid.kt                # grade responsiva (minSize)
│   │   ├── FlowChipGroup.kt               # grupo de chips com quebra
│   │   ├── FormColumn.kt                  # coluna de formulário com largura máxima
│   │   ├── AdaptableCardActions.kt        # ações que quebram / viram menu
│   │   └── ItemThumbnail.kt               # existente; insets e tamanho por classe
│   └── theme/                             # intocado
├── feature/<módulo>/<Tela>.kt             # 30 telas ajustadas em fases
core/ui/                                   # ProgressBar/MetricCard compartilhados, se aprovado
app/src/androidTest/java/br/com/diecastcollector/
├── testing/WindowSizeRule.kt              # regra de teste por classe de janela
└── feature/**/*Test.kt                    # asserções de adaptação acrescentadas
```

**Structure Decision**: manter a estrutura por feature já existente. Componentes novos vão para `ui/components` (usados só pelo `app`); extrações que também sirvam a módulos vão para `core:ui`. Nenhum pacote de domínio é tocado.

## Estratégia técnica

### 1. Decisão de tamanho em um único lugar

- Calcular `WindowSizeClass` em `MainActivity` (`calculateWindowSizeClass(windowContext)` ou `currentWindowAdaptiveInfo()` conforme a API disponível na versão do BOM) e publicar por `CompositionLocal`.
- Regra: **a classificação vem do contêiner da tela**, nunca de `DisplayMetrics` global nem de `LocalConfiguration.screenWidthDp` (VII.3). O `LocalConfiguration` só pode ser usado como sinal adicional de fonte/altura.
- Nenhuma tela decide sozinha: consome `LocalWindowSizeClass` (ou helper `isExpanded`, `columnsFor(minSize)`).

### 2. Shell adaptável (`AppNavHost`)

- `C` → `NavigationBar` inferior (comportamento atual).
- `M`/`E` → `NavigationRail` (material3) à esquerda com os cinco destinos; rótulos/ícones conforme largura do trilho.
- Preservar `popUpTo`/`saveState`/`restoreState` e a lógica de "aba selecionada" já implementada; nenhuma rota muda.
- As 27 rotas de detalhe continuam sem navegação principal.

### 3. Componentes adaptativos (fase 1, antes das telas)

| Componente | Papel | Telas que passam a usá-lo |
|---|---|---|
| `AdaptiveGrid` | grade com `GridCells.Adaptive(minSize)` sobre `LazyVerticalGrid` | catálogo, busca (grade/galeria), coleções, séries, locais, duplicados, fotos |
| `FlowChipGroup` | grupo de chips que quebra em linhas (sem rolagem horizontal não semântica) | cadastro rápido, ficha do exemplar, variante, veículo real, duplicado, desejos, relatórios |
| `FormColumn` | coluna com largura máxima de leitura, centralizada em M/E | 13 telas de formulário |
| `AdaptableCardActions` | ações em linha que quebram ou viram menu | desejos, coleção (cabeçalho/linhas), duplicados, cabeçalhos com título + ações |
| `WindowSizeClassLocal` | distribui a classe e helpers | shell + todas as telas |
| `ItemThumbnail` (ajuste) | insets no visualizador; tamanho por classe | listas e detalhe |

`ProgressBar` e `MetricCard` duplicados em `DashboardScreen`/`CollectionsScreen` e `DashboardScreen`/`FinanceScreen` são extraídos para reuso (II.5) já na fase que toca as telas de origem.

### 4. Diálogos e folhas

- Diálogos: trocar alturas fixas (`height(320.dp)` do seletor de coleção) por `heightIn(max = …)` derivado do espaço do diálogo (`BoxWithConstraints`) e rolagem interna.
- Filtros de busca: painel lateral persistente em `E`; folha modal em `C`/`M` (decisão revisável em ADR de UI se necessário).
- Folhas existentes mantêm rolagem própria e botões fixos no fim.
- Editor de fotos: `panelMaxHeight` passa a derivar do espaço real do diálogo, preservando a folga de 32 dp do rodapé conquistada na spec 004.

### 5. Acessibilidade e insets

- Alvos de 32/36 dp da grade de fotos → área de toque ≥ 48 dp mantendo o ícone pequeno.
- Visualizadores em tela cheia com `WindowInsets.safeDrawing`.
- Nenhuma mudança de contraste, tipografia base ou tokens de tema.

## Fases de implementação

Cada fase é independente e entregável; a ordem segue do geral para o específico.

### Fase 1 — Estrutura global, dependência e componentes

- ADR: `material3-window-size-class` (e decisão sobre `material3-adaptive`).
- `WindowSizeClassLocal` + instalação em `MainActivity`.
- `AppNavHost` com barra ↔ trilho; preservação de estado das abas.
- Componentes `AdaptiveGrid`, `FlowChipGroup`, `FormColumn`, `AdaptableCardActions`.
- Regra de teste `WindowSizeRule` em `androidTest/testing`.
- **Saída**: app navegável nas três classes; componentes prontos; testes do shell.
- **Arquivos**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MainActivity.kt`, `navigation/AppNavHost.kt`, `ui/components/*` (novos), `app/src/androidTest/.../testing/WindowSizeRule.kt`, `NavigationBarTest.kt`.
- **Risco**: mudança de shell pode quebrar asserções que assumem `NavigationBar`; ajustar de forma explícita (V.4), sem remover cobertura.

### Fase 2 — Painel, acervo e coleções

- `DashboardScreen` (métricas por largura mínima; manter regra de fonte).
- `CatalogScreen` (grade responsiva; atalhos com quebra em M/E).
- `CollectionsScreen`, `CollectionDetailScreen` (cabeçalho e ações adaptáveis), `SeriesScreen`.
- **Arquivos**: os 5 arquivos de tela + `DashboardScreenTest`, `CatalogScreenTest`, `CollectionDetailTest`, `CollectionsScreen` (teste novo se não houver).

### Fase 3 — Fabricantes, modelos, variantes e exemplares

- `CreateCatalogEntryScreen`, `ModelFormScreen`, `VariantFormScreen` (criação e edição), `AddExemplarScreen`, `ExemplarListScreen`, `ExemplarDetailScreen`, `IncompleteEntriesScreen`.
- Aplicar `FormColumn`, `FlowChipGroup` (condição, embalagem, veículo real), `AdaptiveGrid` nas listas.
- Diálogos de variante (mesclar/dividir) com rolagem e altura relativa.
- **Arquivos**: 7 telas + `PhotoGrid` (uso) + testes correspondentes (`AddExemplarScreenTest`, `CreateCatalogEntryScreenTest`, `ModelFormScreenTest`, `VariantEditScreenTest`, `ExemplarDetailScreenTest`, `IncompleteEntriesTest`, `CatalogScreenTest`).

### Fase 4 — Fotografias e editor

- `PhotoGrid`: grade por largura mínima (3:4 preservado), alvos ≥ 48 dp, visualizador com insets.
- `PhotoCaptureActions`, `PhotoReviewDialog`, `PhotoEditDialog`.
- `PhotoEditorScreen`: altura do painel pelo contêiner; manter insets e folga do rodapé.
- **Arquivos**: `PhotoGrid.kt`, `PhotoCapture.kt`, `PhotoReviewDialog.kt`, `PhotoEditDialog.kt`, `feature/photoeditor/PhotoEditorScreen.kt` + `PhotoGridTest`, `PhotoEditorScreenTest`, `PhotoReviewDialogTest`.

### Fase 5 — Busca, financeiro, desejos, duplicados e localizações

- `SearchScreen` + `CatalogViews` (grade adaptativa) + filtros (painel lateral em E).
- `FinanceScreen` (linha com rótulo `weight`/`maxLines`; métricas por largura).
- `WishlistScreen`, `WishlistFormScreen` (ações adaptáveis).
- `DuplicatesScreen` + `DuplicateActionsSheet` (chips com quebra), `NegotiationScreen`, `NegotiationHistoryScreen`.
- `LocationsScreen`, `LocationContentsScreen`, `BarcodeScanScreen`, `RecognitionReviewSheet`.
- **Arquivos**: 11 telas/folhas + `SearchScreenTest`, `FinanceScreenTest`, `WishlistScreenTest`, `DuplicatesFlowTest`, `LocationsTest`, `BarcodeScanScreenTest`, `RecognitionReviewSheetTest`.

### Fase 6 — Configurações e telas auxiliares

- `SettingsScreen`, `BackupScreen` (ações do diálogo), `ImportScreen`, `ReportsScreen`, `DataControlScreen`, `AboutScreen`, `PrivacyPolicyScreen`.
- `FormColumn` para largura de leitura; `AdaptableCardActions` nos cabeçalhos.
- **Arquivos**: 7 telas + `SettingsScreenTest`, `BackupScreenTest`, `DataControlScreenTest`.

### Fase 7 — Fechamento

- Varredura final: nenhum `GridCells.Fixed`, `height(<dp>)` de conteúdo, `Row` de chips sem quebra, `fillMaxWidth` sem limite em formulário.
- Matriz de rastreabilidade (Anexo B) FR ↔ tela ↔ teste.
- Medições NFR-301/302/303 e registro de resultados.
- Capturas antes/depois por classe de janela em `build/responsive/`.

## Estratégia de testes (V.10)

| Camada | O que cobre |
|---|---|
| Unitário (Robolectric/JUnit) | nenhuma regra de negócio muda; testes existentes devem continuar verdes sem alteração |
| Instrumentado por classe de janela | cada tela alterada: renderizar em 360 dp (C), 700 dp (M) e 1000 dp (E) e afirmar: navegação presente na forma esperada, ausência de recorte (bounds dentro do contêiner), contagem de colunas, alvo ≥ 48 dp, ações acima dos insets |
| Paisagem compacta | altura ~360 dp nas telas com diálogo/editor: diálogo cabe, rolagem interna funciona, rodapé acima da barra |
| Regressão funcional | suíte atual das 30 classes: sem remoção, sem afrouxamento; ajustes apenas onde a forma de navegação mudou (asserção de `NavigationBar` → rail em janela grande) |
| Medição | macrobenchmark existente para startup/rolagem; comparar antes/depois da Fase 1 e da Fase 4 |

Como renderizar tamanhos diferentes: `WindowSizeRule` envolve o conteúdo do teste em `Box(Modifier.size(width.dp, height.dp))` e fornece a `WindowSizeClass` correspondente pelo `CompositionLocal`, além de testes instrumentados que redimensionam a janela do emulador (`adb shell wm size`) para as classes média/expandida.

## Arquivos que provavelmente precisarão de alteração

**Configuração/dependência (1)**: `gradle/libs.versions.toml`, `app/build.gradle.kts` (adicionar `material3-window-size-class`).

**Shell e temas (3)**: `MainActivity.kt`, `navigation/AppNavHost.kt`, `ui/theme` (somente se um token de largura for necessário — preferencialmente nenhum).

**Componentes (7)**: `ui/components/ItemThumbnail.kt` (ajuste) + 5 novos em `ui/components` + `core/ui` (extrações `ProgressBar`/`MetricCard`).

**Telas por fase**:
- Fase 2: `feature/dashboard/DashboardScreen.kt`, `feature/catalog/CatalogScreen.kt`, `feature/organization/CollectionsScreen.kt`, `feature/organization/CollectionDetailScreen.kt`, `feature/organization/SeriesScreen.kt`.
- Fase 3: `feature/catalog/CreateCatalogEntryScreen.kt`, `ModelFormScreen.kt`, `VariantFormScreen.kt`, `AddExemplarScreen.kt`, `ExemplarListScreen.kt`, `ExemplarDetailScreen.kt`, `IncompleteEntriesScreen.kt`, `organization/CollectionItemPicker.kt`, `organization/CollectionFormScreen.kt`.
- Fase 4: `feature/catalog/PhotoGrid.kt`, `PhotoCapture.kt`, `PhotoEditDialog.kt`, `feature/photoeditor/PhotoReviewDialog.kt`, `feature/photoeditor/PhotoEditorScreen.kt`.
- Fase 5: `feature/search/SearchScreen.kt`, `feature/search/CatalogViews.kt`, `feature/finance/FinanceScreen.kt`, `feature/wishlist/WishlistScreen.kt`, `feature/wishlist/WishlistFormScreen.kt`, `feature/duplicates/DuplicatesScreen.kt`, `DuplicateActionsSheet.kt`, `NegotiationScreen.kt`, `NegotiationHistoryScreen.kt`, `feature/locations/LocationsScreen.kt`, `LocationContentsScreen.kt`, `feature/recognition/BarcodeScanScreen.kt`, `RecognitionReviewSheet.kt`.
- Fase 6: `feature/settings/SettingsScreen.kt`, `feature/transfer/BackupScreen.kt`, `feature/transfer/ImportScreen.kt`, `feature/reports/ReportsScreen.kt`, `feature/settings/DataControlScreen.kt`, `AboutScreen.kt`, `PrivacyPolicyScreen.kt`.

**Testes (novos/ajustados)**: `app/src/androidTest/.../testing/WindowSizeRule.kt` (novo), `NavigationBarTest.kt`, e as 15 classes que cobrem as telas das fases 2–6.

**Não muda**: `core/domain/**`, `core/data/**`, `app/src/main/java/.../data/**`, migrations, `AndroidManifest.xml` (exceto se algum `configChanges` faltar — não previsto), `applicationId`, banco.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Shell novo quebrar testes que assumem `NavigationBar` | médio | Ajustar asserções de forma explícita e rodar `NavigationBarTest` a cada fase (V.4) |
| `material3-window-size-class` indisponível na versão do BOM | baixo | Verificar no primeiro passo da Fase 1; se faltar, usar `currentWindowAdaptiveInfo()` correspondente ou atualizar o BOM com ADR |
| Adaptação virar reescrita de telas | alto | Regra de "alteração mínima suficiente": só layout, nenhum fluxo refeito; revisão por fase |
| Regressão em fonte ampliada | médio | Teste a 200% nas telas com chips/formulários |
| Medição em emulador só de celular | médio | Emulador adicional com perfil tablet e, se possível, dobrável; registrar limitações |
| Crescer artefato acima de 1 MB | baixo | Medir APK antes/depois da Fase 1; sem bibliotecas de layout extras |
| `FormColumn` quebrar campos que dependem de largura total | baixo | Aplicar por tela, comparando captura antes/depois |

## Verificação (comandos previstos)

```bash
# unitários e lint
export JAVA_HOME="$LOCALAPPDATA/Android/Jdk/jdk-17.0.21+9"
export MSYS_NO_PATHCONV=1
./gradlew :app:testDebugUnitTest :app:lintDebug

# instrumentados (emulador API 36)
"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd hw_test -no-snapshot -no-audio -no-boot-anim -gpu swiftshader_indirect
./gradlew :app:connectedDebugAndroidTest

# classes de janela no emulador
adb shell wm size 1080x2400 && adb shell wm density 420   # compacta
adb shell wm size 1600x2560 && adb shell wm density 320   # média/expandida
adb shell wm size reset && adb shell wm density reset

# capturas por classe
adb exec-out screencap -p > build/responsive/<tela>-<classe>.png
```

## Próximos passos

1. Revisar e aprovar `spec.md` e `audit.md` (as dúvidas Q1–Q3 da auditoria podem virar `/speckit-clarify`).
2. `/speckit-tasks` para gerar `tasks.md` por fase.
3. Implementar a Fase 1 (shell + componentes) e medir antes/depois.
