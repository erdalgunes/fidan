package com.erdalgunes.fidan.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.erdalgunes.fidan.data.dao.SessionDao
import com.erdalgunes.fidan.data.dao.TreeDao
import com.erdalgunes.fidan.data.dao.StatsDao
import com.erdalgunes.fidan.data.entity.SessionEntity
import com.erdalgunes.fidan.data.entity.TreeEntity
import com.erdalgunes.fidan.data.entity.DailyStatsEntity

/**
 * Room database for Fidan app following SOLID principles.
 * Single source of truth for all persistent data.
 * KISS: Simple database with clear entities and DAOs.
 */
@Database(
    entities = [
        SessionEntity::class,
        TreeEntity::class,
        DailyStatsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FidanDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    abstract fun treeDao(): TreeDao
    abstract fun statsDao(): StatsDao
    
    companion object {
        private const val DATABASE_NAME = "fidan_database"
        
        @Volatile
        private var INSTANCE: FidanDatabase? = null
        
        fun getInstance(context: Context): FidanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): FidanDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FidanDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}