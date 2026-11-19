package br.com.hotwheelscollector.di

import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.domain.usecase.CollectionProgressUseCase
import br.com.hotwheelscollector.domain.usecase.CreateCatalogEntryUseCase
import br.com.hotwheelscollector.domain.usecase.CreateModelUseCase
import br.com.hotwheelscollector.domain.usecase.CreateVariantUseCase
import br.com.hotwheelscollector.domain.usecase.MissingItemsUseCase
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
    public fun provideCollectionProgressUseCase(
        collections: CollectionRepository,
    ): CollectionProgressUseCase = CollectionProgressUseCase(collections)

    @Provides
    @Singleton
    public fun provideMissingItemsUseCase(
        collections: CollectionRepository,
    ): MissingItemsUseCase = MissingItemsUseCase(collections)
}
