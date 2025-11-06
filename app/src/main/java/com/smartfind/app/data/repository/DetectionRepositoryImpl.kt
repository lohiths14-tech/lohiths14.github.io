package com.smartfind.app.data.repository

import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.dao.ObjectLocationDao
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionRepositoryImpl @Inject constructor(
    private val detectedObjectDao: DetectedObjectDao,
    private val objectLocationDao: ObjectLocationDao
) : DetectionRepository {
    
    // Detection operations
    override fun getAllDetections(): Flow<List<DetectedObjectWithLocation>> {
        return detectedObjectDao.getAllWithLocation()
    }
    
    override suspend fun getDetectionById(id: Long): DetectedObjectWithLocation? {
        return detectedObjectDao.getByIdWithLocation(id)
    }
    
    override fun searchDetections(query: String): Flow<List<DetectedObject>> {
        return detectedObjectDao.searchByName(query)
    }
    
    override fun getDetectionsSince(startTime: Long): Flow<List<DetectedObject>> {
        return detectedObjectDao.getObjectsSince(startTime)
    }
    
    override suspend fun getDetectionCount(): Int {
        return detectedObjectDao.getCount()
    }
    
    override fun getDistinctObjectNames(): Flow<List<String>> {
        return detectedObjectDao.getDistinctObjectNames()
    }
    
    override suspend fun insertDetection(detectedObject: DetectedObject): Long {
        return detectedObjectDao.insert(detectedObject)
    }
    
    override suspend fun deleteDetection(detectedObject: DetectedObject) {
        detectedObjectDao.delete(detectedObject)
    }
    
    override suspend fun deleteDetectionById(id: Long) {
        detectedObjectDao.deleteById(id)
    }
    
    override suspend fun deleteOldDetections(timestamp: Long): Int {
        return detectedObjectDao.deleteOlderThan(timestamp)
    }
    
    override suspend fun deleteAllDetections() {
        detectedObjectDao.deleteAll()
    }
    
    // Location operations
    override suspend fun insertLocation(location: ObjectLocation): Long {
        return objectLocationDao.insert(location)
    }
    
    override suspend fun getLocationById(id: Long): ObjectLocation? {
        return objectLocationDao.getById(id)
    }
    
    override suspend fun deleteOldLocations(timestamp: Long): Int {
        return objectLocationDao.deleteOlderThan(timestamp)
    }
}
