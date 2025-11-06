package com.smartfind.app.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.smartfind.app.R
import com.smartfind.app.databinding.ActivityOnboardingBinding
import com.smartfind.app.presentation.MainActivity

/**
 * Onboarding activity that guides new users through app features.
 *
 * Shows a series of screens explaining:
 * - Object detection capabilities
 * - Detection history and search
 * - Reminder functionality
 * - Privacy and offline features
 *
 * Only shown on first app launch. Users can skip or complete the tutorial.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    private val onboardingItems = listOf(
        OnboardingItem(
            title = "Welcome to SmartFind",
            description = "Find everyday objects instantly with AI-powered camera detection. Your smart assistant for locating items.",
            imageRes = R.drawable.onboarding_welcome,
            backgroundColor = R.color.onboarding_bg_1
        ),
        OnboardingItem(
            title = "Real-time Object Detection",
            description = "Point your camera at objects and SmartFind will identify them in real-time. Supports 90+ everyday items like keys, wallet, phone, and more.",
            imageRes = R.drawable.onboarding_detection,
            backgroundColor = R.color.onboarding_bg_2
        ),
        OnboardingItem(
            title = "Smart History & Search",
            description = "Every detection is automatically saved with timestamp and location. Search, filter, and find when you last saw any item.",
            imageRes = R.drawable.onboarding_history,
            backgroundColor = R.color.onboarding_bg_3
        ),
        OnboardingItem(
            title = "Set Reminders",
            description = "Never forget important items again! Set custom reminders for objects you detect, with flexible timing options.",
            imageRes = R.drawable.onboarding_reminders,
            backgroundColor = R.color.onboarding_bg_4
        ),
        OnboardingItem(
            title = "100% Private & Offline",
            description = "All processing happens on your device. No data is sent to cloud. Your privacy is our priority. Works completely offline.",
            imageRes = R.drawable.onboarding_privacy,
            backgroundColor = R.color.onboarding_bg_5
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
        updateUI(0)
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUI(position)
            }
        })

        // Setup dots indicator
        binding.dotsIndicator.attachTo(binding.viewPager)
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            if (isLastPage()) {
                finishOnboarding()
            } else {
                binding.viewPager.currentItem += 1
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }

        binding.btnBack.setOnClickListener {
            binding.viewPager.currentItem -= 1
        }
    }

    private fun updateUI(position: Int) {
        // Update background color
        val colorRes = onboardingItems[position].backgroundColor
        binding.root.setBackgroundColor(ContextCompat.getColor(this, colorRes))

        // Show/hide back button
        binding.btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE

        // Update next/get started button
        if (isLastPage()) {
            binding.btnNext.text = "Get Started"
            binding.btnSkip.visibility = View.INVISIBLE
        } else {
            binding.btnNext.text = "Next"
            binding.btnSkip.visibility = View.VISIBLE
        }
    }

    private fun isLastPage(): Boolean {
        return binding.viewPager.currentItem == adapter.itemCount - 1
    }

    private fun finishOnboarding() {
        // Save onboarding completion state
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()

        // Navigate to main activity
        startActivity(Intent(this, MainActivity::class.java))
        finish()

        // Apply transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val PREFS_NAME = "SmartFindPrefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        /**
         * Check if user has completed onboarding
         */
        fun hasCompletedOnboarding(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_COMPLETED, false)
        }

        /**
         * Reset onboarding state (for testing/debugging)
         */
        fun resetOnboarding(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, false)
                .apply()
        }
    }
}

/**
 * Data class representing a single onboarding page
 */
data class OnboardingItem(
    val title: String,
    val description: String,
    val imageRes: Int,
    val backgroundColor: Int
)
