package com.smartfind.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.data.repository.DetectionRepository
import com.smartfind.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val repository: DetectionRepository,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    
    val allDetections: LiveData<List<DetectedObjectWithLocation>> = 
        repository.getAllDetections().asLiveData()
    
    private val _filteredDetections = MutableLiveData<List<DetectedObjectWithLocation>>()
    val filteredDetections: LiveData<List<DetectedObjectWithLocation>> = _filteredDetections
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private var currentSearchQuery: String = ""
    
    init {
        // Initialize filtered detections with all detections
        viewModelScope.launch {
            allDetections.observeForever { detections ->
                if (currentSearchQuery.isEmpty()) {
                    _filteredDetections.value = detections
                } else {
                    filterDetections(currentSearchQuery)
                }
            }
        }
    }
    
    fun searchDetections(query: String) {
        currentSearchQuery = query
        if (query.isEmpty()) {
            _filteredDetections.value = allDetections.value
        } else {
            filterDetections(query)
        }
    }
    
    private fun filterDetections(query: String) {
        val lowerQuery = query.lowercase()
        _filteredDetections.value = allDetections.value?.filter { detection ->
            detection.detectedObject.objectName.lowercase().contains(lowerQuery) ||
            detection.location?.address?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    fun deleteDetection(detection: DetectedObjectWithLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete image files
                ImageUtils.deleteImageFiles(
                    detection.detectedObject.imagePath,
                    detection.detectedObject.thumbnailPath
                )
                
                // Delete from database
                repository.deleteDetection(detection.detectedObject)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete detection: ${e.message}")
            }
        }
    }
    
    fun restoreDetection(detection: DetectedObjectWithLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Re-insert the detection into database
                repository.insertDetection(detection.detectedObject)
                
                // Re-insert location if available
                detection.location?.let {
                    repository.insertLocation(it)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to restore detection: ${e.message}")
            }
        }
    }
    
    fun deleteOldDetections(daysOld: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
                
                // Get old detections to delete their files
                val oldDetections = allDetections.value?.filter {
                    it.detectedObject.timestamp < timestamp
                }
                
                oldDetections?.forEach { detection ->
                    ImageUtils.deleteImageFiles(
                        detection.detectedObject.imagePath,
                        detection.detectedObject.thumbnailPath
                    )
                }
                
                // Delete from database
                val deletedCount = repository.deleteOldDetections(timestamp)
                repository.deleteOldLocations(timestamp)
                
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Deleted $deletedCount old detections"
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete old detections: ${e.message}")
            }
        }
    }
    
    fun exportToCsv(): File? {
        return try {
            val context = getApplication<Application>()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "smartfind_export_${dateFormat.format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Object Name,Confidence,Date,Time,Latitude,Longitude,Address,Image Path\n")
                
                // Write data
                allDetections.value?.forEach { detection ->
                    val obj = detection.detectedObject
                    val loc = detection.location
                    
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(obj.timestamp))
                    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(obj.timestamp))
                    
                    writer.append("${obj.objectName},")
                    writer.append("${obj.confidence},")
                    writer.append("$date,")
                    writer.append("$time,")
                    writer.append("${loc?.latitude ?: ""},")
                    writer.append("${loc?.longitude ?: ""},")
                    writer.append("\"${loc?.address ?: ""}\",")
                    writer.append("${obj.imagePath ?: ""}\n")
                }
            }
            
            file
        } catch (e: Exception) {
            _errorMessage.postValue("Failed to export CSV: ${e.message}")
            null
        }
    }
    
    fun getStorageInfo(): Pair<Long, String> {
        val context = getApplication<Application>()
        val storageDir = ImageUtils.getImageStorageDir(context)
        val size = ImageUtils.getDirectorySize(storageDir)
        val formatted = ImageUtils.formatFileSize(size)
        return Pair(size, formatted)
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
