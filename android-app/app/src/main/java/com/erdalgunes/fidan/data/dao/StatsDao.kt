package com.erdalgunes.fidan.data.dao

import androidx.room.*
import com.erdalgunes.fidan.data.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO for statistics operations.
 * KISS: Simple aggregated stats queries.
 */
@Dao
interface StatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStatsEntity)
    
    @Update
    suspend fun updateDailyStats(stats: DailyStatsEntity)
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: Date): DailyStatsEntity?
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int): Flow<List<DailyStatsEntity>>
    
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    fun getStatsInRange(startDate: Date, endDate: Date): Flow<List<DailyStatsEntity>>
    
    @Query("SELECT SUM(totalFocusMinutes) FROM daily_stats")
    suspend fun getAllTimeFocusMinutes(): Int?
    
    @Query("SELECT SUM(treesGrown) FROM daily_stats")
    suspend fun getAllTimeTreesGrown(): Int?
    
    @Query("SELECT MAX(longestStreak) FROM daily_stats")
    suspend fun getLongestStreak(): Int?
    
    @Query("SELECT AVG(totalFocusMinutes) FROM daily_stats WHERE date >= :startDate")
    suspend fun getAverageDailyFocus(startDate: Date): Float?
}