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

- [ ] T401 [P] Criar `core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/ThemeMode.kt` com o enum `SYSTEM`/`LIGHT`/`DARK` e a resolução pura (`ThemeMode.isDark(systemInDark: Boolean)`) — arquivo novo. Ajustar `core/ui/.../theme/Theme.kt` para `DiecastTheme(mode: ThemeMode = ThemeMode.SYSTEM, content)`, consultando `isSystemInDarkTheme()` somente em `SYSTEM`, e corrigir o KDoc para descrever o comportamento implementado (FR-405, FR-417, FR-418, FR-429). **Aceite**: `./gradlew :app:assembleDebug` compila; nenhuma chamada existente a `DiecastTheme { … }` precisa mudar; não existe segunda paleta nem segunda função de tema; nenhum uso de `darkTheme` remanescente. **Janela**: build apenas.
- [ ] T402 [P] Criar `app/src/main/java/br/com/diecastcollector/appearance/AppearancePreferences.kt` (chave `appearance_mode`, valores `system`/`light`/`dark`, `Flow<ThemeMode>` tolerante a ausência/valor desconhecido/falha, `suspend fun setMode`) e `app/src/main/java/br/com/diecastcollector/di/PreferencesModule.kt` (`@Singleton` `DataStore<Preferences>`; um único dono do arquivo) — FR-404, FR-412–FR-416, NFR-405, NFR-406. **Aceite**: nenhuma dependência nova em `libs.versions.toml`; nenhum `SharedPreferences` para aparência; decodificação em funções puras. **Janela**: não se aplica.
- [ ] T403 [P] Testes de unidade do store e da resolução — `app/src/test/.../appearance/AppearancePreferencesTest.kt` e `ThemeModeTest.kt`. **Aceite**: cobre ausência ⇒ `SYSTEM`; ida/volta dos três valores; valor desconhecido ⇒ `SYSTEM`; escrita concorrente resulta em valor íntegro; falha de leitura não lança para a UI; `SYSTEM` acompanha o booleano e `LIGHT`/`DARK` o ignoram. **Janela**: não se aplica.
- [ ] T404 Criar o fundo de janela por modo: `app/src/main/res/values/colors.xml` com `window_background` claro (`#F4F1EE`) e novo `app/src/main/res/values-night/colors.xml` com `#121212`; `themes.xml` inalterado na referência (FR-410). **Aceite**: abertura com o Android no claro não mostra mais piscada escura; com o Android no escuro o fundo continua `#121212`. **Janela**: C, E.
- [ ] T405 [P] Acrescentar as strings pt-BR em `app/src/main/res/values/strings.xml`: `settings_appearance`, `settings_appearance_subtitle`, `settings_appearance_system`, `settings_appearance_light`, `settings_appearance_dark` (FR-401, IV.1). **Aceite**: nenhum texto literal na composição; os três rótulos cabem inteiros em 360 dp e fonte 200% (verificado em T408). **Janela**: C, F.

**Checkpoint**: preferência persistida, tema obediente e recursos de janela corretos; nenhuma tela alterada.

---

## Phase 2: US1 (P1) — Escolher a aparência e aplicá-la imediatamente

**Goal**: seção **Aparência** em Ajustes, ligada ao tema, com troca imediata.

**Independent Test**: em instalação limpa, abrir Ajustes, confirmar **Sistema**, tocar em **Claro** e **Escuro** e ver a superfície inteira mudar sem reiniciar e sem perder estado.

