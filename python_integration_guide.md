# Complete Python Integration Guide for Watch IMU Recorder

## Overview

This guide provides everything needed to integrate with the Watch IMU Recorder from Python. The watch app runs an HTTP server that accepts commands to start/stop IMU data recording synchronously across multiple watches.

## Prerequisites

### Python Requirements
```bash
pip install requests
```

### Network Requirements
- All watches and PC must be on the same WiFi network
- Watches must have WiFi enabled and connected
- No firewall blocking HTTP traffic on ports 8080-9090

### Watch Setup
1. Install the Watch IMU Recorder app on each watch
2. Grant all requested permissions (sensors, network, notifications, storage)
3. Launch the app and note the IP addresses displayed
4. Ensure server status shows "Running" (use manual "Start" button if needed)

## Watch App API Reference

### Base URL Format
```
http://[WATCH_IP]:[PORT]/[ENDPOINT]
```

### Available Endpoints

#### 1. Health Check
- **URL**: `/ping`
- **Method**: GET
- **Response**: Plain text "pong"
- **Purpose**: Test connectivity and server status

#### 2. Start Recording
- **URL**: `/start`
- **Method**: GET
- **Response**: Plain text success/error message
- **Purpose**: Begin IMU data collection
- **Side Effects**: Creates new CSV file, starts sensor sampling

#### 3. Stop Recording
- **URL**: `/stop`
- **Method**: GET
- **Response**: Plain text success/error message
- **Purpose**: End IMU data collection
- **Side Effects**: Finalizes CSV file, stops sensor sampling

#### 4. Get Status
- **URL**: `/status`
- **Method**: GET
- **Response**: JSON object
- **Purpose**: Get current recording state and sample count

#### Status Response Format
```json
{
    "server_running": true,
    "recording_state": "RECORDING",
    "sample_count": 1500,
    "ip_address": "192.168.1.101",
    "port": 8080
}
```

**Recording States:**
- `IDLE`: Not recording, ready to start
- `RECORDING`: Currently collecting IMU data
- `STOPPING`: In process of stopping (brief transition state)

## Complete Python Integration Script

