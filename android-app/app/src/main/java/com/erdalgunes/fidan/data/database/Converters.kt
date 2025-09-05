package com.erdalgunes.fidan.data.database

import androidx.room.TypeConverter
import com.erdalgunes.fidan.data.entity.TreeSpecies
import com.erdalgunes.fidan.data.entity.GrowthStage
import com.erdalgunes.fidan.data.entity.HealthStatus
import java.util.Date

/**
 * Type converters for Room database.
 * DRY: Centralized conversion logic.
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromTreeSpecies(species: TreeSpecies): String {
        return species.name
    }
    
    @TypeConverter
    fun toTreeSpecies(species: String): TreeSpecies {
        return TreeSpecies.valueOf(species)
    }
    
    @TypeConverter
    fun fromGrowthStage(stage: GrowthStage): String {
        return stage.name
    }
    
    @TypeConverter
    fun toGrowthStage(stage: String): GrowthStage {
        return GrowthStage.valueOf(stage)
    }
    
    @TypeConverter
    fun fromHealthStatus(status: HealthStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toHealthStatus(status: String): HealthStatus {
        return HealthStatus.valueOf(status)
    }
}