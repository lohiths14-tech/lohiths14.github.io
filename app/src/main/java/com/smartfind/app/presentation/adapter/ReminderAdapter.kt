package com.smartfind.app.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartfind.app.R
import com.smartfind.app.data.local.entity.ObjectReminder
import com.smartfind.app.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying reminders in RecyclerView
 */
class ReminderAdapter(
    private val onCancelClick: (ObjectReminder) -> Unit
) : ListAdapter<ObjectReminder, ReminderAdapter.ReminderViewHolder>(ReminderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding, onCancelClick)
    }
    
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ReminderViewHolder(
        private val binding: ItemReminderBinding,
        private val onCancelClick: (ObjectReminder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        
        fun bind(reminder: ObjectReminder) {
            binding.tvTitle.text = reminder.title
            binding.tvMessage.text = reminder.message
            binding.tvDatetime.text = dateFormat.format(Date(reminder.reminder_time))
            
            // Set status chip
            val context = binding.root.context
            when {
                reminder.is_cancelled -> {
                    binding.chipStatus.text = "Cancelled"
                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                    binding.ivStatus.setColorFilter(
                        ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    )
                }
                reminder.is_triggered -> {
                    binding.chipStatus.text = "Triggered"
                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                }
                reminder.reminder_time < System.currentTimeMillis() -> {
                    binding.chipStatus.text = "Overdue"
                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                }
                else -> {
                    binding.chipStatus.text = "Active"
                    binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    binding.ivStatus.setColorFilter(
                        ContextCompat.getColor(context, android.R.color.holo_green_dark)
                    )
                }
            }
            
            // Set cancel button click listener
            binding.btnCancel.setOnClickListener {
                onCancelClick(reminder)
            }
            
            // Disable cancel button if already cancelled or triggered
            binding.btnCancel.isEnabled = !reminder.is_cancelled && !reminder.is_triggered
            binding.btnCancel.alpha = if (binding.btnCancel.isEnabled) 1.0f else 0.5f
        }
    }
    
    class ReminderDiffCallback : DiffUtil.ItemCallback<ObjectReminder>() {
        override fun areItemsTheSame(oldItem: ObjectReminder, newItem: ObjectReminder): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ObjectReminder, newItem: ObjectReminder): Boolean {
            return oldItem == newItem
        }
    }
}