- [ ] T406 [US1] Adicionar a seção **Aparência** a `app/src/main/java/br/com/diecastcollector/feature/settings/SettingsScreen.kt`, dentro da `FormColumn` existente: título, linha de situação e três opções em `Column.selectableGroup()` com `Modifier.selectable(role = Role.RadioButton)` + `RadioButton`, recebendo `appearance: ThemeMode = ThemeMode.SYSTEM` e `onAppearanceChange: (ThemeMode) -> Unit = {}` (FR-402, FR-403, FR-424, FR-425). **Aceite**: nenhuma rota nova; seleção única; marca visível que não depende só de cor; toque em qualquer ponto da linha seleciona; seção herda 720 dp e rolagem da Spec 005. **Janela**: C/M/E, F, P.
- [ ] T407 [US1] Estender `app/src/main/java/br/com/diecastcollector/feature/settings/SettingsViewModel.kt` com `appearance: StateFlow<ThemeMode>` e `fun setAppearance(mode: ThemeMode)`, ligando `SettingsRoute` ao store (FR-405, NFR-401). **Aceite**: ação persiste e o estado exibido reflete o aplicado; nenhuma lógica de tema duplicada no ViewModel. **Janela**: não se aplica.
- [ ] T408 [US1] Ligar `app/src/main/java/br/com/diecastcollector/MainActivity.kt`: observar o modo (`collectAsState(initial = ThemeMode.SYSTEM)`, sem `runBlocking`), passar a `DiecastTheme(mode = …)` e sincronizar as barras do sistema com `SystemBarStyle.auto(…){ escuroResolvido }` em `SideEffect` (FR-406, FR-407, FR-409, NFR-402). **Aceite**: troca aplicada sem recriação exigida; em **Sistema**, `cmd uimode night yes/no` reflete em ≤ 1 s; ícones das barras contrastam nos dois modos e com aparência forçada divergente. **Janela**: C, E; P.
- [ ] T409 [US1] Testes instrumentados da seção — `app/src/androidTest/.../appearance/AppearanceSettingsTest.kt`. **Aceite**: três opções visíveis; **Sistema** marcada na ausência de preferência; toque seleciona e persiste a marca; semântica de seleção única (estado selecionado/não); alvos ≥ 48 dp; fonte 200% sem truncar; 1.000 dp alinhado à coluna de leitura. **Janela**: C/M/E, F.
- [ ] T410 [US1] Teste de aplicação imediata e preservação de estado — `app/src/androidTest/.../appearance/AppearanceApplicationTest.kt` (parte 1): alternar Claro ↔ Escuro com formulário preenchido, lista rolada e aba atual; em **Sistema**, simular mudança de tema do Android na mesma sessão (override de configuração/Robolectric qualifiers ou equivalente; se a API de teste não permitir, registrar como evidência manual em T413) (FR-406, FR-407, FR-426, SC-404). **Aceite**: paleta muda; texto digitado, rolagem e aba permanecem; nenhuma recriação de atividade exigida (verificada pelo estado preservado); a mudança do sistema em **Sistema** é refletida em ≤ 1 s sem reiniciar. **Janela**: C, F.

**Checkpoint**: US1 entregue — o usuário escolhe, a tela inteira obedece na hora.

---

## Phase 3: US2 (P1) — A escolha persiste

**Goal**: sobrevivência a processo, aparelho, atualização e backup; padrão correto.

- [ ] T411 [P] [US2] Teste de persistência entre recriações — `app/src/androidTest/.../appearance/AppearancePersistenceTest.kt`: escolher cada modo, `ActivityScenario.recreate()` e verificar a paleta e a opção marcada; restauração de pacote não altera a aparência (FR-412, FR-415). **Aceite**: os três modos persistem após `recreate`; após restauração de backup simulado, o modo é o mesmo; nenhum dado da coleção é lido para decidir aparência. **Limite**: encerramento real do processo e reinício de aparelho ficam em T413 — `am force-stop` mataria o processo de instrumentação. **Janela**: C.
- [ ] T412 [P] [US2] Cobrir o fallback de preferência ausente/ilegível/desconhecida em teste de unidade + instrumentado (FR-404, BR-402, NFR-405). **Aceite**: nenhum crash, nenhuma exceção visível, coleção intacta, tema **Sistema**. **Janela**: C.
- [ ] T413 [US2] Evidência manual dos critérios não automatizáveis: encerramento do processo (`am force-stop`), reinício do aparelho e atualização `install -r` preservando dados, seguindo `quickstart.md` §5–§6; registrar comandos, datas e capturas em `docs/validacao.md` (SC-405, SC-406). **Aceite**: cada cenário com resultado e caminho de captura, ou declarado **não executado** — nunca "aprovado" sem evidência. **Janela**: C.
- [ ] T423 [US2] Tratar a falha de **escrita** da preferência — `appearance/AppearancePreferences.kt`, `feature/settings/SettingsViewModel.kt` e strings: erro tipado, aviso não bloqueante em pt-BR e manutenção da aparência anterior (FR-414). **Aceite**: teste com store que falha na escrita (fake no ViewModel e/ou DataStore em diretório somente leitura no teste) prova que nenhuma exceção chega à UI, o valor anterior permanece e há mensagem; nenhum diálogo modal. **Janela**: C.

