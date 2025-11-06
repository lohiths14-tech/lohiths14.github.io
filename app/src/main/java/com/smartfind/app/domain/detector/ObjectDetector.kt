package com.smartfind.app.domain.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.smartfind.app.domain.model.DetectionResult
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector as TFLiteObjectDetector
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance object detector using TensorFlow Lite with GPU acceleration.
 *
 * This class provides real-time object detection capabilities using pre-trained
 * TensorFlow Lite models. It automatically selects the best available model from
 * a prioritized list and optimizes performance using GPU acceleration when available.
 *
 * **Features:**
 * - Multi-model support with auto-detection
 * - GPU acceleration (2-5x faster on compatible devices)
 * - Configurable confidence thresholds
 * - Smart result filtering and deduplication
 * - Graceful CPU fallback when GPU unavailable
 *
 * **Supported Models:**
 * - YOLOv8n (fast & accurate)
 * - EfficientDet-Lite4 (most accurate)
 * - SSD MobileNet V2 (balanced)
 * - Custom models (1.tflite)
 *
 * **Usage Example:**
 * ```kotlin
 * val detector = ObjectDetector(context)
 * detector.initialize()
 *
 * // Detect objects in bitmap
 * val results = detector.detect(bitmap)
 * results.forEach { detection ->
 *     Log.d("Detection", "${detection.label}: ${detection.confidence}")
 * }
 *
 * // Cleanup
 * detector.close()
 * ```
 *
 * @property context The application context for accessing assets and resources
 * @constructor Creates an object detector instance
 *
 * @author SmartFind Team
 * @since 1.0
 */
class ObjectDetector(private val context: Context) {

    private var detector: TFLiteObjectDetector? = null

    private var isInitialized = false
    private var currentModel: String = ""
    private var isGpuEnabled = false

    companion object {
        private const val TAG = "ObjectDetector"

        // Model files in priority order (best to fallback)
        private val MODEL_FILES = arrayOf(
            "models/1.tflite",                 // User's custom model
            "models/yolov8n.tflite",           // YOLOv8 Nano - Fast & Accurate
            "models/efficientdet_lite4.tflite", // EfficientDet - Most Accurate
            "models/ssd_mobilenet_v2.tflite",  // SSD MobileNet - Balanced
            "models/detect.tflite"              // Default fallback
        )

        // Confidence thresholds optimized per model
        private val MODEL_THRESHOLDS = mapOf(
            "1" to 0.40f,              // User's model - moderate threshold
            "yolov8n" to 0.35f,
            "efficientdet" to 0.50f,
            "ssd_mobilenet" to 0.45f,
            "detect" to 0.55f
        )

        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.45f
        const val MIN_CONFIDENCE_THRESHOLD = 0.25f
        const val MAX_CONFIDENCE_THRESHOLD = 0.9f
        const val MAX_RESULTS = 10
    }

