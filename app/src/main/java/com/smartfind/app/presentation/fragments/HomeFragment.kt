package com.smartfind.app.presentation.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartfind.app.R
import com.smartfind.app.databinding.FragmentHomeBinding
import com.smartfind.app.presentation.camera.CameraManager
import com.smartfind.app.presentation.camera.toBitmap
import com.smartfind.app.presentation.viewmodel.CameraViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()
    private lateinit var cameraManager: CameraManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = CameraManager(requireContext(), binding.previewView)
        cameraManager.initialize()

        setupUI()
        setupObservers()
        checkPermissionsAndStartCamera()
    }

    private fun setupUI() {
        binding.fabStartStop.setOnClickListener {
            if (viewModel.isDetecting.value == true) {
                stopDetection()
            } else {
                startDetection()
            }
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnSettings.setOnClickListener {
            // Navigate to settings
            Toast.makeText(requireContext(), "Settings clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnFind.setOnClickListener {
            if (viewModel.findModeLabel.value != null) {
                viewModel.stopFindMode()
            } else {
                showFindObjectDialog()
            }
        }

        binding.btnCapture.setOnClickListener {
            captureDetection()
        }
    }

    private fun setupObservers() {
        viewModel.isDetecting.observe(viewLifecycleOwner) { isDetecting ->
            if (isDetecting) {
                binding.fabStartStop.text = "Stop"
                binding.fabStartStop.setIconResource(R.drawable.ic_stop)
            } else {
                binding.fabStartStop.text = "Start"
                binding.fabStartStop.setIconResource(R.drawable.ic_play)
            }
        }

        viewModel.detectionResults.observe(viewLifecycleOwner) { results ->
            binding.overlayView.updateDetections(results,
                binding.previewView.width,
                binding.previewView.height,
                viewModel.findModeLabel.value)
        }

        viewModel.detectionCount.observe(viewLifecycleOwner) { count ->
            binding.tvDetectionCount.text = "Detections: $count"
        }

        viewModel.isLowLight.observe(viewLifecycleOwner) { isLowLight ->
            if (isLowLight) {
                binding.tvLowLight.visibility = View.VISIBLE
            } else {
                binding.tvLowLight.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.modelInitialized.observe(viewLifecycleOwner) { initialized ->
            if (!initialized && !viewModel.isModelAvailable()) {
                showModelNotFoundDialog()
            }
        }

        viewModel.findModeLabel.observe(viewLifecycleOwner) { label ->
            if (label != null) {
                binding.btnFind.setImageResource(R.drawable.ic_close)
                Toast.makeText(requireContext(), "Finding: $label", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnFind.setImageResource(R.drawable.ic_search)
            }
        }

        viewModel.objectFoundEvent.observe(viewLifecycleOwner) { found ->
            if (found) {
                performHapticFeedback()
                viewModel.onObjectFoundEventHandled()
            }
        }
    }

    private fun checkPermissionsAndStartCamera() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        lifecycleScope.launch {
            cameraManager.startCamera(viewLifecycleOwner, createImageAnalyzer())
        }
    }

    private fun createImageAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            if (viewModel.isDetecting.value == true) {
                processImage(imageProxy)
            } else {
                imageProxy.close()
            }
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            try {
                val bitmap = imageProxy.toBitmap()
                viewModel.detectObjects(bitmap)
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun startDetection() {
        viewModel.startDetection()
        Toast.makeText(requireContext(), "Detection started", Toast.LENGTH_SHORT).show()
    }

    private fun stopDetection() {
        viewModel.stopDetection()
        binding.overlayView.clear()
        Toast.makeText(requireContext(), "Detection stopped", Toast.LENGTH_SHORT).show()
    }

    private fun captureDetection() {
        val currentResults = viewModel.detectionResults.value
        if (currentResults.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No objects detected. Start detection first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the current preview and save the top detection
        lifecycleScope.launch {
            try {
                val topResult = currentResults.firstOrNull { it.confidence >= viewModel.confidenceThreshold }
                if (topResult != null) {
                    // Request manual capture
                    viewModel.requestManualCapture(topResult)
                    Toast.makeText(requireContext(), "Capturing: ${topResult.label}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No confident detection found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to capture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFlash() {
        val currentMode = cameraManager.getFlashMode()
        val newMode = when (currentMode) {
            androidx.camera.core.ImageCapture.FLASH_MODE_AUTO -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_on)
                androidx.camera.core.ImageCapture.FLASH_MODE_ON
            }
            androidx.camera.core.ImageCapture.FLASH_MODE_ON -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
                androidx.camera.core.ImageCapture.FLASH_MODE_OFF
            }
            else -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_auto)
                androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
            }
        }
        cameraManager.setFlashMode(newMode)
    }

    private fun switchCamera() {
        lifecycleScope.launch {
            cameraManager.switchCamera(viewLifecycleOwner, createImageAnalyzer())
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("Camera permission is required for object detection. Please grant the permission in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showModelNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Model Not Found")
            .setMessage("The object detection model is not available. Please download the model using the provided script.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showFindObjectDialog() {
        val names = viewModel.distinctObjectNames.value ?: emptyList()
        if (names.isEmpty()) {
            Toast.makeText(requireContext(), "No previously detected objects to find.", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Find Object")
            .setItems(names.toTypedArray()) { dialog, which ->
                val selectedObject = names[which]
                viewModel.startFindMode(selectedObject)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                viewModel.stopFindMode()
                dialog.dismiss()
            }
            .show()
    }

    private fun performHapticFeedback() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraManager.shutdown()
        _binding = null
    }
}
