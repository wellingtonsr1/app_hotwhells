package br.com.hotwheelscollector.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** WorkManager inicializado por `HotWheelsApp`, injetável nos ViewModels (T134). */
@Module
@InstallIn(SingletonComponent::class)
public object WorkModule {

    @Provides
    @Singleton
    public fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
