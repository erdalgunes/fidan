package com.erdalgunes.fidan.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.service.ForestService
import androidx.compose.runtime.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object StatsScreen : Screen

data class StatsState(
    val completedTrees: Int = 0,
    val incompleteTrees: Int = 0,
    val totalSessions: Int = 0,
    val totalFocusTime: String = "0h 0m",
    val completionRate: String = "0%"
) : CircuitUiState

class StatsPresenter @Inject constructor(
    private val forestService: ForestService
) : Presenter<StatsState> {
    
    @Composable
    override fun present(): StatsState {
        val forestState by forestService.forestState.collectAsState()
        
        val completedTrees = forestService.getCompletedTreesCount()
        val incompleteTrees = forestService.getIncompleteTreesCount()
        val totalSessions = completedTrees + incompleteTrees
        val totalFocusMillis = forestService.getTotalFocusTime()
        val totalFocusMinutes = (totalFocusMillis / 1000 / 60).toInt()
        val hours = totalFocusMinutes / 60
        val minutes = totalFocusMinutes % 60
        val totalFocusTime = "${hours}h ${minutes}m"
        val completionRate = "${forestService.getCompletionRate().toInt()}%"
        
        return StatsState(
            completedTrees = completedTrees,
            incompleteTrees = incompleteTrees,
            totalSessions = totalSessions,
            totalFocusTime = totalFocusTime,
            completionRate = completionRate
        )
    }
}

class StatsUi @Inject constructor() : Ui<StatsState> {
    
    @Composable
    override fun Content(state: StatsState, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "${state.completedTrees}",
                    subtitle = "Completed Trees",
                    icon = "Trees",
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "${state.incompleteTrees}",
                    subtitle = "Incomplete Sessions",
                    icon = "Saplings",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = state.totalFocusTime,
                    subtitle = "Total Time",
                    icon = "Time",
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = state.completionRate,
                    subtitle = "Completion Rate",
                    icon = "Rate",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    @Composable
    private fun StatCard(
        modifier: Modifier = Modifier,
        title: String,
        subtitle: String,
        icon: String,
        color: androidx.compose.ui.graphics.Color
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

class StatsPresenterFactory @Inject constructor(
    private val presenter: StatsPresenter
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is StatsScreen -> presenter
            else -> null
        }
    }
}

class StatsUiFactory @Inject constructor(
    private val ui: StatsUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is StatsScreen -> ui
            else -> null
        }
    }
}