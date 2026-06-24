package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Host
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    @Query("SELECT * FROM hosts ORDER BY name ASC")
    fun getAllHosts(): Flow<List<Host>>

    @Query("SELECT * FROM hosts WHERE id = :id LIMIT 1")
    fun getHostById(id: Long): Flow<Host?>

    @Query("SELECT * FROM hosts WHERE id = :id LIMIT 1")
    suspend fun getHostByIdOneShot(id: Long): Host?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: Host): Long

    @Update
    suspend fun updateHost(host: Host)

    @Delete
    suspend fun deleteHost(host: Host)

    @Query("DELETE FROM hosts WHERE id = :id")
    suspend fun deleteHostById(id: Long)
}
