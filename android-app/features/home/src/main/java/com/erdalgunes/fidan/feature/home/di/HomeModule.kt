package com.erdalgunes.fidan.feature.home.di

import com.erdalgunes.fidan.feature.home.HomePresenterFactory
import com.erdalgunes.fidan.feature.home.HomeUiFactory
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface HomeModule {
    
    @Binds
    @IntoSet
    fun bindHomePresenterFactory(factory: HomePresenterFactory): Presenter.Factory
    
    @Binds
    @IntoSet
    fun bindHomeUiFactory(factory: HomeUiFactory): Ui.Factory
}