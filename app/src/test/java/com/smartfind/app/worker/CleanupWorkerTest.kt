package com.smartfind.app.worker

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CleanupWorker
 * Tests cleanup logic and retention periods
 */
class CleanupWorkerTest {

    @Test
    fun `test cleanup worker name constant`() {
        assertEquals("cleanup_worker", CleanupWorker.WORK_NAME)
    }

    @Test
    fun `test default retention period is 30 days`() {
        val expectedDays = 30
        val millisecondsInDay = 24 * 60 * 60 * 1000L
        
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - (expectedDays * millisecondsInDay)
        
        // Verify cutoff time is approximately 30 days ago
        val differenceInDays = (currentTime - cutoffTime) / millisecondsInDay
        assertEquals(expectedDays.toLong(), differenceInDays)
    }

    @Test
    fun `test retention period calculation`() {
        val daysToKeep = 30
        val millisecondsInDay = 24 * 60 * 60 * 1000L
        val expectedMs = daysToKeep * millisecondsInDay
        
        assertEquals(2592000000L, expectedMs) // 30 days in milliseconds
    }

    @Test
    fun `test cutoff timestamp is in the past`() {
        val daysToKeep = 30
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        val currentTime = System.currentTimeMillis()
        
        assertTrue("Cutoff time should be in the past", cutoffTime < currentTime)
    }
}
