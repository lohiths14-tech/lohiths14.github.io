package com.smartfind.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartfind.app.R
import com.smartfind.app.presentation.MainActivity

/**
 * Manages notifications for object reminders
 */
class ReminderNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "smartfind_reminders"
        const val CHANNEL_NAME = "Object Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for object detection reminders"
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showReminderNotification(
        reminderId: Long,
        title: String,
        message: String,
        objectName: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminderId)
            putExtra("object_name", objectName)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        val notificationId = (NOTIFICATION_ID_BASE + reminderId).toInt()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("ReminderNotification", "Permission denied for notification", e)
        }
    }
    
    fun cancelNotification(reminderId: Long) {
        val notificationId = (NOTIFICATION_ID_BASE + reminderId).toInt()
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }
}
