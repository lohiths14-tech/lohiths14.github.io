package com.smartfind.app.data.repository

import com.smartfind.app.data.local.entity.ObjectReminder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for reminder operations
 */
interface ReminderRepository {
    
    fun getAllReminders(): Flow<List<ObjectReminder>>
    
    fun getRemindersForObject(objectId: Long): Flow<List<ObjectReminder>>
    
    fun getActiveReminders(): Flow<List<ObjectReminder>>
    
    suspend fun getReminderById(id: Long): ObjectReminder?
    
    suspend fun insertReminder(reminder: ObjectReminder): Long
    
    suspend fun updateReminder(reminder: ObjectReminder)
    
    suspend fun deleteReminder(reminder: ObjectReminder)
    
    suspend fun deleteReminderById(id: Long)
    
    suspend fun markAsTriggered(id: Long)
    
    suspend fun markAsCancelled(id: Long)
    
    suspend fun getActiveReminderCount(): Int
    
    suspend fun deleteOldReminders(timestamp: Long): Int
}
