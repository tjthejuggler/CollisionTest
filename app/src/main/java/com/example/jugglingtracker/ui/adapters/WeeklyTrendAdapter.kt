package com.example.jugglingtracker.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.databinding.ItemWeeklyTrendBinding
import com.example.jugglingtracker.ui.stats.WeeklyTrendItem

class WeeklyTrendAdapter : ListAdapter<WeeklyTrendItem, WeeklyTrendAdapter.WeeklyTrendViewHolder>(WeeklyTrendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyTrendViewHolder {
        val binding = ItemWeeklyTrendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WeeklyTrendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeeklyTrendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WeeklyTrendViewHolder(
        private val binding: ItemWeeklyTrendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WeeklyTrendItem) {
            binding.apply {
                textWeekLabel.text = item.weekLabel
                textWeekDate.text = item.weekDateRange
                textWeekPoints.text = "${item.points} pts"
                
                // Set level indicator color
                try {
                    val color = Color.parseColor(item.level.color)
                    viewLevelIndicator.setBackgroundColor(color)
                } catch (e: IllegalArgumentException) {
                    // Fallback to default color if parsing fails
                    viewLevelIndicator.setBackgroundColor(Color.GRAY)
                }
                
                // Highlight current week
                if (item.isCurrentWeek) {
                    textWeekLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    textWeekPoints.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                } else {
                    textWeekLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    textWeekPoints.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
            }
        }
    }

    private class WeeklyTrendDiffCallback : DiffUtil.ItemCallback<WeeklyTrendItem>() {
        override fun areItemsTheSame(oldItem: WeeklyTrendItem, newItem: WeeklyTrendItem): Boolean {
            return oldItem.weekDateRange == newItem.weekDateRange
        }

        override fun areContentsTheSame(oldItem: WeeklyTrendItem, newItem: WeeklyTrendItem): Boolean {
            return oldItem == newItem
        }
    }
}