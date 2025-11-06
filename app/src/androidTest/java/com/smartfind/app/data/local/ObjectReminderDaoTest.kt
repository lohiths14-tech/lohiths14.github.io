package com.smartfind.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.ObjectReminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for ObjectReminderDao
 * Tests reminder database operations on real Android device/emulator
 */
@RunWith(AndroidJUnit4::class)
class ObjectReminderDaoTest {

    private lateinit var database: SmartFindDatabase
    private lateinit var reminderDao: com.smartfind.app.data.local.dao.ObjectReminderDao
    private lateinit var objectDao: com.smartfind.app.data.local.dao.DetectedObjectDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmartFindDatabase::class.java
        ).build()
        reminderDao = database.objectReminderDao()
        objectDao = database.detectedObjectDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveReminder() = runBlocking {
        // First create a detected object
        val detectedObject = DetectedObject(
            id = 0,
            objectName = "phone",
            confidence = 0.85f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/test/phone.jpg",
            thumbnailPath = "/test/phone_thumb.jpg",
            locationId = null
        )
        val objectId = objectDao.insert(detectedObject)

        // Create reminder
        val reminderTime = System.currentTimeMillis() + 3600000 // 1 hour from now
        val reminder = ObjectReminder(
            id = 0,
            detected_object_id = objectId,
            reminder_time = reminderTime,
            title = "Remember: Phone",
            message = "Don't forget your phone",
            is_triggered = false,
            is_cancelled = false
        )

        // Insert
        val id = reminderDao.insert(reminder)
        assertTrue("ID should be positive", id > 0)

        // Retrieve
        val allReminders = reminderDao.getAllReminders().first()
        assertEquals(1, allReminders.size)
        assertEquals("Remember: Phone", allReminders[0].title)
        assertEquals("Don't forget your phone", allReminders[0].message)
        assertFalse(allReminders[0].is_triggered)
        assertFalse(allReminders[0].is_cancelled)
    }

    @Test
    fun testGetPendingReminders() = runBlocking {
        // Create object
        val objectId = objectDao.insert(
            DetectedObject(0, "keys", 0.9f, System.currentTimeMillis(), "/keys.jpg", null, null)
        )

        val now = System.currentTimeMillis()
        
        // Create past reminder (should be pending)
        val pastReminder = ObjectReminder(
            id = 0,
            detected_object_id = objectId,
            reminder_time = now - 1000,
            title = "Past Reminder",
            message = "This is overdue",
            is_triggered = false,
            is_cancelled = false
        )
        reminderDao.insert(pastReminder)

        // Create future reminder (should not be pending)
        val futureReminder = ObjectReminder(
            id = 0,
            detected_object_id = objectId,
            reminder_time = now + 3600000,
            title = "Future Reminder",
            message = "Not yet due",
            is_triggered = false,
            is_cancelled = false
        )
        reminderDao.insert(futureReminder)

        // Get pending reminders
        val pending = reminderDao.getPendingReminders(now)
        assertEquals(1, pending.size)
        assertEquals("Past Reminder", pending[0].title)
    }

    @Test
    fun testMarkAsTriggered() = runBlocking {
        val objectId = objectDao.insert(
            DetectedObject(0, "wallet", 0.8f, System.currentTimeMillis(), "/wallet.jpg", null, null)
        )

        val reminder = ObjectReminder(
            id = 0,
            detected_object_id = objectId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Test",
            message = "Test message",
            is_triggered = false,
            is_cancelled = false
        )
        val reminderId = reminderDao.insert(reminder)

        // Mark as triggered
        reminderDao.markAsTriggered(reminderId)

        // Verify
        val retrieved = reminderDao.getReminderById(reminderId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.is_triggered)
    }

    @Test
    fun testMarkAsCancelled() = runBlocking {
        val objectId = objectDao.insert(
            DetectedObject(0, "laptop", 0.9f, System.currentTimeMillis(), "/laptop.jpg", null, null)
        )

        val reminder = ObjectReminder(
            id = 0,
            detected_object_id = objectId,
            reminder_time = System.currentTimeMillis() + 3600000,
            title = "Test",
            message = "Test message",
            is_triggered = false,
            is_cancelled = false
        )
        val reminderId = reminderDao.insert(reminder)

        // Mark as cancelled
        reminderDao.markAsCancelled(reminderId)

        // Verify
        val retrieved = reminderDao.getReminderById(reminderId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.is_cancelled)
    }

    @Test
    fun testGetActiveReminders() = runBlocking {
        val objectId = objectDao.insert(
            DetectedObject(0, "phone", 0.85f, System.currentTimeMillis(), "/phone.jpg", null, null)
        )

        // Active reminder
        reminderDao.insert(ObjectReminder(
            0, objectId, System.currentTimeMillis() + 3600000,
            "Active", "Message", false, false
        ))

        // Triggered reminder (not active)
        reminderDao.insert(ObjectReminder(
            0, objectId, System.currentTimeMillis() + 3600000,
            "Triggered", "Message", true, false
        ))

        // Cancelled reminder (not active)
        reminderDao.insert(ObjectReminder(
            0, objectId, System.currentTimeMillis() + 3600000,
            "Cancelled", "Message", false, true
        ))

        val activeReminders = reminderDao.getActiveReminders().first()
        assertEquals(1, activeReminders.size)
        assertEquals("Active", activeReminders[0].title)
    }

    @Test
    fun testGetActiveReminderCount() = runBlocking {
        val objectId = objectDao.insert(
            DetectedObject(0, "keys", 0.75f, System.currentTimeMillis(), "/keys.jpg", null, null)
        )

        // Initially 0
        assertEquals(0, reminderDao.getActiveReminderCount())

        // Add 3 active reminders
        reminderDao.insert(ObjectReminder(0, objectId, System.currentTimeMillis() + 3600000, "1", "M", false, false))
        reminderDao.insert(ObjectReminder(0, objectId, System.currentTimeMillis() + 3600000, "2", "M", false, false))
        reminderDao.insert(ObjectReminder(0, objectId, System.currentTimeMillis() + 3600000, "3", "M", false, false))

        assertEquals(3, reminderDao.getActiveReminderCount())
    }

    @Test
    fun testDeleteOldReminders() = runBlocking {
        val objectId = objectDao.insert(
            DetectedObject(0, "cup", 0.7f, System.currentTimeMillis(), "/cup.jpg", null, null)
        )

        val now = System.currentTimeMillis()
        val fortyDaysAgo = now - (40 * 24 * 60 * 60 * 1000L)
        val twentyDaysAgo = now - (20 * 24 * 60 * 60 * 1000L)

        // Old reminder
        reminderDao.insert(ObjectReminder(
            0, objectId, fortyDaysAgo, "Old", "Message", true, false
        ))

        // Recent reminder
        reminderDao.insert(ObjectReminder(
            0, objectId, twentyDaysAgo, "Recent", "Message", false, false
        ))

        // Delete reminders older than 30 days
        val cutoff = now - (30 * 24 * 60 * 60 * 1000L)
        val deletedCount = reminderDao.deleteOlderThan(cutoff)
        
        assertEquals(1, deletedCount)

        // Verify only recent remains
        val remaining = reminderDao.getAllReminders().first()
        assertEquals(1, remaining.size)
        assertEquals("Recent", remaining[0].title)
    }
}
