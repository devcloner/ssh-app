package com.example.data.repository

import com.example.data.local.HostDao
import com.example.data.model.Host
import kotlinx.coroutines.flow.Flow

class HostRepository(private val hostDao: HostDao) {
    val allHosts: Flow<List<Host>> = hostDao.getAllHosts()

    fun getHostById(id: Long): Flow<Host?> = hostDao.getHostById(id)

    suspend fun getHostByIdOneShot(id: Long): Host? = hostDao.getHostByIdOneShot(id)

    suspend fun insertHost(host: Host): Long = hostDao.insertHost(host)

    suspend fun updateHost(host: Host) = hostDao.updateHost(host)

    suspend fun deleteHost(host: Host) = hostDao.deleteHost(host)

    suspend fun deleteHostById(id: Long) = hostDao.deleteHostById(id)
}
