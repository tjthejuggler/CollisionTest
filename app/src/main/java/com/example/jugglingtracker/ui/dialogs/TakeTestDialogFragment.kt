package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.DialogAddTestSessionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for recording a new test session.
 * Implements the "Taking a Test" workflow from the project plan.
 */
class TakeTestDialogFragment : DialogFragment() {

    private var _binding: DialogAddTestSessionBinding? = null
    private val binding get() = _binding!!

    private var onTestSessionCreated: ((Int, Int, Int, String?) -> Unit)? = null

    companion object {
        fun newInstance(
            patternName: String,
            onTestSessionCreated: (durationMinutes: Int, successCount: Int, attemptCount: Int, notes: String?) -> Unit
        ): TakeTestDialogFragment {
            return TakeTestDialogFragment().apply {
                this.onTestSessionCreated = onTestSessionCreated
                arguments = Bundle().apply {
                    putString("pattern_name", patternName)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddTestSessionBinding.inflate(layoutInflater)
        
        val patternName = arguments?.getString("pattern_name") ?: "Pattern"
        
        setupUI()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Record Test Session for $patternName")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                saveTestSession()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun setupUI() {
        // Setup duration spinner with common test lengths
        val durations = arrayOf("1 minute", "2 minutes", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "Custom")
        val durationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = durationAdapter
        
        // Set default to 5 minutes
        binding.spinnerDuration.setSelection(2)
        
        // Setup spinner listener to show/hide custom duration input
        binding.spinnerDuration.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == durations.size - 1) { // Custom selected
                    binding.editTextCustomDuration.visibility = android.view.View.VISIBLE
                    binding.labelCustomDuration.visibility = android.view.View.VISIBLE
                } else {
                    binding.editTextCustomDuration.visibility = android.view.View.GONE
                    binding.labelCustomDuration.visibility = android.view.View.GONE
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Set initial focus on success count
        binding.editTextSuccessCount.requestFocus()
        
        // Add input validation
        binding.editTextAttemptCount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAttemptCount()
            }
        }
    }

    private fun validateAttemptCount() {
        val successCount = binding.editTextSuccessCount.text.toString().toIntOrNull() ?: 0
        val attemptCount = binding.editTextAttemptCount.text.toString().toIntOrNull() ?: 0
        
        if (attemptCount < successCount) {
            binding.editTextAttemptCount.error = "Attempt count cannot be less than success count"
        } else {
            binding.editTextAttemptCount.error = null
        }
    }

    private fun saveTestSession() {
        val durationMinutes = getDurationMinutes()
        val successCount = binding.editTextSuccessCount.text.toString().toIntOrNull() ?: 0
        val attemptCount = binding.editTextAttemptCount.text.toString().toIntOrNull() ?: 0
        val notes = binding.editTextNotes.text.toString().takeIf { it.isNotBlank() }

        // Validate input
        if (attemptCount < successCount) {
            // This should have been caught by validation, but double-check
            return
        }

        if (durationMinutes <= 0) {
            binding.editTextCustomDuration.error = "Please enter a valid duration"
            return
        }

        onTestSessionCreated?.invoke(durationMinutes, successCount, attemptCount, notes)
    }

    private fun getDurationMinutes(): Int {
        val selectedPosition = binding.spinnerDuration.selectedItemPosition
        
        return when (selectedPosition) {
            0 -> 1
            1 -> 2
            2 -> 5
            3 -> 10
            4 -> 15
            5 -> 30
            6 -> { // Custom
                binding.editTextCustomDuration.text.toString().toIntOrNull() ?: 5
            }
            else -> 5
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}