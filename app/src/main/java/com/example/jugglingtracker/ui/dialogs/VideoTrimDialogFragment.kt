package com.example.jugglingtracker.ui.dialogs

import android.app.Dialog
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.example.jugglingtracker.R
import com.example.jugglingtracker.databinding.DialogVideoTrimBinding
import com.example.jugglingtracker.utils.VideoManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.io.File

/**
 * Dialog for basic video trimming functionality
 */
class VideoTrimDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_VIDEO_FILE_PATH = "video_file_path"
        
        fun newInstance(videoFilePath: String): VideoTrimDialogFragment {
            return VideoTrimDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_FILE_PATH, videoFilePath)
                }
            }
        }
    }

    private var _binding: DialogVideoTrimBinding? = null
    private val binding get() = _binding!!
    
    private var videoFile: File? = null
    private var videoManager: VideoManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoDuration: Long = 0L
    private var startTime: Long = 0L
    private var endTime: Long = 0L
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
    
    var onVideoTrimmed: ((File) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogVideoTrimBinding.inflate(LayoutInflater.from(requireContext()))
        
        val videoFilePath = arguments?.getString(ARG_VIDEO_FILE_PATH)
        videoFile = videoFilePath?.let { File(it) }
        videoManager = VideoManager(requireContext())
        
        setupVideoPlayer()
        setupTrimControls()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Trim Video")
            .setView(binding.root)
            .setPositiveButton("Trim") { _, _ ->
                trimVideo()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun setupVideoPlayer() {
        videoFile?.let { file ->
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                binding.videoView.setVideoURI(uri)
                
                binding.videoView.setOnPreparedListener { mediaPlayer ->
                    this.mediaPlayer = mediaPlayer
                    videoDuration = mediaPlayer.duration.toLong()
                    endTime = videoDuration
                    
                    // Setup seek bars
                    binding.seekBarProgress.max = videoDuration.toInt()
                    binding.seekBarStart.max = videoDuration.toInt()
                    binding.seekBarEnd.max = videoDuration.toInt()
                    binding.seekBarEnd.progress = videoDuration.toInt()
                    
                    updateTimeDisplays()
                    startProgressTracking()
                }
                
                binding.videoView.setOnCompletionListener {
                    binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                    stopProgressTracking()
                }
            }
        }
    }

    private fun setupTrimControls() {
        // Play/Pause button
        binding.btnPlayPause.setOnClickListener {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
                binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                stopProgressTracking()
            } else {
                binding.videoView.start()
                binding.btnPlayPause.setIconResource(R.drawable.ic_pause)
                startProgressTracking()
            }
        }
        
        // Progress seek bar
        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Start time seek bar
        binding.seekBarStart.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    startTime = progress.toLong()
                    if (startTime >= endTime) {
                        startTime = endTime - 1000 // Ensure at least 1 second difference
                        binding.seekBarStart.progress = startTime.toInt()
                    }
                    updateTimeDisplays()
                    binding.videoView.seekTo(startTime.toInt())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // End time seek bar
        binding.seekBarEnd.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    endTime = progress.toLong()
                    if (endTime <= startTime) {
                        endTime = startTime + 1000 // Ensure at least 1 second difference
                        binding.seekBarEnd.progress = endTime.toInt()
                    }
                    updateTimeDisplays()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Jump to start/end buttons
        binding.btnJumpToStart.setOnClickListener {
            binding.videoView.seekTo(startTime.toInt())
            binding.seekBarProgress.progress = startTime.toInt()
        }
        
        binding.btnJumpToEnd.setOnClickListener {
            binding.videoView.seekTo(endTime.toInt())
            binding.seekBarProgress.progress = endTime.toInt()
        }
    }

    private fun startProgressTracking() {
        updateProgressRunnable = object : Runnable {
            override fun run() {
                if (binding.videoView.isPlaying) {
                    val currentPosition = binding.videoView.currentPosition
                    binding.seekBarProgress.progress = currentPosition
                    
                    // Auto-pause at end time during preview
                    if (currentPosition >= endTime) {
                        binding.videoView.pause()
                        binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                        binding.videoView.seekTo(startTime.toInt())
                        return
                    }
                    
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(updateProgressRunnable!!)
    }

    private fun stopProgressTracking() {
        updateProgressRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateTimeDisplays() {
        binding.tvStartTime.text = formatTime(startTime)
        binding.tvEndTime.text = formatTime(endTime)
        binding.tvDuration.text = formatTime(endTime - startTime)
    }

    private fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000) % 60
        val minutes = (timeMs / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun trimVideo() {
        val originalFile = videoFile ?: return
        val manager = videoManager ?: return
        
        // For this basic implementation, we'll create a simple trimmed video
        // In a production app, you'd use FFmpeg or MediaMetadataRetriever for actual trimming
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a new file for the trimmed video
                val trimmedFile = manager.createVideoFile("trimmed_${System.currentTimeMillis()}.mp4")
                
                // For now, we'll just copy the original file as a placeholder
                // In a real implementation, you would use video processing libraries
                originalFile.copyTo(trimmedFile, overwrite = true)
                
                withContext(Dispatchers.Main) {
                    onVideoTrimmed?.invoke(trimmedFile)
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle error
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopProgressTracking()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}