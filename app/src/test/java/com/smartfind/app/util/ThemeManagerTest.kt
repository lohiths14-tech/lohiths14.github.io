package com.smartfind.app.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for [ThemeManager].
 *
 * Tests theme mode switching, persistence, and retrieval functionality.
 */
class ThemeManagerTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        // Mock Context
        context = mockk(relaxed = true)
        
        // Mock SharedPreferences
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        // Setup SharedPreferences behavior
        every { sharedPreferences.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.apply() } just Runs
        
        // Mock PreferenceManager
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sharedPreferences
        
        // Mock AppCompatDelegate
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `applyTheme should set default night mode from preferences`() {
        // Given
        val expectedMode = AppCompatDelegate.MODE_NIGHT_YES
        every { sharedPreferences.getInt(any(), any()) } returns expectedMode
        
        // When
        ThemeManager.applyTheme(context)
        
        // Then
        verify { AppCompatDelegate.setDefaultNightMode(expectedMode) }
    }
    
    @Test
    fun `applyTheme should use FOLLOW_SYSTEM as default when no preference saved`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        
        // When
        ThemeManager.applyTheme(context)
        
        // Then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
    }
    
    @Test
    fun `setThemeMode should apply LIGHT theme correctly`() {
        // When
        ThemeManager.setThemeMode(context, ThemeManager.ThemeMode.LIGHT)
        
        // Then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        verify { editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO) }
        verify { editor.apply() }
    }
    
    @Test
    fun `setThemeMode should apply DARK theme correctly`() {
        // When
        ThemeManager.setThemeMode(context, ThemeManager.ThemeMode.DARK)
        
        // Then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        verify { editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES) }
        verify { editor.apply() }
    }
    
    @Test
    fun `setThemeMode should apply SYSTEM theme correctly`() {
        // When
        ThemeManager.setThemeMode(context, ThemeManager.ThemeMode.SYSTEM)
        
        // Then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        verify { editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        verify { editor.apply() }
    }
    
    @Test
    fun `toggleTheme should switch from LIGHT to DARK`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_NO
        
        // When
        val newMode = ThemeManager.toggleTheme(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.DARK, newMode)
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
    }
    
    @Test
    fun `toggleTheme should switch from DARK to LIGHT`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_YES
        
        // When
        val newMode = ThemeManager.toggleTheme(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.LIGHT, newMode)
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
    }
    
    @Test
    fun `toggleTheme should switch from SYSTEM to DARK`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        
        // When
        val newMode = ThemeManager.toggleTheme(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.DARK, newMode)
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
    }
    
    @Test
    fun `getCurrentThemeMode should return LIGHT mode`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_NO
        
        // When
        val mode = ThemeManager.getCurrentThemeMode(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.LIGHT, mode)
    }
    
    @Test
    fun `getCurrentThemeMode should return DARK mode`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_YES
        
        // When
        val mode = ThemeManager.getCurrentThemeMode(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.DARK, mode)
    }
    
    @Test
    fun `getCurrentThemeMode should return SYSTEM mode`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        
        // When
        val mode = ThemeManager.getCurrentThemeMode(context)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.SYSTEM, mode)
    }
    
    @Test
    fun `isDarkModeActive should return true when DARK mode is set`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_YES
        
        // When
        val isDark = ThemeManager.isDarkModeActive(context)
        
        // Then
        assertTrue(isDark)
    }
    
    @Test
    fun `isDarkModeActive should return false when LIGHT mode is set`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_NO
        
        // When
        val isDark = ThemeManager.isDarkModeActive(context)
        
        // Then
        assertFalse(isDark)
    }
    
    @Test
    fun `isDarkModeActive should check system settings when SYSTEM mode is set`() {
        // Given
        every { sharedPreferences.getInt(any(), any()) } returns AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES
        
        every { context.resources } returns resources
        every { resources.configuration } returns configuration
        
        // When
        val isDark = ThemeManager.isDarkModeActive(context)
        
        // Then
        assertTrue(isDark)
    }
    
    @Test
    fun `ThemeMode fromValue should return correct mode`() {
        // Test LIGHT
        assertEquals(
            ThemeManager.ThemeMode.LIGHT,
            ThemeManager.ThemeMode.fromValue(AppCompatDelegate.MODE_NIGHT_NO)
        )
        
        // Test DARK
        assertEquals(
            ThemeManager.ThemeMode.DARK,
            ThemeManager.ThemeMode.fromValue(AppCompatDelegate.MODE_NIGHT_YES)
        )
        
        // Test SYSTEM
        assertEquals(
            ThemeManager.ThemeMode.SYSTEM,
            ThemeManager.ThemeMode.fromValue(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
    }
    
    @Test
    fun `ThemeMode fromValue should return SYSTEM for unknown value`() {
        // When
        val mode = ThemeManager.ThemeMode.fromValue(999)
        
        // Then
        assertEquals(ThemeManager.ThemeMode.SYSTEM, mode)
    }
}
