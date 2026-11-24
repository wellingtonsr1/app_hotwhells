# Contrato — `PhotoProcessor` e estado do editor

## `PhotoProcessor`

```kotlin
interface PhotoProcessor {
    suspend fun preview(source: ByteArray, edit: PhotoEdit): ProcessedImage
    suspend fun export(source: ByteArray, edit: PhotoEdit): ProcessedImage
}

data class ProcessedImage(val bytes: ByteArray, val width: Int, val height: Int)
```

| ID | Garantia | Verificação |
|---|---|---|
| P-01 | Executa fora da thread principal (`Dispatchers.Default`) | inspeção + teste de UI sem ANR |
| P-02 | Prévia usa aresta ≤ 1080 px; exportação ≤ 1600 px | teste instrumentado de dimensões |
| P-03 | `rotationQuarterTurns=1` troca largura/altura | teste |
| P-04 | Corte 1:1 devolve imagem quadrada | teste |
| P-05 | Brilho máximo clareia pixels; mínimo escurece | teste de pixel médio |
| P-06 | Contraste e saturação neutros não alteram pixels além do JPEG | teste de tolerância |
| P-07 | Nitidez = 0 não aplica convolução | teste de igualdade aproximada |
| P-08 | Saída é JPEG válido e ≤ 1,5 MB | teste |
| P-09 | Imagem inválida lança `IllegalArgumentException` (convertida em erro de domínio pelo chamador) | teste |
| P-10 | `Bitmap`s intermediários são reciclados | inspeção |

## Estado do editor (`PhotoEditState`)

```kotlin
data class PhotoEditState(
    val edit: PhotoEdit,
    val undoStack: List<PhotoEdit>,
    val redoStack: List<PhotoEdit>,
    val tool: EditorTool,
    val showOriginal: Boolean,
    val saving: Boolean,
    val preview: ProcessedImage?,
    val message: UiMessage?,
) {
    val canUndo: Boolean
    val canRedo: Boolean
    val hasChanges: Boolean
}
```

| ID | Garantia | Verificação |
|---|---|---|
| E-01 | Cada alteração empilha o estado anterior e limpa o redo | `PhotoEditStateTest` |
| E-02 | Desfazer/refazer percorrem a pilha na ordem correta | `PhotoEditStateTest` |
| E-03 | Restaurar tudo volta ao `PhotoEdit.NONE` mantendo o histórico desfazível | `PhotoEditStateTest` |
| E-04 | Comparar (pressionar e segurar) não altera o estado editável | teste |
| E-05 | Salvamento ignora segundo toque enquanto `saving` | teste de ViewModel |
| E-06 | Descartar com `hasChanges` pede confirmação | teste de UI |
| E-07 | Estado da edição sobrevive à recriação da tela | `SavedStateHandle` + teste instrumentado |
