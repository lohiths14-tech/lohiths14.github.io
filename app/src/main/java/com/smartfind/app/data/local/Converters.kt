package com.smartfind.app.data.local

import androidx.room.TypeConverter
import com.smartfind.app.data.local.entity.RecurrenceType
import com.smartfind.app.data.local.entity.ReminderPriority

/**
 * Room TypeConverters for enum types
 */
class Converters {
    
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String {
        return value.name
    }
    
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return try {
            RecurrenceType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RecurrenceType.NONE
        }
    }
    
    @TypeConverter
    fun fromReminderPriority(value: ReminderPriority): String {
        return value.name
    }
    
    @TypeConverter
    fun toReminderPriority(value: String): ReminderPriority {
        return try {
            ReminderPriority.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ReminderPriority.MEDIUM
        }
    }
}
