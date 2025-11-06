package com.smartfind.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.DetectionStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced export manager supporting multiple formats:
 * - CSV (existing)
 * - JSON (new)
 * - PDF Report (new)
 * - HTML Dashboard (new)
 */
class EnhancedExportManager(private val context: Context) {
    
    enum class ExportFormat {
        CSV,
        JSON,
        PDF,
        HTML
    }
    
    /**
     * Export detections to JSON format
     */
    suspend fun exportToJson(
        detections: List<DetectedObject>,
        statistics: List<DetectionStatistics>? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject().apply {
                put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("total_detections", detections.size)
                put("app_version", "2.0.0")
                
                // Add detections array
                val detectionsArray = JSONArray()
                detections.forEach { detection ->
                    val detectionObj = JSONObject().apply {
                        put("id", detection.id)
                        put("object_name", detection.objectName)
                        put("confidence", detection.confidence)
                        put("timestamp", detection.timestamp)
                        put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(detection.timestamp)))
                        put("image_path", detection.imagePath)
                        put("thumbnail_path", detection.thumbnailPath)
                        detection.locationId?.let { put("location_id", it) }
                    }
                    detectionsArray.put(detectionObj)
                }
                put("detections", detectionsArray)
                
                // Add statistics if provided
                statistics?.let { stats ->
                    val statsArray = JSONArray()
                    stats.forEach { stat ->
                        val statObj = JSONObject().apply {
                            put("date", stat.date)
                            put("total_detections", stat.total_detections)
                            put("unique_objects", stat.unique_objects)
                            put("average_confidence", stat.average_confidence)
                            put("most_detected_object", stat.most_detected_object)
                            put("saved_detections", stat.saved_detections)
                        }
                        statsArray.put(statObj)
                    }
                    put("statistics", statsArray)
                }
            }
            
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            exportsDir.mkdirs()
            
            val filename = "smartfind_export_${System.currentTimeMillis()}.json"
            val file = File(exportsDir, filename)
            
            FileOutputStream(file).use { output ->
                output.write(jsonObject.toString(2).toByteArray())
            }
            
            file
        } catch (e: Exception) {
            android.util.Log.e("EnhancedExportManager", "Error exporting to JSON", e)
            null
        }
    }
    
    /**
     * Export as HTML dashboard with charts
     */
    suspend fun exportToHtml(
        detections: List<DetectedObject>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val frequencyMap = detections.groupingBy { it.objectName }.eachCount()
                .entries.sortedByDescending { it.value }.take(10)
            
            val avgConfidence = if (detections.isNotEmpty()) {
                detections.map { it.confidence }.average()
            } else 0.0
            
            val html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>SmartFind Detection Report</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            margin: 0;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            background: white;
                            border-radius: 20px;
                            padding: 40px;
                            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        }
                        h1 {
                            color: #667eea;
                            text-align: center;
                            margin-bottom: 10px;
                        }
                        .subtitle {
                            text-align: center;
                            color: #666;
                            margin-bottom: 40px;
                        }
                        .stats-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                            gap: 20px;
                            margin-bottom: 40px;
                        }
                        .stat-card {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px;
                            border-radius: 15px;
                            text-align: center;
                        }
                        .stat-value {
                            font-size: 48px;
                            font-weight: bold;
                            margin: 10px 0;
                        }
                        .stat-label {
                            font-size: 14px;
                            opacity: 0.9;
                        }
                        .chart-container {
                            margin: 40px 0;
                        }
                        .bar-chart {
                            margin-top: 20px;
                        }
                        .bar {
                            display: flex;
                            align-items: center;
                            margin-bottom: 15px;
                        }
                        .bar-label {
                            width: 150px;
                            font-weight: bold;
                            color: #333;
                        }
                        .bar-fill {
                            height: 30px;
                            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                            border-radius: 5px;
                            transition: width 0.3s ease;
                        }
                        .bar-value {
                            margin-left: 10px;
                            font-weight: bold;
                            color: #667eea;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 20px;
                        }
                        th, td {
                            padding: 12px;
                            text-align: left;
                            border-bottom: 1px solid #eee;
                        }
                        th {
                            background: #667eea;
                            color: white;
                        }
                        tr:hover {
                            background: #f5f5f5;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 40px;
                            color: #999;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>📱 SmartFind Detection Report</h1>
                        <p class="subtitle">Generated on ${SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())}</p>
                        
                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-label">Total Detections</div>
                                <div class="stat-value">${detections.size}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Unique Objects</div>
                                <div class="stat-value">${detections.map { it.objectName }.toSet().size}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Avg Confidence</div>
                                <div class="stat-value">${String.format(Locale.US, "%.1f", avgConfidence * 100)}%</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Most Detected</div>
                                <div class="stat-value" style="font-size: 24px;">${frequencyMap.firstOrNull()?.key ?: "N/A"}</div>
                            </div>
                        </div>
                        
                        <div class="chart-container">
                            <h2>📊 Top 10 Detected Objects</h2>
                            <div class="bar-chart">
                                ${frequencyMap.joinToString("\n") { (name, count) ->
                                    val maxCount = frequencyMap.firstOrNull()?.value ?: 1
                                    val percentage = (count.toFloat() / maxCount * 100).toInt()
                                    """
                                    <div class="bar">
                                        <div class="bar-label">$name</div>
                                        <div class="bar-fill" style="width: ${percentage}%;"></div>
                                        <div class="bar-value">$count</div>
                                    </div>
                                    """.trimIndent()
                                }}
                            </div>
                        </div>
                        
                        <div class="chart-container">
                            <h2>📝 Recent Detections</h2>
                            <table>
                                <thead>
                                    <tr>
                                        <th>Object</th>
                                        <th>Confidence</th>
                                        <th>Date & Time</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${detections.take(20).joinToString("\n") { detection ->
                                        """
                                        <tr>
                                            <td><strong>${detection.objectName}</strong></td>
                                            <td>${String.format(Locale.US, "%.1f%%", detection.confidence * 100)}</td>
                                            <td>${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(detection.timestamp))}</td>
                                        </tr>
                                        """.trimIndent()
                                    }}
                                </tbody>
                            </table>
                        </div>
                        
                        <div class="footer">
                            <p>Generated by SmartFind v2.0.0 - Object Detection & Reminder App</p>
                            <p>© 2025 SmartFind. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()
            
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            exportsDir.mkdirs()
            
            val filename = "smartfind_report_${System.currentTimeMillis()}.html"
            val file = File(exportsDir, filename)
            
            FileOutputStream(file).use { output ->
                output.write(html.toByteArray())
            }
            
            file
        } catch (e: Exception) {
            android.util.Log.e("EnhancedExportManager", "Error exporting to HTML", e)
            null
        }
    }
    
    /**
     * Export to PDF report (simplified version)
     */
    suspend fun exportToPdf(
        detections: List<DetectedObject>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
            }
            
            // Draw title
            canvas.drawText("SmartFind Detection Report", 50f, 50f, paint)
            
            // Draw statistics
            paint.textSize = 16f
            var yPos = 100f
            canvas.drawText("Total Detections: ${detections.size}", 50f, yPos, paint)
            yPos += 30f
            canvas.drawText("Unique Objects: ${detections.map { it.objectName }.toSet().size}", 50f, yPos, paint)
            yPos += 30f
            
            val avgConfidence = if (detections.isNotEmpty()) {
                detections.map { it.confidence }.average()
            } else 0.0
            canvas.drawText("Average Confidence: ${String.format(Locale.US, "%.1f%%", avgConfidence * 100)}", 50f, yPos, paint)
            yPos += 50f
            
            // Draw recent detections
            canvas.drawText("Recent Detections:", 50f, yPos, paint)
            yPos += 30f
            
            paint.textSize = 12f
            detections.take(15).forEach { detection ->
                val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(detection.timestamp))
                canvas.drawText(
                    "${detection.objectName} - ${String.format(Locale.US, "%.1f%%", detection.confidence * 100)} - $dateStr",
                    50f,
                    yPos,
                    paint
                )
                yPos += 25f
                
                if (yPos > 780f) return@forEach // Stop if we reach bottom of page
            }
            
            pdfDocument.finishPage(page)
            
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            exportsDir.mkdirs()
            
            val filename = "smartfind_report_${System.currentTimeMillis()}.pdf"
            val file = File(exportsDir, filename)
            
            FileOutputStream(file).use { output ->
                pdfDocument.writeTo(output)
            }
            
            pdfDocument.close()
            file
        } catch (e: Exception) {
            android.util.Log.e("EnhancedExportManager", "Error exporting to PDF", e)
            null
        }
    }
}
