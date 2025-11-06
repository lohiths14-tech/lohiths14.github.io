package com.smartfind.app.data.repository

import com.smartfind.app.data.local.dao.ObjectReminderDao
import com.smartfind.app.data.local.entity.ObjectReminder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for ReminderRepository
 * Tests reminder CRUD operations
 */
class ReminderRepositoryTest {

    @Mock
    private lateinit var mockReminderDao: ObjectReminderDao

    private lateinit var repository: ReminderRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ReminderRepositoryImpl(mockReminderDao)
    }

    @Test
    fun `test insertReminder returns ID`() = runBlocking {
        val testReminder = ObjectReminder(
            id = 0,
            detected_object_id = 1,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Remember: Phone",
            message = "Don't forget your phone",
            is_triggered = false,
            is_cancelled = false
        )

        `when`(mockReminderDao.insert(testReminder)).thenReturn(1L)

        val resultId = repository.insertReminder(testReminder)

        assertEquals(1L, resultId)
        verify(mockReminderDao).insert(testReminder)
    }

    @Test
    fun `test getActiveReminders returns flow`() = runBlocking {
        val expectedFlow = flowOf(emptyList<ObjectReminder>())
        
        `when`(mockReminderDao.getActiveReminders()).thenReturn(expectedFlow)

        val result = repository.getActiveReminders()

        assertNotNull(result)
        verify(mockReminderDao).getActiveReminders()
    }

    @Test
    fun `test markAsTriggered calls dao`() = runBlocking {
        val reminderId = 1L

        repository.markAsTriggered(reminderId)

        verify(mockReminderDao).markAsTriggered(reminderId)
    }

    @Test
    fun `test markAsCancelled calls dao`() = runBlocking {
        val reminderId = 1L

        repository.markAsCancelled(reminderId)

        verify(mockReminderDao).markAsCancelled(reminderId)
    }

    @Test
    fun `test getActiveReminderCount returns count`() = runBlocking {
        `when`(mockReminderDao.getActiveReminderCount()).thenReturn(5)

        val count = repository.getActiveReminderCount()

        assertEquals(5, count)
        verify(mockReminderDao).getActiveReminderCount()
    }

    @Test
    fun `test deleteOldReminders with timestamp`() = runBlocking {
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        `when`(mockReminderDao.deleteOlderThan(cutoffTime)).thenReturn(3)

        val deletedCount = repository.deleteOldReminders(cutoffTime)

        assertEquals(3, deletedCount)
        verify(mockReminderDao).deleteOlderThan(cutoffTime)
    }
}
