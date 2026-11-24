# Editor integrado de fotografias

**Spec**: `specs/004-editor-fotografias/` · **Versão do banco**: 4 · **Dependências novas**: nenhuma

## 1. Arquitetura encontrada

Antes desta funcionalidade, uma foto era: um `ByteArray` capturado por `ActivityResultContracts.TakePicture`/`PickVisualMedia` em `feature/catalog/PhotoCapture.kt`, entregue direto ao ViewModel, comprimido por `FileImageStore` (máx. 1600 px, ~1,5 MB, JPEG, EXIF removido, rotação aplicada, escrita atômica em `.tmp`) e registrado em `photo` pelo `PhotoRepositoryImpl`. A grade (`PhotoGrid`) exibia `path` via Coil; `is_primary` era definido na primeira foto. O backup (`FileBackupService`) copiava apenas os arquivos referenciados por `photo.path`.

Consequência: a captura não tinha revisão — a foto era associada imediatamente — e não havia como editar sem sair do aplicativo.

## 2. Fluxo antigo e novo

**Antes**

```
Câmera/Galeria → bytes → ViewModel → PhotoRepository.add → grade
```

**Depois**

```
Câmera/Galeria → bytes → REVISÃO (Editar | Salvar sem editar | Tirar novamente | Cancelar)
                              │
                              ├── Salvar sem editar → PhotoRepository.add(origem)
                              └── Editar → EDITOR (corte, giro, espelho, inclinação,
                                             brilho, contraste, saturação, nitidez,
                                             zoom, comparar, desfazer/refazer,
                                             qualidade) → bytes editados
                                             → add(origem) + saveEdit(derivada)
Foto já cadastrada → "Editar" na miniatura → editor parte do original → saveEdit/restoreOriginal
```

## 3. Estratégia de preservação (não destrutiva)

- `photo.original_path` guarda o arquivo original; `photo.path` guarda o arquivo **exibido**.
- Foto nunca editada: `original_path = NULL` e `path` é o próprio original.
- Editar grava uma derivada em arquivo novo, atualiza o registro e só então apaga a derivada anterior. Isso mantém **no máximo dois arquivos por foto** e evita recomprimir sobre o original (FR-214/FR-215).
- Toda edição parte do original, nunca da derivada — cinco edições não degradam a imagem.
- "Restaurar original" apaga a derivada, volta `path` para o original e zera `original_path`/`edit_params`/`edited_at`.
- Excluir a foto apaga os dois arquivos. Excluir tudo (`deleteAll`) já removia `images/`.

## 4. Banco de dados

Migração **3 → 4**, aditiva e não destrutiva (Art. I.10, R.8):

```sql
ALTER TABLE photo ADD COLUMN original_path TEXT;
ALTER TABLE photo ADD COLUMN edit_params TEXT;
ALTER TABLE photo ADD COLUMN edited_at INTEGER;
```

`edit_params` usa um formato compacto versionado (`v1;crop=…;aspect=…;rot=…;…;quality=…`) produzido por `PhotoEdit.toParameters()` e tolerante a corrupção (`fromParameters` devolve edição neutra). O schema exportado está em `core/data/schemas/.../4.json`.

Categoria (`angle`), vínculo (`exemplar_id`), ordem e `is_primary` **não** aparecem no `UPDATE` de edição — por construção, editar não troca nenhum deles.

## 5. Processamento de imagem

`AndroidPhotoProcessor` (`core/data/.../storage/PhotoProcessor.kt`), sobre `android.graphics` (ADR-002, sem dependência nova):

1. decodifica com `inSampleSize` (foto grande não estoura memória);
2. corrige EXIF (foto da galeria) + giro 90° e espelho do usuário;
3. corta pela moldura normalizada;
4. endireita (−15° a +15°, fundo preto);
5. limita a aresta (prévia 1080 px, exportação conforme perfil);
6. cor: saturação → contraste em torno de 128 → brilho (`ColorMatrix`);
7. nitidez: unsharp mask 3×3 com força máxima 0,45 (sem halo);
8. JPEG no perfil escolhido, reduzindo qualidade/aresta até caber em 1,5 MB.

Perfis: **Alta** 1600 px/92, **Equilibrada** 1600 px/85 (padrão), **Reduzida** 1280 px/72.

Prévia e exportação usam a mesma função; a prévia tem debounce de 120 ms e roda em `Dispatchers.Default`. Na ferramenta de enquadramento, arrastar a moldura não reprocessa pixels — só redesenha a sobreposição.

## 6. Interface

`PhotoEditorDialog` é um diálogo de tela cheia (ADR-004) para não perder rascunhos de formulários em andamento. Tema escuro e Material 3 existentes.

- **Enquadrar**: proporções Livre/Original/1:1/4:5/16:9, cantos arrastáveis, mover área, aproximar/afastar.
- **Girar**: 90° esquerda/direita, espelhar, endireitar.
- **Cor**: brilho, contraste, saturação, cada um com restauração individual.
- **Nitidez**: controle + qualidade de exportação.
- **Barra superior**: fechar, desfazer, refazer, salvar (com progresso e proteção contra toque duplo).
- **Prévia**: pinça para zoom (1×–5×), arrastar, botão "ajustar à tela", selo "Editada", comparação por pressionar e segurar.
- Textos em pt-BR em `strings.xml`; alvos de toque ≥ 48 dp; `contentDescription` nas ações.

## 7. Backup

