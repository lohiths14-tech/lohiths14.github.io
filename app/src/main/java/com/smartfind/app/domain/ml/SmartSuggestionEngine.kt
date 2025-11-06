package com.smartfind.app.domain.ml

import com.smartfind.app.data.local.entity.DetectedObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Smart suggestion engine that provides intelligent recommendations
 * based on user's detection patterns and behavior
 */
class SmartSuggestionEngine {
    
    data class Suggestion(
        val type: SuggestionType,
        val message: String,
        val objectName: String?,
        val confidence: Float,
        val actionText: String
    )
    
    enum class SuggestionType {
        SET_REMINDER,
        FREQUENT_OBJECT,
        TIME_PATTERN,
        LOCATION_PATTERN,
        CONFIDENCE_TIP,
        NEW_OBJECT,
        MILESTONE
    }
    
    /**
     * Analyze detection history and generate smart suggestions
     */
    fun generateSuggestions(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        if (detections.isEmpty()) {
            return suggestions
        }
        
        // Check for frequent objects without reminders
        suggestions.addAll(checkFrequentObjectsWithoutReminders(detections))
        
        // Check for time patterns
        suggestions.addAll(detectTimePatterns(detections))
        
        // Check for confidence issues
        suggestions.addAll(analyzeConfidenceLevels(detections))
        
        // Check for new objects
        suggestions.addAll(detectNewObjects(detections))
        
        // Check for milestones
        suggestions.addAll(checkMilestones(detections))
        
        return suggestions.take(5) // Return top 5 suggestions
    }
    
    private fun checkFrequentObjectsWithoutReminders(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // Find objects detected more than 5 times
        val frequencyMap = detections.groupingBy { it.objectName }.eachCount()
        val frequentObjects = frequencyMap.filter { it.value >= 5 }.keys
        
        for (objectName in frequentObjects.take(3)) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.SET_REMINDER,
                    message = "You've detected '$objectName' ${frequencyMap[objectName]} times. Set a reminder to never lose it?",
                    objectName = objectName,
                    confidence = 0.8f,
                    actionText = "Set Reminder"
                )
            )
        }
        
        return suggestions
    }
    
    private fun detectTimePatterns(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // Group detections by hour of day
        val timeGroups = detections.groupBy { detection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = detection.timestamp
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        
        // Find peak detection hour
        val peakHour = timeGroups.maxByOrNull { it.value.size }?.key ?: return suggestions
        val peakCount = timeGroups[peakHour]?.size ?: 0
        
        if (peakCount >= 5) {
            val mostDetectedInPeak = timeGroups[peakHour]
                ?.groupingBy { it.objectName }
                ?.eachCount()
                ?.maxByOrNull { it.value }
                ?.key
            
            if (mostDetectedInPeak != null) {
                val period = when (peakHour) {
                    in 5..11 -> "morning"
                    in 12..16 -> "afternoon"
                    in 17..20 -> "evening"
                    else -> "night"
                }
                
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.TIME_PATTERN,
                        message = "You often detect '$mostDetectedInPeak' in the $period. Set a reminder for that time?",
                        objectName = mostDetectedInPeak,
                        confidence = 0.7f,
                        actionText = "Create $period Reminder"
                    )
                )
            }
        }
        
        return suggestions
    }
    
    private fun analyzeConfidenceLevels(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        if (detections.size < 10) return suggestions
        
        val avgConfidence = detections.map { it.confidence }.average().toFloat()
        
        when {
            avgConfidence < 0.6f -> {
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.CONFIDENCE_TIP,
                        message = "Your average detection confidence is ${String.format(Locale.US, "%.1f%%", avgConfidence * 100)}. Try improving lighting or holding the camera steadier.",
                        objectName = null,
                        confidence = 0.9f,
                        actionText = "View Tips"
                    )
                )
            }
            avgConfidence > 0.85f -> {
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.CONFIDENCE_TIP,
                        message = "Excellent! Your detection confidence is ${String.format(Locale.US, "%.1f%%", avgConfidence * 100)}. You're using SmartFind like a pro!",
                        objectName = null,
                        confidence = 0.9f,
                        actionText = "View Stats"
                    )
                )
            }
        }
        
        return suggestions
    }
    
    private fun detectNewObjects(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        if (detections.size < 5) return suggestions
        
        // Get recent detections (last 24 hours)
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val recentDetections = detections.filter { it.timestamp >= oneDayAgo }
        
        // Get older detections
        val olderDetections = detections.filter { it.timestamp < oneDayAgo }
        
        val recentObjects = recentDetections.map { it.objectName }.toSet()
        val olderObjects = olderDetections.map { it.objectName }.toSet()
        
        val newObjects = recentObjects - olderObjects
        
        if (newObjects.isNotEmpty()) {
            newObjects.take(2).forEach { objectName ->
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.NEW_OBJECT,
                        message = "New detection! You've started tracking '$objectName'. Want to set a reminder for it?",
                        objectName = objectName,
                        confidence = 0.75f,
                        actionText = "Set Reminder"
                    )
                )
            }
        }
        
        return suggestions
    }
    
    private fun checkMilestones(detections: List<DetectedObject>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        val totalCount = detections.size
        val milestones = listOf(10, 50, 100, 500, 1000)
        
        // Check if we just hit a milestone (within last 5 detections)
        for (milestone in milestones) {
            if (totalCount >= milestone && totalCount < milestone + 5) {
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.MILESTONE,
                        message = "🎉 Congratulations! You've made $totalCount detections! Keep up the great work!",
                        objectName = null,
                        confidence = 1.0f,
                        actionText = "View Stats"
                    )
                )
                break
            }
        }
        
        // Check for unique objects milestone
        val uniqueObjects = detections.map { it.objectName }.toSet().size
        val uniqueMilestones = listOf(5, 10, 20, 50)
        
        for (milestone in uniqueMilestones) {
            if (uniqueObjects == milestone) {
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.MILESTONE,
                        message = "🌟 Amazing! You've detected $uniqueObjects different types of objects!",
                        objectName = null,
                        confidence = 1.0f,
                        actionText = "View Objects"
                    )
                )
                break
            }
        }
        
        return suggestions
    }
    
    /**
     * Get a quick tip based on detection count
     */
    fun getQuickTip(detectionCount: Int): String {
        return when {
            detectionCount == 0 -> "Tip: Point your camera at everyday objects like phones, keys, laptops, or bottles!"
            detectionCount < 5 -> "Tip: Hold your camera steady for better detection accuracy."
            detectionCount < 10 -> "Tip: Try detecting objects in good lighting conditions."
            detectionCount < 20 -> "Tip: You can adjust confidence threshold in Settings for more or fewer detections."
            detectionCount < 50 -> "Tip: Set reminders for frequently detected objects so you never lose them!"
            else -> "Tip: Check out your statistics to see your most detected objects!"
        }
    }
    
    /**
     * Suggest optimal confidence threshold based on user's detection history
     */
    fun suggestOptimalConfidenceThreshold(detections: List<DetectedObject>): Float {
        if (detections.size < 20) return 0.6f // Default
        
        val avgConfidence = detections.map { it.confidence }.average().toFloat()
        
        return when {
            avgConfidence > 0.85f -> 0.7f // User has good conditions, can increase threshold
            avgConfidence < 0.6f -> 0.5f // User has poor conditions, lower threshold
            else -> 0.6f // Keep default
        }
    }
}
