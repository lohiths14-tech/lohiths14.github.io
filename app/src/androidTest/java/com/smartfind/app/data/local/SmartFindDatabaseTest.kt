package com.smartfind.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for SmartFindDatabase
 * Tests Room database operations on real Android device/emulator
 */
@RunWith(AndroidJUnit4::class)
class SmartFindDatabaseTest {

    private lateinit var database: SmartFindDatabase
    private lateinit var detectedObjectDao: com.smartfind.app.data.local.dao.DetectedObjectDao
    private lateinit var locationDao: com.smartfind.app.data.local.dao.ObjectLocationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmartFindDatabase::class.java
        ).build()
        detectedObjectDao = database.detectedObjectDao()
        locationDao = database.objectLocationDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveDetection() = runBlocking {
        // Create test detection
        val detection = DetectedObject(
            id = 0,
            objectName = "phone",
            confidence = 0.85f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/test/path.jpg",
            thumbnailPath = "/test/thumb.jpg",
            locationId = null
        )

        // Insert
        val id = detectedObjectDao.insert(detection)
        assertTrue("ID should be positive", id > 0)

        // Retrieve
        val allDetections = detectedObjectDao.getAllWithLocation().first()
        assertEquals(1, allDetections.size)
        assertEquals("phone", allDetections[0].detectedObject.objectName)
        assertEquals(0.85f, allDetections[0].detectedObject.confidence, 0.001f)
    }

    @Test
    fun testInsertLocationAndLinkToDetection() = runBlocking {
        // Create location
        val location = ObjectLocation(
            id = 0,
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 10.0f,
            address = "San Francisco, CA",
            timestamp = System.currentTimeMillis()
        )

        val locationId = locationDao.insert(location)
        assertTrue("Location ID should be positive", locationId > 0)

        // Create detection with location
        val detection = DetectedObject(
            id = 0,
            objectName = "laptop",
            confidence = 0.92f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/test/laptop.jpg",
            thumbnailPath = "/test/laptop_thumb.jpg",
            locationId = locationId
        )

        val detectionId = detectedObjectDao.insert(detection)
        assertTrue("Detection ID should be positive", detectionId > 0)

        // Retrieve with location
        val detectionWithLocation = detectedObjectDao.getByIdWithLocation(detectionId)
        assertNotNull(detectionWithLocation)
        assertEquals("laptop", detectionWithLocation?.detectedObject?.objectName)
        assertNotNull(detectionWithLocation?.location)
        assertEquals("San Francisco, CA", detectionWithLocation?.location?.address)
    }

    @Test
    fun testSearchByObjectName() = runBlocking {
        // Insert multiple detections
        val phone = DetectedObject(0, "phone", 0.8f, System.currentTimeMillis(), "/phone.jpg", null, null)
        val laptop = DetectedObject(0, "laptop", 0.9f, System.currentTimeMillis(), "/laptop.jpg", null, null)
        val phoneCase = DetectedObject(0, "phone case", 0.7f, System.currentTimeMillis(), "/case.jpg", null, null)

        detectedObjectDao.insert(phone)
        detectedObjectDao.insert(laptop)
        detectedObjectDao.insert(phoneCase)

        // Search for "phone"
        val results = detectedObjectDao.searchByName("phone").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.objectName == "phone" })
        assertTrue(results.any { it.objectName == "phone case" })
    }

    @Test
    fun testDeleteOldDetections() = runBlocking {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
        val fiftyDaysAgo = now - (50 * 24 * 60 * 60 * 1000L)

        // Insert old and new detections
        val oldDetection = DetectedObject(0, "old_phone", 0.8f, fiftyDaysAgo, "/old.jpg", null, null)
        val newDetection = DetectedObject(0, "new_phone", 0.9f, now, "/new.jpg", null, null)

        detectedObjectDao.insert(oldDetection)
        detectedObjectDao.insert(newDetection)

        // Delete detections older than 30 days
        val deletedCount = detectedObjectDao.deleteOlderThan(thirtyDaysAgo)
        assertEquals(1, deletedCount)

        // Verify only new detection remains
        val remaining = detectedObjectDao.getAllWithLocation().first()
        assertEquals(1, remaining.size)
        assertEquals("new_phone", remaining[0].detectedObject.objectName)
    }

    @Test
    fun testGetDistinctObjectNames() = runBlocking {
        // Insert multiple detections with duplicate names
        detectedObjectDao.insert(DetectedObject(0, "phone", 0.8f, System.currentTimeMillis(), "/1.jpg", null, null))
        detectedObjectDao.insert(DetectedObject(0, "laptop", 0.9f, System.currentTimeMillis(), "/2.jpg", null, null))
        detectedObjectDao.insert(DetectedObject(0, "phone", 0.85f, System.currentTimeMillis(), "/3.jpg", null, null))
        detectedObjectDao.insert(DetectedObject(0, "keys", 0.7f, System.currentTimeMillis(), "/4.jpg", null, null))

        val distinctNames = detectedObjectDao.getDistinctObjectNames().first()
        assertEquals(3, distinctNames.size)
        assertTrue(distinctNames.contains("phone"))
        assertTrue(distinctNames.contains("laptop"))
        assertTrue(distinctNames.contains("keys"))
    }

    @Test
    fun testDeleteDetectionById() = runBlocking {
        val detection = DetectedObject(0, "wallet", 0.75f, System.currentTimeMillis(), "/wallet.jpg", null, null)
        val id = detectedObjectDao.insert(detection)

        // Verify inserted
        val allBefore = detectedObjectDao.getAllWithLocation().first()
        assertEquals(1, allBefore.size)

        // Delete by ID
        detectedObjectDao.deleteById(id)

        // Verify deleted
        val allAfter = detectedObjectDao.getAllWithLocation().first()
        assertEquals(0, allAfter.size)
    }

    @Test
    fun testGetDetectionCount() = runBlocking {
        // Initially should be 0
        assertEquals(0, detectedObjectDao.getCount())

        // Insert detections
        detectedObjectDao.insert(DetectedObject(0, "phone", 0.8f, System.currentTimeMillis(), "/1.jpg", null, null))
        detectedObjectDao.insert(DetectedObject(0, "laptop", 0.9f, System.currentTimeMillis(), "/2.jpg", null, null))
        detectedObjectDao.insert(DetectedObject(0, "keys", 0.7f, System.currentTimeMillis(), "/3.jpg", null, null))

        // Should be 3
        assertEquals(3, detectedObjectDao.getCount())
    }
}
