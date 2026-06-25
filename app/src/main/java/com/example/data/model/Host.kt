package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ipOrHostname: String,
    val port: Int = 22,
    val username: String,
    val authType: String = "PASSWORD", // "PASSWORD" or "SSH_KEY"
    val secretValue: String = "",
    val useTailscale: Boolean = false,
    val tailscaleIp: String = "",
    val isActive: Boolean = false,
    val connectionStatus: String = "OFFLINE", // "ACTIVE", "OFFLINE", "AUTHENTICATING"
    val lastChecked: Long = 0,
    val cpuUsage: Int = 0,
    val ramUsage: Int = 0,
    val diskUsage: Int = 0,
    val modelAgentType: String = "GEMINI", // "GEMINI", "HERMES_API", "HERMES_SSH"
    val hermesApiUrl: String = "https://api.openrouter.ai/api/v1", // Default to OpenRouter or Ollama
    val hermesApiKey: String = "",
    val hermesModelName: String = "nousresearch/hermes-2-pro-llama-3-8b"
)
