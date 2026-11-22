package br.com.diecastcollector.feature.settings

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Test

/**
 * Lembrete de backup (T120): o vencimento é exatamente o fim do intervalo, nunca feito
 * conta como atrasado, e desligar o lembrete silencia qualquer aviso.
 */
public class BackupReminderTest {

    private val now: Instant = Instant.parse("2026-11-19T12:00:00Z")

    @Test
    public fun semBackupOIntervaloAtivoJaEstaAtrasado() {
        val reminder = BackupReminder(lastBackupAt = null, intervalDays = 30)

        assertThat(reminder.isOverdue(now)).isTrue()
        assertThat(reminder.daysSinceLast(now)).isNull()
    }

    @Test
    public fun venceNoFimDoIntervaloENaoAntes() {
        val last = now.minus(29, ChronoUnit.DAYS)
        val reminder = BackupReminder(last, intervalDays = 30)

        assertThat(reminder.isOverdue(now)).isFalse()

        val due = BackupReminder(now.minus(30, ChronoUnit.DAYS), intervalDays = 30)
        assertThat(due.isOverdue(now)).isTrue()
        assertThat(due.daysSinceLast(now)).isEqualTo(30)
    }

    @Test
    public fun desligadoNuncaAvisa() {
        val reminder = BackupReminder(lastBackupAt = null, intervalDays = 0)

        assertThat(reminder.enabled).isFalse()
        assertThat(reminder.isOverdue(now)).isFalse()
    }

    @Test
    public fun backupRecenteNaoAvisa() {
        val reminder = BackupReminder(now.minus(3, ChronoUnit.DAYS), intervalDays = 7)

        assertThat(reminder.isOverdue(now)).isFalse()
        assertThat(reminder.daysSinceLast(now)).isEqualTo(3)
    }
}
