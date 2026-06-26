package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Host
import com.example.data.model.PresetAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Host::class, ChatMessage::class, PresetAgent::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun presetAgentDao(): PresetAgentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "termiagent_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialPresets(database.presetAgentDao())
                    populateInitialHosts(database.hostDao())
                }
            }
        }

        private suspend fun populateInitialPresets(dao: PresetAgentDao) {
            val initialPresets = listOf(
                PresetAgent(
                    title = "Docker Status Inspector",
                    description = "Query active containers, resources consumed, and health stats of the Docker engine.",
                    iconName = "view_module",
                    commandTemplate = "docker ps --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\" && docker stats --no-stream",
                    category = "DOCKER",
                    userPrompt = "Check all running Docker containers and print performance/resource usage statistics."
                ),
                PresetAgent(
                    title = "System Storage Clean-up",
                    description = "Audit large folders, prune unused logs, clean temporary files, and display free disk blocks.",
                    iconName = "cleaning_services",
                    commandTemplate = "df -h && sudo du -sh /var/log/* | sort -rh | head -n 5 && echo 'Clean cache targets...'",
                    category = "CLEANUP",
                    userPrompt = "Prune log buffers, free up inactive cached blocks, and clean trash folders safely."
                ),
                PresetAgent(
                    title = "Network Port Auditor",
                    description = "Inspect active TCP/UDP ports, matching processes, and socket configuration.",
                    iconName = "lan",
                    commandTemplate = "sudo ss -tulpn | grep LISTEN",
                    category = "NETWORK",
                    userPrompt = "Find which ports are currently listening for network requests and pinpoint their process names."
                ),
                PresetAgent(
                    title = "Heavy Processes Monitor",
                    description = "Examine CPU heat, system loads, and rank top 10 memory-intensive running applications.",
                    iconName = "speed",
                    commandTemplate = "ps -eo pid,ppid,cmd,%mem,%cpu --sort=-%cpu | head -n 11",
                    category = "SYSTEM",
                    userPrompt = "List the top 10 heaviest processes active right now with detailed CPU and memory percentages."
                )
            )
            dao.insertPresets(initialPresets)
        }

        private suspend fun populateInitialHosts(dao: HostDao) {
            // Prepopulate a sample host so first-launch users can play around immediately!
            dao.insertHost(
                Host(
                    name = "Central Dev Server",
                    ipOrHostname = "10.8.0.45",
                    port = 22,
                    username = "ubuntu",
                    authType = "PASSWORD",
                    secretValue = "••••••••",
                    useTailscale = true,
                    tailscaleIp = "100.92.14.88",
                    isActive = true,
                    connectionStatus = "ACTIVE",
                    cpuUsage = 24,
                    ramUsage = 48,
                    diskUsage = 61,
                    lastChecked = System.currentTimeMillis(),
                    modelAgentType = "GEMINI"
                )
            )
            dao.insertHost(
                Host(
                    name = "Raspberry Pi Gateway",
                    ipOrHostname = "192.168.1.102",
                    port = 8022,
                    username = "pi",
                    authType = "SSH_KEY",
                    secretValue = "-----BEGIN OPENSSH PRIVATE KEY-----\n...",
                    useTailscale = false,
                    tailscaleIp = "",
                    isActive = false,
                    cpuUsage = 0,
                    ramUsage = 0,
                    diskUsage = 0,
                    lastChecked = 0,
                    modelAgentType = "HERMES_API",
                    hermesApiUrl = "https://api.openrouter.ai/api/v1",
                    hermesModelName = "nousresearch/hermes-2-pro-llama-3-8b"
                )
            )
        }
    }
}
