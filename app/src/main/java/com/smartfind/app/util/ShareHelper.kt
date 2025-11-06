package com.smartfind.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enum to define the format for sharing content.
 */
enum class ShareFormat {
    TEXT_ONLY,
    IMAGE_ONLY,
    TEXT_AND_IMAGE
}

/**
 * A helper class to facilitate sharing of object detection results.
 *
 * This class handles the creation of sharing intents for single or multiple detections,
 * supports text, image, and combined sharing formats, and adds a "Found with SmartFind"
 * overlay to shared images. It relies on a FileProvider for secure file sharing.
 *
 * @property context The context used to create intents and access the file system.
 */
class ShareHelper(private val context: Context) {

    private val fileProviderAuthority = "${context.packageName}.provider"

    /**
     * Shares a single detected object.
     *
     * @param detection The [DetectedObjectWithLocation] to be shared.
     * @param format The desired [ShareFormat].
     */
    suspend fun shareDetection(detection: DetectedObjectWithLocation, format: ShareFormat) {
        val shareText = generateShareText(detection)
        val imagePath = if (format != ShareFormat.TEXT_ONLY) detection.detectedObject.imagePath else null

        val shareIntent = createShareIntent(shareText, imagePath, format)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Share Detection"))
        }
    }

    /**
     * Shares a list of detected objects.
     *
     * For TEXT format, it concatenates the details of all detections.
     * For IMAGE or TEXT_AND_IMAGE formats, it uses the image of the first detection in the list.
     *
     * @param detections A list of [DetectedObjectWithLocation] to be shared.
     * @param format The desired [ShareFormat].
     */
    suspend fun shareDetections(detections: List<DetectedObjectWithLocation>, format: ShareFormat) {
        if (detections.isEmpty()) return

        val combinedText = detections.joinToString(separator = "\n\n") { generateShareText(it) }
        val imagePath = if (format != ShareFormat.TEXT_ONLY) detections.firstOrNull()?.detectedObject?.imagePath else null

        val shareIntent = createShareIntent(combinedText, imagePath, format)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Share Detections"))
        }
    }

    /**
     * Creates the Android sharing Intent based on the specified format.
     */
    private fun createShareIntent(text: String, imagePath: String?, format: ShareFormat): Intent? {
        val intent = Intent(Intent.ACTION_SEND)
        var isIntentReady = false

        when (format) {
            ShareFormat.TEXT_ONLY -> {
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, text)
                isIntentReady = true
            }
            ShareFormat.IMAGE_ONLY -> {
                val imageUri = getUriForImagePath(imagePath) ?: return null
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_STREAM, imageUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                isIntentReady = true
            }
            ShareFormat.TEXT_AND_IMAGE -> {
                val imageUri = getUriForImagePath(imagePath) ?: return null
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_STREAM, imageUri)
                intent.putExtra(Intent.EXTRA_TEXT, text)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                isIntentReady = true
            }
        }

        return if (isIntentReady) intent else null
    }

    /**
     * Generates a descriptive string for a single detection.
     */
    private fun generateShareText(detection: DetectedObjectWithLocation): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(detection.detectedObject.timestamp))
        val locationString = if (detection.location?.latitude != null && detection.location.longitude != null) {
            "\n- Location: ${"%.4f".format(detection.location.latitude)}, ${"%.4f".format(detection.location.longitude)}"
        } else {
            ""
        }

        return """
        Object Detected with SmartFind!
        - Object: ${detection.detectedObject.objectName.replaceFirstChar { it.titlecase(Locale.getDefault()) }}
        - Confidence: ${String.format("%.1f%%", detection.detectedObject.confidence * 100)}
        - Date: $date$locationString
        """.trimIndent()
    }

    /**
     * Converts a file path to a content URI through the FileProvider.
     * This method also creates the image overlay.
     */
    private fun getUriForImagePath(imagePath: String?): Uri? {
        if (imagePath == null) return null
        val imageFile = File(imagePath)
        if (!imageFile.exists()) return null

        val bitmapWithOverlay = createImageWithOverlay(imageFile)
        val tempFile = saveBitmapToCacheDir(bitmapWithOverlay) ?: return null

        return FileProvider.getUriForFile(context, fileProviderAuthority, tempFile)
    }

    /**
     * Reads a bitmap from a file and draws a text overlay on it.
     */
    private fun createImageWithOverlay(imageFile: File): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = mutableBitmap.height * 0.05f // Responsive text size
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        val bgPaint = Paint().apply {
            color = Color.argb(128, 0, 0, 0) // 50% transparent black
        }

        val text = "Found with SmartFind"
        val padding = mutableBitmap.height * 0.04f
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        // Position at bottom-left
        val x = padding
        val y = mutableBitmap.height - padding

        canvas.drawRect(
            x - padding / 2,
            y - textBounds.height() - padding / 2,
            x + textBounds.width() + padding / 2,
            y + padding / 2,
            bgPaint
        )
        canvas.drawText(text, x, y, textPaint)

        return mutableBitmap
    }

    /**
     * Saves a bitmap to a temporary file in the cache directory.
     */
    private fun saveBitmapToCacheDir(bitmap: Bitmap): File? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_images")
            cacheDir.mkdirs()
            val file = File.createTempFile("share_", ".jpg", cacheDir)
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
