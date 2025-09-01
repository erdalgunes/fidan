package com.erdalgunes.fidan.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CircuitModule {
    
    @Provides
    @Singleton
    fun provideCircuit(
        presenters: Set<@JvmSuppressWildcards Presenter.Factory>,
        uis: Set<@JvmSuppressWildcards Ui.Factory>
    ): Circuit {
        return Circuit.Builder()
            .addPresenterFactories(presenters)
            .addUiFactories(uis)
            .build()
    }
}