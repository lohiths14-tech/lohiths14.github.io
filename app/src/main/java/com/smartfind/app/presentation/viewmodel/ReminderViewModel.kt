package com.smartfind.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartfind.app.analytics.AnalyticsManager
import com.smartfind.app.data.local.entity.ObjectReminder
import com.smartfind.app.data.repository.ReminderRepository
import com.smartfind.app.notification.ReminderNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing reminders
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    application: Application,
    private val repository: ReminderRepository,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    
    private val notificationManager = ReminderNotificationManager(application)
    
    val allReminders: LiveData<List<ObjectReminder>> = repository.getAllReminders().asLiveData()
    val activeReminders: LiveData<List<ObjectReminder>> = repository.getActiveReminders().asLiveData()
    
    private val _reminderSaved = MutableLiveData<Boolean>()
    val reminderSaved: LiveData<Boolean> = _reminderSaved
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _activeReminderCount = MutableLiveData<Int>()
    val activeReminderCount: LiveData<Int> = _activeReminderCount
    
    fun getRemindersForObject(objectId: Long): LiveData<List<ObjectReminder>> {
        return repository.getRemindersForObject(objectId).asLiveData()
    }
    
    fun createReminder(
        detectedObjectId: Long,
        reminderTime: Long,
        title: String,
        message: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminder = ObjectReminder(
                    detected_object_id = detectedObjectId,
                    reminder_time = reminderTime,
                    title = title,
                    message = message
                )
                
                val id = repository.insertReminder(reminder)
                _reminderSaved.postValue(true)
                
                android.util.Log.d("ReminderViewModel", "Reminder created with ID: $id for time: $reminderTime")
                
                // Update count
                updateReminderCount()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to create reminder: ${e.message}")
                _reminderSaved.postValue(false)
                android.util.Log.e("ReminderViewModel", "Error creating reminder", e)
            }
        }
    }
    
    fun createReminderEnhanced(
        detectedObjectId: Long,
        reminderTime: Long,
        title: String,
        message: String,
        isRecurring: Boolean,
        recurrenceType: com.smartfind.app.data.local.entity.RecurrenceType,
        priority: com.smartfind.app.data.local.entity.ReminderPriority
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminder = ObjectReminder(
                    detected_object_id = detectedObjectId,
                    reminder_time = reminderTime,
                    title = title,
                    message = message,
                    is_recurring = isRecurring,
                    recurrence_type = recurrenceType,
                    priority = priority
                )
                
                val id = repository.insertReminder(reminder)
                _reminderSaved.postValue(true)
                
                android.util.Log.d("ReminderViewModel", "Enhanced reminder created with ID: $id, recurring: $isRecurring, priority: $priority")
                
                // Update count
                updateReminderCount()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to create reminder: ${e.message}")
                _reminderSaved.postValue(false)
                android.util.Log.e("ReminderViewModel", "Error creating enhanced reminder", e)
            }
        }
    }
    
    fun deleteReminder(reminder: ObjectReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteReminder(reminder)
                notificationManager.cancelNotification(reminder.id)
                updateReminderCount()
                android.util.Log.d("ReminderViewModel", "Reminder deleted: ${reminder.id}")
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete reminder: ${e.message}")
                android.util.Log.e("ReminderViewModel", "Error deleting reminder", e)
            }
        }
    }
    
    fun cancelReminder(reminderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.markAsCancelled(reminderId)
                notificationManager.cancelNotification(reminderId)
                updateReminderCount()
                android.util.Log.d("ReminderViewModel", "Reminder cancelled: $reminderId")
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to cancel reminder: ${e.message}")
                android.util.Log.e("ReminderViewModel", "Error cancelling reminder", e)
            }
        }
    }
    
    private fun updateReminderCount() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.getActiveReminderCount()
                _activeReminderCount.postValue(count)
            } catch (e: Exception) {
                android.util.Log.e("ReminderViewModel", "Error updating reminder count", e)
            }
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        return notificationManager.hasNotificationPermission()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    init {
        updateReminderCount()
    }
}
