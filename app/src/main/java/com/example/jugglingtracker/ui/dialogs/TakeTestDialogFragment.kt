package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.jugglingtracker.R
import com.example.jugglingtracker.data.entities.TestSession
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
    private var onTestSessionUpdated: ((TestSession) -> Unit)? = null
    private var existingTestSession: TestSession? = null
    
    // Timer related variables
    private var countdownTimer: CountDownTimer? = null
    private var testTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var isCountdownRunning = false
    private var elapsedTimeSeconds = 0
    private var maxTestDurationSeconds = 0

    companion object {
        fun newInstance(
            patternName: String,
            onTestSessionCreated: (durationMinutes: Int, successCount: Int, dropsCount: Int, notes: String?) -> Unit
        ): TakeTestDialogFragment {
            return TakeTestDialogFragment().apply {
                this.onTestSessionCreated = onTestSessionCreated
                arguments = Bundle().apply {
                    putString("pattern_name", patternName)
                }
            }
        }
        
        fun newInstanceForEdit(
            patternName: String,
            testSession: TestSession,
            onTestSessionUpdated: (TestSession) -> Unit
        ): TakeTestDialogFragment {
            return TakeTestDialogFragment().apply {
                this.onTestSessionUpdated = onTestSessionUpdated
                this.existingTestSession = testSession
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
        
        val isEditing = existingTestSession != null
        val title = if (isEditing) "Edit Test Session - $patternName" else "Test Session - $patternName"
        val positiveButtonText = if (isEditing) "Update Test" else "Submit Test"
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                saveTestSession()
            }
            .setNegativeButton("Cancel") { _, _ ->
                stopAllTimers()
                dismiss()
            }
            .setCancelable(true)
            .create()
    }

    private fun setupUI() {
        val existingSession = existingTestSession
        
        if (existingSession != null) {
            // Populate fields with existing data
            populateFieldsFromTestSession(existingSession)
        } else {
            // Set default test length to short (first chip)
            binding.chipShortTest.isChecked = true
            
            // Set default values
            binding.etDropsCount.setText("0")
            
            // Set initial focus on success count
            binding.etSuccessCount.requestFocus()
        }
        
        // Set up chip group listener to update max test duration
        binding.chipGroupTestLength.setOnCheckedStateChangeListener { group, checkedIds ->
            updateMaxDurationFromChip()
        }

        // Initialize max duration based on default selection
        updateMaxDurationFromChip()
        
        // Setup timer controls
        setupTimerControls()
    }
    
    private fun populateFieldsFromTestSession(testSession: TestSession) {
        // Set success count
        binding.etSuccessCount.setText(testSession.successCount.toString())
        
        // Set drops count (attemptCount - successCount)
        val dropsCount = testSession.attemptCount - testSession.successCount
        binding.etDropsCount.setText(dropsCount.toString())
        
        // Set notes
        binding.etNotes.setText(testSession.notes ?: "")
        
        // Set test length based on duration
        val durationMinutes = testSession.duration / (1000 * 60)
        when {
            durationMinutes <= 5 -> binding.chipShortTest.isChecked = true
            durationMinutes <= 15 -> binding.chipMediumTest.isChecked = true
            else -> binding.chipLongTest.isChecked = true
        }
        
        // Set timer display to show the original duration
        elapsedTimeSeconds = (testSession.duration / 1000).toInt()
        updateTimerDisplay()
        binding.tvCountdownDisplay.text = "Original session time"
    }

    private fun setupButtons() {
        // Buttons are now handled by the MaterialAlertDialogBuilder
        // No setup needed for layout buttons since they're hidden
    }
    
    private fun setupTimerControls() {
        // Ensure timer button is visible and enabled
        binding.btnTimerToggle.visibility = View.VISIBLE
        binding.btnTimerToggle.isEnabled = true
        
        binding.btnTimerToggle.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
            } else {
                // Always reset to 0 when starting
                elapsedTimeSeconds = 0
                updateTimerDisplay()
                startCountdown()
            }
        }
        
        // Set up hidden buttons for compatibility
        binding.btnStartTimer.setOnClickListener { /* hidden */ }
        binding.btnStopTimer.setOnClickListener { /* hidden */ }
        binding.btnResetTimer.setOnClickListener { /* hidden */ }
        binding.btnCancelTimer.setOnClickListener { /* hidden */ }
    }

    private fun updateMaxDurationFromChip() {
        val checkedChipId = binding.chipGroupTestLength.checkedChipId
        maxTestDurationSeconds = when (checkedChipId) {
            R.id.chip_short_test -> 5 * 60  // 5 minutes
            R.id.chip_medium_test -> 15 * 60  // 15 minutes
            R.id.chip_long_test -> 30 * 60  // 30 minutes
            else -> 5 * 60
        }
    }
    
    private fun startCountdown() {
        if (isCountdownRunning || isTimerRunning) return
        
        isCountdownRunning = true
        binding.btnTimerToggle.isEnabled = false
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        
        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                binding.tvCountdownDisplay.text = getString(R.string.timer_countdown_format, secondsLeft)
            }
            
            override fun onFinish() {
                isCountdownRunning = false
                binding.btnTimerToggle.isEnabled = true
                binding.tvCountdownDisplay.visibility = View.GONE
                startTimer()
            }
        }.start()
    }
    
    private fun startTimer() {
        if (isTimerRunning) return
        
        isTimerRunning = true
        elapsedTimeSeconds = 0
        binding.btnTimerToggle.text = getString(R.string.button_stop_timer)
        binding.btnTimerToggle.setIconResource(R.drawable.ic_pause)
        binding.tvCountdownDisplay.text = getString(R.string.timer_running)
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        
        testTimer = object : CountDownTimer((maxTestDurationSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTimeSeconds++
                updateTimerDisplay()
            }
            
            override fun onFinish() {
                elapsedTimeSeconds = maxTestDurationSeconds
                updateTimerDisplay()
                stopTimer()
            }
        }.start()
    }
    
    private fun stopTimer() {
        testTimer?.cancel()
        isTimerRunning = false
        binding.btnTimerToggle.text = getString(R.string.button_start_timer)
        binding.btnTimerToggle.setIconResource(R.drawable.ic_play)
        binding.tvCountdownDisplay.text = getString(R.string.timer_stopped)
        updateTimerDisplay()
    }
    
    private fun cancelTimer() {
        stopAllTimers()
        elapsedTimeSeconds = 0
        binding.btnTimerToggle.text = getString(R.string.button_start_timer)
        binding.btnTimerToggle.setIconResource(R.drawable.ic_play)
        binding.tvCountdownDisplay.text = getString(R.string.timer_ready)
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        updateTimerDisplay()
    }
    
    private fun resetTimer() {
        elapsedTimeSeconds = 0
        binding.btnTimerToggle.text = getString(R.string.button_start_timer)
        binding.btnTimerToggle.setIconResource(R.drawable.ic_play)
        binding.tvCountdownDisplay.text = getString(R.string.timer_ready)
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        updateTimerDisplay()
    }
    
    private fun stopAllTimers() {
        countdownTimer?.cancel()
        testTimer?.cancel()
        isCountdownRunning = false
        isTimerRunning = false
    }
    
    private fun updateTimerDisplay() {
        val minutes = elapsedTimeSeconds / 60
        val seconds = elapsedTimeSeconds % 60
        binding.tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }


    private fun saveTestSession() {
        val durationMinutes = if (elapsedTimeSeconds > 0) {
            // Convert elapsed seconds to minutes, rounding up
            (elapsedTimeSeconds + 59) / 60
        } else {
            // If timer wasn't used, default to chip selection
            when (binding.chipGroupTestLength.checkedChipId) {
                R.id.chip_short_test -> 5
                R.id.chip_medium_test -> 15
                R.id.chip_long_test -> 30
                else -> 5
            }
        }
        
        val successCount = binding.etSuccessCount.text.toString().toIntOrNull() ?: 0
        val dropsCount = binding.etDropsCount.text.toString().toIntOrNull() ?: 0
        val notes = binding.etNotes.text.toString().takeIf { it.isNotBlank() }

        // Validate input
        if (successCount < 0) {
            binding.tilSuccessCount.error = "Success count cannot be negative"
            return
        }

        if (dropsCount < 0) {
            binding.tilDropsCount.error = "Drops count cannot be negative"
            return
        }

        // Clear any existing errors
        binding.tilSuccessCount.error = null
        binding.tilDropsCount.error = null

        stopAllTimers()
        onTestSessionCreated?.invoke(durationMinutes, successCount, dropsCount, notes)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAllTimers()
        _binding = null
    }
}