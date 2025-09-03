package com.erdalgunes.fidan.di

import com.erdalgunes.fidan.data.ImpactRepository
import com.erdalgunes.fidan.service.TimerService
import com.erdalgunes.fidan.service.ForestService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideImpactRepository(): ImpactRepository {
        return ImpactRepository()
    }
    
    // TimerService and ForestService have @Singleton and @Inject constructor
    // so they don't need @Provides methods - they can inject themselves
}