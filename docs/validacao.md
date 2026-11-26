# Validação no aparelho — v1

Execução da T138 em **2026-11-20**, sobre o commit `e925c98`, com o build de debug no
emulador. Este documento registra o que foi executado, o que ficou de fora e o veredito
explícito de cada critério de sucesso pedido pela tarefa (SC-003, SC-005, SC-009, SC-011,
SC-012 e SC-014).

> **Resumo:** a v1 passa em SC-003 (escopo entregue), SC-005, SC-009 (com ressalva) e
> SC-014. **SC-011 não foi medida** e **SC-012 reprova em fonte 200%**, com defeitos
> reproduzíveis listados na seção 5. A T138 **não fecha** enquanto esses pontos e os
> cenários fora do escopo não forem tratados.
>
> **Atualização de 2026-11-21:** esta é a execução histórica de 2026-11-20. Desde então os
> defeitos da T130 foram corrigidos em código, o backup/restauração/importação passaram a
> rodar no WorkManager (T134) e o release ganhou Baseline Profile (T135). **Nada disso foi
> revalidado nos 14 cenários** — a seção 8 registra o que mudou e o que continua pendente;
> os vereditos das seções 4 e 6 valem para o commit `e925c98`.

## 1. Ambiente e build

| Item | Valor |
|---|---|
| Data | 2026-11-20, entre 18:20 e 18:32 (America/Sao_Paulo) |
| Commit | `e925c98` (working tree limpo durante a execução; só docs e capturas foram criados depois) |
| Build | `:app:assembleDebug` — `0.1.0`, versionCode 1, pacote `br.com.diecastcollector.debug` |
| Aparelho | emulador `hw_test`, Android 16 (API 36), x86_64, perfil Pixel 5 (1080×2340) |
| Toolchain | JDK 17 local (`.tools/jdk-17`), Gradle 8.11.1, AGP 8.9.2 |
| Rede | **modo avião ligado** durante toda a suíte (`airplane_mode_on=1`) |

O APK de release **não** foi executado: ele filtra as ABIs `arm64-v8a`/`armeabi-v7a` e o
emulador é x86_64 (instalação falharia com `INSTALL_FAILED_NO_MATCHING_ABIS`). Tudo o que
está aqui foi verificado no build de debug, que usa o mesmo código de produção. A execução
do release assinado em aparelho ARM continua pendente e é parte do aceite da T138.

Comandos usados:

```bash
export JAVA_HOME="$PWD/.tools/jdk-17"
export ANDROID_AVD_HOME="$PWD/.tools/avd"
.tools/android-sdk/emulator/emulator -avd hw_test -no-window -no-audio \
  -no-snapshot-save -gpu swiftshader_indirect &

adb shell cmd connectivity airplane-mode enable      # airplane_mode_on=1
./gradlew :app:connectedDebugAndroidTest             # 51 testes, 0 falhas, 4m14s

adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -W -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity
adb exec-out screencap -p > docs/validacao-painel-com-dados.png

adb shell settings put system font_scale 2.0          # fonte máxima
adb shell settings put system font_scale 1.0          # restaurado ao fim
adb shell cmd connectivity airplane-mode disable
```

Para a captura com dados, o banco do app foi semeado com `sqlite3` via `run-as`
(1 fabricante, 1 modelo, 1 variante, 2 exemplares, 1 aquisição de R$ 50,00 + R$ 10,00 de
frete e 1 avaliação de R$ 70,00). É um **fixture de validação**, não um caminho do app:
as capturas servem para conferir a tela, não substituem os testes de fluxo.

## 2. Resultado da suíte

```text
$ ./gradlew :app:connectedDebugAndroidTest
Starting 51 tests on hw_test(AVD) - 16
Finished 51 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 4m 14s
EXIT=0
```

Todos os 51 testes instrumentados passaram **em modo avião**, cobrindo cadastro em dois
passos, exemplar, fotos (câmera e galeria), incompletos, catálogo, busca, painel,
financeiro, desejos, coleções, localizações, duplicados, backup, controle de dados e a
barra de navegação.

