package com.example.watchimurecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.watchimurecorder.data.RecordingSession
import com.example.watchimurecorder.data.RecordingState
import com.example.watchimurecorder.data.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class IMUDataService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "IMUDataService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "imu_recording_channel"
        private const val SAMPLE_RATE = SensorManager.SENSOR_DELAY_FASTEST // ~200Hz
    }

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount

    private var currentSession: RecordingSession? = null
    private var csvWriter: FileWriter? = null
    private var recordingFile: File? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Sensor data buffers
    private var lastAccelData = FloatArray(3)
    private var lastGyroData = FloatArray(3)
    private var lastMagData = FloatArray(3) { Float.NaN }

    inner class LocalBinder : Binder() {
        fun getService(): IMUDataService = this@IMUDataService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IMUDataService created")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Initialize sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        createNotificationChannel()
        acquireWakeLock()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WatchIMURecorder::IMUDataWakeLock"
        )
        wakeLock?.acquire()
        Log.d(TAG, "Wake lock acquired for IMU service")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released for IMU service")
            }
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("IMU Recording Service Ready"))
        return START_STICKY
    }

    fun startRecording(): Boolean {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Recording already in progress, current state: ${_recordingState.value}")
            return false
        }

        return try {
            Log.d(TAG, "Starting IMU recording")
            _recordingState.value = RecordingState.RECORDING
            _sampleCount.value = 0

            // Create new session
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            currentSession = RecordingSession(
                sessionId = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                deviceId = deviceId
            )

            // Create recording file
            setupRecordingFile()

            // Register sensor listeners
            var sensorsRegistered = 0
            accelerometer?.let {
                val success = sensorManager.registerListener(this, it, SAMPLE_RATE)
                Log.d(TAG, "Accelerometer registration: $success")
                if (success) sensorsRegistered++
            }
            gyroscope?.let {
                val success = sensorManager.registerListener(this, it, SAMPLE_RATE)
                Log.d(TAG, "Gyroscope registration: $success")
                if (success) sensorsRegistered++
            }
            magnetometer?.let {
                val success = sensorManager.registerListener(this, it, SAMPLE_RATE)
                Log.d(TAG, "Magnetometer registration: $success")
                if (success) sensorsRegistered++
            }

            Log.d(TAG, "Registered $sensorsRegistered sensors")
            updateNotification("Recording IMU data...")
            Log.d(TAG, "Recording started successfully, state: ${_recordingState.value}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recordingState.value = RecordingState.IDLE
            false
        }
    }

    fun stopRecording(): Boolean {
        if (_recordingState.value != RecordingState.RECORDING) {
            Log.w(TAG, "No recording in progress, current state: ${_recordingState.value}")
            return false
        }

        return try {
            Log.d(TAG, "Stopping IMU recording")
            _recordingState.value = RecordingState.STOPPING

            // Unregister sensor listeners
            sensorManager.unregisterListener(this)
            Log.d(TAG, "Sensor listeners unregistered")

            // Finalize recording
            serviceScope.launch {
                finalizeRecording()
            }

            updateNotification("Recording stopped")
            Log.d(TAG, "Recording stopped successfully, state: ${_recordingState.value}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            false
        }
    }

    private fun setupRecordingFile() {
        val session = currentSession ?: return
        
        // Create recordings directory
        val recordingsDir = File(getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        // Create file with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date(session.startTime))
        val fileName = "imu_${session.deviceId}_$timestamp.csv"
        
        recordingFile = File(recordingsDir, fileName)
        csvWriter = FileWriter(recordingFile!!)

        // Write metadata and header
        csvWriter?.apply {
            write(session.toCsvMetadata())
            write(SensorReading.CSV_HEADER + "\n")
            flush()
        }
    }

    private suspend fun finalizeRecording() {
        try {
            val session = currentSession?.copy(
                endTime = System.currentTimeMillis(),
                sampleCount = _sampleCount.value
            )

            csvWriter?.close()
            csvWriter = null

            Log.d(TAG, "Recording finalized: ${session?.sampleCount} samples saved to ${recordingFile?.name}")
            
            _recordingState.value = RecordingState.IDLE
            currentSession = null
            recordingFile = null
            
            updateNotification("IMU Recording Service Ready")
            Log.d(TAG, "Recording finalized, state: ${_recordingState.value}")

        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing recording", e)
            _recordingState.value = RecordingState.IDLE
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (_recordingState.value != RecordingState.RECORDING) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelData = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroData = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagData = event.values.clone()
            }
        }

        // Write sensor reading (triggered by any sensor update)
        writeSensorReading(event.timestamp)
    }

    private fun writeSensorReading(timestamp: Long) {
        serviceScope.launch {
            try {
                val reading = SensorReading(
                    timestamp = timestamp,
                    accelerometerX = lastAccelData[0],
                    accelerometerY = lastAccelData[1],
                    accelerometerZ = lastAccelData[2],
                    gyroscopeX = lastGyroData[0],
                    gyroscopeY = lastGyroData[1],
                    gyroscopeZ = lastGyroData[2],
                    magnetometerX = if (lastMagData[0].isNaN()) null else lastMagData[0],
                    magnetometerY = if (lastMagData[1].isNaN()) null else lastMagData[1],
                    magnetometerZ = if (lastMagData[2].isNaN()) null else lastMagData[2]
                )

                csvWriter?.apply {
                    write(reading.toCsvRow() + "\n")
                    if (_sampleCount.value % 100 == 0) { // Flush every 100 samples
                        flush()
                    }
                }

                _sampleCount.value = _sampleCount.value + 1

            } catch (e: Exception) {
                Log.e(TAG, "Error writing sensor reading", e)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IMU Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when IMU data is being recorded"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watch IMU Recorder")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IMUDataService destroyed")
        
        if (_recordingState.value == RecordingState.RECORDING) {
            stopRecording()
        }
        
        sensorManager.unregisterListener(this)
        csvWriter?.close()
    }
}