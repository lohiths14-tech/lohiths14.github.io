package com.smartfind.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    
    private const val TAG = "ImageUtils"
    private const val IMAGE_QUALITY = 90
    private const val THUMBNAIL_SIZE = 200
    
    fun getImageStorageDir(context: Context): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "SmartFind"
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun createImageFile(context: Context, objectName: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = objectName.replace(Regex("[^A-Za-z0-9]"), "_")
        val fileName = "${sanitizedName}_$timestamp.jpg"
        return File(getImageStorageDir(context), fileName)
    }
    
    fun saveBitmap(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap", e)
            false
        }
    }
    
    fun createThumbnail(bitmap: Bitmap, maxSize: Int = THUMBNAIL_SIZE): Bitmap {
        val ratio = maxSize.toFloat() / Math.max(bitmap.width, bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    fun saveThumbnail(context: Context, originalFile: File): File? {
        return try {
            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            val thumbnail = createThumbnail(bitmap)
            
            val thumbnailFile = File(
                getImageStorageDir(context),
                "thumb_${originalFile.name}"
            )
            
            if (saveBitmap(thumbnail, thumbnailFile)) {
                bitmap.recycle()
                thumbnail.recycle()
                thumbnailFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail", e)
            null
        }
    }
    
    fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun getImageOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading image orientation", e)
            0
        }
    }
    
    fun deleteImageFiles(imagePath: String?, thumbnailPath: String?) {
        imagePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image file: $path", e)
            }
        }
        
        thumbnailPath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting thumbnail file: $path", e)
            }
        }
    }
    
    fun getDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        }
        return size
    }
    
    fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1 -> String.format(Locale.US, "%.2f KB", kb)
            else -> "$size B"
        }
    }
}