## 3. Os 14 cenários do `quickstart.md`

| # | Cenário | Status | Evidência |
|---|---|---|---|
| 1 | Offline total | **Executado (escopo v1)** | 51 testes verdes com `airplane_mode_on=1`; capturas com ícone de modo avião. Relatórios e importação não existem na v1 (ver item 7/8), então a parte do roteiro que os cita não se aplica. |
| 2 | Cadastro rápido (< 20 s) | **Executado por automação** | `CatalogFlowTest.catalogoAteDoisExemplaresDaMesmaVariante` percorre catálogo vazio → modelo → variante/fotos → primeiro e segundo exemplar em **15,7 s** — o fluxo inteiro, não só o exemplar. |
| 3 | Contagens do painel | **Executado** | Painel com 2 exemplares, 1 modelo, 1 variante, 1 fabricante; investido R$ 60,00 (50 + 10 de frete), estimado R$ 70,00, diferença R$ 10,00 — confere com o fixture. Captura: `validacao-painel-com-dados.png`. |
| 4 | Variante ≠ duplicata | **Executado por automação** | `DuplicateQueryTest` (4 testes, 0 falhas) e `DuplicatesFlowTest` (1 teste, 0 falhas). |
| 5 | Busca tolerante | **Executado por automação** | `SearchIndexTest` (5) e `SearchScreenTest` (6), 0 falhas. |
| 6 | Volume (10.000) | **Não executado** | Depende de importação, que não existe na v1; a medição de desempenho é a T058, ainda aberta. |
| 7 | Importação segura | **Fora do escopo da v1** | T107, T109, T111–T118 no bloco C do `tasks.md`. |
| 8 | PDF local | **Fora do escopo da v1** | Idem (gerador de PDF não implementado). |
| 9 | Backup ida e volta | **Parcial** | `BackupServiceTest` (4) prova ida e volta com banco e foto; `BackupIntegrityTest` (1) valida o SQLite/FTS real do Android; `BackupScreenTest` (2) cobre a tela. O caminho manual pelo seletor SAF (escolher arquivo, confirmar, reabrir) **não foi executado**. |
| 10 | Interrupção (rotação/morte do processo) | **Não executado no aparelho** | O rascunho do exemplar usa `SavedStateHandle` e tem teste de unidade, mas não houve simulação de morte de processo em formulário aberto. |
| 11 | Acessibilidade | **Reprovado em fonte máxima** | Fonte 200% aplicada e inspecionada; defeitos na seção 5. TalkBack **não** foi exercitado. |
| 12 | Sem permissão de câmera | **Parcial** | `PhotoCaptureTest` (2) cobre recusa e captura na borda do `ActivityResultRegistry`; o app de câmera do sistema não foi aberto. A parte de “reconhecimento indisponível” é de funcionalidade fora do escopo. |
| 13 | Código de barras sem correspondência | **Fora do escopo da v1** | T121–T129 no bloco C do `tasks.md`. |
| 14 | Armazenamento cheio | **Não executado** | É a T149, ainda aberta. |

## 4. Veredito dos critérios pedidos

