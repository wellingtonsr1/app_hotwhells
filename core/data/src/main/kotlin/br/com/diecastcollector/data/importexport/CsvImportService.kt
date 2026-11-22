package br.com.diecastcollector.data.importexport

import androidx.room.withTransaction
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ColorEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.PackagingCondition
import br.com.diecastcollector.domain.usecase.CreateVariantUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado da aplicação de um CSV no banco (fase 7 do contrato). */
public data class ImportApplyResult(
    val importedExemplars: Int,
    val skippedDuplicates: Int,
    val invalidRows: Int,
    val createdManufacturers: Int,
    val createdModels: Int,
    val createdVariants: Int,
    val createdColors: Int,
)

/**
 * Aplicação transacional da importação (T113, fase 7, FR-136–FR-139).
 *
 * Lê e valida de novo (nunca confia só na pré-visualização da tela) e grava, numa única
 * transação, fabricante → modelo → variante → exemplar de cada linha válida. Linhas com
 * problema ou duplicata não são gravadas; a mesma importação repetida não duplica
 * exemplares porque o `codigo_interno` já existente é pulado (FR-138).
 */
@Singleton
public class CsvImportService @Inject constructor(
    private val database: AppDatabase,
) {

    /** Pré-visualização com os códigos internos já existentes no banco. */
    public suspend fun preview(text: String): ImportPreview = withContext(Dispatchers.IO) {
        val existing = database.exemplarDao().allInternalCodes().toSet()
        CsvImportPipeline.preview(text, existing)
    }

    public suspend fun apply(
        text: String,
        now: Long = System.currentTimeMillis(),
    ): ImportApplyResult = withContext(Dispatchers.IO) {
        val existing = database.exemplarDao().allInternalCodes().toSet()
        val preview = CsvImportPipeline.preview(text, existing)

        var imported = 0
        var skippedDuplicates = 0
        var invalid = 0
        var createdManufacturers = 0
        var createdModels = 0
        var createdVariants = 0
        var createdColors = 0

        database.withTransaction {
            for (row in preview.rows) {
                if (row.isInvalid) {
                    invalid++
                    if (row.duplicate) skippedDuplicates++
                    continue
                }
                val values = row.values
                val manufacturerName = values["fabricante"].orEmpty().trim()
                val modelName = values["modelo"].orEmpty().trim()
                val scale = values["escala"].nullable() ?: "1:64"

                val manufacturer = database.manufacturerDao().findByName(manufacturerName)
                    ?: ManufacturerEntity(
                        id = newId(),
                        name = manufacturerName,
                        country = null,
                        archivedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ).also {
                        database.manufacturerDao().insert(it)
                        createdManufacturers++
                    }

                val model = database.carModelDao()
                    .findByIdentity(manufacturer.id, modelName, scale)
                    ?: CarModelEntity(
                        id = newId(),
                        manufacturerId = manufacturer.id,
                        name = modelName,
                        scale = scale,
                        brandId = null,
                        realVehicleId = null,
                        bodyType = values["tipo_carroceria"].nullable(),
                        material = values["material"].nullable(),
                        countryOfManufacture = null,
                        notes = null,
                        archivedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ).also {
                        database.carModelDao().insert(it)
                        createdModels++
                    }

                val colorName = values["cor_principal"].nullable()
                val colorId = colorName?.let { name ->
                    database.colorDao().findByName(name)?.id
                        ?: ColorEntity(id = newId(), name = name, hex = null).also {
                            database.colorDao().insert(it)
                            createdColors++
                        }.id
                }

                val releaseYear = values["ano_lancamento"].nullable()?.toIntOrNull()
                val fingerprint = CreateVariantUseCase.calculateFingerprint(
                    modelId = model.id,
                    seriesId = null,
                    primaryColorId = colorId,
                    secondaryColorIds = emptyList(),
                    wheelsType = values["rodas"].nullable(),
                    baseType = values["base"].nullable(),
                    decoration = values["decoracao"].nullable(),
                    releaseYear = releaseYear,
                    edition = null,
                    manufacturerCode = values["codigo_fabricante"].nullable(),
                )

                val variant = database.variantDao().findActiveByFingerprint(fingerprint)
                    ?: VariantEntity(
                        id = newId(),
                        modelId = model.id,
                        fingerprint = fingerprint,
                        seriesId = null,
                        primaryColorId = colorId,
                        wheelsType = values["rodas"].nullable(),
                        baseType = values["base"].nullable(),
                        decoration = values["decoracao"].nullable(),
                        releaseYear = releaseYear,
                        collectionNumber = values["numero_colecao"].nullable(),
                        seriesNumber = null,
                        edition = null,
                        manufacturerCode = values["codigo_fabricante"].nullable(),
                        packagingType = packagingName(values["condicao_embalagem"]),
                        archivedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ).also {
                        database.variantDao().insert(it)
                        createdVariants++
                    }

                val internalCode = values["codigo_interno"].nullable()
                    ?: "IMP-${UUID.randomUUID().toString().take(8).uppercase()}"
                if (database.exemplarDao().findByInternalCode(internalCode) != null) {
                    skippedDuplicates++
                    continue
                }

                database.exemplarDao().insert(
                    ExemplarEntity(
                        id = newId(),
                        variantId = variant.id,
                        internalCode = internalCode,
                        condition = conditionName(values["condicao"]),
                        packagingCondition = packagingName(values["condicao_embalagem"]),
                        locationId = null,
                        status = "OWNED",
                        notes = values["observacoes"].nullable(),
                        rowVersion = 0,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                imported++
            }
        }

        ImportApplyResult(
            importedExemplars = imported,
            skippedDuplicates = skippedDuplicates,
            invalidRows = invalid,
            createdManufacturers = createdManufacturers,
            createdModels = createdModels,
            createdVariants = createdVariants,
            createdColors = createdColors,
        )
    }

    private fun String?.nullable(): String? = this?.trim()?.takeUnless { it.isEmpty() }

    private fun conditionName(raw: String?): String = when (raw.nullable()?.lowercase()) {
        "lacrado" -> Condition.SEALED.name
        "danificado" -> Condition.DAMAGED.name
        "restaurado" -> Condition.RESTORED.name
        "incompleto" -> Condition.INCOMPLETE.name
        else -> Condition.LOOSE.name
    }

    private fun packagingName(raw: String?): String? = when (raw.nullable()?.lowercase()) {
        "lacrada" -> PackagingCondition.SEALED.name
        "aberta" -> PackagingCondition.OPENED.name
        "danificada" -> PackagingCondition.DAMAGED.name
        "sem embalagem" -> PackagingCondition.NONE.name
        else -> null
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