```python
#!/usr/bin/env python3
"""
Watch IMU Recorder Python Integration
Complete script for controlling multiple watches for synchronized IMU data collection
"""

import requests
import time
import json
import sys
from typing import List, Dict, Optional, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class WatchController:
    """Controller for managing multiple Watch IMU Recorders"""
    
    def __init__(self, watch_ips: List[str], default_port: int = 8080, timeout: int = 5):
        """
        Initialize watch controller
        
        Args:
            watch_ips: List of watch IP addresses
            default_port: Default port to try first
            timeout: HTTP request timeout in seconds
        """
        self.watch_ips = watch_ips
        self.default_port = default_port
        self.timeout = timeout
        self.watch_ports = {}  # Store discovered ports for each watch
        
    def discover_watches(self) -> Dict[str, int]:
        """
        Discover active watches and their ports
        
        Returns:
            Dictionary mapping IP addresses to active ports
        """
        logger.info("Discovering active watches...")
        active_watches = {}
        
        # Ports to try in order
        ports_to_try = [8080, 8081, 8082, 8083, 9090]
        
        for ip in self.watch_ips:
            logger.info(f"Testing connectivity to {ip}...")
            
            for port in ports_to_try:
                try:
                    url = f"http://{ip}:{port}/ping"
                    response = requests.get(url, timeout=self.timeout)
                    
                    if response.status_code == 200 and response.text.strip() == "pong":
                        active_watches[ip] = port
                        self.watch_ports[ip] = port
                        logger.info(f"✓ Found active watch at {ip}:{port}")
                        break
                        
                except requests.RequestException:
                    continue
            
            if ip not in active_watches:
                logger.warning(f"✗ Could not connect to watch at {ip}")
        
        logger.info(f"Discovered {len(active_watches)} active watches")
        return active_watches
    
    def send_command_to_watch(self, ip: str, endpoint: str) -> Tuple[str, bool, str]:
        """
        Send command to a single watch
        
        Args:
            ip: Watch IP address
            endpoint: API endpoint (start, stop, status, ping)
            
        Returns:
            Tuple of (ip, success, response_text)
        """
        port = self.watch_ports.get(ip, self.default_port)
        url = f"http://{ip}:{port}/{endpoint}"
        
        try:
            response = requests.get(url, timeout=self.timeout)
            success = response.status_code == 200
            return ip, success, response.text
            
        except requests.RequestException as e:
            return ip, False, str(e)
    
    def send_command_to_all_watches(self, endpoint: str) -> Dict[str, Tuple[bool, str]]:
        """
        Send command to all active watches simultaneously
        
        Args:
            endpoint: API endpoint to call
            
        Returns:
            Dictionary mapping IP addresses to (success, response) tuples
        """
        results = {}
        
        # Use ThreadPoolExecutor for concurrent requests
        with ThreadPoolExecutor(max_workers=len(self.watch_ips)) as executor:
            # Submit all requests
            future_to_ip = {
                executor.submit(self.send_command_to_watch, ip, endpoint): ip 
                for ip in self.watch_ips if ip in self.watch_ports
            }
            
            # Collect results
            for future in as_completed(future_to_ip):
                ip = future_to_ip[future]
                try:
                    _, success, response = future.result()
                    results[ip] = (success, response)
                except Exception as e:
                    results[ip] = (False, str(e))
        
        return results
    
    def start_recording_all(self) -> bool:
        """
        Start recording on all watches
        
        Returns:
            True if all watches started successfully
        """
        logger.info("Starting recording on all watches...")
        results = self.send_command_to_all_watches("start")
        
        all_success = True
        for ip, (success, response) in results.items():
            if success:
                logger.info(f"✓ {ip}: {response}")
            else:
                logger.error(f"✗ {ip}: {response}")
                all_success = False
        
        return all_success
    
    def stop_recording_all(self) -> bool:
        """
        Stop recording on all watches
        
        Returns:
            True if all watches stopped successfully
        """
        logger.info("Stopping recording on all watches...")
        results = self.send_command_to_all_watches("stop")
        
        all_success = True
        for ip, (success, response) in results.items():
            if success:
                logger.info(f"✓ {ip}: {response}")
            else:
                logger.error(f"✗ {ip}: {response}")
                all_success = False
        
        return all_success
    
    def get_status_all(self) -> Dict[str, Optional[Dict]]:
        """
        Get status from all watches
        
        Returns:
            Dictionary mapping IP addresses to status dictionaries
        """
        results = self.send_command_to_all_watches("status")
        status_data = {}
        
        for ip, (success, response) in results.items():
            if success:
                try:
                    status_data[ip] = json.loads(response)
                except json.JSONDecodeError:
                    status_data[ip] = None
                    logger.error(f"Invalid JSON response from {ip}: {response}")
            else:
                status_data[ip] = None
                logger.error(f"Failed to get status from {ip}: {response}")
        
        return status_data
    
    def wait_for_recording_state(self, target_state: str, max_wait: int = 10) -> bool:
        """
        Wait for all watches to reach a specific recording state
        
        Args:
            target_state: Target state (IDLE, RECORDING, STOPPING)
            max_wait: Maximum wait time in seconds
            
        Returns:
            True if all watches reached target state
        """
        logger.info(f"Waiting for all watches to reach state: {target_state}")
        
        start_time = time.time()
        while time.time() - start_time < max_wait:
            status_data = self.get_status_all()
            
            all_ready = True
            for ip, status in status_data.items():
                if status is None:
                    all_ready = False
                    break
                    
                current_state = status.get("recording_state", "UNKNOWN")
                if current_state != target_state:
                    all_ready = False
                    break
            
            if all_ready:
                logger.info(f"All watches reached state: {target_state}")
                return True
            
            time.sleep(0.5)
        
        logger.warning(f"Timeout waiting for state: {target_state}")
        return False
    
    def synchronized_recording_session(self, duration: float) -> bool:
        """
        Perform a synchronized recording session
        
        Args:
            duration: Recording duration in seconds
            
        Returns:
            True if session completed successfully
        """
        logger.info(f"Starting synchronized recording session ({duration}s)")
        
        # 1. Check initial state
        if not self.wait_for_recording_state("IDLE", max_wait=5):
            logger.error("Not all watches are in IDLE state")
            return False
        
        # 2. Start recording on all watches
        if not self.start_recording_all():
            logger.error("Failed to start recording on all watches")
            return False
        
        # 3. Wait for recording state
        if not self.wait_for_recording_state("RECORDING", max_wait=5):
            logger.error("Not all watches entered RECORDING state")
            return False
        
        # 4. Record for specified duration
        logger.info(f"Recording for {duration} seconds...")
        time.sleep(duration)
        
        # 5. Stop recording on all watches
        if not self.stop_recording_all():
            logger.error("Failed to stop recording on all watches")
            return False
        
        # 6. Wait for idle state
        if not self.wait_for_recording_state("IDLE", max_wait=10):
            logger.warning("Not all watches returned to IDLE state")
        
        logger.info("Synchronized recording session completed")
        return True

def main():
    """Example usage of the WatchController"""
    
    # Configuration - UPDATE THESE IP ADDRESSES
    WATCH_IPS = [
        "192.168.1.101",  # Replace with your first watch IP
        "192.168.1.102",  # Replace with your second watch IP
    ]
    
    # Create controller
    controller = WatchController(WATCH_IPS)
    
    # Discover active watches
    active_watches = controller.discover_watches()
    
    if not active_watches:
        logger.error("No active watches found!")
        sys.exit(1)
    
    if len(active_watches) < len(WATCH_IPS):
        logger.warning(f"Only {len(active_watches)} of {len(WATCH_IPS)} watches are active")
    
    # Example 1: Simple start/stop
    print("\n=== Example 1: Simple Start/Stop ===")
    controller.start_recording_all()
    time.sleep(5)  # Record for 5 seconds
    controller.stop_recording_all()
    
    # Example 2: Synchronized session
    print("\n=== Example 2: Synchronized Session ===")
    controller.synchronized_recording_session(duration=10.0)
    
    # Example 3: Status monitoring
    print("\n=== Example 3: Status Check ===")
    status_data = controller.get_status_all()
    for ip, status in status_data.items():
        if status:
            print(f"{ip}: {status['recording_state']} - {status['sample_count']} samples")
        else:
            print(f"{ip}: No status available")

if __name__ == "__main__":
    main()
```

