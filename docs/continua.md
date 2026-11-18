# Continuar em outra máquina

Estado do projeto em 2026-11-18. A verdade tarefa a tarefa está em
[`specs/001-hot-wheels-collector/tasks.md`](../specs/001-hot-wheels-collector/tasks.md),
nos blocos «Evidências» de cada fase. Este documento é operacional.

## 1. O que está no repositório

- Fases 1–5 verificadas: fundação (banco v1, migração, Hilt, navegação), US1 (cadastro
  rápido de exemplar), US2 (cadastro em dois passos com fotos e incompletos) e US3
  (painel).
- Ciclo de vida do exemplar: lista por variante, edição, movimentação com histórico,
  arquivamento e exclusão bloqueada por dependentes (FR-028, FR-091).
- Nada além disso: busca/filtros, coleções, desejos, financeiro, relatórios/backup e
  reconhecimento ainda não têm tela.

## 2. Pré-requisitos

| Componente | Versão usada |
|---|---|
| JDK | 17 (Temurin 17.0.21) — **Java 11 não compila** (`jvmTarget = 17`) |
| Gradle | 8.11.1 (wrapper) |
| Android SDK | `platforms;android-36`, `build-tools;36.0.0` — AGP 8.9.2 |
| Emulador | API 36, x86_64, **perfil de celular** (Pixel 5) |

Neste checkout a toolchain foi baixada para `.tools/` (gitignored) e `local.properties`
aponta para ela:

```bash
export JAVA_HOME="$PWD/.tools/jdk-17"
export ANDROID_HOME="$PWD/.tools/android-sdk"
export ANDROID_AVD_HOME="$PWD/.tools/avd"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Em outra máquina, instale JDK 17 + Android SDK e crie `local.properties`:

```properties
sdk.dir=/caminho/para/Android/Sdk
```

## 3. Ordem de verificação

```bash
# a) tudo que roda em JVM
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest

# b) APK + lint (lint reprova com erro; avisos não bloqueiam)
./gradlew :app:assembleDebug :app:lintDebug

# c) fluxo real no emulador (API 36)
.tools/android-sdk/emulator/emulator -avd hw_test -no-window -no-audio \
  -gpu swiftshader_indirect -no-snapshot -accel on &
./gradlew :app:connectedDebugAndroidTest
```

Resultado de referência em 2026-11-18: 87 unitários e 20 instrumentados, 0 falhas;
lint com 0 erros e 7 avisos.

## 4. Decisões já tomadas (não reabrir sem motivo)

- **Fotos pertencem ao exemplar** (`photo.exemplar_id`, FR-020); por isso o formulário de
  variante cria também o primeiro exemplar.
- **Cadastro em dois passos**: `+ Mini` → modelo (`CreateCatalogEntryUseCase`) → variante +
  primeiro exemplar + fotos.
- **Exemplar arquivado sai do acervo e das contagens** (`status = 'OWNED'` nas consultas),
  mas aquisição, fotos, movimentações e auditoria continuam no banco (Art. I.7).
- **Exclusão física de exemplar** só sem dependentes; com qualquer vínculo o domínio
  devolve `DomainError.Blocked` e a tela oferece arquivar.
- **T048** lista modelo sem variante e variante sem cor/ano; complementar a variante exige
  tela de edição que ainda não existe.
- **T131 parcial**: rótulos estão em `strings.xml`; mensagens de snackbar dos ViewModels e
  o texto do `PlaceholderScreen` continuam em Kotlin.

## 5. Próximos passos sugeridos

1. **Avaliação e aquisição (US8 parcial)**: editar a aquisição a partir do exemplar e
   registrar avaliações (valor, data, fonte), com o rótulo «estimativa do colecionador».
   Hoje o painel mostra «Estimado» sempre zerado.
2. **US4**: busca FTS4 (a tabela já existe), filtros combináveis e visualizações.
3. **Backup/exportação mínima (US10)**: a coleção vive num único SQLite no aparelho.
4. **Variante**: exclusão/arquivamento com o mesmo padrão do exemplar e união/separação
   (T143–T145).
5. **Release**: assinatura (T136), acessibilidade (T130), strings restantes (T131),
   matriz SC→teste (T150).

## 6. Armadilhas de ambiente já encontradas

- `MigrationTestHelper` **não executa callbacks** do `RoomDatabase`: o índice parcial de
  `variant.fingerprint` é verificado abrindo o banco real.
- AGP serve saída antiga de `res/` com cache incremental: `./gradlew --stop`, apagar
  `build/` e as caches de build resolve.
- **AVD pequeno quebra testes**: com 320×640, `AddExemplarScreenTest` e dois
  `DashboardScreenTest` falham por elemento fora da tela. Recrie o AVD com `-d pixel_5`;
  não é regressão de código.
- O `gradlew` do repositório estava versionado sem bit de execução; use `./gradlew` após
  `chmod +x` (já corrigido no git).
