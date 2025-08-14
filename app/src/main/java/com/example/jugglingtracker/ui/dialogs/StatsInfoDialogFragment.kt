package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.DialogStatsInfoBinding

class StatsInfoDialogFragment : DialogFragment() {

    private var _binding: DialogStatsInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogStatsInfoBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.confirm_ok) { _, _ ->
                dismiss()
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StatsInfoDialogFragment"

        fun newInstance(): StatsInfoDialogFragment {
            return StatsInfoDialogFragment()
        }
    }
}