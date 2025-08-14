package com.example.jugglingtracker.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.jugglingtracker.JugglingTrackerApplication
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.FragmentProgressChartBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProgressChartFragment : Fragment(), OnChartValueSelectedListener {

    private var _binding: FragmentProgressChartBinding? = null
    private val binding get() = _binding!!
    
    private val args: ProgressChartFragmentArgs by navArgs()
    
    private val viewModel: ProgressChartViewModel by viewModels {
        ProgressChartViewModelFactory(
            (requireActivity().application as JugglingTrackerApplication).testSessionRepository
        )
    }

    private val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val fullDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()
        setupFilterControls()
        observeViewModel()
        
        // Load progress data for the specific pattern
        val patternId = args.patternId
        viewModel.loadProgressData(patternId)
    }

    private fun setupChart() {
        with(binding.lineChart) {
            // Chart configuration
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chart_background))
            
            // Set chart value selected listener
            setOnChartValueSelectedListener(this@ProgressChartFragment)
            
            // Set custom marker view
            marker = ChartMarkerView(requireContext())
            
            // Enable highlighting
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = false
            
            // X-Axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                textSize = 10f
                granularity = 1f
                setLabelCount(6, false)
                setAvoidFirstLastClipping(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return dateFormatter.format(Date(value.toLong()))
                    }
                }
            }
            
            // Left Y-Axis configuration (Success Rate)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                textSize = 10f
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(6, false)
                setDrawZeroLine(true)
                zeroLineColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}%"
                    }
                }
            }
            
            // Right Y-Axis configuration (disabled)
            axisRight.isEnabled = false
            
            // Legend configuration
            legend.apply {
                isEnabled = true
                form = Legend.LegendForm.LINE
                textSize = 12f
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
            }
            
            // Animation
            animateX(1000, Easing.EaseInOutQuart)
            
            // Extra offsets for better visibility
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
    }

    private fun setupFilterControls() {
        // Test Length Filter
        binding.chipGroupTestLength.setOnCheckedStateChangeListener { group, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_short_tests -> TestLengthFilter.SHORT
                R.id.chip_medium_tests -> TestLengthFilter.MEDIUM
                R.id.chip_long_tests -> TestLengthFilter.LONG
                else -> null
            }
            viewModel.updateTestLengthFilter(filter)
        }
        
        // Timeframe Filter
        binding.chipGroupTimeframe.setOnCheckedStateChangeListener { group, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_last_week -> TimeframeFilter.LAST_WEEK
                R.id.chip_last_month -> TimeframeFilter.LAST_MONTH
                R.id.chip_all_time -> TimeframeFilter.ALL_TIME
                else -> TimeframeFilter.ALL_TIME
            }
            viewModel.updateTimeframeFilter(filter)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe chart data points
                launch {
                    viewModel.chartDataPoints.collect { dataPoints ->
                        updateChart(dataPoints)
                    }
                }
                
                // Observe UI state
                launch {
                    viewModel.uiState.collect { uiState ->
                        handleUiState(uiState)
                    }
                }
                
                // Observe statistics
                launch {
                    viewModel.statistics.collect { stats ->
                        updateStatistics(stats)
                    }
                }
            }
        }
    }

    private fun updateChart(dataPoints: List<ChartDataPoint>) {
        if (dataPoints.isEmpty()) {
            showNoDataState()
            return
        }
        
        hideNoDataState()
        
        // Create entries for the line chart
        val entries = dataPoints.mapIndexed { index, point ->
            Entry(point.date.toFloat(), point.successRate.toFloat(), point)
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Success Rate").apply {
            // Line styling
            color = ContextCompat.getColor(requireContext(), R.color.chart_line_success)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_line_success))
            lineWidth = 3f
            circleRadius = 4f
            setDrawCircleHole(false)
            
            // Fill styling
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.chart_line_success)
            fillAlpha = 30
            
            // Value styling
            setDrawValues(false)
            
            // Highlight styling
            highlightLineWidth = 2f
            highLightColor = ContextCompat.getColor(requireContext(), R.color.primary)
            
            // Smooth curve
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }
        
        // Create line data
        val lineData = LineData(dataSet)
        
        // Set data to chart
        binding.lineChart.apply {
            data = lineData
            invalidate() // Refresh chart
            
            // Fit chart to data
            fitScreen()
        }
    }

    private fun updateStatistics(stats: ProgressStatistics) {
        with(binding) {
            // Update success rate
            tvSuccessRate.text = "${stats.overallSuccessRate.toInt()}%"
            
            // Update total attempts (sum of all attempts)
            // Note: We need to calculate total attempts from filtered sessions
            // This will be handled by observing filtered test sessions
            
            // Update practice time
            val hours = stats.totalPracticeTime / (1000 * 60 * 60)
            val minutes = (stats.totalPracticeTime % (1000 * 60 * 60)) / (1000 * 60)
            tvPracticeTime.text = if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }
        
        // Observe filtered sessions for additional stats
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredTestSessions.collect { sessions ->
                updateDetailedStatistics(sessions)
            }
        }
    }

    private fun updateDetailedStatistics(sessions: List<com.example.jugglingtracker.data.entities.TestSession>) {
        // Calculate best results by test length
        val shortSessions = sessions.filter { it.duration <= 5 * 60 * 1000L }
        val mediumSessions = sessions.filter { it.duration in (5 * 60 * 1000L)..(15 * 60 * 1000L) }
        val longSessions = sessions.filter { it.duration > 15 * 60 * 1000L }
        
        // Best short test
        val bestShort = shortSessions.maxByOrNull { session ->
            if (session.attemptCount > 0) session.successCount.toDouble() / session.attemptCount.toDouble() else 0.0
        }
        binding.tvBestShort.text = bestShort?.let { "${it.successCount}/${it.attemptCount}" } ?: "N/A"
        
        // Best medium test
        val bestMedium = mediumSessions.maxByOrNull { session ->
            if (session.attemptCount > 0) session.successCount.toDouble() / session.attemptCount.toDouble() else 0.0
        }
        binding.tvBestMedium.text = bestMedium?.let { "${it.successCount}/${it.attemptCount}" } ?: "N/A"
        
        // Best long test
        val bestLong = longSessions.maxByOrNull { session ->
            if (session.attemptCount > 0) session.successCount.toDouble() / session.attemptCount.toDouble() else 0.0
        }
        binding.tvBestLong.text = bestLong?.let { "${it.successCount}/${it.attemptCount}" } ?: "N/A"
        
        // Total attempts
        val totalAttempts = sessions.sumOf { it.attemptCount }
        binding.tvTotalAttempts.text = totalAttempts.toString()
    }

    private fun showNoDataState() {
        binding.lineChart.visibility = View.GONE
        binding.layoutNoChartData.visibility = View.VISIBLE
    }

    private fun hideNoDataState() {
        binding.lineChart.visibility = View.VISIBLE
        binding.layoutNoChartData.visibility = View.GONE
    }

    private fun handleUiState(uiState: ProgressChartUiState) {
        // Handle loading state
        // Note: You might want to add a progress bar to the layout for loading state
        
        // Handle error state
        uiState.error?.let { error ->
            // Show error message (you might want to add a Snackbar or error view)
            showNoDataState()
        }
    }

    // Chart value selection callbacks
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        e?.let { entry ->
            val dataPoint = entry.data as? ChartDataPoint
            dataPoint?.let { point ->
                // Show detailed information about the selected data point
                val date = fullDateFormatter.format(Date(point.date))
                val successRate = "%.1f%%".format(point.successRate)
                val duration = formatDuration(point.sessionDuration)
                
                // You could show this in a tooltip, dialog, or update a text view
                // For now, we'll show the information in a different way since toolbar was removed
                // TODO: Consider showing this information in a dedicated text view or toast
            }
        }
    }

    override fun onNothingSelected() {
        // Reset any displayed information since toolbar was removed
        // TODO: Consider clearing any dedicated text view that shows selection info
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / (1000 * 60)
        val seconds = (durationMs % (1000 * 60)) / 1000
        return "${minutes}m ${seconds}s"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}