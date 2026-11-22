package br.com.diecastcollector.domain.repository

import br.com.diecastcollector.domain.model.DashboardSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Leitura agregada do acervo (FR-001–FR-011).
 *
 * Só existe `observe`: o painel é derivado do que está gravado, não tem estado próprio.
 * Cada emissão reflete uma mudança em qualquer tabela que participe dos números.
 */
public interface DashboardRepository {

    public fun observeSnapshot(): Flow<DashboardSnapshot>
}
