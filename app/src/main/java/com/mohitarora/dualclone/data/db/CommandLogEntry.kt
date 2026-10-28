package com.mohitarora.dualclone.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_log")
data class CommandLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val command: String,
    val result: String,
    val success: Boolean,
    val timestampMs: Long = System.currentTimeMillis(),
    val userId: Int = -1,
    val exitCode: Int = 0
)
