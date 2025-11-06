package com.smartfind.app.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.repository.DetectionRepository
import com.smartfind.app.domain.detector.ObjectDetector
import com.smartfind.app.domain.model.DetectionResult
import com.smartfind.app.util.ImageUtils
import com.smartfind.app.util.LocationHelper
import com.smartfind.app.util.RatingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val repository: DetectionRepository,
    private val objectDetector: ObjectDetector,
    private val analyticsManager: AnalyticsManager,
    private val ratingManager: RatingManager
) : AndroidViewModel(application) {

    private val locationHelper = LocationHelper(application)

    private val _detectionResults = MutableLiveData<List<DetectionResult>>()
    val detectionResults: LiveData<List<DetectionResult>> = _detectionResults

    private val _isDetecting = MutableLiveData(false)
    val isDetecting: LiveData<Boolean> = _isDetecting

    private val _detectionCount = MutableLiveData(0)
    val detectionCount: LiveData<Int> = _detectionCount

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _modelInitialized = MutableLiveData(false)
    val modelInitialized: LiveData<Boolean> = _modelInitialized

    private val _isLowLight = MutableLiveData(false)
    val isLowLight: LiveData<Boolean> = _isLowLight

    // Find Mode
    private val _findModeLabel = MutableLiveData<String?>()
    val findModeLabel: LiveData<String?> = _findModeLabel

    private val _objectFoundEvent = MutableLiveData<Boolean>()
    val objectFoundEvent: LiveData<Boolean> = _objectFoundEvent

    private val _distinctObjectNames = MutableLiveData<List<String>>()
    val distinctObjectNames: LiveData<List<String>> = _distinctObjectNames

    private var detectionJob: Job? = null
    private var lastAutoCaptureTime = 0L
    private val autoCaptureIntervalMs = 3000L
    private var lastBitmap: Bitmap? = null
    private var pendingManualCapture: DetectionResult? = null

    // Settings
    var confidenceThreshold = ObjectDetector.DEFAULT_CONFIDENCE_THRESHOLD
    var detectionIntervalMs = 500L
    var autoSaveEnabled = true

    companion object {
        private const val TAG = "CameraViewModel"
    }

    init {
        initializeDetector()
        loadDistinctObjectNames()
    }

    private fun initializeDetector(modelName: String? = null) {
        viewModelScope.launch {
            val success = objectDetector.initialize(confidenceThreshold, modelName)
            _modelInitialized.postValue(success)
            if (!success) {
                _errorMessage.postValue("Failed to initialize object detection model. Please ensure the model file is available.")
            }
        }
    }

    private fun loadDistinctObjectNames() {
        viewModelScope.launch {
            repository.getDistinctObjectNames().collect { names ->
                _distinctObjectNames.postValue(names)
            }
        }
    }

    fun isModelAvailable(): Boolean {
        return objectDetector.isModelAvailable()
    }

    fun startDetection() {
        if (_isDetecting.value == true) return

        _isDetecting.value = true
        detectionJob = viewModelScope.launch {
            while (_isDetecting.value == true) {
                delay(detectionIntervalMs)
            }
        }
    }

    fun stopDetection() {
        _isDetecting.value = false
        detectionJob?.cancel()
        detectionJob = null
        _detectionResults.value = emptyList()
    }

    fun startFindMode(label: String) {
        _findModeLabel.value = label
        // analyticsManager.trackFindModeStarted(label)
    }

    fun stopFindMode() {
        _findModeLabel.value = null
    }

    fun onObjectFoundEventHandled() {
        _objectFoundEvent.value = false
    }

    suspend fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        return withContext(Dispatchers.Default) {
            try {
                // Store last bitmap for manual capture
                lastBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)

                // Check for low light conditions
                checkLowLight(bitmap)

                // Handle pending manual capture
                pendingManualCapture?.let { result ->
                    saveDetection(result, bitmap)
                    pendingManualCapture = null
                }

                val startTime = System.currentTimeMillis()
                val results = objectDetector.detect(bitmap)
                val detectionTime = System.currentTimeMillis() - startTime

                _detectionResults.postValue(results)

                if (results.isNotEmpty()) {
                    _detectionCount.postValue((_detectionCount.value ?: 0) + results.size)

                    // Check if the object in "Find Mode" is detected
                    _findModeLabel.value?.let { label ->
                        if (results.any { it.label.equals(label, ignoreCase = true) }) {
                            _objectFoundEvent.postValue(true)
                            // analyticsManager.trackFindModeObjectFound(label)
                        }
                    }

                    // Track analytics for each detection
                    results.forEach { result ->
                        analyticsManager.trackObjectDetection(
                            objectName = result.label,
                            confidence = result.confidence,
                            detectionTimeMs = detectionTime,
                            isGpuEnabled = objectDetector.isGpuAccelerationEnabled()
                        )
                    }

                    // Auto-capture if confidence detection above threshold
                    val highConfidenceResult = results.firstOrNull { it.confidence >= confidenceThreshold }
                    if (highConfidenceResult != null && shouldAutoCapture()) {
                        if (autoSaveEnabled) {
                            saveDetection(highConfidenceResult, bitmap)
                        }
                        lastAutoCaptureTime = System.currentTimeMillis()
                    }
                }

                results
            } catch (e: Exception) {
                Log.e(TAG, "Error detecting objects", e)
                analyticsManager.trackError(
                    errorType = "detection_error",
                    errorMessage = e.message ?: "Unknown detection error",
                    screenName = "camera"
                )
                emptyList()
            }
        }
    }

    private fun checkLowLight(bitmap: Bitmap) {
        val brightness = calculateBrightness(bitmap)
        _isLowLight.postValue(brightness < 50) // Threshold for low light
    }

    private fun calculateBrightness(bitmap: Bitmap): Int {
        // Sample pixels to calculate average brightness
        val sampleSize = 10
        var totalBrightness = 0
        var sampleCount = 0

        for (x in 0 until bitmap.width step bitmap.width / sampleSize) {
            for (y in 0 until bitmap.height step bitmap.height / sampleSize) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    totalBrightness += (r + g + b) / 3
                    sampleCount++
                }
            }
        }

        return if (sampleCount > 0) totalBrightness / sampleCount else 0
    }

    private fun shouldAutoCapture(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAutoCaptureTime >= autoCaptureIntervalMs
    }

    fun saveDetection(result: DetectionResult, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // Save image
                val imageFile = ImageUtils.createImageFile(context, result.label)
                val saved = ImageUtils.saveBitmap(bitmap, imageFile)

                if (!saved) {
                    Log.e(TAG, "Failed to save image")
                    return@launch
                }

                // Create thumbnail
                val thumbnailFile = ImageUtils.saveThumbnail(context, imageFile)

                // Get location
                val location = if (locationHelper.hasLocationPermission()) {
                    locationHelper.getCurrentLocation()
                } else {
                    null
                }

                val objectLocation = locationHelper.createObjectLocation(location)
                val locationId = objectLocation?.let {
                    repository.insertLocation(it)
                }

                // Save to database
                val detectedObject = DetectedObject(
                    objectName = result.label,
                    confidence = result.confidence,
                    timestamp = System.currentTimeMillis(),
                    imagePath = imageFile.absolutePath,
                    thumbnailPath = thumbnailFile?.absolutePath,
                    locationId = locationId
                )

                repository.insertDetection(detectedObject)
                Log.d(TAG, "Detection saved: ${result.label}")

                // Notify the rating manager about the new detection.
                ratingManager.incrementDetectionCount()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving detection", e)
                _errorMessage.postValue("Failed to save detection: ${e.message}")
            }
        }
    }

    fun updateConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold
        objectDetector.updateConfidenceThreshold(threshold)
        // Reinitialize detector with new threshold
        initializeDetector()
    }

    fun updateModel(modelName: String) {
        // Reinitialize detector with new model
        initializeDetector(modelName)
    }

    fun getCurrentModelName(): String {
        return objectDetector.getCurrentModelName()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun requestManualCapture(result: DetectionResult) {
        pendingManualCapture = result
    }

    override fun onCleared() {
        super.onCleared()
        objectDetector.close()
        stopDetection()
    }
}
