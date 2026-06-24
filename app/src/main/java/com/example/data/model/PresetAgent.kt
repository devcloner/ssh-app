package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preset_agents")
data class PresetAgent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val iconName: String, // e.g. "dns", "terminal", "delete", "bolt"
    val commandTemplate: String,
    val category: String, // "SYSTEM", "DOCKER", "NETWORK", "CLEANUP"
    val userPrompt: String
)