| SC | Critério | Veredito | Base |
|---|---|---|---|
| **SC-003** | 100% das funções essenciais funcionam em modo avião | **Verificado** (escopo da v1) | 51 testes verdes com a rede desligada; relatórios não fazem parte da v1 |
| **SC-005** | Variantes diferentes nunca contam como duplicata; exemplares da mesma variante sempre contam | **Verificado** | `DuplicateQueryTest` + `DuplicatesFlowTest`, ambos verdes |
| **SC-009** | Nenhum dado sai do aparelho sem ação explícita | **Verificado com ressalva** | Nenhum uso de API de rede no código (`grep` por `HttpURLConnection`, `okhttp3`, `java.net.URL`, `WebView`: 0 ocorrências).Ressalva: o manifesto final **declara `INTERNET` e `ACCESS_NETWORK_STATE`**, herdadas de
bibliotecas — `INTERNET` de `com.google.android.datatransport:transport-backend-cct:2.3.3`
(transporte do ML Kit) e `ACCESS_NETWORK_STATE` de `androidx.work:work-runtime:2.10.0`
(relatório de junção de manifesto); a prova de ausência de tráfego é a T139, ainda aberta |
| **SC-011** | 95% dos cadastros concluídos na primeira tentativa | **Não verificado** | Nenhuma medição com usuários; a suíte mostra todos os cadastros automatizados concluindo de primeira, o que não representa a taxa de 95% |
| **SC-012** | Fluxos principais operáveis com leitor de tela e fonte máxima | **Reprovado em fonte 200%** | Defeitos da seção 5; TalkBack não testado |
| **SC-014** | Responder “quantos exemplares”, “quantos modelos” e “quanto investi” em < 10 s ao abrir | **Verificado** | Abertura fria em **2,07 s / 2,48 s / 2,56 s** (3 execuções) e painel é a tela inicial, com os três números visíveis sem navegar |

## 5. Defeitos encontrados (entram na T130)

Todos reproduzidos no emulador, com capturas em `docs/`.

1. **Título de Ajustes sob a barra de status** — em fonte normal, “Ajustes” é desenhado
   atrás do relógio do sistema (`validacao-ajustes.png`). As outras telas respeitam o
   inset; `SettingsScreen` não aplica `statusBarsPadding`.
2. **Rótulos do painel quebram no meio da palavra em 200%** — “Exempl ares”,
   “Fabrica ntes”, “Coleçõ es”, “Variantes repetid as”
   (`validacao-fonte-200-painel.png`).
3. **Rótulos da barra inferior quebram no meio da palavra em 200%** — “Pain el”,
   “Catál ogo”, “Desej os”, “Finan ceiro”, “Ajust es”
   (`validacao-fonte-200-painel.png`, `validacao-fonte-200-catalogo.png`).
4. **FAB “+ Mini” cortado em 200%** — o texto cresce além da borda direita
   (`validacao-fonte-200-catalogo.png`).
5. **Legenda do cartão do catálogo quebra no meio em 200%** — “2 exemplare s”
   (`validacao-fonte-200-catalogo.png`).

Em fonte normal (100%) o catálogo, o painel e o FAB não apresentam esses problemas — a
comparação está em `validacao-catalogo-com-dados.png` e `validacao-ajustes.png`.

> **Reconferência de 2026-11-23 — defeito 5**: não se reproduz no código atual. O atalho
> «Ver exemplares» saiu da linha do título (commit `899ba85`), liberando a largura que
> fazia a palavra quebrar, e o subtítulo passou a ter `maxLines = 2` com reticências
> (`676a01d`). O caso agora é coberto por
> `CatalogScreenTest.emFonteDobradaAMiniaturaMantem64DpESubtituloNaoParteAPalavra`, com
> `fontScale = 2f`: 9/9 testes verdes no emulador API 36 em 2026-11-23. Reconferido por
> teste automatizado, **não** por nova captura — os defeitos 1 a 4 e a captura da fonte
> 200% continuam como registrados acima, e o veredito de SC-012 segue dependente de
> TalkBack e de nova verificação visual.

## 6. Assinatura da verificação

```text
Verificação executada por: Buffy (agente de código)
Data: 2026-11-20 18:32 (America/Sao_Paulo)
Commit: e925c98
Dispositivo: emulador hw_test, Android 16 (API 36), x86_64
Build: debug 0.1.0 (versionCode 1), pacote br.com.diecastcollector.debug
Rede: modo avião ligado

SC-003 .................. VERIFICADO (escopo da v1)
SC-005 .................. VERIFICADO
SC-009 .................. VERIFICADO COM RESSALVA (INTERNET de transport-backend-cct; ver T139)
SC-011 .................. NÃO VERIFICADO
SC-012 .................. REPROVADO (fonte 200%); TalkBack não testado
SC-014 .................. VERIFICADO (abertura fria 2,07–2,56 s)

Pendente de assinatura humana: SC-011 (medição com usuários) e TalkBack em SC-012.
```

