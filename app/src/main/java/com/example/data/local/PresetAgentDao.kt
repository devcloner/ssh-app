package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PresetAgent
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetAgentDao {
    @Query("SELECT * FROM preset_agents ORDER BY title ASC")
    fun getAllPresets(): Flow<List<PresetAgent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetAgent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetAgent>)

    @Query("SELECT COUNT(*) FROM preset_agents")
    suspend fun getPresetCount(): Int
}
