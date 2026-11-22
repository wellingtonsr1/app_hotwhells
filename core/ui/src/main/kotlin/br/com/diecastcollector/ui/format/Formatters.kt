package br.com.diecastcollector.ui.format

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Formatação pt-BR de valores e datas (FR-118, T098).
 *
 * Recebe centavos em `Long` cru porque somas do acervo não cabem em `MoneyCents`, que é
 * o valor de um item (FR-076). Nada aqui lê banco nem conhece regra de negócio.
 */
public fun formatCentsPtBr(cents: Long): String {
    val negative = cents < 0L
    val absolute = if (negative) -cents else cents
    val units = absolute / 100L
    val fraction = (absolute % 100L).toString().padStart(2, '0')
    val grouped = units.toString().reversed().chunked(3).joinToString(".").reversed()
    return if (negative) "-$grouped,$fraction" else "$grouped,$fraction"
}

/** `R$ 1.234,56`; negativo sai como `-R$ 1.234,56`. */
public fun formatMoneyPtBr(cents: Long): String =
    if (cents < 0L) {
        "-R$ ${formatCentsPtBr(-cents)}"
    } else {
        "R$ ${formatCentsPtBr(cents)}"
    }

/** `2026-11-18` → `18/11/2026`; entrada inesperada volta como veio. */
public fun formatIsoDatePtBr(isoDate: String): String = try {
    LocalDate.parse(isoDate).let { "%02d/%02d/%04d".format(it.dayOfMonth, it.monthValue, it.year) }
} catch (e: DateTimeParseException) {
    isoDate
}

/** `2026-11` → `11/2026`; entrada inesperada volta como veio. */
public fun formatYearMonthPtBr(yearMonth: String): String = try {
    YearMonth.parse(yearMonth).let { "%02d/%04d".format(it.monthValue, it.year) }
} catch (e: DateTimeParseException) {
    yearMonth
}