`FileBackupService.readPhotoPaths` passou a ler `path` **e** `original_path`. Como o pacote ZIP copia `images/` inteiro, original e derivada entram no backup, e a restauração (substituir ou mesclar) devolve os arquivos e os vínculos. O teste `pacoteIncluiOriginalEDerivadaDaFotoEditada` cobre o caso.

## 8. Arquivos

**Criados**

| Arquivo | Papel |
|---|---|
| `core/domain/.../model/PhotoEdit.kt` | parâmetros, corte normalizado, proporções, perfis, serialização |
| `core/data/.../storage/ImageOrientation.kt` | EXIF em um único ponto (captura e edição) |
| `core/data/.../storage/PhotoProcessor.kt` | pipeline de imagem |
| `app/.../feature/photoeditor/PhotoEditState.kt` | histórico desfazer/refazer |
| `app/.../feature/photoeditor/PhotoEditorViewModel.kt` | prévias, exportação, estado |
| `app/.../feature/photoeditor/PhotoEditorScreen.kt` | UI do editor |
| `app/.../feature/photoeditor/PhotoReviewDialog.kt` | revisão pós-captura |
| `app/.../feature/catalog/PhotoEditDialog.kt` | ponte para editar foto cadastrada |
| `docs/editor-fotografias.md` | este documento |
| testes | `PhotoEditTest`, `PhotoEditHistoryTest`, `PhotoProcessorTest`, `PhotoReviewDialogTest` |

**Modificados**

`CatalogModels.kt`, `ItemRepositories.kt`, `PhotoEntity.kt`, `AppDatabase.kt`, `ItemDao.kt`, `ItemMappers.kt`, `PhotoRepositoryImpl.kt`, `ImageStore.kt`, `FileBackupService.kt`, `DatabaseModule.kt`, `PhotoCapture.kt`, `PhotoGrid.kt`, `ExemplarDetailScreen.kt`, `ExemplarDetailViewModel.kt`, `strings.xml`, `VariantFormViewModelTest.kt`, `PhotoCaptureTest.kt`, `CreateCatalogEntryScreenTest.kt`, `MigrationTest.kt`, `PhotoRepositoryTest.kt`, `BackupServiceTest.kt`, `BackupIntegrityTest.kt`, `core/data/build.gradle.kts` (nenhuma dependência ficou).

## 9. Dependências

Nenhuma adicionada. `android.graphics`, Compose, Coil, Room e Hilt já estavam no projeto. Impacto em APK/startup: zero.

## 10. Testes

| Suíte | Resultado |
|---|---|
| `:core:domain:test` | 74 testes, 0 falhas (12 classes) |
| `:core:data:testDebugUnitTest` | 177 testes, 0 falhas (30 classes) |
| `:app:testDebugUnitTest` | 76 testes, 0 falhas (12 classes) |
| `PhotoProcessorTest` (instrumentado, emulador API 36) | 8 testes, 0 falhas |
| `PhotoEditorScreenTest` (instrumentado) | 4 testes, 0 falhas — ferramentas, giro aplicado ao salvar, arrasto da moldura, fechar sem salvar, rodapé acima da barra de navegação |
| `PhotoReviewDialogTest`, `PhotoCaptureTest`, `PhotoGridTest` (instrumentados) | 10 testes, 0 falhas |
| `CreateCatalogEntryScreenTest` (instrumentado, app real) | 1 teste, 0 falhas — inclui abrir/fechar o editor e salvar sem editar |

Cobertura dos cenários da spec: captura (1), salvar sem editar (2), abrir editor (3), corte livre/proporcional (4–5), rotação (6), espelhamento (7), brilho/contraste/saturação/nitidez (9–12), comparação (14), desfazer/refazer (15–16), restaurar (17), salvar editada (18), cancelar (19), editar existente (20 — persistência e regras no teste de repositório; a tela do editor é exercitada por `PhotoEditorScreenTest`, mas a ponte `PhotoEditDialog` a partir da ficha do exemplar não tem teste instrumentado próprio), galeria (21), categoria/vínculo/principal (22–23), sem duplicação (24), imagem inválida (25), backup (27), foto grande/aresta (28). Zoom (13) é verificado pelo processador e pela prévia da tela; a falta de espaço (26) é tratada no código, mas não há teste que force `ENOSPC`.

## 11. Compilação

```
JAVA_HOME=.tools/jdk-17 ./gradlew :app:compileDebugKotlin --offline   → BUILD SUCCESSFUL
JAVA_HOME=.tools/jdk-17 ./gradlew :app:assembleDebug --offline         → BUILD SUCCESSFUL
```

## 12. Limitações conhecidas

1. **Câmera**: a captura continua usando a câmera do sistema via intent (permissão em contexto, R.9), não um preview CameraX embutido. A revisão, a edição e o salvamento são 100% no aplicativo; não há mais necessidade de outro app de edição.
2. **Morte do processo com editor aberto**: rotação de tela é preservada (ViewModel), mas a captura ainda não salva e os ajustes da sessão são perdidos se o Android encerrar o processo. Não há persistência de rascunho de edição.
3. **Nitidez**: kernel 3×3 limita o ganho em fotos muito suaves; recuperar detalhe inexistente não é prometido (e não é feito).
4. **Zoom no modo de enquadramento**: é feito pelos botões aproximar/afastar e pelos cantos; a pinça atua na visualização das demais ferramentas.
5. **Falta de espaço**: tratada com mensagem e escrita atômica, mas não há teste automatizado que simule `ENOSPC`.
6. **Backup do sistema (Android Auto Backup)**: não foi alterado; a cópia portátil (ZIP) é a que cobre original e editada.
