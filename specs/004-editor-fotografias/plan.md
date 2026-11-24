# Implementation Plan: Editor Integrado de Fotografias

**Branch**: `004-editor-fotografias` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-editor-fotografias/spec.md`

## Summary

Adicionar ao aplicativo um editor de fotografias completo and integrado: revisão pós-captura (Editar / Salvar sem editar / Tirar novamente / Cancelar), editor não destrutivo com enquadramento, giro, espelho, brilho, contraste, saturação, nitidez, zoom, comparação antes/depois, desfazer/refazer, restauração do original e escolha de qualidade de exportação, disponível para capturas novas, imagens da galeria e fotos já cadastradas.

A edição é **não destrutiva por derivação controlada**: no máximo dois arquivos por fotografia — o original preservado e uma derivada ativa. A derivada é o que `photo.path` referencia (grade, miniatura, visualização, backup); o original fica em `photo.original_path`. Editar de novo sempre parte do original, nunca da derivada, e a derivada anterior é apagada depois do commit. "Restaurar original" apaga a derivada e volta `path` para o original.

O processamento usa a API de gráficos da plataforma (`Bitmap`, `Matrix`, `Canvas`, `ColorMatrix`, convolução 3×3), já disponível e testada, sem nova dependência (R.13).

## Technical Context

**Language/Version**: Kotlin 2.1.0 / JVM 17, Android `minSdk 26`, `targetSdk 36`

**Primary Dependencies**: Jetpack Compose (BOM 2025.01.00), Material 3, Hilt 2.53.1, Room 2.6.1, Coil 2.7.0, kotlinx-coroutines 1.9.0. Nenhuma dependência nova.

**Storage**: Room/SQLite versão 4 (migração 3→4 aditiva em `photo`) + arquivos no armazenamento privado (`filesDir/images/<exemplarId>/`).

**Testing**: JUnit4 + Truth + Robolectric (JVM), testes de banco real com Room em memória, Compose UI Test (instrumentado), testes de migração existentes em `MigrationTest`.

**Target Platform**: Android 8.0+ (API 26), uso primário em celular, offline.

**Project Type**: aplicativo Android nativo modular (`app`, `core:domain`, `core:data`, `core:ui`).

**Performance Goals**: revisão pós-captura < 1 s; pré-visualização de ajuste < 300 ms; salvar edição < 3 s para foto de 1600 px; interface nunca bloqueada.

**Constraints**: sem dependência nova; imagem ≤ 1600 px de aresta e ~1,5 MB; não recomprimir sobre o original; nenhuma migração destrutiva; nenhuma alteração em cadastros, coleções, financeiro, busca ou navegação.

**Scale/Scope**: 5 módulos, ~10 arquivos novos e ~12 alterados; 8 categorias de fotografia existentes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Verificação | Status |
|---|---|---|
| Art. I.6 / I.7 — nada de perda silenciosa | original preservado; exclusão mantém regra atual | ✅ |
| Art. I.8 — trilha auditável | eventos `photo.edited` e `photo.restored` via `AuditLogWriter` | ✅ |
| Art. I.10 / R.8 — migração não destrutiva | `ALTER TABLE photo ADD COLUMN` nulos, migração 3→4 testada | ✅ |
| Art. II.1–II.4 — modularidade | modelo puro no domínio, processamento no `core:data`, UI em `feature/photoeditor` | ✅ |
| Art. II.5 / R.4 — sem duplicação, decisão registrada | uma única implementação de processamento; ADR-004 | ✅ |
| Art. IV.1 / IV.4 / IV.6 — pt-BR, feedback, acessibilidade | strings em `strings.xml`, `contentDescription`, alvos ≥ 48 dp | ✅ |
| Art. IV.9 — ação destrutiva com confirmação | descartar edição e restaurar original exigem confirmação | ✅ |
| Art. V.1–V.4 — conclusão exige teste | testes de domínio, dados e UI; falhas não serão silenciadas | ✅ |
| Art. V.6–V.7 — erros tipados e log correlacionável | `DomainError.Storage` com mensagem pt-BR | ✅ |
| R.2 / R.3 — arquivo fora do banco, EXIF/limite ao persistir | reaproveita `ImageStore`; derivada passa pelo mesmo pipeline | ✅ |
| R.9 — permissões mínimas | câmera em contexto; galeria por Photo Picker; recusa tratada | ✅ |
| R.12 — nada bloqueia a thread principal | todo processamento em `Dispatchers.Default/IO` | ✅ |
| R.13 — impacto de dependência | nenhuma dependência nova | ✅ |
| R.6 — backup testado com imagens | coleta inclui `original_path`; teste de backup cobre ambos | ✅ |

Sem violações; nenhuma exceção a registrar.

## Project Structure

### Documentation (this feature)

```text
specs/004-editor-fotografias/
├── spec.md
├── plan.md                 # este arquivo
├── research.md             # decisões e alternativas (ADRs)
├── data-model.md           # entidade, colunas e migração
├── quickstart.md           # roteiro de validação manual
├── contracts/
│   ├── photo-repository.md # contrato de domínio da edição
│   └── photo-editor.md     # contrato do processador de imagem
├── tasks.md
└── analysis.md
```

### Source Code (repository root)

```text
core/domain/src/main/kotlin/br/com/diecastcollector/domain/
├── model/PhotoEdit.kt                      # NOVO: parâmetros + matemática de corte
├── model/CatalogModels.kt                  # ALTERADO: Photo ganha originalPath/editedAt/editParams
└── repository/ItemRepositories.kt          # ALTERADO: saveEdit / restoreOriginal

