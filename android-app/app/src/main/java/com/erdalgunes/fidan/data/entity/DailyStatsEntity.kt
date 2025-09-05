package com.erdalgunes.fidan.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for tracking daily statistics.
 * DRY: Aggregated stats prevent recalculation.
 */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: Date,
    val totalSessions: Int,
    val completedSessions: Int,
    val totalFocusMinutes: Int,
    val treesGrown: Int,
    val longestStreak: Int
)