# Hot Wheels Collector

Aplicativo Android nativo, offline-first, para catalogar miniaturas 1:64 distinguindo
**modelo → variante → exemplar**. Toda leitura e escrita acontece no aparelho: o banco
Room local é a coleção, e nenhuma funcionalidade essencial usa rede.

Kotlin 2.1 + Jetpack Compose (Material3) + Room + Hilt, organizados em quatro módulos
Gradle: `:core:domain` (Kotlin puro), `:core:data` (Room, arquivos, import/export),
`:core:ui` (design system) e `:app` (navegação, features, DI).

A especificação funcional, o plano, o modelo de dados e a lista de tarefas ficam em
[`specs/001-hot-wheels-collector/`](specs/001-hot-wheels-collector/). Para retomar o
trabalho em outra máquina, use [`docs/continua.md`](docs/continua.md).

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

- **Fases 1–3** (fundação e US1) verificadas: banco v1, migração testada, cadastro
  rápido de exemplar validado em emulador.
- **Fase 4 (US2)** implementada — cadastro em dois passos (modelo → variante + primeiro
  exemplar + fotos) e lista de cadastros incompletos — com **verificação pendente**.
  O estado tarefa a tarefa está no bloco «Situação real — Fase 4» do `tasks.md`.
- **Fases 5–15** não iniciadas.

Nenhum dado sai do aparelho: sem conta, sem telemetria, sem permissão ampla de
armazenamento (importação e backup usam Storage Access Framework).
