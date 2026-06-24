package com.example.data.repository

import com.example.data.local.PresetAgentDao
import com.example.data.model.PresetAgent
import kotlinx.coroutines.flow.Flow

class PresetAgentRepository(private val presetAgentDao: PresetAgentDao) {
    val allPresets: Flow<List<PresetAgent>> = presetAgentDao.getAllPresets()

    suspend fun insertPreset(preset: PresetAgent) = presetAgentDao.insertPreset(preset)

    suspend fun insertPresets(presets: List<PresetAgent>) = presetAgentDao.insertPresets(presets)
}
