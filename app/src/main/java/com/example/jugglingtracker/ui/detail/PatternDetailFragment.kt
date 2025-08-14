package com.example.jugglingtracker.ui.detail

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.EditText
import android.widget.SeekBar
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jugglingtracker.JugglingTrackerApplication
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.FragmentPatternDetailBinding
import com.example.jugglingtracker.ui.ViewModelFactory
import com.example.jugglingtracker.ui.adapters.SimpleTestSessionAdapter
import com.example.jugglingtracker.ui.adapters.TagChipAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PatternDetailFragment : Fragment() {

    private var _binding: FragmentPatternDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: PatternDetailFragmentArgs by navArgs()

    private val viewModel: PatternDetailViewModel by viewModels {
        val app = requireActivity().application as JugglingTrackerApplication
        ViewModelFactory(
            app.patternRepository,
            app.testSessionRepository,
            app.tagRepository,
            requireContext()
        )
    }

    private lateinit var testSessionAdapter: SimpleTestSessionAdapter
    private lateinit var tagAdapter: TagChipAdapter
    
    // Video playback components
    private var videoView: VideoView? = null
    private var playPauseButton: com.google.android.material.button.MaterialButton? = null
    private var seekBar: SeekBar? = null
    private var durationText: android.widget.TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
    private var isVideoLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatternDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupVideoPlayer()
        setupClickListeners()
        observeViewModel()
        
        // Load pattern data
        viewModel.loadPattern(args.patternId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.pattern_detail_menu, menu)
        
        // Show/hide share video option based on video availability
        val shareVideoItem = menu.findItem(R.id.action_share_video)
        shareVideoItem?.isVisible = viewModel.hasVideo()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                // Navigate to edit pattern
                findNavController().navigateUp() // Placeholder navigation
                true
            }
            R.id.action_clone -> {
                showClonePatternDialog()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_share_video -> {
                shareVideo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerViews() {
        // Test sessions adapter
        testSessionAdapter = SimpleTestSessionAdapter(
            onSessionClick = { testSession ->
                // Could navigate to test session detail if implemented
            },
            onDeleteClick = null // Don't show delete in detail view
        )

        binding.recyclerViewRecentSessions?.apply {
            adapter = testSessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Tags adapter
        tagAdapter = TagChipAdapter { tag ->
            // Tags are read-only in detail view
        }

        binding.recyclerViewTags?.apply {
            adapter = tagAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupVideoPlayer() {
        videoView = binding.videoPlayer
        playPauseButton = binding.btnPlayPause
        seekBar = binding.seekBar
        durationText = binding.tvVideoDuration
        
        // Setup video view listeners
        videoView?.setOnPreparedListener { mediaPlayer ->
            isVideoLoaded = true
            viewModel.setVideoLoading(false)
            
            val duration = mediaPlayer.duration
            seekBar?.max = duration
            durationText?.text = formatDuration(duration.toLong())
            
            // Setup progress tracking
            startProgressTracking()
            
            mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                // Adjust video view aspect ratio if needed
            }
        }
        
        videoView?.setOnErrorListener { _, what, extra ->
            viewModel.setVideoError("Video playback error: $what, $extra")
            true
        }
        
        videoView?.setOnCompletionListener {
            viewModel.stopVideo()
            playPauseButton?.setIconResource(R.drawable.ic_play)
            stopProgressTracking()
        }
        
        // Setup seek bar listener
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isVideoLoaded) {
                    videoView?.seekTo(progress)
                    viewModel.seekTo(progress.toLong())
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        binding.buttonTakeTest?.setOnClickListener {
            showTakeTestDialog()
        }

        binding.buttonViewAllSessions?.setOnClickListener {
            // Navigate to test history
            findNavController().navigateUp() // Placeholder navigation
        }
        
        // Video playback controls
        playPauseButton?.setOnClickListener {
            if (viewModel.videoPlaybackState.value.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe pattern entity
                launch {
                    viewModel.patternEntity.collect { patternEntity ->
                        patternEntity?.let { entity ->
                            updateUI(entity)
                        }
                    }
                }

                // Observe recent test sessions
                launch {
                    viewModel.recentTestSessions.collect { sessions ->
                        testSessionAdapter.submitList(sessions)
                        
                        // Show/hide empty state
                        binding.textNoSessions?.visibility = if (sessions.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        
                        binding.recyclerViewRecentSessions?.visibility = if (sessions.isEmpty()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    }
                }

                // Observe statistics
                launch {
                    viewModel.getSuccessRate().collect { successRate ->
                        binding.textSuccessRate?.text = String.format("%.1f%%", successRate)
                    }
                }

                launch {
                    viewModel.getTotalPracticeTime().collect { totalTime ->
                        val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
                        binding.textTotalPracticeTime?.text = if (hours > 0) {
                            "${hours}h ${minutes}m"
                        } else {
                            "${minutes}m"
                        }
                    }
                }

                launch {
                    viewModel.bestTestSession.collect { bestSession ->
                        bestSession?.let { session ->
                            val successRate = if (session.attemptCount > 0) {
                                (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                            } else 0.0
                            binding.textBestScore?.text = String.format("%.1f%%", successRate)
                        } ?: run {
                            binding.textBestScore?.text = "N/A"
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

                        // Handle navigation after cloning
                        state.clonedPatternId?.let { clonedId ->
                            findNavController().navigateUp() // Navigate back for now
                            viewModel.clearClonedPatternId()
                        }

                        // Handle navigation after deletion
                        if (state.patternDeleted) {
                            findNavController().navigateUp()
                            viewModel.clearPatternDeleted()
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
                
                // Observe video playback state
                launch {
                    viewModel.videoPlaybackState.collect { playbackState ->
                        updateVideoControls(playbackState)
                    }
                }
                
                // Observe video file changes
                launch {
                    viewModel.videoFile.collect { videoFile ->
                        loadVideoFile(videoFile)
                    }
                }
                
                // Load video when URI is available
                launch {
                    viewModel.getVideoUri()?.let { uri ->
                        loadVideo(uri)
                    }
                }
            }
        }
    }

    private fun updateUI(patternEntity: com.example.jugglingtracker.data.entities.PatternEntity) {
        val pattern = patternEntity.pattern
        
        binding.apply {
            // Pattern details
            textPatternName?.text = pattern.name
            textDifficulty?.text = "Difficulty: ${pattern.difficulty}/10"
            textNumBalls?.text = "Balls: ${pattern.numBalls}"
            
            // Last tested
            textLastTested?.text = if (pattern.lastTested != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                "Last tested: ${dateFormat.format(Date(pattern.lastTested))}"
            } else {
                "Never tested"
            }

            // Tags
            tagAdapter.submitList(patternEntity.tags)
            
            // Show/hide tags section
            if (patternEntity.tags.isEmpty()) {
                labelTags?.visibility = View.GONE
                recyclerViewTags?.visibility = View.GONE
            } else {
                labelTags?.visibility = View.VISIBLE
                recyclerViewTags?.visibility = View.VISIBLE
            }

            // Prerequisites, dependents, related patterns
            updateRelationships(patternEntity)
        }
    }

    private fun updateRelationships(patternEntity: com.example.jugglingtracker.data.entities.PatternEntity) {
        binding.apply {
            // Prerequisites
            if (patternEntity.prerequisites.isNotEmpty()) {
                textPrerequisites?.text = patternEntity.prerequisites.joinToString(", ") { it.name }
                labelPrerequisites?.visibility = View.VISIBLE
                textPrerequisites?.visibility = View.VISIBLE
            } else {
                labelPrerequisites?.visibility = View.GONE
                textPrerequisites?.visibility = View.GONE
            }

            // Dependents
            if (patternEntity.dependents.isNotEmpty()) {
                textDependents?.text = patternEntity.dependents.joinToString(", ") { it.name }
                labelDependents?.visibility = View.VISIBLE
                textDependents?.visibility = View.VISIBLE
            } else {
                labelDependents?.visibility = View.GONE
                textDependents?.visibility = View.GONE
            }

            // Related patterns
            if (patternEntity.relatedPatterns.isNotEmpty()) {
                textRelatedPatterns?.text = patternEntity.relatedPatterns.joinToString(", ") { it.name }
                labelRelatedPatterns?.visibility = View.VISIBLE
                textRelatedPatterns?.visibility = View.VISIBLE
            } else {
                labelRelatedPatterns?.visibility = View.GONE
                textRelatedPatterns?.visibility = View.GONE
            }
        }
    }

    private fun showTakeTestDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_test_session, null)
        
        val editDuration = dialogView.findViewById<EditText>(R.id.editTextDuration)
        val editSuccessCount = dialogView.findViewById<EditText>(R.id.editTextSuccessCount)
        val editAttemptCount = dialogView.findViewById<EditText>(R.id.editTextAttemptCount)
        val editNotes = dialogView.findViewById<EditText>(R.id.editTextNotes)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Record Test Session")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val duration = editDuration.text.toString().toIntOrNull() ?: 5
                val successCount = editSuccessCount.text.toString().toIntOrNull() ?: 0
                val attemptCount = editAttemptCount.text.toString().toIntOrNull() ?: 0
                val notes = editNotes.text.toString().takeIf { it.isNotBlank() }

                viewModel.createTestSession(duration, successCount, attemptCount, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClonePatternDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter new pattern name"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clone Pattern")
            .setView(editText)
            .setPositiveButton("Clone") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank()) {
                    viewModel.clonePattern(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadVideoFile(videoFile: java.io.File?) {
        if (videoFile != null && videoFile.exists()) {
            val uri = Uri.fromFile(videoFile)
            loadVideo(uri)
        } else {
            // Hide video player if no video
            binding.videoPlayer.visibility = View.GONE
        }
    }

    private fun loadVideo(uri: Uri) {
        viewModel.setVideoLoading(true)
        videoView?.setVideoURI(uri)
        binding.videoPlayer.visibility = View.VISIBLE
    }

    private fun playVideo() {
        if (isVideoLoaded) {
            videoView?.start()
            viewModel.playVideo()
            playPauseButton?.setIconResource(R.drawable.ic_pause)
            startProgressTracking()
        }
    }

    private fun pauseVideo() {
        videoView?.pause()
        viewModel.pauseVideo()
        playPauseButton?.setIconResource(R.drawable.ic_play)
        stopProgressTracking()
    }

    private fun updateVideoControls(playbackState: com.example.jugglingtracker.ui.detail.VideoPlaybackState) {
        when {
            playbackState.isLoading -> {
                // Show loading state
                playPauseButton?.isEnabled = false
            }
            playbackState.error != null -> {
                // Show error state
                Snackbar.make(binding.root, playbackState.error, Snackbar.LENGTH_LONG).show()
                viewModel.clearVideoError()
            }
            else -> {
                playPauseButton?.isEnabled = true
                val iconRes = if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                playPauseButton?.setIconResource(iconRes)
            }
        }
    }

    private fun startProgressTracking() {
        updateProgressRunnable = object : Runnable {
            override fun run() {
                videoView?.let { video ->
                    if (video.isPlaying) {
                        val currentPosition = video.currentPosition
                        seekBar?.progress = currentPosition
                        viewModel.updateVideoPosition(currentPosition.toLong())
                        handler.postDelayed(this, 100) // Update every 100ms
                    }
                }
            }
        }
        handler.post(updateProgressRunnable!!)
    }

    private fun stopProgressTracking() {
        updateProgressRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun shareVideo() {
        val shareIntent = viewModel.shareVideo()
        if (shareIntent != null) {
            startActivity(Intent.createChooser(shareIntent, "Share Video"))
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Pattern")
            .setMessage("Are you sure you want to delete this pattern? This will also delete all associated test sessions.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePattern()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopProgressTracking()
        videoView?.stopPlayback()
        _binding = null
    }
    
    override fun onPause() {
        super.onPause()
        if (viewModel.videoPlaybackState.value.isPlaying) {
            pauseVideo()
        }
    }
}