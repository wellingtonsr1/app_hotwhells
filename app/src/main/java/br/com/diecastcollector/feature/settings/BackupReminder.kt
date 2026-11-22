package br.com.diecastcollector.feature.settings

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Lembrete de backup (T120).
 *
 * A coleção inteira mora em um aparelho, e o Android não faz backup das fotos. O lembrete
 * não bloqueia nada: ele aparece em Ajustes quando o intervalo escolhido vence, com atalho
 * para criar o pacote. Sem backup registrado, o lembrete já aparece — é o estado mais
 * arriscado de todos.
 */
public data class BackupReminder(
    val lastBackupAt: Instant?,
    val intervalDays: Int,
) {

    public val enabled: Boolean get() = intervalDays > 0

    /** Vence exatamente no fim do intervalo; nunca feito conta como atrasado. */
    public fun isOverdue(now: Instant): Boolean = enabled && (
        lastBackupAt == null ||
            !now.isBefore(lastBackupAt.plus(intervalDays.toLong(), ChronoUnit.DAYS))
        )

    public fun daysSinceLast(now: Instant): Long? =
        lastBackupAt?.let { ChronoUnit.DAYS.between(it, now) }

    public companion object {
        public const val DEFAULT_INTERVAL_DAYS: Int = 30

        /** Opções da tela, em dias; `0` desliga o lembrete. */
        public val INTERVAL_OPTIONS: List<Int> = listOf(7, 30, 90, 0)
    }
}
