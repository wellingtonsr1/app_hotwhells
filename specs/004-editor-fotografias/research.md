# Research — Editor Integrado de Fotografias

**Data**: 2026-11-23 | **Plano**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

## Inventário do que já existe (levantado antes de planejar)

| # | Investigação exigida | Achado | Consequência |
|---|---|---|---|
| 1 | Implementação atual da câmera | `app/.../feature/catalog/PhotoCapture.kt` usa `ActivityResultContracts.TakePicture` com arquivo em cache + `FileProvider`, e `PickVisualMedia` para galeria | Mantém-se; a revisão e o editor entram depois do callback |
| 2 | Fluxo de confirmação de fotografia | Não existe: a captura é entregue direto como `ByteArray` para o ViewModel | É o ponto a substituir (FR-201–FR-203) |
| 3 | Integração com a galeria | Photo Picker (`PickVisualMedia`), sem permissão especial; bytes copiados para memória | Reaproveitado; abre editor em seguida (FR-204) |
| 4 | Cadastro de fotografia | `PhotoGrid.kt` (grade + visualizador) chama `onAddPhotoBytes`; `VariantFormViewModel` guarda pendentes e `ExemplarDetailViewModel` grava direto | Grade ganha revisão e ação de editar; nada do cadastro muda |
| 5 | Entidades Room de imagens | `PhotoEntity` (id, exemplar_id, angle, path, checksum, is_primary, sort_order, width, height, bytes, captured_at) | Faltam original/edição → migração aditiva 3→4 |
| 6 | Armazenamento de arquivos | `FileImageStore`: `filesDir/images/<exemplarId>/<angle>_<ts>.jpg`, compressão JPEG, EXIF removido, rotação aplicada, escrita em `.tmp` + rename, SHA-256 | Base pronta; adiciona-se gravação de derivada e cópia do original |
| 7 | Relação foto ↔ exemplar | FK `photo.exemplar_id` → `exemplar.id` com `RESTRICT`; `PhotoRepository` é o único acesso | Edição atua por `photoId`, sem tocar no vínculo |
| 8 | Categorias de fotografia | `PhotoAngle`: FRONT, BACK, LEFT, RIGHT, TOP, BASE, PACKAGING, DETAIL (8), rótulos em `strings.xml` | Editor é agnóstico de categoria (FR-221) |
| 9 | Fotografia principal | Primeira foto vira principal; `setPrimary` em transação | Edição não mexe em `is_primary` (FR-218) |
| 10 | Bibliotecas de processamento existentes | Nenhuma: apenas `android.graphics` via `ImageStore` e Coil para exibição | Processamento escrito sobre a API de plataforma, sem dependência nova (R.13) |

## Decisões (formato ADR resumido)

### ADR-001 — Edição não destrutiva por derivação controlada

**Contexto**: é obrigatório preservar o original, evitar duplicatas e recompressões sucessivas.

**Alternativas**:
1. Sobrescrever o arquivo e guardar só parâmetros — viola "original preservado" quando o arquivo muda.
2. Histórico de N derivadas — acumula arquivos e espaço.
3. **Original + uma derivada ativa** (escolhida).

**Decisão**: guardar o original em `photo.original_path` e a derivada em `photo.path`. Nova edição sempre decodifica o original e substitui a derivada; restauração apaga a derivada. Fotos nunca editadas têm `original_path = NULL`.

**Consequências**: no máximo 2 arquivos por foto; qualidade estável entre edições; exclusão precisa apagar ambos (feito no repositório); backup precisa copiar os dois caminhos (feito).

### ADR-002 — Processamento com `android.graphics` em vez de biblioteca externa

**Contexto**: corte, giro, espelho, cor e nitidez em fotos ≤ 1600 px; R.13 pede avaliação de impacto.

**Alternativas**: (a) bibliotecas de filtro de terceiros (~1–3 MB, manutenção incerta); (b) `android.graphics` + `RenderScript` (descontinuado); (c) **`android.graphics` + `ColorMatrix` + convolução 3×3 manual** (escolhida).

**Decisão**: usar `Bitmap`, `Matrix`, `Canvas`, `ColorMatrix` e um kernel de nitidez (unsharp mask) implementado no módulo de dados.

**Consequências**: zero dependência e zero crescimento de APK; nitidez limitada a kernel 3×3, suficiente para fotos de 1600 px; testes possíveis com Robolectric/instrumentados.

### ADR-003 — Pré-visualização por bitmap processado com debounce

**Contexto**: brilho/contraste/saturação poderiam ser `ColorFilter` em tempo real, mas nitidez e corte precisam de pixels reais.

**Alternativas**: (a) duas trilhas de renderização (filtro + bitmap) — divergência entre preview e resultado; (b) **uma trilha única: processar prévia reduzida (~1080 px) em `Dispatchers.Default` com debounce de 120 ms** (escolhida).

**Decisão**: a mesma função de processamento gera prévia e exportação, mudando apenas a aresta máxima e a qualidade. Gestos de zoom/pan usam `graphicsLayer` (sem reprocessar).

**Consequências**: prévia fiel ao resultado; custo de CPU controlado pela aresta e pelo debounce; salvamento continua processando a partir do original.

### ADR-004 — Editor como diálogo de tela cheia, sem novas rotas

**Contexto**: a grade de fotos aparece dentro de formulários com estado não salvo (variante/exemplar novos) e também na tela de exemplar salvo.

**Alternativas**: (a) nova rota de navegação — exigiria transportar estado pendente e arriscaria perdê-lo (Art. IV.7); (b) **diálogo Compose de tela cheia** (escolhida).

**Decisão**: `PhotoEditorDialog` recebe a imagem em memória ou em arquivo e devolve bytes editados; quem chamou decide salvar.

**Consequências**: nenhuma alteração na navegação; funciona em qualquer contexto que tenha a grade.

### ADR-005 — Migração 3→4 aditiva

**Decisão**: `ALTER TABLE photo ADD COLUMN original_path TEXT`, `edit_params TEXT`, `edited_at INTEGER`; nada é reescrito. Teste de migração 3→4 e cadeia 1→4.

**Consequências**: fotos antigas ficam com `original_path = NULL` e são tratadas como "original = arquivo atual"; a primeira edição passa a preservar esse arquivo como original.

### ADR-006 — Qualidade de exportação

**Decisão**: três perfis — Alta (aresta 1600 px, JPEG 92), Equilibrada (1600 px, 85 — padrão), Reduzida (1280 px, 72) — com limite de ~1,5 MB já aplicado por `ImageStore`. A imagem derivada nunca ultrapassa o limite atual.

**Consequências**: escolha explícita do usuário sem quebrar R.3; detalhes de decalques preservados no perfil equilibrado/alta.

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Estouro de memória ao decodificar foto grande | `inJustDecodeBounds` + `inSampleSize` antes de decodificar (já usado no `ImageStore`) |
| Deriva órfã se o processo morrer entre gravar arquivo e commit no banco | Derivada é escrita em `.tmp` e renomeada; se o commit falhar, o repositório apaga a derivada recém-criada |
| Original perdido ao restaurar | restauração só altera o registro após confirmar que `original_path` existe no disco |
| Backup sem original | `readPhotoPaths` passa a ler `path` e `original_path` |
| Regressão no fluxo atual de fotos | testes existentes (`PhotoRepositoryTest`, `PhotoCaptureTest`, `PhotoGridTest`) continuam válidos; assinatura pública de `PhotoRepository.add` inalterada |
| Rotação de tela no editor | estado do editor em `SavedStateHandle` + `rememberSaveable` para a seleção de ferramenta |
