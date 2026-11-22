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
