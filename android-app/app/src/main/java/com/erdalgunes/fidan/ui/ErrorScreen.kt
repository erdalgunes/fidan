package com.erdalgunes.fidan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable error screen component following DRY principles.
 * Shows user-friendly error messages with recovery options.
 */
@Composable
fun ErrorScreen(
    paddingValues: PaddingValues = PaddingValues(),
    message: String,
    canRecover: Boolean = true,
    onRetry: (() -> Unit)? = null,
    additionalInfo: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFEBEE),  // Light red background for errors
                        Color(0xFFFFCDD2)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFD32F2F)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error title
        Text(
            text = "Oops! Something went wrong",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Error message
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                if (additionalInfo != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = additionalInfo,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recovery button
        if (canRecover && onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Help text
        Text(
            text = if (canRecover) 
                "Don't worry, your progress is safe! Tap 'Try Again' to continue." 
            else 
                "Your progress has been saved. Please restart the app if the problem persists.",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Loading screen component for async operations.
 * Follows KISS principle with simple, clear design.
 */
@Composable
fun LoadingScreen(
    paddingValues: PaddingValues = PaddingValues(),
    message: String = "Loading...",
    showProgress: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),  // Light blue for loading
                        Color(0xFFBBDEFB)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF2196F3),
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF424242),
            textAlign = TextAlign.Center
        )
    }
}