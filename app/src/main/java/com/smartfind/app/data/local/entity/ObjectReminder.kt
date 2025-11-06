package com.smartfind.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a reminder for a detected object with recurring support
 */
@Entity(
    tableName = "object_reminders",
    foreignKeys = [
        ForeignKey(
            entity = DetectedObject::class,
            parentColumns = ["id"],
            childColumns = ["detected_object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("detected_object_id"), Index("reminder_time")]
)
data class ObjectReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val detected_object_id: Long,
    val reminder_time: Long,
    val title: String,
    val message: String,
    val is_triggered: Boolean = false,
    val is_cancelled: Boolean = false,
    val created_at: Long = System.currentTimeMillis(),
    // Recurring reminder support
    val is_recurring: Boolean = false,
    val recurrence_type: RecurrenceType = RecurrenceType.NONE,
    val recurrence_interval: Int = 0, // in days for daily, weeks for weekly, etc.
    val last_triggered_at: Long = 0,
    // Snooze support
    val snooze_until: Long = 0,
    val snooze_count: Int = 0,
    // Priority
    val priority: ReminderPriority = ReminderPriority.MEDIUM
)

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class ReminderPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
