# Tasks: Editor Integrado de Fotografias

**Input**: Design documents from `specs/004-editor-fotografias/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: exigidos pela constituição (Art. V.1–V.5) e pela spec (seção 14).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [x] T201 [P] Criar pacote `app/.../feature/photoeditor` e arquivo de strings pt-BR do editor em `app/src/main/res/values/strings.xml`
- [x] T202 [P] Registrar decisões em `specs/004-editor-fotografias/research.md` (feito) e documentação técnica em `docs/editor-fotografias.md`

---

## Phase 2: Foundational (bloqueia as histórias)

- [x] T203 Criar `core/domain/.../model/PhotoEdit.kt` com `NormalizedRect`, `PhotoAspect`, `PhotoQuality`, `PhotoEdit` (clamp, proporções, neutro, JSON)
- [x] T204 Estender `Photo` em `CatalogModels.kt` com `originalPath`, `editedAt`, `editParams` e `isEdited`
- [x] T205 Acrescentar colunas e migração 3→4 em `PhotoEntity.kt` e `AppDatabase.kt`; atualizar schema exportado
- [x] T206 Atualizar `ItemMappers.kt` (domínio ↔ entidade)
- [x] T207 Atualizar `PhotoDao` em `ItemDao.kt` (update de edição/restauração, busca por id)
- [x] T208 Criar `core/data/.../storage/PhotoProcessor.kt` (pipeline de imagem)
- [x] T209 Adicionar gravação de derivada ao `ImageStore`/`FileImageStore`
- [x] T210 Implementar `saveEdit` e `restoreOriginal` em `PhotoRepositoryImpl` e no contrato de domínio
- [x] T211 Ligar o `PhotoProcessor` no Hilt (`DatabaseModule`) e ajustar `ImageStore.delete`

**Checkpoint**: banco, repositório e processamento prontos.

---

## Phase 3: US1 (P1) — Captura, revisão e salvamento

- [x] T212 Criar `PhotoReviewDialog` com as quatro ações
- [x] T213 Alterar `PhotoCapture.kt` para entregar a captura à revisão (câmera e galeria)
- [x] T214 Integrar revisão ao `PhotoGrid.kt` preservando a categoria selecionada
- [x] T215 Teste de UI: revisão pós-captura e "Salvar sem editar" (`PhotoCaptureTest`/`PhotoGridTest`)

---

## Phase 4: US2 (P1) — Enquadramento e orientação

- [x] T216 Criar `PhotoEditState.kt` (ferramentas, undo/redo, proporções, comparação)
- [x] T217 Criar `PhotoEditorViewModel.kt` (prévia com debounce, exportação, salvamento)
- [x] T218 Criar `PhotoEditorScreen.kt` com prévia, corte arrastável, giro, espelho, inclinação, zoom
- [x] T219 Testes de domínio do corte/proporção (`PhotoEditTest`) e de undo/redo (`PhotoEditStateTest`)

---

## Phase 5: US3 (P2) — Cor, nitidez, comparação e qualidade

- [x] T220 Adicionar sliders de brilho, contraste, saturação e nitidez com restauração individual
- [x] T221 Adicionar comparação por pressão longa e seletor de qualidade de exportação
- [x] T222 Teste instrumentado do processador (dimensões, rotação, corte, brilho, limites)

---

## Phase 6: US4 (P2) — Editar e restaurar fotos cadastradas

- [x] T223 Adicionar ação "Editar" na miniatura da foto salva (`PhotoGrid.kt`)
- [x] T224 Criar `PhotoEditDialog.kt` e ligar em `ExemplarDetailScreen.kt`
- [x] T225 Adicionar `onSavePhotoEdit`/`onRestorePhotoOriginal` em `ExemplarDetailViewModel.kt`
- [x] T226 Testes de dados: `PhotoRepositoryTest` (C-02…C-10) e de migração 3→4

---

## Phase 7: US5 (P3) — Galeria

- [x] T227 Garantir que a imagem da galeria abre o editor e que o original não é alterado
- [x] T228 Teste de UI do fluxo da galeria

---

## Phase 8: Robustez, backup e documentação

- [x] T229 Incluir `original_path` na coleta do backup e testar backup/restauração com edição
- [x] T230 Tratar imagem inválida, falta de espaço, clique duplo, descarte com confirmação e limpeza de temporários
- [x] T231 Escrever `docs/editor-fotografias.md`
- [x] T232 Rodar compilação, testes de domínio/dados/UI e registrar resultados

---

## Dependencies

- Phase 2 bloqueia todas as histórias.
- US2 depende de US1 apenas para o ponto de entrada; pode ser validada pelo diálogo direto.
- US3 depende de US2 (sliders sobre o mesmo processador).
- US4 depende de US2/US3 para o editor; a persistência depende de T203–T211.
- US5 reusa US1 (revisão) e US2 (editor).
- T229 depende de T205/T210; T232 depende de tudo.
