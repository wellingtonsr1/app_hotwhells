package br.com.diecastcollector.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Escopo que vive enquanto o app vive.
 *
 * Existe para o trabalho que precisa terminar depois que a tela e o ViewModel somem: no
 * `onCleared` o `viewModelScope` já foi cancelado, então uma foto de rascunho apagada por
 * ele nunca sairia do disco.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
public object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    public fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
