# ADR-006 — Persistência da preferência de aparência em DataStore Preferences

**Status**: aceito em 2026-11-25 · **Spec**: [006-preferencia-aparencia](../specs/006-preferencia-aparencia/spec.md) · **Constituição**: R.4, R.5, R.13, II.3

## Contexto

A spec 006 decide que o usuário escolhe entre **Sistema**, **Claro** e **Escuro**, com aplicação imediata e persistência local (FR-401–FR-416). O aplicativo já tem duas paletas completas em `core/ui/.../theme/Theme.kt` e escolhe entre elas apenas por `isSystemInDarkTheme()`; não existe preferência de aparência em lugar nenhum.

O código atual tem **um** precedente de preferência local: `app/.../feature/settings/BackupReminderStore.kt`, em `SharedPreferences` (`backup_reminder`), deliberadamente fora do banco para que a restauração de um pacote não altere o lembrete. A dependência `androidx.datastore:datastore-preferences` **1.1.1** já está declarada em `gradle/libs.versions.toml` e no classpath de `app` e `core:data`, porém **sem nenhum uso** — e `docs/dependencias.md` a descreve com a finalidade "lembrete de backup", que o código não cumpre.

A decisão pendente é qual mecanismo guarda um valor único, pequeno, lido na abertura e alterado raramente, sem bloquear a thread principal e sem criar um segundo mecanismo de preferências.

## Alternativas consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| **DataStore Preferences 1.1.1** (escolhida) | Assíncrono e baseado em `Flow`; escritas transacionais (arquivo inteiro, sem estado parcial); dependência **já declarada** (sem ADR de inclusão, sem versão nova); tolera escrita concorrente; leitura em `Dispatchers.IO` embutida | Primeiro valor chega alguns milissegundos após o início (resíduo de primeiro quadro em modo forçado); uso real pode aumentar o APK, pois o R8 deixa de remover a biblioteca |
| `SharedPreferences` (padrão do lembrete) | Leitura síncrona disponível, o que eliminaria a piscada de primeiro quadro | API bloqueante e desencorajada; `apply` não garante integridade em morte de processo; criaria **dois** mecanismos de preferência para a mesma finalidade, contrariando II.5 e a própria spec 006 |
| Tabela de configuração no Room | Um único armazenamento | Migração de esquema para um dado que **não** é da coleção; contraria FR-416 e a separação preferência × coleção; leitura mais lenta para um valor de apresentação |
| Arquivo próprio (JSON/properties) | Controle total, zero dependência | Reimplementa o que o DataStore já entrega; risco de corrupção e de escrita não atômica; manutenção sem ganho |
| Cache síncrono adicional só para a abertura | Eliminaria o resíduo de primeiro quadro | Dois armazenamentos para o mesmo valor, com risco de divergência; contraria "não duplicar mecanismo" |

## Decisão

Usar **DataStore Preferences** para a chave textual `appearance_mode`, com valores canônicos `system` | `light` | `dark` e fallback `system` para ausência, valor desconhecido ou falha de leitura (data-model.md §1).

- Um único dono do arquivo: `DataStore<Preferences>` provido por `@Singleton` em `app/.../di/PreferencesModule.kt`. Nenhum `preferencesDataStore` duplicado em outro módulo ou tela.
- O store `AppearancePreferences` fica na camada de aplicação (`app/.../appearance/`) e expõe `Flow<ThemeMode>` + `suspend fun setMode`. `core:ui` não conhece DataStore: recebe apenas `ThemeMode` em `DiecastTheme` (II.3).
- O arquivo fica no armazenamento privado, **fora** do banco e **fora** do pacote de backup da coleção (`FileBackupService` copia banco + fotos + tabelas exportadas; não varre `filesDir` nem DataStore).
- Sem dependência nova: a versão continua sendo a 1.1.1 já registrada; `docs/dependencias.md` e `docs/revisao-dependencias.md` passam a declarar a finalidade real (preferências simples do app).

## Consequências

- **Positivas**: um mecanismo moderno e atômico, sem dependência nova; leitura não bloqueia a interface (FR-413, NFR-402); escrita concorrente resulta sempre em um valor íntegro (NFR-405); a preferência não interfere no backup/restauração (FR-415).
- **Negativas**: o primeiro quadro em modo forçado pode usar a paleta do sistema por poucos milissegundos; o valor inicial é `SYSTEM` e o resíduo é **medido e registrado** (NFR-403, T420), não mascarado. O APK precisa ser medido antes/depois, porque a biblioteca deixa de ser removida pelo R8 (NFR-404: +≤ 100 KB).
- **Limite conhecido**: `SharedPreferences` continua sendo o mecanismo do lembrete de backup. A coexistência é aceita porque os dados têm ciclos de vida diferentes e migrar o lembrete está fora do escopo da spec 006; o ADR registra que, numa migração futura, o destino é o mesmo DataStore, não um terceiro mecanismo.
- **Rastreabilidade**: decisão usada por FR-404, FR-412–FR-416, NFR-405 e NFR-406; alternativas detalhadas em [`specs/006-preferencia-aparencia/research.md`](../specs/006-preferencia-aparencia/research.md) §2.1.

## Registro

- Spec e plano: [`specs/006-preferencia-aparencia/spec.md`](../specs/006-preferencia-aparencia/spec.md) (FR-412, FR-428) e [`plan.md`](../specs/006-preferencia-aparencia/plan.md) §Estratégia técnica 2.
- Inventário: [`docs/dependencias.md`](./dependencias.md) §1 e [`docs/revisao-dependencias.md`](./revisao-dependencias.md) — finalidade corrigida na tarefa T419 da spec 006.
- Não há mudança de `applicationId`, banco, esquema Room, repositórios ou funcionamento offline.
