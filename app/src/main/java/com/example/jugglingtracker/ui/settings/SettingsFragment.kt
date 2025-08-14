package com.example.jugglingtracker.ui.settings

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.JugglingTrackerApplication
import com.example.jugglingtracker.R
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.databinding.FragmentSettingsBinding
import com.example.jugglingtracker.ui.ViewModelFactory
import com.example.jugglingtracker.ui.adapters.TagAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as JugglingTrackerApplication
        ViewModelFactory(
            app.patternRepository,
            app.testSessionRepository,
            app.tagRepository
        )
    }

    private lateinit var tagAdapter: TagAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTagsSection()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupTagsSection() {
        // Setup tags RecyclerView
        tagAdapter = TagAdapter(
            onTagClick = { tag ->
                // Could show tag details or usage
            },
            onTagLongClick = { tag ->
                showTagOptionsDialog(tag)
            },
            onEditClick = { tag ->
                viewModel.startEditingTag(tag)
                showAddEditTagDialog(isEdit = true)
            },
            onDeleteClick = { tag ->
                showDeleteTagConfirmationDialog(tag)
            }
        )

        binding.recyclerViewTags?.apply {
            adapter = tagAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Add new tag
        binding.buttonAddTag?.setOnClickListener {
            showAddEditTagDialog(isEdit = false)
        }

        // Export data
        binding.buttonExportData?.setOnClickListener {
            viewModel.exportData()
        }

        // Import data
        binding.buttonImportData?.setOnClickListener {
            viewModel.importData()
        }

        // Reset data
        binding.buttonResetData?.setOnClickListener {
            showResetDataConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe all tags
                launch {
                    viewModel.allTags.collect { tags ->
                        tagAdapter.submitList(tags)
                        
                        // Show/hide empty state
                        binding.textEmptyTags?.visibility = if (tags.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        
                        binding.recyclerViewTags?.visibility = if (tags.isEmpty()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    }
                }

                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        // Show/hide loading
                        binding.progressBar?.visibility = if (state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        // Show error messages
                        state.error?.let { error ->
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }

                        // Show success messages
                        state.message?.let { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }

    private fun showAddEditTagDialog(isEdit: Boolean) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_2, null)
        
        val editText = EditText(requireContext()).apply {
            hint = "Tag name"
            if (isEdit) {
                setText(viewModel.newTagName.value)
            }
        }

        val title = if (isEdit) "Edit Tag" else "Add New Tag"
        val positiveButtonText = if (isEdit) "Update" else "Add"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val tagName = editText.text.toString()
                viewModel.updateNewTagName(tagName)
                
                if (isEdit) {
                    viewModel.updateTag()
                } else {
                    viewModel.createTag()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (isEdit) {
                    viewModel.cancelEditingTag()
                }
            }
            .show()
    }

    private fun showTagOptionsDialog(tag: Tag) {
        val options = arrayOf("Edit", "Delete")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tag.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Edit
                        viewModel.startEditingTag(tag)
                        showAddEditTagDialog(isEdit = true)
                    }
                    1 -> { // Delete
                        showDeleteTagConfirmationDialog(tag)
                    }
                }
            }
            .show()
    }

    private fun showDeleteTagConfirmationDialog(tag: Tag) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Tag")
            .setMessage("Are you sure you want to delete \"${tag.name}\"? This will remove it from all patterns.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTag(tag)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetDataConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset All Data")
            .setMessage("This will permanently delete all patterns, test sessions, and tags. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.resetAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}