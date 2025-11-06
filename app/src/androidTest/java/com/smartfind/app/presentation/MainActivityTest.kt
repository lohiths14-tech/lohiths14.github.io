package com.smartfind.app.presentation

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfind.app.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for MainActivity
 * Tests UI components and navigation
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun testActivityLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Verify activity is displayed
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }

    @Test
    fun testBottomNavigationIsDisplayed() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Verify bottom navigation exists
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }

    @Test
    fun testBottomNavigationHasAllMenuItems() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Verify all navigation items exist
        onView(withId(R.id.homeFragment))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.historyFragment))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.searchFragment))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.settingsFragment))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}
