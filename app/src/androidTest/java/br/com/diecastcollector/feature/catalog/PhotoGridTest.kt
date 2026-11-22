package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Grade de fotos por ângulo e definição da principal (T046 - US2, FR-020, FR-021).
 *
 * O que se verifica aqui é o que a transação do `PhotoRepository` não pode provar: existe
 * **um cartão por foto**, e tocar na estrela de um cartão avisa exatamente aquele id — se a
 * grade trocasse a referência de cartão, o usuário trocaria a principal da foto errada.
 *
 * A troca em si (desmarcar a anterior e marcar a nova numa transação) é coberta em
 * `PhotoRepositoryTest.setPrimary altera a foto principal`, que roda no módulo de dados.
 */
class PhotoGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val primaryCalls = mutableListOf<String>()
    private val removedCalls = mutableListOf<String>()

    private fun setContent(photos: List<PhotoTile>) {
        composeRule.setContent {
            DiecastTheme {
                PhotoGrid(
                    photos = photos,
                    onAddPhotoBytes = { _, _ -> },
                    onSetPrimary = { primaryCalls += it },
                    onRemovePhoto = { removedCalls += it },
                    onPermissionDenied = {},
                )
            }
        }
    }

    private fun tile(id: String, angle: PhotoAngle, primary: Boolean) = PhotoTile(
        id = id,
        angle = angle,
        isPrimary = primary,
        source = ByteArray(16),
        pending = true,
    )

    private fun twoPhotos() = listOf(
        tile("p1", PhotoAngle.FRONT, primary = true),
        tile("p2", PhotoAngle.BACK, primary = false),
    )

    @Test
    fun gradeMostraUmCartaoPorFotoComOsDoisAngulos() {
        setContent(twoPhotos())

        val stars = composeRule.onAllNodesWithContentDescription("Definir como principal")
            .fetchSemanticsNodes()

        assertEquals(2, stars.size)
    }

    @Test
    fun definirPrincipalNaSegundaFotoAvisaOIdEscolhido() {
        setContent(twoPhotos())

        // [1] é o cartão da traseira, não a principal de hoje.
        composeRule.onAllNodesWithContentDescription("Definir como principal")[1].performClick()

        assertEquals(listOf("p2"), primaryCalls)
    }

    @Test
    fun removerFotoAvisaOIdDoCartaoTocado() {
        setContent(twoPhotos())

        composeRule.onAllNodesWithContentDescription("Remover foto")[0].performClick()

        assertEquals(listOf("p1"), removedCalls)
    }
}
