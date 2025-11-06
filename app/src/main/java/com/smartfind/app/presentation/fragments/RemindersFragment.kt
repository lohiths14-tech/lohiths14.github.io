package com.smartfind.app.presentation.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartfind.app.databinding.FragmentRemindersBinding
import com.smartfind.app.presentation.adapter.ReminderAdapter
import com.smartfind.app.presentation.viewmodel.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment displaying all reminders
 */
@AndroidEntryPoint
class RemindersFragment : Fragment() {
    
    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showNotificationPermissionDialog()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        checkNotificationPermission()
    }
    
    private fun setupRecyclerView() {
        adapter = ReminderAdapter(
            onCancelClick = { reminder ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cancel Reminder")
                    .setMessage("Are you sure you want to cancel this reminder?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.cancelReminder(reminder.id)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )
        
        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RemindersFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.activeReminders.observe(viewLifecycleOwner) { reminders ->
            if (reminders.isEmpty()) {
                binding.rvReminders.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.rvReminders.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
                adapter.submitList(reminders)
            }
        }
        
        viewModel.activeReminderCount.observe(viewLifecycleOwner) { count ->
            binding.tvActiveCount.text = when {
                count == 0 -> "No active reminders"
                count == 1 -> "1 active reminder"
                else -> "$count active reminders"
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .setPositiveButton("OK", null)
                    .show()
                viewModel.clearError()
            }
        }
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionDialog()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notification Permission Required")
            .setMessage("To receive reminder notifications, please grant notification permission in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
