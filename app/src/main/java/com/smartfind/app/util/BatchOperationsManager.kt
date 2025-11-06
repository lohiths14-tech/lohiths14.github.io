package com.smartfind.app.util

import android.content.Context
import com.google.gson.Gson
import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages batch operations on detected objects, such as bulk deletion and exporting.
 *
 * This class provides a centralized way to perform actions on multiple database entries
 * at once, improving efficiency and code organization. It's designed to be injected
 * via Hilt.
 *
 * @property detectedObjectDao The DAO for interacting with the detected objects table.
 */
@Singleton
class BatchOperationsManager @Inject constructor(
    private val detectedObjectDao: DetectedObjectDao
) {

    /**
     * Deletes multiple detected objects from the database.
     *
     * @param objectIds A list of primary key IDs for the objects to be deleted.
     * @return The number of rows deleted.
     */
    suspend fun deleteMultiple(objectIds: List<Long>) {
        if (objectIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            objectIds.forEach { detectedObjectDao.deleteById(it) }
        }
    }

    /**
     * Exports a selection of detected objects to a JSON file.
     *
     * @param context The context used for file operations.
     * @param objectIds The IDs of the objects to export.
     * @return A [File] object pointing to the created JSON file, or null on failure.
     */
    suspend fun exportMultipleToJson(context: Context, objectIds: List<Long>): File? {
        if (objectIds.isEmpty()) return null
        val objectsToExport = fetchObjectsByIds(objectIds)
        if (objectsToExport.isEmpty()) return null

        val gson = Gson()
        val jsonString = gson.toJson(objectsToExport)

        return saveStringToFile(context, "smartfind_export_${generateTimestamp()}.json", jsonString)
    }

    /**
     * Exports a selection of detected objects to a CSV file.
     *
     * @param context The context used for file operations.
     * @param objectIds The IDs of the objects to export.
     * @return A [File] object pointing to the created CSV file, or null on failure.
     */
    suspend fun exportMultipleToCsv(context: Context, objectIds: List<Long>): File? {
        if (objectIds.isEmpty()) return null
        val objectsToExport = fetchObjectsByIds(objectIds)
        if (objectsToExport.isEmpty()) return null

        val csvContent = generateCsvContent(objectsToExport)
        return saveStringToFile(context, "smartfind_export_${generateTimestamp()}.csv", csvContent)
    }

    /**
     * Fetches a list of [DetectedObjectWithLocation] from the database by their IDs.
     */
    private suspend fun fetchObjectsByIds(objectIds: List<Long>): List<DetectedObjectWithLocation> {
        return withContext(Dispatchers.IO) {
            objectIds.mapNotNull { detectedObjectDao.getByIdWithLocation(it) }
        }
    }

    /**
     * Generates the full content of the CSV file, including the header row.
     */
    private fun generateCsvContent(data: List<DetectedObjectWithLocation>): String {
        val header = "id,timestamp,label,confidence,latitude,longitude,imagePath"
        return buildString {
            appendLine(header)
            data.forEach { entity ->
                appendLine(entityToCsvRow(entity))
            }
        }
    }

    /**
     * Converts a single [DetectedObjectWithLocation] to a CSV-formatted string row.
     */
    private fun entityToCsvRow(entity: DetectedObjectWithLocation): String {
        return "${entity.detectedObject.id},${entity.detectedObject.timestamp},${entity.detectedObject.objectName},${entity.detectedObject.confidence},${entity.location?.latitude ?: ""},${entity.location?.longitude ?: ""},${entity.detectedObject.imagePath}"
    }

    /**
     * Saves a given string content to a file in the app's external files directory.
     */
    private suspend fun saveStringToFile(context: Context, fileName: String, content: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Using getExternalFilesDir for user-accessible, app-specific files.
                val storageDir = context.getExternalFilesDir("exports")
                if (storageDir != null && !storageDir.exists()) {
                    storageDir.mkdirs()
                }

                val file = File(storageDir, fileName)
                FileWriter(file).use { writer ->
                    writer.append(content)
                }
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Generates a simple timestamp string for unique filenames.
     */
    private fun generateTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}
