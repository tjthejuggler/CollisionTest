package com.example.jugglingtracker.ui.progress

import android.content.Context
import android.widget.TextView
import com.example.jugglingtracker.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom marker view for displaying detailed information about chart data points
 */
class ChartMarkerView(context: Context) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvDate: TextView = findViewById(R.id.tv_marker_date)
    private val tvSuccessRate: TextView = findViewById(R.id.tv_marker_success_rate)
    private val tvAttempts: TextView = findViewById(R.id.tv_marker_attempts)
    private val tvDuration: TextView = findViewById(R.id.tv_marker_duration)

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val dataPoint = entry.data as? ChartDataPoint
            dataPoint?.let { point ->
                // Format date
                tvDate.text = dateFormatter.format(Date(point.date))
                
                // Format success rate
                tvSuccessRate.text = context.getString(
                    R.string.test_session_success_rate,
                    point.successRate
                )
                
                // Format attempts
                tvAttempts.text = "${point.successCount}/${point.attemptCount}"
                
                // Format duration
                val minutes = point.sessionDuration / (1000 * 60)
                val seconds = (point.sessionDuration % (1000 * 60)) / 1000
                tvDuration.text = if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }
            } ?: run {
                // Fallback if no data point is attached
                tvDate.text = "Unknown Date"
                tvSuccessRate.text = "${entry.y.toInt()}%"
                tvAttempts.text = "N/A"
                tvDuration.text = "N/A"
            }
        }
        
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}