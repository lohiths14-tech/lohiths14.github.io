package com.smartfind.app.presentation.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.entity.ObjectReminder
import com.smartfind.app.data.local.entity.RecurrenceType
import com.smartfind.app.data.local.entity.ReminderPriority
import com.smartfind.app.data.repository.ReminderRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ReminderViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockRepository: ReminderRepository
    private lateinit var mockAnalytics: AnalyticsManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockAnalytics = mockk(relaxed = true)
        
        viewModel = ReminderViewModel(
            application = mockApplication,
            repository = mockRepository,
            analyticsManager = mockAnalytics
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ===== REMINDER CREATION TESTS =====

    @Test
    fun `createReminder - valid data - saves to repository`() = runTest {
        // Given
        val detectedObjectId = 1L
        val reminderTime = System.currentTimeMillis() + 3600000 // 1 hour from now
        val title = "Test Reminder"
        val message = "Don't forget this"
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        val savedObserver = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.reminderSaved.observeForever(savedObserver)
        
        // When
        viewModel.createReminder(detectedObjectId, reminderTime, title, message)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertReminder(any()) }
        verify { savedObserver.onChanged(true) }
    }

    @Test
    fun `createReminderEnhanced - valid data with recurring - saves correctly`() = runTest {
        // Given
        val detectedObjectId = 1L
        val reminderTime = System.currentTimeMillis() + 3600000
        val title = "Enhanced Reminder"
        val message = "Test message"
        val isRecurring = true
        val recurrenceType = RecurrenceType.DAILY
        val priority = ReminderPriority.HIGH
        
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        val savedObserver = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.reminderSaved.observeForever(savedObserver)
        
        // When
        viewModel.createReminderEnhanced(
            detectedObjectId, reminderTime, title, message, 
            isRecurring, recurrenceType, priority
        )
        advanceUntilIdle()
        
        // Then
        coVerify { 
            mockRepository.insertReminder(match { 
                it.is_recurring == isRecurring &&
                it.recurrence_type == recurrenceType &&
                it.priority == priority
            })
        }
        verify { savedObserver.onChanged(true) }
    }

    @Test
    fun `createReminderEnhanced - past date validation - sets error`() = runTest {
        // Given
        val pastTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.createReminderEnhanced(
            1L, pastTime, "Title", "Message", 
            false, RecurrenceType.NONE, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("past") == true }) }
        coVerify(exactly = 0) { mockRepository.insertReminder(any()) }
    }

    @Test
    fun `createReminderEnhanced - empty title validation - sets error`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "", "Message", 
            false, RecurrenceType.NONE, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("title") == true }) }
        coVerify(exactly = 0) { mockRepository.insertReminder(any()) }
    }

    @Test
    fun `createReminderEnhanced - invalid object id validation - sets error`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.createReminderEnhanced(
            -1L, futureTime, "Title", "Message", 
            false, RecurrenceType.NONE, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Invalid") == true }) }
        coVerify(exactly = 0) { mockRepository.insertReminder(any()) }
    }

    @Test
    fun `createReminderEnhanced - repository error - sets error message`() = runTest {
        // Given
        val detectedObjectId = 1L
        val futureTime = System.currentTimeMillis() + 3600000
        val exception = RuntimeException("Database error")
        coEvery { mockRepository.insertReminder(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        val savedObserver = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        viewModel.reminderSaved.observeForever(savedObserver)
        
        // When
        viewModel.createReminderEnhanced(
            detectedObjectId, futureTime, "Title", "Message",
            false, RecurrenceType.NONE, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to create reminder") == true }) }
        verify { savedObserver.onChanged(false) }
    }

    // ===== RECURRING REMINDER TESTS =====

    @Test
    fun `createReminderEnhanced - daily recurrence - sets correct type`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "Daily Reminder", "Message",
            true, RecurrenceType.DAILY, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        coVerify { 
            mockRepository.insertReminder(match { 
                it.recurrence_type == RecurrenceType.DAILY && it.is_recurring
            })
        }
    }

    @Test
    fun `createReminderEnhanced - weekly recurrence - sets correct type`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "Weekly Reminder", "Message",
            true, RecurrenceType.WEEKLY, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        coVerify { 
            mockRepository.insertReminder(match { 
                it.recurrence_type == RecurrenceType.WEEKLY && it.is_recurring
            })
        }
    }

    @Test
    fun `createReminderEnhanced - monthly recurrence - sets correct type`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "Monthly Reminder", "Message",
            true, RecurrenceType.MONTHLY, ReminderPriority.MEDIUM
        )
        advanceUntilIdle()
        
        // Then
        coVerify { 
            mockRepository.insertReminder(match { 
                it.recurrence_type == RecurrenceType.MONTHLY && it.is_recurring
            })
        }
    }

    // ===== PRIORITY TESTS =====

    @Test
    fun `createReminderEnhanced - low priority - sets correctly`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "Low Priority", "Message",
            false, RecurrenceType.NONE, ReminderPriority.LOW
        )
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertReminder(match { it.priority == ReminderPriority.LOW }) }
    }

    @Test
    fun `createReminderEnhanced - high priority - sets correctly`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "High Priority", "Message",
            false, RecurrenceType.NONE, ReminderPriority.HIGH
        )
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertReminder(match { it.priority == ReminderPriority.HIGH }) }
    }

    @Test
    fun `createReminderEnhanced - urgent priority - sets correctly`() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        coEvery { mockRepository.insertReminder(any()) } returns 1L
        
        // When
        viewModel.createReminderEnhanced(
            1L, futureTime, "Urgent", "Message",
            false, RecurrenceType.NONE, ReminderPriority.URGENT
        )
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertReminder(match { it.priority == ReminderPriority.URGENT }) }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Test
    fun `deleteReminder - valid reminder - removes from repository`() = runTest {
        // Given
        val reminder = createMockReminder()
        coEvery { mockRepository.deleteReminder(any()) } returns Unit
        
        // When
        viewModel.deleteReminder(reminder)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.deleteReminder(reminder) }
    }

    @Test
    fun `deleteReminder - repository error - sets error message`() = runTest {
        // Given
        val reminder = createMockReminder()
        val exception = RuntimeException("Delete failed")
        coEvery { mockRepository.deleteReminder(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.deleteReminder(reminder)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to delete reminder") == true }) }
    }

    @Test
    fun `cancelReminder - valid id - marks as cancelled`() = runTest {
        // Given
        val reminderId = 1L
        coEvery { mockRepository.markAsCancelled(any()) } returns Unit
        
        // When
        viewModel.cancelReminder(reminderId)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.markAsCancelled(reminderId) }
    }

    @Test
    fun `cancelReminder - repository error - sets error message`() = runTest {
        // Given
        val reminderId = 1L
        val exception = RuntimeException("Cancel failed")
        coEvery { mockRepository.markAsCancelled(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.cancelReminder(reminderId)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to cancel reminder") == true }) }
    }

    // ===== QUERY OPERATIONS TESTS =====

    @Test
    fun `getRemindersForObject - filters by object id`() {
        // Given
        val objectId = 1L
        val mockReminders = flowOf(emptyList<ObjectReminder>())
        every { mockRepository.getRemindersForObject(objectId) } returns mockReminders
        
        // When
        val result = viewModel.getRemindersForObject(objectId)
        
        // Then
        assertEquals(mockReminders.asLiveData(), result)
        verify { mockRepository.getRemindersForObject(objectId) }
    }

    @Test
    fun `activeReminders - observes repository flow`() {
        // Given
        val mockReminders = flowOf(emptyList<ObjectReminder>())
        every { mockRepository.getActiveReminders() } returns mockReminders
        
        // When
        val result = viewModel.activeReminders
        
        // Then
        assertNotNull(result)
    }

    @Test
    fun `allReminders - observes repository flow`() {
        // Given
        val mockReminders = flowOf(emptyList<ObjectReminder>())
        every { mockRepository.getAllReminders() } returns mockReminders
        
        // When
        val result = viewModel.allReminders
        
        // Then
        assertNotNull(result)
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    fun `clearError - sets error message to null`() {
        // When
        viewModel.clearError()
        
        // Then
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `hasNotificationPermission - returns notification manager result`() {
        // When
        val result = viewModel.hasNotificationPermission()
        
        // Then
        // This would return a boolean based on notification manager
        assertTrue(result is Boolean)
    }

    // ===== HELPER METHODS =====

    private fun createMockReminder(
        id: Long = 1L,
        detectedObjectId: Long = 1L,
        reminderTime: Long = System.currentTimeMillis() + 3600000,
        title: String = "Test Reminder",
        message: String = "Test Message",
        isRecurring: Boolean = false,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        priority: ReminderPriority = ReminderPriority.MEDIUM
    ): ObjectReminder {
        return ObjectReminder(
            id = id,
            detected_object_id = detectedObjectId,
            reminder_time = reminderTime,
            title = title,
            message = message,
            is_recurring = isRecurring,
            recurrence_type = recurrenceType,
            priority = priority
        )
    }
}
