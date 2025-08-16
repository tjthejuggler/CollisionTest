package com.example.watchimurecorder.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.watchimurecorder.R
import kotlin.math.abs

@Composable
fun SwipeToRevealMenu(
    modifier: Modifier = Modifier,
    onShutdown: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isMenuVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    // Animation for menu visibility
    val menuAlpha by animateFloatAsState(
        targetValue = if (isMenuVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuAlpha"
    )
    
    // Animation for content offset
    val contentOffsetX by animateFloatAsState(
        targetValue = if (isMenuVisible) with(density) { 80.dp.toPx() } else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "contentOffset"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Side menu (hidden by default)
        if (isMenuVisible || menuAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(Color.Black.copy(alpha = 0.9f))
                    .alpha(menuAlpha),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Shutdown button
                    Button(
                        onClick = {
                            onShutdown()
                            isMenuVisible = false
                        },
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        ),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Shutdown",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = "Shutdown",
                        style = MaterialTheme.typography.caption2,
                        color = Color.White
                    )
                }
            }
        }

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = with(density) { contentOffsetX.toDp() })
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Determine if menu should stay open or close
                            val threshold = size.width * 0.2f // 20% of screen width
                            isMenuVisible = offsetX > threshold
                            offsetX = 0f
                        }
                    ) { change, _ ->
                        // Only allow right swipe (positive X direction)
                        val newOffsetX = offsetX + change.position.x
                        if (newOffsetX >= 0) {
                            offsetX = newOffsetX.coerceAtMost(size.width * 0.4f) // Max 40% of screen
                            
                            // Show menu when dragging starts
                            if (abs(change.position.x) > 10f && !isMenuVisible) {
                                isMenuVisible = true
                            }
                        }
                    }
                }
        ) {
            content()
        }
        
        // Overlay to close menu when tapping outside
        if (isMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = with(density) { contentOffsetX.toDp() })
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ ->
                            isMenuVisible = false
                        }
                    }
            )
        }
    }
}