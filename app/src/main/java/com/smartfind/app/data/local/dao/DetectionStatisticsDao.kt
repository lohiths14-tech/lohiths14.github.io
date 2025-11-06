package com.smartfind.app.data.local.dao

import androidx.room.*
import com.smartfind.app.data.local.entity.DetectionStatistics
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionStatisticsDao {
    
    @Query("SELECT * FROM detection_statistics ORDER BY date DESC")
    fun getAllStatistics(): Flow<List<DetectionStatistics>>
    
    @Query("SELECT * FROM detection_statistics WHERE date = :date")
    suspend fun getStatisticsForDate(date: String): DetectionStatistics?
    
    @Query("SELECT * FROM detection_statistics WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getStatisticsForDateRange(startDate: String, endDate: String): Flow<List<DetectionStatistics>>
    
    @Query("SELECT SUM(total_detections) FROM detection_statistics")
    suspend fun getTotalDetectionsAllTime(): Int?
    
    @Query("SELECT SUM(saved_detections) FROM detection_statistics")
    suspend fun getTotalSavedDetectionsAllTime(): Int?
    
    @Query("SELECT AVG(average_confidence) FROM detection_statistics")
    suspend fun getAverageConfidenceAllTime(): Float?
    
    @Query("SELECT most_detected_object FROM detection_statistics GROUP BY most_detected_object ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostFrequentObjectAllTime(): String?
    
    @Query("SELECT SUM(detection_time_millis) FROM detection_statistics")
    suspend fun getTotalDetectionTimeAllTime(): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: DetectionStatistics): Long
    
    @Update
    suspend fun update(statistics: DetectionStatistics)
    
    @Delete
    suspend fun delete(statistics: DetectionStatistics)
    
    @Query("DELETE FROM detection_statistics WHERE date < :date")
    suspend fun deleteOlderThan(date: String): Int
    
    @Query("SELECT * FROM detection_statistics ORDER BY date DESC LIMIT 7")
    fun getLastSevenDays(): Flow<List<DetectionStatistics>>
    
    @Query("SELECT * FROM detection_statistics ORDER BY date DESC LIMIT 30")
    fun getLastThirtyDays(): Flow<List<DetectionStatistics>>
}