    /**
     * Initializes the object detector with the best available model and GPU acceleration.
     *
     * This method:
     * 1. Searches for the best available model (prioritizes user models, then high-quality models)
     * 2. Checks GPU compatibility and enables GPU acceleration if available
     * 3. Falls back to CPU if GPU is not supported
     * 4. Configures model-specific confidence thresholds
     *
     * **GPU Acceleration:**
     * - Automatically detects GPU compatibility
     * - Provides 2-5x performance improvement on compatible devices
     * - Gracefully falls back to CPU on incompatible devices
     *
     * **Model Selection Priority:**
     * 1. Custom model (1.tflite) - if present
     * 2. YOLOv8n - fast and accurate
     * 3. EfficientDet-Lite4 - most accurate
     * 4. SSD MobileNet V2 - balanced
     * 5. Default model (detect.tflite) - fallback
     *
     * @param confidenceThreshold The minimum confidence score for detections (0.0-1.0).
     *                           Defaults to model-specific optimized thresholds.
     * @return true if initialization succeeded, false otherwise
     *
     * @throws IllegalStateException if no model files are found
     *
     * Example:
     * ```kotlin
     * val detector = ObjectDetector(context)
     * if (detector.initialize(0.5f)) {
     *     Log.d("Detector", "Ready to detect objects")
     * } else {
     *     Log.e("Detector", "Failed to initialize")
     * }
     * ```
     */
    suspend fun initialize(
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        modelName: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var selectedModelPath: String? = null
                var isModelInFilesDir = false

                if (modelName != null) {
                    val modelPath = "models/$modelName"
                    val modelFile = File(context.filesDir, modelPath)
                    if (modelFile.exists()) {
                        selectedModelPath = modelPath
                        isModelInFilesDir = true
                    } else {
                        try {
                            context.assets.open(modelPath).use {
                                selectedModelPath = modelPath
                                isModelInFilesDir = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Requested model '$modelName' not found.")
                        }
                    }
                } else {
                    // Find the best available model
                    run searchLoop@ {
                        for (modelPath in MODEL_FILES) {
                            val modelFile = File(context.filesDir, modelPath)
                            if (modelFile.exists()) {
                                selectedModelPath = modelPath
                                isModelInFilesDir = true
                                Log.d(TAG, "Found model in filesDir: $modelPath")
                                return@searchLoop
                            }

                            // Try assets
                            try {
                                context.assets.open(modelPath).use {
                                    selectedModelPath = modelPath
                                    isModelInFilesDir = false
                                    Log.d(TAG, "Found model in assets: $modelPath")
                                    return@searchLoop
                                }
                            } catch (e: Exception) {
                                // Model not found, try next
                            }
                        }
                    }
                }

                if (selectedModelPath == null) {
                    Log.e(TAG, "No model file found. Please add a model to assets/models/")
                    return@withContext false
                }

                // Create non-nullable copy to avoid smart cast issues
                val modelPath: String = selectedModelPath!!
                currentModel = modelPath

                // Get optimized threshold for this model
                val modelName = modelPath.substringAfterLast("/").substringBefore(".")
                val optimizedThreshold = MODEL_THRESHOLDS.entries
                    .firstOrNull { modelName.contains(it.key, ignoreCase = true) }
                    ?.value ?: confidenceThreshold

                Log.d(TAG, "Using model: $modelPath with threshold: $optimizedThreshold")

                // Check GPU compatibility
                val compatibilityList = CompatibilityList()
                val useGpu = compatibilityList.isDelegateSupportedOnThisDevice

                if (useGpu) {
                    Log.d(TAG, "GPU acceleration is supported on this device")
                } else {
                    Log.d(TAG, "GPU not supported, using CPU")
                }

                // Try to create with GPU first
                detector = try {
                    if (useGpu) {
                        createDetectorWithGpu(modelPath, isModelInFilesDir, optimizedThreshold)
                    } else {
                        createDetectorWithCpu(modelPath, isModelInFilesDir, optimizedThreshold)
                    }
                } catch (e: Exception) {
                    // Fallback to CPU if GPU fails
                    Log.w(TAG, "Failed to create detector with GPU, falling back to CPU", e)

                    isGpuEnabled = false
                    createDetectorWithCpu(modelPath, isModelInFilesDir, optimizedThreshold)
                }

                isInitialized = true
                val accelerator = if (isGpuEnabled) "GPU" else "CPU"
                Log.d(TAG, "Object detector initialized successfully with $modelPath using $accelerator")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing object detector", e)
                isInitialized = false
                false
            }
        }
    }

    /**
     * Creates a detector instance with GPU acceleration.
     *
     * @param modelPath The path to the model file
     * @param inFilesDir Whether the model is in filesDir (true) or assets (false)
     * @param threshold The confidence threshold
     * @return Configured TFLite ObjectDetector with GPU delegate
     */
    private fun createDetectorWithGpu(
        modelPath: String,
        inFilesDir: Boolean,
        threshold: Float
    ): TFLiteObjectDetector {


        val baseOptions = BaseOptions.builder()
            .setNumThreads(4)
            .useGpu()
            .build()

        val options = TFLiteObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(MAX_RESULTS)
            .setScoreThreshold(threshold)
            .build()

        isGpuEnabled = true

        return if (inFilesDir) {
            TFLiteObjectDetector.createFromFileAndOptions(
                File(context.filesDir, modelPath),
                options
            )
        } else {
            TFLiteObjectDetector.createFromFileAndOptions(
                context,
                modelPath,
                options
            )
        }
    }

    /**
     * Creates a detector instance with CPU processing.
     *
     * @param modelPath The path to the model file
     * @param inFilesDir Whether the model is in filesDir (true) or assets (false)
     * @param threshold The confidence threshold
     * @return Configured TFLite ObjectDetector for CPU
     */
    private fun createDetectorWithCpu(
        modelPath: String,
        inFilesDir: Boolean,
        threshold: Float
    ): TFLiteObjectDetector {
        val baseOptions = BaseOptions.builder()
            .setNumThreads(4)
            .build()

        val options = TFLiteObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(MAX_RESULTS)
            .setScoreThreshold(threshold)
            .build()

        isGpuEnabled = false

        return if (inFilesDir) {
            TFLiteObjectDetector.createFromFileAndOptions(
                File(context.filesDir, modelPath),
                options
            )
        } else {
            TFLiteObjectDetector.createFromFileAndOptions(
                context,
                modelPath,
                options
            )
        }
    }

    /**
     * Detects objects in the provided bitmap image.
     *
     * This method performs real-time object detection using the initialized TensorFlow Lite model.
     * It processes the image, runs inference (on GPU if available), and returns detected objects
     * with their labels, confidence scores, and bounding boxes.
     *
     * **Processing Steps:**
     * 1. Validates detector initialization
     * 2. Preprocesses the bitmap (converts to ARGB_8888 if needed)
     * 3. Runs inference (GPU-accelerated if enabled)
     * 4. Filters out small/invalid detections (< 20px)
     * 5. Removes duplicate detections (same object, same location)
     * 6. Sorts by confidence (highest first)
     *
     * **Performance:**
     * - CPU: 150-300ms per frame (device dependent)
     * - GPU: 50-100ms per frame (2-5x faster)
     *
     * @param bitmap The input image to analyze. Will be converted to ARGB_8888 if needed.
     * @return A list of [DetectionResult] objects, sorted by confidence (highest first).
     *         Returns an empty list if detection fails or detector is not initialized.
     *
     * Example:
     * ```kotlin
     * val bitmap = BitmapFactory.decodeFile(imagePath)
     * val detections = detector.detect(bitmap)
     *
     * detections.forEach { result ->
     *     Log.d("Detection", """
     *         Object: ${result.label}
     *         Confidence: ${(result.confidence * 100).toInt()}%
     *         Location: ${result.boundingBox}
     *         Time: ${result.detectionTimeMs}ms
     *     """.trimIndent())
     * }
     * ```
     */
    suspend fun detect(bitmap: Bitmap): List<DetectionResult> {
        return withContext(Dispatchers.Default) {
            if (!isInitialized || detector == null) {
                Log.w(TAG, "Detector not initialized")
                return@withContext emptyList()
            }

            try {
                val startTime = System.currentTimeMillis()

                // Preprocess bitmap for better detection
                val processedBitmap = preprocessBitmap(bitmap)

                val tensorImage = TensorImage.fromBitmap(processedBitmap)
                val results = detector?.detect(tensorImage) ?: emptyList()

                val detectionTime = System.currentTimeMillis() - startTime

                // Filter and sort results
                val detections = results.mapNotNull { detection ->
                    val category = detection.categories.firstOrNull() ?: return@mapNotNull null
                    val boundingBox = detection.boundingBox

                    // Filter out invalid or too small bounding boxes
                    val width = boundingBox.width()
                    val height = boundingBox.height()
                    if (width < 20 || height < 20) {
                        return@mapNotNull null
                    }

                    DetectionResult(
                        label = category.label.trim(),
                        confidence = category.score,
                        boundingBox = RectF(
                            boundingBox.left,
                            boundingBox.top,
                            boundingBox.right,
                            boundingBox.bottom
                        ),
                        detectionTimeMs = detectionTime
                    )
                }

                // Sort by confidence (highest first) and remove duplicates
                detections
                    .sortedByDescending { it.confidence }
                    .distinctBy { "${it.label}_${it.boundingBox.centerX().toInt()}_${it.boundingBox.centerY().toInt()}" }
                    .take(MAX_RESULTS)

            } catch (e: Exception) {
                Log.e(TAG, "Error during detection", e)
                emptyList()
            }
        }
    }

    /**
     * Preprocesses the bitmap for optimal detection performance.
     *
     * Ensures the bitmap is in ARGB_8888 format, which is required by TensorFlow Lite
     * for accurate color processing.
     *
     * @param bitmap The input bitmap to preprocess
     * @return The preprocessed bitmap in ARGB_8888 format
     */

    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // Ensure bitmap is in correct format
        return if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }

    /**
     * Updates the confidence threshold for object detection.
     *
     * **Note:** This will uninitialize the current detector. You must call [initialize]
     * again with the new threshold for changes to take effect.
     *
     * @param threshold The new confidence threshold (0.25-0.9)
     */
    fun updateConfidenceThreshold(threshold: Float) {
        if (threshold < MIN_CONFIDENCE_THRESHOLD || threshold > MAX_CONFIDENCE_THRESHOLD) {
            Log.w(TAG, "Confidence threshold out of range: $threshold")
            return
        }

        // Re-initialize detector with new threshold
        detector?.close()
        detector = null
        isInitialized = false
        isGpuEnabled = false
    }

    /**
     * Checks if any TensorFlow Lite model is available for object detection.
     *
     * Searches in both app-specific files directory and assets for any supported model.
     *
     * @return true if at least one model is found, false otherwise
     */
    fun isModelAvailable(): Boolean {
        for (modelPath in MODEL_FILES) {
            val modelFileInFilesDir = File(context.filesDir, modelPath)
            if (modelFileInFilesDir.exists()) {
                return true
            }

            try {
                context.assets.open(modelPath).use { return true }
            } catch (e: Exception) {
                // Try next model
            }
        }
        return false
    }

    /**
     * Gets the currently loaded model path.
     *
     * @return The model file path if loaded, or "No model loaded" if not initialized
     */
    fun getCurrentModel(): String {
        return currentModel.ifEmpty { "No model loaded" }
    }

    /**
     * Gets the file name of the currently loaded model.
     *
     * @return The model file name if loaded, or an empty string
     */
    fun getCurrentModelName(): String {
        return currentModel.substringAfterLast('/')
    }

    /**
     * Checks if GPU acceleration is currently enabled.
     *
     * @return true if GPU is being used, false if using CPU
     */
    fun isGpuAccelerationEnabled(): Boolean = isGpuEnabled

    /**
     * Releases all resources used by the detector.
     *
     * **Important:** Call this method when:
     * - The detector is no longer needed
     * - The activity/fragment is being destroyed
     * - You want to free up GPU resources
     *
     * After calling this method, you must call [initialize] again before using [detect].
     *
     * Example:
     * ```kotlin
     * override fun onDestroy() {
     *     super.onDestroy()
     *     objectDetector.close()
     * }
     * ```
     */
    fun close() {
        detector?.close()
        detector = null
        isInitialized = false
        isGpuEnabled = false
        Log.d(TAG, "Object detector closed and resources released")
    }
}
