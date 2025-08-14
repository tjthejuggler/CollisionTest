package com.example.jugglingtracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TestSessionRepository
import com.example.jugglingtracker.data.repository.TagRepository
import com.example.jugglingtracker.data.repository.BackupRepository
import com.example.jugglingtracker.ui.addedit.AddEditPatternViewModel
import com.example.jugglingtracker.ui.detail.PatternDetailViewModel
import com.example.jugglingtracker.ui.history.TestHistoryViewModel
import com.example.jugglingtracker.ui.patterns.PatternsListViewModel
import com.example.jugglingtracker.ui.progress.ProgressChartViewModel
import com.example.jugglingtracker.ui.settings.SettingsViewModel

/**
 * ViewModelFactory for creating ViewModels with repository dependencies.
 * This factory provides proper dependency injection for all ViewModels in the app.
 */
class ViewModelFactory(
    private val patternRepository: PatternRepository,
    private val testSessionRepository: TestSessionRepository,
    private val tagRepository: TagRepository,
    private val backupRepository: BackupRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PatternsListViewModel::class.java) -> {
                PatternsListViewModel(patternRepository, tagRepository) as T
            }
            modelClass.isAssignableFrom(PatternDetailViewModel::class.java) -> {
                PatternDetailViewModel(patternRepository, testSessionRepository, context) as T
            }
            modelClass.isAssignableFrom(AddEditPatternViewModel::class.java) -> {
                AddEditPatternViewModel(patternRepository, tagRepository, context) as T
            }
            modelClass.isAssignableFrom(ProgressChartViewModel::class.java) -> {
                ProgressChartViewModel(testSessionRepository) as T
            }
            modelClass.isAssignableFrom(TestHistoryViewModel::class.java) -> {
                TestHistoryViewModel(testSessionRepository, patternRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(tagRepository, backupRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}