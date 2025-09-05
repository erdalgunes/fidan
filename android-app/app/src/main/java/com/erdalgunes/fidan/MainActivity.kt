package com.erdalgunes.fidan

// Android imports
import android.content.Intent
import android.net.Uri
import android.os.Bundle

// AndroidX imports
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.widget.Toast

// Compose imports
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel

// Gamification imports
import com.erdalgunes.fidan.gamification.*
import com.erdalgunes.fidan.ui.gamification.*
import com.erdalgunes.fidan.ui.theme.*

// Kotlinx imports
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// App imports
import com.erdalgunes.fidan.config.AppConfig
import com.erdalgunes.fidan.data.*
import com.erdalgunes.fidan.forest.*
import com.erdalgunes.fidan.ui.theme.FidanTheme
import com.erdalgunes.fidan.ui.viewmodel.ImpactViewModel
import com.erdalgunes.fidan.ui.viewmodel.ImpactViewModelFactory
import com.erdalgunes.fidan.ui.viewmodel.ImpactUiState
import com.erdalgunes.fidan.ui.viewmodel.ErrorType
import com.erdalgunes.fidan.ui.ErrorScreen
import com.erdalgunes.fidan.gamification.*
import com.erdalgunes.fidan.utils.UrlUtils

class MainActivity : ComponentActivity(), TimerCallback {
    private lateinit var timerManager: TimerManager
    private lateinit var forestManager: ForestManager
    private lateinit var timerRepository: com.erdalgunes.fidan.repository.TimerRepository
    private lateinit var forestRepository: com.erdalgunes.fidan.repository.ForestRepository
    private lateinit var gamificationManager: com.erdalgunes.fidan.gamification.GamificationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize repositories following dependency injection pattern
        timerRepository = com.erdalgunes.fidan.repository.DefaultTimerRepository()
        forestRepository = com.erdalgunes.fidan.repository.DefaultForestRepository(this)
        gamificationManager = com.erdalgunes.fidan.gamification.GamificationManager(this)
        
        // Initialize managers with repositories (following dependency inversion)
        timerManager = TimerManager(this, timerRepository, lifecycleScope)
        forestManager = ForestManager(this) // Will be refactored to use repository
        
        setContent {
            FidanTheme {
                FidanApp(timerManager, forestManager, forestRepository, gamificationManager)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Notify timer manager that app is going to background
        timerManager.onAppPaused()
    }
    
    override fun onResume() {
        super.onResume()
        // Notify timer manager that app is resuming
        timerManager.onAppResumed()
    }
    
    override fun onSessionCompleted() {
        // Process session completion through gamification system
        lifecycleScope.launch {
            val sessionData = SessionData(
                taskName = "Focus Session",
                durationMillis = 25 * 60 * 1000L,
                completedDate = java.util.Date(),
                wasCompleted = true
            )
            
            // Process gamification rewards and achievements
            val gamificationResult = gamificationManager.processSessionComplete(sessionData)
            gamificationResult.onSuccess { result ->
                // Show rewards and achievements to user
                if (result.newAchievements.isNotEmpty()) {
                    showToast("🏆 Achievement unlocked! ${result.newAchievements.first().title}")
                }
                
                if (result.newLevel != null) {
                    showToast("🎉 Level up! You're now level ${result.newLevel}")
                }
            }.onError { exception, message ->
                showToast("Progress couldn't be saved: $message")
            }
            
            // Add tree to forest
            val forestResult = forestRepository.addTree(sessionData)
            forestResult.onError { exception, message ->
                showToast("Tree couldn't be saved: $message")
            }
        }
    }
    
    override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
        // Process incomplete session if it was running and had progress
        if (wasRunning && timeElapsed > 0) {
            lifecycleScope.launch {
                val sessionData = SessionData(
                    taskName = "Focus Session (Stopped)",
                    durationMillis = timeElapsed,
                    completedDate = java.util.Date(),
                    wasCompleted = false
                )
                
                // Process partial session through gamification (still earns some rewards)
                val gamificationResult = gamificationManager.processSessionComplete(sessionData)
                gamificationResult.onError { exception, message ->
                    showToast("Progress couldn't be saved: $message")
                }
                
                // Add incomplete tree to forest
                val forestResult = forestRepository.addTree(sessionData)
                forestResult.onError { exception, message ->
                    showToast("Tree couldn't be saved: $message")
                }
            }
        }
    }
    
