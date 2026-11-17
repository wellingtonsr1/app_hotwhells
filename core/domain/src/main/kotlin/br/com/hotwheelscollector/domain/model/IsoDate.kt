package br.com.hotwheelscollector.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Dia do calendário do colecionador, sem fuso horário.
 *
 * Persistido como `TEXT` ISO `YYYY-MM-DD` (D12); a interface mostra `dd/MM/yyyy`.
 */
@JvmInline
public value class IsoDate(public val value: LocalDate) : Comparable<IsoDate> {

    public fun toIsoString(): String = value.toString()

    public fun formatPtBr(): String = value.format(PT_BR_FORMAT)

    public fun isAfter(other: IsoDate): Boolean = value.isAfter(other.value)

    override fun compareTo(other: IsoDate): Int = value.compareTo(other.value)

    public companion object {
        /**
         * `uuuu` + `STRICT`: sem isso o formatador "conserta" datas impossíveis —
         * `31/02/2026` virava 28/02 e o erro passava silencioso para o banco.
         */
        private val PT_BR_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)

        /** Aceita apenas ISO `YYYY-MM-DD`; qualquer outra coisa vira `null`. */
        public fun parseIsoOrNull(raw: String): IsoDate? = try {
            IsoDate(LocalDate.parse(raw.trim()))
        } catch (_: DateTimeParseException) {
            null
        }

        /** Interpreta `dd/MM/yyyy`, o formato digitado no Brasil. */
        public fun parsePtBrOrNull(raw: String): IsoDate? = try {
            IsoDate(LocalDate.parse(raw.trim(), PT_BR_FORMAT))
        } catch (_: DateTimeParseException) {
            null
        }

        public fun today(clock: Clock = Clock.systemDefaultZone()): IsoDate = IsoDate(LocalDate.now(clock))
    }
}
