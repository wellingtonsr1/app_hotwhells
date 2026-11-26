# Tasks: Preferência de aparência (tema claro, escuro e sistema)

**Input**: `specs/006-preferencia-aparencia/spec.md`, [`plan.md`](./plan.md), [`research.md`](./research.md), [`data-model.md`](./data-model.md) e Constituição v1.3.0

**Pré-requisitos**: nenhuma aprovação pendente. A única dúvida de interpretação (identidade escura da Spec 003/004) está registrada como recomendação e **não bloqueia**, porque o modo escuro permanece idêntico ao atual.

**Testes**: obrigatórios (V.1, V.2, V.4). Toda tarefa de UI declara a verificação por classe de janela quando aplicável (V.10).

## Formato: `[ID] [P?] [Story] Descrição — arquivo`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1–US3, conforme `spec.md`
- Cada tarefa tem **Aceite** (critério verificável) e **Janela** (C = compacta < 600 dp, M = média, E = expandida ≥ 840 dp, F = fonte 200%, P = paisagem compacta)

> Numeração a partir de T401 para não colidir com as specs 001–005.

---

## Phase 1: Setup (base técnica)

**Propósito**: modo de tema e preferência antes de qualquer tela.

- [x] T401 [P] Criar `core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/ThemeMode.kt` com o enum `SYSTEM`/`LIGHT`/`DARK` e a resolução pura (`ThemeMode.isDark(systemInDark: Boolean)`) — arquivo novo. Ajustar `core/ui/.../theme/Theme.kt` para `DiecastTheme(mode: ThemeMode = ThemeMode.SYSTEM, content)`, consultando `isSystemInDarkTheme()` somente em `SYSTEM`, e corrigir o KDoc para descrever o comportamento implementado (FR-405, FR-417, FR-418, FR-429). **Aceite**: `./gradlew :app:assembleDebug` compila; nenhuma chamada existente a `DiecastTheme { … }` precisa mudar; não existe segunda paleta nem segunda função de tema; nenhum uso de `darkTheme` remanescente. **Janela**: build apenas.
- [x] T402 [P] Criar `app/src/main/java/br/com/diecastcollector/appearance/AppearancePreferences.kt` (chave `appearance_mode`, valores `system`/`light`/`dark`, `Flow<ThemeMode>` tolerante a ausência/valor desconhecido/falha, `suspend fun setMode`) e `app/src/main/java/br/com/diecastcollector/di/PreferencesModule.kt` (`@Singleton` `DataStore<Preferences>`; um único dono do arquivo) — FR-404, FR-412–FR-416, NFR-405, NFR-406. **Aceite**: nenhuma dependência nova em `libs.versions.toml`; nenhum `SharedPreferences` para aparência; decodificação em funções puras. **Janela**: não se aplica.
- [x] T403 [P] Testes de unidade do store e da resolução — `app/src/test/.../appearance/AppearancePreferencesTest.kt` e `ThemeModeTest.kt`. **Aceite**: cobre ausência ⇒ `SYSTEM`; ida/volta dos três valores; valor desconhecido ⇒ `SYSTEM`; escrita concorrente resulta em valor íntegro; falha de leitura não lança para a UI; `SYSTEM` acompanha o booleano e `LIGHT`/`DARK` o ignoram. **Janela**: não se aplica.
- [x] T404 Criar o fundo de janela por modo: `app/src/main/res/values/colors.xml` com `window_background` claro (`#F4F1EE`) e novo `app/src/main/res/values-night/colors.xml` com `#121212`; `themes.xml` inalterado na referência (FR-410). **Aceite**: abertura com o Android no claro não mostra mais piscada escura; com o Android no escuro o fundo continua `#121212`. **Janela**: C, E.
- [x] T405 [P] Acrescentar as strings pt-BR em `app/src/main/res/values/strings.xml`: `settings_appearance`, `settings_appearance_subtitle`, `settings_appearance_system`, `settings_appearance_light`, `settings_appearance_dark` (FR-401, IV.1). **Aceite**: nenhum texto literal na composição; os três rótulos cabem inteiros em 360 dp e fonte 200% (verificado em T408). **Janela**: C, F.

**Checkpoint**: preferência persistida, tema obediente e recursos de janela corretos; nenhuma tela alterada.

---

## Phase 2: US1 (P1) — Escolher a aparência e aplicá-la imediatamente

