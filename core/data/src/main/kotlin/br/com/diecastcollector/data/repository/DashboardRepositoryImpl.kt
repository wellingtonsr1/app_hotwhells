package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.dao.AcquisitionDao
import br.com.diecastcollector.data.db.dao.CollectionProgressRow
import br.com.diecastcollector.data.db.dao.DashboardDao
import br.com.diecastcollector.data.db.dao.DashboardEntryRow
import br.com.diecastcollector.data.db.dao.ValuationDao
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.domain.model.DashboardEntry
import br.com.diecastcollector.domain.model.DashboardSnapshot
import br.com.diecastcollector.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Painel derivado de consultas (FR-001–FR-011).
 *
 * Investido e estimado reaproveitam as somas de `AcquisitionDao` e `ValuationDao`: a regra
 * de "última avaliação por exemplar" existe em um lugar só e o painel não tem cópia dela.
 * As consultas são combinadas porque cada `Flow` do Room serve de gatilho para a mesma
 * leitura; o painel nunca mostra números de momentos diferentes.
 */
@Singleton
public class DashboardRepositoryImpl @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val acquisitionDao: AcquisitionDao,
    private val valuationDao: ValuationDao,
) : DashboardRepository {

    override fun observeSnapshot(): Flow<DashboardSnapshot> {
        val catalog = combine(
            dashboardDao.observeOwnedExemplars(),
            dashboardDao.observeDistinctModels(),
            dashboardDao.observeDistinctVariants(),
            dashboardDao.observeDistinctManufacturers(),
            dashboardDao.observeDistinctSeries(),
        ) { exemplars, models, variants, manufacturers, series ->
            Totals(
                ownedExemplars = exemplars,
                distinctModels = models,
                distinctVariants = variants,
                distinctManufacturers = manufacturers,
                distinctSeries = series,
            )
        }
        val extras = combine(
            dashboardDao.observeActiveCollections(),
            dashboardDao.observeVariantsWithDuplicates(),
            dashboardDao.observeWantedItems(),
            dashboardDao.observeExemplarsWithoutValuation(),
        ) { collections, duplicates, wanted, withoutValuation ->
            Totals(
                activeCollections = collections,
                variantsWithDuplicates = duplicates,
                wantedItems = wanted,
                exemplarsWithoutValuation = withoutValuation,
            )
        }
        val money = combine(
            acquisitionDao.observeTotalInvestedCents(),
            valuationDao.observeCurrentEstimatedCents(),
            dashboardDao.observeLatestValuationDate(),
        ) { invested, estimated, latestValuation ->
            Totals(
                investedCents = invested,
                estimatedCents = estimated,
                latestValuationDate = latestValuation,
            )
        }
        val lists = combine(
            dashboardDao.observeLatestAcquisitions(),
            dashboardDao.observeLatestEntries(),
            dashboardDao.observeCollectionProgress(),
        ) { acquisitions, entries, progress ->
            RecentLists(acquisitions = acquisitions, entries = entries, progress = progress)
        }

        return combine(catalog, extras, money, lists) { a, b, c, listsNow ->
            val totals = Totals(
                ownedExemplars = a.ownedExemplars,
                distinctModels = a.distinctModels,
                distinctVariants = a.distinctVariants,
                distinctManufacturers = a.distinctManufacturers,
                distinctSeries = a.distinctSeries,
                activeCollections = b.activeCollections,
                variantsWithDuplicates = b.variantsWithDuplicates,
                wantedItems = b.wantedItems,
                exemplarsWithoutValuation = b.exemplarsWithoutValuation,
                investedCents = c.investedCents,
                estimatedCents = c.estimatedCents,
                latestValuationDate = c.latestValuationDate,
            )
            totals.toSnapshot(listsNow)
        }
    }

    private fun Totals.toSnapshot(lists: RecentLists): DashboardSnapshot = DashboardSnapshot(
        ownedExemplars = ownedExemplars,
        distinctModels = distinctModels,
        distinctVariants = distinctVariants,
        distinctManufacturers = distinctManufacturers,
        distinctSeries = distinctSeries,
        activeCollections = activeCollections,
        variantsWithDuplicates = variantsWithDuplicates,
        wantedItems = wantedItems,
        investedCents = investedCents,
        estimatedCents = estimatedCents,
        exemplarsWithoutValuation = exemplarsWithoutValuation,
        latestValuationDate = latestValuationDate,
        latestAcquisitions = lists.acquisitions.map { it.toEntry() },
        latestEntries = lists.entries.map { it.toEntry() },
        collectionProgress = lists.progress
            .map { it.toProgress() }
            .sortedWith(compareByDescending<CollectionProgress> { it.percent }.thenBy { it.name }),
    )

    private fun DashboardEntryRow.toEntry(): DashboardEntry = DashboardEntry(
        id = id,
        title = modelName,
        subtitle = manufacturerName,
        date = day,
    )

    private fun CollectionProgressRow.toProgress(): CollectionProgress = CollectionProgress(
        id = id,
        name = name,
        owned = owned,
        known = known,
    )

    private data class Totals(
        val ownedExemplars: Int = 0,
        val distinctModels: Int = 0,
        val distinctVariants: Int = 0,
        val distinctManufacturers: Int = 0,
        val distinctSeries: Int = 0,
        val activeCollections: Int = 0,
        val variantsWithDuplicates: Int = 0,
        val wantedItems: Int = 0,
        val exemplarsWithoutValuation: Int = 0,
        val investedCents: Long = 0L,
        val estimatedCents: Long = 0L,
        val latestValuationDate: String? = null,
    )

    private data class RecentLists(
        val acquisitions: List<DashboardEntryRow>,
        val entries: List<DashboardEntryRow>,
        val progress: List<CollectionProgressRow>,
    )
}
