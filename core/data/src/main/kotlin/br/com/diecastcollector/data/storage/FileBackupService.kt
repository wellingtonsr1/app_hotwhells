package br.com.diecastcollector.data.storage

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.domain.backup.BACKUP_FORMAT_VERSION
import br.com.diecastcollector.domain.backup.BackupException
import br.com.diecastcollector.domain.backup.BackupFailure
import br.com.diecastcollector.domain.backup.BackupFileEntry
import br.com.diecastcollector.domain.backup.BackupService
import br.com.diecastcollector.domain.backup.BackupSummary
import br.com.diecastcollector.domain.backup.RestoreMode
import br.com.diecastcollector.domain.backup.ValidatedBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Backup manual (T119, FR-140–FR-145).
 *
 * O pacote é um ZIP autocontido:
 * - `data/diecast.db` (e, se houver, `-wal`), copiado depois de um checkpoint;
 * - `images/...` com as fotos efetivamente referenciadas por `photo.path`;
 * - `manifest.json` com versões, contagens e SHA-256 de cada arquivo;
 * - `README.txt` explicando como abrir tudo sem o aplicativo.
 *
 * Decisão de formato (v1): o banco vai como arquivo SQLite, não como um JSON por tabela.
 * Isso preserva exatamente o que o usuário tem — inclusive tabelas novas de versões
 * futuras — e torna a restauração uma cópia validada, sem reescrever linhas. A exportação
 * aberta (JSON/CSV) continua sendo a T119/FR-142 e não é substituída por este pacote.
 *
 * A validação acontece inteira no diretório temporário, antes de tocar nos dados atuais:
 * qualquer falha aborta sem gravar nada (FR-143).
 */
