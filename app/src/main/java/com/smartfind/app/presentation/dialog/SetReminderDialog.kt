package com.smartfind.app.presentation.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.smartfind.app.R
import com.smartfind.app.data.local.entity.RecurrenceType
import com.smartfind.app.data.local.entity.ReminderPriority
import com.smartfind.app.presentation.viewmodel.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for setting a reminder for a detected object
 */
@AndroidEntryPoint
class SetReminderDialog : DialogFragment() {
    
    private val reminderViewModel: ReminderViewModel by viewModels()
    private var selectedDateTime: Calendar = Calendar.getInstance()
    private var detectedObjectId: Long = -1
    private var objectName: String = ""
    
    private var isRecurring: Boolean = false
    private var recurrenceType: RecurrenceType = RecurrenceType.NONE
    private var priority: ReminderPriority = ReminderPriority.MEDIUM
    
    companion object {
        private const val ARG_OBJECT_ID = "object_id"
        private const val ARG_OBJECT_NAME = "object_name"
        
        fun newInstance(detectedObjectId: Long, objectName: String): SetReminderDialog {
            return SetReminderDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_OBJECT_ID, detectedObjectId)
                    putString(ARG_OBJECT_NAME, objectName)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            detectedObjectId = it.getLong(ARG_OBJECT_ID)
            objectName = it.getString(ARG_OBJECT_NAME, "")
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return try {
            val view = layoutInflater.inflate(R.layout.dialog_set_reminder, null)
            
            val titleInput = view.findViewById<TextInputEditText>(R.id.et_reminder_title)
            val messageInput = view.findViewById<TextInputEditText>(R.id.et_reminder_message)
            val dateTimeButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_select_datetime)
            val checkRecurring = view.findViewById<SwitchMaterial>(R.id.check_recurring)
            val spinnerRecurrence = view.findViewById<Spinner>(R.id.spinner_recurrence)
            val spinnerPriority = view.findViewById<Spinner>(R.id.spinner_priority)
            
            // Set default values
            titleInput.setText("Remember: $objectName")
            messageInput.setText("Don't forget your $objectName")
            
            // Set default date/time (1 hour from now)
            selectedDateTime.add(Calendar.HOUR_OF_DAY, 1)
            updateDateTimeButtonText(dateTimeButton)
            
            dateTimeButton.setOnClickListener {
                showDateTimePicker(dateTimeButton)
            }
            
            // Setup recurrence spinner
            val recurrenceTypes = arrayOf("None", "Daily", "Weekly", "Monthly")
            spinnerRecurrence.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, recurrenceTypes)
            spinnerRecurrence.isEnabled = false
            
            // Setup priority spinner
            val priorities = arrayOf("Low", "Medium", "High", "Urgent")
            spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
            spinnerPriority.setSelection(1) // Default to Medium
            
            // Toggle recurrence options
            checkRecurring.setOnCheckedChangeListener { _, isChecked ->
                isRecurring = isChecked
                spinnerRecurrence.isEnabled = isChecked
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle("Set Reminder")
                .setView(view)
                .setPositiveButton("Save") { _, _ ->
                    try {
                        val title = titleInput.text.toString().trim()
                        val message = messageInput.text.toString().trim()
                        
                        if (title.isEmpty()) {
                            Toast.makeText(requireContext(), "Please enter a reminder title", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        
                        if (detectedObjectId == -1L) {
                            Toast.makeText(requireContext(), "Invalid object ID", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        
                        val reminderTime = selectedDateTime.timeInMillis
                        val currentTime = System.currentTimeMillis()
                        
                        if (reminderTime <= currentTime) {
                            Toast.makeText(requireContext(), "Please select a future date/time", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        
                        // Get recurrence type
                        recurrenceType = when (spinnerRecurrence.selectedItemPosition) {
                            1 -> RecurrenceType.DAILY
                            2 -> RecurrenceType.WEEKLY
                            3 -> RecurrenceType.MONTHLY
                            else -> RecurrenceType.NONE
                        }
                        
                        // Get priority
                        priority = when (spinnerPriority.selectedItemPosition) {
                            0 -> ReminderPriority.LOW
                            1 -> ReminderPriority.MEDIUM
                            2 -> ReminderPriority.HIGH
                            3 -> ReminderPriority.URGENT
                            else -> ReminderPriority.MEDIUM
                        }
                        
                        reminderViewModel.createReminderEnhanced(
                            detectedObjectId = detectedObjectId,
                            reminderTime = reminderTime,
                            title = title,
                            message = message,
                            isRecurring = isRecurring,
                            recurrenceType = recurrenceType,
                            priority = priority
                        )
                        
                        Toast.makeText(requireContext(), "Reminder set for $objectName", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("SetReminderDialog", "Error creating reminder", e)
                        Toast.makeText(requireContext(), "Failed to create reminder: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
        } catch (e: Exception) {
            android.util.Log.e("SetReminderDialog", "Error setting up reminder dialog", e)
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to open reminder dialog. Please try again.")
                .setPositiveButton("OK", null)
                .create()
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }
    
    private fun setupObservers() {
        reminderViewModel.reminderSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                dismiss()
            }
        }
        
        reminderViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                reminderViewModel.clearError()
            }
        }
    }
    
    private fun showDateTimePicker(button: com.google.android.material.button.MaterialButton) {
        val currentDate = Calendar.getInstance()
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // Now show time picker
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        selectedDateTime.set(Calendar.SECOND, 0)
                        updateDateTimeButtonText(button)
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = currentDate.timeInMillis
        }.show()
    }
    
    private fun updateDateTimeButtonText(button: com.google.android.material.button.MaterialButton) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        button.text = dateFormat.format(selectedDateTime.time)
    }
}
