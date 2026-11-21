package br.com.hotwheelscollector.data.recognition

import br.com.hotwheelscollector.domain.recognition.RecognitionField
import br.com.hotwheelscollector.domain.recognition.RecognitionSuggestion
import java.text.Normalizer

/**
 * Extrai sugestões do texto da embalagem (T125, FR-107).
 *
 * É deliberadamente conservador: só propõe o que está escrito de forma reconhecível —
 * rótulo explícito ("Série: …"), linha de fabricante conhecido, ano isolado, código
 * alfanumérico e uma linha de nome. Nada é inferido a partir de contexto, para que a
 * confiança baixa vire "não identificado" em vez de um palpite (FR-105).
 *
 * Separado do ML Kit para poder ser testado em JVM, sem câmera nem modelo nativo.
 */
internal object PackagingTextParser {

    private val YEAR_ONLY = Regex("""^(19|20)\d{2}$""")
    private val SCALE = Regex("""^1\s*[:/]\s*\d+$""")
    private val LABEL = Regex("""^([A-Za-zÀ-ÿ]{2,12})\s*[:\-]\s*(.+)$""")
    private val CODE = Regex("""^[A-Z0-9][A-Z0-9./-]{2,14}$""")

    /** Marcas conhecidas: a linha inteira (ou seu começo) basta para identificar. */
    private val MANUFACTURERS: List<Pair<String, String>> = listOf(
        "hot wheels" to "Hot Wheels",
        "matchbox" to "Matchbox",
        "majorette" to "Majorette",
        "spin master" to "Spin Master",
        "greenlight" to "Greenlight",
        "auto world" to "Auto World",
        "johnny lightning" to "Johnny Lightning",
        "tomica" to "Tomica",
        "siku" to "Siku",
        "bburago" to "Bburago",
        "maisto" to "Maisto",
        "kinsmart" to "Kinsmart",
        "jada" to "Jada",
        "mattel" to "Mattel",
    )

    public fun parse(rawText: String): List<RecognitionSuggestion> {
        val lines = rawText.lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val found = LinkedHashMap<RecognitionField, RecognitionSuggestion>()
        val consumedLines = mutableSetOf<Int>()

        fun offer(suggestion: RecognitionSuggestion) {
            val current = found[suggestion.field]
            if (current == null || suggestion.confidence > current.confidence) {
                found[suggestion.field] = suggestion
            }
        }

        lines.forEachIndexed { index, line ->
            val normalized = normalize(line)

            MANUFACTURERS.firstOrNull { (key, _) ->
                normalized == key || normalized.startsWith("$key ")
            }?.let { (_, label) ->
                offer(suggestion(RecognitionField.MANUFACTURER, label, 0.9))
                consumedLines += index
            }

            LABEL.matchEntire(line)?.let { match ->
                val label = normalize(match.groupValues[1])
                val value = match.groupValues[2].trim()
                if (value.isEmpty()) return@let
                val field = when (label) {
                    "fabricante", "marca", "brand", "manufacturer" -> RecognitionField.MANUFACTURER
                    "modelo", "model", "nome", "name" -> RecognitionField.MODEL
                    "serie", "series", "colecao", "collection" -> RecognitionField.SERIES
                    "codigo", "code", "cod", "codigo de barras", "barcode" -> RecognitionField.CODE
                    "ano", "year" -> RecognitionField.YEAR
                    else -> null
                } ?: return@let

                if (field == RecognitionField.YEAR && !YEAR_ONLY.matches(value)) return@let
                offer(suggestion(field, value, 0.85))
                consumedLines += index
            }

            if (YEAR_ONLY.matches(line)) {
                offer(suggestion(RecognitionField.YEAR, line, 0.75))
                consumedLines += index
            }
        }

        lines.forEachIndexed { index, line ->
            if (index in consumedLines) return@forEachIndexed
            if (isCode(line)) {
                offer(suggestion(RecognitionField.CODE, line, 0.7))
                consumedLines += index
            }
        }

        // Nome: a linha restante mais longa; nada de juntar pedaços de linhas diferentes.
        val modelLine = lines.indices
            .filter { it !in consumedLines }
            .filter { index ->
                val line = lines[index]
                !SCALE.matches(line) &&
                    line.length >= MIN_MODEL_LENGTH &&
                    line.any { it.isLetter() }
            }
            .maxByOrNull { lines[it].length }
        if (modelLine != null) {
            offer(suggestion(RecognitionField.MODEL, lines[modelLine], 0.65))
        }

        return found.values.sortedByDescending { it.confidence }
    }

    private fun suggestion(
        field: RecognitionField,
        value: String,
        confidence: Double,
    ): RecognitionSuggestion = RecognitionSuggestion(field, value.trim(), confidence)

    /** Código de barras/catálogo: alfanumérico, sem espaço, com letra e número. */
    private fun isCode(line: String): Boolean {
        if (line.contains(' ')) return false
        if (SCALE.matches(line) || YEAR_ONLY.matches(line)) return false
        if (!CODE.matches(line)) return false
        return line.any { it.isDigit() } && line.any { it.isLetter() }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    private const val MIN_MODEL_LENGTH = 3
}
