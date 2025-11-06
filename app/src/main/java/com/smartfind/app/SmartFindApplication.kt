package com.smartfind.app

import android.app.Application
import androidx.work.*
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.initialize
import com.google.firebase.perf.FirebasePerformance
import com.smartfind.app.worker.CleanupWorker
import com.smartfind.app.worker.ReminderCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SmartFindApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        Firebase.initialize(this)
        initializeFirebase()
        
        // Schedule periodic cleanup work
        scheduleCleanupWork()
        
        // Schedule reminder check work
        scheduleReminderCheckWork()
    }
    
    private fun initializeFirebase() {
        // Enable Crashlytics only in release builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        
        // Enable Performance Monitoring
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = !BuildConfig.DEBUG
        
        // Set custom keys for better crash reporting
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        }
    }
    
    private fun scheduleCleanupWork() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            7, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
    
    private fun scheduleReminderCheckWork() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .build()
        
        // Check for reminders every 15 minutes
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
