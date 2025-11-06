package com.smartfind.app

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfind.app.data.local.SmartFindDatabase
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.ObjectLocation
import com.smartfind.app.data.local.entity.ObjectReminder
import com.smartfind.app.data.local.entity.RecurrenceType
import com.smartfind.app.data.local.entity.ReminderPriority
import com.smartfind.app.data.repository.DetectionRepositoryImpl
import com.smartfind.app.data.repository.ReminderRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DetectionFlowIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: SmartFindDatabase
    private lateinit var detectionRepository: DetectionRepositoryImpl
    private lateinit var reminderRepository: ReminderRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartFindDatabase::class.java
        ).allowMainThreadQueries().build()

        detectionRepository = DetectionRepositoryImpl(
            detectedObjectDao = database.detectedObjectDao(),
            objectLocationDao = database.objectLocationDao()
        )

        reminderRepository = ReminderRepositoryImpl(
            reminderDao = database.objectReminderDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun cleanup() {
        database.close()
    }
    // ===== DETECTION FLOW TESTS =====

    @Test
    fun completeDetectionFlow_savesToDatabaseWithLocation() = runBlocking {
        // Given - Create a detected object with location
        val location2 = ObjectLocation(
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 8.0f,
            address = "New York, NY",
            timestamp = System.currentTimeMillis()
        )

        val detection = DetectedObject(
            objectName = "phone",
            confidence = 0.85f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/mock/path/phone.jpg",
            thumbnailPath = "/mock/path/thumb_phone.jpg",
            locationId = null
        )

        // When - Save location first, then detection
        val locationId = detectionRepository.insertLocation(location2)
        val detectionWithLocation = detection.copy(locationId = locationId)
        val detectionId = detectionRepository.insertDetection(detectionWithLocation)

        // Then - Verify data is saved correctly
        val savedDetections = detectionRepository.getAllDetections().first()
        assertEquals(1, savedDetections.size)
        
        val savedDetection = savedDetections[0]
        assertEquals("phone", savedDetection.detectedObject.objectName)
        assertEquals(0.85f, savedDetection.detectedObject.confidence, 0.001f)
        assertNotNull(savedDetection.location)
        assertEquals("New York, NY", savedDetection.location?.address)
        assertEquals(locationId, savedDetection.detectedObject.locationId)
    }

    @Test
    fun detectionWithoutLocation_savesWithoutLocationData() = runBlocking {
        // Given - Detection without location
        val detection = DetectedObject(
            objectName = "laptop",
            confidence = 0.92f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/mock/path/laptop.jpg",
            thumbnailPath = null,
            locationId = null
        )

        // When - Save detection
        val detectionId = detectionRepository.insertDetection(detection)

        // Then - Verify detection saved without location
        val savedDetections = detectionRepository.getAllDetections().first()
        assertEquals(1, savedDetections.size)
        
        val savedDetection = savedDetections[0]
        assertEquals("laptop", savedDetection.detectedObject.objectName)
        assertNull(savedDetection.location)
        assertNull(savedDetection.detectedObject.locationId)
    }

    @Test
    fun searchDetections_filtersCorrectly() = runBlocking {
        // Given - Multiple detections
        val phone = DetectedObject(
            objectName = "phone", confidence = 0.85f,
            timestamp = System.currentTimeMillis(), imagePath = "/phone.jpg",
            thumbnailPath = null, locationId = null
        )
        val laptop = DetectedObject(
            objectName = "laptop", confidence = 0.90f,
            timestamp = System.currentTimeMillis(), imagePath = "/laptop.jpg",
            thumbnailPath = null, locationId = null
        )
        val tablet = DetectedObject(
            objectName = "tablet", confidence = 0.75f,
            timestamp = System.currentTimeMillis(), imagePath = "/tablet.jpg",
            thumbnailPath = null, locationId = null
        )

        // When - Save all detections
        detectionRepository.insertDetection(phone)
        detectionRepository.insertDetection(laptop)
        detectionRepository.insertDetection(tablet)

        // Then - Search by partial name
        val phoneResults = detectionRepository.searchDetections("pho").first()
        assertEquals(1, phoneResults.size)
        assertEquals("phone", phoneResults[0].objectName)

        // Search by common substring
        val tabResults = detectionRepository.searchDetections("tab").first()
        assertEquals(1, tabResults.size)
        assertEquals("tablet", tabResults[0].objectName)

        // Search non-existent
        val noResults = detectionRepository.searchDetections("car").first()
        assertEquals(0, noResults.size)
    }
    // ===== REMINDER FLOW TESTS =====

    @Test
    fun completeReminderFlow_createsAndTriggersNotification() = runBlocking {
        // Given - Create detection first
        val detection = DetectedObject(
            objectName = "keys",
            confidence = 0.88f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/keys.jpg",
            thumbnailPath = null,
            locationId = null
        )
        val detectionId = detectionRepository.insertDetection(detection)

        // Create reminder for the detection
        val reminder = ObjectReminder(
            detected_object_id = detectionId,
            reminder_time = System.currentTimeMillis() + 1000, // 1 second from now
            title = "Don't forget your keys",
            message = "You left your keys on the counter",
            priority = ReminderPriority.HIGH
        )

        // When - Save reminder
        val reminderId = reminderRepository.insertReminder(reminder)

        // Then - Verify reminder is saved and retrievable
        val activeReminders = reminderRepository.getActiveReminders().first()
        assertEquals(1, activeReminders.size)
        
        val savedReminder = activeReminders[0]
        assertEquals("Don't forget your keys", savedReminder.title)
        assertEquals(ReminderPriority.HIGH, savedReminder.priority)
        assertEquals(detectionId, savedReminder.detected_object_id)

        // Test pending reminders (past due)
        Thread.sleep(1100) // Wait for reminder to be due
        val pendingReminders = reminderRepository.getActiveReminders().first()
        assertEquals(1, pendingReminders.size)

        // Mark as triggered
        reminderRepository.markAsTriggered(reminderId)
        val activeAfterTrigger = reminderRepository.getActiveReminders().first()
        assertEquals(0, activeAfterTrigger.size)
    }

    @Test
    fun recurringReminder_setsCorrectProperties() = runBlocking {
        // Given - Detection
        val detection = DetectedObject(
            objectName = "medication", confidence = 0.95f,
            timestamp = System.currentTimeMillis(), imagePath = "/medication.jpg",
            thumbnailPath = null, locationId = null
        )
        val detectionId = detectionRepository.insertDetection(detection)

        // Create recurring reminder
        val recurringReminder = ObjectReminder(
            detected_object_id = detectionId,
            reminder_time = System.currentTimeMillis() + 86400000,
            title = "Take medication",
            message = "Don't forget your daily medication",
            is_recurring = true,
            recurrence_type = RecurrenceType.DAILY,
            priority = ReminderPriority.URGENT
        )

        // When - Save recurring reminder
        val reminderId = reminderRepository.insertReminder(recurringReminder)

        // Then - Verify recurring properties
        val savedReminder = reminderRepository.getReminderById(reminderId)
        assertNotNull(savedReminder)
        assertTrue(savedReminder!!.is_recurring)
        assertEquals(RecurrenceType.DAILY, savedReminder.recurrence_type)
        assertEquals(ReminderPriority.URGENT, savedReminder.priority)
    }

    @Test
    fun reminderForObject_filtersCorrectly() = runBlocking {
        // Given - Multiple detections and reminders
        val phoneDetection = DetectedObject(
            objectName = "phone", confidence = 0.85f,
            timestamp = System.currentTimeMillis(), imagePath = "/phone.jpg",
            thumbnailPath = null, locationId = null
        )
        val keysDetection = DetectedObject(
            objectName = "keys", confidence = 0.88f,
            timestamp = System.currentTimeMillis(), imagePath = "/keys.jpg",
            thumbnailPath = null, locationId = null
        )

        val phoneId = detectionRepository.insertDetection(phoneDetection)
        val keysId = detectionRepository.insertDetection(keysDetection)

        // Create reminders for each
        val phoneReminder = ObjectReminder(
            detected_object_id = phoneId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Phone reminder", message = "Check your phone"
        )
        val keysReminder1 = ObjectReminder(
            detected_object_id = keysId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Keys reminder 1", message = "Don't forget keys"
        )
        val keysReminder2 = ObjectReminder(
            detected_object_id = keysId,
            reminder_time = System.currentTimeMillis() + 7200000,
            title = "Keys reminder 2", message = "Keys reminder again"
        )

        // When - Save all reminders
        reminderRepository.insertReminder(phoneReminder)
        reminderRepository.insertReminder(keysReminder1)
        reminderRepository.insertReminder(keysReminder2)

        // Then - Filter by object
        val phoneReminders = reminderRepository.getRemindersForObject(phoneId).first()
        assertEquals(1, phoneReminders.size)
        assertEquals("Phone reminder", phoneReminders[0].title)

        val keysReminders = reminderRepository.getRemindersForObject(keysId).first()
        assertEquals(2, keysReminders.size)
        assertTrue(keysReminders.any { it.title == "Keys reminder 1" })
        assertTrue(keysReminders.any { it.title == "Keys reminder 2" })
    }

    // ===== CASCADE DELETE TESTS =====

    @Test
    fun deleteDetection_cascadeDeletesReminders() = runBlocking {
        // Given - Detection with reminders
        val detection = DetectedObject(
            objectName = "wallet", confidence = 0.92f,
            timestamp = System.currentTimeMillis(), imagePath = "/wallet.jpg",
            thumbnailPath = null, locationId = null
        )
        val detectionId = detectionRepository.insertDetection(detection)

        val reminder1 = ObjectReminder(
            detected_object_id = detectionId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Wallet reminder 1",
            message = "Don't forget wallet"
        )
        val reminder2 = ObjectReminder(
            detected_object_id = detectionId,
            reminder_time = System.currentTimeMillis() + 7200000,
            title = "Wallet reminder 2", message = "Wallet check"
        )

        reminderRepository.insertReminder(reminder1)
        reminderRepository.insertReminder(reminder2)

        // Verify reminders exist
        val initialReminders = reminderRepository.getRemindersForObject(detectionId).first()
        assertEquals(2, initialReminders.size)

        // When - Delete detection
        detectionRepository.deleteDetection(detection.copy(id = detectionId))

        // Then - Verify cascade delete worked
        val remainingDetections = detectionRepository.getAllDetections().first()
        assertEquals(0, remainingDetections.size)

        val remainingReminders = reminderRepository.getAllReminders().first()
        assertEquals(0, remainingReminders.size)
    }

    // ===== PERFORMANCE TESTS =====

    @Test
    fun bulkOperations_performEfficiently() = runBlocking {
        // Given - Large number of detections
        val startTime = System.currentTimeMillis()
        val detections = mutableListOf<DetectedObject>()
        
        // Create 100 detections
        repeat(100) { i ->
            detections.add(DetectedObject(
                objectName = "object_$i",
                confidence = 0.5f + (i % 50) * 0.01f, // Vary confidence
                timestamp = System.currentTimeMillis() + i * 1000,
                imagePath = "/path/object_$i.jpg",
                thumbnailPath = null,
                locationId = null
            ))
        }

        // When - Insert all detections
        detections.forEach { detection ->
            detectionRepository.insertDetection(detection)
        }

        val insertTime = System.currentTimeMillis() - startTime

        // Then - Verify all inserted and query performance
        val queryStart = System.currentTimeMillis()
        val allDetections = detectionRepository.getAllDetections().first()
        val queryTime = System.currentTimeMillis() - queryStart

        assertEquals(100, allDetections.size)
        
        // Performance assertions (should be fast)
        assertTrue("Insert time should be < 5000ms, was ${insertTime}ms", insertTime < 5000)
        assertTrue("Query time should be < 1000ms, was ${queryTime}ms", queryTime < 1000)
    }

    // ===== DATA INTEGRITY TESTS =====

    @Test
    fun dataIntegrity_maintainedAcrossOperations() = runBlocking {
        // Given - Complex data setup
        val location = ObjectLocation(
            latitude = 37.7749, longitude = -122.4194,
            accuracy = 5.0f, address = "San Francisco, CA",
            timestamp = System.currentTimeMillis()
        )

        val detection = DetectedObject(
            objectName = "laptop", confidence = 0.91f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/laptop.jpg", thumbnailPath = "/thumb_laptop.jpg",
            locationId = null
        )

        // When - Perform operations
        val locationId = detectionRepository.insertLocation(location)
        val detectionId = detectionRepository.insertDetection(detection.copy(locationId = locationId))
        
        val reminder = ObjectReminder(
            detected_object_id = detectionId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Laptop check", message = "Check your laptop",
            is_recurring = true, recurrence_type = RecurrenceType.WEEKLY,
            priority = ReminderPriority.MEDIUM
        )
        val reminderId = reminderRepository.insertReminder(reminder)

        // Then - Verify all relationships intact
        val detectionWithLocation = detectionRepository.getAllDetections().first()[0]
        assertNotNull(detectionWithLocation.location)
        assertEquals("San Francisco, CA", detectionWithLocation.location?.address)
        assertEquals(locationId, detectionWithLocation.detectedObject.locationId)

        val reminderForDetection = reminderRepository.getRemindersForObject(detectionId).first()[0]
        assertEquals(detectionId, reminderForDetection.detected_object_id)
        assertEquals("Laptop check", reminderForDetection.title)
        assertTrue(reminderForDetection.is_recurring)
        assertEquals(RecurrenceType.WEEKLY, reminderForDetection.recurrence_type)

        // Test updates don't break relationships
        reminderRepository.markAsTriggered(reminderId)
        val updatedReminder = reminderRepository.getReminderById(reminderId)
        assertTrue(updatedReminder!!.is_triggered)
        assertEquals(detectionId, updatedReminder.detected_object_id) // Relationship preserved
    }
}
