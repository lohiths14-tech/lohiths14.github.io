package com.smartfind.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfind.app.data.local.SmartFindDatabase
import com.smartfind.app.notification.ReminderNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that checks for pending reminders and triggers notifications
 */
class ReminderCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "reminder_check_worker"
        private const val TAG = "ReminderCheckWorker"
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting reminder check")
                
                val database = SmartFindDatabase.getInstance(applicationContext)
                val reminderDao = database.objectReminderDao()
                val objectDao = database.detectedObjectDao()
                
                val currentTime = System.currentTimeMillis()
                val pendingReminders = reminderDao.getPendingReminders(currentTime)
                
                Log.d(TAG, "Found ${pendingReminders.size} pending reminders")
                
                val notificationManager = ReminderNotificationManager(applicationContext)
                
                for (reminder in pendingReminders) {
                    try {
                        // Get the associated detected object
                        val detectedObject = objectDao.getByIdWithLocation(reminder.detected_object_id)
                        
                        if (detectedObject != null) {
                            // Show notification
                            notificationManager.showReminderNotification(
                                reminderId = reminder.id,
                                title = reminder.title,
                                message = reminder.message,
                                objectName = detectedObject.detectedObject.objectName
                            )
                            
                            // Mark as triggered
                            reminderDao.markAsTriggered(reminder.id)
                            
                            Log.d(TAG, "Triggered reminder ${reminder.id} for object: ${detectedObject.detectedObject.objectName}")
                        } else {
                            Log.w(TAG, "Detected object not found for reminder ${reminder.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing reminder ${reminder.id}", e)
                    }
                }
                
                Log.d(TAG, "Reminder check completed")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Reminder check work failed", e)
                Result.retry()
            }
        }
    }
}