core/data/src/main/kotlin/br/com/diecastcollector/data/
├── storage/ImageStore.kt                   # ALTERADO: saveDerived
├── storage/PhotoProcessor.kt               # NOVO: processamento Bitmap (corte/giro/cor/nitidez)
├── storage/FileBackupService.kt            # ALTERADO: coleta path + original_path
├── db/entity/PhotoEntity.kt                # ALTERADO: 3 colunas opcionais
├── db/AppDatabase.kt                       # ALTERADO: versão 4 + MIGRATION_3_4
├── db/dao/ItemDao.kt                       # ALTERADO: update de edição/restauração
├── mapper/ItemMappers.kt                   # ALTERADO: novos campos
├── repository/PhotoRepositoryImpl.kt       # ALTERADO: salvar edição, restaurar original
└── di/DatabaseModule.kt                    # ALTERADO: bind do PhotoProcessor

app/src/main/java/br/com/diecastcollector/feature/photoeditor/
├── PhotoEditState.kt                       # NOVO: estado, undo/redo, proporções
├── PhotoEditorViewModel.kt                 # NOVO: orquestra processamento e salvamento
├── PhotoEditorScreen.kt                    # NOVO: UI do editor (ferramentas, zoom, comparação)
└── PhotoReviewDialog.kt                    # NOVO: revisão pós-captura

app/src/main/java/br/com/diecastcollector/feature/catalog/
├── PhotoCapture.kt                         # ALTERADO: revisão pós-captura
├── PhotoGrid.kt                            # ALTERADO: ação editar em foto cadastrada
├── ExemplarDetailScreen.kt                 # ALTERADO: abre editor de foto existente
├── ExemplarDetailViewModel.kt              # ALTERADO: onSavePhotoEdit / onRestorePhotoOriginal
└── PhotoEditDialog.kt                      # NOVO: ponte entre grade e editor

app/src/main/res/values/strings.xml         # ALTERADO: textos pt-BR do editor
docs/editor-fotografias.md                  # NOVO: documentação técnica

core/data/src/test/...                      # PhotoRepositoryTest, MigrationTest, BackupServiceTest
app/src/test/...                            # PhotoEditTest, PhotoEditStateTest
app/src/androidTest/...                     # PhotoEditorScreenTest, PhotoCaptureTest
```

**Structure Decision**: mantém a arquitetura vigente — regra e modelo no `core:domain`, processamento e persistência no `core:data`, telas e ViewModels em `app/feature`. O editor é uma funcionalidade de apresentação do módulo `app` que consome contratos de domínio, sem criar módulo novo.

## Complexity Tracking

Nenhuma violação da constituição. A única complexidade adicional — dois arquivos por fotografia — é exigência direta da preservação não destrutiva (FR-214/FR-215) e está limitada a uma derivada ativa.
