# Implementation Plan: Preferência de aparência (tema claro, escuro e sistema)

**Branch**: `006-preferencia-aparencia` | **Date**: 2026-11-25 | **Spec**: [`spec.md`](./spec.md)

**Input**: especificação `006-preferencia-aparencia`, derivada da Constituição v1.3.0 e do levantamento do código atual de tema.

> **Natureza desta fase**: planejamento. Nenhum arquivo de produção é alterado aqui. As tarefas detalhadas estão em [`tasks.md`](./tasks.md); este documento fixa abordagem técnica, arquivos, ordem e critérios de verificação.

## Summary

Substituir a escolha implícita de tema (`isSystemInDarkTheme()` sem alternativa) por uma preferência explícita de três valores — **Sistema**, **Claro**, **Escuro** — persistida em DataStore Preferences e resolvida em um único ponto do tema existente. A abordagem é: (1) `ThemeMode` e a resolução ficam em `core:ui`, dentro de `DiecastTheme`, reaproveitando `LightColors`/`DarkColors`; (2) um store `@Singleton` na camada de aplicação expõe `StateFlow<ThemeMode>` e escrita suspensa; (3) `MainActivity` observa o fluxo, alimenta o tema e sincroniza o estilo das barras do sistema; (4) Ajustes ganha a seção **Aparência** com seleção única; (5) fundo de janela e sobreposições sobre fotos passam a depender da aparência, não do modo do sistema. Nenhuma paleta nova, nenhuma rota nova, nenhum toque em domínio ou banco.

## Technical Context

**Language/Version**: Kotlin 2.1.0, JVM 17

**Primary Dependencies atuais**: Compose BOM `2025.01.00` (material3, ui), `activity-compose 1.10.0`, `navigation-compose 2.8.5`, Hilt 2.53.1, Room 2.6.1, DataStore Preferences 1.1.1 (declarada e **sem uso atual**), Coil 2.7.0

**Dependência pretendida**: **nenhuma nova**. DataStore Preferences já está em `gradle/libs.versions.toml`, em `app/build.gradle.kts` e em `core/data/build.gradle.kts`; a feature apenas passa a usá-la. `core:ui` ganha apenas Kotlin/Compose já presentes.

**Storage**: preferência local em DataStore, fora do banco. Room, esquema, migrações e repositórios de domínio **inalterados**. O pacote de backup (`FileBackupService`) copia banco + fotos + tabelas exportadas e **não** inclui arquivos de preferência — confirmado no código, então FR-415 é atendido sem alteração no backup.

**Testing**: JUnit4 + Robolectric (unidade em `app/src/test`), Compose UI Test + Espresso (instrumentados em `app/src/androidTest`), reuso de `ContrastAssert`, `WindowSizeRule`, `ResponsiveCapturesTest` e da infraestrutura de capturas da Spec 005

**Target Platform**: Android `minSdk 26`, `targetSdk 36`; celular, tablet e dobrável

**Project Type**: aplicativo Android multi-módulo (`app`, `core:domain`, `core:data`, `core:ui`, `benchmark`)

**Performance Goals**: tema aplicado em < 100 ms após o toque; +≤ 50 ms na abertura fria sobre o baseline de 993 ms (Spec 005); sem recriação de atividade exigida

**Constraints**: sem domínio/banco/offline/`applicationId`/`app_name`; sem dependência nova; sem segundo mecanismo de tema; decisão de persistência registrada em `docs/adr-006-preferencia-aparencia.md` (R.4); sem alteração nas Specs 001–005

**Scale/Scope**: 1 enum + 1 função de tema, 1 store, 1 seção em Ajustes, 1 ajuste de fundo de janela, 1 ajuste de barras do sistema, ~40 superfícies já inventariadas pela Spec 005, 6–8 arquivos de teste novos/estendidos

## Constitution Check

*GATE: deve passar antes da Fase 0 e ser revalidado após o desenho.*

