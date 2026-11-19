package br.com.hotwheelscollector.feature.wishlist

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Abre o link de referência em um app externo (T079).
 *
 * Só `http` e `https` saem do app: um link `file:` ou um esquema próprio poderia abrir
 * algo que não é a loja anunciada. Falha ao abrir (nenhum navegador instalado) é silenciosa
 * aqui; a tela continua utilizável e o link permanece visível.
 */
internal fun openReferenceLink(context: Context, url: String) {
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return
    val intent = Intent(Intent.ACTION_VIEW, trimmed.toUri())
    runCatching { context.startActivity(intent) }
}
