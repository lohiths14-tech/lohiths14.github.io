package com.smartfind.app.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartfind.app.databinding.FragmentSearchBinding
import com.smartfind.app.presentation.adapter.DetectionHistoryAdapter
import com.smartfind.app.presentation.dialog.ImagePreviewDialog
import com.smartfind.app.presentation.dialog.SetReminderDialog
import com.smartfind.app.presentation.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {
    
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: DetectionHistoryAdapter
    
    private val popularObjects = listOf(
        "person", "car", "phone", "laptop", "bottle", 
        "cup", "keyboard", "mouse", "book", "wallet"
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupPopularChips()
        setupObservers()
        
        // Load all detections on start
        viewModel.searchDetections("")
    }
    
    private fun setupRecyclerView() {
        adapter = DetectionHistoryAdapter(
            onItemClick = { detection ->
                // Show image preview dialog
                val obj = detection.detectedObject
                val imagePath = obj.imagePath ?: obj.thumbnailPath
                
                val dialog = ImagePreviewDialog.newInstance(
                    imagePath,
                    obj.objectName,
                    obj.confidence,
                    obj.timestamp
                )
                dialog.show(parentFragmentManager, "ImagePreviewDialog")
            },
            onSetReminderClick = { detection ->
                // Open reminder dialog
                val dialog = SetReminderDialog.newInstance(
                    detectedObjectId = detection.detectedObject.id,
                    objectName = detection.detectedObject.objectName
                )
                dialog.show(parentFragmentManager, "SetReminderDialog")
            },
            onDeleteClick = { detection ->
                showDeleteConfirmationDialog(detection)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchFragment.adapter
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.searchDetections("")
                }
                return true
            }
        })
    }
    
    private fun setupPopularChips() {
        popularObjects.forEach { objectName ->
            val chip = Chip(requireContext()).apply {
                text = objectName
                isClickable = true
                setOnClickListener {
                    performSearch(objectName)
                    binding.searchView.setQuery(objectName, false)
                }
            }
            binding.chipGroup.addView(chip)
        }
    }
    
    private fun performSearch(query: String) {
        viewModel.searchDetections(query)
    }
    
    private fun setupObservers() {
        viewModel.filteredDetections.observe(viewLifecycleOwner) { detections ->
            val safeDetections = detections ?: emptyList()
            adapter.submitList(safeDetections)
            
            binding.tvResultCount.text = "${safeDetections.size} results found"
            
            if (safeDetections.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showDeleteConfirmationDialog(detection: com.smartfind.app.data.local.entity.DetectedObjectWithLocation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Detection?")
            .setMessage("Are you sure you want to delete '${detection.detectedObject.objectName}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDetection(detection)
                
                Snackbar.make(binding.root, "${detection.detectedObject.objectName} deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.restoreDetection(detection)
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
