package com.erdalgunes.fidan.di

import com.erdalgunes.fidan.TimerCallback
import com.erdalgunes.fidan.TimerManager
import com.erdalgunes.fidan.forest.ForestManager
import com.erdalgunes.fidan.repository.DefaultTimerRepository
import com.erdalgunes.fidan.repository.ImpactRepository
import com.erdalgunes.fidan.repository.TimerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-wide dependencies
 * Following SOLID principles:
 * - Dependency Inversion Principle: Depend on abstractions (interfaces) not concretions
 * - Single Responsibility Principle: Each provider method has single purpose
 * - Open/Closed Principle: Easy to extend with new providers without modifying existing code
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimerRepository(): TimerRepository {
        return DefaultTimerRepository()
    }

    @Provides
    @Singleton
    fun provideImpactRepository(): ImpactRepository {
        return ImpactRepository()
    }
}

/**
 * Activity-scoped dependencies that need Activity context
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideTimerManager(
        timerCallback: TimerCallback,
        timerRepository: TimerRepository
    ): TimerManager {
        return TimerManager(timerCallback, timerRepository)
    }

    @Provides
    @ActivityScoped
    fun provideForestManager(
        timerCallback: TimerCallback
    ): ForestManager {
        return ForestManager(timerCallback)
    }
}