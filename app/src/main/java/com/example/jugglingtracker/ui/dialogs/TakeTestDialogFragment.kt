package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.DialogAddTestSessionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

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
        setupButtons()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Record Test Session for $patternName")
            .setView(binding.root)
            .create()
    }

    private fun setupUI() {
        // Set default test length to short (first chip)
        binding.chipShortTest.isChecked = true
        
        // Set initial focus on success count
        binding.etSuccessCount.requestFocus()
        
        // Add input validation
        binding.etTotalAttempts.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAttemptCount()
            }
        }

        // Set up chip group listener to update duration field
        binding.chipGroupTestLength.setOnCheckedStateChangeListener { group, checkedIds ->
            updateDurationFromChip()
        }

        // Initialize duration based on default selection
        updateDurationFromChip()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveTestSession()
        }
    }

    private fun updateDurationFromChip() {
        val checkedChipId = binding.chipGroupTestLength.checkedChipId
        val durationMinutes = when (checkedChipId) {
            R.id.chip_short_test -> 5
            R.id.chip_medium_test -> 15
            R.id.chip_long_test -> 30
            else -> 5
        }
        binding.etDuration.setText(durationMinutes.toString())
    }

    private fun validateAttemptCount() {
        val successCount = binding.etSuccessCount.text.toString().toIntOrNull() ?: 0
        val attemptCount = binding.etTotalAttempts.text.toString().toIntOrNull() ?: 0
        
        if (attemptCount < successCount) {
            binding.tilTotalAttempts.error = "Attempt count cannot be less than success count"
        } else {
            binding.tilTotalAttempts.error = null
        }
    }

    private fun saveTestSession() {
        val durationMinutes = binding.etDuration.text.toString().toIntOrNull() ?: 5
        val successCount = binding.etSuccessCount.text.toString().toIntOrNull() ?: 0
        val attemptCount = binding.etTotalAttempts.text.toString().toIntOrNull() ?: 0
        val notes = binding.etNotes.text.toString().takeIf { it.isNotBlank() }

        // Validate input
        if (attemptCount < successCount) {
            binding.tilTotalAttempts.error = "Attempt count cannot be less than success count"
            return
        }

        if (durationMinutes <= 0) {
            binding.tilDuration.error = "Please enter a valid duration"
            return
        }

        if (successCount < 0) {
            binding.tilSuccessCount.error = "Success count cannot be negative"
            return
        }

        if (attemptCount <= 0) {
            binding.tilTotalAttempts.error = "Attempt count must be greater than 0"
            return
        }

        // Clear any existing errors
        binding.tilDuration.error = null
        binding.tilSuccessCount.error = null
        binding.tilTotalAttempts.error = null

        onTestSessionCreated?.invoke(durationMinutes, successCount, attemptCount, notes)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}