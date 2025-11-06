package com.smartfind.app.data.repository

import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.flow.Flow

interface DetectionRepository {
    
    // Detection operations
    fun getAllDetections(): Flow<List<DetectedObjectWithLocation>>
    
    suspend fun getDetectionById(id: Long): DetectedObjectWithLocation?
    
    fun searchDetections(query: String): Flow<List<DetectedObject>>
    
    fun getDetectionsSince(startTime: Long): Flow<List<DetectedObject>>
    
    suspend fun getDetectionCount(): Int
    
    fun getDistinctObjectNames(): Flow<List<String>>
    
    suspend fun insertDetection(detectedObject: DetectedObject): Long
    
    suspend fun deleteDetection(detectedObject: DetectedObject)
    
    suspend fun deleteDetectionById(id: Long)
    
    suspend fun deleteOldDetections(timestamp: Long): Int
    
    suspend fun deleteAllDetections()
    
    // Location operations
    suspend fun insertLocation(location: ObjectLocation): Long
    
    suspend fun getLocationById(id: Long): ObjectLocation?
    
    suspend fun deleteOldLocations(timestamp: Long): Int
}