| Princípio | Como o plano atende | Situação |
|---|---|---|
| I (integridade dos dados) | Preferência fora do banco; nenhuma migração; backup não a inclui e restauração não a altera | PASSA |
| II.3/II.5 (dependências unidirecionais, ponto único) | `core:ui` recebe só `ThemeMode`; leitura do DataStore fica em `app`; uma resolução de tema, sem paleta duplicada | PASSA |
| IV.1 (pt-BR) | Rótulos «Aparência», «Sistema», «Claro», «Escuro» em `strings.xml` | PASSA |
| IV.6/IV.7/VII.10 (acessibilidade, entrada preservada) | Grupo de seleção única com semântica, alvo ≥ 48 dp, fonte 200%; troca de tema não recria a atividade nem perde estado | PASSA |
| V.1/V.2/V.4 (testes e verificação) | Testes de unidade do store/resolução, instrumentados de UI e de contraste renderizado; suíte existente mantida | PASSA |
| VII.9/VII.11 (identidade; apresentação sem negócio) | Mesmos tokens e tipografia; nenhuma mudança de regra, rota ou dado | PASSA |
| R.4/R.5 (decisão e inventário de dependências) | `docs/adr-006-preferencia-aparencia.md` registra alternativas, decisão, trade-offs e consequências; nenhuma dependência nova; `docs/dependencias.md` e `docs/revisao-dependencias.md` corrigidos quanto à finalidade real do DataStore | PASSA |
| R.13 (tamanho/startup) | Medição antes/depois do APK e da abertura fria, com limites em NFR-404 | PASSA |
| IV.9/R.15 (identidade do produto) | Nome, ícone, `applicationId` e tema de janela mantidos | PASSA |

Sem violações a justificar em *Complexity Tracking*.

## Project Structure

### Documentação (esta feature)

```text
specs/006-preferencia-aparencia/
├── spec.md              # requisitos, histórias, critérios (esta fase)
├── plan.md              # este documento
├── research.md          # levantamento do código e alternativas descartadas
├── data-model.md        # valor persistido, estados e invariantes
├── quickstart.md        # roteiro manual dos 12 critérios de aceitação
├── tasks.md             # tarefas por fase com aceite e janela
└── checklists/
    └── requirements.md  # validação de qualidade da spec
```

### Código (estrutura existente, sem reorganização)

```text
core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/
├── Theme.kt             # alterado: recebe ThemeMode e resolve a paleta
├── ThemeMode.kt         # novo: enum System/Light/Dark + resolvedor puro
├── Color.kt             # inalterado (paleta única)
└── Type.kt              # inalterado

app/src/main/java/br/com/diecastcollector/
├── MainActivity.kt      # alterado: observa o modo, tema e barras do sistema
├── di/PreferencesModule.kt          # novo: DataStore<Preferences> singleton
├── appearance/AppearancePreferences.kt  # novo: store da preferência
├── feature/settings/SettingsScreen.kt   # alterado: seção Aparência
├── feature/settings/SettingsViewModel.kt # alterado: estado e ação
└── res/values/colors.xml, res/values-night/colors.xml, res/values/strings.xml

app/src/test/java/br/com/diecastcollector/appearance/     # novo: store e resolução
app/src/androidTest/java/br/com/diecastcollector/appearance/ # novo: UI e aplicação global
```

## Estratégia técnica

### 1. Modo e resolução no tema (ponto único)

`ThemeMode.kt` em `core:ui`:

```kotlin
public enum class ThemeMode { SYSTEM, LIGHT, DARK }

public fun ThemeMode.isDark(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
```

`DiecastTheme` passa a ser `DiecastTheme(mode: ThemeMode = ThemeMode.SYSTEM, content)`, resolve `isSystemInDarkTheme()` apenas quando `mode == SYSTEM` e escolhe entre `LightColors` e `DarkColors`. O parâmetro padrão mantém os previews e os ~30 testes instrumentados que hoje chamam `DiecastTheme { … }` compilando sem alteração (FR-429). O parâmetro `darkTheme: Boolean` deixa de existir; não há chamada em produção que o use (verificado por grep).

