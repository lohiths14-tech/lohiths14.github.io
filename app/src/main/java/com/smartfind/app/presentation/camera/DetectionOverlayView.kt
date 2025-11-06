package com.smartfind.app.presentation.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.smartfind.app.domain.model.DetectionResult

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val highlightPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    private val highlightTextBackgroundPaint = Paint().apply {
        color = Color.YELLOW
        alpha = 128
        style = Paint.Style.FILL
    }



    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#AA000000")
        style = Paint.Style.FILL
    }

    private var detectionResults: List<DetectionResult> = emptyList()
    private var findModeLabel: String? = null
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    fun updateDetections(
        results: List<DetectionResult>,
        imageWidth: Int,
        imageHeight: Int,
        findModeLabel: String?
    ) {
        this.detectionResults = results
        this.findModeLabel = findModeLabel

        // Calculate scale and offset to map detection coordinates to view coordinates
        val viewAspectRatio = width.toFloat() / height.toFloat()
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = height.toFloat() / imageHeight
            offsetX = (width - imageWidth * scaleFactor) / 2f
            offsetY = 0f
        } else {
            scaleFactor = width.toFloat() / imageWidth
            offsetX = 0f
            offsetY = (height - imageHeight * scaleFactor) / 2f
        }

        invalidate()
    }

    fun clear() {
        detectionResults = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in detectionResults) {
            val box = result.boundingBox

            // Transform bounding box coordinates
            val left = box.left * scaleFactor + offsetX
            val top = box.top * scaleFactor + offsetY
            val right = box.right * scaleFactor + offsetX
            val bottom = box.bottom * scaleFactor + offsetY

            val isHighlight = findModeLabel?.equals(result.label, ignoreCase = true) == true

            // Draw bounding box
            val currentBoxPaint = if (isHighlight) {
                highlightPaint
            } else {
                boxPaint.apply { color = getConfidenceColor(result.confidence) }
            }
            canvas.drawRect(left, top, right, bottom, currentBoxPaint)

            // Draw label with background
            val label = "${result.label} ${(result.confidence * 100).toInt()}%"
            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)

            val textX = left + 10f
            val textY = top - 10f

            // Draw background for text
            val bgLeft = textX - 5f
            val bgTop = textY - textBounds.height() - 5f
            val bgRight = textX + textBounds.width() + 5f
            val bgBottom = textY + 5f

            val currentBgPaint = if (isHighlight) highlightTextBackgroundPaint else backgroundPaint
            canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, currentBgPaint)

            // Draw text
            canvas.drawText(label, textX, textY, textPaint)
        }
    }

    private fun getConfidenceColor(confidence: Float): Int {
        return when {
            confidence >= 0.8f -> Color.GREEN
            confidence >= 0.6f -> Color.YELLOW
            else -> Color.RED
        }
    }
}
