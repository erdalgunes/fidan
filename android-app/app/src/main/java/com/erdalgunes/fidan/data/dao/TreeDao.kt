package com.erdalgunes.fidan.data.dao

import androidx.room.*
import com.erdalgunes.fidan.data.entity.TreeEntity
import com.erdalgunes.fidan.data.entity.TreeSpecies
import com.erdalgunes.fidan.data.entity.GrowthStage
import kotlinx.coroutines.flow.Flow

/**
 * DAO for tree/forest operations.
 * SOLID: Single responsibility for tree data management.
 */
@Dao
interface TreeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: TreeEntity)
    
    @Update
    suspend fun updateTree(tree: TreeEntity)
    
    @Delete
    suspend fun deleteTree(tree: TreeEntity)
    
    @Query("SELECT * FROM trees ORDER BY plantedDate DESC")
    fun getAllTrees(): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE id = :treeId")
    suspend fun getTree(treeId: String): TreeEntity?
    
    @Query("SELECT * FROM trees WHERE sessionId = :sessionId")
    suspend fun getTreeBySession(sessionId: String): TreeEntity?
    
    @Query("SELECT * FROM trees WHERE species = :species")
    fun getTreesBySpecies(species: TreeSpecies): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE growthStage = :stage")
    fun getTreesByGrowthStage(stage: GrowthStage): Flow<List<TreeEntity>>
    
    @Query("SELECT COUNT(*) FROM trees")
    suspend fun getTotalTreeCount(): Int
    
    @Query("SELECT COUNT(DISTINCT species) FROM trees")
    suspend fun getUniqueSpeciesCount(): Int
    
    @Query("UPDATE trees SET growthStage = :newStage WHERE id = :treeId")
    suspend fun updateTreeGrowth(treeId: String, newStage: GrowthStage)
}