### 2. Store da preferência (camada de aplicação)

`AppearancePreferences` (`@Singleton`, `@Inject constructor(@ApplicationContext context)`) sobre DataStore:

- chave `appearance_mode` (String): `system` | `light` | `dark`;
- `val mode: Flow<ThemeMode>` com `map { decode(it[KEY]) }` e `catch { emit(System) }` — valor ausente/desconhecido/falha de leitura ⇒ `SYSTEM` (FR-404, NFR-405);
- `suspend fun setMode(mode: ThemeMode)` com `dataStore.edit { … }`; erro de escrita é propagado de forma tipada e tratado pelo chamador, sem derrubar a UI;
- decodificação em funções puras (`encode`/`decode`) testáveis sem Android.

`di/PreferencesModule.kt` provê o `DataStore<Preferences>` com `preferencesDataStore`/`PreferenceDataStoreFactory` sobre `context.preferencesDataStoreFile("appearance")`, em escopo singleton, para não abrir dois donos do mesmo arquivo (evita `IllegalStateException` de multiple DataStores).

`MainActivity` usa `collectAsState(initial = ThemeMode.SYSTEM)` (sem API nova) e passa o valor a `DiecastTheme`. Não há `runBlocking`; a primeira leitura chega em milissegundos e o resíduo em modo forçado é medido (NFR-403).

### 3. Barras do sistema

`enableEdgeToEdge()` hoje deriva os ícones do modo escuro **do sistema**. Passa a ser chamado dentro de `SideEffect`, com `SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { escuroResolvido }` para status e navegação, de modo que a aparência forçada também governe o contraste dos ícones (FR-409). A chamada é idempotente e aceita ser repetida a cada recomposição do modo.

### 4. Seção Aparência em Ajustes

- `SettingsScreen` recebe `appearance: ThemeMode = ThemeMode.SYSTEM` e `onAppearanceChange: (ThemeMode) -> Unit = {}`, mantendo a possibilidade de teste sem Hilt (padrão já usado para `reminder`/`onIntervalChange`).
- A seção usa `Column.selectableGroup()` com três linhas `Modifier.selectable(selected, role = Role.RadioButton)` + `RadioButton`, título «Aparência» e uma linha de situação com a opção ativa. Fica dentro da `FormColumn` existente, herdando o limite de 720 dp e a rolagem (Spec 005).
- Strings novas: `settings_appearance`, `settings_appearance_subtitle`, `settings_appearance_system`, `settings_appearance_light`, `settings_appearance_dark`.
- `SettingsViewModel` expõe `appearance: StateFlow<ThemeMode>` e `fun setAppearance(mode: ThemeMode)` chamando o store no `viewModelScope`.
- Nenhuma rota ou entrada de navegação nova (FR-402).

### 5. Superfícies especiais

- **Fundo de janela**: `values/colors.xml` com `window_background` claro (`#F4F1EE`) e `values-night/colors.xml` com `#121212`; `themes.xml` continua referenciando `@color/window_background`. Corrige a piscada escura que hoje aparece para quem usa o Android no claro (FR-410) e mantém o escuro atual.
- **Sobreposições sobre fotos**: `PhotoGrid`, `PhotoEditorScreen`, `PhotoReviewDialog` e `ItemThumbnail` já usam `MaterialTheme.colorScheme.scrim` com alfa 0,55–0,7 e `Color.White` sobre a imagem. Não são alterados por padrão: o *scrim* é derivado do tema (`scrim` é `InkBlack` no claro e no escuro), e os testes de contraste renderizado provam o pior caso. Exceções documentadas: `Color.White`/`Color.Black` sobre fotos e a moldura de recorte são cores de **legibilidade sobre imagem**, não de tema (BR-406), com comentário no código.
- **Editor**: fundo `Color.Black` permanece (área neutra de edição); a moldura de ferramentas usa tokens.

