package com.erdalgunes.fidan.ui.gamification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.erdalgunes.fidan.gamification.*

/**
 * Currency display component showing Focus Points, Growth Tokens, and Prestige Seeds.
 * Follows KISS principle with clean, readable design.
 */
@Composable
fun CurrencyDisplay(
    currency: GameCurrency,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyItem(
                icon = "🎯",
                value = currency.focusPoints,
                label = if (!compact) "FP" else null,
                color = Color(0xFF4CAF50)
            )
            
            if (currency.growthTokens > 0) {
                CurrencyItem(
                    icon = "🌱",
                    value = currency.growthTokens,
                    label = if (!compact) "GT" else null,
                    color = Color(0xFF8BC34A)
                )
            }
            
            if (currency.prestigeSeeds > 0) {
                CurrencyItem(
                    icon = "✨",
                    value = currency.prestigeSeeds,
                    label = if (!compact) "PS" else null,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
private fun CurrencyItem(
    icon: String,
    value: Long,
    label: String?,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Text(
            text = formatNumber(value),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

/**
 * Player level display with animated XP progress.
 * Uses circular progress for a compact, elegant display.
 */
@Composable
fun PlayerLevelDisplay(
    playerLevel: PlayerLevel,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = playerLevel.progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOutCubic
        ),
        label = "xp_progress"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Level number in circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF2E7D32)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = playerLevel.level.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Level ${playerLevel.level}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                
                // XP Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                    
                    Text(
                        text = "${playerLevel.currentXP} / ${playerLevel.currentXP + playerLevel.xpToNextLevel} XP",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

/**
 * Achievement notification popup.
 * Follows Material Design for consistency and accessibility.
 */
@Composable
fun AchievementNotification(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = achievement != null,
        enter = slideInVertically() + fadeIn() + scaleIn(),
        exit = slideOutVertically() + fadeOut() + scaleOut()
    ) {
        achievement?.let { ach ->
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Achievement icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    when (ach.tier) {
                                        AchievementTier.BRONZE -> Color(0xFFCD7F32)
                                        AchievementTier.SILVER -> Color(0xFFC0C0C0)
                                        AchievementTier.GOLD -> Color(0xFFFFD700)
                                        AchievementTier.PLATINUM -> Color(0xFFE5E4E2)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ach.icon,
                                fontSize = 32.sp
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🏆 Achievement Unlocked!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            
                            Text(
                                text = ach.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = ach.description,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // Reward display
                        val reward = ach.getReward()
                        if (reward.focusPoints > 0 || reward.growthTokens > 0 || reward.experiencePoints > 0) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                )
                            ) {
                                CurrencyDisplay(
                                    currency = reward,
                                    modifier = Modifier.padding(12.dp),
                                    compact = true
                                )
                            }
                        }
                        
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Awesome!",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Daily challenge card component.
 * Compact design that doesn't obstruct the main timer.
 */
@Composable
fun DailyChallengeCard(
    dailyChallenge: DailyChallenge,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = true,
    onToggleCollapse: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (dailyChallenge.isCompleted) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFF2196F3).copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (dailyChallenge.isCompleted) "✅" else "🎯",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Daily Challenge",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dailyChallenge.isCompleted) Color(0xFF2E7D32) else Color(0xFF1976D2)
                    )
                }
                
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        tint = Color(0xFF666666)
                    )
                }
            }
            
            if (!isCollapsed) {
                Text(
                    text = dailyChallenge.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                
                // Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = dailyChallenge.progressPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (dailyChallenge.isCompleted) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        trackColor = Color(0xFF000000).copy(alpha = 0.1f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${dailyChallenge.progress} / ${dailyChallenge.target}",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                        
                        if (!dailyChallenge.isCompleted) {
                            Text(
                                text = "${(dailyChallenge.progressPercentage).toInt()}%",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                // Reward preview
                if (!dailyChallenge.isCompleted) {
                    Text(
                        text = "Reward: ${dailyChallenge.reward.focusPoints} FP + ${dailyChallenge.reward.experiencePoints} XP",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Compact achievement progress indicator.
 * Shows recent achievements and progress overview.
 */
@Composable
fun AchievementProgress(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size
    val progressPercentage = if (totalCount > 0) (unlockedCount.toFloat() / totalCount) * 100f else 0f
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Achievements",
                tint = Color(0xFFFF9800)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Achievements",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progressPercentage / 100f,
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF9800),
                        trackColor = Color(0xFFFF9800).copy(alpha = 0.2f)
                    )
                    
                    Text(
                        text = "$unlockedCount/$totalCount",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

/**
 * Session progress overlay for timer.
 * Circular progress indicator that wraps around timer display.
 */
@Composable
fun SessionProgressOverlay(
    progress: Float,
    timeLeft: Long,
    totalTime: Long,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 500,
            easing = EaseOutCubic
        ),
        label = "session_progress"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Progress circle
        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.size(220.dp),
            color = Color(0xFF4CAF50),
            strokeWidth = 8.dp,
            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
        )
        
        // Focus earnings preview
        val estimatedFP = ((totalTime - timeLeft) / (60 * 1000L)).toInt()
        if (estimatedFP > 0) {
            Card(
                modifier = Modifier
                    .offset(y = 60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+$estimatedFP FP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Utility function to format large numbers compactly.
 */
private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> "${(number / 1_000_000_000)}B"
        number >= 1_000_000 -> "${(number / 1_000_000)}M"
        number >= 1_000 -> "${(number / 1_000)}K"
        else -> number.toString()
    }
}