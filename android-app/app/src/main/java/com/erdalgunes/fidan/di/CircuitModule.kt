package com.erdalgunes.fidan.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.screens.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class CircuitModule {
    
    // Presenter Factories
    @Binds
    @IntoSet
    abstract fun bindTimerPresenterFactory(factory: TimerPresenterFactory): Presenter.Factory
    
    @Binds
    @IntoSet
    abstract fun bindForestPresenterFactory(factory: ForestPresenterFactory): Presenter.Factory
    
    @Binds
    @IntoSet
    abstract fun bindStatsPresenterFactory(factory: StatsPresenterFactory): Presenter.Factory
    
    @Binds
    @IntoSet
    abstract fun bindImpactPresenterFactory(factory: ImpactPresenterFactory): Presenter.Factory
    
    // UI Factories
    @Binds
    @IntoSet
    abstract fun bindTimerUiFactory(factory: TimerUiFactory): Ui.Factory
    
    @Binds
    @IntoSet
    abstract fun bindForestUiFactory(factory: ForestUiFactory): Ui.Factory
    
    @Binds
    @IntoSet
    abstract fun bindStatsUiFactory(factory: StatsUiFactory): Ui.Factory
    
    @Binds
    @IntoSet
    abstract fun bindImpactUiFactory(factory: ImpactUiFactory): Ui.Factory
}