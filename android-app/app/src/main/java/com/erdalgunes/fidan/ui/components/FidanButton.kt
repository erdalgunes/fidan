package com.erdalgunes.fidan.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.erdalgunes.fidan.ui.theme.Dimensions

/**
 * Reusable Button components following DRY principle
 */
@Composable
fun FidanPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text)
    }
}

@Composable
fun FidanOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text)
    }
}

/**
 * Timer control floating action button
 */
@Composable
fun TimerControlButton(
    onClick: () -> Unit,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.TimerButtonSize
) {
    val (icon, color, contentDescription) = if (isRunning) {
        Triple(
            Icons.Default.Stop,
            MaterialTheme.colorScheme.error,
            "Stop"
        )
    } else {
        Triple(
            Icons.Default.PlayArrow,
            MaterialTheme.colorScheme.primary,
            "Start"
        )
    }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(size),
        containerColor = color,
        shape = CircleShape
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.5f),
            tint = Color.White
        )
    }
}