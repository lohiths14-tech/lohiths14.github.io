package com.smartfind.app.data.local.dao

import androidx.room.*
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectedObjectDao {
    
    @Transaction
    @Query("SELECT * FROM detected_objects ORDER BY timestamp DESC")
    fun getAllWithLocation(): Flow<List<DetectedObjectWithLocation>>
    
    @Transaction
    @Query("SELECT * FROM detected_objects WHERE id = :id")
    suspend fun getByIdWithLocation(id: Long): DetectedObjectWithLocation?
    
    @Query("SELECT * FROM detected_objects WHERE object_name LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchByName(query: String): Flow<List<DetectedObject>>
    
    @Query("SELECT * FROM detected_objects WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getObjectsSince(startTime: Long): Flow<List<DetectedObject>>
    
    @Query("SELECT COUNT(*) FROM detected_objects")
    suspend fun getCount(): Int
    
    @Query("SELECT DISTINCT object_name FROM detected_objects ORDER BY object_name ASC")
    fun getDistinctObjectNames(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detectedObject: DetectedObject): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(objects: List<DetectedObject>)
    
    @Update
    suspend fun update(detectedObject: DetectedObject)
    
    @Delete
    suspend fun delete(detectedObject: DetectedObject)
    
    @Query("DELETE FROM detected_objects WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM detected_objects WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM detected_objects")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM detected_objects ORDER BY timestamp DESC")
    suspend fun getAllDetectionsSync(): List<DetectedObject>
}
