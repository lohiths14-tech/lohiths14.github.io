package com.smartfind.app.data.repository

import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.dao.ObjectLocationDao
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for DetectionRepository
 * Tests CRUD operations and data flow
 */
class DetectionRepositoryTest {

    @Mock
    private lateinit var mockDetectedObjectDao: DetectedObjectDao

    @Mock
    private lateinit var mockLocationDao: ObjectLocationDao

    private lateinit var repository: DetectionRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = DetectionRepositoryImpl(mockDetectedObjectDao, mockLocationDao)
    }

    @Test
    fun `test insertDetection returns ID`() = runBlocking {
        val testObject = DetectedObject(
            id = 0,
            objectName = "phone",
            confidence = 0.85f,
            timestamp = System.currentTimeMillis(),
            imagePath = "/test/path.jpg",
            thumbnailPath = "/test/thumb.jpg",
            locationId = null
        )

        `when`(mockDetectedObjectDao.insert(testObject)).thenReturn(1L)

        val resultId = repository.insertDetection(testObject)

        assertEquals(1L, resultId)
        verify(mockDetectedObjectDao).insert(testObject)
    }

    @Test
    fun `test insertLocation returns ID`() = runBlocking {
        val testLocation = ObjectLocation(
            id = 0,
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 10.0f,
            address = "San Francisco, CA",
            timestamp = System.currentTimeMillis()
        )

        `when`(mockLocationDao.insert(testLocation)).thenReturn(1L)

        val resultId = repository.insertLocation(testLocation)

        assertEquals(1L, resultId)
        verify(mockLocationDao).insert(testLocation)
    }

    @Test
    fun `test getDetectionCount returns correct count`() = runBlocking {
        `when`(mockDetectedObjectDao.getCount()).thenReturn(42)

        val count = repository.getDetectionCount()

        assertEquals(42, count)
        verify(mockDetectedObjectDao).getCount()
    }

    @Test
    fun `test deleteOldDetections with timestamp`() = runBlocking {
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        `when`(mockDetectedObjectDao.deleteOlderThan(cutoffTime)).thenReturn(15)

        val deletedCount = repository.deleteOldDetections(cutoffTime)

        assertEquals(15, deletedCount)
        verify(mockDetectedObjectDao).deleteOlderThan(cutoffTime)
    }

    @Test
    fun `test searchDetections returns flow`() = runBlocking {
        val searchQuery = "phone"
        val expectedFlow = flowOf(emptyList<DetectedObject>())
        
        `when`(mockDetectedObjectDao.searchByName(searchQuery)).thenReturn(expectedFlow)

        val result = repository.searchDetections(searchQuery)

        assertNotNull(result)
        verify(mockDetectedObjectDao).searchByName(searchQuery)
    }
}
