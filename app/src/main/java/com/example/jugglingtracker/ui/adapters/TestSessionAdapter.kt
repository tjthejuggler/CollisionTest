package com.example.jugglingtracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.databinding.ItemTestSessionBinding
import com.example.jugglingtracker.ui.history.TestSessionWithPattern
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * RecyclerView adapter for displaying test sessions in a list.
 */
class TestSessionAdapter(
    private val onSessionClick: (TestSession) -> Unit = {},
    private val onSessionLongClick: (TestSession) -> Unit = {},
    private val onDeleteClick: ((TestSession) -> Unit)? = null,
    private val showPatternName: Boolean = true
) : ListAdapter<TestSessionWithPattern, TestSessionAdapter.TestSessionViewHolder>(TestSessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestSessionViewHolder {
        val binding = ItemTestSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TestSessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestSessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TestSessionViewHolder(
        private val binding: ItemTestSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sessionWithPattern: TestSessionWithPattern) {
            val session = sessionWithPattern.testSession
            val pattern = sessionWithPattern.pattern

            binding.apply {
                // Pattern name (if showing)
                if (showPatternName && pattern != null) {
                    tvPatternName.text = pattern.name
                    tvPatternName.visibility = android.view.View.VISIBLE
                } else {
                    tvPatternName.visibility = android.view.View.GONE
                }

                // Date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(session.date))

                // Duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(session.duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(session.duration) % 60
                tvDuration.text = if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }

                // Success rate and attempts
                val successRate = if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
                tvSuccessRate.text = String.format("%.1f%%", successRate)
                tvAttempts.text = "${session.successCount}/${session.attemptCount}"

                // Set success rate color based on performance
                val successRateColor = when {
                    successRate >= 90 -> android.graphics.Color.parseColor("#4CAF50") // Green - Excellent
                    successRate >= 70 -> android.graphics.Color.parseColor("#8BC34A") // Light Green - Good
                    successRate >= 50 -> android.graphics.Color.parseColor("#FF9800") // Orange - Fair
                    else -> android.graphics.Color.parseColor("#F44336") // Red - Poor
                }
                tvSuccessRate.setTextColor(successRateColor)

                // Notes indicator
                ivHasNotes.visibility = if (!session.notes.isNullOrBlank()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Video indicator
                ivHasVideo.visibility = if (session.videoPath != null) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onSessionClick(session)
                }

                root.setOnLongClickListener {
                    onSessionLongClick(session)
                    true
                }

                // Menu button
                btnMenu.setOnClickListener {
                    onSessionLongClick(session)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class TestSessionDiffCallback : DiffUtil.ItemCallback<TestSessionWithPattern>() {
        override fun areItemsTheSame(oldItem: TestSessionWithPattern, newItem: TestSessionWithPattern): Boolean {
            return oldItem.testSession.id == newItem.testSession.id
        }

        override fun areContentsTheSame(oldItem: TestSessionWithPattern, newItem: TestSessionWithPattern): Boolean {
            return oldItem.testSession == newItem.testSession && oldItem.pattern == newItem.pattern
        }
    }
}

/**
 * Simplified adapter for test sessions without pattern information
 */
class SimpleTestSessionAdapter(
    private val onSessionClick: (TestSession) -> Unit = {},
    private val onDeleteClick: ((TestSession) -> Unit)? = null
) : ListAdapter<TestSession, SimpleTestSessionAdapter.SimpleTestSessionViewHolder>(SimpleTestSessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleTestSessionViewHolder {
        val binding = ItemTestSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimpleTestSessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimpleTestSessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SimpleTestSessionViewHolder(
        private val binding: ItemTestSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: TestSession) {
            binding.apply {
                // Hide pattern name for simple adapter
                tvPatternName.visibility = android.view.View.GONE

                // Date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(session.date))

                // Duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(session.duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(session.duration) % 60
                tvDuration.text = if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }

                // Success rate and attempts
                val successRate = if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
                tvSuccessRate.text = String.format("%.1f%%", successRate)
                tvAttempts.text = "${session.successCount}/${session.attemptCount}"

                // Notes indicator
                ivHasNotes.visibility = if (!session.notes.isNullOrBlank()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onSessionClick(session)
                }

                // Menu button for delete functionality
                btnMenu.setOnClickListener {
                    onDeleteClick?.invoke(session)
                }
            }
        }
    }

    private class SimpleTestSessionDiffCallback : DiffUtil.ItemCallback<TestSession>() {
        override fun areItemsTheSame(oldItem: TestSession, newItem: TestSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TestSession, newItem: TestSession): Boolean {
            return oldItem == newItem
        }
    }
}