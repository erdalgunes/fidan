package com.erdalgunes.fidan.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Shared session model for KMP.
 * KISS: Simple data class with essential properties.
 * DRY: Single model used across all platforms.
 */
@Serializable
data class Session(
    val id: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val targetDurationSeconds: Long,
    val actualDurationSeconds: Long = 0,
    val isCompleted: Boolean = false,
    val taskName: String? = null,
    val treeId: String? = null
) {
    val progress: Float
        get() = if (targetDurationSeconds > 0) {
            actualDurationSeconds.toFloat() / targetDurationSeconds.toFloat()
        } else 0f
    
    val remainingSeconds: Long
        get() = (targetDurationSeconds - actualDurationSeconds).coerceAtLeast(0)
}