### 6. Documentação

- KDoc de `Theme.kt` descreve o comportamento implementado (modo, padrão, persistência).
- `docs/dependencias.md`: corrigir a finalidade do DataStore (hoje "lembrete de backup", que o código não usa) para "preferências simples do app (aparência; lembrete de backup segue em SharedPreferences)".
- `docs/revisao-dependencias.md`: mesma correção de finalidade, mantendo versão/licença.
- `docs/validacao.md`: nova seção com medições e evidências desta feature.
- Manual (`docs/manual/MANUAL_USUARIO_COMPLETO.md`, §28 Ajustes; `INDICE_FUNCIONALIDADES.md`): seção Aparência e capturas nos dois modos.
- Referências cruzadas nas Specs 001, 003, 004 e 005: apenas **recomendação** registrada em `spec.md` (não alterar os requisitos aprovados nesta fase).

## Fases de implementação

### Fase 1 — Tema e preferência (base)

1. `ThemeMode` + resolução pura em `core:ui`; `DiecastTheme(mode=…)` preservando o padrão.
2. `AppearancePreferences` + `PreferencesModule`; chave versionada e decodificação tolerante; ADR-006 em `docs/adr-006-preferencia-aparencia.md` (R.4).
3. Testes de unidade do store e da resolução.
4. Ajuste do fundo de janela (`values`/`values-night`).

**Saída**: preferência persistida e tema capaz de obedecê-la, sem UI ainda.

### Fase 2 — Ligação no shell e na tela de Ajustes

1. `MainActivity` observa o modo, alimenta o tema e sincroniza as barras do sistema.
2. Seção **Aparência** em Ajustes + `SettingsViewModel`.
3. Strings pt-BR e semântica de seleção única.
4. Testes de UI: padrão Sistema, seleção, troca imediata, fonte 200%, classes de janela.

**Saída**: usuário consegue escolher; troca imediata; escolha persiste.

### Fase 3 — Consistência, contraste e evidências

1. Varredura das superfícies que ainda dependem do modo do sistema (barras, insets, fundo, *scrims*, placeholders).
2. Testes de aplicação global por tela representativa em Claro e Escuro.
3. Contraste renderizado (captura + amostragem) em textos sobre fotos, cartões e editor.
4. Capturas de evidência nos dois modos (reuso do percurso da Spec 005) e checklist.
5. Documentação: KDoc, dependências, manual, validação, recomendações cruzadas.

**Saída**: feature completa, medida e documentada.

## Estratégia de testes (V.1, V.2, V.10)

| Camada | Arquivo previsto | Cobertura |
|---|---|---|
| Unidade | `app/src/test/.../appearance/ThemeModeTest.kt` | resolução Sistema/Claro/Escuro; independência do sistema nos modos forçados |
| Unidade | `app/src/test/.../appearance/AppearancePreferencesTest.kt` | padrão Sistema; ida e volta dos três valores; valor desconhecido; corrupção/falha; escrita concorrente |
| Unidade | `app/src/test/.../feature/settings/SettingsViewModelAppearanceTest.kt` | estado inicial e persistência da ação |
| Instrumentado | `app/src/androidTest/.../appearance/AppearanceSettingsTest.kt` | três opções, padrão, seleção única, semântica, 48 dp, fonte 200%, 360/700/1.000 dp |
| Instrumentado | `app/src/androidTest/.../appearance/AppearanceApplicationTest.kt` | paleta aplicada em telas representativas; ausência de superfície oposta; barras do sistema |
| Instrumentado | `app/src/androidTest/.../appearance/AppearancePersistenceTest.kt` | escolha sobrevive a `recreate` e a reabertura; restauração de backup não altera |
| Instrumentado | `app/src/androidTest/.../appearance/RenderedContrastTest.kt` | contraste de elementos renderizados (texto sobre foto clara/escura, cartões, editor) |
| Evidência | capturas Claro/Escuro por classe | SC-407/SC-408, `build/responsive/aparencia/` + checklist |

