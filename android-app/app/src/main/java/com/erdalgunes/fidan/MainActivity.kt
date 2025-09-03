package com.erdalgunes.fidan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.erdalgunes.fidan.data.ImpactRepository
import com.erdalgunes.fidan.data.ImpactData
import com.erdalgunes.fidan.data.Result

class MainActivity : ComponentActivity(), TimerCallback {
    private lateinit var timerManager: TimerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        timerManager = TimerManager(this, lifecycleScope)
        
        setContent {
            FidanTheme {
                FidanApp(timerManager)
            }
        }
    }
    
    override fun onSessionCompleted() {
        // This will be handled in the Composable through state updates
    }
    
    override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
        // This will be handled in the Composable through state updates
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
    }
}

@Composable
fun FidanTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF81C784),
        secondary = Color(0xFF795548),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFA1887F),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onBackground = Color(0xFF1B5E20),
        onSurface = Color(0xFF2E7D32)
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidanApp(timerManager: TimerManager) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var completedTrees by rememberSaveable { mutableIntStateOf(0) }
    var incompleteTrees by rememberSaveable { mutableIntStateOf(0) }
    
    // Observe timer state from TimerManager
    val timerState by timerManager.state.collectAsState()
    
    // Handle session completion and early stopping
    LaunchedEffect(timerState.sessionCompleted, timerState.isRunning) {
        if (timerState.sessionCompleted && !timerState.isRunning) {
            completedTrees++
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Fidan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("⏰", fontSize = 20.sp) },
                    label = { Text("Timer") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🌳", fontSize = 20.sp) },
                    label = { Text("Forest") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("🌍", fontSize = 20.sp) },
                    label = { Text("Impact") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TimerScreen(
                paddingValues = innerPadding,
                timerManager = timerManager,
                onSessionStopped = { 
                    if (timerState.timeLeftMillis < 25 * 60 * 1000L) {
                        incompleteTrees++
                    }
                }
            )
            1 -> ForestScreen(innerPadding, completedTrees, incompleteTrees)
            2 -> StatsScreen(innerPadding, completedTrees, incompleteTrees)
            3 -> ImpactScreen(innerPadding)
        }
    }
}

@Composable
fun TimerScreen(
    paddingValues: PaddingValues,
    timerManager: TimerManager,
    onSessionStopped: () -> Unit
) {
    val timerState by timerManager.state.collectAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (timerState.isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "timer_scale"
    )
    
    val timeText = timerManager.getCurrentTimeText()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Timer Circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Focus Time",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Start and Stop Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (timerState.isRunning) {
                FloatingActionButton(
                    onClick = { 
                        timerManager.stopTimer()
                        onSessionStopped()
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { 
                        timerManager.startTimer()
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = timerManager.getStatusMessage(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ForestScreen(
    paddingValues: PaddingValues,
    completedTrees: Int,
    incompleteTrees: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show forest visualization
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // Show completed trees
                    repeat(completedTrees) {
                        Text(
                            text = "🌳",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    // Show incomplete trees (stubs)
                    repeat(incompleteTrees) {
                        Text(
                            text = "🌱",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    // Show placeholder if no trees yet
                    if (completedTrees == 0 && incompleteTrees == 0) {
                        Text(
                            text = "🏞️",
                            fontSize = 72.sp
                        )
                    }
                }
                
                Text(
                    text = "Your Forest",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$completedTrees 🌳 Full Trees",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (incompleteTrees > 0) {
                        Text(
                            text = "$incompleteTrees 🌱 Seedlings (incomplete)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (completedTrees == 0 && incompleteTrees == 0) {
                        "Complete focus sessions to grow your virtual forest!"
                    } else {
                        "Keep focusing to grow more trees!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsScreen(
    paddingValues: PaddingValues,
    completedTrees: Int,
    incompleteTrees: Int
) {
    val totalSessions = completedTrees + incompleteTrees
    val completionRate = if (totalSessions > 0) {
        (completedTrees * 100) / totalSessions
    } else 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Today",
                value = "0h",
                subtitle = "Focus Time"
            )
            StatCard(
                title = "This Week",
                value = "0h",
                subtitle = "Total Time"
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Sessions",
                value = "$completedTrees",
                subtitle = "Completed"
            )
            StatCard(
                title = "Success",
                value = "$completionRate%",
                subtitle = "Completion Rate"
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

sealed class ImpactUiState {
    object Loading : ImpactUiState()
    data class Success(val data: ImpactData) : ImpactUiState()
    data class Error(val message: String) : ImpactUiState()
}

@Composable
fun ImpactScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { ImpactRepository() }
    var uiState by remember { mutableStateOf<ImpactUiState>(ImpactUiState.Loading) }
    val scope = rememberCoroutineScope()
    
    // URLs for external links
    val githubSponsorsUrl = "https://github.com/sponsors/erdalgunes"
    val transparencyReportUrl = "https://github.com/erdalgunes/fidan/wiki/Transparency-Report"
    
    // Load impact data using repository
    LaunchedEffect(Unit) {
        when (val result = repository.getImpactData()) {
            is Result.Success -> uiState = ImpactUiState.Success(result.data)
            is Result.Error -> uiState = ImpactUiState.Error(result.message)
            is Result.Loading -> uiState = ImpactUiState.Loading
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Real Environmental Impact",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        when (uiState) {
            is ImpactUiState.Loading -> {
                ImpactLoadingState()
            }
            is ImpactUiState.Error -> {
                ImpactErrorState(
                    errorMessage = (uiState as ImpactUiState.Error).message,
                    onRetry = {
                        scope.launch {
                            uiState = ImpactUiState.Loading
                            when (val result = repository.getImpactData()) {
                                is Result.Success -> uiState = ImpactUiState.Success(result.data)
                                is Result.Error -> uiState = ImpactUiState.Error(result.message)
                                is Result.Loading -> uiState = ImpactUiState.Loading
                            }
                        }
                    }
                )
            }
            is ImpactUiState.Success -> {
                ImpactSuccessContent(
                    impactData = (uiState as ImpactUiState.Success).data,
                    githubSponsorsUrl = githubSponsorsUrl,
                    transparencyReportUrl = transparencyReportUrl,
                    context = context
                )
            }
        }
    }
}

@Composable
fun ImpactLoadingState() {
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
fun ImpactErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
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
fun ImpactSuccessContent(
    impactData: ImpactData,
    githubSponsorsUrl: String,
    transparencyReportUrl: String,
    context: android.content.Context
) {
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubSponsorsUrl))
                        context.startActivity(intent)
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
                
                TransparencyItem("Total Donations", "${"$%,.2f".format(impactData.totalDonations)}")
                TransparencyItem("Tree Planting Fund", "75% of proceeds")
                TransparencyItem("Maintenance Fund", "25% for development")
                TransparencyItem("Partner Organizations", "${impactData.partnersCount} active")
                TransparencyItem("Monthly Growth", "+${impactData.monthlyGrowth}%")
                TransparencyItem("Planting Certificates", "${impactData.certificates} verified")
                TransparencyItem("Last Update", impactData.lastUpdated)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(transparencyReportUrl))
                        context.startActivity(intent)
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
fun SponsorshipTier(name: String, price: String, benefit: String) {
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
fun TransparencyItem(label: String, value: String) {
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
fun PartnerItem(name: String, location: String) {
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
fun AnimatedTreeCount(
    targetCount: Int,
    modifier: Modifier = Modifier
) {
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

@Preview(showBackground = true)
@Composable
fun FidanAppPreview() {
    FidanTheme {
        // Preview with mock timer manager would need DI setup
        // Commenting out for now
        // FidanApp(timerManager)
    }
}