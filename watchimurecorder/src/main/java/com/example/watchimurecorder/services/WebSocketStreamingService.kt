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
import com.example.watchimurecorder.data.StreamingData
import com.example.watchimurecorder.data.StreamingStatus
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.NetworkInterface
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WebSocketStreamingService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "WebSocketStreamingService"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "websocket_streaming_channel"
        private const val DEFAULT_PORT = 8081
        private const val SAMPLE_RATE = SensorManager.SENSOR_DELAY_FASTEST
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private var webSocketServer: ApplicationEngine? = null
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Connected WebSocket sessions
    private val connectedSessions = ConcurrentHashMap<String, DefaultWebSocketSession>()
    
    // Streaming status
    private val _streamingStatus = MutableStateFlow(
        StreamingStatus(
            isStreaming = false,
            connectedClients = 0,
            serverPort = DEFAULT_PORT
        )
    )
    val streamingStatus: StateFlow<StreamingStatus> = _streamingStatus
    
    // Device ID for watch identification
    private lateinit var deviceId: String
    
    // Data channel for sensor events
    private val sensorDataChannel = Channel<StreamingData>(capacity = Channel.UNLIMITED)

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketStreamingService = this@WebSocketStreamingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WebSocketStreamingService created")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        deviceId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "left_watch"
        
        createNotificationChannel()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WatchIMURecorder::WebSocketStreamingWakeLock"
        )
        wakeLock?.acquire()
        Log.d(TAG, "Wake lock acquired for WebSocket streaming service")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released for WebSocket streaming service")
            }
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("WebSocket Streaming Service Ready"))
        return START_STICKY
    }

    fun startStreaming(): Boolean {
        if (_streamingStatus.value.isStreaming) {
            Log.w(TAG, "Streaming already active")
            return true
        }

        return try {
            Log.d(TAG, "Starting WebSocket streaming server")
            
            val ipAddress = getWifiIpAddress()
            Log.d(TAG, "Starting server on IP: $ipAddress, Port: $DEFAULT_PORT")
            
            webSocketServer = embeddedServer(CIO, port = DEFAULT_PORT) {
                install(WebSockets) {
                    pingPeriod = Duration.ofSeconds(15)
                    timeout = Duration.ofSeconds(15)
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                
                routing {
                    webSocket("/imu") {
                        val sessionId = UUID.randomUUID().toString()
                        connectedSessions[sessionId] = this
                        updateClientCount()
                        
                        Log.d(TAG, "Client connected: $sessionId")
                        updateNotification("Streaming to ${connectedSessions.size} client(s)")
                        
                        try {
                            // Start sensor listeners when first client connects
                            if (connectedSessions.size == 1) {
                                startSensorListeners()
                            }
                            
                            // Keep connection alive and handle incoming messages
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        Log.d(TAG, "Received from client $sessionId: $text")
                                    }
                                    is Frame.Close -> {
                                        Log.d(TAG, "Client $sessionId disconnected")
                                        break
                                    }
                                    else -> {}
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in WebSocket session $sessionId", e)
                        } finally {
                            connectedSessions.remove(sessionId)
                            updateClientCount()
                            Log.d(TAG, "Client $sessionId removed, remaining: ${connectedSessions.size}")
                            
                            // Stop sensor listeners when no clients connected
                            if (connectedSessions.isEmpty()) {
                                stopSensorListeners()
                                updateNotification("WebSocket Streaming Service Ready")
                            } else {
                                updateNotification("Streaming to ${connectedSessions.size} client(s)")
                            }
                        }
                    }
                }
            }
            
            webSocketServer?.start(wait = false)
            
            _streamingStatus.value = _streamingStatus.value.copy(
                isStreaming = true,
                serverPort = DEFAULT_PORT
            )
            
            // Start coroutine to send sensor data to connected clients
            serviceScope.launch {
                for (data in sensorDataChannel) {
                    sendToAllClients(data)
                }
            }
            
            val serverInfo = "WebSocket server running on ws://${ipAddress ?: "localhost"}:$DEFAULT_PORT/imu"
            updateNotification(serverInfo)
            Log.d(TAG, "WebSocket streaming started: $serverInfo")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebSocket streaming", e)
            _streamingStatus.value = _streamingStatus.value.copy(isStreaming = false)
            updateNotification("Streaming failed: ${e.message}")
            false
        }
    }

    fun stopStreaming(): Boolean {
        return try {
            Log.d(TAG, "Stopping WebSocket streaming")
            
            stopSensorListeners()
            
            // Close all WebSocket connections
            connectedSessions.values.forEach { session ->
                try {
                    serviceScope.launch {
                        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down"))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing WebSocket session", e)
                }
            }
            connectedSessions.clear()
            
            webSocketServer?.stop(1000, 2000)
            webSocketServer = null
            
            _streamingStatus.value = StreamingStatus(
                isStreaming = false,
                connectedClients = 0,
                serverPort = DEFAULT_PORT
            )
            
            updateNotification("WebSocket Streaming Service Ready")
            Log.d(TAG, "WebSocket streaming stopped")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WebSocket streaming", e)
            false
        }
    }

    private fun startSensorListeners() {
        Log.d(TAG, "Starting sensor listeners for streaming")
        
        accelerometer?.let {
            val success = sensorManager.registerListener(this, it, SAMPLE_RATE)
            Log.d(TAG, "Accelerometer registration for streaming: $success")
        }
        
        gyroscope?.let {
            val success = sensorManager.registerListener(this, it, SAMPLE_RATE)
            Log.d(TAG, "Gyroscope registration for streaming: $success")
        }
    }

    private fun stopSensorListeners() {
        Log.d(TAG, "Stopping sensor listeners for streaming")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!_streamingStatus.value.isStreaming || connectedSessions.isEmpty()) return

        val sensorType = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "accel"
            Sensor.TYPE_GYROSCOPE -> "gyro"
            else -> return
        }

        // Create single data message with all three axis values
        val timestamp = event.timestamp
        
        val data = StreamingData(
            watch_id = deviceId,
            type = sensorType,
            timestamp_ns = timestamp,
            x = event.values[0],
            y = event.values[1],
            z = event.values[2]
        )

        // Send the sensor data
        serviceScope.launch {
            sensorDataChannel.trySend(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private suspend fun sendToAllClients(data: StreamingData) {
        val jsonString = Json.encodeToString(data)
        
        val sessionsToRemove = mutableListOf<String>()
        
        connectedSessions.forEach { (sessionId, session) ->
            try {
                session.send(Frame.Text(jsonString))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send data to client $sessionId", e)
                sessionsToRemove.add(sessionId)
            }
        }
        
        // Remove failed sessions
        sessionsToRemove.forEach { sessionId ->
            connectedSessions.remove(sessionId)
        }
        
        if (sessionsToRemove.isNotEmpty()) {
            updateClientCount()
        }
    }

    private fun updateClientCount() {
        _streamingStatus.value = _streamingStatus.value.copy(
            connectedClients = connectedSessions.size
        )
    }

    private fun getWifiIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            
            for (networkInterface in interfaces) {
                if (networkInterface.name.equals("wlan0", ignoreCase = true) ||
                    networkInterface.name.equals("wlan1", ignoreCase = true) ||
                    networkInterface.name.startsWith("wlan", ignoreCase = true)) {
                    
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
            
            // Fallback: try any non-loopback IPv4 address
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi IP address", e)
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WebSocket Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when WebSocket streaming is active"
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watch IMU Streamer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        // Disabled to prevent repeated vibrations that interfere with IMU readings
        // val notification = createNotification(content)
        // val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WebSocketStreamingService destroyed")
        
        stopStreaming()
        releaseWakeLock()
    }
}