## 7. O que falta para fechar a T138

1. Executar o release assinado em aparelho ARM (o emulador não roda o APK filtrado).
2. Fazer o ciclo manual de backup → limpar dados → restaurar pelo seletor SAF (cenário 9).
3. Simular interrupção de formulário em aparelho (rotação, segundo plano, morte de
   processo) para o cenário 10 / SC-008.
4. Corrigir os defeitos de acessibilidade e repetir a inspeção em 200%, além de exercitar
   TalkBack (T130).
5. Medir SC-011 com uso real; a evidência automatizada não substitui a taxa de 95%.
6. T149 (armazenamento cheio) e T058 (volume) para os cenários 14 e 6.
7. Cenários 7, 8 e 13 só fazem sentido se importação, relatórios e reconhecimento forem
   trazidos de volta ao escopo.

## 8. Atualização de 2026-11-21 (sem nova execução dos 14 cenários)

Esta seção não substitui a execução acima: registra o estado do código depois da
convergência e das tarefas T130, T134, T135, T154–T160. O aceite da T138 continua aberto.

### 8.1 O que mudou desde `e925c98`

- **T130 (acessibilidade)**: os cinco defeitos de fonte 200% da seção 5 foram corrigidos em
  `SettingsScreen`, `DashboardScreen`, `AppNavHost` e `CatalogScreen`. Há testes de tela,
  mas **não houve nova inspeção visual em 200% nem execução com TalkBack**.
- **T134 (segundo plano)**: backup, inspeção de pacote, restauração e aplicação da
  importação rodam em `TransferWorker` com notificação `dataSync`; fechar a tela não perde a
  operação. Um teste instrumentado enfileira um backup real e confere pacote `PK` e resumo.
- **T135 (desempenho)**: Baseline Profile empacotado; abertura fria do release x86_64 no
  emulador ficou em **1.456 ms** de mediana (10 execuções). O ganho do perfil ficou dentro
  do ruído; a meta de 2 s é cumprida mesmo sem ele.
- **T154–T160**: ações de variante (arquivar, unir, separar), confirmação de remoção de
  foto, aviso de foto repetida, data de compilação e datas de criação/alteração. Nenhuma
  delas foi incluída numa nova passagem pelos 14 cenários.

### 8.2 Verificação executada em 2026-11-21

```text
$ ./gradlew :core:domain:test :core:data:test :app:testDebugUnitTest
64 + 160 + 66 = 290 testes, 0 falhas

$ ./gradlew :app:connectedDebugAndroidTest
Starting 68 tests on hw_test(AVD) - 16
Finished 68 tests on hw_test(AVD) - 16
BUILD SUCCESSFUL in 5m 19s

$ ./gradlew :app:lintDebug
0 errors, 10 warnings
```

O emulador é o mesmo `hw_test` (API 36, x86_64). A suíte completa passou em 2026-11-21 com
a nova inicialização do WorkManager/Hilt e o teste instrumentado de backup; em modo avião
não houve nova verificação, então SC-003 continua apoiado na execução de 2026-11-20.

### 8.3 Estado dos critérios

| SC | Estado em 2026-11-21 | O que falta |
|---|---|---|
| SC-003 | Mantido | 68 testes verdes em 2026-11-21; a execução em modo avião segue a de 2026-11-20 |
| SC-005 | Mantido | Nada novo |
| SC-009 | Mantido com ressalva | T139 segue aberta para provar ausência de tráfego |
| SC-011 | **Não verificado** | Medição com uso real; nenhum teste automatizado substitui a taxa de 95% |
| SC-012 | **Corrigido em código, não revalidado** | Repetir a inspeção em fonte 200% e exercitar TalkBack; o veredito anterior continua sendo o registrado |
| SC-014 | Mantido | Abertura fria agora em 1,456 s no emulador; falta aparelho físico |

