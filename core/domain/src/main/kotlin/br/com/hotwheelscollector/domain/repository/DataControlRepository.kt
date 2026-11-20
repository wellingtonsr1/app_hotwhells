package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * O que existe no aparelho, contado na hora — é o que a confirmação da exclusão mostra
 * antes de apagar (FR-157). Nenhum número é armazenado: sai direto das tabelas.
 */
public data class UserDataCounts(
    val exemplars: Int = 0,
    val models: Int = 0,
    val photos: Int = 0,
    val collections: Int = 0,
    val wishlistItems: Int = 0,
    val locations: Int = 0,
) {
    /** Sem acervo nem registros: não há o que exportar nem o que excluir. */
    val isEmpty: Boolean
        get() = exemplars == 0 &&
            models == 0 &&
            photos == 0 &&
            collections == 0 &&
            wishlistItems == 0 &&
            locations == 0
}

/**
 * Controle dos dados do usuário (T146, FR-142, FR-157, Art. III.7).
 *
 * A exclusão apaga **todos** os registros da coleção e as fotos em disco do aparelho.
 * Ela não é uma operação de domínio reversível: a interface exige dupla confirmação e
 * oferece exportação antes (FR-163). O que ficar fora do app — pacotes exportados pelo
 * próprio usuário — não é tocado.
 */
public interface DataControlRepository {

    /** Contagem viva, para a tela mostrar exatamente o que será apagado. */
    public fun observeCounts(): Flow<UserDataCounts>

    /**
     * Remove todos os registros e as fotos. Nunca apaga em silêncio um arquivo de fora do
     * acervo: só as tabelas do banco local e o diretório `images/` do app.
     */
    public suspend fun deleteAll(): DomainResult<Unit>
}
