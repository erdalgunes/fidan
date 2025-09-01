package com.erdalgunes.fidan.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object HomeScreen : Screen

data class HomeState(
    val message: String = "Welcome to Fidan - Your Focus Companion"
) : CircuitUiState

class HomePresenter @Inject constructor() : Presenter<HomeState> {
    @Composable
    override fun present(): HomeState {
        return HomeState()
    }
}

class HomeUi @Inject constructor() : Ui<HomeState> {
    override fun Content(state: HomeState, modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.message,
                textAlign = TextAlign.Center
            )
        }
    }
}

class HomePresenterFactory @Inject constructor(
    private val presenter: HomePresenter
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is HomeScreen -> presenter
            else -> null
        }
    }
}

class HomeUiFactory @Inject constructor(
    private val ui: HomeUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is HomeScreen -> ui
            else -> null
        }
    }
}