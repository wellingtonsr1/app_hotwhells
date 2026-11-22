package br.com.diecastcollector.feature.locations

import android.content.res.Resources
import br.com.diecastcollector.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Texto real das mensagens de exclusão de localização.
 *
 * O `LocationsViewModelTest` prova qual número chega à mensagem; aqui o `strings.xml` é
 * resolvido de verdade, para que um plural errado (ou um `%1$d` esquecido) não passe.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // o Robolectric instalado ainda não sobe com targetSdk 36
public class LocationsMessageTextTest {

    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    @Test
    public fun `movimentacao concorda no singular`() {
        assertThat(
            resources.getQuantityString(R.plurals.locations_archived_moved, 1, 1),
        ).isEqualTo("Localização excluída. 1 exemplar movido.")
    }

    @Test
    public fun `movimentacao concorda no plural`() {
        assertThat(
            resources.getQuantityString(R.plurals.locations_archived_moved, 3, 3),
        ).isEqualTo("Localização excluída. 3 exemplares movidos.")
    }

    @Test
    public fun `arquivamento concorda no singular e no plural`() {
        assertThat(
            resources.getQuantityString(R.plurals.locations_archived_archived, 1, 1),
        ).isEqualTo("Localização excluída. 1 exemplar arquivado.")
        assertThat(
            resources.getQuantityString(R.plurals.locations_archived_archived, 3, 3),
        ).isEqualTo("Localização excluída. 3 exemplares arquivados.")
    }

    @Test
    public fun `localizacao vazia nao fala de exemplares`() {
        assertThat(resources.getString(R.string.locations_archived_empty))
            .isEqualTo("Localização excluída.")
    }
}
