package br.com.diecastcollector.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Agregações do painel (T050, FR-001–FR-011) no banco de verdade.
 *
 * O seed é SQL cru de propósito: o teste existe para verificar as consultas, e um seed feito
 * pelos `insert` dos DAOs testaria a outra ponta. As linhas cobrem as decisões que dão
 * errado com facilidade:
 *
 * - exemplar vendido (`SOLD`) continua no banco mas não entra em indicador nem em valor;
 * - só a **última** avaliação de cada exemplar conta na estimativa;
 * - variante repetida é contada uma vez, não uma vez por exemplar;
 * - item de coleção marcado como não colecionável não entra no total;
 * - coleção arquivada não aparece no progresso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class DashboardQueryTest {

    private lateinit var database: AppDatabase

    @Before
    public fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .build()
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun snapshotDerivaOsIndicadoresDoQueEstaGravado(): Unit = runBlocking {
        seed()

        val snapshot = dashboardRepository().observeSnapshot().first()

        assertThat(snapshot.ownedExemplars).isEqualTo(3)
        assertThat(snapshot.distinctModels).isEqualTo(1)
        assertThat(snapshot.distinctVariants).isEqualTo(2)
        assertThat(snapshot.distinctManufacturers).isEqualTo(1)
        assertThat(snapshot.distinctSeries).isEqualTo(1)
        assertThat(snapshot.activeCollections).isEqualTo(1)
        assertThat(snapshot.variantsWithDuplicates).isEqualTo(1)
        assertThat(snapshot.wantedItems).isEqualTo(1)

        // Preço + frete − desconto do exemplar possuído; a aquisição do vendido fica fora.
        assertThat(snapshot.investedCents).isEqualTo(100_500L)
        // 80.000 (última avaliação do e1) + 45.000 (e3); a primeira avaliação do e1 é passado.
        assertThat(snapshot.estimatedCents).isEqualTo(125_000L)
        assertThat(snapshot.estimatedDifferenceCents).isEqualTo(24_500L)
        // A data mais recente entre as avaliações consideradas, com a do vendido ignorada.
        assertThat(snapshot.latestValuationDate).isEqualTo("2026-06-01")

        assertThat(snapshot.exemplarsWithoutValuation).isEqualTo(1)
    }

    @Test
    public fun listasRecentesTrazemOModeloEAIgnoramOExemplarVendido(): Unit = runBlocking {
        seed()

        val snapshot = dashboardRepository().observeSnapshot().first()

        assertThat(snapshot.latestAcquisitions).hasSize(1)
        assertThat(snapshot.latestAcquisitions.first().title).isEqualTo("Twin Mill")
        assertThat(snapshot.latestAcquisitions.first().subtitle).isEqualTo("Mattel")
        assertThat(snapshot.latestAcquisitions.first().date).isEqualTo("2026-05-02")

        // Cadastro mais recente primeiro: e3 foi criado depois de e1/e2.
        assertThat(snapshot.latestEntries).hasSize(3)
        assertThat(snapshot.latestEntries.first().id).isEqualTo("e3")
    }

    @Test
    public fun progressoDaColecaoContaItensPossuidosESoAsAtivas(): Unit = runBlocking {
        seed()

        val progress = dashboardRepository().observeSnapshot().first().collectionProgress

        assertThat(progress).hasSize(1)
        val collection = progress.first()
        assertThat(collection.name).isEqualTo("Hot Wheels 2024")
        assertThat(collection.known).isEqualTo(2)
        assertThat(collection.owned).isEqualTo(2)
        assertThat(collection.percent).isEqualTo(100)
        assertThat(collection.isComplete).isTrue()
    }

    @Test
    public fun bancoVazioViraSnapshotVazio(): Unit = runBlocking {
        val snapshot = dashboardRepository().observeSnapshot().first()

        assertThat(snapshot.isEmpty).isTrue()
        assertThat(snapshot.ownedExemplars).isEqualTo(0)
        assertThat(snapshot.investedCents).isEqualTo(0L)
        assertThat(snapshot.latestAcquisitions).isEmpty()
    }

    private fun dashboardRepository(): DashboardRepositoryImpl = DashboardRepositoryImpl(
        dashboardDao = database.dashboardDao(),
        acquisitionDao = database.acquisitionDao(),
        valuationDao = database.valuationDao(),
    )

    /**
     * Um modelo, duas variantes e quatro exemplares: e1 e e2 repetem a v1 (e2 sem
     * avaliação), e3 é a v2 avaliada, e4 é a v2 vendida — com valores altos justamente
     * para o teste falhar se ela vazar para algum total.
     */
    private fun seed() {
        val db = database.openHelper.writableDatabase
        db.execSQL("INSERT INTO manufacturer (id, name, created_at, updated_at) VALUES ('m1','Mattel',1,1)")
        db.execSQL(
            "INSERT INTO series (id, manufacturer_id, name, created_at, updated_at) " +
                "VALUES ('s1','m1','Hot Wheels 2024',1,1)",
        )
        db.execSQL(
            "INSERT INTO car_model (id, manufacturer_id, name, created_at, updated_at) " +
                "VALUES ('cm1','m1','Twin Mill',1,1)",
        )
        db.execSQL(
            "INSERT INTO variant (id, model_id, fingerprint, series_id, created_at, updated_at) " +
                "VALUES ('v1','cm1','fp1','s1',1,1)",
        )
        db.execSQL(
            "INSERT INTO variant (id, model_id, fingerprint, series_id, created_at, updated_at) " +
                "VALUES ('v2','cm1','fp2','s1',1,1)",
        )
        insertExemplar(id = "e1", variantId = "v1", status = "OWNED", createdAt = 1_000)
        insertExemplar(id = "e2", variantId = "v1", status = "OWNED", createdAt = 2_000)
        insertExemplar(id = "e3", variantId = "v2", status = "OWNED", createdAt = 3_000)
        insertExemplar(id = "e4", variantId = "v2", status = "SOLD", createdAt = 4_000)

        db.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, shipping_cents, " +
                "discount_cents, created_at, updated_at) " +
                "VALUES ('a1','e1','2026-05-02',100000,1000,500,1,1)",
        )
        db.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, created_at, updated_at) " +
                "VALUES ('a2','e4','2026-06-01',999999,2,2)",
        )

        db.execSQL(
            "INSERT INTO valuation (id, exemplar_id, value_cents, date, created_at) " +
                "VALUES ('val1','e1',60000,'2026-01-01',1)",
        )
        db.execSQL(
            "INSERT INTO valuation (id, exemplar_id, value_cents, date, created_at) " +
                "VALUES ('val2','e1',80000,'2026-06-01',2)",
        )
        db.execSQL(
            "INSERT INTO valuation (id, exemplar_id, value_cents, date, created_at) " +
                "VALUES ('val3','e3',45000,'2026-06-01',3)",
        )

        db.execSQL(
            "INSERT INTO wishlist_item (id, model_id, status, created_at, updated_at) " +
                "VALUES ('w1','cm1','WANTED',1,1)",
        )
        db.execSQL(
            "INSERT INTO wishlist_item (id, model_id, status, created_at, updated_at) " +
                "VALUES ('w2','cm1','ACQUIRED',1,1)",
        )

        db.execSQL(
            "INSERT INTO collection (id, name, created_at, updated_at) " +
                "VALUES ('c1','Hot Wheels 2024',1,1)",
        )
        db.execSQL(
            "INSERT INTO collection (id, name, archived_at, created_at, updated_at) " +
                "VALUES ('c2','Coleção arquivada',9,1,1)",
        )
        db.execSQL("INSERT INTO collection_item (id, collection_id, variant_id) VALUES ('i1','c1','v1')")
        db.execSQL("INSERT INTO collection_item (id, collection_id, model_id) VALUES ('i2','c1','cm1')")
        db.execSQL(
            "INSERT INTO collection_item (id, collection_id, variant_id, non_collectible) " +
                "VALUES ('i3','c1','v2',1)",
        )
    }

    private fun insertExemplar(id: String, variantId: String, status: String, createdAt: Long) {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO exemplar (id, variant_id, internal_code, condition, status, created_at, updated_at) " +
                "VALUES ('$id','$variantId','HW-$id','LOOSE','$status',$createdAt,$createdAt)",
        )
    }
}
