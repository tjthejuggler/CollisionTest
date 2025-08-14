package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.databinding.DialogTagSelectionBinding
import com.example.jugglingtracker.ui.adapters.SelectableTag
import com.example.jugglingtracker.ui.adapters.SelectableTagAdapter
import com.example.jugglingtracker.ui.adapters.toSelectableTags
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for selecting tags.
 * Used in pattern creation/editing and filtering workflows.
 */
class TagSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogTagSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagAdapter: SelectableTagAdapter
    private var allTags: List<Tag> = emptyList()
    private var selectedTags: MutableSet<Tag> = mutableSetOf()
    private var onTagsSelected: ((Set<Tag>) -> Unit)? = null

    companion object {
        fun newInstance(
            allTags: List<Tag>,
            selectedTags: Set<Tag> = emptySet(),
            title: String = "Select Tags",
            onTagsSelected: (Set<Tag>) -> Unit
        ): TagSelectionDialogFragment {
            return TagSelectionDialogFragment().apply {
                this.allTags = allTags
                this.selectedTags = selectedTags.toMutableSet()
                this.onTagsSelected = onTagsSelected
                arguments = Bundle().apply {
                    putString("title", title)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTagSelectionBinding.inflate(layoutInflater)
        
        val title = arguments?.getString("title") ?: "Select Tags"
        
        setupRecyclerView()
        setupButtons()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("Done") { _, _ ->
                onTagsSelected?.invoke(selectedTags)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All") { _, _ ->
                selectedTags.clear()
                onTagsSelected?.invoke(selectedTags)
            }
            .create()
    }

    private fun setupRecyclerView() {
        tagAdapter = SelectableTagAdapter { tag, isSelected ->
            if (isSelected) {
                selectedTags.add(tag)
            } else {
                selectedTags.remove(tag)
            }
            updateSelectedCount()
        }

        binding.recyclerViewTags.apply {
            adapter = tagAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Submit initial data
        val selectableTags = allTags.toSelectableTags(selectedTags)
        tagAdapter.submitList(selectableTags)
        
        updateSelectedCount()
        updateEmptyState()
    }

    private fun setupButtons() {
        binding.buttonSelectAll.setOnClickListener {
            selectedTags.addAll(allTags)
            tagAdapter.updateSelection(selectedTags)
            updateSelectedCount()
        }

        binding.buttonClearAll.setOnClickListener {
            selectedTags.clear()
            tagAdapter.updateSelection(selectedTags)
            updateSelectedCount()
        }
    }

    private fun updateSelectedCount() {
        binding.textSelectedCount.text = "Selected: ${selectedTags.size}"
    }

    private fun updateEmptyState() {
        if (allTags.isEmpty()) {
            binding.textEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewTags.visibility = android.view.View.GONE
            binding.buttonSelectAll.visibility = android.view.View.GONE
            binding.buttonClearAll.visibility = android.view.View.GONE
        } else {
            binding.textEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewTags.visibility = android.view.View.VISIBLE
            binding.buttonSelectAll.visibility = android.view.View.VISIBLE
            binding.buttonClearAll.visibility = android.view.View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}