### 8.4 O que continua pendente para fechar a T138

1. Executar o release assinado em aparelho ARM.
2. Fazer o ciclo manual de backup → limpar dados → restaurar pelo seletor SAF, incluindo
   fechar o app durante o backup e conferir a notificação e o resultado ao voltar.
3. Simular morte de processo com formulário aberto (cenário 10).
4. Repetir a inspeção de acessibilidade em 200% e com TalkBack (SC-012).
5. Medir SC-011 com uso real.
6. T149 (armazenamento cheio) e T058 (volume) para os cenários 14 e 6.

### 8.5 Correção de evidência — T118

A evidência antiga da T118 dizia que «os demais tipos de relatório reusam o mesmo gerador».
Isso não corresponde ao código: a tela de relatórios gera **apenas o inventário**, em PDF,
CSV ou XLSX; os outros dez tipos não existem. A tarefa foi marcada como parcial no
`tasks.md`; o escopo não muda, mas a evidência não deve induzir a erro.

## 9. Medições da interface responsiva (spec 005, 2026-11-24)

Complementa as seções anteriores para os NFRs da spec 005. Tudo foi medido no emulador
`hotwheels_test` (API 36, x86_64, `-gpu swiftshader_indirect`), com JDK 17 e o mesmo
`local.properties` do projeto.

### 9.1 NFR-301 — reorganização em menos de 1 s

`ResponsiveComponentsTest.reorganizacaoDaGradeAbaixoDeUmSegundo` troca a largura do contêiner
de 360 para 1.000 dp e mede o tempo até a grade de 1 passar a 5 colunas. O valor sai no
logcat:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.diecastcollector.ui.components.ResponsiveComponentsTest
adb logcat -d | grep NFR-301
# I System.out: NFR-301: grade de 1 para 5 colunas em 130ms
```

| Medida | Valor | Limite | Estado |
|---|---|---|---|
| Recomposição/medida da grade (1 → 5 colunas) | **130 ms** | 1.000 ms | Atende |

A medição cobre a reorganização do layout depois que a classe é recalculada; a troca de
configuração do sistema (recriação de `Activity`) não entra no número.

### 9.2 NFR-303 — tamanho do artefato

Comparação do commit-base `000721b` com a árvore atual, compilando o mesmo módulo nos dois
lados (`git worktree add /tmp/baseline005 000721b` + `local.properties` copiado):

```bash
./gradlew :app:assembleDebug :app:assembleRelease
ls -l app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release.apk
```

| Artefato | Antes (`000721b`) | Depois | Δ | Limite |
|---|---|---|---|---|
| Debug (`app-debug.apk`) | 86.919.553 B | 87.057.909 B | **+138.356 B (+0,13 MB)** | +1 MB |
| Release (`app-release*.apk`) | 31.930.785 B | 31.943.073 B | **+12.288 B (+0,01 MB)** | +1 MB |

O release de referência é o `app-release-unsigned.apk` gerado no worktree; o atual sai
assinado com a chave de release do projeto, o que explica a diferença de poucos bytes além
do código. Ambos ficam muito abaixo de 1 MB.

### 9.3 NFR-302 e abertura — estado da medição

A abertura fria do build de **debug** no emulador por software, com `am start -W`
(`adb shell am start -W -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity`),
ficou em **4,3 s / 5,5 s / 7,9 s** em três execuções — ruído alto do `swiftshader`, sem
relação com a meta de 2 s da spec 003, que é medida no build `:app:benchmark`
(`CompilationMode.Partial` + Baseline Profile). A medição de NFR-302 (abertura de lista,
rolagem e detalhe) é a mesma de SC-003 e está registrada na seção 9.4.

### 9.4 NFR-302 — abertura da Coleção com 500 variantes

Mesmo roteiro de SC-003 (spec 003): semente de 500 variantes (450 com foto, 25 com arquivo
ausente, 25 sem foto), build `:app:benchmark` e `CatalogBenchmark`:

```bash
python3 tools/benchmark_seed.py
./gradlew :app:assembleBenchmark :benchmark:assembleBenchmarkBenchmark
ADB=$ANDROID_SDK_ROOT/platform-tools/adb bash tools/seed-benchmark-device.sh
$ADB shell am instrument -w -r \
  -e class br.com.diecastcollector.benchmark.CatalogBenchmark \
  -e androidx.benchmark.enabledRules Macrobenchmark \
  -e androidx.benchmark.suppressErrors EMULATOR \
  br.com.diecastcollector.benchmark/androidx.test.runner.AndroidJUnitRunner
