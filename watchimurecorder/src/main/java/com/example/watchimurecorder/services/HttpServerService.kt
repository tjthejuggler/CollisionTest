package com.example.watchimurecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.watchimurecorder.data.ServerStatus
import com.example.watchimurecorder.data.RecordingState
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.util.Collections
import java.io.File

class HttpServerService : Service() {

    companion object {
        private const val TAG = "HttpServerService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "http_server_channel"
        private const val DEFAULT_PORT = 8080
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private var httpServer: IMUHttpServer? = null
    private var imuDataService: IMUDataService? = null
    private var imuServiceConnection: ServiceConnection? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _serverStatus = MutableStateFlow(
        ServerStatus(
            isRunning = false,
            ipAddress = null,
            port = DEFAULT_PORT
        )
    )
    val serverStatus: StateFlow<ServerStatus> = _serverStatus
    
    private val _lastRequestTime = MutableStateFlow(0L)
    val lastRequestTime: StateFlow<Long> = _lastRequestTime
    
    private val _isClientConnected = MutableStateFlow(false)
    val isClientConnected: StateFlow<Boolean> = _isClientConnected

    inner class LocalBinder : Binder() {
        fun getService(): HttpServerService = this@HttpServerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HttpServerService created")
        createNotificationChannel()
        bindToIMUService()
        acquireWakeLock()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WatchIMURecorder::HttpServerWakeLock"
        )
        wakeLock?.acquire()
        Log.d(TAG, "Wake lock acquired for HTTP server")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released for HTTP server")
            }
        }
        wakeLock = null
    }
    
    fun refreshServerStatus() {
        Log.d(TAG, "Refreshing server status")
        val isRunning = httpServer?.isAlive == true
        val ipAddress = if (isRunning) getWifiIpAddress() else null
        
        _serverStatus.value = _serverStatus.value.copy(
            isRunning = isRunning,
            ipAddress = ipAddress
        )
        
        val statusMessage = if (isRunning) {
            "Server running on ${ipAddress ?: "localhost"}:${_serverStatus.value.port}"
        } else {
            "Server stopped"
        }
        
        updateNotification(statusMessage)
        Log.d(TAG, "Server status refreshed: isRunning=$isRunning, ip=$ipAddress")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        startForeground(NOTIFICATION_ID, createNotification("HTTP Server Starting..."))
        
        // Try to start server with a small delay to ensure service is fully initialized
        serviceScope.launch {
            kotlinx.coroutines.delay(1000) // Wait 1 second
            val success = startServer()
            Log.d(TAG, "Server start result: $success")
            
            // Always refresh status after start attempt
            refreshServerStatus()
        }
        
        return START_STICKY
    }

    private fun bindToIMUService() {
        imuServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as IMUDataService.LocalBinder
                imuDataService = binder.getService()
                Log.d(TAG, "Connected to IMUDataService")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                imuDataService = null
                Log.d(TAG, "Disconnected from IMUDataService")
            }
        }

        val intent = Intent(this, IMUDataService::class.java)
        startService(intent)
        bindService(intent, imuServiceConnection!!, Context.BIND_AUTO_CREATE)
    }

    fun startServer(): Boolean {
        Log.d(TAG, "startServer() called")
        
        if (httpServer?.isAlive == true) {
            Log.w(TAG, "Server already running")
            // Update status to reflect current state and refresh
            refreshServerStatus()
            return true
        }

        return try {
            Log.d(TAG, "Starting HTTP server...")
            
            // First try to get IP address
            val ipAddress = getWifiIpAddress()
            Log.d(TAG, "Detected IP address: $ipAddress")
            
            // Try different ports if default fails
            var serverStarted = false
            var actualPort = DEFAULT_PORT
            var lastException: Exception? = null
            val portsToTry = listOf(DEFAULT_PORT, 8081, 8082, 8083, 9090)
            
            for (port in portsToTry) {
                try {
                    Log.d(TAG, "Attempting to start server on port: $port")
                    httpServer = IMUHttpServer(port)
                    Log.d(TAG, "Created IMUHttpServer instance for port: $port")
                    
                    httpServer?.start()
                    Log.d(TAG, "Called start() on server for port: $port")
                    
                    // Small delay to let server initialize
                    Thread.sleep(100)
                    
                    // Test if server is actually running
                    val isAlive = httpServer?.isAlive == true
                    Log.d(TAG, "Server isAlive check for port $port: $isAlive")
                    
                    if (isAlive) {
                        actualPort = port
                        serverStarted = true
                        Log.d(TAG, "Server successfully started on port: $port")
                        break
                    } else {
                        Log.w(TAG, "Server not alive after start on port: $port")
                        httpServer?.stop()
                        httpServer = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception starting server on port $port", e)
                    lastException = e
                    httpServer?.stop()
                    httpServer = null
                }
            }
            
            if (serverStarted) {
                _serverStatus.value = _serverStatus.value.copy(
                    isRunning = true,
                    ipAddress = ipAddress ?: "localhost",
                    port = actualPort
                )

                val serverInfo = if (ipAddress != null) {
                    "Server running on $ipAddress:$actualPort"
                } else {
                    "Server running on port $actualPort"
                }
                
                updateNotification(serverInfo)
                Log.d(TAG, "HTTP Server started successfully: $serverInfo")
                
                // Refresh status to ensure UI is updated
                refreshServerStatus()
                true
            } else {
                val errorMsg = "Could not start server on any port. Last error: ${lastException?.message}"
                Log.e(TAG, errorMsg)
                _serverStatus.value = _serverStatus.value.copy(
                    isRunning = false,
                    ipAddress = "Error: ${lastException?.message}"
                )
                updateNotification("Server failed: ${lastException?.message}")
                throw Exception(errorMsg)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server", e)
            _serverStatus.value = _serverStatus.value.copy(
                isRunning = false,
                ipAddress = "Error: ${e.message}"
            )
            updateNotification("Server failed: ${e.message}")
            false
        }
    }

    fun stopServer(): Boolean {
        return try {
            httpServer?.stop()
            httpServer = null

            _serverStatus.value = _serverStatus.value.copy(
                isRunning = false,
                ipAddress = null
            )

            updateNotification("Server stopped")
            Log.d(TAG, "HTTP Server stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop HTTP server", e)
            false
        }
    }

    private fun getWifiIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            Log.d(TAG, "Available network interfaces:")
            
            for (networkInterface in interfaces) {
                Log.d(TAG, "Interface: ${networkInterface.name}")
                
                // Try multiple common WiFi interface names
                if (networkInterface.name.equals("wlan0", ignoreCase = true) ||
                    networkInterface.name.equals("wlan1", ignoreCase = true) ||
                    networkInterface.name.startsWith("wlan", ignoreCase = true)) {
                    
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        Log.d(TAG, "Address: ${address.hostAddress}, isLoopback: ${address.isLoopbackAddress}")
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            Log.d(TAG, "Found WiFi IP: ${address.hostAddress}")
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
                        Log.d(TAG, "Found fallback IP: ${address.hostAddress}")
                        return address.hostAddress
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi IP address", e)
        }
        Log.w(TAG, "No WiFi IP address found")
        return null
    }

    private inner class IMUHttpServer(port: Int) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method
            val clientIP = session.remoteIpAddress

            Log.d(TAG, "Received request: $method $uri from $clientIP")
            
            // Update last request time and client connection status
            _lastRequestTime.value = System.currentTimeMillis()
            _isClientConnected.value = true
            
            // Schedule a task to reset client connection status after 30 seconds of inactivity
            serviceScope.launch {
                kotlinx.coroutines.delay(30000) // 30 seconds
                val timeSinceLastRequest = System.currentTimeMillis() - _lastRequestTime.value
                if (timeSinceLastRequest >= 30000) {
                    _isClientConnected.value = false
                    Log.d(TAG, "Client connection timeout - no requests for 30 seconds")
                }
            }

            return when {
                uri == "/start" && method == Method.GET -> handleStartRecording()
                uri == "/stop" && method == Method.GET -> handleStopRecording()
                uri == "/status" && method == Method.GET -> handleGetStatus()
                uri == "/ping" && method == Method.GET -> handlePing()
                uri == "/data" && method == Method.GET -> handleGetData()
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "Endpoint not found. Available endpoints: /start, /stop, /status, /ping, /data"
                )
            }
        }

        private fun handleStartRecording(): Response {
            return try {
                val success = imuDataService?.startRecording() ?: false
                
                if (success) {
                    serviceScope.launch {
                        updateServerStatus()
                    }
                    newFixedLengthResponse(
                        Response.Status.OK,
                        MIME_PLAINTEXT,
                        "Recording started successfully"
                    )
                } else {
                    newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "Failed to start recording"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Error: ${e.message}"
                )
            }
        }

        private fun handleStopRecording(): Response {
            return try {
                val success = imuDataService?.stopRecording() ?: false
                
                if (success) {
                    serviceScope.launch {
                        updateServerStatus()
                    }
                    newFixedLengthResponse(
                        Response.Status.OK,
                        MIME_PLAINTEXT,
                        "Recording stopped successfully"
                    )
                } else {
                    newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "Failed to stop recording"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Error: ${e.message}"
                )
            }
        }

        private fun handleGetStatus(): Response {
            return try {
                val recordingState = imuDataService?.recordingState?.value ?: RecordingState.IDLE
                val sampleCount = imuDataService?.sampleCount?.value ?: 0
                
                val status = """
                    {
                        "server_running": true,
                        "recording_state": "${recordingState.name}",
                        "sample_count": $sampleCount,
                        "ip_address": "${_serverStatus.value.ipAddress}",
                        "port": ${_serverStatus.value.port}
                    }
                """.trimIndent()

                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    status
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting status", e)
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Error: ${e.message}"
                )
            }
        }

        private fun handlePing(): Response {
            return newFixedLengthResponse(
                Response.Status.OK,
                MIME_PLAINTEXT,
                "pong"
            )
        }

        private fun handleGetData(): Response {
            return try {
                val imuService = imuDataService
                if (imuService == null) {
                    return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "IMU service not available"
                    )
                }

                // Get the most recent recording file
                val recordingsDir = File(getExternalFilesDir(null), "recordings")
                if (!recordingsDir.exists()) {
                    return newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        "application/json",
                        "[]"
                    )
                }

                val csvFiles = recordingsDir.listFiles { file ->
                    file.name.endsWith(".csv") && file.name.startsWith("imu_")
                }?.sortedByDescending { it.lastModified() }

                if (csvFiles.isNullOrEmpty()) {
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        "[]"
                    )
                }

                // Read the most recent CSV file and convert to JSON
                val mostRecentFile = csvFiles.first()
                val jsonData = convertCsvToJson(mostRecentFile)

                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    jsonData
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting data", e)
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Error: ${e.message}"
                )
            }
        }

        // Add this helper method to convert CSV to JSON:
        private fun convertCsvToJson(csvFile: File): String {
            val readings = mutableListOf<Map<String, Any>>()
            
            try {
                csvFile.bufferedReader().use { reader ->
                    var isHeader = true
                    var isMetadata = true
                    
                    reader.forEachLine { line ->
                        when {
                            line.startsWith("#") -> {
                                // Skip metadata lines
                                isMetadata = true
                            }
                            isHeader && !isMetadata -> {
                                // Skip CSV header
                                isHeader = false
                            }
                            !line.trim().isEmpty() && !isMetadata -> {
                                // Parse data line
                                val parts = line.split(",")
                                if (parts.size >= 7) {
                                    val reading = mapOf<String, Any>(
                                        "timestamp" to (parts[0].toLongOrNull() ?: 0L),
                                        "accel_x" to (parts[1].toDoubleOrNull() ?: 0.0),
                                        "accel_y" to (parts[2].toDoubleOrNull() ?: 0.0),
                                        "accel_z" to (parts[3].toDoubleOrNull() ?: 0.0),
                                        "gyro_x" to (parts[4].toDoubleOrNull() ?: 0.0),
                                        "gyro_y" to (parts[5].toDoubleOrNull() ?: 0.0),
                                        "gyro_z" to (parts[6].toDoubleOrNull() ?: 0.0),
                                        "mag_x" to (if (parts.size > 7) parts[7].toDoubleOrNull() ?: 0.0 else 0.0),
                                        "mag_y" to (if (parts.size > 8) parts[8].toDoubleOrNull() ?: 0.0 else 0.0),
                                        "mag_z" to (if (parts.size > 9) parts[9].toDoubleOrNull() ?: 0.0 else 0.0)
                                    )
                                    readings.add(reading)
                                }
                            }
                        }
                        
                        if (isHeader && !line.startsWith("#")) {
                            isMetadata = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading CSV file: ${csvFile.name}", e)
            }
            
            // Convert to JSON string
            return buildString {
                append("[")
                readings.forEachIndexed { index, reading ->
                    if (index > 0) append(",")
                    append("{")
                    reading.entries.forEachIndexed { entryIndex, entry ->
                        if (entryIndex > 0) append(",")
                        append("\"${entry.key}\":${entry.value}")
                    }
                    append("}")
                }
                append("]")
            }
        }
    }

    private suspend fun updateServerStatus() {
        val recordingState = imuDataService?.recordingState?.value ?: RecordingState.IDLE
        _serverStatus.value = _serverStatus.value.copy(recordingState = recordingState)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HTTP Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when HTTP server is running"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watch IMU Recorder Server")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        Log.d(TAG, "HttpServerService destroyed")
        
        stopServer()
        
        imuServiceConnection?.let {
            unbindService(it)
        }
    }
}