## Integration with Video Recording

### Synchronized Video + IMU Recording

```python
import subprocess
import threading

def record_video_and_imu(duration: float, video_output: str, watch_ips: List[str]):
    """
    Record video and IMU data simultaneously
    
    Args:
        duration: Recording duration in seconds
        video_output: Output video file path
        watch_ips: List of watch IP addresses
    """
    controller = WatchController(watch_ips)
    controller.discover_watches()
    
    # Start IMU recording
    logger.info("Starting IMU recording...")
    if not controller.start_recording_all():
        logger.error("Failed to start IMU recording")
        return False
    
    # Start video recording (example using ffmpeg)
    video_cmd = [
        "ffmpeg", "-f", "v4l2", "-i", "/dev/video0",
        "-t", str(duration), "-y", video_output
    ]
    
    logger.info("Starting video recording...")
    video_process = subprocess.Popen(video_cmd)
    
    # Wait for recording to complete
    video_process.wait()
    
    # Stop IMU recording
    logger.info("Stopping IMU recording...")
    controller.stop_recording_all()
    
    logger.info("Synchronized recording completed")
    return True
```

## Data Retrieval

### CSV File Format
The watch saves IMU data in CSV format with the following structure:

```csv
# Session ID: uuid-here
# Device ID: device-android-id
# Start Time: 1642680000000
# End Time: 1642680030000
# Sample Count: 6000
# Generated by Watch IMU Recorder
timestamp,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,mag_x,mag_y,mag_z
1642680000000000,0.1,-9.8,0.2,0.01,0.02,-0.01,45.2,12.3,-8.7
```

