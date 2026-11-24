# Análise de Consistência — Editor Integrado de Fotografias

**Data**: 2026-11-23 | **Artefatos**: [spec](./spec.md) · [plan](./plan.md) · [research](./research.md) · [data-model](./data-model.md) · [tasks](./tasks.md) · [quickstart](./quickstart.md)

## 1. Constituição ↔ Spec

| Requisito da spec | Princípio | Situação |
|---|---|---|
| FR-214, FR-215, FR-216 (não destrutivo, uma derivada, restaurar) | Art. I.6, I.7, R.2, R.3 | ✅ |
| FR-218 (vínculo, categoria, principal) | Art. I.4, I.5 | ✅ |
| FR-220 (data e parâmetros na trilha) | Art. I.8 | ✅ |
| FR-222/FR-223/FR-226 (erros, duplo toque, temporários) | Art. V.6, R.12 | ✅ |
| FR-224 (estado em recriação) | Art. IV.7 | ✅ |
| FR-225 (backup com original e editada) | R.6 | ✅ |
| FR-212 (qualidade sem perder detalhe) | R.3, R.13 | ✅ |
| SC-202…SC-204 (metas de tempo) | Art. V.8/V.9 — metas provisórias até medição | ✅ registradas como metas |

Nenhum requisito sem princípio; nenhum princípio da constituição exige algo que a spec contrarie.

## 2. Spec ↔ Plano

| Requisito | Cobertura no plano |
|---|---|
| FR-201–FR-204 | revisão + `PhotoReviewDialog` + `PhotoCapture` |
| FR-205–FR-207 | `PhotoEditState`, `PhotoEditorScreen` |
| FR-208 | `PhotoProcessor` assíncrono + debounce (ADR-003) |
| FR-209–FR-211 | zoom `graphicsLayer`, comparação, undo/redo |
| FR-212 | `PhotoQuality` (ADR-006) |
| FR-213 | confirmação de descarte |
| FR-214–FR-217 | ADR-001, `saveEdit`/`restoreOriginal` |
| FR-218–FR-219 | `photoId`, ação editar na grade |
| FR-220 | auditoria `photo.edited`/`photo.restored` |
| FR-221 | `PhotoAngle` intocado |
| FR-222–FR-226 | fase 8 |

## 3. Plano ↔ Tarefas

Todas as decisões do plano têm tarefa correspondente (T203–T211 para a fundação, T212–T228 para as histórias, T229–T232 para robustez). Nenhuma tarefa cria módulo novo nem adiciona dependência.

## 4. Conflitos identificados e resolução

| Conflito potencial | Análise | Resolução |
|---|---|---|
| `Photo.path` é usado por backup e miniaturas | A derivada substitui o original no caminho exibido; sem ajuste no backup, o original sumiria do pacote | T229 lê também `original_path` (já previsto no data-model) |
| `ImageStore.delete` recebe um caminho | Excluir a foto apagaria só a derivada, deixando o original órfão | T211 estende a exclusão para os dois caminhos |
| `PhotoRepository.add` já grava a captura | Se o editor também gravasse, nasceriam duas fotos | A captura grava 1 registro; "Salvar sem editar" usa esse registro; edição usa `saveEdit` |
| `PhotoGrid` é compartilhada por formulários com estado pendente | Navegar para uma rota do editor perderia o rascunho | ADR-004: diálogo de tela cheia |
| `PhotoEdit` no domínio não pode depender de `Bitmap` | Domínio puro é exigência de Art. II.6 | `PhotoEdit` sem tipos Android; processamento em `core:data` |
| Testes de processamento exigem Android | `core:data` usa Robolectric para unidade e instrumentado para UI | T222 instrumentado; lógica pura em teste JVM |
| Migração 3→4 com índice parcial de `variant` | Room valida todos os índices da tabela após migração | Migração remove/recria o índice como as anteriores |

## 5. Lacunas de teste apontadas antes da implementação

1. Não há teste atual para "original preservado depois de 5 edições" — coberto por T226 (loop de edições).
2. Não há teste atual para backup de duas versões da mesma foto — coberto por T229.
3. Não há teste atual de descarte com temporários — coberto por T230/quickstart passo 12.
4. Teste de migração 3→4 precisa exercitar a cadeia 1→4 — T226.

## 6. Veredito

Especificação, plano, modelo de dados e tarefas estão consistentes com a constituição. Nenhuma migração adicional além da 3→4 é necessária. Recomenda-se implementar na ordem das fases, com os portões G3/G4 fechados por este documento e por `tasks.md`.

## 7. Verificação pós-implementação (G6)

| Portão | Evidência |
|---|---|
| Compilação | `:app:assembleDebug` → BUILD SUCCESSFUL (APK gerado) |
| Domínio | `:core:domain:test` → 74 testes, 0 falhas |
| Dados | `:core:data:testDebugUnitTest` → 177 testes, 0 falhas (inclui migração 3→4, edição não destrutiva, backup com original) |
| App | `:app:testDebugUnitTest` → 76 testes, 0 falhas |
| Instrumentados | `PhotoProcessorTest` (8), `PhotoEditorScreenTest` (3), `PhotoReviewDialogTest`/`PhotoCaptureTest`/`PhotoGridTest` (10), `CreateCatalogEntryScreenTest` (1) — 22 testes, 0 falhas no emulador API 36 |
| Migração | `MIGRATION_3_4` exercitada em `MigrationTest`, incluindo banco real com índice parcial e cobertura de todas as versões |
| Backup | `pacoteIncluiOriginalEDerivadaDaFotoEditada` e restauração existentes passam |

Lacunas remanescentes estão declaradas em `docs/editor-fotografias.md` §12 (sem teste que force `ENOSPC`; ponte `PhotoEditDialog` sem teste instrumentado próprio; morte de processo com editor aberto não preserva rascunho de captura).
