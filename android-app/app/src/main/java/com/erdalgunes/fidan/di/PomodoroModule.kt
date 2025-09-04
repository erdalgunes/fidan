package com.erdalgunes.fidan.di

import com.erdalgunes.fidan.domain.SessionTimer
import com.erdalgunes.fidan.service.TimerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PomodoroModule {
    
    @Binds
    @Singleton
    abstract fun bindSessionTimer(
        timerService: TimerService
    ): SessionTimer
}