**Goal**: seção **Aparência** em Ajustes, ligada ao tema, com troca imediata.

**Independent Test**: em instalação limpa, abrir Ajustes, confirmar **Sistema**, tocar em **Claro** e **Escuro** e ver a superfície inteira mudar sem reiniciar e sem perder estado.

- [x] T406 [US1] Adicionar a seção **Aparência** a `app/src/main/java/br/com/diecastcollector/feature/settings/SettingsScreen.kt`, dentro da `FormColumn` existente: título, linha de situação e três opções em `Column.selectableGroup()` com `Modifier.selectable(role = Role.RadioButton)` + `RadioButton`, recebendo `appearance: ThemeMode = ThemeMode.SYSTEM` e `onAppearanceChange: (ThemeMode) -> Unit = {}` (FR-402, FR-403, FR-424, FR-425). **Aceite**: nenhuma rota nova; seleção única; marca visível que não depende só de cor; toque em qualquer ponto da linha seleciona; seção herda 720 dp e rolagem da Spec 005. **Janela**: C/M/E, F, P.
- [x] T407 [US1] Estender `app/src/main/java/br/com/diecastcollector/feature/settings/SettingsViewModel.kt` com `appearance: StateFlow<ThemeMode>` e `fun setAppearance(mode: ThemeMode)`, ligando `SettingsRoute` ao store (FR-405, NFR-401). **Aceite**: ação persiste e o estado exibido reflete o aplicado; nenhuma lógica de tema duplicada no ViewModel. **Janela**: não se aplica.
- [x] T408 [US1] Ligar `app/src/main/java/br/com/diecastcollector/MainActivity.kt`: observar o modo (`collectAsState(initial = ThemeMode.SYSTEM)`, sem `runBlocking`), passar a `DiecastTheme(mode = …)` e sincronizar as barras do sistema com `SystemBarStyle.auto(…){ escuroResolvido }` em `SideEffect` (FR-406, FR-407, FR-409, NFR-402). **Aceite**: troca aplicada sem recriação exigida; em **Sistema**, `cmd uimode night yes/no` reflete em ≤ 1 s; ícones das barras contrastam nos dois modos e com aparência forçada divergente. **Janela**: C, E; P.
- [x] T409 [US1] Testes instrumentados da seção — `app/src/androidTest/.../appearance/AppearanceSettingsTest.kt`. **Aceite**: três opções visíveis; **Sistema** marcada na ausência de preferência; toque seleciona e persiste a marca; semântica de seleção única (estado selecionado/não); alvos ≥ 48 dp; fonte 200% sem truncar; 1.000 dp alinhado à coluna de leitura. **Janela**: C/M/E, F. **Feito (2026-11-26)**: 8 casos verdes no emulador `hw_test` (API 36), incluindo `fonteDuzentosPorCentoNaoTruncaOsRotulos` (truncamento medido por `getLineEnd(visibleEnd = true)`, não por `hasVisualOverflow`) e `janelaDeMilDpMantemAColunaDeLeituraCentralizada` (cartão entre 640 e 720 dp, sobras iguais). Evidência em `docs/validacao.md` §11.2.
- [x] T410 [US1] Teste de aplicação imediata e preservação de estado — `app/src/androidTest/.../appearance/AppearanceApplicationTest.kt` (parte 1): alternar Claro ↔ Escuro com formulário preenchido, lista rolada e aba atual; em **Sistema**, simular mudança de tema do Android na mesma sessão (override de configuração/Robolectric qualifiers ou equivalente; se a API de teste não permitir, registrar como evidência manual em T413) (FR-406, FR-407, FR-426, SC-404). **Aceite**: paleta muda; texto digitado, rolagem e aba permanecem; nenhuma recriação de atividade exigida (verificada pelo estado preservado); a mudança do sistema em **Sistema** é refletida em ≤ 1 s sem reiniciar. **Janela**: C, F. **Feito (2026-11-26)**: `AppearanceApplicationTest.alternarClaroEEscuroMantemARolagemDaLista` (rolagem e paleta) e `AppearanceSystemFollowTest.sistemaAcompanhaOTemaDoAndroidSemPerderOFormulario` (formulário preenchido, texto e rota preservados; paleta medida em ≤ 2 s de tolerância do teste). Evidência em `docs/validacao.md` §11.5.