### File Locations on Watch
- **Path**: `/Android/data/com.example.watchimurecorder/files/recordings/`
- **Naming**: `imu_{device_id}_{timestamp}.csv`

### Retrieving Files via ADB

```python
import subprocess
import os

def retrieve_imu_files(output_dir: str = "./imu_data"):
    """
    Retrieve IMU CSV files from connected watches via ADB
    
    Args:
        output_dir: Local directory to save files
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # Get list of connected devices
    result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    devices = []
    
    for line in result.stdout.split('\n')[1:]:
        if '\tdevice' in line:
            devices.append(line.split('\t')[0])
    
    logger.info(f"Found {len(devices)} connected devices")
    
    for device in devices:
        logger.info(f"Retrieving files from device: {device}")
        
        # List files on device
        list_cmd = [
            "adb", "-s", device, "shell", "ls", 
            "/Android/data/com.example.watchimurecorder/files/recordings/"
        ]
        
        result = subprocess.run(list_cmd, capture_output=True, text=True)
        
        if result.returncode == 0:
            files = result.stdout.strip().split('\n')
            
            for file in files:
                if file.endswith('.csv'):
                    # Pull file from device
                    remote_path = f"/Android/data/com.example.watchimurecorder/files/recordings/{file}"
                    local_path = os.path.join(output_dir, f"{device}_{file}")
                    
                    pull_cmd = ["adb", "-s", device, "pull", remote_path, local_path]
                    subprocess.run(pull_cmd)
                    
                    logger.info(f"Retrieved: {local_path}")
        else:
            logger.warning(f"Could not access files on device: {device}")
```

## Troubleshooting

### Common Issues

1. **Server Status Shows "Stopped"**
   - Check WiFi connection on watch
   - Try manual "Start" button on watch
   - Check Android logs for detailed error messages

2. **Connection Refused Errors**
   - Verify IP addresses are correct
   - Check firewall settings
   - Ensure watches and PC are on same network

3. **Recording Doesn't Start**
   - Verify sensor permissions are granted
   - Check storage permissions
   - Ensure watch isn't already recording

### Debug Commands

```python
# Test individual watch connectivity
def debug_watch(ip: str, port: int = 8080):
    """Debug connectivity to a single watch"""
    endpoints = ["ping", "status"]
    
    for endpoint in endpoints:
        try:
            url = f"http://{ip}:{port}/{endpoint}"
            response = requests.get(url, timeout=5)
            print(f"{endpoint}: {response.status_code} - {response.text}")
        except Exception as e:
            print(f"{endpoint}: ERROR - {e}")

# Usage
debug_watch("192.168.1.101")
```

## Complete Usage Example

```python
#!/usr/bin/env python3
"""
Complete example: Record juggling session with synchronized IMU data
"""

from watch_controller import WatchController
import time
import logging

def record_juggling_session():
    # Your watch IP addresses (get these from the watch screens)
    WATCH_IPS = ["192.168.1.101", "192.168.1.102"]
    
    # Create controller
    controller = WatchController(WATCH_IPS)
    
    # Discover watches
    active_watches = controller.discover_watches()
    print(f"Found {len(active_watches)} active watches")
    
    if len(active_watches) < 2:
        print("Need at least 2 watches for juggling analysis")
        return
    
    # Record 30-second juggling session
    print("Starting juggling recording session...")
    print("Get ready to juggle!")
    
    # Countdown
    for i in range(3, 0, -1):
        print(f"{i}...")
        time.sleep(1)
    
    print("START JUGGLING!")
    
    # Start synchronized recording
    success = controller.synchronized_recording_session(duration=30.0)
    
    if success:
        print("Recording completed successfully!")
        print("Files saved on watches. Use ADB to retrieve them.")
    else:
        print("Recording failed. Check watch connections.")

if __name__ == "__main__":
    record_juggling_session()
```

This guide provides everything needed for Python integration with the Watch IMU Recorder system.