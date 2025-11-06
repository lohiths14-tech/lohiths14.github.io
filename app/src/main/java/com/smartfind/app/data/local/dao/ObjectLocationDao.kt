package com.smartfind.app.data.local.dao

import androidx.room.*
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectLocationDao {
    
    @Query("SELECT * FROM object_locations WHERE id = :id")
    suspend fun getById(id: Long): ObjectLocation?
    
    @Query("SELECT * FROM object_locations ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ObjectLocation>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: ObjectLocation): Long
    
    @Update
    suspend fun update(location: ObjectLocation)
    
    @Delete
    suspend fun delete(location: ObjectLocation)
    
    @Query("DELETE FROM object_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM object_locations WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM object_locations")
    suspend fun deleteAll()
}
