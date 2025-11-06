package com.smartfind.app.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.repository.DetectionRepository
import com.smartfind.app.domain.detector.ObjectDetector
import com.smartfind.app.domain.model.DetectionResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CameraViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockDetectionRepository: DetectionRepository
    private lateinit var mockObjectDetector: ObjectDetector
    private lateinit var mockAnalyticsManager: AnalyticsManager
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockApplication = mockk(relaxed = true)
        mockDetectionRepository = mockk(relaxed = true)
        mockObjectDetector = mockk(relaxed = true)
        mockAnalyticsManager = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        
        // Setup bitmap mocks
        every { mockBitmap.config } returns Bitmap.Config.ARGB_8888
        every { mockBitmap.width } returns 640
        every { mockBitmap.height } returns 480
        every { mockBitmap.getPixel(any(), any()) } returns 0xFF808080.toInt()
        every { mockBitmap.copy(any(), any()) } returns mockBitmap
        
        // Setup detector mocks
        coEvery { mockObjectDetector.initialize(any()) } returns true
        every { mockObjectDetector.isModelAvailable() } returns true
        every { mockObjectDetector.isGpuAccelerationEnabled() } returns true
        
        viewModel = CameraViewModel(
            application = mockApplication,
            repository = mockDetectionRepository,
            objectDetector = mockObjectDetector,
            analyticsManager = mockAnalyticsManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ===== INITIALIZATION TESTS =====

    @Test
    fun `initialization - model available - sets initialized to true`() = runTest {
        // Given
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.modelInitialized.observeForever(observer)
        
        // When
        advanceUntilIdle()
        
        // Then
        verify { observer.onChanged(true) }
        coVerify { mockObjectDetector.initialize(ObjectDetector.DEFAULT_CONFIDENCE_THRESHOLD) }
    }

    @Test
    fun `initialization - model unavailable - sets error message`() = runTest {
        // Given
        coEvery { mockObjectDetector.initialize(any()) } returns false
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        val initObserver = mockk<Observer<Boolean>>(relaxed = true)
        
        val newViewModel = CameraViewModel(
            application = mockApplication,
            repository = mockDetectionRepository,
            objectDetector = mockObjectDetector,
            analyticsManager = mockAnalyticsManager
        )
        
        newViewModel.errorMessage.observeForever(errorObserver)
        newViewModel.modelInitialized.observeForever(initObserver)
        
        // When
        advanceUntilIdle()
        
        // Then
        verify { initObserver.onChanged(false) }
        verify { errorObserver.onChanged(match { it?.contains("Failed to initialize") == true }) }
    }

    @Test
    fun `isModelAvailable - returns detector availability`() {
        // When
        val result = viewModel.isModelAvailable()
        
        // Then
        assertTrue(result)
        verify { mockObjectDetector.isModelAvailable() }
    }

    // ===== DETECTION TESTS =====

    @Test
    fun `detectObjects - successful detection - updates results and count`() = runTest {
        // Given
        val detectionResult = DetectionResult(
            label = "phone",
            confidence = 0.85f,
            boundingBox = mockk(),
            detectionTimeMs = System.currentTimeMillis()
        )
        coEvery { mockObjectDetector.detect(any()) } returns listOf(detectionResult)
        
        val resultsObserver = mockk<Observer<List<DetectionResult>>>(relaxed = true)
        val countObserver = mockk<Observer<Int>>(relaxed = true)
        viewModel.detectionResults.observeForever(resultsObserver)
        viewModel.detectionCount.observeForever(countObserver)
        
        // When
        val results = viewModel.detectObjects(mockBitmap)
        advanceUntilIdle()
        
        // Then
        assertEquals(1, results.size)
        assertEquals("phone", results[0].label)
        verify { resultsObserver.onChanged(match { it.size == 1 && it[0].label == "phone" }) }
        verify { countObserver.onChanged(1) }
    }

    @Test
    fun `detectObjects - multiple detections - increments count correctly`() = runTest {
        // Given
        val detection1 = DetectionResult("phone", 0.85f, mockk(), System.currentTimeMillis())
        val detection2 = DetectionResult("laptop", 0.90f, mockk(), System.currentTimeMillis())
        coEvery { mockObjectDetector.detect(any()) } returns listOf(detection1, detection2)
        
        val countObserver = mockk<Observer<Int>>(relaxed = true)
        viewModel.detectionCount.observeForever(countObserver)
        
        // When
        viewModel.detectObjects(mockBitmap)
        advanceUntilIdle()
        
        // Then
        verify { countObserver.onChanged(2) }
        verify { 
            mockAnalyticsManager.trackObjectDetection(
                objectName = "phone",
                confidence = 0.85f,
                detectionTimeMs = any(),
                isGpuEnabled = true
            )
        }
        verify { 
            mockAnalyticsManager.trackObjectDetection(
                objectName = "laptop",
                confidence = 0.90f,
                detectionTimeMs = any(),
                isGpuEnabled = true
            )
        }
    }

    @Test
    fun `detectObjects - no detections - returns empty list`() = runTest {
        // Given
        coEvery { mockObjectDetector.detect(any()) } returns emptyList()
        val resultsObserver = mockk<Observer<List<DetectionResult>>>(relaxed = true)
        viewModel.detectionResults.observeForever(resultsObserver)
        
        // When
        val results = viewModel.detectObjects(mockBitmap)
        advanceUntilIdle()
        
        // Then
        assertTrue(results.isEmpty())
        verify { resultsObserver.onChanged(emptyList()) }
    }

    @Test
    fun `detectObjects - exception thrown - returns empty list and tracks error`() = runTest {
        // Given
        val exception = RuntimeException("Detection failed")
        coEvery { mockObjectDetector.detect(any()) } throws exception
        
        // When
        val results = viewModel.detectObjects(mockBitmap)
        advanceUntilIdle()
        
        // Then
        assertTrue(results.isEmpty())
        verify { 
            mockAnalyticsManager.trackError(
                errorType = "detection_error",
                errorMessage = "Detection failed",
                screenName = "camera"
            )
        }
    }

    @Test
    fun `detectObjects - null bitmap config - handles gracefully`() = runTest {
        // Given
        val nullConfigBitmap = mockk<Bitmap>(relaxed = true)
        every { nullConfigBitmap.config } returns null
        every { nullConfigBitmap.width } returns 640
        every { nullConfigBitmap.height } returns 480
        every { nullConfigBitmap.copy(any(), any()) } returns mockBitmap
        coEvery { mockObjectDetector.detect(any()) } returns emptyList()
        
        // When
        val results = viewModel.detectObjects(nullConfigBitmap)
        advanceUntilIdle()
        
        // Then
        assertNotNull(results)
        assertTrue(results.isEmpty())
    }

    // ===== START/STOP DETECTION TESTS =====

    @Test
    fun `startDetection - sets isDetecting to true`() {
        // Given
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.isDetecting.observeForever(observer)
        
        // When
        viewModel.startDetection()
        
        // Then
        verify { observer.onChanged(true) }
    }

    @Test
    fun `stopDetection - sets isDetecting to false and clears results`() {
        // Given
        val detectingObserver = mockk<Observer<Boolean>>(relaxed = true)
        val resultsObserver = mockk<Observer<List<DetectionResult>>>(relaxed = true)
        viewModel.isDetecting.observeForever(detectingObserver)
        viewModel.detectionResults.observeForever(resultsObserver)
        
        viewModel.startDetection()
        
        // When
        viewModel.stopDetection()
        
        // Then
        verify { detectingObserver.onChanged(false) }
        verify { resultsObserver.onChanged(emptyList()) }
    }

    @Test
    fun `startDetection - called twice - does not start twice`() {
        // Given
        viewModel.startDetection()
        val initialValue = viewModel.isDetecting.value
        
        // When
        viewModel.startDetection()
        
        // Then
        assertEquals(true, initialValue)
        assertEquals(true, viewModel.isDetecting.value)
    }

    // ===== CONFIDENCE THRESHOLD TESTS =====

    @Test
    fun `updateConfidenceThreshold - valid threshold - updates and reinitializes`() = runTest {
        // Given
        val newThreshold = 0.75f
        
        // When
        viewModel.updateConfidenceThreshold(newThreshold)
        advanceUntilIdle()
        
        // Then
        assertEquals(newThreshold, viewModel.confidenceThreshold)
        verify { mockObjectDetector.updateConfidenceThreshold(newThreshold) }
        coVerify(atLeast = 2) { mockObjectDetector.initialize(any()) }
    }

    @Test
    fun `updateConfidenceThreshold - extreme values - handles correctly`() = runTest {
        // When/Then - minimum
        viewModel.updateConfidenceThreshold(ObjectDetector.MIN_CONFIDENCE_THRESHOLD)
        advanceUntilIdle()
        assertEquals(ObjectDetector.MIN_CONFIDENCE_THRESHOLD, viewModel.confidenceThreshold)
        
        // When/Then - maximum
        viewModel.updateConfidenceThreshold(ObjectDetector.MAX_CONFIDENCE_THRESHOLD)
        advanceUntilIdle()
        assertEquals(ObjectDetector.MAX_CONFIDENCE_THRESHOLD, viewModel.confidenceThreshold)
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    fun `clearError - sets error message to null`() {
        // When
        viewModel.clearError()
        
        // Then
        assertNull(viewModel.errorMessage.value)
    }

    // ===== CLEANUP TESTS =====

    @Test
    fun `stopDetection - stops detection properly`() = runTest {
        // Given
        viewModel.startDetection()
        assertTrue(viewModel.isDetecting.value == true)
        
        // When
        viewModel.stopDetection()
        
        // Then
        assertEquals(false, viewModel.isDetecting.value)
    }
}
