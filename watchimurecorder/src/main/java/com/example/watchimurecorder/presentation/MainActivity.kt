package com.example.watchimurecorder.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.example.watchimurecorder.data.RecordingState
import com.example.watchimurecorder.data.ServerStatus
import com.example.watchimurecorder.presentation.theme.JugglingTrackerTheme
import com.example.watchimurecorder.services.HttpServerService
import com.example.watchimurecorder.services.IMUDataService
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    private var httpServerService: HttpServerService? = null
    private var imuDataService: IMUDataService? = null
    
    private var httpServiceConnection: ServiceConnection? = null
    private var imuServiceConnection: ServiceConnection? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startServices()
        } else {
            Log.w(TAG, "Some permissions were denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity created")
        
        // Keep screen on while app is running
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setTheme(android.R.style.Theme_DeviceDefault)
        
        setContent {
            SwipeToRevealMenu(
                onShutdown = {
                    shutdownApp()
                }
            ) {
                WearApp()
            }
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "Checking permissions...")
        Log.d(TAG, "Missing permissions: ${missingPermissions.joinToString()}")

        if (missingPermissions.isEmpty()) {
            Log.d(TAG, "All permissions granted, starting services")
            startServices()
        } else {
            Log.d(TAG, "Requesting missing permissions: ${missingPermissions.joinToString()}")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startServices() {
        Log.d(TAG, "Starting services")
        
        // Start and bind to HTTP Server Service
        httpServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                try {
                    val binder = service as HttpServerService.LocalBinder
                    httpServerService = binder.getService()
                    Log.d(TAG, "Connected to HttpServerService successfully")
                    
                    // Give the service a moment to fully initialize before refreshing status
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(500) // Wait 500ms for service initialization
                        Log.d(TAG, "Refreshing server status after service connection")
                        httpServerService?.refreshServerStatus()
                        
                        // Only try to start server if it's not already running
                        val currentStatus = httpServerService?.serverStatus?.value
                        if (currentStatus?.isRunning != true) {
                            Log.d(TAG, "Auto-starting server after service connection")
                            val success = httpServerService?.startServer() ?: false
                            Log.d(TAG, "Auto-start server result: $success")
                        } else {
                            Log.d(TAG, "Server already running, skipping auto-start")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to HttpServerService", e)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                httpServerService = null
                Log.d(TAG, "Disconnected from HttpServerService")
            }
        }

        // Start and bind to IMU Data Service
        imuServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                try {
                    val binder = service as IMUDataService.LocalBinder
                    imuDataService = binder.getService()
                    Log.d(TAG, "Connected to IMUDataService successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to IMUDataService", e)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                imuDataService = null
                Log.d(TAG, "Disconnected from IMUDataService")
            }
        }

        // Start services
        val httpIntent = Intent(this, HttpServerService::class.java)
        val imuIntent = Intent(this, IMUDataService::class.java)
        
        Log.d(TAG, "Starting foreground services...")
        startForegroundService(httpIntent)
        startForegroundService(imuIntent)
        
        Log.d(TAG, "Binding to services...")
        val httpBound = bindService(httpIntent, httpServiceConnection!!, Context.BIND_AUTO_CREATE)
        val imuBound = bindService(imuIntent, imuServiceConnection!!, Context.BIND_AUTO_CREATE)
        
        Log.d(TAG, "HTTP service bind result: $httpBound")
        Log.d(TAG, "IMU service bind result: $imuBound")
    }

    @Composable
    fun WearApp() {
        // Debug state
        var debugInfo by remember { mutableStateOf("Starting...") }
        
        // Periodic status refresh to ensure UI stays updated
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000) // Refresh every 2 seconds
                httpServerService?.let { service ->
                    Log.d(TAG, "Periodic status refresh")
                    service.refreshServerStatus()
                    val status = service.serverStatus.value
                    val connected = service.isClientConnected.value
                    debugInfo = "Server: ${if (status.isRunning) "Running" else "Stopped"}, PC: ${if (connected) "Connected" else "Disconnected"}, IP: ${status.ipAddress}"
                } ?: run {
                    debugInfo = "Service not connected"
                }
            }
        }
        
        JugglingTrackerTheme {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title
                Text(
                    text = "IMU Logger",
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )

                // Debug info
                Text(
                    text = debugInfo,
                    style = MaterialTheme.typography.body2,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Server Status
                ServerStatusCard()

                Spacer(modifier = Modifier.height(8.dp))

                // Recording Status
                RecordingStatusCard()

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Controls
                ManualControlsCard()
            }
        }
    }

    @Composable
    fun ServerStatusCard() {
        var serverStatus by remember { mutableStateOf(ServerStatus(false, null, 8080)) }
        var isClientConnected by remember { mutableStateOf(false) }
        var lastUpdate by remember { mutableStateOf(0L) }

        // Force recomposition every 2 seconds
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                httpServerService?.let { service ->
                    val newStatus = service.serverStatus.value
                    val newConnected = service.isClientConnected.value
                    if (newStatus != serverStatus || newConnected != isClientConnected) {
                        serverStatus = newStatus
                        isClientConnected = newConnected
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "ServerStatusCard - Forced update: $newStatus, connected: $newConnected")
                    }
                }
            }
        }

        // Collect server status
        LaunchedEffect(httpServerService) {
            httpServerService?.let { service ->
                // Get initial values
                serverStatus = service.serverStatus.value
                isClientConnected = service.isClientConnected.value
                lastUpdate = System.currentTimeMillis()
                Log.d(TAG, "Initial server status: $serverStatus, client connected: $isClientConnected")
                
                // Collect updates
                launch {
                    service.serverStatus.collect { status ->
                        serverStatus = status
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "UI ServerStatusCard - Status updated to: $status")
                    }
                }
                launch {
                    service.isClientConnected.collect { connected ->
                        isClientConnected = connected
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "UI ServerStatusCard - Client connection updated to: $connected")
                    }
                }
            }
        }

        // Refresh status when service becomes available
        LaunchedEffect(httpServerService) {
            httpServerService?.let { service ->
                kotlinx.coroutines.delay(100) // Small delay to ensure service is ready
                Log.d(TAG, "Refreshing server status for UI update")
                service.refreshServerStatus()
                Log.d(TAG, "Current server status after refresh: ${service.serverStatus.value}")
            }
        }

        Card(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Server Status",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (serverStatus.isRunning) Color.Green else Color.Red,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (serverStatus.isRunning) "Running" else "Stopped",
                        style = MaterialTheme.typography.body2
                    )
                }

                if (serverStatus.isRunning && serverStatus.ipAddress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${serverStatus.ipAddress}:${serverStatus.port}",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    // Show client connection status
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (isClientConnected) Color.Blue else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isClientConnected) "PC Connected" else "No PC Connection",
                            style = MaterialTheme.typography.body2,
                            color = if (isClientConnected) Color.Blue else Color.Gray
                        )
                    }
                } else if (!serverStatus.isRunning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            Log.d(TAG, "Manual server start button clicked")
                            Log.d(TAG, "httpServerService is null: ${httpServerService == null}")
                            
                            if (httpServerService == null) {
                                Log.e(TAG, "HttpServerService not connected! Attempting to reconnect...")
                                startServices()
                            } else {
                                lifecycleScope.launch {
                                    Log.d(TAG, "Refreshing server status first")
                                    httpServerService?.refreshServerStatus()
                                    
                                    // Wait a moment for status refresh
                                    kotlinx.coroutines.delay(200)
                                    
                                    Log.d(TAG, "Calling startServer() on httpServerService")
                                    val success = httpServerService?.startServer() ?: false
                                    Log.d(TAG, "Manual server start result: $success")
                                    
                                    // Refresh status again after start attempt
                                    kotlinx.coroutines.delay(500)
                                    httpServerService?.refreshServerStatus()
                                }
                            }
                        },
                        modifier = Modifier.size(width = 80.dp, height = 24.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Blue
                        )
                    ) {
                        Text(
                            text = "Start",
                            style = MaterialTheme.typography.body2,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun RecordingStatusCard() {
        var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
        var sampleCount by remember { mutableStateOf(0) }
        var lastUpdate by remember { mutableStateOf(0L) }

        // Force recomposition every 2 seconds
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                imuDataService?.let { service ->
                    val newState = service.recordingState.value
                    val newCount = service.sampleCount.value
                    if (newState != recordingState || newCount != sampleCount) {
                        recordingState = newState
                        sampleCount = newCount
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "RecordingStatusCard - Forced update: $newState, samples: $newCount")
                    }
                }
            }
        }

        // Collect recording state
        LaunchedEffect(imuDataService) {
            imuDataService?.let { service ->
                // Get initial values
                recordingState = service.recordingState.value
                sampleCount = service.sampleCount.value
                lastUpdate = System.currentTimeMillis()
                Log.d(TAG, "Initial recording state: $recordingState, samples: $sampleCount")
                
                // Collect updates
                launch {
                    service.recordingState.collect { state ->
                        recordingState = state
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "UI RecordingStatusCard - Recording state updated to: $state")
                    }
                }
                launch {
                    service.sampleCount.collect { count ->
                        sampleCount = count
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "UI RecordingStatusCard - Sample count updated to: $count")
                    }
                }
            }
        }

        Card(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recording",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (recordingState) {
                                    RecordingState.RECORDING -> Color.Red
                                    RecordingState.STOPPING -> Color.Yellow
                                    RecordingState.IDLE -> Color.Gray
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = recordingState.name,
                        style = MaterialTheme.typography.body2
                    )
                }

                if (recordingState == RecordingState.RECORDING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Samples: $sampleCount",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }

    @Composable
    fun ManualControlsCard() {
        var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
        var lastUpdate by remember { mutableStateOf(0L) }

        // Force recomposition every 2 seconds
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                imuDataService?.let { service ->
                    val newState = service.recordingState.value
                    if (newState != recordingState) {
                        recordingState = newState
                        lastUpdate = System.currentTimeMillis()
                        Log.d(TAG, "ManualControlsCard - Forced update: $newState")
                    }
                }
            }
        }

        // Collect recording state
        LaunchedEffect(imuDataService) {
            imuDataService?.let { service ->
                // Get initial value
                recordingState = service.recordingState.value
                lastUpdate = System.currentTimeMillis()
                Log.d(TAG, "Initial manual controls recording state: $recordingState")
                
                // Collect updates
                service.recordingState.collect { state ->
                    recordingState = state
                    lastUpdate = System.currentTimeMillis()
                    Log.d(TAG, "UI ManualControlsCard - Recording state updated to: $state")
                }
            }
        }

        Card(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Manual Control",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Button
                    Button(
                        onClick = {
                            Log.d(TAG, "Start button clicked, current state: $recordingState")
                            Log.d(TAG, "imuDataService is null: ${imuDataService == null}")
                            
                            if (imuDataService == null) {
                                Log.e(TAG, "IMUDataService not connected!")
                                return@Button
                            }
                            
                            lifecycleScope.launch {
                                val success = imuDataService?.startRecording() ?: false
                                Log.d(TAG, "Start recording result: $success")
                                
                                // Give a moment for state to update
                                kotlinx.coroutines.delay(100)
                                val newState = imuDataService?.recordingState?.value
                                Log.d(TAG, "Recording state after start: $newState")
                            }
                        },
                        enabled = recordingState == RecordingState.IDLE,
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Green
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Recording",
                            tint = Color.White
                        )
                    }

                    // Stop Button
                    Button(
                        onClick = {
                            Log.d(TAG, "Stop button clicked, current state: $recordingState")
                            Log.d(TAG, "imuDataService is null: ${imuDataService == null}")
                            
                            if (imuDataService == null) {
                                Log.e(TAG, "IMUDataService not connected!")
                                return@Button
                            }
                            
                            lifecycleScope.launch {
                                val success = imuDataService?.stopRecording() ?: false
                                Log.d(TAG, "Stop recording result: $success")
                                
                                // Give a moment for state to update
                                kotlinx.coroutines.delay(100)
                                val newState = imuDataService?.recordingState?.value
                                Log.d(TAG, "Recording state after stop: $newState")
                            }
                        },
                        enabled = recordingState == RecordingState.RECORDING,
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    private fun shutdownApp() {
        Log.d(TAG, "Shutdown requested")
        
        lifecycleScope.launch {
            try {
                // Stop recording if active
                imuDataService?.let { service ->
                    if (service.recordingState.value == RecordingState.RECORDING) {
                        Log.d(TAG, "Stopping recording before shutdown")
                        service.stopRecording()
                        kotlinx.coroutines.delay(1000) // Wait for recording to stop
                    }
                }
                
                // Stop HTTP server
                httpServerService?.let { service ->
                    if (service.serverStatus.value.isRunning) {
                        Log.d(TAG, "Stopping HTTP server before shutdown")
                        service.stopServer()
                        kotlinx.coroutines.delay(500) // Wait for server to stop
                    }
                }
                
                // Unbind services
                httpServiceConnection?.let { unbindService(it) }
                imuServiceConnection?.let { unbindService(it) }
                
                // Stop services
                stopService(Intent(this@MainActivity, HttpServerService::class.java))
                stopService(Intent(this@MainActivity, IMUDataService::class.java))
                
                Log.d(TAG, "Services stopped, finishing activity")
                
                // Finish activity and exit
                finish()
                exitProcess(0)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during shutdown", e)
                // Force exit even if there's an error
                finish()
                exitProcess(0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
        
        httpServiceConnection?.let { unbindService(it) }
        imuServiceConnection?.let { unbindService(it) }
    }
}