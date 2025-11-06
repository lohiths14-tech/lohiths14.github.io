package com.smartfind.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.smartfind.app.R
import com.smartfind.app.presentation.MainActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home screen widget for SmartFind app.
 *
 * Provides quick access to:
 * - Recent detection count
 * - Last detected object
 * - Quick action buttons (Camera, History, Reminders)
 * - App launch shortcut
 *
 * Widget sizes:
 * - Small (2x1): Basic stats + camera button
 * - Medium (4x2): Stats + last detection + action buttons
 * - Large (4x3): Full stats + recent detections list
 *
 * Updates:
 * - On demand (when detection occurs)
 * - Periodic (every 30 minutes)
 * - On widget click
 */
class QuickDetectWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget instance created
        // Initialize any resources needed
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
        // Clean up resources
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_OPEN_CAMERA -> {
                openCamera(context)
            }
            ACTION_OPEN_HISTORY -> {
                openHistory(context)
            }
            ACTION_OPEN_REMINDERS -> {
                openReminders(context)
            }
            ACTION_REFRESH -> {
                refreshWidget(context)
            }
        }
    }

    private fun openCamera(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CAMERA
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun openHistory(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_HISTORY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun openReminders(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_REMINDERS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, QuickDetectWidget::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private const val ACTION_OPEN_CAMERA = "com.smartfind.app.widget.OPEN_CAMERA"
        private const val ACTION_OPEN_HISTORY = "com.smartfind.app.widget.OPEN_HISTORY"
        private const val ACTION_OPEN_REMINDERS = "com.smartfind.app.widget.OPEN_REMINDERS"
        private const val ACTION_REFRESH = "com.smartfind.app.widget.REFRESH"

        /**
         * Update widget with latest data
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Get widget data from database (async)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val widgetData = fetchWidgetData(context)
                    val views = createRemoteViews(context, widgetData)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    // Show error state
                    val errorViews = createErrorViews(context)
                    appWidgetManager.updateAppWidget(appWidgetId, errorViews)
                }
            }
        }

        /**
         * Fetch widget data from database
         */
        private suspend fun fetchWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
            try {
                // In production, fetch from repository
                // For now, return mock data or use SharedPreferences
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

                WidgetData(
                    totalDetections = prefs.getInt("total_detections", 0),
                    todayDetections = prefs.getInt("today_detections", 0),
                    lastObject = prefs.getString("last_object", "N/A") ?: "N/A",
                    lastDetectionTime = prefs.getLong("last_detection_time", 0L),
                    activeReminders = prefs.getInt("active_reminders", 0)
                )
            } catch (e: Exception) {
                WidgetData()
            }
        }

        /**
         * Create RemoteViews with widget data
         */
        private fun createRemoteViews(context: Context, data: WidgetData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_detect)

            // Set data
            views.setTextViewText(R.id.tv_total_detections, data.totalDetections.toString())
            views.setTextViewText(R.id.tv_today_detections, "+${data.todayDetections} today")
            views.setTextViewText(R.id.tv_last_object, data.lastObject)

            // Format last detection time
            val timeText = if (data.lastDetectionTime > 0) {
                formatRelativeTime(data.lastDetectionTime)
            } else {
                "No detections yet"
            }
            views.setTextViewText(R.id.tv_last_detection_time, timeText)

            // Active reminders count
            views.setTextViewText(R.id.tv_active_reminders, data.activeReminders.toString())

            // Setup click intents
            setupClickIntents(context, views)

            return views
        }

        /**
         * Create error state RemoteViews
         */
        private fun createErrorViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_detect)

            views.setTextViewText(R.id.tv_total_detections, "--")
            views.setTextViewText(R.id.tv_today_detections, "Error loading data")
            views.setTextViewText(R.id.tv_last_object, "Tap to refresh")
            views.setTextViewText(R.id.tv_last_detection_time, "")
            views.setTextViewText(R.id.tv_active_reminders, "0")

            setupClickIntents(context, views)

            return views
        }

        /**
         * Setup click intents for widget buttons
         */
        private fun setupClickIntents(context: Context, views: RemoteViews) {
            // Main widget click - open app
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

            // Camera button
            val cameraIntent = Intent(context, QuickDetectWidget::class.java).apply {
                action = ACTION_OPEN_CAMERA
            }
            val cameraPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_camera, cameraPendingIntent)

            // History button
            val historyIntent = Intent(context, QuickDetectWidget::class.java).apply {
                action = ACTION_OPEN_HISTORY
            }
            val historyPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                historyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_history, historyPendingIntent)

            // Reminders button
            val remindersIntent = Intent(context, QuickDetectWidget::class.java).apply {
                action = ACTION_OPEN_REMINDERS
            }
            val remindersPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                remindersIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_reminders, remindersPendingIntent)

            // Refresh button
            val refreshIntent = Intent(context, QuickDetectWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                4,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
        }

        /**
         * Format timestamp to relative time (e.g., "2 hours ago")
         */
        private fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }

        /**
         * Manually trigger widget update from app
         */
        fun notifyWidgetDataChanged(context: Context) {
            val intent = Intent(context, QuickDetectWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    android.content.ComponentName(context, QuickDetectWidget::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        /**
         * Update widget data in SharedPreferences
         */
        fun updateWidgetData(
            context: Context,
            totalDetections: Int? = null,
            todayDetections: Int? = null,
            lastObject: String? = null,
            lastDetectionTime: Long? = null,
            activeReminders: Int? = null
        ) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                totalDetections?.let { putInt("total_detections", it) }
                todayDetections?.let { putInt("today_detections", it) }
                lastObject?.let { putString("last_object", it) }
                lastDetectionTime?.let { putLong("last_detection_time", it) }
                activeReminders?.let { putInt("active_reminders", it) }
                apply()
            }

            // Trigger widget update
            notifyWidgetDataChanged(context)
        }
    }
}

/**
 * Data class holding widget display data
 */
data class WidgetData(
    val totalDetections: Int = 0,
    val todayDetections: Int = 0,
    val lastObject: String = "N/A",
    val lastDetectionTime: Long = 0L,
    val activeReminders: Int = 0
)
