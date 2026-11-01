package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PartPilotDao {

    // --- Locations ---
    @Query("SELECT * FROM locations ORDER BY shelf ASC, level ASC, box ASC")
    fun getAllLocationsFlow(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY shelf ASC, level ASC, box ASC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE code = :code LIMIT 1")
    suspend fun getLocationByCode(code: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    // --- Parts ---
    @Query("SELECT * FROM parts ORDER BY name ASC")
    fun getAllPartsFlow(): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts ORDER BY name ASC")
    suspend fun getAllParts(): List<PartEntity>

    @Query("SELECT * FROM parts WHERE id = :id LIMIT 1")
    fun getPartFlow(id: Long): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE id = :id LIMIT 1")
    suspend fun getPartById(id: Long): PartEntity?

    @Query("SELECT * FROM parts WHERE locationId = :locationId")
    suspend fun getPartsByLocation(locationId: Long): List<PartEntity>

    @Query("SELECT * FROM parts WHERE name LIKE '%' || :query || '%' OR specs LIKE '%' || :query || '%' OR remarks LIKE '%' || :query || '%'")
    suspend fun searchParts(query: String): List<PartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: PartEntity): Long

    @Update
    suspend fun updatePart(part: PartEntity)

    @Delete
    suspend fun deletePart(part: PartEntity)

    // --- Part Events ---
    @Query("SELECT * FROM part_events WHERE partId = :partId ORDER BY createdAt DESC")
    fun getEventsForPartFlow(partId: Long): Flow<List<PartEventEntity>>

    @Query("SELECT * FROM part_events WHERE partId = :partId ORDER BY createdAt DESC")
    suspend fun getEventsForPart(partId: Long): List<PartEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartEvent(event: PartEventEntity): Long

    // --- Projects ---
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectFlow(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    // --- Project Parts ---
    @Query("SELECT * FROM project_parts WHERE projectId = :projectId")
    fun getProjectPartsFlow(projectId: Long): Flow<List<ProjectPartEntity>>

    @Query("SELECT * FROM project_parts WHERE projectId = :projectId")
    suspend fun getProjectParts(projectId: Long): List<ProjectPartEntity>

    @Query("SELECT * FROM project_parts")
    suspend fun getAllProjectParts(): List<ProjectPartEntity>

    @Query("SELECT * FROM project_parts WHERE partId = :partId")
    suspend fun getProjectPartsForPart(partId: Long): List<ProjectPartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectPart(part: ProjectPartEntity): Long

    @Update
    suspend fun updateProjectPart(part: ProjectPartEntity)

    @Delete
    suspend fun deleteProjectPart(part: ProjectPartEntity)

    // --- Cart Items ---
    @Query("SELECT * FROM cart_items ORDER BY createdAt DESC")
    fun getAllCartItemsFlow(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY createdAt DESC")
    suspend fun getAllCartItems(): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity): Long

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Delete
    suspend fun deleteCartItem(item: CartItemEntity)

    // --- AI Action Log ---
    @Query("SELECT * FROM ai_action_log ORDER BY executedAt DESC")
    fun getAllAiLogsFlow(): Flow<List<AiActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiLog(log: AiActionLogEntity): Long
}