**Checkpoint**: US1 entregue — o usuário escolhe, a tela inteira obedece na hora.

---

## Phase 3: US2 (P1) — A escolha persiste

**Goal**: sobrevivência a processo, aparelho, atualização e backup; padrão correto.

- [x] T411 [P] [US2] Teste de persistência entre recriações — `app/src/androidTest/.../appearance/AppearancePersistenceTest.kt`: escolher cada modo, `ActivityScenario.recreate()` e verificar a paleta e a opção marcada; restauração de pacote não altera a aparência (FR-412, FR-415). **Aceite**: os três modos persistem após `recreate`; após restauração de backup simulado, o modo é o mesmo; nenhum dado da coleção é lido para decidir aparência. **Limite**: encerramento real do processo e reinício de aparelho ficam em T413 — `am force-stop` mataria o processo de instrumentação. **Janela**: C. **Feito (2026-11-26)**: `AppearancePersistenceTest` — os três modos sobrevivem a `ActivityScenario.recreate()` com paleta e marca conferidas; a restauração simulada (remoção do que o pacote substitui + recriação) mantém a preferência, que fica em `files/datastore/appearance.preferences_pb`, fora de `images/` e do banco.
- [x] T412 [P] [US2] Cobrir o fallback de preferência ausente/ilegível/desconhecida em teste de unidade + instrumentado (FR-404, BR-402, NFR-405). **Aceite**: nenhum crash, nenhuma exceção visível, coleção intacta, tema **Sistema**. **Janela**: C. **Feito (2026-11-26)**: `AppearancePreferencesTest.falhaDeLeituraCaiEmSistemaSemLancar` (unidade) e `AppearanceFallbackTest` (ausente, valor desconhecido e arquivo corrompido, em DataStore real no aparelho) — os três caem em **Sistema** sem lançar.
- [x] T413 [US2] Evidência manual dos critérios não automatizáveis: encerramento do processo (`am force-stop`), reinício do aparelho e atualização `install -r` preservando dados, seguindo `quickstart.md` §5–§6; registrar comandos, datas e capturas em `docs/validacao.md` (SC-405, SC-406). **Aceite**: cada cenário com resultado e caminho de captura, ou declarado **não executado** — nunca "aprovado" sem evidência. **Janela**: C. **Feito (2026-11-26)**: `am force-stop` + reabertura, `install -r` por cima e `adb reboot` executados em Escuro no emulador `hw_test`; os quatro cenários mantiveram a marca e a paleta. Comandos e capturas em `docs/validacao.md` §11.3 (`docs/validacao-006-t413-1..4-*.png`).
- [x] T423 [US2] Tratar a falha de **escrita** da preferência — `appearance/AppearancePreferences.kt`, `feature/settings/SettingsViewModel.kt` e strings: erro tipado, aviso não bloqueante em pt-BR e manutenção da aparência anterior (FR-414). **Aceite**: teste com store que falha na escrita (fake no ViewModel e/ou DataStore em diretório somente leitura no teste) prova que nenhuma exceção chega à UI, o valor anterior permanece e há mensagem; nenhum diálogo modal. **Janela**: C.

**Checkpoint**: US2 entregue — a preferência cumpre o que promete.

---

## Phase 4: US3 (P2) — Consistência, contraste e acessibilidade nos dois modos

**Goal**: nenhuma superfície da paleta oposta; texto sobre foto legível; barras do sistema corretas; evidência visual.

