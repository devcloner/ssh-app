package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val sender: String, // "USER" or "AGENT"
    val messageText: String,
    val proposedCommand: String? = null,
    val commandExplanation: String? = null,
    val safetyLevel: String? = null, // "SAFE", "WARN", "DANGEROUS"
    val safetyReason: String? = null,
    val isExecuted: Boolean = false,
    val commandOutput: String? = null,
    val isExecuting: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
