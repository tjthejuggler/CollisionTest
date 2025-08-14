package com.example.jugglingtracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.Pattern
import com.example.jugglingtracker.databinding.ItemPatternBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying patterns in a list.
 */
class PatternListAdapter(
    private val onPatternClick: (Pattern) -> Unit,
    private val onPatternLongClick: (Pattern) -> Unit = {},
    private val onDeleteClick: ((Pattern) -> Unit)? = null
) : ListAdapter<Pattern, PatternListAdapter.PatternViewHolder>(PatternDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatternViewHolder {
        val binding = ItemPatternBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatternViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatternViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PatternViewHolder(
        private val binding: ItemPatternBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pattern: Pattern) {
            binding.apply {
                // Pattern name
                textPatternName.text = pattern.name

                // Difficulty
                textDifficulty.text = pattern.difficulty.toString()
                
                // Number of balls
                textNumBalls.text = pattern.numBalls.toString()

                // Last tested date
                textLastTested.text = if (pattern.lastTested != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(pattern.lastTested))
                } else {
                    "Never tested"
                }

                // Set difficulty background color based on level
                val difficultyColor = when {
                    pattern.difficulty <= 3 -> android.graphics.Color.parseColor("#4CAF50") // Green - Easy
                    pattern.difficulty <= 6 -> android.graphics.Color.parseColor("#FF9800") // Orange - Medium
                    pattern.difficulty <= 8 -> android.graphics.Color.parseColor("#F44336") // Red - Hard
                    else -> android.graphics.Color.parseColor("#9C27B0") // Purple - Expert
                }
                textDifficulty.setBackgroundColor(difficultyColor)

                // Click listeners
                root.setOnClickListener {
                    onPatternClick(pattern)
                }

                root.setOnLongClickListener {
                    onPatternLongClick(pattern)
                    true
                }

                // Delete button (if delete callback is provided)
                onDeleteClick?.let { deleteCallback ->
                    buttonDelete.visibility = android.view.View.VISIBLE
                    buttonDelete.setOnClickListener {
                        deleteCallback(pattern)
                    }
                } ?: run {
                    buttonDelete.visibility = android.view.View.GONE
                }

                // Video indicator
                imageVideoIndicator.visibility = if (pattern.videoUri != null) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class PatternDiffCallback : DiffUtil.ItemCallback<Pattern>() {
        override fun areItemsTheSame(oldItem: Pattern, newItem: Pattern): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pattern, newItem: Pattern): Boolean {
            return oldItem == newItem
        }
    }
}