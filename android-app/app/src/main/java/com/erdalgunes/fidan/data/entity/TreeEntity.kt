package com.erdalgunes.fidan.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a tree grown in the forest.
 * KISS: Simple tree model with essential properties.
 */
@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val species: TreeSpecies,
    val plantedDate: Date,
    val growthStage: GrowthStage,
    val healthStatus: HealthStatus,
    val positionX: Float,
    val positionY: Float
)

enum class TreeSpecies {
    OAK,
    PINE,
    MAPLE,
    BIRCH,
    WILLOW,
    CHERRY
}

enum class GrowthStage {
    SEED,
    SAPLING,
    YOUNG,
    MATURE,
    ANCIENT
}

enum class HealthStatus {
    HEALTHY,
    WITHERING,
    DEAD
}