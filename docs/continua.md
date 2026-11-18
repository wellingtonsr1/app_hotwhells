# Continuar em outra máquina

Estado do projeto no fim de 2026-11-17 e o que fazer para retomar em outro ambiente.
Este documento é operacional: a verdade sobre **cada tarefa** está em
[`specs/001-hot-wheels-collector/tasks.md`](../specs/001-hot-wheels-collector/tasks.md),
nos blocos «Situação real — Fase 4 / US2» e «T131 — parcial».

## 1. Antes de sair desta máquina

O trabalho **não está commitado**. `git status` mostra ~30 arquivos (25 modificados,
4 novos, 1 removido) e o remoto é `origin` (`wellingtonsr1/app_hotwhells`). Sem commit e
push, a outra máquina não tem nada disso.

```
git status --short
git add -A
git commit -m "US2: cadastro em dois passos, fotos no exemplar e cadastros incompletos"
git push
```

Nada aqui foi compilado. O commit registra o estado, não uma aprovação.

## 2. Pré-requisitos na outra máquina

| Componente | Versão usada | Observação |
|---|---|---|
| JDK | 17 (Temurin 17.0.21) | **Java 11 não compila**: `jvmTarget = 17` |
| Gradle | 8.11.1 | vem do wrapper (`./gradlew`) |
| Android SDK | `platforms;android-36`, `build-tools;36.0.0` | AGP 8.9.2; 8.7.x não aceita `compileSdk 36` |
| Emulador | API 36, x86_64 | o debug inclui x86_64; o release filtra para ARM |

Criar `local.properties` (não versionado):

```properties
sdk.dir=/caminho/para/Android/Sdk
```

## 3. Ordem de verificação

```bash
# a) tudo que roda em JVM — é aqui que o primeiro erro aparece
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest

# b) APK + lint (lint reprova com qualquer achado)
./gradlew :app:assembleDebug :app:lint

# c) fluxo real no emulador (API 36)
./gradlew :app:connectedDebugAndroidTest
```

Se (a) falhar por KSP/Hilt, (b) nem roda — o erro de compilação vem antes.

## 4. Suspeitos do primeiro build, em ordem

1. **Hilt**: dois bindings novos em `core/data/.../di/DatabaseModule.kt`
   (`ColorRepositoryImpl`, `SeriesRepositoryImpl`) e três ViewModels com dependências novas
   (`VariantFormViewModel`, `IncompleteEntriesViewModel`, `CatalogViewModel`).
   Sintoma: `MissingBinding` em tempo de compilação.
2. **Room**: `CarModelDao.observeWithoutVariant()` com projeção `IncompleteModelRow`
   (`NOT EXISTS` + aliases). Sintoma: erro do KSP apontando a query.
3. **Interface de domínio**: `CarModelRepository` ganhou `observeWithoutVariant()`; há
   **quatro** implementações (uma real e três fakes de teste). Um `override` esquecido
   quebra a compilação dos testes.
4. **Navegação**: rotas `catalog/variant/{modelId}` e `catalog/incomplete`; argumento
   `modelId` declarado em `navArgument`.
5. **`CatalogFlowTest`**: foi reescrito para modelo → variante → exemplar e nunca rodou.

## 5. Decisões já tomadas (não reabrir sem motivo)

- **Fotos pertencem ao exemplar** (`photo.exemplar_id`, FR-020). Por isso o formulário de
  variante cria também o primeiro exemplar; sem isso a foto não tem dono válido.
- **Cadastro em dois passos**: `+ Mini` → modelo (`CreateCatalogEntryUseCase`, devolve
  `CarModel`) → variante + primeiro exemplar + fotos (`CreateVariantUseCase` +
  `AddExemplarUseCase` + `PhotoRepository`).
- **T048 com escopo reduzido**: lista apenas modelo sem variante. Variante sem cor/ano
  exigiria tela de edição de variante, que não existe e ficou fora.
- **`ModelFormScreen` removida** (era maquete com IDs falsos). Cadastro de **veículo real**
  continua sem tela — parte de T043 que segue aberta.
- **T131 parcial**: rótulos e o texto do cartão saíram do Kotlin; mensagens de snackbar dos
  ViewModels e o texto do `PlaceholderScreen` ainda não.

## 6. Próximas tarefas sugeridas, em ordem

1. Rodar a verificação do item 3 e marcar no `tasks.md` o que ficar verde
   (T037–T039, T041–T042, T044–T047, T049).
2. Refazer as capturas `docs/e2e-1..7-*.png` — elas são do fluxo antigo e hoje enganam.
3. Testes que faltam: `CatalogViewModel`, `CreateCatalogEntryViewModel`,
   `IncompleteEntriesViewModel`.
4. Matriz SC→testes (T150) e `docs/operacao.md` (T137).
5. Só depois: edição de variante (fecha o resto de FR-029) ou US3 (painel).

## 7. Armadilhas de ambiente já encontradas

- `MigrationTestHelper` **não executa callbacks** do `RoomDatabase`: o índice parcial de
  `variant.fingerprint` é verificado abrindo o banco real, não pelo helper.
- AGP serve saída antiga de `res/` com cache incremental: sintoma é ícone/recurso vazio
  mesmo com o arquivo certo. Solução usada: `./gradlew --stop`, apagar `build/` e
  `~/.gradle/caches/build-cache-1`, e rodar com `--no-build-cache`.
- `./gradlew` sem `JAVA_HOME` apontando para 17 falha de forma confusa antes de qualquer
  tarefa.