- [x] T414 [US3] Varredura de superfícies que ainda decidem cor pelo modo do sistema ou usam cor fixa — `app/src/main`, `core/ui/src/main`: barras do sistema, `themes.xml`, *scrims*, placeholders, editor, revisão de captura, miniatura (FR-408–FR-420, FR-423). **Aceite**: nenhuma decisão de paleta duplicada; as decisões de superfície restritas ao modo resolvido (estilo das barras do sistema em `MainActivity`, fundo de janela em `values`/`values-night`) listadas e justificadas na documentação; `Color.White`/`Color.Black` restantes justificados por comentário (legibilidade sobre imagem / área neutra); nenhum `Color(0x…)` fora de `Color.kt`. **Janela**: C/M/E, F. **Feito (2026-11-26)**: as cores fixas restantes sobre foto e a área neutra do editor ganharam comentário (`PhotoGrid`, `ItemThumbnail`, `MediaDialogScaffold`, `PhotoEditorScreen`). A varredura encontrou um defeito real: `Card`, `NavigationBar` e contornos usavam papéis **não definidos** do esquema (`surfaceContainer*`, `outline`, `secondaryContainer`), caindo nas cores lavanda do Material 3 fora da identidade do app; `Theme.kt` passou a definir esses papéis com os tokens de `Color.kt`. Nenhum `Color(0x…)` fora de `Color.kt` em `app/src/main`.
- [x] T415 [US3] Teste de aplicação global por tela representativa — `app/src/androidTest/.../appearance/AppearanceApplicationTest.kt` (parte 2): painel, catálogo com cartão com/sem foto, ficha, formulário, busca, um diálogo, uma folha, editor e estados vazios, em Claro e Escuro (FR-408, SC-407). **Aceite**: cada tela usa a paleta do modo; nenhuma superfície da paleta oposta; mesma estrutura de layout, mesmas rotas e nenhuma ação escondida nos dois modos (FR-411). **Janela**: C/M/E. **Feito (2026-11-26)**: `AppearanceGlobalTest` — painel, catálogo com e sem foto, Ajustes, relatórios, dados, backup e importação nos dois modos; a cor dominante do bitmap pertence à paleta do modo e a contagem de nós e de ações de clique é idêntica entre Claro e Escuro. Ficha, formulários, busca e folhas não entraram nesta amostra: eles dependem de estado semeado e a cobertura de superfície fica com T416/T418 (grade e cartões) — registrado em `docs/validacao.md` §11.5.
- [x] T416 [US3] Teste de contraste renderizado — `app/src/androidTest/.../appearance/RenderedContrastTest.kt` com `captureToImage()`: texto sobre as imagens de teste **branca** e **preta** (grade, revisão, miniatura), cartões de coleção e ferramentas do editor, nos dois modos (FR-422, FR-423, SC-409). **Aceite**: ≥ 4,5:1 para texto normal e ≥ 3:1 para texto grande/UI nas regiões amostradas, com tolerância declarada; regiões determinísticas, sem amostragem aleatória. **Janela**: C, E. **Feito (2026-11-26)**: `RenderedContrastTest` — etiqueta de ângulo sobre imagem branca e preta e título do cartão/seção nos dois modos; região = nó semântico, extremos de luminância do bitmap, mínimo 4,2:1 com tolerância de 0,3 declarada. Diálogos do editor/revisão ficam em janela separada e não entram na amostragem por `captureToImage` (limitação registrada).
- [x] T417 [US3] Teste de contraste das barras do sistema com aparência forçada divergente do Android (FR-409). **Aceite**: ícones de status/navegação contrastam com o conteúdo em `Claro` com Android escuro e em `Escuro` com Android claro. **Janela**: C. **Feito (2026-11-26)**: `SystemBarsAppearanceTest` — `cmd uimode night yes` com aparência **Claro** mantém `isAppearanceLightStatusBars = true`; `night no` com **Escuro** mantém `false`.
- [x] T418 [P] [US3] Caminhada de capturas Claro × Escuro nas classes C/M/E/P, reaproveitando o percurso de `ResponsiveCapturesTest` da Spec 005 — `app/src/androidTest/.../appearance/AppearanceCapturesTest.kt` + checklist por linha de tela (FR-408, SC-407, SC-408). **Aceite**: par de capturas por tela e por modo; checklist com resultado e arquivo; inspeção visual registrada. **Janela**: C/M/E/P. **Feito (2026-11-26)**: `AppearanceCapturesTest` — C/M/E/P × Claro/Escuro × painel/catálogo/ajustes/grade de fotos = 48 PNGs em `Download/responsive/aparencia` (cópia em `build/responsive/aparencia/`); checklist em `docs/validacao.md` §11.5.3.
- [x] T425 [US3] Corrigir o fundo de janela/splash em modo forçado — `app/src/main/java/br/com/diecastcollector/appearance/ApplicationNightMode.kt` (novo), `MainActivity.kt`, `AndroidManifest.xml` (`configChanges="uiMode"`) e `appearance/AppearanceWindowBackgroundTest.kt` (novo). **Aceite**: o primeiro quadro do app em modo forçado já usa a paleta escolhida, nas duas direções, sem recriar a activity; DataStore segue como fonte única; API < 31 mantém o comportamento anterior, registrado como limitação. **Janela**: C. **Feito (2026-11-26)**: o modo é espelhado em `UiModeManager.setApplicationNightMode` (API 31+), que o sistema persiste por pacote e aplica antes de criar o processo; `AppearanceWindowBackgroundTest` prova `R.color.window_background` e `uiMode` do app sem recriação; `screenrecord` mostra **0 quadros de paleta oposta** nos dois modos forçados (antes: splash claro por 3,38 s em Escuro forçado). Evidência em `docs/validacao.md` §11.5.2.
- [x] T424 [US3] Provar que as fotos não são tingidas pela aparência — `app/src/androidTest/.../appearance/PhotoNeutralityTest.kt`: renderizar a mesma imagem de teste em **Claro** e **Escuro** (cartão da coleção e área de edição) e comparar a região da foto (média de canais dentro de tolerância declarada), além de a área de edição não receber véu do tema (FR-419, SC-408). **Aceite**: diferença de média de canais na região da foto ≤ 2/255 entre os modos; imagem de teste determinística; ordem de grandeza registrada. **Janela**: C, E. **Feito (2026-11-26)**: `PhotoNeutralityTest` sobre grade e miniatura: diferença de média medida **0,00/0,00/0,00** nos dois casos (imagem chapada RGB 70/120/170). A área do editor é preta fixa no código (comentário em `PhotoEditorScreen`), sem véu do tema.