**Não automatizável com honestidade**: reinício do **aparelho** e atualização por cima — evidência manual com comando registrado em `quickstart.md` e `docs/validacao.md`, nunca declarados como teste automatizado. Contraste renderizado usa `captureToImage()` com tolerância e regiões determinísticas (não amostragem aleatória).

## Arquivos que provavelmente precisarão de alteração

| Arquivo | Mudança |
|---|---|
| `core/ui/src/main/kotlin/.../theme/Theme.kt` | assinatura por `ThemeMode`; KDoc corrigido |
| `core/ui/src/main/kotlin/.../theme/ThemeMode.kt` | novo |
| `app/src/main/java/.../appearance/AppearancePreferences.kt` | novo |
| `app/src/main/java/.../di/PreferencesModule.kt` | novo |
| `app/src/main/java/.../MainActivity.kt` | observa modo; tema; barras do sistema |
| `app/src/main/java/.../feature/settings/SettingsScreen.kt` | seção Aparência |
| `app/src/main/java/.../feature/settings/SettingsViewModel.kt` | estado e ação |
| `app/src/main/res/values/colors.xml`, `values-night/colors.xml` | fundo de janela por modo |
| `app/src/main/res/values/strings.xml` | 5 strings novas |
| `app/src/test/...` e `app/src/androidTest/...` | testes da tabela acima |
| `docs/adr-006-preferencia-aparencia.md` | novo: decisão de persistência (R.4) |
| `docs/dependencias.md`, `docs/revisao-dependencias.md`, `docs/validacao.md`, `docs/manual/*` | documentação |

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Dois `DataStore` para o mesmo arquivo | `@Singleton` no módulo Hilt; nenhum `preferencesDataStore` duplicado |
| Primeiro quadro no modo forçado | Valor inicial `SYSTEM` sem bloquear; medir o resíduo por captura de quadro (limite de um quadro, ≤ 16,7 ms — NFR-403) e registrar como limitação com o valor medido |
| `enableEdgeToEdge` divergir do tema | Estilo derivado do modo resolvido em `SideEffect`; teste de contraste das barras |
| Testes existentes assumirem `darkTheme` | Grep confirma que nenhum passa o parâmetro; valor padrão preserva as chamadas |
| Contraste sobre foto clara | *Scrim* existente + teste renderizado de pior caso; ajustar alfa se falhar (não afrouxar asserção) |
| Interpretação da identidade escura (Spec 003/004) | Modo escuro permanece idêntico; esclarecimento proposto sem alterar as specs |
| Crescimento do APK com DataStore em uso | Medir antes/depois; limite de +100 KB |

## Verificação (comandos previstos)

```bash
# unidade + lint
./gradlew :app:testDebugUnitTest :app:lintDebug

# instrumentados (emulador API 36)
./gradlew :app:connectedDebugAndroidTest --tests '*Appearance*'
./gradlew :app:connectedDebugAndroidTest

# tamanho do APK
./gradlew :app:assembleDebug && ls -l app/build/outputs/apk/debug/

# abertura fria (10 execuções, mediana)
adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -W -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity

# persistência entre processos
adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity

# atualização preservando dados
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Próximos passos

1. `/speckit-tasks` detalha `tasks.md` (já iniciado nesta entrega).
2. `/speckit-analyze` valida Constituição ↔ spec ↔ plano ↔ tarefas (G5).
3. `/speckit-implement` começa pela Fase 1, com commit por tarefa.

**Dependências para iniciar**: nenhuma pendente de aprovação; a única decisão em aberto é o esclarecimento recomendado para a Spec 003, que **não bloqueia** a implementação porque o modo escuro permanece inalterado.
