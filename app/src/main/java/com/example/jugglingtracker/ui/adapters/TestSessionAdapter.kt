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
                    textPatternName.text = pattern.name
                    textPatternName.visibility = android.view.View.VISIBLE
                } else {
                    textPatternName.visibility = android.view.View.GONE
                }

                // Date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                textDate.text = dateFormat.format(Date(session.date))

                // Duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(session.duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(session.duration) % 60
                textDuration.text = if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }

                // Success count and attempts
                textSuccessCount.text = session.successCount.toString()
                textAttemptCount.text = session.attemptCount.toString()

                // Success rate
                val successRate = if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
                textSuccessRate.text = String.format("%.1f%%", successRate)

                // Set success rate color based on performance
                val successRateColor = when {
                    successRate >= 90 -> android.graphics.Color.parseColor("#4CAF50") // Green - Excellent
                    successRate >= 70 -> android.graphics.Color.parseColor("#8BC34A") // Light Green - Good
                    successRate >= 50 -> android.graphics.Color.parseColor("#FF9800") // Orange - Fair
                    else -> android.graphics.Color.parseColor("#F44336") // Red - Poor
                }
                textSuccessRate.setTextColor(successRateColor)

                // Notes (if available)
                if (!session.notes.isNullOrBlank()) {
                    textNotes.text = session.notes
                    textNotes.visibility = android.view.View.VISIBLE
                    labelNotes.visibility = android.view.View.VISIBLE
                } else {
                    textNotes.visibility = android.view.View.GONE
                    labelNotes.visibility = android.view.View.GONE
                }

                // Video indicator
                imageVideoIndicator.visibility = if (session.videoPath != null) {
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

                // Delete button (if delete callback is provided)
                onDeleteClick?.let { deleteCallback ->
                    buttonDelete.visibility = android.view.View.VISIBLE
                    buttonDelete.setOnClickListener {
                        deleteCallback(session)
                    }
                } ?: run {
                    buttonDelete.visibility = android.view.View.GONE
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
                textPatternName.visibility = android.view.View.GONE

                // Date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                textDate.text = dateFormat.format(Date(session.date))

                // Duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(session.duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(session.duration) % 60
                textDuration.text = if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }

                // Success count and attempts
                textSuccessCount.text = session.successCount.toString()
                textAttemptCount.text = session.attemptCount.toString()

                // Success rate
                val successRate = if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
                textSuccessRate.text = String.format("%.1f%%", successRate)

                // Notes
                if (!session.notes.isNullOrBlank()) {
                    textNotes.text = session.notes
                    textNotes.visibility = android.view.View.VISIBLE
                    labelNotes.visibility = android.view.View.VISIBLE
                } else {
                    textNotes.visibility = android.view.View.GONE
                    labelNotes.visibility = android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onSessionClick(session)
                }

                // Delete button
                onDeleteClick?.let { deleteCallback ->
                    buttonDelete.visibility = android.view.View.VISIBLE
                    buttonDelete.setOnClickListener {
                        deleteCallback(session)
                    }
                } ?: run {
                    buttonDelete.visibility = android.view.View.GONE
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