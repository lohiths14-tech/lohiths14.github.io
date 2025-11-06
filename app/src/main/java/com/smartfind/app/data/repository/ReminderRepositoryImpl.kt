package com.smartfind.app.data.repository

import com.smartfind.app.data.local.dao.ObjectReminderDao
import com.smartfind.app.data.local.entity.ObjectReminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReminderRepository using Hilt dependency injection
 */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ObjectReminderDao
) : ReminderRepository {
    
    override fun getAllReminders(): Flow<List<ObjectReminder>> {
        return reminderDao.getAllReminders()
    }
    
    override fun getRemindersForObject(objectId: Long): Flow<List<ObjectReminder>> {
        return reminderDao.getRemindersForObject(objectId)
    }
    
    override fun getActiveReminders(): Flow<List<ObjectReminder>> {
        return reminderDao.getActiveReminders()
    }
    
    override suspend fun getReminderById(id: Long): ObjectReminder? {
        return reminderDao.getReminderById(id)
    }
    
    override suspend fun insertReminder(reminder: ObjectReminder): Long {
        return reminderDao.insert(reminder)
    }
    
    override suspend fun updateReminder(reminder: ObjectReminder) {
        reminderDao.update(reminder)
    }
    
    override suspend fun deleteReminder(reminder: ObjectReminder) {
        reminderDao.delete(reminder)
    }
    
    override suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteById(id)
    }
    
    override suspend fun markAsTriggered(id: Long) {
        reminderDao.markAsTriggered(id)
    }
    
    override suspend fun markAsCancelled(id: Long) {
        reminderDao.markAsCancelled(id)
    }
    
    override suspend fun getActiveReminderCount(): Int {
        return reminderDao.getActiveReminderCount()
    }
    
    override suspend fun deleteOldReminders(timestamp: Long): Int {
        return reminderDao.deleteOlderThan(timestamp)
    }
}
