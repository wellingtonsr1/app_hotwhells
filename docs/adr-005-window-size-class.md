# ADR-005 — Classificação de janela por `material3-window-size-class`

**Status**: aceito em 2026-11-24 · **Spec**: [005-interface-responsiva](../specs/005-interface-responsiva/spec.md) · **Constituição**: R.4, R.5, R.17

## Contexto

A spec 005 exige que toda tela responda a três classes de largura — compacta (< 600 dp), média (600–839 dp) e expandida (≥ 840 dp) — derivadas do contêiner da tela, e que a classificação seja recalculada ao girar, redimensionar ou dobrar/desdobrar (FR-301, VII.3). Hoje o app só consulta `LocalConfiguration` em dois pontos isolados (escala de fonte no painel e altura do painel do editor), sem nenhuma noção compartilhada de classe.

A escolha é qual biblioteca dá a classificação, sabendo que ela será lida por um `CompositionLocal` em todas as telas e não pode depender da janela inteira: em split-screen e janela livre a largura que importa é a do contêiner.

## Alternativas consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| **`androidx.compose.material3:material3-window-size-class`** (escolhida) | Tipos oficiais (`WindowSizeClass`, `WindowWidthSizeClass`), `calculateFromSize(DpSize)` permite derivar do contêiner via `BoxWithConstraints`, versão controlada pelo BOM do Compose, sem `Activity` no meio | A versão antiga de `calculateWindowSizeClass(activity)` usa a janela inteira — precisa ser evitada |
| `androidx.compose.material3.adaptive:adaptive` | Traz `currentWindowAdaptiveInfo`, pane scaffolds e `ListDetailPaneScaffold` | Puxa navegação de painéis que a spec 005 proíbe (FR-307); API ainda em evolução; dependência maior para uma necessidade de três limiares |
| Solução caseira sobre `BoxWithConstraints` | Zero dependência; controle total | Reimplementaria limiares, testes e semântica que a plataforma já define; risco de divergir de 600/840 dp |
| `LocalConfiguration.screenWidthDp` | Já existe no projeto | Mede a tela, não o contêiner: erra em split-screen e janela livre (FR-301, FR-325) |
| Biblioteca de terceiros (`WindowManager` / `androidx.window`) | Boa para dobra e `FoldingFeature` | Necessária só para dobra física, que continua como limitação registrada (T360); adicionaria dependência sem cobrir requisito novo |

## Decisão

Adotar `androidx.compose.material3:material3-window-size-class`, com versão herdada do **Compose BOM 2025.01.00** (sem versão explícita no catálogo), e usá-la **apenas** por `WindowSizeClass.calculateFromSize(DpSize(largura, altura))` dentro de um `BoxWithConstraints` que envolve o app uma única vez (`ProvideWindowSizeClass` em `ui/components/WindowSizeClassLocal.kt`). O resultado é publicado em `LocalWindowSizeClass`; nenhuma tela consulta a janela, a `Activity` ou `DisplayMetrics` diretamente.

`calculateWindowSizeClass(activity)` fica proibido nesta feature: ele mede a janela do sistema, o que dá a classe errada quando o app ocupa metade da tela.

## Consequências

- **Positivas**: um único ponto de verdade para a classe; previews e testes recebem classe compacta por padrão, sem `Activity`; a API é estável e vem do mesmo BOM do Material 3, então não há versão para manter à mão.
- **Negativas**: a classificação por contêiner exige um `BoxWithConstraints` no topo — custo desprezível, mas é um ponto de atenção para não duplicar o provider em telas.
- **Limites**: a biblioteca não informa dobra física; FR-325 é verificado por insets e por duas janelas emuladas (T372), com a limitação de hardware registrada em T360.
- **Escopo preservado**: lista-detalhe e gaveta continuam fora (FR-307, Q3); nenhuma tela nova, nenhuma rota nova, nenhum dado novo.

## Registro

- Dependência inventariada em [`docs/dependencias.md`](./dependencias.md) §1 e revisada em [`docs/revisao-dependencias.md`](./revisao-dependencias.md) §2.
- Consumo em `app/build.gradle.kts` (`implementation(libs.compose.material3.window.size.class)`) e declaração sem versão em `gradle/libs.versions.toml`.
