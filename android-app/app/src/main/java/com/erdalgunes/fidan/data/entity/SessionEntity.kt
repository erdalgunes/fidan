package com.erdalgunes.fidan.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a focus session.
 * YAGNI: Only essential fields, can extend later if needed.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val startTime: Date,
    val endTime: Date?,
    val targetDurationMillis: Long,
    val actualDurationMillis: Long,
    val isCompleted: Boolean,
    val taskName: String?,
    val treeId: String?
)