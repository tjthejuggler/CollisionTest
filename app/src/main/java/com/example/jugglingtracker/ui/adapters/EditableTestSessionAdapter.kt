package com.example.jugglingtracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.databinding.ItemTestSessionEditableBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying test sessions with edit and delete functionality
 */
class EditableTestSessionAdapter(
    private val onEditClick: (TestSession) -> Unit,
    private val onDeleteClick: (TestSession) -> Unit
) : ListAdapter<TestSession, EditableTestSessionAdapter.TestSessionViewHolder>(TestSessionDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestSessionViewHolder {
        val binding = ItemTestSessionEditableBinding.inflate(
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
        private val binding: ItemTestSessionEditableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(testSession: TestSession) {
            with(binding) {
                // Date
                tvSessionDate.text = dateFormatter.format(Date(testSession.date))
                
                // Duration
                val durationMinutes = testSession.duration / (1000 * 60)
                tvSessionDuration.text = "${durationMinutes} min"
                
                // Results
                val successRate = if (testSession.attemptCount > 0) {
                    (testSession.successCount.toDouble() / testSession.attemptCount.toDouble() * 100).toInt()
                } else 0
                tvSessionResults.text = "${testSession.successCount}/${testSession.attemptCount} (${successRate}%)"
                
                // Test length badge
                val testLengthLabel = when {
                    testSession.duration <= 5 * 60 * 1000L -> "SHORT"
                    testSession.duration <= 15 * 60 * 1000L -> "MEDIUM"
                    else -> "LONG"
                }
                tvTestLengthBadge.text = testLengthLabel
                
                // Notes
                if (testSession.notes.isNullOrBlank()) {
                    tvSessionNotes.visibility = View.GONE
                } else {
                    tvSessionNotes.visibility = View.VISIBLE
                    tvSessionNotes.text = testSession.notes
                }
                
                // Click listeners
                btnEditSession.setOnClickListener {
                    onEditClick(testSession)
                }
                
                btnDeleteSession.setOnClickListener {
                    onDeleteClick(testSession)
                }
                
                // Make the whole item clickable for editing
                root.setOnClickListener {
                    onEditClick(testSession)
                }
            }
        }
    }

    private class TestSessionDiffCallback : DiffUtil.ItemCallback<TestSession>() {
        override fun areItemsTheSame(oldItem: TestSession, newItem: TestSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TestSession, newItem: TestSession): Boolean {
            return oldItem == newItem
        }
    }
}