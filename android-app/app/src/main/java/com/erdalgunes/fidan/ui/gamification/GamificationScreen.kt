package com.erdalgunes.fidan.ui.gamification

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erdalgunes.fidan.gamification.*

/**
 * Comprehensive gamification screen showing player progress, achievements, and upgrades.
 * Follows Material Design 3 with forest theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    paddingValues: PaddingValues,
    playerProgress: PlayerProgress,
    achievements: List<Achievement>,
    dailyChallenge: DailyChallenge?,
    onPurchaseUpgrade: (TreeUpgradeType) -> Unit,
    onPrestige: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
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
            )
    ) {
        // Top status bar with player info
        PlayerStatusBar(
            playerProgress = playerProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
            contentColor = Color(0xFF2E7D32)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Progress") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Achievements") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Upgrades") }
            )
        }
        
        // Tab content
        when (selectedTab) {
            0 -> ProgressTab(
                playerProgress = playerProgress,
                dailyChallenge = dailyChallenge,
                onPrestige = onPrestige
            )
            1 -> AchievementsTab(achievements = achievements)
            2 -> UpgradesTab(
                playerProgress = playerProgress,
                onPurchaseUpgrade = onPurchaseUpgrade
            )
        }
    }
}

@Composable
private fun PlayerStatusBar(
    playerProgress: PlayerProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerLevelDisplay(
                    playerLevel = playerProgress.level,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                CurrencyDisplay(
                    currency = playerProgress.currency,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Sessions",
                    value = playerProgress.totalSessionsCompleted.toString(),
                    icon = "📊"
                )
                StatItem(
                    label = "Current Streak",
                    value = "${playerProgress.currentStreak} days",
                    icon = "🔥"
                )
                StatItem(
                    label = "Prestige",
                    value = "Level ${playerProgress.prestigeLevel}",
                    icon = "✨"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 18.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressTab(
    playerProgress: PlayerProgress,
    dailyChallenge: DailyChallenge?,
    onPrestige: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily challenge
        dailyChallenge?.let { challenge ->
            item {
                var isExpanded by remember { mutableStateOf(false) }
                DailyChallengeCard(
                    dailyChallenge = challenge,
                    isCollapsed = !isExpanded,
                    onToggleCollapse = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Focus time stats
        item {
            StatsCard(
                title = "Focus Statistics",
                stats = listOf(
                    "Total Focus Points" to formatNumber(playerProgress.totalLifetimeFP),
                    "Sessions Completed" to playerProgress.totalSessionsCompleted.toString(),
                    "Longest Streak" to "${playerProgress.longestStreak} days",
                    "Prestige Bonus" to "+${((playerProgress.prestigeBonus - 1.0) * 100).toInt()}%"
                )
            )
        }
        
        // Prestige section
        if (playerProgress.canPrestige) {
            item {
                PrestigeCard(
                    playerProgress = playerProgress,
                    onPrestige = onPrestige
                )
            }
        } else {
            item {
                PrestigeProgressCard(playerProgress = playerProgress)
            }
        }
    }
}

@Composable
private fun AchievementsTab(achievements: List<Achievement>) {
    val groupedAchievements = achievements.groupBy { it.category }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedAchievements.forEach { (category, categoryAchievements) ->
            item {
                AchievementCategorySection(
                    category = category,
                    achievements = categoryAchievements
                )
            }
        }
    }
}

@Composable
private fun UpgradesTab(
    playerProgress: PlayerProgress,
    onPurchaseUpgrade: (TreeUpgradeType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tree Upgrades",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(TreeUpgradeType.values()) { upgradeType ->
            UpgradeCard(
                upgradeType = upgradeType,
                playerProgress = playerProgress,
                onPurchase = { onPurchaseUpgrade(upgradeType) }
            )
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    stats: List<Pair<String, String>>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrestigeCard(
    playerProgress: PlayerProgress,
    onPrestige: () -> Unit
) {
    val preview = playerProgress.getPrestigePreview()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨",
                    fontSize = 24.sp
                )
                Text(
                    text = "Ready to Prestige!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            
            Text(
                text = "Reset your progress to gain permanent bonuses and prestige seeds!",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "You'll gain:",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "✨ ${preview.prestigeSeeds} Prestige Seeds",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "🚀 ${((preview.newPrestigeBonus - playerProgress.prestigeBonus) * 100).toInt()}% bonus",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Button(
                    onClick = onPrestige,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Prestige",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PrestigeProgressCard(playerProgress: PlayerProgress) {
    val requirement = playerProgress.getPrestigeRequirement()
    val progress = playerProgress.totalLifetimeFP.toFloat() / requirement
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF9E9E9E).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Next Prestige",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666)
            )
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF9E9E9E),
                trackColor = Color(0xFF9E9E9E).copy(alpha = 0.2f)
            )
            
            Text(
                text = "${formatNumber(playerProgress.totalLifetimeFP)} / ${formatNumber(requirement)} FP",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun AchievementCategorySection(
    category: AchievementCategory,
    achievements: List<Achievement>
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(achievements) { achievement ->
                        AchievementCard(achievement = achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                when (achievement.tier) {
                    AchievementTier.BRONZE -> Color(0xFFCD7F32).copy(alpha = 0.1f)
                    AchievementTier.SILVER -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
                    AchievementTier.GOLD -> Color(0xFFFFD700).copy(alpha = 0.1f)
                    AchievementTier.PLATINUM -> Color(0xFFE5E4E2).copy(alpha = 0.1f)
                }
            } else {
                Color(0xFF000000).copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (achievement.isUnlocked || !achievement.isHidden) achievement.icon else "🔒",
                fontSize = 24.sp,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .wrapContentSize()
            )
            
            Text(
                text = if (achievement.isUnlocked || !achievement.isHidden) achievement.title else "Hidden",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (achievement.isUnlocked) Color(0xFF2E7D32) else Color(0xFF666666)
            )
            
            if (achievement.isUnlocked || !achievement.isHidden) {
                LinearProgressIndicator(
                    progress = achievement.progressPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (achievement.isUnlocked) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    trackColor = Color(0xFF000000).copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    upgradeType: TreeUpgradeType,
    playerProgress: PlayerProgress,
    onPurchase: () -> Unit
) {
    val cost = playerProgress.treeUpgrades.getUpgradeCost(upgradeType)
    val canAfford = playerProgress.currency.canAfford(cost)
    val canUpgrade = playerProgress.treeUpgrades.canUpgrade(upgradeType)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (canUpgrade && canAfford) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else if (!canUpgrade) {
                Color(0xFF9E9E9E).copy(alpha = 0.1f)
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = getUpgradeIcon(upgradeType),
                fontSize = 24.sp
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = getUpgradeTitle(upgradeType),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                
                Text(
                    text = getUpgradeDescription(upgradeType, playerProgress.treeUpgrades),
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                
                if (canUpgrade) {
                    CurrencyDisplay(
                        currency = cost,
                        compact = true
                    )
                } else {
                    Text(
                        text = "Max Level",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (canUpgrade) {
                Button(
                    onClick = onPurchase,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color(0xFF9E9E9E).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Upgrade",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun getUpgradeIcon(upgradeType: TreeUpgradeType): String {
    return when (upgradeType) {
        TreeUpgradeType.GROWTH_SPEED -> "⚡"
        TreeUpgradeType.MAX_SIZE -> "📏"
        TreeUpgradeType.VARIETY -> "🌺"
        TreeUpgradeType.AUTO_PLANTING -> "🤖"
        TreeUpgradeType.FOREST_EXPANSION -> "🗺️"
        TreeUpgradeType.SEASONAL_EFFECTS -> "🌙"
        TreeUpgradeType.MAGICAL_TREES -> "🔮"
        TreeUpgradeType.OFFLINE_GROWTH -> "💤"
    }
}

private fun getUpgradeTitle(upgradeType: TreeUpgradeType): String {
    return upgradeType.name.replace('_', ' ')
        .lowercase().split(' ').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

private fun getUpgradeDescription(upgradeType: TreeUpgradeType, upgrades: TreeUpgrades): String {
    return when (upgradeType) {
        TreeUpgradeType.GROWTH_SPEED -> "Trees grow ${upgrades.growthSpeed}x faster"
        TreeUpgradeType.MAX_SIZE -> "Trees can reach size ${upgrades.maxTreeSize}"
        TreeUpgradeType.VARIETY -> "${upgrades.treeVariety} tree types available"
        TreeUpgradeType.AUTO_PLANTING -> if (upgrades.autoPlanting) "Trees plant automatically" else "Automatically plant trees"
        TreeUpgradeType.FOREST_EXPANSION -> "Forest area level ${upgrades.forestExpansion}"
        TreeUpgradeType.SEASONAL_EFFECTS -> if (upgrades.seasonalEffects) "Day/night cycles active" else "Enable day/night cycles"
        TreeUpgradeType.MAGICAL_TREES -> if (upgrades.magicalTrees) "Magical effects active" else "Unlock magical tree effects"
        TreeUpgradeType.OFFLINE_GROWTH -> if (upgrades.offlineGrowth) "Trees grow offline" else "Enable offline growth"
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> "${(number / 1_000_000_000)}B"
        number >= 1_000_000 -> "${(number / 1_000_000)}M"
        number >= 1_000 -> "${(number / 1_000)}K"
        else -> number.toString()
    }
}