```

| Medida | Antes (2026-11-23) | Depois (2026-11-24) | Meta |
|---|---|---|---|
| Abertura da Coleção — mediana (10 aberturas) | 83 ms | **147 ms** | < 1.000 ms |
| Abertura da Coleção — p90 | 127 ms | **182 ms** | < 1.000 ms |
| Abertura — máxima (1ª abertura) | 1.230 ms | 1.312 ms | — |

A abertura continua bem abaixo de 1 s, mas ficou **64 ms mais lenta na mediana**; como a
baseline foi medida em outro dia no mesmo emulador por software, a diferença está dentro da
variação da máquina (a primeira abertura, que inclui compilação, variou 82 ms entre as duas
execuções). Não há evidência suficiente para afirmar ganho nem regressão de NFR-302 na
abertura.

A metade "rolagem" **não foi medida**: `rolarColecaoCom500Variantes` falha no emulador com
`IllegalStateException: Observed no renderthread slices in trace`, porque o `swiftshader` não
emite os slices de renderização que `FrameTimingMetric` lê. É a mesma limitação já registrada
na spec 003 (rolagem fluida depende de aparelho físico); a tarefa T029 daquela spec continua
bloqueada.

### 9.5 Baseline da spec 005 (T303)

Referência anterior à mudança e estado de fechamento, ambos no `hotwheels_test`
(API 36, x86_64, `-gpu swiftshader_indirect`) com JDK 17. Diferente de §9.2–9.4, esta
tabela reúne os números que a T303 exige e fica versionada, porque `build/` é ignorado
pelo git.

| Medida | Antes (pré-005) | Agora (2026-11-25) | Δ | Limite |
|---|---|---|---|---|
| APK debug | 86.919.553 B (`000721b`) | 87.556.411 B | +636.858 B (+0,61 MB) | +1 MB |
| APK release | 31.930.785 B (`000721b`) | 31.943.073 B | +12.288 B (+0,01 MB) | +1 MB |
| Abertura fria — mediana de 10 execuções | **1.456 ms** (2026-11-21) | **993 ms** (904–1.073 ms) | −463 ms | — |

O número de 1.456 ms é o do baseline profile da spec 003 (§8.1), imediatamente antes de a
spec 005 começar; APKs de `000721b` e números de startup no mesmo emulador. Comandos:

```bash
./gradlew :app:assembleDebug :app:assembleRelease
ls -l app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release.apk

./gradlew :app:assembleBenchmark
adb install -r app/build/outputs/apk/benchmark/app-benchmark.apk
for i in $(seq 1 10); do
  adb shell am force-stop br.com.diecastcollector; sleep 2
  adb shell am start -W -n br.com.diecastcollector/br.com.diecastcollector.MainActivity
