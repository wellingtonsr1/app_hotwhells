package br.com.diecastcollector.data.di

import android.content.Context
import androidx.room.Room
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.repository.AcquisitionRepositoryImpl
import br.com.diecastcollector.data.repository.CarModelRepositoryImpl
import br.com.diecastcollector.data.repository.CollectionRepositoryImpl
import br.com.diecastcollector.data.repository.ColorRepositoryImpl
import br.com.diecastcollector.data.repository.DashboardRepositoryImpl
import br.com.diecastcollector.data.repository.DataControlRepositoryImpl
import br.com.diecastcollector.data.repository.DuplicateRepositoryImpl
import br.com.diecastcollector.data.repository.ExemplarRepositoryImpl
import br.com.diecastcollector.data.repository.FinanceRepositoryImpl
import br.com.diecastcollector.data.repository.HistoryRepositoryImpl
import br.com.diecastcollector.data.repository.ManufacturerRepositoryImpl
import br.com.diecastcollector.data.repository.LocationRepositoryImpl
import br.com.diecastcollector.data.repository.NegotiationRepositoryImpl
import br.com.diecastcollector.data.repository.PhotoRepositoryImpl
import br.com.diecastcollector.data.recognition.MlKitBarcodeProvider
import br.com.diecastcollector.data.recognition.MlKitTextProvider
import br.com.diecastcollector.data.repository.RealVehicleRepositoryImpl
import br.com.diecastcollector.data.repository.RecognitionRepositoryImpl
import br.com.diecastcollector.data.repository.SearchRepositoryImpl
import br.com.diecastcollector.data.repository.SeriesRepositoryImpl
import br.com.diecastcollector.data.repository.ValuationRepositoryImpl
import br.com.diecastcollector.data.repository.VariantRepositoryImpl
import br.com.diecastcollector.data.repository.WishlistRepositoryImpl
import br.com.diecastcollector.data.storage.FileBackupService
import br.com.diecastcollector.data.storage.FileImageStore
import br.com.diecastcollector.data.storage.ImageStore
import br.com.diecastcollector.domain.backup.BackupService
import br.com.diecastcollector.domain.recognition.RecognitionProvider
import br.com.diecastcollector.domain.repository.AcquisitionRepository
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.CollectionRepository
import br.com.diecastcollector.domain.repository.ColorRepository
import br.com.diecastcollector.domain.repository.DashboardRepository
import br.com.diecastcollector.domain.repository.DataControlRepository
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.FinanceRepository
import br.com.diecastcollector.domain.repository.HistoryRepository
import br.com.diecastcollector.domain.repository.ManufacturerRepository
import br.com.diecastcollector.domain.repository.LocationRepository
import br.com.diecastcollector.domain.repository.NegotiationRepository
import br.com.diecastcollector.domain.repository.PhotoRepository
import br.com.diecastcollector.domain.repository.RealVehicleRepository
import br.com.diecastcollector.domain.repository.RecognitionRepository
import br.com.diecastcollector.domain.repository.SearchRepository
import br.com.diecastcollector.domain.repository.SeriesRepository
import br.com.diecastcollector.domain.repository.ValuationRepository
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.repository.WishlistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Banco único do app.
 *
 * `addCallback(AppDatabase.CALLBACK)` não é opcional: é o que liga as FKs e cria o
 * índice único parcial de `variant.fingerprint` a cada abertura.
 */
@Module
@InstallIn(SingletonComponent::class)
public object DatabaseModule {

