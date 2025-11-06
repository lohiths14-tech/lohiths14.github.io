package com.smartfind.app.presentation.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.smartfind.app.domain.model.DetectionResult
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView
) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_AUTO
    
    private lateinit var cameraExecutor: ExecutorService
    
    var onDetectionResult: ((List<DetectionResult>) -> Unit)? = null
    var onCameraError: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "CameraManager"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
    
    fun initialize() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        analyzer: ImageAnalysis.Analyzer
    ) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            bindCameraUseCases(lifecycleOwner, analyzer)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            onCameraError?.invoke("Failed to start camera: ${e.message}")
        }
    }
    
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        analyzer: ImageAnalysis.Analyzer
    ) {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera provider is not initialized")
        
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        // Preview use case
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        
        preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis use case
        imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, analyzer)
            }
        
        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        // Unbind all use cases before rebinding
        cameraProvider.unbindAll()
        
        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
                imageCapture
            )
            
            // Enable tap to focus
            setupTapToFocus()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera use cases", e)
            onCameraError?.invoke("Failed to bind camera: ${e.message}")
        }
    }
    
    private fun setupTapToFocus() {
        previewView.setOnTouchListener { _, event ->
            val meteringPointFactory = previewView.meteringPointFactory
            val focusPoint = meteringPointFactory.createPoint(event.x, event.y)
            
            val action = FocusMeteringAction.Builder(focusPoint)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            camera?.cameraControl?.startFocusAndMetering(action)
            true
        }
    }
    
    fun switchCamera(lifecycleOwner: LifecycleOwner, analyzer: ImageAnalysis.Analyzer) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        
        bindCameraUseCases(lifecycleOwner, analyzer)
    }
    
    fun setFlashMode(mode: Int) {
        flashMode = mode
        imageCapture?.flashMode = mode
    }
    
    fun getFlashMode(): Int = flashMode
    
    fun isFrontCamera(): Boolean = lensFacing == CameraSelector.LENS_FACING_FRONT
    
    fun captureImage(onImageCaptured: (Bitmap?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "Image capture use case not initialized")
            onImageCaptured(null)
            return
        }
        
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    onImageCaptured(bitmap)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error capturing image", exception)
                    onImageCaptured(null)
                }
            }
        )
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
    
    fun hasCamera(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
    
    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
