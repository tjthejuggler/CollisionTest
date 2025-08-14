package com.example.jugglingtracker.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.databinding.ItemTagBinding
import com.example.jugglingtracker.databinding.ItemSelectableTagBinding

/**
 * RecyclerView adapter for displaying tags in a list.
 */
class TagAdapter(
    private val onTagClick: (Tag) -> Unit = {},
    private val onTagLongClick: (Tag) -> Unit = {},
    private val onEditClick: ((Tag) -> Unit)? = null,
    private val onDeleteClick: ((Tag) -> Unit)? = null
) : ListAdapter<Tag, TagAdapter.TagViewHolder>(TagDiffCallback()) {

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
                // Tag name
                tvTagName.text = tag.name

                // Tag color indicator
                viewTagColor.setBackgroundColor(tag.color)

                // Click listeners
                root.setOnClickListener {
                    onTagClick(tag)
                }

                root.setOnLongClickListener {
                    onTagLongClick(tag)
                    true
                }

                // Delete button
                btnDeleteTag.setOnClickListener {
                    onDeleteClick?.invoke(tag)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class TagDiffCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Adapter for selectable tags (used in pattern creation/editing)
 */
class SelectableTagAdapter(
    private val onTagSelectionChanged: (Tag, Boolean) -> Unit
) : ListAdapter<SelectableTag, SelectableTagAdapter.SelectableTagViewHolder>(SelectableTagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableTagViewHolder {
        val binding = ItemSelectableTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectableTagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectableTagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SelectableTagViewHolder(
        private val binding: ItemSelectableTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(selectableTag: SelectableTag) {
            val tag = selectableTag.tag
            
            binding.apply {
                // Tag name
                tvTagName.text = tag.name

                // Tag color indicator
                viewTagColor.setBackgroundColor(tag.color)

                // Checkbox state
                checkboxTag.isChecked = selectableTag.isSelected

                // Set background color based on selection
                val backgroundColor = if (selectableTag.isSelected) {
                    Color.argb(
                        100, // More opaque when selected
                        Color.red(tag.color),
                        Color.green(tag.color),
                        Color.blue(tag.color)
                    )
                } else {
                    Color.argb(
                        30, // Less opaque when not selected
                        Color.red(tag.color),
                        Color.green(tag.color),
                        Color.blue(tag.color)
                    )
                }
                root.setBackgroundColor(backgroundColor)

                // Click listeners
                root.setOnClickListener {
                    val newSelection = !selectableTag.isSelected
                    onTagSelectionChanged(tag, newSelection)
                }

                checkboxTag.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != selectableTag.isSelected) {
                        onTagSelectionChanged(tag, isChecked)
                    }
                }
            }
        }
    }

    /**
     * Update the selection state of tags
     */
    fun updateSelection(selectedTags: Set<Tag>) {
        val currentList = currentList.toMutableList()
        var hasChanges = false

        for (i in currentList.indices) {
            val selectableTag = currentList[i]
            val shouldBeSelected = selectedTags.contains(selectableTag.tag)
            
            if (selectableTag.isSelected != shouldBeSelected) {
                currentList[i] = selectableTag.copy(isSelected = shouldBeSelected)
                hasChanges = true
            }
        }

        if (hasChanges) {
            submitList(currentList)
        }
    }

    private class SelectableTagDiffCallback : DiffUtil.ItemCallback<SelectableTag>() {
        override fun areItemsTheSame(oldItem: SelectableTag, newItem: SelectableTag): Boolean {
            return oldItem.tag.id == newItem.tag.id
        }

        override fun areContentsTheSame(oldItem: SelectableTag, newItem: SelectableTag): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Simple chip-style adapter for displaying selected tags
 */
class TagChipAdapter(
    private val onTagRemove: (Tag) -> Unit
) : ListAdapter<Tag, TagChipAdapter.TagChipViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagChipViewHolder {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagChipViewHolder(
        private val binding: ItemTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag) {
            binding.apply {
                // Tag name
                tvTagName.text = tag.name

                // Tag color
                viewTagColor.setBackgroundColor(tag.color)
                
                // Chip-style background
                root.setBackgroundColor(Color.argb(
                    80,
                    Color.red(tag.color),
                    Color.green(tag.color),
                    Color.blue(tag.color)
                ))

                // Use delete button for remove functionality
                btnDeleteTag.setOnClickListener {
                    onTagRemove(tag)
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

/**
 * Data class for selectable tags
 */
data class SelectableTag(
    val tag: Tag,
    val isSelected: Boolean = false
)

/**
 * Extension function to convert tags to selectable tags
 */
fun List<Tag>.toSelectableTags(selectedTags: Set<Tag> = emptySet()): List<SelectableTag> {
    return this.map { tag ->
        SelectableTag(
            tag = tag,
            isSelected = selectedTags.contains(tag)
        )
    }
}