    @Provides
    @Singleton
    public fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.CALLBACK)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    /** Relógio injetável: os testes trocam por um fixo e o comportamento fica determinístico. */
    @Provides
    @Singleton
    public fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    public fun provideExemplarDao(database: AppDatabase) = database.exemplarDao()

    @Provides
    public fun provideVariantDao(database: AppDatabase) = database.variantDao()

    @Provides
    public fun provideVariantSecondaryColorDao(database: AppDatabase) = database.variantSecondaryColorDao()

    @Provides
    public fun provideLocationDao(database: AppDatabase) = database.locationDao()

    @Provides
    public fun provideMovementDao(database: AppDatabase) = database.movementDao()

    @Provides
    public fun provideAcquisitionDao(database: AppDatabase) = database.acquisitionDao()

    @Provides
    public fun provideValuationDao(database: AppDatabase) = database.valuationDao()

    @Provides
    public fun provideHistoryDao(database: AppDatabase) = database.historyDao()

    @Provides
    public fun provideCarModelDao(database: AppDatabase) = database.carModelDao()

    @Provides
    public fun providePhotoDao(database: AppDatabase) = database.photoDao()

    @Provides
    public fun provideDashboardDao(database: AppDatabase) = database.dashboardDao()

    @Provides
    public fun provideRecognitionResultDao(database: AppDatabase) = database.recognitionResultDao()
}

/** Ligação interface de domínio → implementação de dados. */
@Module
@InstallIn(SingletonComponent::class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    public abstract fun bindImageStore(impl: FileImageStore): ImageStore

    @Binds
    @Singleton
    public abstract fun bindBackupService(impl: FileBackupService): BackupService

    @Binds
    @Singleton
    public abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    public abstract fun bindExemplarRepository(impl: ExemplarRepositoryImpl): ExemplarRepository

    @Binds
    @Singleton
    public abstract fun bindManufacturerRepository(
        impl: ManufacturerRepositoryImpl,
    ): ManufacturerRepository

    @Binds
    @Singleton
    public abstract fun bindCarModelRepository(impl: CarModelRepositoryImpl): CarModelRepository

    @Binds
    @Singleton
    public abstract fun bindRealVehicleRepository(
        impl: RealVehicleRepositoryImpl,
    ): RealVehicleRepository

    @Binds
    @Singleton
    public abstract fun bindVariantRepository(impl: VariantRepositoryImpl): VariantRepository

    @Binds
    @Singleton
    public abstract fun bindColorRepository(impl: ColorRepositoryImpl): ColorRepository

    @Binds
    @Singleton
    public abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds
    @Singleton
    public abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    public abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    public abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    public abstract fun bindDraftRepository(impl: br.com.diecastcollector.data.repository.DraftRepositoryImpl): br.com.diecastcollector.domain.repository.DraftRepository

    @Binds
    @Singleton
    public abstract fun bindAcquisitionRepository(impl: AcquisitionRepositoryImpl): AcquisitionRepository

    @Binds
    @Singleton
    public abstract fun bindValuationRepository(impl: ValuationRepositoryImpl): ValuationRepository

    @Binds
    @Singleton
    public abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository

    @Binds
    @Singleton
    public abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    public abstract fun bindWishlistRepository(impl: WishlistRepositoryImpl): WishlistRepository

    @Binds
    @Singleton
    public abstract fun bindCollectionRepository(
        impl: CollectionRepositoryImpl,
    ): CollectionRepository

    @Binds
    @Singleton
    public abstract fun bindDuplicateRepository(impl: DuplicateRepositoryImpl): DuplicateRepository

    @Binds
    @Singleton
    public abstract fun bindNegotiationRepository(
        impl: NegotiationRepositoryImpl,
    ): NegotiationRepository

    @Binds
    @Singleton
    public abstract fun bindDataControlRepository(
        impl: DataControlRepositoryImpl,
    ): DataControlRepository

    @Binds
    @Singleton
    public abstract fun bindRecognitionRepository(
        impl: RecognitionRepositoryImpl,
    ): RecognitionRepository

    @Binds
    @Singleton
    @BarcodeRecognition
    public abstract fun bindBarcodeRecognition(impl: MlKitBarcodeProvider): RecognitionProvider

    @Binds
    @Singleton
    @TextRecognition
    public abstract fun bindTextRecognition(impl: MlKitTextProvider): RecognitionProvider
}
