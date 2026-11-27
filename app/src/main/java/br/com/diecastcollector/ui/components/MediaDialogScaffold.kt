package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.diecastcollector.R

/**
 * Diálogo de mídia em tela cheia que respeita as barras do sistema (FR-323, FR-325).
 *
 * Os visualizadores abriam com `fillMaxSize` e o botão fechar caía sob a status bar em
 * paisagem. Aqui o conteúdo e o fechar ficam dentro dos insets do diálogo, então nenhum
 * deles fica atrás de recorte, barra de navegação ou gestos. A leitura usa a mesma reserva
 * do editor de fotografia: em alguns aparelhos a janela do diálogo reporta zero embaixo.
 */
@Composable
public fun MediaDialogScaffold(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Sem isto a janela do diálogo não reporta insets: com edge-to-edge
            // obrigatório o atalho "Ver galeria completa" caía sob a barra de gestos.
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.95f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(dialogInsetsPadding()),
            ) {
                content()
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        // Fundo é o scrim (quase preto) nos dois temas: branco sempre lê.
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
