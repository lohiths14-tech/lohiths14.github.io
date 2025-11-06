package com.smartfind.app.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartfind.app.R
import com.smartfind.app.databinding.FragmentHistoryBinding
import com.smartfind.app.presentation.adapter.DetectionHistoryAdapter
import com.smartfind.app.presentation.dialog.ImagePreviewDialog
import com.smartfind.app.presentation.viewmodel.HistoryViewModel
import com.smartfind.app.data.local.entity.DetectedObjectWithLocation
import com.smartfind.app.util.RatingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: DetectionHistoryAdapter

    @Inject
    lateinit var ratingManager: RatingManager

    // Selection mode for batch delete
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<Long>()

    // For undo functionality
    private var lastDeletedItem: DetectedObjectWithLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ratingManager.requestReview(requireActivity())

        setupMenu()
        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        setupObservers()
        setupFab()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (isSelectionMode) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_selection, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_selected -> {
                        deleteSelectedItems()
                        true
                    }
                    R.id.action_select_all -> {
                        selectAllItems()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFab() {
        // FAB will be used for batch operations
        binding.fabBatchDelete.setOnClickListener {
            toggleSelectionMode()
        }
    }

    private fun setupRecyclerView() {
        adapter = DetectionHistoryAdapter(
            onItemClick = { detection ->
                if (isSelectionMode) {
                    toggleItemSelection(detection.detectedObject.id)
                } else {
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
                }
            },
            onSetReminderClick = { detection ->
                val dialog = com.smartfind.app.presentation.dialog.SetReminderDialog.newInstance(
                    detection.detectedObject.id,
                    detection.detectedObject.objectName
                )
                dialog.show(parentFragmentManager, "SetReminderDialog")
            },
            onDeleteClick = { detection ->
                showDeleteConfirmationDialog(detection)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }

        // Swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val detection = adapter.currentList[position]
                lastDeletedItem = detection

                viewModel.deleteDetection(detection)

                Snackbar.make(binding.root, "${detection.detectedObject.objectName} deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        lastDeletedItem?.let { item ->
                            viewModel.restoreDetection(item)
                            lastDeletedItem = null
                        }
                    }
                    .show()
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchDetections(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchDetections(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.searchDetections("")
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupObservers() {
        viewModel.filteredDetections.observe(viewLifecycleOwner) { detections ->
            adapter.submitList(detections)

            if (detections.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showDeleteConfirmationDialog(detection: DetectedObjectWithLocation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Detection?")
            .setMessage("Are you sure you want to delete '${detection.detectedObject.objectName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lastDeletedItem = detection
                viewModel.deleteDetection(detection)

                Snackbar.make(binding.root, "${detection.detectedObject.objectName} deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        lastDeletedItem?.let { item ->
                            viewModel.restoreDetection(item)
                            lastDeletedItem = null
                        }
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        selectedItems.clear()

        if (isSelectionMode) {
            binding.fabBatchDelete.setImageResource(R.drawable.ic_close)
            Toast.makeText(requireContext(), "Selection mode: Tap items to select", Toast.LENGTH_SHORT).show()
        } else {
            binding.fabBatchDelete.setImageResource(R.drawable.ic_delete)
        }

        requireActivity().invalidateOptionsMenu()
        adapter.notifyDataSetChanged()
    }

    private fun toggleItemSelection(itemId: Long) {
        if (selectedItems.contains(itemId)) {
            selectedItems.remove(itemId)
        } else {
            selectedItems.add(itemId)
        }
        adapter.notifyDataSetChanged()
    }

    private fun selectAllItems() {
        val allIds = adapter.currentList.map { it.detectedObject.id }
        selectedItems.addAll(allIds)
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "${selectedItems.size} items selected", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSelectedItems() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items selected", Toast.LENGTH_SHORT).show()
            return
        }

        val count = selectedItems.size
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete $count Items?")
            .setMessage("Are you sure you want to delete $count selected detection(s)?")
            .setPositiveButton("Delete") { _, _ ->
                val itemsToDelete = adapter.currentList.filter {
                    selectedItems.contains(it.detectedObject.id)
                }

                itemsToDelete.forEach { detection ->
                    viewModel.deleteDetection(detection)
                }

                selectedItems.clear()
                toggleSelectionMode()

                Toast.makeText(requireContext(), "$count item(s) deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
