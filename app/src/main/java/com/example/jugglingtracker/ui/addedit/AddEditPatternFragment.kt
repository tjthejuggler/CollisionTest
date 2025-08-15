package com.example.jugglingtracker.ui.addedit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.jugglingtracker.JugglingTrackerApplication
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.FragmentAddEditPatternBinding
import com.example.jugglingtracker.ui.ViewModelFactory
import com.example.jugglingtracker.ui.adapters.TagChipAdapter
import com.example.jugglingtracker.ui.dialogs.TagSelectionDialogFragment
import com.example.jugglingtracker.ui.dialogs.VideoTrimDialogFragment
import com.example.jugglingtracker.utils.CameraXVideoRecorder
import com.example.jugglingtracker.utils.VideoManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddEditPatternFragment : Fragment() {

    private var _binding: FragmentAddEditPatternBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditPatternFragmentArgs by navArgs()

    private val viewModel: AddEditPatternViewModel by viewModels {
        val app = requireActivity().application as JugglingTrackerApplication
        ViewModelFactory(
            app.patternRepository,
            app.testSessionRepository,
            app.tagRepository,
            app.backupRepository,
            requireContext()
        )
    }

    private lateinit var tagAdapter: TagChipAdapter
    private val isEditMode get() = args.patternId != -1L
    
    // Video recording components
    private var cameraXVideoRecorder: CameraXVideoRecorder? = null
    private var videoManager: VideoManager? = null
    private var isRecording = false
    
    // Activity result launchers
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVideoRecording()
        } else {
            showPermissionDeniedMessage()
        }
    }
    
    private val requestVideoImport = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setVideoFromImport(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditPatternBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupTagsRecyclerView()
        setupVideoComponents()
        setupClickListeners()
        observeViewModel()
        
        // Initialize for edit mode if needed
        if (isEditMode) {
            viewModel.initializeForEdit(args.patternId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Menu will be handled by the save button in the layout
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun setupUI() {
        // Set title
        val title = if (isEditMode) "Edit Pattern" else "Add Pattern"
        requireActivity().title = title
        
        // Setup difficulty seekbar
        binding.seekbarDifficulty.apply {
            max = 9 // 1-10 range (0-9 progress)
            progress = 2 // Default to 3
        }
    }

    private fun setupTagsRecyclerView() {
        tagAdapter = TagChipAdapter { tag ->
            viewModel.removeTag(tag)
        }

        binding.chipGroupTags.removeAllViews()
    }
    
    private fun setupVideoComponents() {
        videoManager = VideoManager(requireContext())
    }

    private fun setupClickListeners() {
        // Name field
        binding.etPatternName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateName(binding.etPatternName.text.toString())
            }
        }

        // Description field
        binding.etPatternDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateDescription(binding.etPatternDescription.text.toString())
            }
        }

        // Difficulty seekbar
        binding.seekbarDifficulty.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val difficulty = progress + 1 // Convert 0-9 to 1-10
                viewModel.updateDifficulty(difficulty)
                binding.tvDifficultyValue.text = difficulty.toString()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Ball count field
        binding.etBallCount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val ballCount = binding.etBallCount.text.toString().toIntOrNull() ?: 3
                viewModel.updateNumBalls(ballCount)
            }
        }

        // Add tags button
        binding.btnSelectTags.setOnClickListener {
            showTagSelectionDialog()
        }

        // Save button
        binding.btnSavePattern.setOnClickListener {
            // Update all fields before saving
            viewModel.updateName(binding.etPatternName.text.toString())
            viewModel.updateDescription(binding.etPatternDescription.text.toString())
            val ballCount = binding.etBallCount.text.toString().toIntOrNull() ?: 3
            viewModel.updateNumBalls(ballCount)
            viewModel.savePattern()
        }
        
        // Video recording button
        binding.btnRecordVideo.setOnClickListener {
            if (isRecording) {
                stopVideoRecording()
            } else {
                checkCameraPermissionAndRecord()
            }
        }
        
        // Video import button
        binding.btnImportVideo.setOnClickListener {
            importVideoFromGallery()
        }
        
        // Video trim button
        binding.btnTrimVideo.setOnClickListener {
            showTrimVideoDialog()
        }
        
        // Video thumbnail click
        binding.ivVideoThumbnail.setOnClickListener {
            // Preview video if available
            viewModel.videoFile.value?.let { videoFile ->
                previewVideo(videoFile)
            }
        }
        
        // Relationship selection buttons
        binding.btnSelectPrerequisites.setOnClickListener {
            showPatternSelectionDialog("Prerequisites") { selectedPatterns ->
                viewModel.updatePrerequisites(selectedPatterns)
            }
        }
        
        binding.btnSelectDependents.setOnClickListener {
            showPatternSelectionDialog("Dependents") { selectedPatterns ->
                viewModel.updateDependents(selectedPatterns)
            }
        }
        
        binding.btnSelectRelated.setOnClickListener {
            showPatternSelectionDialog("Related Patterns") { selectedPatterns ->
                viewModel.updateRelated(selectedPatterns)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe form fields
                launch {
                    viewModel.name.collect { name ->
                        if (binding.etPatternName.text.toString() != name) {
                            binding.etPatternName.setText(name)
                        }
                    }
                }

                launch {
                    viewModel.difficulty.collect { difficulty ->
                        binding.seekbarDifficulty.progress = difficulty - 1 // Convert 1-10 to 0-9
                        binding.tvDifficultyValue.text = difficulty.toString()
                    }
                }

                launch {
                    viewModel.numBalls.collect { numBalls ->
                        binding.etBallCount.setText(numBalls.toString())
                    }
                }

                launch {
                    viewModel.description.collect { description ->
                        if (binding.etPatternDescription.text.toString() != description) {
                            binding.etPatternDescription.setText(description)
                        }
                    }
                }

                // Observe selected tags
                launch {
                    viewModel.selectedTags.collect { tags ->
                        // Update chip group with selected tags
                        binding.chipGroupTags.removeAllViews()
                        tags.forEach { tag ->
                            val chip = com.google.android.material.chip.Chip(requireContext())
                            chip.text = tag.name
                            chip.isCloseIconVisible = true
                            chip.setOnCloseIconClickListener {
                                viewModel.removeTag(tag)
                            }
                            binding.chipGroupTags.addView(chip)
                        }
                    }
                }

                // Observe form validation
                launch {
                    viewModel.isFormValid.collect { isValid ->
                        binding.btnSavePattern.isEnabled = isValid
                    }
                }

                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        // Handle navigation after saving
                        if (state.patternSaved) {
                            findNavController().navigateUp()
                            viewModel.clearSavedPattern()
                        }

                        // Show field errors
                        binding.tilPatternName.error = state.nameError
                        
                        // Show general error messages
                        state.error?.let { error ->
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
                    
                // Observe video-related state
                launch {
                    viewModel.videoFile.collect { videoFile ->
                        updateVideoThumbnail(videoFile)
                        binding.btnTrimVideo.isEnabled = videoFile != null
                    }
                }
                
                launch {
                    viewModel.thumbnailFile.collect { thumbnailFile ->
                        updateVideoThumbnailImage(thumbnailFile)
                    }
                }
                
                launch {
                    viewModel.videoDuration.collect { duration ->
                        updateVideoDurationDisplay(duration)
                    }
                }
                
                launch {
                    viewModel.videoFileSize.collect { fileSize ->
                        updateVideoFileSizeDisplay(fileSize)
                    }
                }
            }
        }
    }

    private fun showTagSelectionDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allTags = viewModel.allTags.value
            val selectedTags = viewModel.selectedTags.value
            
            val dialog = TagSelectionDialogFragment.newInstance(
                allTags = allTags,
                selectedTags = selectedTags,
                title = "Select Tags",
                onTagsSelected = { newSelectedTags ->
                    // Update selected tags
                    newSelectedTags.forEach { tag ->
                        if (!selectedTags.contains(tag)) {
                            viewModel.addTag(tag)
                        }
                    }
                    
                    selectedTags.forEach { tag ->
                        if (!newSelectedTags.contains(tag)) {
                            viewModel.removeTag(tag)
                        }
                    }
                },
                onCreateTag = { tagName, color ->
                    // Create tag through ViewModel
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = viewModel.createAndAddTag(tagName, color)
                        if (result.isFailure) {
                            // Show error message
                            Snackbar.make(
                                binding.root,
                                "Failed to create tag: ${result.exceptionOrNull()?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
            
            dialog.show(parentFragmentManager, "tag_selection")
        }
    }

    private fun showPatternSelectionDialog(title: String, onSelection: (List<com.example.jugglingtracker.data.entities.Pattern>) -> Unit) {
        // For now, show a simple message that this feature is coming soon
        // In a full implementation, this would show a dialog with available patterns to select from
        Snackbar.make(binding.root, "$title selection coming soon!", Snackbar.LENGTH_SHORT).show()
        
        // TODO: Implement pattern selection dialog similar to tag selection
        // This would require:
        // 1. Getting all available patterns from the repository
        // 2. Showing them in a selection dialog
        // 3. Allowing multi-selection
        // 4. Calling onSelection with the selected patterns
    }

    private fun checkCameraPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVideoRecording()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startVideoRecording() {
        showVideoRecordingDialog()
    }
    
    private fun showVideoRecordingDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_video_recording, null)
        
        val previewView = dialogView.findViewById<PreviewView>(R.id.preview_view)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Record Video")
            .setView(dialogView)
            .setPositiveButton("Start Recording") { _, _ ->
                // Will be replaced with actual recording logic
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            
            // Initialize CameraX recorder
            cameraXVideoRecorder = CameraXVideoRecorder(
                requireContext(),
                viewLifecycleOwner,
                previewView
            )
            
            lifecycleScope.launch {
                val initResult = cameraXVideoRecorder?.initializeCamera()
                if (initResult?.isSuccess == true) {
                    positiveButton.setOnClickListener {
                        if (!isRecording) {
                            val videoFile = videoManager?.createVideoFile()
                            if (videoFile != null) {
                                startActualRecording(videoFile, positiveButton, dialog)
                            }
                        } else {
                            stopActualRecording(positiveButton, dialog)
                        }
                    }
                } else {
                    positiveButton.isEnabled = false
                    Snackbar.make(binding.root, "Failed to initialize camera", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        
        dialog.show()
    }
    
    private fun startActualRecording(videoFile: java.io.File, button: android.widget.Button, dialog: androidx.appcompat.app.AlertDialog) {
        cameraXVideoRecorder?.startRecording(videoFile, object : CameraXVideoRecorder.RecordingListener {
            override fun onRecordingStarted() {
                isRecording = true
                button.text = "Stop Recording"
                button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
            }
            
            override fun onRecordingFinished(videoFile: java.io.File) {
                isRecording = false
                viewModel.setVideoFromRecording(videoFile)
                dialog.dismiss()
                Snackbar.make(binding.root, "Video recorded successfully!", Snackbar.LENGTH_SHORT).show()
            }
            
            override fun onRecordingError(error: String) {
                isRecording = false
                button.text = "Start Recording"
                button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                Snackbar.make(binding.root, "Recording error: $error", Snackbar.LENGTH_LONG).show()
            }
            
            override fun onRecordingProgress(durationMs: Long) {
                // Update recording duration display if needed
            }
        })
    }
    
    private fun stopActualRecording(button: android.widget.Button, dialog: androidx.appcompat.app.AlertDialog) {
        cameraXVideoRecorder?.stopRecording()
        button.text = "Start Recording"
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
    }
    
    private fun stopVideoRecording() {
        cameraXVideoRecorder?.stopRecording()
        isRecording = false
    }
    
    private fun importVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        requestVideoImport.launch(intent)
    }
    
    private fun showTrimVideoDialog() {
        // TODO: Implement video trimming dialog
        Snackbar.make(binding.root, "Video trimming coming soon!", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun previewVideo(videoFile: java.io.File) {
        val uri = Uri.fromFile(videoFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "No video player found", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun updateVideoThumbnail(videoFile: java.io.File?) {
        if (videoFile != null && videoManager?.videoExists(videoFile) == true) {
            binding.layoutNoVideo.visibility = View.GONE
            binding.ivVideoThumbnail.visibility = View.VISIBLE
        } else {
            binding.layoutNoVideo.visibility = View.VISIBLE
            binding.ivVideoThumbnail.visibility = View.GONE
        }
    }
    
    private fun updateVideoThumbnailImage(thumbnailFile: java.io.File?) {
        if (thumbnailFile != null && thumbnailFile.exists()) {
            Glide.with(this)
                .load(thumbnailFile)
                .centerCrop()
                .into(binding.ivVideoThumbnail)
        }
    }
    
    private fun updateVideoDurationDisplay(duration: Long) {
        if (duration > 0) {
            val formattedDuration = viewModel.getFormattedVideoDuration()
            // Update UI with duration if needed
        }
    }
    
    private fun updateVideoFileSizeDisplay(fileSize: Long) {
        if (fileSize > 0) {
            val formattedSize = viewModel.getFormattedVideoFileSize()
            // Update UI with file size if needed
        }
    }
    
    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "Camera permission is required to record videos",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            // Open app settings
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraXVideoRecorder?.release()
        cameraXVideoRecorder = null
        _binding = null
    }
}