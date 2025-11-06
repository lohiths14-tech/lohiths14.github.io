package com.smartfind.app.presentation.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartfind.app.BuildConfig
import com.smartfind.app.databinding.FragmentSettingsBinding
import com.smartfind.app.presentation.dialog.ModelSelectionDialog
import com.smartfind.app.presentation.viewmodel.CameraViewModel
import com.smartfind.app.presentation.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(), ModelSelectionDialog.ModelSelectionListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val cameraViewModel: CameraViewModel by activityViewModels()
    private val historyViewModel: HistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        // Confidence threshold
        binding.sliderConfidence.addOnChangeListener { _, value, _ ->
            binding.tvConfidenceValue.text = "${(value * 100).toInt()}%"
            cameraViewModel.confidenceThreshold = value
        }

        // Detection interval
        binding.sliderInterval.addOnChangeListener { _, value, _ ->
            binding.tvIntervalValue.text = "${value.toInt()}ms"
            cameraViewModel.detectionIntervalMs = value.toLong()
        }

        // Auto-save toggle
        binding.switchAutoSave.setOnCheckedChangeListener { _, isChecked ->
            cameraViewModel.autoSaveEnabled = isChecked
        }

        // Dark mode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Storage management
        binding.btnClearOldFiles.setOnClickListener {
            showClearOldFilesDialog()
        }

        binding.btnViewStorage.setOnClickListener {
            showStorageInfo()
        }

        // Export CSV
        binding.btnExportCsv.setOnClickListener {
            exportToCsv()
        }

        // About
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.btnSelectModel.setOnClickListener {
            showModelSelectionDialog()
        }
    }

    private fun loadSettings() {
        binding.sliderConfidence.value = cameraViewModel.confidenceThreshold
        binding.tvConfidenceValue.text = "${(cameraViewModel.confidenceThreshold * 100).toInt()}%"

        binding.sliderInterval.value = cameraViewModel.detectionIntervalMs.toFloat()
        binding.tvIntervalValue.text = "${cameraViewModel.detectionIntervalMs}ms"

        binding.switchAutoSave.isChecked = cameraViewModel.autoSaveEnabled

        val currentMode = AppCompatDelegate.getDefaultNightMode()
        binding.switchDarkMode.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun showClearOldFilesDialog() {
        val options = arrayOf("7 days", "14 days", "30 days", "60 days")
        val days = arrayOf(7, 14, 30, 60)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Old Files")
            .setItems(options) { _, which ->
                historyViewModel.deleteOldDetections(days[which])
                Toast.makeText(
                    requireContext(),
                    "Clearing files older than ${options[which]}...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStorageInfo() {
        val (size, formatted) = historyViewModel.getStorageInfo()

        val warningThreshold = 450 * 1024 * 1024L // 450MB
        val maxThreshold = 500 * 1024 * 1024L // 500MB

        val message = buildString {
            append("Storage used: $formatted\n\n")

            when {
                size >= maxThreshold -> append("⚠️ Storage limit reached! Please clear old files.")
                size >= warningThreshold -> append("⚠️ Storage is almost full. Consider clearing old files.")
                else -> append("Storage is within normal limits.")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Storage Information")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Clear Old Files") { _, _ ->
                showClearOldFilesDialog()
            }
            .show()
    }

    private fun exportToCsv() {
        val file = historyViewModel.exportToCsv()

        if (file != null) {
            // Share the CSV file
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export CSV"))

            Toast.makeText(
                requireContext(),
                "CSV exported: ${file.name}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showModelSelectionDialog() {
        // Ideally, these would be fetched from a dynamic source like the ViewModel
        val models = arrayOf(
            "yolov8n.tflite",
            "efficientdet_lite4.tflite",
            "ssd_mobilenet_v2.tflite",
            "detect.tflite"
        )
        // This requires a method in the ViewModel to get the current model name
        val currentModel = cameraViewModel.getCurrentModelName()
        val dialog = ModelSelectionDialog.newInstance(models, currentModel)
        dialog.show(childFragmentManager, "ModelSelectionDialog")
    }

    override fun onModelSelected(modelName: String) {
        cameraViewModel.updateModel(modelName)
        Toast.makeText(
            requireContext(),
            "Model selection changed to: $modelName. Restarting detector...",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showAboutDialog() {
        val message = """
            SmartFind v${BuildConfig.VERSION_NAME}

            An offline-first Android app for object detection and tracking.

            Developer Information:
            👤 Name: Lohith S
            🎓 Branch: AIML

            Features:
            • On-device object detection using TensorFlow Lite
            • Camera-based real-time detection
            • Location tagging with reminders
            • History and search
            • Privacy-focused (all data stored locally)

            Built with:
            • Kotlin
            • CameraX
            • Room Database
            • TensorFlow Lite
            • Material Design 3
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About SmartFind")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
