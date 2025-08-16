package com.example.jugglingtracker.utils

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX video recording helper class
 */
class CameraXVideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    
    companion object {
        private const val TAG = "CameraXVideoRecorder"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var isRecording = false
    private var recordingListener: RecordingListener? = null
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingFinished(videoFile: File)
        fun onRecordingError(error: String)
        fun onRecordingProgress(durationMs: Long)
    }

    /**
     * Initialize the camera
     */
    suspend fun initializeCamera(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                continuation.resume(Result.success(Unit))
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                continuation.resume(Result.failure(e))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Bind camera use cases
     */
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed")

        // Preview use case
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Video capture use case
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                )
            )
            .build()
        
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector,
                preview,
                videoCapture
            )

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            throw e
        }
    }

    /**
     * Start video recording
     */
    fun startRecording(outputFile: File, listener: RecordingListener) {
        if (isRecording) {
            listener.onRecordingError("Recording is already in progress")
            return
        }

        val videoCapture = videoCapture ?: run {
            listener.onRecordingError("Camera not initialized")
            return
        }

        recordingListener = listener

        val mediaStoreOutputOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                // Enable audio recording if permission is granted
                if (PermissionChecker.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        listener.onRecordingStarted()
                        Log.d(TAG, "Recording started")
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        if (!recordEvent.hasError()) {
                            listener.onRecordingFinished(outputFile)
                            Log.d(TAG, "Recording finished successfully")
                        } else {
                            val error = "Recording failed: ${recordEvent.error}"
                            listener.onRecordingError(error)
                            Log.e(TAG, error)
                        }
                        recording = null
                    }
                    is VideoRecordEvent.Status -> {
                        // Update recording progress
                        listener.onRecordingProgress(recordEvent.recordingStats.recordedDurationNanos / 1_000_000)
                    }
                }
            }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        if (!isRecording) return
        
        recording?.stop()
        recording = null
        isRecording = false
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        if (isRecording) {
            Log.w(TAG, "Cannot switch camera while recording")
            return
        }
        
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // Rebind camera with new selector
        try {
            bindCameraUseCases()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera", e)
            // Revert to previous camera selector
            currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    /**
     * Enable/disable flash
     */
    fun toggleFlash() {
        camera?.let { camera ->
            if (camera.cameraInfo.hasFlashUnit()) {
                val currentFlashMode = camera.cameraInfo.torchState.value
                camera.cameraControl.enableTorch(currentFlashMode != TorchState.ON)
            }
        }
    }

    /**
     * Get camera info
     */
    fun getCameraInfo(): CameraInfo? = camera?.cameraInfo

    /**
     * Release camera resources
     */
    fun release() {
        stopRecording()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    /**
     * Check if camera has flash
     */
    fun hasFlash(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() == true
    }
    
    /**
     * Check if front camera is currently selected
     */
    fun isFrontCamera(): Boolean {
        return currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }

    /**
     * Get available camera qualities
     */
    fun getAvailableQualities(): List<Quality> {
        val cameraInfo = camera?.cameraInfo ?: return emptyList()
        return QualitySelector.getSupportedQualities(cameraInfo)
    }

    /**
     * Set video quality
     */
    fun setVideoQuality(quality: Quality) {
        // This would require rebinding the camera with new quality settings
        // Implementation depends on specific requirements
    }
}