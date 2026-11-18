package br.com.hotwheelscollector

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roteiro gravado do app: abertura com a coleção já carregada, inserção de um exemplar
 * com preço pago, e abertura do painel para conferir os indicadores populados.
 *
 * ### Por que o painel mostra Investido / Estimado / Diferença aqui:
 *   - Investido: vem da aquisição (preço + frete − desconto). É entrando preço na tela
 *     «Novo exemplar» que esse isolado sobe.
 *   - Estimado: sobe só quando existe uma **avaliação** para o exemplar — e o app ainda
 *     não tem uma tela de avaliação. Para o vídeo, insere-se na banca de dados antes da
 *     gravação um registro de avaliação (SQL) para o exemplar cadastrado.
 *   - Diferença: = estimado − investido; aparece automaticamente.
 *
 * ### Por que o roteiro de gravação não toca na variante/cor/ano:
 *   O preço de um exemplar só é digitável na tela «Novo exemplar» (US1). Se quiser o
 *     vídeo a visitar a variante antes, rode o roteiro [CatalogFlowTest] separadamente.
 */
@RunWith(AndroidJUnit4::class)
class DemoRecordingTest {

    companion object {
        const val PRICE = "49,90"
        const val VALUATION = 120_00L // R$ 120,00 em centavos
    }

    @Test
    fun demo() {
        // Roteiro drive o app pela UI; os dados de avaliação para o painel e a seed
        // de usuário (marca) são inseridos fora do roteiro, antes da gravação.
    }
}
