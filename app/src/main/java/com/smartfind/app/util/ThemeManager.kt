package com.smartfind.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

/**
 * Manager for handling app theme (Light/Dark mode).
 *
 * Provides functionality to:
 * - Apply saved theme preference
 * - Toggle between light and dark modes
 * - Save theme preference persistently
 * - Get current theme mode
 *
 * @since 1.0
 */
object ThemeManager {
    
    private const val PREF_KEY_THEME = "theme_mode"
    private const val TAG = "ThemeManager"
    
    /**
     * Theme modes available in the app.
     */
    enum class ThemeMode(val value: Int) {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        companion object {
            fun fromValue(value: Int): ThemeMode {
                return values().find { it.value == value } ?: SYSTEM
            }
        }
    }
    
    /**
     * Applies the saved theme preference to the app.
     *
     * This should be called in Application.onCreate() or MainActivity.onCreate()
     * before setContentView() to ensure the theme is applied immediately.
     *
     * @param context The application context
     *
     * Example:
     * ```kotlin
     * class MainActivity : AppCompatActivity() {
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         ThemeManager.applyTheme(this)
     *         super.onCreate(savedInstanceState)
     *     }
     * }
     * ```
     */
    fun applyTheme(context: Context) {
        val prefs = getPreferences(context)
        val themeMode = prefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
    
    /**
     * Sets the theme mode and saves the preference.
     *
     * @param context The application context
     * @param mode The [ThemeMode] to apply
     *
     * Example:
     * ```kotlin
     * ThemeManager.setThemeMode(context, ThemeManager.ThemeMode.DARK)
     * ```
     */
    fun setThemeMode(context: Context, mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.value)
        saveThemePreference(context, mode.value)
    }
    
    /**
     * Toggles between light and dark modes.
     * If currently on SYSTEM mode, switches to DARK.
     *
     * @param context The application context
     * @return The new [ThemeMode] that was applied
     */
    fun toggleTheme(context: Context): ThemeMode {
        val currentMode = getCurrentThemeMode(context)
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(context, newMode)
        return newMode
    }
    
    /**
     * Gets the currently applied theme mode.
     *
     * @param context The application context
     * @return The current [ThemeMode]
     */
    fun getCurrentThemeMode(context: Context): ThemeMode {
        val prefs = getPreferences(context)
        val value = prefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return ThemeMode.fromValue(value)
    }
    
    /**
     * Checks if dark mode is currently active.
     *
     * @param context The application context
     * @return true if dark mode is active, false otherwise
     */
    fun isDarkModeActive(context: Context): Boolean {
        val mode = getCurrentThemeMode(context)
        return when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val uiMode = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
    
    /**
     * Saves the theme preference to SharedPreferences.
     *
     * @param context The application context
     * @param mode The theme mode value to save
     */
    private fun saveThemePreference(context: Context, mode: Int) {
        val prefs = getPreferences(context)
        prefs.edit().putInt(PREF_KEY_THEME, mode).apply()
    }
    
    /**
     * Gets the SharedPreferences instance.
     *
     * @param context The application context
     * @return The SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
