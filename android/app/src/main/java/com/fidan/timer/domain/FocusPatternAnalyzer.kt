package com.fidan.timer.domain

import com.fidan.timer.data.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.math.roundToInt

data class FocusPattern(
    val mostProductiveHour: Int,
    val averageSessionsPerDay: Double,
    val averageFocusDuration: Double,
    val peakDayOfWeek: String,
    val completionRate: Double,
    val streakDays: Int,
    val totalFocusHours: Double,
    val recommendation: FocusRecommendation
)

data class FocusRecommendation(
    val optimalStartTime: String,
    val suggestedDuration: Int,
    val dailyGoal: Int,
    val tips: List<String>,
    val motivationalQuote: String
)

data class ProductivityInsight(
    val title: String,
    val description: String,
    val metric: String,
    val trend: Trend,
    val actionable: String?
)

enum class Trend {
    IMPROVING, STABLE, DECLINING
}

class FocusPatternAnalyzer {
    
    fun analyzeFocusPatterns(sessions: List<FocusSession>): FocusPattern {
        val completedSessions = sessions.filter { it.completed }
        
        val hourlyDistribution = completedSessions.groupBy { 
            Calendar.getInstance().apply { time = it.startTime }.get(Calendar.HOUR_OF_DAY)
        }
        
        val mostProductiveHour = hourlyDistribution.maxByOrNull { it.value.size }?.key ?: 9
        
        val dailySessions = completedSessions.groupBy {
            Calendar.getInstance().apply { time = it.startTime }.get(Calendar.DAY_OF_YEAR)
        }
        
        val avgSessionsPerDay = if (dailySessions.isNotEmpty()) {
            dailySessions.values.map { it.size }.average()
        } else 0.0
        
        val avgFocusDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.duration }.average() / 60000.0 // Convert to minutes
        } else 25.0
        
        val weeklyDistribution = completedSessions.groupBy {
            Calendar.getInstance().apply { time = it.startTime }.get(Calendar.DAY_OF_WEEK)
        }
        
        val peakDay = weeklyDistribution.maxByOrNull { it.value.size }?.key ?: Calendar.MONDAY
        val peakDayName = getDayName(peakDay)
        
        val completionRate = if (sessions.isNotEmpty()) {
            completedSessions.size.toDouble() / sessions.size
        } else 0.0
        
        val streakDays = calculateStreak(completedSessions)
        val totalFocusHours = completedSessions.sumOf { it.duration } / 3600000.0
        
        val recommendation = generateRecommendation(
            mostProductiveHour, 
            avgSessionsPerDay, 
            completionRate,
            streakDays
        )
        
        return FocusPattern(
            mostProductiveHour = mostProductiveHour,
            averageSessionsPerDay = avgSessionsPerDay,
            averageFocusDuration = avgFocusDuration,
            peakDayOfWeek = peakDayName,
            completionRate = completionRate,
            streakDays = streakDays,
            totalFocusHours = totalFocusHours,
            recommendation = recommendation
        )
    }
    
    fun generateInsights(
        current: FocusPattern, 
        previous: FocusPattern?
    ): List<ProductivityInsight> {
        val insights = mutableListOf<ProductivityInsight>()
        
        // Streak insight
        insights.add(
            ProductivityInsight(
                title = "Focus Streak",
                description = "Your current consecutive days of focus",
                metric = "${current.streakDays} days",
                trend = when {
                    previous == null -> Trend.STABLE
                    current.streakDays > previous.streakDays -> Trend.IMPROVING
                    current.streakDays < previous.streakDays -> Trend.DECLINING
                    else -> Trend.STABLE
                },
                actionable = if (current.streakDays < 3) {
                    "Try to maintain at least one session daily"
                } else null
            )
        )
        
        // Completion rate insight
        val completionPercentage = (current.completionRate * 100).roundToInt()
        insights.add(
            ProductivityInsight(
                title = "Session Completion",
                description = "Percentage of started sessions you complete",
                metric = "$completionPercentage%",
                trend = when {
                    previous == null -> Trend.STABLE
                    current.completionRate > previous.completionRate + 0.05 -> Trend.IMPROVING
                    current.completionRate < previous.completionRate - 0.05 -> Trend.DECLINING
                    else -> Trend.STABLE
                },
                actionable = if (completionPercentage < 80) {
                    "Consider shorter sessions or removing distractions"
                } else null
            )
        )
        
        // Productivity hour insight
        insights.add(
            ProductivityInsight(
                title = "Peak Performance",
                description = "Your most productive hour of the day",
                metric = "${current.mostProductiveHour}:00",
                trend = Trend.STABLE,
                actionable = "Schedule important tasks around ${current.mostProductiveHour}:00"
            )
        )
        
        // Daily average insight
        insights.add(
            ProductivityInsight(
                title = "Daily Average",
                description = "Average focus sessions per day",
                metric = String.format("%.1f sessions", current.averageSessionsPerDay),
                trend = when {
                    previous == null -> Trend.STABLE
                    current.averageSessionsPerDay > previous.averageSessionsPerDay + 0.5 -> Trend.IMPROVING
                    current.averageSessionsPerDay < previous.averageSessionsPerDay - 0.5 -> Trend.DECLINING
                    else -> Trend.STABLE
                },
                actionable = if (current.averageSessionsPerDay < 3) {
                    "Aim for at least 3 focus sessions daily"
                } else null
            )
        )
        
        return insights
    }
    
    private fun calculateStreak(sessions: List<FocusSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sortedSessions = sessions.sortedByDescending { it.startTime }
        val calendar = Calendar.getInstance()
        var streak = 0
        var currentDate = calendar.apply { time = Date() }.get(Calendar.DAY_OF_YEAR)
        
        for (session in sortedSessions) {
            val sessionDay = calendar.apply { time = session.startTime }.get(Calendar.DAY_OF_YEAR)
            
            if (sessionDay == currentDate || sessionDay == currentDate - 1) {
                if (sessionDay == currentDate - 1) {
                    streak++
                    currentDate = sessionDay
                }
            } else {
                break
            }
        }
        
        // Add today if there's a session today
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (sortedSessions.any { 
            Calendar.getInstance().apply { time = it.startTime }.get(Calendar.DAY_OF_YEAR) == today 
        }) {
            streak++
        }
        
        return streak
    }
    
    private fun generateRecommendation(
        productiveHour: Int,
        avgSessions: Double,
        completionRate: Double,
        streak: Int
    ): FocusRecommendation {
        val tips = mutableListOf<String>()
        
        if (completionRate < 0.7) {
            tips.add("Try shorter 15-minute sessions to build momentum")
        }
        
        if (avgSessions < 2) {
            tips.add("Set reminders for regular focus breaks")
        }
        
        if (productiveHour in 9..11) {
            tips.add("You're a morning person! Tackle important tasks early")
        } else if (productiveHour in 14..17) {
            tips.add("Afternoon is your prime time! Save complex work for then")
        } else if (productiveHour >= 20) {
            tips.add("Night owl detected! Ensure good lighting for evening sessions")
        }
        
        if (streak > 7) {
            tips.add("Amazing streak! Reward yourself for consistency")
        } else if (streak < 3) {
            tips.add("Build consistency with just one session daily")
        }
        
        val optimalStart = when (productiveHour) {
            in 6..8 -> "7:00 AM"
            in 9..11 -> "9:30 AM"
            in 12..14 -> "1:00 PM"
            in 15..17 -> "3:30 PM"
            in 18..20 -> "7:00 PM"
            else -> "9:00 AM"
        }
        
        val suggestedDuration = when {
            completionRate > 0.9 -> 30
            completionRate > 0.7 -> 25
            completionRate > 0.5 -> 20
            else -> 15
        }
        
        val dailyGoal = when {
            avgSessions > 5 -> 6
            avgSessions > 3 -> 4
            avgSessions > 1 -> 3
            else -> 2
        }
        
        val quote = motivationalQuotes.random()
        
        return FocusRecommendation(
            optimalStartTime = optimalStart,
            suggestedDuration = suggestedDuration,
            dailyGoal = dailyGoal,
            tips = tips,
            motivationalQuote = quote
        )
    }
    
    private fun getDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Unknown"
    }
    
    companion object {
        private val motivationalQuotes = listOf(
            "Focus is the gateway to all thinking.",
            "Where focus goes, energy flows.",
            "The successful warrior is the average person with laser-like focus.",
            "Starve your distractions, feed your focus.",
            "Focus on being productive instead of busy.",
            "Your focus determines your reality.",
            "Concentrate all your thoughts upon the work at hand.",
            "The power of focus is the power to achieve.",
            "Energy flows where attention goes.",
            "Be like a postage stamp—stick to one thing until you get there."
        )
    }
}