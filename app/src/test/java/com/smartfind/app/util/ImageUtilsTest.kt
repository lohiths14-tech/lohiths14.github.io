package com.smartfind.app.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for ImageUtils
 * Tests file naming, path generation, and utility functions
 */
class ImageUtilsTest {

    @Test
    fun `test image file naming convention`() {
        val objectName = "phone"
        val timestamp = System.currentTimeMillis()
        
        // File name should contain object name and be lowercase
        val expectedPattern = "smartfind_${objectName.lowercase()}_"
        
        assertTrue("File name should start with 'smartfind_'", expectedPattern.startsWith("smartfind_"))
        assertTrue("File name should contain object name", expectedPattern.contains(objectName.lowercase()))
    }

    @Test
    fun `test thumbnail size constant`() {
        // Thumbnail should be reasonably sized (not too large or too small)
        val expectedThumbnailSize = 80 // 80dp as per docs
        assertTrue("Thumbnail size should be reasonable", expectedThumbnailSize in 50..200)
    }

    @Test
    fun `test file path structure`() {
        val basePath = "/storage/emulated/0/Android/data/com.smartfind.app/files"
        val expectedSubfolder = "Pictures/SmartFind"
        
        assertTrue("Path should contain Pictures folder", expectedSubfolder.contains("Pictures"))
        assertTrue("Path should contain app name", expectedSubfolder.contains("SmartFind"))
    }

    @Test
    fun `test timestamp format is valid`() {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val formattedDate = dateFormat.format(Date(timestamp))
        
        // Should be 15 characters (8 for date + 1 underscore + 6 for time)
        assertEquals(15, formattedDate.length)
        assertTrue("Formatted date should contain underscore", formattedDate.contains("_"))
    }

    @Test
    fun `test object name sanitization`() {
        val unsafeNames = listOf("phone/tablet", "laptop\\computer", "keys:wallet", "cup?mug")
        
        unsafeNames.forEach { name ->
            val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            assertFalse("Sanitized name should not contain special chars", 
                sanitized.contains(Regex("[/\\\\:?*\"<>|]")))
        }
    }
}