    override fun onError(error: String, isRecoverable: Boolean) {
        // New callback method for handling timer errors
        showToast("Timer error: $error")
        
        if (!isRecoverable) {
            // For non-recoverable errors, reset the timer
            lifecycleScope.launch {
                timerManager.resetTimer()
            }
        }
    }
    
    private fun showToast(message: String) {
        // Helper method to show user-friendly error messages
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidanApp(
    timerManager: TimerManager,
    forestManager: ForestManager,
    forestRepository: com.erdalgunes.fidan.repository.ForestRepository,
    gamificationManager: com.erdalgunes.fidan.gamification.GamificationManager
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var completedTrees by rememberSaveable { mutableIntStateOf(0) }
    var incompleteTrees by rememberSaveable { mutableIntStateOf(0) }
    
    // Observe timer state from TimerManager
    val timerState by timerManager.state.collectAsState()
    val timerUiState by timerManager.uiState.collectAsState()
    
    // Observe forest stats for error handling
    var forestStats by remember { mutableStateOf<com.erdalgunes.fidan.repository.ForestStats?>(null) }
    var forestError by remember { mutableStateOf<String?>(null) }
    
    // Observe gamification state - TEMPORARILY COMMENTED OUT
    // val playerProgress by gamificationManager.playerProgress.collectAsState()
    // val achievements by gamificationManager.achievements.collectAsState()
    // val dailyChallenge by gamificationManager.dailyChallenge.collectAsState()
    // val recentUnlocks by gamificationManager.recentUnlocks.collectAsState()
    
    // Handle session completion and early stopping
    LaunchedEffect(timerState.sessionCompleted, timerState.isRunning) {
        if (timerState.sessionCompleted && !timerState.isRunning) {
            completedTrees++
        }
    }
    
    // Load forest stats with error handling
    LaunchedEffect(Unit) {
        forestRepository.getForestStats().onSuccess { stats ->
            forestStats = stats
            completedTrees = stats.completedSessions
            incompleteTrees = stats.incompleteSessions
        }.onError { _, message ->
            forestError = message
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
                    label = { Text("Progress") }
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
                },
                playerProgress = PlayerProgress(),
                dailyChallenge = null,
                recentUnlocks = emptyList(),
                onClearUnlocks = { }
            )
            1 -> ForestPlaceholderScreen(innerPadding)
            2 -> com.erdalgunes.fidan.ui.gamification.GamificationScreen(
                paddingValues = innerPadding,
                playerProgress = PlayerProgress(),
                achievements = emptyList(),
                dailyChallenge = null,
                onPurchaseUpgrade = { },
                onPrestige = { }
            )
            3 -> ImpactPlaceholderScreen(innerPadding)
        }
    }
}

