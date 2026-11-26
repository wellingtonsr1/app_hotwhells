package br.com.diecastcollector.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dono único do arquivo de preferências (ADR-006).
 *
 * O `delegate` é privado deste arquivo de propósito: abrir dois `DataStore` sobre o mesmo
 * arquivo lançaria `IllegalStateException` em runtime.
 */
private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
)

@Module
@InstallIn(SingletonComponent::class)
public object PreferencesModule {

    @Provides
    @Singleton
    public fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.appearanceDataStore
}
