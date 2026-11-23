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

## Pendência de desempenho (SC-003)

Não existe suíte de benchmark implementada no projeto (o módulo `benchmark` só tem
`build.gradle.kts`). Para declarar SC-003 atendido é preciso medir com uma lista de 500
variantes — por exemplo um macrobenchmark de abertura/rolagem da Coleção — e registrar o
resultado. Até lá, o critério permanece meta (Constituição V.8).
