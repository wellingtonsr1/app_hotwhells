package br.com.diecastcollector.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError

/**
 * Texto de interface com origem em `strings.xml` (T131, Art. IV).
 *
 * O ViewModel não conhece Android nem redige português: carrega o id do recurso e os
 * argumentos, e a tela resolve com [text]. Assim, mensagem de snackbar, erro e confirmação
 * passam pelo mesmo caminho das demais strings — e entram na tradução/lint.
 */
public data class UiMessage(
    @StringRes val resId: Int,
    val args: List<Any> = emptyList(),
) {
    public constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())

    @Composable
    public fun text(): String = stringResource(resId, *args.toTypedArray())
}

/**
 * Tradução de exceção para mensagem de tela.
 *
 * Erros de domínio mantêm a mensagem específica de [DomainError.messageRes]; o motivo
 * livre (`reason`) vira argumento. Qualquer outra exceção cai no texto genérico da
 * operação — texto cru de exceção não é interface e não deve chegar ao snackbar.
 */
public fun Throwable.toUiMessage(
    @StringRes fallback: Int = R.string.error_generic,
): UiMessage = when (this) {
    is DomainError.Validation -> UiMessage(messageRes(), reason)
    is DomainError.Blocked -> UiMessage(messageRes(), reason)
    is DomainError.Storage -> UiMessage(messageRes(), reason)
    is DomainError -> UiMessage(messageRes())
    else -> UiMessage(fallback)
}
