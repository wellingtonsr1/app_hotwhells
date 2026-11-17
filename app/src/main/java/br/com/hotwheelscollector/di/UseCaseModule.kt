package br.com.hotwheelscollector.di

import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.domain.usecase.CreateCatalogEntryUseCase
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
        variants: VariantRepository,
        clock: Clock,
    ): CreateCatalogEntryUseCase = CreateCatalogEntryUseCase(manufacturers, models, variants, clock)
}
