package com.smartfind.app.presentation.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.data.local.entity.ObjectLocation
import com.smartfind.app.data.repository.DetectionRepository
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
import java.io.File

@ExperimentalCoroutinesApi
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HistoryViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockRepository: DetectionRepository
    private lateinit var mockAnalytics: AnalyticsManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockAnalytics = mockk(relaxed = true)
        
        // Mock application context methods
        every { mockApplication.getExternalFilesDir(any()) } returns File("/mock/path")
        
        viewModel = HistoryViewModel(
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

    // ===== SEARCH FUNCTIONALITY TESTS =====

    @Test
    fun `searchDetections - empty query - returns all results`() = runTest {
        // Given
        val allDetections = listOf(
            createMockDetectionWithLocation("phone"),
            createMockDetectionWithLocation("laptop")
        )
        every { mockRepository.getAllDetections() } returns flowOf(allDetections)
        val observer = mockk<Observer<List<DetectedObjectWithLocation>>>(relaxed = true)
        viewModel.filteredDetections.observeForever(observer)
        
        // When
        viewModel.searchDetections("")
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(allDetections) }
    }

    @Test
    fun `searchDetections - case insensitive - finds matches`() = runTest {
        // Given
        val phoneDetection = createMockDetectionWithLocation("Phone")
        val laptopDetection = createMockDetectionWithLocation("laptop")
        val allDetections = listOf(phoneDetection, laptopDetection)
        
        every { mockRepository.getAllDetections() } returns flowOf(allDetections)
        val observer = mockk<Observer<List<DetectedObjectWithLocation>>>(relaxed = true)
        viewModel.filteredDetections.observeForever(observer)
        
        // When
        viewModel.searchDetections("phone")
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(match { it.size == 1 && it[0].detectedObject.objectName == "Phone" }) }
    }

    @Test
    fun `searchDetections - location address search - finds matches`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("phone", "New York")
        val allDetections = listOf(detection)
        
        every { mockRepository.getAllDetections() } returns flowOf(allDetections)
        val observer = mockk<Observer<List<DetectedObjectWithLocation>>>(relaxed = true)
        viewModel.filteredDetections.observeForever(observer)
        
        // When
        viewModel.searchDetections("york")
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(match { it.size == 1 }) }
    }

    @Test
    fun `searchDetections - no matches - returns empty list`() = runTest {
        // Given
        val allDetections = listOf(createMockDetectionWithLocation("phone"))
        every { mockRepository.getAllDetections() } returns flowOf(allDetections)
        val observer = mockk<Observer<List<DetectedObjectWithLocation>>>(relaxed = true)
        viewModel.filteredDetections.observeForever(observer)
        
        // When
        viewModel.searchDetections("nonexistent")
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(emptyList()) }
    }

    @Test
    fun `searchDetections - special characters - handles correctly`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("test-object_123")
        val allDetections = listOf(detection)
        every { mockRepository.getAllDetections() } returns flowOf(allDetections)
        val observer = mockk<Observer<List<DetectedObjectWithLocation>>>(relaxed = true)
        viewModel.filteredDetections.observeForever(observer)
        
        // When
        viewModel.searchDetections("test-object")
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(match { it.size == 1 }) }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Test
    fun `deleteDetection - successful - removes from repository`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("phone")
        coEvery { mockRepository.deleteDetection(any()) } returns Unit
        
        // When
        viewModel.deleteDetection(detection)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.deleteDetection(detection.detectedObject) }
    }

    @Test
    fun `deleteDetection - repository error - sets error message`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("phone")
        val exception = RuntimeException("Database error")
        coEvery { mockRepository.deleteDetection(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.deleteDetection(detection)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to delete detection") == true }) }
    }

    @Test
    fun `restoreDetection - with location - restores both objects`() = runTest {
        // Given
        val location = ObjectLocation(1L, 40.7128, -74.0060, 10.0f, "New York", System.currentTimeMillis())
        val detection = createMockDetectionWithLocation("phone", location = location)
        coEvery { mockRepository.insertDetection(any()) } returns 1L
        coEvery { mockRepository.insertLocation(any()) } returns 1L
        
        // When
        viewModel.restoreDetection(detection)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertDetection(detection.detectedObject) }
        coVerify { mockRepository.insertLocation(location) }
    }

    @Test
    fun `restoreDetection - without location - restores only detection`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("phone", location = null)
        coEvery { mockRepository.insertDetection(any()) } returns 1L
        
        // When
        viewModel.restoreDetection(detection)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.insertDetection(detection.detectedObject) }
        coVerify(exactly = 0) { mockRepository.insertLocation(any()) }
    }

    @Test
    fun `restoreDetection - database error - sets error message`() = runTest {
        // Given
        val detection = createMockDetectionWithLocation("phone")
        val exception = RuntimeException("Insert failed")
        coEvery { mockRepository.insertDetection(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.restoreDetection(detection)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to restore detection") == true }) }
    }

    // ===== BULK OPERATIONS TESTS =====

    @Test
    fun `deleteOldDetections - successful - deletes files and database entries`() = runTest {
        // Given
        val daysOld = 30
        val oldDetections = listOf(
            createMockDetectionWithLocation("phone", timestamp = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000))
        )
        every { mockRepository.getAllDetections() } returns flowOf(oldDetections)
        coEvery { mockRepository.deleteOldDetections(any()) } returns 1
        coEvery { mockRepository.deleteOldLocations(any()) } returns 0
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.deleteOldDetections(daysOld)
        advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.deleteOldDetections(any()) }
        coVerify { mockRepository.deleteOldLocations(any()) }
        verify { errorObserver.onChanged(match { it?.contains("Deleted 1 old detections") == true }) }
    }

    @Test
    fun `deleteOldDetections - no old detections - completes successfully`() = runTest {
        // Given
        val daysOld = 30
        every { mockRepository.getAllDetections() } returns flowOf(emptyList())
        coEvery { mockRepository.deleteOldDetections(any()) } returns 0
        coEvery { mockRepository.deleteOldLocations(any()) } returns 0
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.deleteOldDetections(daysOld)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged("Deleted 0 old detections") }
    }

    @Test
    fun `deleteOldDetections - database error - sets error message`() = runTest {
        // Given
        val daysOld = 30
        val exception = RuntimeException("Delete failed")
        every { mockRepository.getAllDetections() } returns flowOf(emptyList())
        coEvery { mockRepository.deleteOldDetections(any()) } throws exception
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        viewModel.deleteOldDetections(daysOld)
        advanceUntilIdle()
        
        // Then
        verify { errorObserver.onChanged(match { it?.contains("Failed to delete old detections") == true }) }
    }

    // ===== EXPORT FUNCTIONALITY TESTS =====

    @Test
    fun `exportToCsv - with data - creates file successfully`() = runTest {
        // Given
        val detections = listOf(
            createMockDetectionWithLocation("phone", "New York"),
            createMockDetectionWithLocation("laptop", null)
        )
        every { mockRepository.getAllDetections() } returns flowOf(detections)
        
        // When
        val result = viewModel.exportToCsv()
        
        // Then
        assertNotNull(result)
        assertTrue(result!!.name.startsWith("smartfind_export_"))
        assertTrue(result.name.endsWith(".csv"))
    }

    @Test
    fun `exportToCsv - empty data - creates header only file`() = runTest {
        // Given
        every { mockRepository.getAllDetections() } returns flowOf(emptyList())
        
        // When
        val result = viewModel.exportToCsv()
        
        // Then
        assertNotNull(result)
    }

    @Test
    fun `exportToCsv - file creation error - returns null and sets error`() = runTest {
        // Given
        every { mockApplication.getExternalFilesDir(any()) } returns null
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // When
        val result = viewModel.exportToCsv()
        
        // Then
        assertNull(result)
        verify { errorObserver.onChanged(match { it?.contains("Failed to export CSV") == true }) }
    }

    // ===== STORAGE INFO TESTS =====

    @Test
    fun `getStorageInfo - returns size and formatted string`() {
        // When
        val (size, formatted) = viewModel.getStorageInfo()
        
        // Then
        assertTrue(size >= 0)
        assertNotNull(formatted)
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    fun `clearError - sets error message to null`() {
        // When
        viewModel.clearError()
        
        // Then
        assertNull(viewModel.errorMessage.value)
    }

    // ===== LOADING STATE TESTS =====

    @Test
    fun `loading state - initially false`() {
        // Then
        assertEquals(false, viewModel.isLoading.value)
    }

    // ===== HELPER METHODS =====

    private fun createMockDetectionWithLocation(
        objectName: String,
        address: String? = null,
        location: ObjectLocation? = null,
        timestamp: Long = System.currentTimeMillis()
    ): DetectedObjectWithLocation {
        val detectedObject = DetectedObject(
            id = 1L,
            objectName = objectName,
            confidence = 0.85f,
            timestamp = timestamp,
            imagePath = "/mock/path/$objectName.jpg",
            thumbnailPath = "/mock/path/thumb_$objectName.jpg",
            locationId = location?.id
        )
        
        val objectLocation = location ?: address?.let {
            ObjectLocation(
                id = 1L,
                latitude = 40.7128,
                longitude = -74.0060,
                accuracy = 10.0f,
                address = it,
                timestamp = timestamp
            )
        }
        
        return DetectedObjectWithLocation(detectedObject, objectLocation)
    }
}
