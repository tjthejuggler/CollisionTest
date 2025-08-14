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
            app.tagRepository,
            requireContext()
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

        binding.rvTags.apply {
            adapter = tagAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Add new tag
        binding.btnAddTag.setOnClickListener {
            showAddEditTagDialog(isEdit = false)
        }

        // Export data
        binding.layoutExportData.setOnClickListener {
            viewModel.exportData()
        }

        // Import data
        binding.layoutImportData.setOnClickListener {
            viewModel.importData()
        }

        // Theme setting (placeholder)
        binding.tvThemeSummary.setOnClickListener {
            showThemeSelectionDialog()
        }

        // Video quality setting (placeholder)
        binding.tvVideoQualitySummary.setOnClickListener {
            showVideoQualitySelectionDialog()
        }

        // Privacy policy (placeholder)
        binding.layoutPrivacyPolicy.setOnClickListener {
            // Open privacy policy
            Snackbar.make(binding.root, "Privacy policy would open here", Snackbar.LENGTH_SHORT).show()
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
                        if (tags.isEmpty()) {
                            binding.emptyTagsState.visibility = View.VISIBLE
                            binding.rvTags.visibility = View.GONE
                        } else {
                            binding.emptyTagsState.visibility = View.GONE
                            binding.rvTags.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
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
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    viewModel.updateNewTagName(tagName)
                    
                    if (isEdit) {
                        viewModel.updateTag()
                    } else {
                        viewModel.createTag()
                    }
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

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("System Default", "Light", "Dark")
        var selectedTheme = 0 // Default to system

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, selectedTheme) { _, which ->
                selectedTheme = which
            }
            .setPositiveButton("Apply") { _, _ ->
                binding.tvThemeSummary.text = themes[selectedTheme]
                Snackbar.make(binding.root, "Theme changed to ${themes[selectedTheme]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVideoQualitySelectionDialog() {
        val qualities = arrayOf("Low", "Medium", "High", "Ultra")
        var selectedQuality = 2 // Default to High

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Video Quality")
            .setSingleChoiceItems(qualities, selectedQuality) { _, which ->
                selectedQuality = which
            }
            .setPositiveButton("Apply") { _, _ ->
                binding.tvVideoQualitySummary.text = qualities[selectedQuality]
                Snackbar.make(binding.root, "Video quality set to ${qualities[selectedQuality]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}