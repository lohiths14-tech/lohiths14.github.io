package com.smartfind.app.presentation.dialog

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartfind.app.R
import com.smartfind.app.databinding.DialogImagePreviewBinding
import java.io.File

class ImagePreviewDialog : DialogFragment() {
    
    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!
    
    private var imagePath: String? = null
    private var objectName: String? = null
    private var confidence: Float = 0f
    private var timestamp: Long = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set fullscreen dialog style
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imagePath = arguments?.getString(ARG_IMAGE_PATH)
        objectName = arguments?.getString(ARG_OBJECT_NAME)
        confidence = arguments?.getFloat(ARG_CONFIDENCE) ?: 0f
        timestamp = arguments?.getLong(ARG_TIMESTAMP) ?: 0L
        
        setupUI()
    }
    
    private fun setupUI() {
        // Set title
        binding.tvObjectName.text = objectName
        binding.tvConfidence.text = "${(confidence * 100).toInt()}%"
        
        // Format timestamp
        val timeAgo = formatTimeAgo(timestamp)
        binding.tvTimestamp.text = timeAgo
        
        // Load image
        imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.ivPreview)
                
                // Enable zoom on image
                binding.ivPreview.setOnClickListener {
                    // Toggle scale type for zoom
                    if (binding.ivPreview.scaleType == ImageView.ScaleType.FIT_CENTER) {
                        binding.ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                    } else {
                        binding.ivPreview.scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                }
            } else {
                binding.ivPreview.setImageResource(R.drawable.ic_image_error)
            }
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                dateFormat.format(java.util.Date(timestamp))
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_OBJECT_NAME = "object_name"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_TIMESTAMP = "timestamp"
        
        fun newInstance(
            imagePath: String?,
            objectName: String,
            confidence: Float,
            timestamp: Long
        ): ImagePreviewDialog {
            return ImagePreviewDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_PATH, imagePath)
                    putString(ARG_OBJECT_NAME, objectName)
                    putFloat(ARG_CONFIDENCE, confidence)
                    putLong(ARG_TIMESTAMP, timestamp)
                }
            }
        }
    }
}
