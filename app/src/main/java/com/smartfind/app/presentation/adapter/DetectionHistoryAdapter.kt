package com.smartfind.app.presentation.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartfind.app.R
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.databinding.ItemDetectionHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DetectionHistoryAdapter(
    private val onItemClick: (DetectedObjectWithLocation) -> Unit,
    private val onSetReminderClick: (DetectedObjectWithLocation) -> Unit,
    private val onDeleteClick: (DetectedObjectWithLocation) -> Unit
) : ListAdapter<DetectedObjectWithLocation, DetectionHistoryAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onSetReminderClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemDetectionHistoryBinding,
        private val onItemClick: (DetectedObjectWithLocation) -> Unit,
        private val onSetReminderClick: (DetectedObjectWithLocation) -> Unit,
        private val onDeleteClick: (DetectedObjectWithLocation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(detection: DetectedObjectWithLocation) {
            val obj = detection.detectedObject
            val loc = detection.location
            
            binding.tvObjectName.text = obj.objectName
            binding.tvConfidence.text = "${(obj.confidence * 100).toInt()}%"
            binding.tvTimestamp.text = formatTimestamp(obj.timestamp)
            
            if (loc != null) {
                binding.tvLocation.text = loc.address ?: "Location available"
            } else {
                binding.tvLocation.text = "No location"
            }
            
            // Load thumbnail with detailed logging
            val thumbnailPath = obj.thumbnailPath ?: obj.imagePath
            if (thumbnailPath != null) {
                val file = File(thumbnailPath)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .into(binding.ivThumbnail)
                } else {
                    // Image file doesn't exist - show path for debugging
                    binding.ivThumbnail.setImageResource(R.drawable.ic_image_error)
                    android.util.Log.e("DetectionAdapter", "Image not found: $thumbnailPath")
                }
            } else {
                // No path set
                binding.ivThumbnail.setImageResource(R.drawable.ic_image_placeholder)
                android.util.Log.e("DetectionAdapter", "No image path for: ${obj.objectName}")
            }
            
            binding.root.setOnClickListener {
                onItemClick(detection)
            }
            
            binding.btnSetReminder.setOnClickListener {
                onSetReminderClick(detection)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(detection)
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} minutes ago"
                diff < 86400_000 -> "${diff / 3600_000} hours ago"
                diff < 604800_000 -> "${diff / 86400_000} days ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<DetectedObjectWithLocation>() {
        override fun areItemsTheSame(
            oldItem: DetectedObjectWithLocation,
            newItem: DetectedObjectWithLocation
        ): Boolean {
            return oldItem.detectedObject.id == newItem.detectedObject.id
        }
        
        override fun areContentsTheSame(
            oldItem: DetectedObjectWithLocation,
            newItem: DetectedObjectWithLocation
        ): Boolean {
            return oldItem == newItem
        }
    }
}
