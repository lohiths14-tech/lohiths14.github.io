package com.smartfind.app.presentation.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.smartfind.app.R
import com.smartfind.app.presentation.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

/**
 * Fragment displaying comprehensive statistics and insights
 */
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    
    private val viewModel: StatisticsViewModel by viewModels()
    
    // UI Elements
    private lateinit var tvTotalDetections: TextView
    private lateinit var tvTotalSaved: TextView
    private lateinit var tvAvgConfidence: TextView
    private lateinit var tvMostFrequent: TextView
    private lateinit var tvTotalTime: TextView
    
    private lateinit var tvTodayDetections: TextView
    private lateinit var tvTodaySaved: TextView
    private lateinit var tvTodayUnique: TextView
    private lateinit var tvTodayAvgConf: TextView
    private lateinit var tvTodayTime: TextView
    
    private lateinit var rvObjectFrequency: RecyclerView
    private lateinit var objectFrequencyAdapter: ObjectFrequencyAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupObservers()
        setupRecyclerView()
    }
    
    private fun initializeViews(view: View) {
        // All-time stats
        tvTotalDetections = view.findViewById(R.id.tv_total_detections)
        tvTotalSaved = view.findViewById(R.id.tv_total_saved)
        tvAvgConfidence = view.findViewById(R.id.tv_avg_confidence)
        tvMostFrequent = view.findViewById(R.id.tv_most_frequent)
        tvTotalTime = view.findViewById(R.id.tv_total_time)
        
        // Today stats
        tvTodayDetections = view.findViewById(R.id.tv_today_detections)
        tvTodaySaved = view.findViewById(R.id.tv_today_saved)
        tvTodayUnique = view.findViewById(R.id.tv_today_unique)
        tvTodayAvgConf = view.findViewById(R.id.tv_today_avg_conf)
        tvTodayTime = view.findViewById(R.id.tv_today_time)
        
        // RecyclerView
        rvObjectFrequency = view.findViewById(R.id.rv_object_frequency)
        
        // Refresh button
        view.findViewById<View>(R.id.btn_refresh_stats)?.setOnClickListener {
            viewModel.refreshStatistics()
        }
    }
    
    private fun setupObservers() {
        // All-time statistics
        viewModel.totalDetections.observe(viewLifecycleOwner) { total ->
            tvTotalDetections.text = total.toString()
        }
        
        viewModel.totalSavedDetections.observe(viewLifecycleOwner) { total ->
            tvTotalSaved.text = total.toString()
        }
        
        viewModel.averageConfidence.observe(viewLifecycleOwner) { avg ->
            tvAvgConfidence.text = String.format(Locale.US, "%.1f%%", avg * 100)
        }
        
        viewModel.mostFrequentObject.observe(viewLifecycleOwner) { obj ->
            tvMostFrequent.text = obj
        }
        
        viewModel.totalDetectionTime.observe(viewLifecycleOwner) { time ->
            tvTotalTime.text = viewModel.formatTime(time)
        }
        
        // Today's statistics
        viewModel.todayStatistics.observe(viewLifecycleOwner) { stats ->
            tvTodayDetections.text = stats.detectionsCount.toString()
            tvTodaySaved.text = stats.savedCount.toString()
            tvTodayUnique.text = stats.uniqueObjects.toString()
            tvTodayAvgConf.text = String.format(Locale.US, "%.1f%%", stats.avgConfidence * 100)
            tvTodayTime.text = viewModel.formatTime(stats.timeSpent)
        }
        
        // Object frequency
        viewModel.objectFrequencyMap.observe(viewLifecycleOwner) { frequencyMap ->
            val sortedList = frequencyMap.entries
                .sortedByDescending { it.value }
                .take(10) // Top 10 objects
            objectFrequencyAdapter.updateData(sortedList)
        }
    }
    
    private fun setupRecyclerView() {
        objectFrequencyAdapter = ObjectFrequencyAdapter()
        rvObjectFrequency.apply {
            adapter = objectFrequencyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshStatistics()
    }
    
    /**
     * Adapter for displaying object frequency
     */
    class ObjectFrequencyAdapter : RecyclerView.Adapter<ObjectFrequencyAdapter.ViewHolder>() {
        
        private var data: List<Map.Entry<String, Int>> = emptyList()
        
        fun updateData(newData: List<Map.Entry<String, Int>>) {
            data = newData
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_object_frequency, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = data[position]
            holder.bind(entry.key, entry.value, position + 1)
        }
        
        override fun getItemCount(): Int = data.size
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvRank: TextView = view.findViewById(R.id.tv_rank)
            private val tvObjectName: TextView = view.findViewById(R.id.tv_object_name)
            private val tvCount: TextView = view.findViewById(R.id.tv_count)
            private val cardView: MaterialCardView = view.findViewById(R.id.card_frequency)
            
            fun bind(objectName: String, count: Int, rank: Int) {
                tvRank.text = "#$rank"
                tvObjectName.text = objectName.replaceFirstChar { it.uppercase() }
                tvCount.text = "$count detections"
                
                // Highlight top 3
                when (rank) {
                    1 -> cardView.strokeColor = Color.parseColor("#FFD700") // Gold
                    2 -> cardView.strokeColor = Color.parseColor("#C0C0C0") // Silver
                    3 -> cardView.strokeColor = Color.parseColor("#CD7F32") // Bronze
                    else -> cardView.strokeColor = Color.parseColor("#E0E0E0")
                }
            }
        }
    }
}
