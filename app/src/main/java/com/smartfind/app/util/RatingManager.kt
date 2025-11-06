package com.smartfind.app.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the logic for when and how to prompt the user for an in-app review.
 *
 * This class tracks app usage metrics (like launch count and detection events)
 * and uses a set of rules to determine the optimal time to request a review,
 * ensuring the prompt is not shown prematurely or too frequently. It uses Google's
 * In-App Review API.
 *
 * @property context The application context, injected by Hilt.
 */
@Singleton
class RatingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "in_app_rating_prefs"
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_DETECTION_COUNT = "detection_count"
        private const val KEY_FIRST_LAUNCH_TIMESTAMP = "first_launch_timestamp"
        private const val KEY_REVIEW_REQUEST_COMPLETED = "review_request_completed"

        // --- C O N F I G U R A T I O N ---
        // The user must have launched the app at least this many times.
        private const val MIN_LAUNCH_COUNT_THRESHOLD = 5
        // The user must have saved at least this many detections.
        private const val MIN_DETECTION_COUNT_THRESHOLD = 10
        // At least this many days must have passed since the first launch.
        private val MIN_DAYS_SINCE_FIRST_LAUNCH = TimeUnit.DAYS.toMillis(3)
    }

    init {
        // Record the first launch timestamp if it's not already set.
        if (prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIMESTAMP, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Increments the app launch counter. This should be called once per app session,
     * typically in the `onCreate` of the main activity.
     */
    fun incrementLaunchCount() {
        val currentCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, currentCount + 1).apply()
    }

    /**
     * Increments the detection saved counter. This should be called every time
     * a user successfully saves a new object detection.
     */
    fun incrementDetectionCount() {
        val currentCount = prefs.getInt(KEY_DETECTION_COUNT, 0)
        prefs.edit().putInt(KEY_DETECTION_COUNT, currentCount + 1).apply()
    }

    /**
     * Determines whether the conditions to request a review have been met.
     * The logic is designed to target engaged users without being intrusive.
     *
     * @return `true` if a review should be requested, `false` otherwise.
     */
    fun shouldRequestReview(): Boolean {
        // If the review flow has already been successfully shown, never show it again.
        if (prefs.getBoolean(KEY_REVIEW_REQUEST_COMPLETED, false)) {
            return false
        }

        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val detectionCount = prefs.getInt(KEY_DETECTION_COUNT, 0)
        val firstLaunchTimestamp = prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, 0L)
        val timeSinceFirstLaunch = System.currentTimeMillis() - firstLaunchTimestamp

        // Condition 1: User is a regular user (checks launch count and time since first launch)
        val isRegularUser = launchCount >= MIN_LAUNCH_COUNT_THRESHOLD && timeSinceFirstLaunch >= MIN_DAYS_SINCE_FIRST_LAUNCH

        // Condition 2: User is highly engaged (checks number of detections)
        val isEngagedUser = detectionCount >= MIN_DETECTION_COUNT_THRESHOLD

        // Return true if either condition is met
        return isRegularUser || isEngagedUser
    }

    /**
     * Initiates the in-app review flow if conditions are met.
     * This method will first check `shouldRequestReview()`.
     *
     * @param activity The activity context needed to launch the review flow UI.
     */
    fun requestReview(activity: Activity) {
        if (!shouldRequestReview()) {
            return
        }

        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // The review flow has finished. To avoid nagging the user,
                    // we mark it as completed regardless of whether they submitted a review.
                    prefs.edit().putBoolean(KEY_REVIEW_REQUEST_COMPLETED, true).apply()
                }
            } else {
                // An error occurred (e.g., no network connection).
                // We don't mark as completed, so the app can try again later.
                task.exception?.printStackTrace()
            }
        }
    }

    /**
     * Resets all stored rating metrics. For debugging and testing purposes only.
     */
    fun resetRatingMetrics() {
        prefs.edit().clear().apply()
    }
}
