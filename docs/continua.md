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
- Financeiro (US8): aquisição editável e avaliações append-only no detalhe do exemplar;
  aba própria com investido, estimado, diferença, custo médio e gastos por fabricante,
  série, coleção e mês, filtráveis por intervalo.
- Busca e filtros (US4): tela `catalog/search` com FTS4 tolerante a acentos, 13 filtros
  combináveis, chips ativos, contagem e grade/lista/agrupamentos. A galeria fotográfica no
  resultado ainda falta (T064 parcial).
- Nada além disso: coleções, desejos, vendas/trocas separadas do investido (T097),
  relatórios/backup e reconhecimento ainda não têm tela.

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

Resultado de referência em 2026-11-18: 112 unitários e 27 instrumentados, 0 falhas;
lint com 0 erros e 6 avisos.

## 4. Decisões já tomadas (não reabrir sem motivo)

- **Fotos pertencem ao exemplar** (`photo.exemplar_id`, FR-020); por isso o formulário de
  variante cria também o primeiro exemplar.
- **Cadastro em dois passos**: `+ Mini` → modelo (`CreateCatalogEntryUseCase`) → variante +
  primeiro exemplar + fotos.
- **Exemplar arquivado sai do acervo e das contagens** (`status = 'OWNED'` nas consultas),
  mas aquisição, fotos, movimentações e auditoria continuam no banco (Art. I.7).
- **Exclusão física de exemplar** só sem dependentes; com qualquer vínculo o domínio
  devolve `DomainError.Blocked` e a tela oferece arquivar.
- **Aquisição é upsert por exemplar**: editar não cria segunda aquisição; id e
  `created_at` originais são preservados.
- **Avaliação é append-only**: cada valor/dia é uma linha; valor atual = avaliação mais
  recente (FR-083–FR-084).
- **Financeiro é derivado**: investido, estimado, média e agrupamentos saem de consulta;
  nada é armazenado. Aquisição sem preço não entra no investido (custo desconhecido não
  vira zero) e a média divide só pelos exemplares com valor, informando os excluídos.
- **Moeda é BRL fixo**: FR-076 define o real como padrão e outras moedas na interface
  estão fora de escopo. Formatação de centavos/datas fica em `core/ui` (T098).
- **T048** lista modelo sem variante e variante sem cor/ano; complementar a variante exige
  tela de edição que ainda não existe.
- **Busca (US4)**: a tela é própria (`catalog/search`), não uma sexta aba. O índice FTS4
  já existia no schema v1; os gatilhos vivem no `onOpen` (idempotentes) com reconstrução
  quando a contagem diverge — não há migração v2. `skyline r34` vira `skyline* r34*`:
  o `AND` explícito do FTS4 voltou vazio no SQLite do Android. Status padrão é `OWNED`.
- **T131 parcial**: rótulos estão em `strings.xml`; mensagens de snackbar dos ViewModels e
  o texto do `PlaceholderScreen` continuam em Kotlin. O erro de preço da busca também
  está em Kotlin (`SearchViewModel.INVALID_PRICE`).

## 5. Próximos passos sugeridos

1. **T064 (US4)**: galeria fotográfica no resultado da busca e teste de desempenho
   (T058, 10.000 exemplares) — únicas pendências da fase.
2. **US5/6**: séries, coleções, progresso, desejos e vendas/trocas; T097 separa
   vendas/trocas concluídas do investido junto com US7.
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
