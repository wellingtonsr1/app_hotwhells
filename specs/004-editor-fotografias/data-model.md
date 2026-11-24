# Data Model — Editor Integrado de Fotografias

## Alterações de esquema

### Tabela `photo` (Room `PhotoEntity`, schema 4)

| Coluna | Tipo | Nulo | Origem | Regra |
|---|---|---|---|---|
| `id` | TEXT | não | existente | UUID estável (Art. I.4) |
| `exemplar_id` | TEXT | não | existente | FK → `exemplar.id`, `RESTRICT` (Art. I.5) |
| `angle` | TEXT | não | existente | uma de `PhotoAngle` |
| `path` | TEXT | não | existente | **arquivo exibido**: derivada quando editada, original quando não |
| `original_path` | TEXT | **sim** | nova | arquivo original preservado; `NULL` = nunca editada |
| `edit_params` | TEXT | **sim** | nova | parâmetros da última edição (string compacta versionada, `PhotoEdit.toParameters()`) |
| `edited_at` | INTEGER | **sim** | nova | epoch millis da última edição |
| `checksum` | TEXT | não | existente | SHA-256 do arquivo em `path` |
| `is_primary` | BOOLEAN | não | existente | principal do exemplar |
| `sort_order` | INTEGER | não | existente | ordem na grade |
| `width`,`height` | INTEGER | sim | existente | dimensões do arquivo em `path` |
| `bytes` | INTEGER | sim | existente | tamanho do arquivo em `path` |
| `captured_at` | INTEGER | sim | existente | data da captura |

**Invariantes**

1. `original_path IS NULL` ⟺ a fotografia nunca foi editada; nesse caso `path` é o original.
2. `original_path IS NOT NULL` ⟹ `original_path != path` e o arquivo original existe enquanto o registro existir.
3. `edit_params` e `edited_at` só existem junto de `original_path` (sem original não há edição).
4. Apagar a fotografia apaga `path` e, quando diferente, `original_path`.
5. Restaurar o original zera `original_path`, `edit_params`, `edited_at` e aponta `path` de volta ao original.

### Migração `MIGRATION_3_4`

```sql
ALTER TABLE photo ADD COLUMN original_path TEXT;
ALTER TABLE photo ADD COLUMN edit_params TEXT;
ALTER TABLE photo ADD COLUMN edited_at INTEGER;
```

Aditiva, determinística, sem reescrita de linhas, sem perda de dados (Art. I.10 / R.8). O índice parcial de `variant` é removido antes da validação e recriado no callback, como nas migrações 1→2 e 2→3.

## Modelo de domínio

### `Photo` (core/domain)

```kotlin
data class Photo(
    val id: String,
    val exemplarId: String,
    val angle: PhotoAngle,
    val path: String,
    val checksum: String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    val width: Int? = null,
    val height: Int? = null,
    val bytes: Long? = null,
    val capturedAt: Instant? = null,
    val originalPath: String? = null,
    val editedAt: Instant? = null,
    val editParams: String? = null,
) {
    val isEdited: Boolean get() = originalPath != null
}
```

### `PhotoEdit` (core/domain/model/PhotoEdit.kt)

| Campo | Faixa / domínio | Neutro |
|---|---|---|
| `crop` | `NormalizedRect` (0..1, left<right, top<bottom) | tela cheia |
| `aspect` | `PhotoAspect` (FREE, ORIGINAL, SQUARE, PORTRAIT_4_5, LANDSCAPE_16_9) | ORIGINAL |
| `rotationQuarterTurns` | 0..3 | 0 |
| `flipHorizontal` | Boolean | false |
| `straightenDegrees` | -15f..15f | 0 |
| `brightness` | -1f..1f | 0 |
| `contrast` | -1f..1f | 0 |
| `saturation` | -1f..1f | 0 |
| `sharpness` | 0f..1f | 0 |
| `quality` | `PhotoQuality` (HIGH, BALANCED, COMPACT) | BALANCED |

Regras puras (testáveis sem Android):

- `withAspect(ratio)` recalcula `crop` centrado, maior retângulo com a proporção dentro da imagem.
- `withCrop(rect)` normaliza e limita a 0..1, tamanho mínimo 5% da imagem.
- `isNeutral` — verdadeiro quando não há nenhuma alteração; usado para "Salvar sem editar" e para não gravar derivada à toa.
- `clamp()` aplicado em toda atualização vinda da UI.
- `toParameters()`/`fromParameters()` para `edit_params`: formato `v1;crop=…;aspect=…;rot=…;…;quality=…`, versionado e tolerante a texto corrompido (volta à edição neutra), sem dependência de serialização no módulo de domínio.

### `PhotoProcessor` (core/data/storage)

```kotlin
interface PhotoProcessor {
    suspend fun preview(source: ByteArray, edit: PhotoEdit): ProcessedImage
    suspend fun export(source: ByteArray, edit: PhotoEdit): ProcessedImage
}

data class ProcessedImage(val bytes: ByteArray, val width: Int, val height: Int)
```

Pipeline, nesta ordem:

1. decodificar com amostragem (`inSampleSize`) para no máximo 2× a aresta alvo;
2. `rotationQuarterTurns` + `straightenDegrees` + `flipHorizontal` em uma `Matrix` sobre `Canvas`;
3. recortar `crop` (em coordenadas da imagem já orientada);
4. limitar a aresta (prévia 1080 px, exportação 1600 px);
5. cor: `ColorMatrix` com brilho (soma), contraste (escala no ponto médio) e saturação (pesos de luminância);
6. nitidez: unsharp mask 3×3 proporcional a `sharpness` (no máximo 0,8 para não criar halo);
7. JPEG com a qualidade do perfil, reduzindo em passos até caber no limite de 1,5 MB.

## Diagrama de estados de uma fotografia

```text
[captura/galeria]
      │ add(origem)
      ▼
 path=original, original_path=NULL  ──editar──►  path=derivada, original_path=original
      ▲                                                │
      └────────────── restaurar original ◄─────────────┘
                                                       │
                                         editar de novo│ (sempre a partir de original,
                                                       ▼  derivada anterior é apagada)
                                          path=nova derivada
```

## Efeito nas consultas existentes

- `observePrimaryPathsByVariant` e `observeByExemplar` continuam lendo `path` — passam a exibir a derivada automaticamente (FR-217).
- `ImageStore.delete(photo.path)` precisa apagar também `photo.original_path` (feito no repositório).
- `deleteAll` já remove `images/` inteiro (originais e derivadas).
- `FileBackupService.readPhotoPaths` passa a coletar `path` e `original_path`; a restauração já copia a pasta inteira.
