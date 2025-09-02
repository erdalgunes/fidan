package com.fidan.timer.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Date,
    val endTime: Date?,
    val duration: Long,
    val completed: Boolean,
    val pauseCount: Int = 0,
    val totalPauseTime: Long = 0,
    val treeType: String = "oak",
    val growthLevel: Float = 0f,
    val notes: String? = null,
    val mood: String? = null,
    val productivity: Int? = null // 1-5 rating
)

@Entity(tableName = "session_analytics")
data class SessionAnalytics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Date,
    val eventType: String, // START, PAUSE, RESUME, COMPLETE, ABANDON
    val metadata: String? = null
)

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,
    val focusDuration: Int = 25,
    val breakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val darkModePreference: String = "system", // system, light, dark
    val selectedTheme: String = "forest",
    val weeklyGoal: Int = 20, // sessions per week
    val dailyGoal: Int = 4 // sessions per day
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconResource: String,
    val unlockedAt: Date?,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val category: String, // streak, total_time, sessions, special
    val rarity: String // common, rare, epic, legendary
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE completed = 1 ORDER BY startTime DESC")
    fun getCompletedSessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch') = DATE('now')")
    fun getTodaySessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch') >= DATE('now', '-7 days')")
    fun getWeeklySessions(): Flow<List<FocusSession>>
    
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    suspend fun getTotalCompletedSessions(): Int
    
    @Query("SELECT SUM(duration) FROM focus_sessions WHERE completed = 1")
    suspend fun getTotalFocusTime(): Long?
    
    @Query("SELECT MAX(DATE(startTime/1000, 'unixepoch')) as lastDate, COUNT(*) as streak FROM focus_sessions WHERE completed = 1 GROUP BY DATE(startTime/1000, 'unixepoch') ORDER BY lastDate DESC")
    suspend fun getCurrentStreak(): Int
    
    @Insert
    suspend fun insertSession(session: FocusSession): Long
    
    @Update
    suspend fun updateSession(session: FocusSession)
    
    @Delete
    suspend fun deleteSession(session: FocusSession)
}

@Dao
interface SessionAnalyticsDao {
    @Insert
    suspend fun logEvent(event: SessionAnalytics)
    
    @Query("SELECT * FROM session_analytics WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getSessionEvents(sessionId: Long): List<SessionAnalytics>
    
    @Query("SELECT COUNT(*) as count, eventType FROM session_analytics GROUP BY eventType")
    suspend fun getEventStatistics(): List<EventStatistic>
}

data class EventStatistic(
    val count: Int,
    val eventType: String
)

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePreferences(preferences: UserPreferences)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE unlockedAt IS NOT NULL")
    fun getUnlockedAchievements(): Flow<List<Achievement>>
    
    @Update
    suspend fun updateAchievement(achievement: Achievement)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)
}

@Database(
    entities = [FocusSession::class, SessionAnalytics::class, UserPreferences::class, Achievement::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun sessionDao(): FocusSessionDao
    abstract fun analyticsDao(): SessionAnalyticsDao
    abstract fun preferencesDao(): UserPreferencesDao
    abstract fun achievementDao(): AchievementDao
    
    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null
        
        fun getDatabase(context: Context): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "timer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}