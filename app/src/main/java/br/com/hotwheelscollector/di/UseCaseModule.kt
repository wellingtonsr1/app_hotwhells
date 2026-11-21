package br.com.hotwheelscollector.di

import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import br.com.hotwheelscollector.domain.repository.DuplicateRepository
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.FinanceRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.NegotiationRepository
import br.com.hotwheelscollector.domain.repository.RecognitionRepository
import br.com.hotwheelscollector.domain.repository.SearchRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.domain.usecase.CloseSaleUseCase
import br.com.hotwheelscollector.domain.usecase.CloseTradeUseCase
import br.com.hotwheelscollector.domain.usecase.CollectionProgressUseCase
import br.com.hotwheelscollector.domain.usecase.CreateCatalogEntryUseCase
import br.com.hotwheelscollector.domain.usecase.CreateModelUseCase
import br.com.hotwheelscollector.domain.usecase.CreateVariantUseCase
import br.com.hotwheelscollector.domain.usecase.UpdateVariantUseCase
import br.com.hotwheelscollector.domain.usecase.FinancialHistoryUseCase
import br.com.hotwheelscollector.domain.usecase.FindCatalogMatchesUseCase
import br.com.hotwheelscollector.domain.usecase.GetDuplicatesUseCase
import br.com.hotwheelscollector.domain.usecase.MissingItemsUseCase
import br.com.hotwheelscollector.domain.usecase.RecordRecognitionResultUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Casos de uso são objetos puros de domínio: não conhecem Hilt. A montagem fica aqui,
 * no módulo de aplicação, que é quem sabe como a UI é construída.
 */
@Module
@InstallIn(SingletonComponent::class)
public object UseCaseModule {

    @Provides
    @Singleton
    public fun provideAddExemplarUseCase(
        exemplarRepository: ExemplarRepository,
        clock: Clock,
    ): AddExemplarUseCase = AddExemplarUseCase(exemplarRepository, clock)

    @Provides
    @Singleton
    public fun provideCreateCatalogEntryUseCase(
        manufacturers: ManufacturerRepository,
        models: CarModelRepository,
    ): CreateCatalogEntryUseCase = CreateCatalogEntryUseCase(manufacturers, models)

    @Provides
    @Singleton
    public fun provideCreateModelUseCase(
        manufacturers: ManufacturerRepository,
        models: CarModelRepository,
    ): CreateModelUseCase = CreateModelUseCase(manufacturers, models)

    @Provides
    @Singleton
    public fun provideCreateVariantUseCase(
        models: CarModelRepository,
        variants: VariantRepository,
        clock: Clock,
    ): CreateVariantUseCase = CreateVariantUseCase(models, variants, clock)

    @Provides
    @Singleton
    public fun provideUpdateVariantUseCase(
        variants: VariantRepository,
        clock: Clock,
    ): UpdateVariantUseCase = UpdateVariantUseCase(variants, clock)

    @Provides
    @Singleton
    public fun provideCollectionProgressUseCase(
        collections: CollectionRepository,
    ): CollectionProgressUseCase = CollectionProgressUseCase(collections)

    @Provides
    @Singleton
    public fun provideMissingItemsUseCase(
        collections: CollectionRepository,
    ): MissingItemsUseCase = MissingItemsUseCase(collections)

    @Provides
    @Singleton
    public fun provideGetDuplicatesUseCase(
        duplicates: DuplicateRepository,
    ): GetDuplicatesUseCase = GetDuplicatesUseCase(duplicates)

    @Provides
    @Singleton
    public fun provideCloseSaleUseCase(
        negotiations: NegotiationRepository,
    ): CloseSaleUseCase = CloseSaleUseCase(negotiations)

    @Provides
    @Singleton
    public fun provideCloseTradeUseCase(
        negotiations: NegotiationRepository,
    ): CloseTradeUseCase = CloseTradeUseCase(negotiations)

    @Provides
    @Singleton
    public fun provideFinancialHistoryUseCase(
        financeRepository: FinanceRepository,
    ): FinancialHistoryUseCase = FinancialHistoryUseCase(financeRepository)

    @Provides
    @Singleton
    public fun provideFindCatalogMatchesUseCase(
        searchRepository: SearchRepository,
    ): FindCatalogMatchesUseCase = FindCatalogMatchesUseCase(searchRepository)

    @Provides
    @Singleton
    public fun provideRecordRecognitionResultUseCase(
        recognitionRepository: RecognitionRepository,
    ): RecordRecognitionResultUseCase = RecordRecognitionResultUseCase(recognitionRepository)
}
