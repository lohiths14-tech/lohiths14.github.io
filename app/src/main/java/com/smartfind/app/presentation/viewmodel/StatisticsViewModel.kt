package com.smartfind.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.dao.DetectionStatisticsDao
import com.smartfind.app.data.local.entity.DetectionStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for managing and displaying detection statistics
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    application: Application,
    private val statisticsDao: DetectionStatisticsDao,
    private val detectedObjectDao: DetectedObjectDao,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    
    val lastSevenDays: LiveData<List<DetectionStatistics>> = statisticsDao.getLastSevenDays().asLiveData()
    val lastThirtyDays: LiveData<List<DetectionStatistics>> = statisticsDao.getLastThirtyDays().asLiveData()
    
    private val _totalDetections = MutableLiveData<Int>()
    val totalDetections: LiveData<Int> = _totalDetections
    
    private val _totalSavedDetections = MutableLiveData<Int>()
    val totalSavedDetections: LiveData<Int> = _totalSavedDetections
    
    private val _averageConfidence = MutableLiveData<Float>()
    val averageConfidence: LiveData<Float> = _averageConfidence
    
    private val _mostFrequentObject = MutableLiveData<String>()
    val mostFrequentObject: LiveData<String> = _mostFrequentObject
    
    private val _totalDetectionTime = MutableLiveData<Long>()
    val totalDetectionTime: LiveData<Long> = _totalDetectionTime
    
    private val _todayStatistics = MutableLiveData<TodayStats>()
    val todayStatistics: LiveData<TodayStats> = _todayStatistics
    
    private val _objectFrequencyMap = MutableLiveData<Map<String, Int>>()
    val objectFrequencyMap: LiveData<Map<String, Int>> = _objectFrequencyMap
    
    data class TodayStats(
        val detectionsCount: Int = 0,
        val savedCount: Int = 0,
        val uniqueObjects: Int = 0,
        val avgConfidence: Float = 0f,
        val timeSpent: Long = 0
    )
    
    init {
        loadAllTimeStatistics()
        loadTodayStatistics()
        loadObjectFrequency()
    }
    
    private fun loadAllTimeStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalDet = statisticsDao.getTotalDetectionsAllTime() ?: 0
                _totalDetections.postValue(totalDet)
                
                val totalSaved = statisticsDao.getTotalSavedDetectionsAllTime() ?: 0
                _totalSavedDetections.postValue(totalSaved)
                
                val avgConf = statisticsDao.getAverageConfidenceAllTime() ?: 0f
                _averageConfidence.postValue(avgConf)
                
                val mostFreq = statisticsDao.getMostFrequentObjectAllTime() ?: "None"
                _mostFrequentObject.postValue(mostFreq)
                
                val totalTime = statisticsDao.getTotalDetectionTimeAllTime() ?: 0L
                _totalDetectionTime.postValue(totalTime)
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error loading all-time stats", e)
            }
        }
    }
    
    private fun loadTodayStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val stats = statisticsDao.getStatisticsForDate(todayDate)
                
                if (stats != null) {
                    _todayStatistics.postValue(
                        TodayStats(
                            detectionsCount = stats.total_detections,
                            savedCount = stats.saved_detections,
                            uniqueObjects = stats.unique_objects,
                            avgConfidence = stats.average_confidence,
                            timeSpent = stats.detection_time_millis
                        )
                    )
                } else {
                    _todayStatistics.postValue(TodayStats())
                }
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error loading today stats", e)
                _todayStatistics.postValue(TodayStats())
            }
        }
    }
    
    fun loadObjectFrequency() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allDetections = detectedObjectDao.getAllDetectionsSync()
                val frequencyMap = allDetections.groupingBy { it.objectName }.eachCount()
                _objectFrequencyMap.postValue(frequencyMap)
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error loading object frequency", e)
            }
        }
    }
    
    fun updateTodayStatistics(
        totalDetections: Int,
        savedDetections: Int,
        uniqueObjects: Set<String>,
        averageConfidence: Float,
        detectionTimeMillis: Long,
        manualCaptures: Int,
        autoSaves: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existing = statisticsDao.getStatisticsForDate(todayDate)
                
                val stats = if (existing != null) {
                    existing.copy(
                        total_detections = existing.total_detections + totalDetections,
                        saved_detections = existing.saved_detections + savedDetections,
                        unique_objects = uniqueObjects.size,
                        average_confidence = (existing.average_confidence + averageConfidence) / 2,
                        detection_time_millis = existing.detection_time_millis + detectionTimeMillis,
                        manual_captures = existing.manual_captures + manualCaptures,
                        auto_saves = existing.auto_saves + autoSaves
                    )
                } else {
                    DetectionStatistics(
                        date = todayDate,
                        total_detections = totalDetections,
                        saved_detections = savedDetections,
                        unique_objects = uniqueObjects.size,
                        average_confidence = averageConfidence,
                        detection_time_millis = detectionTimeMillis,
                        manual_captures = manualCaptures,
                        auto_saves = autoSaves
                    )
                }
                
                statisticsDao.insert(stats)
                loadAllTimeStatistics()
                loadTodayStatistics()
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error updating today stats", e)
            }
        }
    }
    
    fun refreshStatistics() {
        loadAllTimeStatistics()
        loadTodayStatistics()
        loadObjectFrequency()
    }
    
    fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