@Composable
fun TimerScreen(
    paddingValues: PaddingValues,
    timerManager: TimerManager,
    onSessionStopped: () -> Unit,
    playerProgress: PlayerProgress = PlayerProgress(),
    dailyChallenge: DailyChallenge? = null,
    recentUnlocks: List<Achievement> = emptyList(),
    onClearUnlocks: () -> Unit = {}
) {
    val timerState by timerManager.state.collectAsState()
    val timerUiState by timerManager.uiState.collectAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (timerState.isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "timer_scale"
    )
    
    val timeText = timerManager.getCurrentTimeText()
    
    // Handle error states
    when (val currentUiState = timerUiState) {
        is com.erdalgunes.fidan.common.TimerUiState.Error -> {
            ErrorScreen(
                paddingValues = paddingValues,
                message = currentUiState.message,
                canRecover = currentUiState.canRecover,
                onRetry = { 
                    if (currentUiState.canRecover) {
                        timerManager.resetTimer()
                    }
                }
            )
            return
        }
        else -> { /* Continue with normal timer UI */ }
    }
    
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
        // Top gamification bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            com.erdalgunes.fidan.ui.gamification.CurrencyDisplay(
                currency = playerProgress.currency,
                modifier = Modifier.weight(1f),
                compact = true
            )
            
            com.erdalgunes.fidan.ui.gamification.PlayerLevelDisplay(
                playerLevel = playerProgress.level,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Daily challenge (if available and not completed)
        dailyChallenge?.let { challenge ->
            if (!challenge.isCompleted) {
                var isExpanded by remember { mutableStateOf(false) }
                com.erdalgunes.fidan.ui.gamification.DailyChallengeCard(
                    dailyChallenge = challenge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    isCollapsed = !isExpanded,
                    onToggleCollapse = { isExpanded = !isExpanded }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timer Circle with session progress overlay
        Box(
            modifier = Modifier.size(220.dp).scale(scale),
            contentAlignment = Alignment.Center
        ) {
            com.erdalgunes.fidan.ui.gamification.SessionProgressOverlay(
                progress = 1f - (timerState.timeLeftMillis.toFloat() / (25 * 60 * 1000L)),
                timeLeft = timerState.timeLeftMillis,
                totalTime = 25 * 60 * 1000L,
                modifier = Modifier.size(220.dp)
            )
            
            // Inner timer circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            timerState.treeWithering -> Color(0xFFFFF3E0)
                            else -> Color.White
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Show tree emoji based on state
                Text(
                    text = when {
                        timerState.treeWithering -> "🥀"
                        timerState.isRunning -> "🌱"
                        else -> "🌱"
                    },
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = timeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        timerState.treeWithering -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = when {
                        timerState.treeWithering -> "Tree Withering"
                        else -> "Focus Time"
                    },
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
    
    // Achievement notifications overlay
    com.erdalgunes.fidan.ui.gamification.AchievementNotification(
        achievement = recentUnlocks.firstOrNull(),
        onDismiss = onClearUnlocks
    )
}

/*
@Composable  
fun NewForestScreen(
    paddingValues: PaddingValues,
    forestManager: ForestManager
) {
    val forestState by forestManager.forestState.collectAsState()
    var selectedTree by remember { mutableStateOf<Tree?>(null) }
    
    // Update day/night cycle periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Update every minute
            forestManager.updateDayNightCycle()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (forestState.trees.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏞️",
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Forest Awaits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete focus sessions to grow your virtual forest!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Farm grid
            FarmGrid(
                forestState = forestState,
                onPlotTapped = { plot ->
                    plot.tree?.let { tree -> selectedTree = tree }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Forest info overlay
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val completedTrees = forestState.trees.count { it.sessionData.wasCompleted }
                    val incompleteTrees = forestState.trees.count { !it.sessionData.wasCompleted }
                    
                    Text(
                        text = if (forestState.isDayTime) "☀️ Day" else "🌙 Night",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🌳 $completedTrees trees",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (incompleteTrees > 0) {
                        Text(
                            text = "🌱 $incompleteTrees saplings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        
        // Tree detail dialog
        selectedTree?.let { tree ->
            TreeDetailDialog(
                tree = tree,
                onDismiss = { selectedTree = null }
            )
        }
    }
}
*/

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


/*
@Composable
fun ImpactScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { ImpactRepository() }
    val viewModel: ImpactViewModel = viewModel(
        factory = ImpactViewModelFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // URLs for external links
    val githubSponsorsUrl = AppConfig.GITHUB_SPONSORS_URL
    val transparencyReportUrl = AppConfig.TRANSPARENCY_REPORT_URL
    
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
                val errorState = uiState as ImpactUiState.Error
                ImpactErrorState(
                    errorMessage = errorState.message,
                    errorType = errorState.errorType,
                    onRetry = { viewModel.refresh() }
                )
            }
            is ImpactUiState.Success -> {
                val successState = uiState as ImpactUiState.Success
                ImpactSuccessContent(
                    impactData = successState.data,
                    githubSponsorsUrl = githubSponsorsUrl,
                    transparencyReportUrl = transparencyReportUrl,
                    context = context
                )
            }
        }
    }
}
*/

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
    errorType: ErrorType = ErrorType.GENERIC,
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

@Composable
fun ForestPlaceholderScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌳 Forest View",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Coming Soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ImpactPlaceholderScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌍 Impact View", 
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Coming Soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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