**Checkpoint**: US2 entregue — a preferência cumpre o que promete.

---

## Phase 4: US3 (P2) — Consistência, contraste e acessibilidade nos dois modos

**Goal**: nenhuma superfície da paleta oposta; texto sobre foto legível; barras do sistema corretas; evidência visual.

- [ ] T414 [US3] Varredura de superfícies que ainda decidem cor pelo modo do sistema ou usam cor fixa — `app/src/main`, `core/ui/src/main`: barras do sistema, `themes.xml`, *scrims*, placeholders, editor, revisão de captura, miniatura (FR-408–FR-420, FR-423). **Aceite**: nenhuma decisão de paleta duplicada; as decisões de superfície restritas ao modo resolvido (estilo das barras do sistema em `MainActivity`, fundo de janela em `values`/`values-night`) listadas e justificadas na documentação; `Color.White`/`Color.Black` restantes justificados por comentário (legibilidade sobre imagem / área neutra); nenhum `Color(0x…)` fora de `Color.kt`. **Janela**: C/M/E, F.
- [ ] T415 [US3] Teste de aplicação global por tela representativa — `app/src/androidTest/.../appearance/AppearanceApplicationTest.kt` (parte 2): painel, catálogo com cartão com/sem foto, ficha, formulário, busca, um diálogo, uma folha, editor e estados vazios, em Claro e Escuro (FR-408, SC-407). **Aceite**: cada tela usa a paleta do modo; nenhuma superfície da paleta oposta; mesma estrutura de layout, mesmas rotas e nenhuma ação escondida nos dois modos (FR-411). **Janela**: C/M/E.
- [ ] T416 [US3] Teste de contraste renderizado — `app/src/androidTest/.../appearance/RenderedContrastTest.kt` com `captureToImage()`: texto sobre as imagens de teste **branca** e **preta** (grade, revisão, miniatura), cartões de coleção e ferramentas do editor, nos dois modos (FR-422, FR-423, SC-409). **Aceite**: ≥ 4,5:1 para texto normal e ≥ 3:1 para texto grande/UI nas regiões amostradas, com tolerância declarada; regiões determinísticas, sem amostragem aleatória. **Janela**: C, E.
- [ ] T417 [US3] Teste de contraste das barras do sistema com aparência forçada divergente do Android (FR-409). **Aceite**: ícones de status/navegação contrastam com o conteúdo em `Claro` com Android escuro e em `Escuro` com Android claro. **Janela**: C.
- [ ] T418 [P] [US3] Caminhada de capturas Claro × Escuro nas classes C/M/E/P, reaproveitando o percurso de `ResponsiveCapturesTest` da Spec 005 — `app/src/androidTest/.../appearance/AppearanceCapturesTest.kt` + checklist por linha de tela (FR-408, SC-407, SC-408). **Aceite**: par de capturas por tela e por modo; checklist com resultado e arquivo; inspeção visual registrada. **Janela**: C/M/E/P.
- [ ] T424 [US3] Provar que as fotos não são tingidas pela aparência — `app/src/androidTest/.../appearance/PhotoNeutralityTest.kt`: renderizar a mesma imagem de teste em **Claro** e **Escuro** (cartão da coleção e área de edição) e comparar a região da foto (média de canais dentro de tolerância declarada), além de a área de edição não receber véu do tema (FR-419, SC-408). **Aceite**: diferença de média de canais na região da foto ≤ 2/255 entre os modos; imagem de teste determinística; ordem de grandeza registrada. **Janela**: C, E.

