package com.smartfind.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfind.app.data.local.SmartFindDatabase
import com.smartfind.app.data.repository.DetectionRepositoryImpl
import com.smartfind.app.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "cleanup_worker"
        private const val TAG = "CleanupWorker"
        private const val DAYS_TO_KEEP = 30
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting cleanup work")
                
                val database = SmartFindDatabase.getInstance(applicationContext)
                val repository = DetectionRepositoryImpl(
                    database.detectedObjectDao(),
                    database.objectLocationDao()
                )
                
                val cutoffTime = System.currentTimeMillis() - (DAYS_TO_KEEP * 24 * 60 * 60 * 1000L)
                
                // Delete old database entries
                val deletedCount = repository.deleteOldDetections(cutoffTime)
                repository.deleteOldLocations(cutoffTime)
                
                Log.d(TAG, "Cleanup completed: deleted $deletedCount old detections")
                
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup work failed", e)
                Result.retry()
            }
        }
    }
}
