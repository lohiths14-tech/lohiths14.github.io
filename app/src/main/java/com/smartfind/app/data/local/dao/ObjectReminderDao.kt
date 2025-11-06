package com.smartfind.app.data.local.dao

import androidx.room.*
import com.smartfind.app.data.local.entity.ObjectReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectReminderDao {
    
    @Query("SELECT * FROM object_reminders ORDER BY reminder_time ASC")
    fun getAllReminders(): Flow<List<ObjectReminder>>
    
    @Query("SELECT * FROM object_reminders WHERE detected_object_id = :objectId")
    fun getRemindersForObject(objectId: Long): Flow<List<ObjectReminder>>
    
    @Query("SELECT * FROM object_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ObjectReminder?
    
    @Query("SELECT * FROM object_reminders WHERE is_triggered = 0 AND is_cancelled = 0 AND reminder_time <= :currentTime")
    suspend fun getPendingReminders(currentTime: Long): List<ObjectReminder>
    
    @Query("SELECT * FROM object_reminders WHERE is_triggered = 0 AND is_cancelled = 0")
    fun getActiveReminders(): Flow<List<ObjectReminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ObjectReminder): Long
    
    @Update
    suspend fun update(reminder: ObjectReminder)
    
    @Delete
    suspend fun delete(reminder: ObjectReminder)
    
    @Query("DELETE FROM object_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE object_reminders SET is_triggered = 1 WHERE id = :id")
    suspend fun markAsTriggered(id: Long)
    
    @Query("UPDATE object_reminders SET is_cancelled = 1 WHERE id = :id")
    suspend fun markAsCancelled(id: Long)
    
    @Query("DELETE FROM object_reminders WHERE reminder_time < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("SELECT COUNT(*) FROM object_reminders WHERE is_triggered = 0 AND is_cancelled = 0")
    suspend fun getActiveReminderCount(): Int
}
