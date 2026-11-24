# Contrato — `PhotoRepository` (extensões)

## Assinaturas

```kotlin
interface PhotoRepository {
    // existentes, inalteradas
    fun observeByExemplar(exemplarId: String): Flow<List<Photo>>
    fun observePrimaryPathsByVariant(variantId: String): Flow<Map<String, String>>
    suspend fun add(exemplarId: String, angle: PhotoAngle, source: ByteArray): DomainResult<Photo>
    suspend fun setPrimary(exemplarId: String, photoId: String): DomainResult<Unit>
    suspend fun remove(photoId: String): DomainResult<Unit>

    /**
     * Salva uma versão editada mantendo o original preservado.
     * - base = photo.original_path (ou photo.path, se nunca editada)
     * - grava a derivada em novo arquivo
     * - atualiza path/checksum/dimensões/bytes/edit_params/edited_at e original_path
     * - apaga a derivada anterior, se havia
     * - audita "photo.edited"
     */
    suspend fun saveEdit(photoId: String, edited: ByteArray, editParams: String): DomainResult<Photo>

    /** Apaga a derivada e volta a exibir o original; audita "photo.restored". */
    suspend fun restoreOriginal(photoId: String): DomainResult<Photo>
}
```

## Garantias

| ID | Garantia | Verificação |
|---|---|---|
| C-01 | `add` continua criando um único registro e a primeira foto como principal | `PhotoRepositoryTest` |
| C-02 | `saveEdit` não cria registro novo | contagem antes/depois |
| C-03 | `saveEdit` mantém `angle`, `isPrimary`, `sortOrder`, `exemplarId` | teste de dados |
| C-04 | `saveEdit` grava `original_path` = arquivo original e `path` = derivada | teste de dados |
| C-05 | Segunda edição substitui a derivada (2 arquivos no total) | teste de arquivos |
| C-06 | `restoreOriginal` apaga a derivada, zera `original_path`/`edit_params`/`edited_at` | teste de dados + arquivos |
| C-07 | `restoreOriginal` sem original devolve `DomainError.NotFound` sem alterar nada | teste |
| C-08 | `remove` apaga original e derivada | teste |
| C-09 | Falha de gravação da derivada não altera o registro nem apaga o original | teste de erro |
| C-10 | Toda alteração registra auditoria com id da foto e caminho | teste de auditoria |
