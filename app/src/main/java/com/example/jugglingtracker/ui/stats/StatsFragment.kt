package com.example.jugglingtracker.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.FragmentStatsBinding
import com.example.jugglingtracker.ui.adapters.WeeklyTrendAdapter
import com.example.jugglingtracker.ui.dialogs.StatsInfoDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var weeklyTrendAdapter: WeeklyTrendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add menu provider for app bar menu
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        weeklyTrendAdapter = WeeklyTrendAdapter()
        binding.recyclerWeeklyTrends.apply {
            adapter = weeklyTrendAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Menu provider methods
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_stats, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_info -> {
                showInfoDialog()
                true
            }
            else -> false
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    updateUI(uiState)
                }
            }
        }
    }

    private fun updateUI(uiState: StatsUiState) {
        binding.apply {
            // Handle loading state
            if (uiState.isLoading) {
                // You could show a loading indicator here if needed
                return
            }

            // Handle error state
            uiState.error?.let { error ->
                Snackbar.make(root, error, Snackbar.LENGTH_LONG)
                    .setAction(R.string.confirm_retry) {
                        viewModel.refreshStats()
                    }
                    .show()
                return
            }

            // Update current week stats
            textWeeklyScore.text = viewModel.formatPoints(uiState.currentWeekPoints)
            textUsageLevel.text = viewModel.formatLevel(uiState.currentUsageLevel)
            
            // Update progress bar
            progressWeeklyLevel.progress = uiState.progressPercentage.toInt()
            textProgressMin.text = uiState.currentUsageLevel.minPoints.toString()
            textProgressMax.text = if (uiState.currentUsageLevel.maxPoints == Int.MAX_VALUE) {
                "∞"
            } else {
                uiState.currentUsageLevel.maxPoints.toString()
            }

            // Set level color
            try {
                val levelColor = Color.parseColor(uiState.currentUsageLevel.color)
                progressWeeklyLevel.setIndicatorColor(levelColor)
                textUsageLevel.setTextColor(levelColor)
            } catch (e: IllegalArgumentException) {
                // Use default colors if parsing fails
            }

            // Update general usage stats
            textPatternsCreated.text = uiState.patternsCreated.toString()
            textTestsCompleted.text = uiState.testsCompleted.toString()
            textVideosRecorded.text = uiState.videosRecorded.toString()
            textAppOpens.text = uiState.appOpens.toString()
            textTotalTestTime.text = viewModel.formatTestTime(uiState.totalTestTime)
            textAverageWeeklyPoints.text = uiState.averageWeeklyPoints.toInt().toString()

            // Update weekly trends
            if (uiState.weeklyTrends.isNotEmpty()) {
                recyclerWeeklyTrends.visibility = View.VISIBLE
                textNoTrendsData.visibility = View.GONE
                weeklyTrendAdapter.submitList(uiState.weeklyTrends)
            } else {
                recyclerWeeklyTrends.visibility = View.GONE
                textNoTrendsData.visibility = View.VISIBLE
            }
        }
    }

    private fun showInfoDialog() {
        val dialog = StatsInfoDialogFragment.newInstance()
        dialog.show(parentFragmentManager, StatsInfoDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}