done
# TotalTime: 1001, 986, 904, 1000, 940, 1073, 927, 976, 1041, 1041 → mediana 993 ms
```

As cinco capturas compactas da T303 (painel, catálogo, busca, formulário de exemplar e
editor) ficam em `build/responsive/baseline/capturas-compactas/`, geradas por
`ResponsiveCapturesTest` (C = 411×914 dp). As capturas **anteriores** à mudança não existem
mais (o `build/` da época foi limpo); o estado de fechamento serve de referência dali em
diante, junto do conjunto completo de 184 capturas em `build/responsive/capturas/`.

### 9.6 Rotação, janela livre e duas janelas (T305/T372)

O emulador `hw_test` não emparelha split-screen e não tem hardware dobrável; a verificação
foi feita com **rotação real** e **janelas freeform**, no lugar de `wm size` (que muda o
display inteiro) e de dois lados de split.

**Rotação** — `AppShellResponsiveTest.girarParaPaisagemTrocaBarraPorTrilhoEPreservaAAba`
pede `SCREEN_ORIENTATION_LANDSCAPE` na activity de produção, espera a recriação e verifica
que o shell troca barra inferior por trilho e que a aba Financeiro continua selecionada.
7/7 verdes junto dos demais casos do shell.

**Janela livre e duas janelas** — com `enable_freeform_support=1` e
`force_resizable_activities=1`:

```bash
adb shell wm size 2625x2400; adb shell wm density 420   # display 1.000 × 914 dp
adb shell am start --windowingMode 5 -n br.com.diecastcollector/br.com.diecastcollector.MainActivity
adb shell am start -a android.settings.SETTINGS --windowingMode 5
adb shell am task resize <appTask> 0 0 945 2200          # 360 dp
adb shell am task resize <settingsTask> 945 0 2625 2200  # 640 dp
adb shell am task resize <appTask> 0 0 1837 2200         # 700 dp
```

Confirmado no `dumpsys activity activities`: `mode=freeform` nas duas tarefas e
`mGlobalConfig w360dp` / `w700dp` para o app, no mesmo display de 1.000 dp — a classe segue o
contêiner, não a tela, e os dados (500 exemplares no painel) permanecem nas duas larguras.
Com `wm density 240` (display 1.750 dp) o par ficou 1.000 dp (app) + 750 dp (Configurações).
Capturas em `build/responsive/freeform/`: C+C, C+M, M+E, além do app sozinho a 360 dp.

**Não coberto**: pareamento nativo de split-screen (o `am` desta imagem não aceita
`--bounds` e os modos 3/4 não emparelham) e **dobra física** — `cmd device_state state 1`
responde "Requested state: 1 is not supported" e não há perfil dobrável no AVD. A dobra
segue como limitação de hardware; o efeito dela (janela menor) está coberto pelos cenários
freeform acima.

## 10. Esclarecimento G2 da aparência (spec 006, 2026-11-26)

O esclarecimento aprovado em `specs/006-preferencia-aparencia/clarifications-g2.md` foi aplicado
às especificações de origem, exclusivamente por redação — nenhum comportamento visual mudou.

| ID | Documento | Alteração aplicada |
|---|---|---|
| CL-003-01 | `specs/003-colecao-miniaturas/spec.md` | bloco `Clarifications` (2026-11-26), `Status` e **FR-013** com identidade por modo de aparência |
| CL-003-02 | `specs/003-colecao-miniaturas/contracts/collection-card.md` | §Identidade visual por modo e linha «Cartão» com superfície do tema em uso |
| CL-004-01 | `specs/004-editor-fotografias/spec.md` | bloco `Clarifications` (2026-11-26), `Status` e premissa de aparência escolhida + neutralidade da edição |

**O que não mudou**: nenhum FR-001–FR-012/FR-014/FR-015 da spec 003, nenhum FR-201–FR-226 da
spec 004, nenhum SC das duas, nenhum teste e nenhum código. No modo **Escuro** o resultado visual
continua idêntico ao entregue; o modo **Claro** passa a ter respaldo documental e é verificado
pelas tarefas T401–T424 da spec 006 (FR-417–FR-423).

**Verificação**: `git diff` das duas specs e do contrato limitado aos trechos acima; reexecução de
`/speckit-analyze` na spec 006 em 2026-11-26 confirmou o fechamento de H1/M7 (0 críticos, 0 altos,
0 médios e 0 baixos após as correções de documentação).