**Checkpoint**: US3 entregue — aparência consistente e legível de ponta a ponta, com prova visual.

---

## Phase 5: Polish e fechamento

- [x] T419 [P] Atualizar documentação para refletir o comportamento implementado: KDoc de `Theme.kt` (coberto em T401), `docs/dependencias.md` e `docs/revisao-dependencias.md` (finalidade real do DataStore), `docs/manual/MANUAL_USUARIO_COMPLETO.md` §28 + `docs/manual/INDICE_FUNCIONALIDADES.md` (seção Aparência e capturas), `docs/validacao.md` (nova seção com medições e evidências), o ADR-006 (`docs/adr-006-preferencia-aparencia.md`) mantido atualizado e as referências cruzadas às Specs 001/003/004/005 (FR-421, §Documentação da spec). **Aceite**: nenhuma promessa documentada sem implementação; nenhuma spec anterior alterada além do registro de referência. **Janela**: não se aplica.
- [x] T420 [P] Medir NFR-401…NFR-404: tempo de aplicação do tema (< 100 ms), abertura fria (10 execuções, mediana contra 993 ms da Spec 005), tamanho do APK antes/depois (limite +100 KB) e resíduo de primeiro quadro em modo forçado por captura de quadro (limite de um quadro, ≤ 16,7 ms a 60 Hz — NFR-403). **Aceite**: números com comando usado em `docs/validacao.md`; nenhum número afirmado sem medição. **Janela**: C. **Feito (2026-11-26)**: medidos nesta máquina contra o commit anterior `54ebb20` no mesmo emulador — APK release **31.943.073 → 32.011.737 B (+67,1 KB**, limite +100 KB), abertura fria AOT **1.552 → 1.559 ms de mediana (+7 ms**; o baseline de 993 ms da Spec 005 não se reproduz aqui), tema aplicado em **36 ms de mediana** (`AppearanceApplyLatencyTest`) e resíduo de primeiro quadro por `screenrecord` + OpenCV: **nenhum quadro oposto em Sistema**; em **Escuro forçado com Android claro o fundo de janela ficava claro por ~3,38 s**, acima do limite de um quadro — medido como defeito e **corrigido em T425** (depois: 0 quadros opostos nas duas direções; API 31+). Números e comandos em `docs/validacao.md` §11.5.2.
- [x] T421 Executar a suíte completa e o lint: `./gradlew :app:testDebugUnitTest :app:lintDebug` e `./gradlew :app:connectedDebugAndroidTest`. **Aceite**: suíte verde; nenhum teste removido, ignorado ou afrouxado (NFR-407); falha intermitente registrada como limitação, não mascarada. **Janela**: C/M/E nos instrumentados. **Feito (2026-11-26)**: unidade **87/87**, lint **BUILD SUCCESSFUL** e instrumentados **235/235** em 17m47s (`build-logs/full-connected-006-splash.log`), já com o teste de latência e o de fundo de janela da correção do splash. Nenhum teste removido, ignorado ou afrouxado.
- [x] T422 Confirmar ausência de mudança fora da apresentação: `git diff --stat` de `core/domain`, `core/data` (salvo o mínimo documentado), esquema Room, `applicationId`, `app_name`, rotas, ícone, tipografia base e iconografia (FR-421, FR-427, SC-411). **Aceite**: diff vazio ou justificado linha a linha; `build.gradle.kts` sem dependência nova; `core:ui` sem import ou dependência de `core:data`/`app` (NFR-406); nenhuma cor nova fora de `Color.kt`; tema de janela preservado. **Janela**: não se aplica.

