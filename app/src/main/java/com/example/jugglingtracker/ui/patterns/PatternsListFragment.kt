package com.example.jugglingtracker.ui.patterns

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.JugglingTrackerApplication
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.FragmentPatternsListBinding
import com.example.jugglingtracker.ui.ViewModelFactory
import com.example.jugglingtracker.ui.adapters.PatternListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PatternsListFragment : Fragment() {

    private var _binding: FragmentPatternsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PatternsListViewModel by viewModels {
        val app = requireActivity().application as JugglingTrackerApplication
        ViewModelFactory(
            app.patternRepository,
            app.testSessionRepository,
            app.tagRepository
        )
    }

    private lateinit var patternAdapter: PatternListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatternsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.patterns_list_menu, menu)
        
        // Setup search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.updateSearchQuery(newText ?: "")
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_name -> {
                viewModel.updateSortOption(SortOption.NAME)
                true
            }
            R.id.action_sort_difficulty -> {
                viewModel.updateSortOption(SortOption.DIFFICULTY_ASC)
                true
            }
            R.id.action_sort_balls -> {
                viewModel.updateSortOption(SortOption.NUM_BALLS)
                true
            }
            R.id.action_sort_recent -> {
                viewModel.updateSortOption(SortOption.RECENT)
                true
            }
            R.id.action_clear_filters -> {
                viewModel.clearFilters()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        patternAdapter = PatternListAdapter(
            onPatternClick = { pattern ->
                val action = PatternsListFragmentDirections
                    .actionPatternsListToPatternDetail(pattern.id)
                findNavController().navigate(action)
            },
            onPatternLongClick = { pattern ->
                showPatternOptionsDialog(pattern)
            },
            onDeleteClick = { pattern ->
                showDeleteConfirmationDialog(pattern)
            }
        )

        binding.recyclerViewPatterns.apply {
            adapter = patternAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddPattern.setOnClickListener {
            val action = PatternsListFragmentDirections
                .actionPatternsListToAddEditPattern(-1L) // -1 indicates new pattern
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe patterns
                launch {
                    viewModel.patterns.collect { patterns ->
                        patternAdapter.submitList(patterns)
                        
                        // Show/hide empty state
                        if (patterns.isEmpty()) {
                            binding.textEmptyState.visibility = View.VISIBLE
                            binding.recyclerViewPatterns.visibility = View.GONE
                        } else {
                            binding.textEmptyState.visibility = View.GONE
                            binding.recyclerViewPatterns.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        // Show/hide loading
                        binding.progressBar.visibility = if (state.isLoading) {
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

    private fun showPatternOptionsDialog(pattern: com.example.jugglingtracker.data.entities.Pattern) {
        val options = arrayOf("View Details", "Edit", "Clone", "Delete")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(pattern.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // View Details
                        val action = PatternsListFragmentDirections
                            .actionPatternsListToPatternDetail(pattern.id)
                        findNavController().navigate(action)
                    }
                    1 -> { // Edit
                        val action = PatternsListFragmentDirections
                            .actionPatternsListToAddEditPattern(pattern.id)
                        findNavController().navigate(action)
                    }
                    2 -> { // Clone
                        showClonePatternDialog(pattern)
                    }
                    3 -> { // Delete
                        showDeleteConfirmationDialog(pattern)
                    }
                }
            }
            .show()
    }

    private fun showClonePatternDialog(pattern: com.example.jugglingtracker.data.entities.Pattern) {
        // This would be implemented as part of the key workflows
        // For now, navigate to add/edit with clone flag
        val action = PatternsListFragmentDirections
            .actionPatternsListToAddEditPattern(-1L) // Will be enhanced for cloning
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmationDialog(pattern: com.example.jugglingtracker.data.entities.Pattern) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Pattern")
            .setMessage("Are you sure you want to delete \"${pattern.name}\"? This will also delete all associated test sessions.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePattern(pattern)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}