package com.pula.surveysync.data.local.dao

import androidx.room.*
import com.pula.surveysync.data.local.entity.FarmerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Farmer entities.
 * Provides methods to query the pre-loaded farmer database.
 */
@Dao
interface FarmerDao {

    /**
     * Get all active farmers as a Flow for reactive UI updates
     */
    @Query("SELECT * FROM farmers WHERE isActive = 1 ORDER BY lastName, firstName")
    fun getAllActiveFarmers(): Flow<List<FarmerEntity>>

    /**
     * Get all farmers (including inactive)
     */
    @Query("SELECT * FROM farmers ORDER BY lastName, firstName")
    fun getAllFarmers(): Flow<List<FarmerEntity>>

    /**
     * Get a specific farmer by ID
     */
    @Query("SELECT * FROM farmers WHERE id = :farmerId")
    suspend fun getFarmerById(farmerId: String): FarmerEntity?

    /**
     * Search farmers by name
     */
    @Query("""
        SELECT * FROM farmers 
        WHERE isActive = 1 
        AND (firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%')
        ORDER BY lastName, firstName
    """)
    fun searchFarmers(query: String): Flow<List<FarmerEntity>>

    /**
     * Search farmers by location
     */
    @Query("""
        SELECT * FROM farmers 
        WHERE isActive = 1 
        AND (village LIKE '%' || :location || '%' 
            OR district LIKE '%' || :location || '%' 
            OR region LIKE '%' || :location || '%')
        ORDER BY lastName, firstName
    """)
    fun searchFarmersByLocation(location: String): Flow<List<FarmerEntity>>

    /**
     * Insert a farmer (for pre-loading data)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farmer: FarmerEntity)

    /**
     * Insert multiple farmers (bulk pre-load)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(farmers: List<FarmerEntity>)

    /**
     * Update a farmer
     */
    @Update
    suspend fun update(farmer: FarmerEntity)

    /**
     * Delete a farmer (rarely used, prefer marking inactive)
     */
    @Delete
    suspend fun delete(farmer: FarmerEntity)

    /**
     * Mark a farmer as inactive
     */
    @Query("UPDATE farmers SET isActive = 0 WHERE id = :farmerId")
    suspend fun deactivate(farmerId: String)

    /**
     * Get farmer count (useful for checking if data is pre-loaded)
     */
    @Query("SELECT COUNT(*) FROM farmers WHERE isActive = 1")
    suspend fun getActiveFarmerCount(): Int

    /**
     * Clear all farmers (for testing or re-syncing master data)
     */
    @Query("DELETE FROM farmers")
    suspend fun deleteAll()
}

