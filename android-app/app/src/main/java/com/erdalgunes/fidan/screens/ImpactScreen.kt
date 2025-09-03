package com.erdalgunes.fidan.screens

import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.config.AppConfig
import com.erdalgunes.fidan.data.ImpactData
import com.erdalgunes.fidan.data.ImpactRepository
import com.erdalgunes.fidan.data.Result
import com.erdalgunes.fidan.ui.viewmodel.ErrorType
import com.erdalgunes.fidan.utils.UrlUtils
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object ImpactScreen : Screen

sealed class ImpactState : CircuitUiState {
    object Loading : ImpactState()
    data class Success(val impactData: ImpactData) : ImpactState()
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.GENERIC,
        val onRetry: () -> Unit
    ) : ImpactState()
}

class ImpactPresenter @Inject constructor(
    private val repository: ImpactRepository
) : Presenter<ImpactState> {
    
    @Composable
    override fun present(): ImpactState {
        var state by rememberRetained { mutableStateOf<ImpactState>(ImpactState.Loading) }
        val scope = rememberCoroutineScope()
        
        val loadData = remember {
            {
                scope.launch {
                    state = ImpactState.Loading
                    when (val result = repository.getImpactData()) {
                        is Result.Success -> {
                            state = ImpactState.Success(result.data)
                        }
                        is Result.Error -> {
                            val errorType = when {
                                result.message.contains("timeout", ignoreCase = true) -> ErrorType.TIMEOUT
                                result.message.contains("network", ignoreCase = true) -> ErrorType.NETWORK
                                else -> ErrorType.GENERIC
                            }
                            state = ImpactState.Error(result.message, errorType) { loadData() }
                        }
                        is Result.Loading -> {
                            state = ImpactState.Loading
                        }
                    }
                }
            }
        }
        
        LaunchedEffect(Unit) {
            loadData()
        }
        
        return state
    }
}

class ImpactUi @Inject constructor() : Ui<ImpactState> {
    
    @Composable
    override fun Content(state: ImpactState, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Real Environmental Impact",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            when (state) {
                is ImpactState.Loading -> {
                    ImpactLoadingContent()
                }
                is ImpactState.Error -> {
                    ImpactErrorContent(
                        errorMessage = state.message,
                        errorType = state.errorType,
                        onRetry = state.onRetry
                    )
                }
                is ImpactState.Success -> {
                    ImpactSuccessContent(state.impactData)
                }
            }
        }
    }
    
    @Composable
    private fun ImpactLoadingContent() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading impact data...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    @Composable
    private fun ImpactErrorContent(
        errorMessage: String,
        errorType: ErrorType,
        onRetry: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val errorIcon = when (errorType) {
                ErrorType.NETWORK -> "📶"
                ErrorType.TIMEOUT -> "⏰"
                ErrorType.GENERIC -> "⚠️"
            }
            Text(
                text = errorIcon,
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Unable to load impact data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Retry")
            }
        }
    }
    
    @Composable
    private fun ImpactSuccessContent(impactData: ImpactData) {
        val context = LocalContext.current
        val githubSponsorsUrl = AppConfig.GITHUB_SPONSORS_URL
        val transparencyReportUrl = AppConfig.TRANSPARENCY_REPORT_URL
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Real Trees Counter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌳",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AnimatedTreeCount(
                        targetCount = impactData.realTreesPlanted,
                        modifier = Modifier.semantics { 
                            contentDescription = "Real trees planted counter showing ${impactData.realTreesPlanted} trees" 
                        }
                    )
                    Text(
                        text = "Real Trees Planted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Through GitHub Sponsors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Sponsorship Information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "💚 Support Fidan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every GitHub sponsorship plants real trees! 75% of proceeds go directly to verified tree-planting organizations.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "⚡ 75% trees + 25% maintenance = Sustainable impact",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Sponsorship Tiers
                    SponsorshipTier("🌱 Seedling", "$3/month", "1 tree planted")
                    SponsorshipTier("🌿 Sapling", "$10/month", "5 trees planted")  
                    SponsorshipTier("🌳 Forest Guardian", "$25/month", "15 trees planted")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            UrlUtils.openUrl(context, githubSponsorsUrl)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Become a sponsor button - opens GitHub sponsors page" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Become a Sponsor")
                    }
                }
            }
            
            // Transparency Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📊 Transparency",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TransparencyItem("Total Donations", "$%.2f".format(impactData.totalDonations))
                    TransparencyItem("Tree Planting Fund", "75% of proceeds")
                    TransparencyItem("Maintenance Fund", "25% for development")
                    TransparencyItem("Partner Organizations", "${impactData.partnersCount} active")
                    TransparencyItem("Monthly Growth", "+${impactData.monthlyGrowth}%")
                    TransparencyItem("Planting Certificates", "${impactData.certificates} verified")
                    TransparencyItem("Last Update", impactData.lastUpdated)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            UrlUtils.openUrl(context, transparencyReportUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Monthly Report")
                    }
                }
            }
            
            // Partner Organizations
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🤝 Our Partners",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    PartnerItem("One Tree Planted", "North America & Global")
                    PartnerItem("Eden Reforestation Projects", "Madagascar, Haiti, Nepal")
                    PartnerItem("Trees for the Future", "Sub-Saharan Africa")
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "All partners are verified 501(c)(3) organizations with transparent impact reporting. 75% of sponsorship funds are donated to these organizations, 25% supports app development and maintenance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    @Composable
    private fun SponsorshipTier(name: String, price: String, benefit: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = benefit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
    
    @Composable
    private fun TransparencyItem(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    @Composable
    private fun PartnerItem(name: String, location: String) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    @Composable
    private fun AnimatedTreeCount(
        targetCount: Int,
        modifier: Modifier = Modifier
    ) {
        require(targetCount >= 0) { "Tree count must be non-negative" }
        
        val animatedCount by animateIntAsState(
            targetValue = targetCount,
            animationSpec = tween(durationMillis = 2000),
            label = "tree_count_animation"
        )
        
        Text(
            text = animatedCount.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
}

// Factories
class ImpactPresenterFactory @Inject constructor(
    private val presenter: ImpactPresenter
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is ImpactScreen -> presenter
            else -> null
        }
    }
}

class ImpactUiFactory @Inject constructor(
    private val ui: ImpactUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is ImpactScreen -> ui
            else -> null
        }
    }
}