package com.smartfind.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking detection statistics
 */
@Entity(tableName = "detection_statistics")
data class DetectionStatistics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val total_detections: Int = 0,
    val unique_objects: Int = 0,
    val average_confidence: Float = 0f,
    val most_detected_object: String = "",
    val detection_time_millis: Long = 0, // Total time spent detecting
    val saved_detections: Int = 0,
    val manual_captures: Int = 0,
    val auto_saves: Int = 0
)
