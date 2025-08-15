package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.databinding.DialogTagSelectionBinding
import com.example.jugglingtracker.ui.adapters.SelectableTag
import com.example.jugglingtracker.ui.adapters.SelectableTagAdapter
import com.example.jugglingtracker.ui.adapters.toSelectableTags
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Dialog fragment for selecting tags.
 * Used in pattern creation/editing and filtering workflows.
 */
class TagSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogTagSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagAdapter: SelectableTagAdapter
    private var allTags: List<Tag> = emptyList()
    private var filteredTags: List<Tag> = emptyList()
    private var selectedTags: MutableSet<Tag> = mutableSetOf()
    private var onTagsSelected: ((Set<Tag>) -> Unit)? = null
    private var onCreateTag: ((String, Int) -> Unit)? = null

    companion object {
        fun newInstance(
            allTags: List<Tag>,
            selectedTags: Set<Tag> = emptySet(),
            title: String = "Select Tags",
            onTagsSelected: (Set<Tag>) -> Unit,
            onCreateTag: ((String, Int) -> Unit)? = null
        ): TagSelectionDialogFragment {
            return TagSelectionDialogFragment().apply {
                this.allTags = allTags
                this.filteredTags = allTags
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
        setupSearchBar()
        setupCreateTagSection()
        setupButtons()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .create()
    }

    private fun setupRecyclerView() {
        tagAdapter = SelectableTagAdapter { tag, isSelected ->
            if (isSelected) {
                selectedTags.add(tag)
            } else {
                selectedTags.remove(tag)
            }
        }

        binding.rvSelectableTags.apply {
            adapter = tagAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Submit initial data
        updateTagsList()
    }

    private fun setupSearchBar() {
        binding.etSearchTags.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTags(s?.toString() ?: "")
            }
        })
    }

    private fun setupCreateTagSection() {
        binding.btnCreateTag.setOnClickListener {
            val tagName = binding.etNewTagName.text.toString().trim()
            if (tagName.isNotEmpty()) {
                createNewTag(tagName)
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancelTags.setOnClickListener {
            dismiss()
        }

        binding.btnApplyTags.setOnClickListener {
            onTagsSelected?.invoke(selectedTags)
            dismiss()
        }
    }

    private fun filterTags(query: String) {
        filteredTags = if (query.isEmpty()) {
            allTags
        } else {
            allTags.filter { tag ->
                tag.name.contains(query, ignoreCase = true)
            }
        }
        updateTagsList()
    }

    private fun updateTagsList() {
        val selectableTags = filteredTags.toSelectableTags(selectedTags)
        tagAdapter.submitList(selectableTags)
        
        // Show/hide empty state
        if (filteredTags.isEmpty()) {
            binding.rvSelectableTags.visibility = View.GONE
            // Could add empty state view if needed
        } else {
            binding.rvSelectableTags.visibility = View.VISIBLE
        }
    }

    private fun createNewTag(tagName: String) {
        // Check if tag already exists
        val existingTag = allTags.find { it.name.equals(tagName, ignoreCase = true) }
        if (existingTag != null) {
            Snackbar.make(binding.root, "Tag already exists", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Create new tag (in a real app, this would involve the repository)
        val newTag = Tag(
            id = 0L, // Will be assigned by database
            name = tagName,
            color = generateRandomColor()
        )

        // Add to lists
        allTags = allTags + newTag
        filteredTags = filteredTags + newTag
        selectedTags.add(newTag)

        // Update UI
        updateTagsList()
        binding.etNewTagName.text?.clear()
        
        Snackbar.make(binding.root, "Tag created and selected", Snackbar.LENGTH_SHORT).show()
    }

    private fun generateRandomColor(): Int {
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
        )
        return Color.parseColor(colors.random())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}