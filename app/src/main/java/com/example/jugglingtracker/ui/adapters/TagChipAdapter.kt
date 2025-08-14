package com.example.jugglingtracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.databinding.ItemTagBinding

/**
 * Adapter for displaying tags as chips in a horizontal list
 */
class TagChipAdapter(
    private val onTagClick: (Tag) -> Unit
) : ListAdapter<Tag, TagChipAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(
        private val binding: ItemTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag) {
            binding.apply {
                textTagName?.text = tag.name
                
                // Set tag color if available
                tag.color?.let { color ->
                    try {
                        val colorInt = android.graphics.Color.parseColor(color)
                        tagColorIndicator?.setBackgroundColor(colorInt)
                        tagColorIndicator?.visibility = android.view.View.VISIBLE
                    } catch (e: IllegalArgumentException) {
                        // Invalid color format, hide indicator
                        tagColorIndicator?.visibility = android.view.View.GONE
                    }
                } ?: run {
                    tagColorIndicator?.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onTagClick(tag)
                }
            }
        }
    }

    private class TagDiffCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem == newItem
        }
    }
}