@Singleton
public class FileBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val clock: Clock,
) : BackupService {

    private val imagesRoot: File get() = File(context.filesDir, IMAGES_DIR)
    private val foreignKeyCache: MutableMap<String, List<Pair<String, String>>> = HashMap()

    override suspend fun create(
        output: OutputStream,
        appVersion: String,
        deviceName: String?,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val workspace = tempDir("backup-create")
        try {
            val snapshot = snapshotDatabase(workspace)
            val counts = readCounts(snapshot)
            val schemaVersion = SQLiteDatabase.openDatabase(
                snapshot.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { it.version }

            val photos = readPhotoPaths(snapshot)
            val missing = photos.count { !resolvePhoto(it).exists() }
            val totalImages = photos.size - missing

            val entries = mutableListOf<Pair<String, File>>()
            entries += DATABASE_ENTRY to snapshot
            walOf(workspace)?.let { entries += DATABASE_ENTRY_WAL to it }
            photos.map(::resolvePhoto).filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(context.filesDir).invariantSeparatorsPath
                entries += relative to file
            }
            exportTables(snapshot, workspace).forEach { file ->
                entries += file.relativeTo(workspace).invariantSeparatorsPath to file
            }

            val createdAt = clock.instant()
            val readme = readmeText(createdAt).toByteArray(Charsets.UTF_8)
            val totalBytes = entries.sumOf { it.second.length() } + readme.size
            val countsWithImages: Map<String, Int> = counts + (IMAGES_COUNT_KEY to totalImages)

            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                val files = mutableListOf<BackupFileEntry>()
                files += writeText(zip, README_ENTRY, readme)
                entries.forEach { (path, file) -> files += writeFile(zip, path, file) }
                val manifest = JSONObject()
                    .put("format_version", BACKUP_FORMAT_VERSION)
                    .put("schema_version", schemaVersion)
                    .put("app_version", appVersion)
                    .put("created_at", createdAt.toString())
                    .put("created_on_device", deviceName ?: JSONObject.NULL)
                    .put("counts", JSONObject(countsWithImages))
                    .put("missing_images", missing)
                    .put("total_bytes", totalBytes)
                    .put("files", JSONArray().apply {
                        files.forEach { file ->
                            put(
                                JSONObject()
                                    .put("path", file.path)
                                    .put("bytes", file.bytes)
                                    .put("sha256", file.sha256),
                            )
                        }
                    })
                writeText(zip, MANIFEST_ENTRY, manifest.toString(2).toByteArray(Charsets.UTF_8))
            }

            BackupSummary(
                createdAt = createdAt,
                schemaVersion = schemaVersion,
                appVersion = appVersion,
                device = deviceName,
                counts = counts,
                imageCount = totalImages,
                totalBytes = totalBytes,
                missingImages = missing,
            )
        } finally {
            workspace.deleteRecursively()
        }
    }

    override suspend fun inspect(packageFile: File): ValidatedBackup = withContext(Dispatchers.IO) {
        val workspace = tempDir("backup-inspect")
        try {
            val extracted = extract(packageFile, workspace)
            validate(extracted, packageFile)
        } finally {
            workspace.deleteRecursively()
        }
    }

    override suspend fun restore(
        packageFile: File,
        mode: RestoreMode,
    ): BackupSummary = when (mode) {
        RestoreMode.REPLACE -> replace(packageFile)
        RestoreMode.MERGE -> merge(packageFile)
    }

    private suspend fun replace(packageFile: File): BackupSummary = withContext(Dispatchers.IO) {
        val workspace = tempDir("backup-restore")
        try {
            // Revalida: entre a inspeção e o "confirmar" o arquivo pode ter mudado.
            val validated = extract(packageFile, workspace).let { validate(it, packageFile) }
            val extractedDb = File(workspace, DATABASE_ENTRY)

            val liveDb = File(
                database.openHelper.writableDatabase.path
                    ?: throw BackupException(
                        BackupFailure.INVALID_DATABASE,
                        "O banco local não tem caminho em disco.",
                    ),
            )
            checkpoint()

            val databaseDirectory = liveDb.parentFile
                ?: throw BackupException(BackupFailure.WRITE_FAILED, "Diretório do banco ausente.")
            databaseDirectory.mkdirs()

            val stamp = FILE_STAMP.format(clock.instant())
            val dbCopy = File(databaseDirectory, "${liveDb.name}.pre-restore-$stamp")
            val imagesCopy = File(context.filesDir, "$IMAGES_DIR.pre-restore-$stamp")
            val hadImages = imagesRoot.exists()

            database.close()

            try {
                if (liveDb.exists()) liveDb.copyTo(dbCopy, overwrite = true)
                val liveWal = File(liveDb.parentFile, "${liveDb.name}-wal")
                if (liveWal.exists()) liveWal.copyTo(File(dbCopy.parentFile, "${dbCopy.name}-wal"), true)
                if (hadImages) {
                    imagesCopy.deleteRecursively()
                    check(imagesRoot.renameTo(imagesCopy)) {
                        "Não foi possível mover as fotos atuais."
                    }
                }

                deleteQuietly(File(liveDb.parentFile, "${liveDb.name}-wal"))
                deleteQuietly(File(liveDb.parentFile, "${liveDb.name}-shm"))
                extractedDb.copyTo(liveDb, overwrite = true)
                File(workspace, DATABASE_ENTRY_WAL).takeIf { it.isFile }?.copyTo(
                    File(liveDb.parentFile, "${liveDb.name}-wal"),
                    overwrite = true,
                )

                imagesRoot.mkdirs()
                File(workspace, IMAGES_DIR).takeIf { it.isDirectory }?.copyRecursively(
                    target = imagesRoot,
                    overwrite = true,
                )

                validated.summary
            } catch (failure: Throwable) {
                // Rollback: o usuário nunca fica sem banco nem sem fotos.
                runCatching {
                    liveDb.delete()
                    if (dbCopy.exists()) dbCopy.copyTo(liveDb, overwrite = true)
                    imagesRoot.deleteRecursively()
                    if (hadImages && imagesCopy.exists()) {
                        imagesCopy.renameTo(imagesRoot) || imagesCopy.copyRecursively(imagesRoot, true)
                    }
                }
                throw BackupException(
                    BackupFailure.WRITE_FAILED,
                    "Falha ao gravar a restauração; os dados anteriores foram mantidos.",
                    failure,
                )
            }
        } finally {
            workspace.deleteRecursively()
        }
    }

    /**
     * Modo mesclar (FR-144): mantém os dois lados e nunca sobrescreve.
     *
     * Cada tabela entra na ordem das dependências; as chaves estrangeiras são remapeadas
     * para os IDs que já existem no banco atual (entidades de catálogo iguais por chave
     * natural são reaproveitadas). Um exemplar com `codigo_interno` já existente é mantido
     * como está e contado como ignorado — nada é substituído silenciosamente. Tudo roda em
     * uma transação; qualquer falha desfaz o lote inteiro.
     */
    private suspend fun merge(packageFile: File): BackupSummary = withContext(Dispatchers.IO) {
        val workspace = tempDir("backup-merge")
        try {
            val validated = extract(packageFile, workspace).let { validate(it, packageFile) }
            val extractedDb = File(workspace, DATABASE_ENTRY)
            val livePath = database.openHelper.writableDatabase.path
                ?: throw BackupException(
                    BackupFailure.INVALID_DATABASE,
                    "O banco local não tem caminho em disco.",
                )
            checkpoint()

            // Conexão própria e curta: o `SQLiteDatabase` do Room é um wrapper, e ATTACH é
            // por conexão. O pacote é aberto à parte e nada é montado em memória.
            val outcome: MergeOutcome = SQLiteDatabase.openDatabase(
                livePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            ).use { live ->
                live.execSQL("PRAGMA foreign_keys = ON")
                SQLiteDatabase.openDatabase(
                    extractedDb.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                ).use { backupDb ->
                    live.beginTransaction()
                    try {
                        val merged = mergeTables(live, backupDb)
                        copyNewImages(File(workspace, IMAGES_DIR), imagesRoot)
                        recordMerge(live, packageFile.name, merged)
                        live.setTransactionSuccessful()
                        merged
                    } finally {
                        live.endTransaction()
                    }
                }
            }

            validated.summary.copy(
                counts = readCounts(livePath),
                imageCount = countFiles(imagesRoot),
                missingImages = 0,
            )
        } finally {
            workspace.deleteRecursively()
        }
    }

    private data class MergeOutcome(
        val inserted: Int,
        val kept: Int,
        val skipped: Int,
        val skippedExemplars: List<String>,
    )

    private class MergeTally {
        var inserted: Int = 0
        var kept: Int = 0
        var skipped: Int = 0
        val skippedExemplars: MutableList<String> = mutableListOf()
    }

    private enum class RowOutcome { INSERTED, KEPT, SKIPPED, DEFERRED }

    private data class BackupRow(val values: Map<String, Any?>)

    private fun mergeTables(db: SQLiteDatabase, backupDb: SQLiteDatabase): MergeOutcome {
        val tally = MergeTally()
        val mappings = HashMap<String, MutableMap<String, String?>>()

        MERGE_ORDER.forEach { table ->
            val mapping = mappings.getOrPut(table) { HashMap() }
            val keys = PRIMARY_KEYS.getValue(table)
            var pending = readBackupRows(backupDb, table)
            var passes = 0
            while (pending.isNotEmpty()) {
                val deferred = ArrayList<BackupRow>()
                var progressed = false
                pending.forEach { row ->
                    when (mergeRow(db, table, keys, row, mapping, mappings, tally)) {
                        RowOutcome.INSERTED, RowOutcome.KEPT, RowOutcome.SKIPPED -> progressed = true
                        RowOutcome.DEFERRED -> deferred.add(row)
                    }
                }
                if (!progressed || ++passes > pending.size + 2) {
                    // Vínculo que não resolve: fica de fora, contado como ignorado.
                    tally.skipped += deferred.size
                    return@forEach
                }
                pending = deferred
            }
        }

        return MergeOutcome(
            inserted = tally.inserted,
            kept = tally.kept,
            skipped = tally.skipped,
            skippedExemplars = tally.skippedExemplars.distinct(),
        )
    }

    private fun mergeRow(
        db: SQLiteDatabase,
        table: String,
        keys: List<String>,
        row: BackupRow,
        mapping: MutableMap<String, String?>,
        mappings: Map<String, MutableMap<String, String?>>,
        tally: MergeTally,
    ): RowOutcome {
        val values = LinkedHashMap(row.values)
        val oldId = keys.singleOrNull()
            ?.takeIf { it == "id" }
            ?.let { values[it]?.toString() }

        for ((column, parent) in foreignKeys(db, table)) {
            val raw = values[column] ?: continue
            val parentMap = mappings[parent] ?: return RowOutcome.DEFERRED
            val oldParent = raw.toString()
            if (!parentMap.containsKey(oldParent)) return RowOutcome.DEFERRED
            val mapped = parentMap[oldParent] ?: return RowOutcome.SKIPPED
            values[column] = mapped
        }

        NATURAL_KEYS[table]?.let { columns ->
            val existing = findLiveValue(db, table, columns, values)
            if (existing != null) {
                if (oldId != null) mapping[oldId] = existing
                tally.kept++
                return RowOutcome.KEPT
            }
        }

        if (table == "app_setting") {
            val key = values["key"]?.toString() ?: return RowOutcome.SKIPPED
            if (existsWithKey(db, table, "key", key)) {
                tally.kept++
                return RowOutcome.KEPT
            }
        }

        if (table == "exemplar") {
            val code = values["internal_code"]?.toString().orEmpty()
            if (code.isNotEmpty() && findLiveValue(db, table, listOf("internal_code"), values) != null) {
                if (oldId != null) mapping[oldId] = null
                tally.skipped++
                tally.skippedExemplars += code
                return RowOutcome.SKIPPED
            }
        }

        if (oldId != null && existsWithKey(db, table, "id", oldId)) {
            mapping[oldId] = oldId
            tally.kept++
            return RowOutcome.KEPT
        }

        val columns = values.keys.toList()
        val sql = "INSERT INTO \"$table\" (" +
            columns.joinToString(", ") { "\"$it\"" } +
            ") VALUES (" +
            columns.joinToString(", ") { "?" } +
            ")"
        return try {
            db.execSQL(sql, columns.map { values[it] }.toTypedArray())
            if (oldId != null) mapping[oldId] = oldId
            tally.inserted++
            RowOutcome.INSERTED
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Conflito de índice único (ex.: mesmo par em uma tabela de junção).
            if (oldId != null) mapping[oldId] = null
            tally.skipped++
            RowOutcome.SKIPPED
        }
    }

    private fun readBackupRows(db: SQLiteDatabase, table: String): List<BackupRow> {
        val rows = ArrayList<BackupRow>()
        db.rawQuery("SELECT * FROM \"$table\"", null).use { cursor ->
            val columns = cursor.columnNames
            while (cursor.moveToNext()) {
                val values = LinkedHashMap<String, Any?>(columns.size)
                columns.forEachIndexed { index, column ->
                    values[column] = when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                        Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(index)
                        else -> cursor.getString(index)
                    }
                }
                rows += BackupRow(values)
            }
        }
        return rows
    }

    private fun findLiveValue(
        db: SQLiteDatabase,
        table: String,
        columns: List<String>,
        values: Map<String, Any?>,
    ): String? {
        val selection = columns.joinToString(" AND ") { "\"$it\" IS ?" }
        val args = columns.map { values[it]?.toString() }.toTypedArray()
        db.rawQuery("SELECT \"id\" FROM \"$table\" WHERE $selection LIMIT 1", args).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun existsWithKey(
        db: SQLiteDatabase,
        table: String,
        column: String,
        value: String,
    ): Boolean = db.rawQuery(
        "SELECT 1 FROM \"$table\" WHERE \"$column\" = ? LIMIT 1",
        arrayOf(value),
    ).use { it.moveToFirst() }

    /** Chaves estrangeiras da tabela, em cache para não repetir o PRAGMA por linha. */
    private fun foreignKeys(db: SQLiteDatabase, table: String): List<Pair<String, String>> {
        foreignKeyCache[table]?.let { return it }
        val keys = mutableListOf<Pair<String, String>>()
        db.rawQuery("PRAGMA foreign_key_list(\"$table\")", null).use { cursor ->
            while (cursor.moveToNext()) {
                val parent = cursor.getString(cursor.getColumnIndexOrThrow("table"))
                val from = cursor.getString(cursor.getColumnIndexOrThrow("from"))
                keys += from to parent
            }
        }
        foreignKeyCache[table] = keys
        return keys
    }

    private fun copyNewImages(source: File?, targetRoot: File) {
        if (source == null || !source.isDirectory) return
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val destination = File(targetRoot, file.relativeTo(source).path)
            if (!destination.exists()) {
                destination.parentFile?.mkdirs()
                file.copyTo(destination, overwrite = false)
            }
        }
    }

    private fun countFiles(root: File): Int =
        if (root.isDirectory) root.walkTopDown().count { it.isFile } else 0

    private fun recordMerge(db: SQLiteDatabase, packageName: String, outcome: MergeOutcome) {
        val at = clock.millis()
        outcome.skippedExemplars.forEach { code ->
            insertHistory(
                db = db,
                entityType = "exemplar",
                entityId = code,
                oldValue = null,
                newValue = "mantido atual (código já existia)",
                at = at,
            )
        }
        insertHistory(
            db = db,
            entityType = "backup",
            entityId = packageName,
            oldValue = null,
            newValue = "mesclar: inseridos=${outcome.inserted}; mantidos=${outcome.kept}; " +
                "ignorados=${outcome.skipped}",
            at = at,
        )
    }

    private fun insertHistory(
        db: SQLiteDatabase,
        entityType: String,
        entityId: String,
        oldValue: String?,
        newValue: String,
        at: Long,
    ) {
        db.execSQL(
            "INSERT INTO \"history_entry\" (id, entity_type, entity_id, field, old_value, new_value, at, source) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                UUID.randomUUID().toString(),
                entityType,
                entityId,
                "merge",
                oldValue,
                newValue,
                at,
                "restore:merge",
            ),
        )
    }

    /** Checkpoint antes de copiar: sem isso o arquivo principal não tem tudo que foi escrito. */
    private fun checkpoint() {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use {
                it.moveToFirst()
            }
        }
    }

    private fun snapshotDatabase(workspace: File): File {
        val writable = database.openHelper.writableDatabase
        writable.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

        val live = File(writable.path ?: error("Banco sem caminho em disco."))
        val snapshot = File(workspace, "diecast.db")
        live.copyTo(snapshot, overwrite = true)

        // O wal truncado tem 0 byte; se algum escritor ainda segurava páginas, ele vai junto.
        listOf("-wal", "-shm").forEach { suffix ->
            val extra = File(live.parentFile, live.name + suffix)
            if (extra.isFile && extra.length() > 0) {
                extra.copyTo(File(workspace, "diecast.db$suffix"), overwrite = true)
            }
        }
        return snapshot
    }

    private fun walOf(workspace: File): File? = File(workspace, "diecast.db-wal").takeIf { it.isFile }

    private fun readCounts(snapshot: File): Map<String, Int> = readCounts(snapshot.path)

    private fun readCounts(path: String): Map<String, Int> =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            TABLES.associateWith { table ->
                db.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
            }
        }

    /**
     * Exportação aberta (FR-142): o mesmo pacote também leva uma cópia legível sem o app,
     * em `data/<tabela>.json` e `CSV/<tabela>.csv`. É gravada em arquivo temporário e
     * adicionada em streaming, para não montar a coleção inteira em memória.
     *
     * `search_index` fica de fora porque é derivado e reconstruído na abertura.
     */
    private fun exportTables(snapshot: File, workspace: File): List<File> {
        val written = mutableListOf<File>()
        SQLiteDatabase.openDatabase(snapshot.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            EXPORT_TABLES.forEach { table ->
                val columns = db.rawQuery("PRAGMA table_info(\"$table\")", null).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(cursor.getString(1))
                    }
                }
                if (columns.isEmpty()) return@forEach

                val jsonFile = File(workspace, "data/$table.json").apply { parentFile?.mkdirs() }
                val csvFile = File(workspace, "CSV/$table.csv").apply { parentFile?.mkdirs() }

                jsonFile.bufferedWriter(Charsets.UTF_8).use { json ->
                    csvFile.bufferedWriter(Charsets.UTF_8).use { csv ->
                        json.write("[\n")
                        csv.write(columns.joinToString(",") { it.csvCell() })
                        csv.write("\n")
                        var first = true
                        db.rawQuery("SELECT * FROM \"$table\"", null).use { cursor ->
                            while (cursor.moveToNext()) {
                                val row = JSONObject()
                                val cells = ArrayList<String>(columns.size)
                                columns.forEachIndexed { index, column ->
                                    when (cursor.getType(index)) {
                                        Cursor.FIELD_TYPE_NULL -> {
                                            row.put(column, JSONObject.NULL)
                                            cells += ""
                                        }
                                        Cursor.FIELD_TYPE_INTEGER -> {
                                            val value = cursor.getLong(index)
                                            row.put(column, value)
                                            cells += value.toString()
                                        }
                                        Cursor.FIELD_TYPE_FLOAT -> {
                                            val value = cursor.getDouble(index)
                                            row.put(column, value)
                                            cells += value.toString()
                                        }
                                        Cursor.FIELD_TYPE_BLOB -> {
                                            val value = Base64.encodeToString(
                                                cursor.getBlob(index),
                                                Base64.NO_WRAP,
                                            )
                                            row.put(column, value)
                                            cells += value
                                        }
                                        else -> {
                                            val value = cursor.getString(index) ?: ""
                                            row.put(column, value)
                                            cells += value
                                        }
                                    }
                                }
                                if (!first) json.write(",\n")
                                json.write(row.toString())
                                first = false
                                csv.write(cells.joinToString(",") { it.csvCell() })
                                csv.write("\n")
                            }
                        }
                        json.write("\n]\n")
                    }
                }
                written += jsonFile
                written += csvFile
            }
        }
        return written
    }

    /** Campo CSV: aspas só quando precisa, com `"` duplicado. */
    private fun String.csvCell(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    /**
     * Caminhos das imagens que o pacote precisa levar.
     *
     * Desde a edição não destrutiva existem dois arquivos por foto editada: `path` (a
     * derivada exibida) e `original_path` (o original preservado). Backup que levasse só
     * `path` restauraria o app sem a foto original, contrariando FR-214/FR-225.
     */
    private fun readPhotoPaths(snapshot: File): List<String> =
        SQLiteDatabase.openDatabase(snapshot.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT path, original_path FROM photo", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                        val original = if (cursor.isNull(1)) null else cursor.getString(1)
                        if (original != null && original != cursor.getString(0)) add(original)
                    }
                }.distinct()
            }
        }

    /** `photo.path` é relativo a `filesDir` e sempre começa por `images/`. */
    private fun resolvePhoto(relativePath: String): File =
        File(context.filesDir, relativePath.removePrefix("/"))

    private fun extract(packageFile: File, workspace: File): Map<String, File> {
        val files = LinkedHashMap<String, File>()
        val root = workspace.canonicalFile
        try {
            ZipInputStream(BufferedInputStream(packageFile.inputStream())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val target = File(workspace, entry.name).canonicalFile
                    if (!target.path.startsWith(root.path + File.separator)) {
                        throw BackupException(
                            BackupFailure.NOT_A_PACKAGE,
                            "O pacote tem um caminho inválido: ${entry.name}.",
                        )
                    }
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                    files[entry.name] = target
                }
            }
        } catch (failure: BackupException) {
            throw failure
        } catch (failure: Exception) {
            throw BackupException(
                BackupFailure.NOT_A_PACKAGE,
                "Não foi possível abrir o arquivo como pacote de backup.",
                failure,
            )
        }
        return files
    }

    private fun validate(extracted: Map<String, File>, packageFile: File): ValidatedBackup {
        val manifestFile = extracted[MANIFEST_ENTRY]
            ?: throw BackupException(
                BackupFailure.NOT_A_PACKAGE,
                "O arquivo não é um pacote de backup: falta o manifesto.",
            )
        val manifest = try {
            JSONObject(manifestFile.readText())
        } catch (failure: JSONException) {
            throw BackupException(
                BackupFailure.NOT_A_PACKAGE,
                "O manifesto do backup está ilegível.",
                failure,
            )
        }

        if (manifest.optInt("format_version", -1) != BACKUP_FORMAT_VERSION) {
            throw BackupException(
                BackupFailure.UNSUPPORTED_FORMAT,
                "Este backup foi criado por uma versão do app que este app não sabe ler.",
            )
        }

        val schemaVersion = manifest.optInt("schema_version", -1)
        if (schemaVersion < 1) {
            throw BackupException(BackupFailure.INVALID_DATABASE, "O backup não informa a versão do banco.")
        }
        val liveVersion = database.openHelper.readableDatabase.version
        if (schemaVersion > liveVersion) {
            throw BackupException(
                BackupFailure.NEWER_SCHEMA,
                "O backup é de um app mais novo (banco v$schemaVersion; este app usa v$liveVersion).",
            )
        }

        val declared = manifest.optJSONArray("files")
            ?: throw BackupException(BackupFailure.NOT_A_PACKAGE, "O manifesto não lista os arquivos.")
        val files = buildList {
            for (index in 0 until declared.length()) {
                val item = declared.optJSONObject(index)
                    ?: throw BackupException(BackupFailure.NOT_A_PACKAGE, "Arquivo inválido no manifesto.")
                val path = item.optString("path")
                val expectedBytes = item.optLong("bytes", -1L)
                val expectedSha = item.optString("sha256")
                val file = extracted[path]
                    ?: throw BackupException(
                        BackupFailure.MISSING_FILE,
                        "Falta o arquivo $path no backup.",
                    )
                if (expectedBytes < 0 || file.length() != expectedBytes) {
                    throw BackupException(
                        BackupFailure.SIZE_MISMATCH,
                        "O arquivo $path não tem o tamanho registrado no manifesto.",
                    )
                }
                val actual = sha256(file)
                if (!actual.equals(expectedSha, ignoreCase = true)) {
                    throw BackupException(
                        BackupFailure.CHECKSUM_MISMATCH,
                        "O arquivo $path está corrompido (hash diferente do manifesto).",
                    )
                }
                add(BackupFileEntry(path, expectedBytes, actual))
            }
        }

        val dbFile = extracted[DATABASE_ENTRY]
            ?: throw BackupException(BackupFailure.INVALID_DATABASE, "O backup não contém o banco de dados.")

        val counts = manifest.optJSONObject("counts")
            ?: throw BackupException(BackupFailure.NOT_A_PACKAGE, "O manifesto não traz as contagens.")
        val declaredCounts = counts.keys().asSequence().associateWith { counts.optInt(it, -1) }

        // Aberto para escrita, embora só leiamos: o `integrity_check` do FTS4 valida o
        // índice invertido do `search_index` e o SQLite recusa isso em banco readonly
        // («attempt to write a readonly database»). É uma cópia temporária, então escrever
        // aqui não toca nos dados do usuário.
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            if (db.version != schemaVersion) {
                throw BackupException(
                    BackupFailure.INVALID_DATABASE,
                    "A versão do banco no pacote não confere com o manifesto.",
                )
            }
            val integrity = db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "sem resposta"
            }
            if (integrity != "ok") {
                throw BackupException(
                    BackupFailure.INVALID_DATABASE,
                    "O banco do backup está corrompido ($integrity).",
                )
            }
            val brokenFks = db.rawQuery("PRAGMA foreign_key_check", null).use { it.count }
            if (brokenFks > 0) {
                throw BackupException(
                    BackupFailure.FOREIGN_KEY_BROKEN,
                    "O banco do backup tem $brokenFks vínculo(s) quebrado(s).",
                )
            }
            TABLES.forEach { table ->
                val expected = declaredCounts[table]
                    ?: throw BackupException(
                        BackupFailure.COUNT_MISMATCH,
                        "O manifesto não informa a contagem de $table.",
                    )
                val actual = db.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                if (actual != expected) {
                    throw BackupException(
                        BackupFailure.COUNT_MISMATCH,
                        "A contagem de $table no backup ($actual) não confere com o manifesto ($expected).",
                    )
                }
            }
        }

        val imageEntries = extracted.keys.count { it.startsWith("$IMAGES_DIR/") }
        val declaredImages = declaredCounts[IMAGES_COUNT_KEY]
        if (declaredImages != null && declaredImages != imageEntries) {
            throw BackupException(
                BackupFailure.COUNT_MISMATCH,
                "O backup declara $declaredImages foto(s), mas contém $imageEntries.",
            )
        }

        val createdAt = try {
            Instant.parse(manifest.optString("created_at"))
        } catch (failure: DateTimeParseException) {
            throw BackupException(
                BackupFailure.NOT_A_PACKAGE,
                "O manifesto não traz a data de criação.",
                failure,
            )
        }

        return ValidatedBackup(
            file = packageFile,
            summary = BackupSummary(
                createdAt = createdAt,
                schemaVersion = schemaVersion,
                appVersion = manifest.optString("app_version"),
                device = if (manifest.isNull("created_on_device")) {
                    null
                } else {
                    manifest.optString("created_on_device").takeIf { it.isNotBlank() }
                },
                counts = declaredCounts.filterKeys { it != IMAGES_COUNT_KEY },
                imageCount = imageEntries,
                totalBytes = manifest.optLong("total_bytes", 0L),
                missingImages = manifest.optInt("missing_images", 0),
            ),
            files = files,
        )
    }

    private fun writeText(zip: ZipOutputStream, path: String, bytes: ByteArray): BackupFileEntry {
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
        return BackupFileEntry(path, bytes.size.toLong(), sha256(bytes))
    }

    private fun writeFile(zip: ZipOutputStream, path: String, file: File): BackupFileEntry {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        zip.putNextEntry(ZipEntry(path))
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                zip.write(buffer, 0, read)
                bytes += read
            }
        }
        zip.closeEntry()
        return BackupFileEntry(path, bytes, digest.digest().toHex())
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun tempDir(prefix: String): File =
        File(context.cacheDir, "$prefix-${System.nanoTime()}").apply { mkdirs() }

    private fun deleteQuietly(file: File) {
        if (file.exists()) file.delete()
    }

    private fun readmeText(createdAt: Instant): String = """
        Diecast Collector — pacote de backup
        Gerado em $createdAt.

        O que há aqui:
        - data/diecast.db: o banco SQLite com toda a coleção (abre em DB Browser for SQLite).
        - data/<tabela>.json e CSV/<tabela>.csv: a mesma informação em formato aberto, uma
          linha por registro, para abrir em planilha ou em qualquer editor de texto.
        - images/: as fotos, na mesma organização de pastas usada pelo aplicativo.
        - manifest.json: versões, contagens e o SHA-256 de cada arquivo, para conferir
          que nada foi alterado.

        Para restaurar no aplicativo:
        1. Abra Ajustes → Backup e restauração.
        2. Escolha "Restaurar backup" e selecione este arquivo.
        3. Confira o resumo e confirme; o app será reaberto com os dados do pacote.

        Este pacote não tem senha e não sai do seu aparelho: guarde-o onde quiser.
    """.trimIndent()

    private companion object {
        const val IMAGES_DIR = "images"
        const val IMAGES_COUNT_KEY = "images"
        const val MANIFEST_ENTRY = "manifest.json"
        const val README_ENTRY = "README.txt"
        const val DATABASE_ENTRY = "data/diecast.db"
        const val DATABASE_ENTRY_WAL = "data/diecast.db-wal"
        const val BUFFER_SIZE = 64 * 1024
        val FILE_STAMP: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(java.time.ZoneOffset.UTC)

        /** Tabelas do modelo presentes no schema atual (schemaVersion 2). */
        val TABLES: List<String> = listOf(
            "manufacturer", "brand", "real_vehicle", "color", "car_model", "series",
            "variant", "variant_secondary_color", "exemplar", "photo", "location",
            "movement", "acquisition", "valuation", "collection", "collection_item",
            "wishlist_item", "negotiation", "negotiation_event", "recognition_result",
            "history_entry", "app_setting", "search_index", "duplicate_offer",
        )

        /** Tabelas exportadas em JSON/CSV; `search_index` é derivado e fica de fora. */
        val EXPORT_TABLES: List<String> = TABLES.filter { it != "search_index" }

        /** Ordem topológica usada no modo mesclar: pais antes dos filhos. */
        val MERGE_ORDER: List<String> = listOf(
            "manufacturer", "brand", "real_vehicle", "color", "car_model", "series",
            "location", "variant", "variant_secondary_color", "exemplar", "photo",
            "acquisition", "valuation", "movement", "collection", "collection_item",
            "wishlist_item", "negotiation", "negotiation_event", "recognition_result",
            "history_entry", "app_setting", "duplicate_offer",
        )

        /** Colunas da chave primária de cada tabela (só as que importam ao mesclar). */
        val PRIMARY_KEYS: Map<String, List<String>> = mapOf(
            "variant_secondary_color" to listOf("variant_id", "color_id"),
            "app_setting" to listOf("key"),
        ).withDefault { listOf("id") }

        /**
         * Chave natural das entidades de catálogo: se já existir no banco atual, a linha do
         * backup é mapeada para a existente em vez de duplicada.
         */
        val NATURAL_KEYS: Map<String, List<String>> = mapOf(
            "manufacturer" to listOf("name"),
            "brand" to listOf("manufacturer_id", "name"),
            "color" to listOf("name"),
            "car_model" to listOf("manufacturer_id", "name", "scale"),
        )
    }
}
