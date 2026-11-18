# Coleção de Miniaturas

Aplicativo Android nativo, offline-first, para catalogar miniaturas 1:64 de **qualquer
fabricante** — Hot Wheels, Matchbox, Mini GT, Greenlight — distinguindo
**modelo → variante → exemplar**. Fabricante é registro do banco, não enum: o nome é
digitado no cadastro e reaproveitado por sugestão. Toda leitura e escrita acontece no
aparelho: o banco Room local é a coleção, e nenhuma funcionalidade essencial usa rede.

Kotlin 2.1 + Jetpack Compose (Material3) + Room + Hilt, organizados em quatro módulos
Gradle: `:core:domain` (Kotlin puro), `:core:data` (Room, arquivos, import/export),
`:core:ui` (design system) e `:app` (navegação, features, DI).

A especificação funcional, o plano, o modelo de dados e a lista de tarefas ficam em
[`specs/001-hot-wheels-collector/`](specs/001-hot-wheels-collector/). Para retomar o
trabalho em outra máquina, use [`docs/continua.md`](docs/continua.md).

A simulação do app completo (maquete animada, não gravação de tela) está em
[`docs/simulacao-app.mp4`](docs/simulacao-app.mp4) e é gerada por
[`tools/mockup/simulacao.py`](tools/mockup/simulacao.py).

## Pré-requisitos

- **JDK 17** (Temurin 17 é o que o projeto usa). Java 11 **não** compila: o
  `compileOptions`/`jvmTarget` do projeto é 17.
- **Android SDK** com `platforms;android-36` e `build-tools;36.0.0`, apontado em
  `local.properties`:
  ```properties
  sdk.dir=/caminho/para/Android/Sdk
  ```
- Para os testes instrumentados: emulador ou aparelho com **API 36** (o debug inclui
  x86_64; o release filtra para ARM).

Neste checkout a toolchain usada na verificação foi instalada em `.tools/` (fora do git):

```bash
export JAVA_HOME="$PWD/.tools/jdk-17"
export ANDROID_HOME="$PWD/.tools/android-sdk"
export ANDROID_AVD_HOME="$PWD/.tools/avd"
```

## Comandos

```bash
# Testes de domínio, dados e ViewModel + APK de debug + lint
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest \
          :app:assembleDebug :app:lint

# Fluxo completo no emulador (catálogo → modelo → variante → exemplar)
./gradlew :app:connectedDebugAndroidTest

# Só o APK
./gradlew :app:assembleDebug
# saída: app/build/outputs/apk/debug/app-debug.apk
```

O lint reprova o build com qualquer achado; o relatório fica em
`app/build/reports/lint-results-debug.txt`.

## Estado atual

- **Fases 1–5** verificadas no emulador (API 36): banco v1, US1 (cadastro rápido de
  exemplar), US2 (cadastro em dois passos, fotos e lista de incompletos) e US3 (painel).
- **Ciclo de vida do exemplar**: lista por variante, edição de condição/embalagem/
  observações, movimentação com histórico e arquivamento. Exclusão física só sem vínculos,
  com bloqueio explicado e arquivamento como alternativa (FR-028, FR-091).
- **Fases 6–15** não iniciadas, com uma exceção pontual: a movimentação de T103 (US9) já
  é entregue dentro do detalhe do exemplar.
- Verificação de 2026-11-18: **87 testes unitários** e **20 instrumentados**, 0 falhas;
  lint com 0 erros e 7 avisos pré-existentes. Detalhes em `tasks.md`.

Os números do painel usam `acquisition` e `valuation`. Editar a aquisição e registrar
avaliações é o próximo passo — hoje o valor estimado aparece sempre zerado.

Nenhum dado sai do aparelho: sem conta, sem telemetria, sem permissão ampla de
armazenamento (importação e backup usam Storage Access Framework).