**Checkpoint**: US3 entregue — aparência consistente e legível de ponta a ponta, com prova visual.

---

## Phase 5: Polish e fechamento

- [ ] T419 [P] Atualizar documentação para refletir o comportamento implementado: KDoc de `Theme.kt` (coberto em T401), `docs/dependencias.md` e `docs/revisao-dependencias.md` (finalidade real do DataStore), `docs/manual/MANUAL_USUARIO_COMPLETO.md` §28 + `docs/manual/INDICE_FUNCIONALIDADES.md` (seção Aparência e capturas), `docs/validacao.md` (nova seção com medições e evidências), o ADR-006 (`docs/adr-006-preferencia-aparencia.md`) mantido atualizado e as referências cruzadas às Specs 001/003/004/005 (FR-421, §Documentação da spec). **Aceite**: nenhuma promessa documentada sem implementação; nenhuma spec anterior alterada além do registro de referência. **Janela**: não se aplica.
- [ ] T420 [P] Medir NFR-401…NFR-404: tempo de aplicação do tema (< 100 ms), abertura fria (10 execuções, mediana contra 993 ms da Spec 005), tamanho do APK antes/depois (limite +100 KB) e resíduo de primeiro quadro em modo forçado por captura de quadro (limite de um quadro, ≤ 16,7 ms a 60 Hz — NFR-403). **Aceite**: números com comando usado em `docs/validacao.md`; nenhum número afirmado sem medição. **Janela**: C.
- [ ] T421 Executar a suíte completa e o lint: `./gradlew :app:testDebugUnitTest :app:lintDebug` e `./gradlew :app:connectedDebugAndroidTest`. **Aceite**: suíte verde; nenhum teste removido, ignorado ou afrouxado (NFR-407); falha intermitente registrada como limitação, não mascarada. **Janela**: C/M/E nos instrumentados.
- [ ] T422 Confirmar ausência de mudança fora da apresentação: `git diff --stat` de `core/domain`, `core/data` (salvo o mínimo documentado), esquema Room, `applicationId`, `app_name`, rotas, ícone, tipografia base e iconografia (FR-421, FR-427, SC-411). **Aceite**: diff vazio ou justificado linha a linha; `build.gradle.kts` sem dependência nova; `core:ui` sem import ou dependência de `core:data`/`app` (NFR-406); nenhuma cor nova fora de `Color.kt`; tema de janela preservado. **Janela**: não se aplica.

---

## Dependências e ordem

- **Setup (T401–T405)**: T401 e T402 em paralelo; T403 depende de T401/T402; T404 e T405 em paralelo.
- **US1 (T406–T410)**: T406/T407 dependem de T401, T402, T405; T408 depende de T401/T402; T409 depende de T406; T410 depende de T408.
- **US2 (T411–T413, T423)**: depende de T402 e T408.
- **US3 (T414–T418, T424)**: depende de T408; T416 e T424 dependem de T414 (superfícies revisadas).
- **Polish (T419–T422)**: T421 por último; T419/T420 em paralelo; T422 depois de T421.
- Nenhuma tarefa toca domínio, banco, migração, `applicationId` ou `app_name`.

## Riscos operacionais

- Dois `DataStore` no mesmo arquivo provocam exceção em runtime: T402 fixa um único dono `@Singleton`; T411 exercita recriação e T413 documenta a reabertura real.
- `captureToImage()` pode ser instável no emulador por software: T416 declara tolerância e, se necessário, registra a limitação **sem** afirmar que elementos renderizados foram verificados.
- Reinício de aparelho e `install -r` não são automatizáveis com honestidade: T413 exige evidência manual datada.
