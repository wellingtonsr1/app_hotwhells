# Quickstart — validar a miniatura na tela Coleção

Guia de execução e validação. Não contém implementação; aponta os artefatos e os comandos.

## Pré-requisitos

- JDK 17. O build **não** funciona com o Java 11 do sistema; no ambiente do projeto:
  `export JAVA_HOME="$PWD/.tools/jdk-17"`
- Android SDK em `.tools/android-sdk` (já apontado por `local.properties`).
- Para os testes de UI: emulador `diecast_test` (API 36) ou aparelho conectado.

## Compilar

```bash
export JAVA_HOME="$PWD/.tools/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$PWD/.tools/android-sdk"

./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
```

Esperado: `BUILD SUCCESSFUL`.

## Testes de dados (sem emulador)

```bash
./gradlew :core:data:testDebugUnitTest --tests "*VariantRepositoryTest*"
```

Esperado: 10 testes, 0 falhas. Cobre: foto principal vence a ordem; variante sem foto traz
caminho nulo; sem principal vale `sort_order` (não a ordem de inserção); exemplar vendido
fornece a foto e não entra na contagem.

## Testes de interface

```bash
# sobe o emulador (headless), se necessário
export ANDROID_AVD_HOME="$PWD/.tools/avd"
.tools/android-sdk/emulator/emulator -avd diecast_test -no-window -no-audio -no-boot-anim \
  -gpu swiftshader_indirect -no-snapshot-save &

# cartões da Coleção
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.diecastcollector.feature.catalog.CatalogScreenTest

# regressão do fluxo P1 e da busca
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.diecastcollector.CatalogFlowTest,br.com.diecastcollector.feature.search.SearchScreenTest
```

Esperado: 6 testes e 7 testes, respectivamente, sem falhas.

## Validação manual no aplicativo

1. Abra a aba **Catálogo** (título «Coleção»).
2. Cadastre uma variante e adicione uma foto; marque-a como principal se houver várias.
3. Volte à Coleção:
   - o cartão mostra a foto principal à esquerda;
   - nome, fabricante, ano e quantidade continuam visíveis;
   - tocar na foto amplia; tocar no texto abre o cadastro rápido;
   - «Ver exemplares» abre a lista daquela variante.
4. Cadastre uma variante sem foto: o cartão mostra o ícone de carrinho no lugar da imagem.
5. Apague o arquivo de uma foto (fora do app) e reabra a Coleção: o cartão cai no mesmo
   marcador, sem erro e sem espaço vazio.
6. Com TalkBack ativo, percorra os cartões: a foto é anunciada com modelo/cor/ano e o
   marcador é anunciado como «Sem foto do carrinho».
7. Abra busca, incompletos, coleções, duplicados e o botão «+ Mini»: todos continuam iguais.

## Compatibilidade e dados

- Nenhuma migração é executada nesta feature. Abrir a Coleção em uma base existente não
  altera registros.
- Fotos, cadastros, backup e financeiro não são tocados; a única operação é leitura.

## Medição de SC-003 (macrobenchmark)

O módulo `benchmark` tem duas suítes: `StartupBaselineProfile` (perfil de abertura, T135) e
`CatalogBenchmark` (abertura e rolagem da Coleção com 500 variantes). A lista é semeada em
proporção de SC-005: 450 variantes com foto, 25 com arquivo ausente e 25 sem foto.

```bash
# 1. Semente: SQL + 450 miniaturas sintéticas em build/benchmark-seed/
python tools/benchmark_seed.py

# 2. Build da variante medida (não minificada, <profileable>, x86_64)
./gradlew :app:assembleBenchmark :benchmark:assembleBenchmarkBenchmark

# 3. Instala o app, abre uma vez para o Room criar o banco, insere as 500 variantes e
#    copia as imagens (usa `adb root`, porque a variante medida não é depurável)
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" bash tools/seed-benchmark-device.sh

# 4. Executa os dois benchmarks
"$ADB" install -r benchmark/build/outputs/apk/benchmarkBenchmark/benchmark-benchmarkBenchmark.apk
"$ADB" shell am instrument -w -r \
  -e class br.com.diecastcollector.benchmark.CatalogBenchmark \
  -e androidx.benchmark.enabledRules Macrobenchmark \
  -e androidx.benchmark.suppressErrors EMULATOR \
  br.com.diecastcollector.benchmark/androidx.test.runner.AndroidJUnitRunner
```

Esperado: `OK (2 tests)` e, no logcat (`CatalogBenchmark`), a linha com mediana e p90 da
abertura. Resultado da execução registrada em 2026-11-23: mediana **83 ms**, p90 127 ms
(10 aberturas, emulador `hw_test` API 36 com GPU por software).

Os passos 3 e 4 são o roteiro manual. Caminho curto equivalente:

```bash
./gradlew :benchmark:connectedBenchmarkBenchmarkAndroidTest
```

Ele instala o `:app:benchmark` e o APK de teste e roda as três suítes (`CatalogBenchmark`
com 2 testes e `StartupBaselineProfile` com 1). Verificado em 2026-11-23: 3 testes,
0 falhas. O task **não semeia** o banco: a semente do passo 3 precisa já estar no aparelho.

**Defeito corrigido em 2026-11-23**: até então esse task instalava o `:app:release` (sem
assinatura e sem x86_64) e falhava com `INSTALL_PARSE_FAILED_NO_CERTIFICATES`, apesar de
existir o build type `benchmark` no `:app`. Causa: o `matchingFallbacks += listOf("release")`
no build type `benchmark` do módulo `:benchmark` — em um módulo `com.android.test` essa
lista escolhe a variante alvo do `:app` e vence o build type de mesmo nome. Removida a
lista, o task empacota `:app:packageBenchmark` (confirmado com `--dry-run`). O fallback de
bibliotecas continua no build type `benchmark` do próprio `:app`, e a geração do perfil de
abertura (`:app:generateBaselineProfile`) segue em `nonMinifiedRelease`.

**Limite**: a medição é de emulador; ela vale como comparação entre versões, não como
desempenho de aparelho real. “Rolagem fluida” segue sem prova em hardware físico.
