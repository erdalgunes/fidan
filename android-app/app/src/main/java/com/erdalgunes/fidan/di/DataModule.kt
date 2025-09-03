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
    
    @Provides
    @Singleton
    fun provideTimerService(): TimerService {
        return TimerService()
    }
    
    @Provides
    @Singleton
    fun provideForestService(): ForestService {
        return ForestService()
    }
}