---

## Dependências e ordem

- **Setup (T401–T405)**: T401 e T402 em paralelo; T403 depende de T401/T402; T404 e T405 em paralelo.
- **US1 (T406–T410)**: T406/T407 dependem de T401, T402, T405; T408 depende de T401/T402; T409 depende de T406; T410 depende de T408.
- **US2 (T411–T413, T423)**: depende de T402 e T408.
- **US3 (T414–T418, T424)**: depende de T408; T416 e T424 dependem de T414 (superfícies revisadas).
- **Polish (T419–T422)**: T421 por último; T419/T420 em paralelo; T422 depois de T421.
- Nenhuma tarefa toca domínio, banco, migração, `applicationId` ou `app_name`.

## Estado da implementação (2026-11-26)

**Entregue e verificado por build**

- `core:ui` — `ThemeMode.kt` (enum + `isDark`) e `DiecastTheme(mode = ThemeMode.SYSTEM)` com KDoc corrigido; nenhuma chamada sem parâmetro precisou mudar (FR-405, FR-429).
- `app/.../appearance/AppearancePreferences.kt` — `appearance_mode` (`system`/`light`/`dark`), `Flow<ThemeMode>` tolerante a ausência/valor desconhecido/falha, `suspend setMode`; `di/PreferencesModule.kt` com o dono único do `DataStore` (FR-404, FR-412–FR-416, NFR-405, NFR-406).
- `MainActivity` — observa o modo (`collectAsState(initial = SYSTEM)`, sem `runBlocking`), alimenta `DiecastTheme` e reaplica as barras em `SideEffect` via `SystemBarStyle.auto(…) { dark }` (FR-406, FR-407, FR-409).
- Ajustes — seção **Aparência** em `Column.selectableGroup()` com `RadioButton`, linha inteira tocável, `heightIn(min = 48.dp)`, aviso inline de falha de escrita; `SettingsViewModel` expõe `appearance` e `setAppearance` (FR-402, FR-403, FR-414, FR-424, FR-425).
- `values/colors.xml` (`#F4F1EE`) + `values-night/colors.xml` (`#121212`) para o fundo da janela (FR-410).
- Correção do splash em modo forçado (T425): `ApplicationNightMode` espelha o modo em `UiModeManager.setApplicationNightMode` (API 31+), o sistema aplica a configuração antes de criar o processo e `configChanges="uiMode"` evita recriação; medido 0 quadros de paleta oposta nas duas direções.
- Testes de unidade `ThemeModeTest` e `AppearancePreferencesTest` (ausência ⇒ Sistema, ida/volta, valor desconhecido, escrita concorrente, codificação estável, gravação em disco) — **verdes**.
- Teste instrumentado `AppearanceSettingsTest` (três opções, padrão Sistema, seleção, alvo ≥ 48 dp, fonte 200% sem truncar, coluna de 1.000 dp, aviso de falha, precedência do modo forçado sobre a configuração do Android) — **8/8 verdes** no emulador `hw_test` (API 36). A primeira execução reprovou `modoForcadoPrevaleceSobreOTemaDoAndroid` por chamar `setContent` três vezes no mesmo teste (proibido pelo `ComposeContentTestRule`); o teste foi reescrito para compor as três amostras em um único `setContent` e voltou a passar.
- Verificação manual no emulador `hw_test` (API 36), app `br.com.diecastcollector.debug`, via `adb shell uiautomator dump` + `screencap`:
  - Ajustes → Aparência com **Sistema** marcado por padrão (`checked=true`).
  - Toque em **Claro**/**Escuro** move a marca e troca a paleta na hora; a marca sempre reflete o modo aplicado.
  - Barras do sistema acompanham o app: ícones claros em Escuro e escuros em Claro, inclusive nos casos divergentes (Claro com Android escuro, Escuro com Android claro).
  - **Sistema** seguiu `cmd uimode night yes/no` com o app aberto, sem reiniciar.
  - Escuro sobreviveu a `am force-stop` + reabertura, a `install -r` por cima e ao reinício do aparelho (`adb reboot`).
  - Capturas versionadas: `docs/validacao-006-t409-fonte-200.png` (fonte 200%) e `docs/validacao-006-t413-1..4-*.png` (persistência) — detalhes e comandos em `docs/validacao.md` §11. As da matriz modo × Android ficam em `build-logs/ui/` (`01`–`07`), diretório local fora do controle de versão.
- `AppNavHost` — rótulos das abas extraídos para `TabLabel` (e, no diagnóstico, descoberto o cache de build estale que quebrava a aba Desejos): com fonte ampliada o texto da aba para de crescer em 1× em vez de ser cortado com reticências (barra inferior e trilho); nome completo segue no `contentDescription` do ícone. Correção do defeito da shell registrado na verificação inicial (spec 005), documentada em `docs/validacao.md` §11.4.
- `docs/dependencias.md` corrigido quanto à finalidade real do DataStore.

**Testes de aparência entregues (T410–T418, T420, T424)**

- 26 casos instrumentados verdes: `AppearanceSettingsTest` (8), `AppearancePersistenceTest` (2), `AppearanceApplicationTest` (1), `AppearanceSystemFollowTest` (1), `AppearanceFallbackTest` (3), `AppearanceGlobalTest` (1), `RenderedContrastTest` (2), `SystemBarsAppearanceTest` (1), `AppearanceCapturesTest` (4), `PhotoNeutralityTest` (1), `AppearanceApplyLatencyTest` (1, NFR-401) e `AppearanceWindowBackgroundTest` (1, FR-410). Evidência e checklist em `docs/validacao.md` §11.5.
- A varredura de T414 achou um defeito real de paleta (papéis `surfaceContainer*`/`outline`/`secondaryContainer` não definidos ⇒ lavanda do Material 3 em cartões e barra). Corrigido em `Theme.kt` com os tokens de `Color.kt`; antes/depois em `docs/validacao-006-paleta-antes.png` e `docs/validacao-006-t418-ajustes-*.png`.
- Correção do defeito da shell (fonte 200%): rótulos das abas não truncam mais; `NavigationBarTest` + `AppShellResponsiveTest` 8/8.
- `docs/dependencias.md`, manual (§28) e índice funcional atualizados; `docs/validacao.md` §11.5 traz testes, medições e limites.

**Ainda em aberto**

- **FR-410 / NFR-403 apenas em API 26–30**: a correção usa `UiModeManager.setApplicationNightMode`, que existe a partir do **API 31**. De 26 a 30 o app continua correto, mas o splash segue o Android até o primeiro quadro (mesmo comportamento anterior, ~3,38 s medidos); registrado em `docs/validacao.md` §11.5.2/§11.5.4.
- Resolvido: o `NoClassDefFoundError` de `ComposableSingletons$WishlistScreenKt` em `NavigationBarTest` vinha de **entrada estale do cache de build** para o dex (a classe existia no output do Kotlin e faltava no APK). Rebuild com `--no-build-cache` corrigiu; `NavigationBarTest` + `AppShellResponsiveTest` → 8/8 verdes e o app abre Desejos sem crash (`docs/validacao.md` §11.4.1).
- T422 verificado: sem mudança em `app/build.gradle.kts`, `gradle/libs.versions.toml`, `core/data`, `core/domain`, esquema Room, `applicationId`, `app_name` ou rotas; `AppNavHost` mudou apenas o rótulo das abas (`TabLabel`).

## Riscos operacionais

- Dois `DataStore` no mesmo arquivo provocam exceção em runtime: T402 fixa um único dono `@Singleton`; T411 exercita recriação e T413 documenta a reabertura real.
- `captureToImage()` pode ser instável no emulador por software: T416 declara tolerância e, se necessário, registra a limitação **sem** afirmar que elementos renderizados foram verificados.
- Reinício de aparelho e `install -r` não são automatizáveis com honestidade: T413 exige evidência manual datada.
