package com.example.jugglingtracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.databinding.ItemTestSessionBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple adapter for displaying test sessions in a list
 */
class SimpleTestSessionAdapter(
    private val onSessionClick: (TestSession) -> Unit,
    private val onDeleteClick: ((TestSession) -> Unit)? = null
) : ListAdapter<TestSession, SimpleTestSessionAdapter.TestSessionViewHolder>(TestSessionDiffCallback()) {

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

        fun bind(testSession: TestSession) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val successRate = if (testSession.attemptCount > 0) {
                (testSession.successCount.toDouble() / testSession.attemptCount.toDouble()) * 100
            } else 0.0

            binding.apply {
                textDate?.text = dateFormat.format(Date(testSession.date))
                textSuccessRate?.text = String.format("%.1f%%", successRate)
                textAttempts?.text = "${testSession.successCount}/${testSession.attemptCount}"
                textDuration?.text = "${testSession.duration / (60 * 1000)}min"
                
                // Show notes if available
                if (!testSession.notes.isNullOrBlank()) {
                    textNotes?.text = testSession.notes
                    textNotes?.visibility = android.view.View.VISIBLE
                } else {
                    textNotes?.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onSessionClick(testSession)
                }

                // Show delete button only if callback is provided
                buttonDelete?.let { deleteButton ->
                    if (onDeleteClick != null) {
                        deleteButton.visibility = android.view.View.VISIBLE
                        deleteButton.setOnClickListener {
                            onDeleteClick.invoke(testSession)
                        }
                    } else {
                        deleteButton.visibility = android.view.View.GONE
                    }
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