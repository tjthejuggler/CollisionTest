package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
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
        
        // Set up chip group listener to update max test duration
        binding.chipGroupTestLength.setOnCheckedStateChangeListener { group, checkedIds ->
            updateMaxDurationFromChip()
        }

        // Initialize max duration based on default selection
        updateMaxDurationFromChip()
        
        // Setup timer controls
        setupTimerControls()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            stopAllTimers()
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveTestSession()
        }
    }
    
    private fun setupTimerControls() {
        binding.btnStartTimer.setOnClickListener {
            startCountdown()
        }
        
        binding.btnStopTimer.setOnClickListener {
            stopTimer()
        }
        
        binding.btnCancelTimer.setOnClickListener {
            cancelTimer()
        }
        
        binding.btnResetTimer.setOnClickListener {
            resetTimer()
        }
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
        binding.btnStartTimer.isEnabled = false
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        
        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                binding.tvCountdownDisplay.text = getString(R.string.timer_countdown_format, secondsLeft)
            }
            
            override fun onFinish() {
                isCountdownRunning = false
                binding.tvCountdownDisplay.visibility = View.GONE
                startTimer()
            }
        }.start()
    }
    
    private fun startTimer() {
        if (isTimerRunning) return
        
        isTimerRunning = true
        elapsedTimeSeconds = 0
        binding.btnStartTimer.isEnabled = false
        binding.btnStopTimer.isEnabled = true
        binding.btnCancelTimer.isEnabled = true
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
        binding.btnStartTimer.isEnabled = true
        binding.btnStopTimer.isEnabled = false
        binding.btnCancelTimer.isEnabled = false
        binding.btnResetTimer.visibility = View.VISIBLE
        binding.tvCountdownDisplay.text = getString(R.string.timer_stopped)
        updateTimerDisplay()
    }
    
    private fun cancelTimer() {
        stopAllTimers()
        elapsedTimeSeconds = 0
        binding.btnStartTimer.isEnabled = true
        binding.btnStopTimer.isEnabled = false
        binding.btnCancelTimer.isEnabled = false
        binding.btnResetTimer.visibility = View.GONE
        binding.tvCountdownDisplay.text = getString(R.string.timer_ready)
        binding.tvCountdownDisplay.visibility = View.VISIBLE
        updateTimerDisplay()
    }
    
    private fun resetTimer() {
        elapsedTimeSeconds = 0
        binding.btnStartTimer.isEnabled = true
        binding.btnStopTimer.isEnabled = false
        binding.btnCancelTimer.isEnabled = false
        binding.btnResetTimer.visibility = View.GONE
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