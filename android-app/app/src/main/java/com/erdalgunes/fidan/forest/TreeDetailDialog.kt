package com.erdalgunes.fidan.forest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.erdalgunes.fidan.data.Tree
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TreeDetailDialog(
    tree: Tree,
    onDismiss: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val durationMinutes = tree.sessionData.durationMillis / (1000 * 60)
    
    // Full screen overlay with modal background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable { /* Prevent click through */ }
                .semantics {
                    contentDescription = "Tree details dialog for ${tree.treeType.displayName}"
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with tree emoji and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tree.treeType.emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = tree.treeType.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics {
                            contentDescription = "Close tree details dialog"
                            role = Role.Button
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null, // Handled by semantics above
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = if (tree.sessionData.wasCompleted) {
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            } else {
                                Color(0xFFFF9800).copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (tree.sessionData.wasCompleted) "Session Completed" else "Session Stopped Early",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (tree.sessionData.wasCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Session details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tree.sessionData.taskName != null) {
                        DetailRow(
                            label = "Task:",
                            value = tree.sessionData.taskName
                        )
                    }
                    
                    DetailRow(
                        label = "Duration:",
                        value = "${durationMinutes}min ${if (durationMinutes < 25) "(incomplete)" else ""}"
                    )
                    
                    DetailRow(
                        label = "Completed:",
                        value = dateFormatter.format(tree.sessionData.completedDate)
                    )
                    
                    DetailRow(
                        label = "Tree Planted:",
                        value = dateFormatter.format(tree.plantedDate)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Motivational message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (tree.sessionData.wasCompleted) {
                            "Great work! You completed a full focus session and helped your forest grow!"
                        } else {
                            "Every effort counts! Keep practicing and your forest will flourish."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}