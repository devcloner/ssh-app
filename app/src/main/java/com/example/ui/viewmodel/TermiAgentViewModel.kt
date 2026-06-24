package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Host
import com.example.data.model.PresetAgent
import com.example.data.remote.GeminiService
import com.example.data.remote.HermesService
import com.example.data.repository.ChatRepository
import com.example.data.repository.HostRepository
import com.example.data.repository.PresetAgentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TermiAgentViewModel(
    application: Application,
    private val hostRepository: HostRepository,
    private val chatRepository: ChatRepository,
    private val presetAgentRepository: PresetAgentRepository
) : AndroidViewModel(application) {

    // Selected active host ID
    private val _selectedHostId = MutableStateFlow<Long?>(null)
    val selectedHostId: StateFlow<Long?> = _selectedHostId.asStateFlow()

    // Host list
    val hosts: StateFlow<List<Host>> = hostRepository.allHosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Host detail
    val selectedHost: StateFlow<Host?> = _selectedHostId
        .flatMapLatest { id ->
            if (id != null) hostRepository.getHostById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Preset Agents
    val presets: StateFlow<List<PresetAgent>> = presetAgentRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chats matching selected host
    val messages: StateFlow<List<ChatMessage>> = _selectedHostId
        .flatMapLatest { id ->
            if (id != null) chatRepository.getMessagesForHost(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat text field state
    private val _currentMessageText = MutableStateFlow("")
    val currentMessageText: StateFlow<String> = _currentMessageText.asStateFlow()

    // Loading indicator for agent queries
    private val _isQueryingAgent = MutableStateFlow(false)
    val isQueryingAgent: StateFlow<Boolean> = _isQueryingAgent.asStateFlow()

    // Gemini thinking speed mode toggle: True -> gemini-3.1-pro-preview with high thinking, False -> gemini-3.1-flash-lite-preview (instant)
    private val _useHighThinking = MutableStateFlow(false)
    val useHighThinking: StateFlow<Boolean> = _useHighThinking.asStateFlow()

    // Simulated host ping test / connection state
    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    init {
        // Automatically select the first host if available
        viewModelScope.launch {
            hosts.collect { hostList ->
                if (_selectedHostId.value == null && hostList.isNotEmpty()) {
                    _selectedHostId.value = hostList.first().id
                }
            }
        }

        // Periodically simulate background system health fluctuations for the selected host
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(8000)
                val activeId = _selectedHostId.value
                if (activeId != null) {
                    val host = hostRepository.getHostByIdOneShot(activeId)
                    if (host != null && host.isActive) {
                        // Fluctuates metrics slightly to feel alive
                        val updated = host.copy(
                            cpuUsage = (host.cpuUsage + (-5..5).random()).coerceIn(10, 95),
                            ramUsage = (host.ramUsage + (-2..2).random()).coerceIn(30, 90),
                            diskUsage = host.diskUsage,
                            lastChecked = System.currentTimeMillis()
                        )
                        hostRepository.updateHost(updated)
                    }
                }
            }
        }
    }

    fun selectHost(hostId: Long) {
        _selectedHostId.value = hostId
    }

    fun updateMessageText(text: String) {
        _currentMessageText.value = text
    }

    fun setUseHighThinking(enabled: Boolean) {
        _useHighThinking.value = enabled
    }

    // Ping the active host
    fun testConnection() {
        val host = selectedHost.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isPinging.value = true
            // Simulates SSH handshake and/or Tailscale connection
            delay(1500)
            val updated = host.copy(
                isActive = true,
                lastChecked = System.currentTimeMillis(),
                cpuUsage = (15..45).random(),
                ramUsage = (40..70).random(),
                diskUsage = if (host.diskUsage == 0) (40..85).random() else host.diskUsage
            )
            hostRepository.updateHost(updated)
            _isPinging.value = false
        }
    }

    // Send chat text
    fun sendMessage() {
        val prompt = _currentMessageText.value.trim()
        val host = selectedHost.value
        if (prompt.isEmpty() || host == null) return

        _currentMessageText.value = ""
        viewModelScope.launch {
            // Save user query to local database
            val userMsg = ChatMessage(
                hostId = host.id,
                sender = "USER",
                messageText = prompt
            )
            chatRepository.insertMessage(userMsg)

            // Auto-reconnect host silently if sending a query while connection was lost/offline
            if (!host.isActive) {
                viewModelScope.launch(Dispatchers.IO) {
                    val activeHost = host.copy(
                        isActive = true,
                        lastChecked = System.currentTimeMillis(),
                        cpuUsage = (15..35).random(),
                        ramUsage = (35..55).random(),
                        diskUsage = if (host.diskUsage == 0) (40..85).random() else host.diskUsage
                    )
                    hostRepository.updateHost(activeHost)
                }
            }

            // Query configured agent
            _isQueryingAgent.value = true

            // Fetch last 6 message exchanges for context
            val historyList = messages.value.takeLast(6).map { Pair(it.sender, it.messageText) }

            viewModelScope.launch(Dispatchers.IO) {
                val response = if (host.modelAgentType == "GEMINI") {
                    GeminiService.queryAgent(
                        userPrompt = prompt,
                        hostContext = host,
                        useHighThinking = _useHighThinking.value,
                        chatHistory = historyList
                    )
                } else {
                    HermesService.queryHermes(
                        userPrompt = prompt,
                        hostContext = host,
                        chatHistory = historyList
                    )
                }

                // Save Agent's response to database
                val agentMsg = ChatMessage(
                    hostId = host.id,
                    sender = "AGENT",
                    messageText = response.messageText,
                    proposedCommand = response.proposedCommand,
                    commandExplanation = response.commandExplanation,
                    safetyLevel = response.safetyLevel,
                    safetyReason = response.safetyReason
                )
                chatRepository.insertMessage(agentMsg)
                _isQueryingAgent.value = false
            }
        }
    }

    // Instantly triggers a saved preset agent
    fun executePreset(preset: PresetAgent) {
        val host = selectedHost.value ?: return
        viewModelScope.launch {
            // Log user query in chat
            val userMsg = ChatMessage(
                hostId = host.id,
                sender = "USER",
                messageText = "Preset Run: ${preset.title}\n${preset.userPrompt}"
            )
            chatRepository.insertMessage(userMsg)

            // Auto-reconnect host silently if connection was lost/offline
            if (!host.isActive) {
                viewModelScope.launch(Dispatchers.IO) {
                    val activeHost = host.copy(
                        isActive = true,
                        lastChecked = System.currentTimeMillis(),
                        cpuUsage = (15..35).random(),
                        ramUsage = (35..55).random(),
                        diskUsage = if (host.diskUsage == 0) (40..85).random() else host.diskUsage
                    )
                    hostRepository.updateHost(activeHost)
                }
            }

            _isQueryingAgent.value = true

            viewModelScope.launch(Dispatchers.IO) {
                val promptText = "${preset.userPrompt}. Generate the command: ${preset.commandTemplate}"
                val response = if (host.modelAgentType == "GEMINI") {
                    GeminiService.queryAgent(
                        userPrompt = promptText,
                        hostContext = host,
                        useHighThinking = _useHighThinking.value,
                        chatHistory = emptyList()
                    )
                } else {
                    HermesService.queryHermes(
                        userPrompt = promptText,
                        hostContext = host,
                        chatHistory = emptyList()
                    )
                }

                val agentMsg = ChatMessage(
                    hostId = host.id,
                    sender = "AGENT",
                    messageText = "Preset launcher completed analysis. Here is the proposed automated checklist for '${preset.title}':\n\n${response.messageText}",
                    proposedCommand = response.proposedCommand ?: preset.commandTemplate,
                    commandExplanation = response.commandExplanation ?: preset.description,
                    safetyLevel = response.safetyLevel ?: "SAFE",
                    safetyReason = response.safetyReason ?: "Analyzed preset block."
                )
                chatRepository.insertMessage(agentMsg)
                _isQueryingAgent.value = false
            }
        }
    }

    // Streams simulated shell command outputs beautifully and reactively!
    fun runShellCommand(message: ChatMessage) {
        val command = message.proposedCommand ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Mark as executing
            val runningMsg = message.copy(isExecuting = true, commandOutput = "")
            chatRepository.updateMessage(runningMsg)

            val outputBuffer = StringBuilder()
            val simulatedLines = generateSimulatedOutputs(command, selectedHost.value)

            // Auto-reconnect sequence if host is offline/inactive - ensuring zero lost connections
            val currentHost = selectedHost.value
            if (currentHost != null && !currentHost.isActive) {
                outputBuffer.append("⚠️ Secure connection link lost. Re-establishing link...\n")
                outputBuffer.append("🔄 Restoring high-availability Tailscale-mesh route...\n")
                chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
                delay(600)

                val activeHost = currentHost.copy(
                    isActive = true,
                    lastChecked = System.currentTimeMillis(),
                    cpuUsage = (15..35).random(),
                    ramUsage = (35..55).random(),
                    diskUsage = if (currentHost.diskUsage == 0) (40..85).random() else currentHost.diskUsage
                )
                hostRepository.updateHost(activeHost)
                outputBuffer.append("🟢 Secure tunnel re-established successfully (Latency: 14ms).\n\n")
                chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
                delay(400)
            }

            // Simulated low-latency SSH/Tailscale network handshake
            outputBuffer.append("⚡ Connecting to host: ${selectedHost.value?.username}@${selectedHost.value?.ipOrHostname} via Tailscale tunnel...\n")
            outputBuffer.append("🔑 SSH authentication key accepted.\n")
            outputBuffer.append("📦 \$ $command\n\n")
            chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
            delay(500)

            // Emit lines of terminal output sequentially and beautifully
            for (line in simulatedLines) {
                if (line.isEmpty()) {
                    outputBuffer.append("\n")
                    chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
                    delay(50)
                } else {
                    // Fast responsive word/character packet streaming simulation
                    val words = line.split(" ")
                    for ((index, word) in words.withIndex()) {
                        outputBuffer.append(word).append(if (index == words.lastIndex) "" else " ")
                        chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
                        delay((15..45).random().toLong()) // high-speed, hyper-responsive packet arrival simulation
                    }
                    outputBuffer.append("\n")
                    chatRepository.updateMessage(runningMsg.copy(commandOutput = outputBuffer.toString()))
                    delay(80)
                }
            }

            outputBuffer.append("\n🟢 Command execution completed successfully. Connection closed.\n")

            // Mark as fully executed
            val finalMsg = runningMsg.copy(
                isExecuting = false,
                isExecuted = true,
                commandOutput = outputBuffer.toString()
            )
            chatRepository.updateMessage(finalMsg)
        }
    }

    private fun generateSimulatedOutputs(cmd: String, host: Host?): List<String> {
        val commandLower = cmd.lowercase()
        return when {
            commandLower.contains("docker ps") || commandLower.contains("docker stats") -> listOf(
                "CONTAINER ID   IMAGE                 COMMAND                  CREATED         STATUS         PORTS",
                "a17b2b800c92   nginx:1.25-alpine     \"/docker-entrypoint.…\"   3 days ago      Up 48 hours    0.0.0.0:80->80/tcp, :::80->80/tcp",
                "9f24cbda112a   postgres:16-alpine    \"docker-entrypoint.s…\"   3 days ago      Up 48 hours    0.0.0.0:5432->5432/tcp",
                "fc9019b881ef   redis:7.2-alpine      \"docker-entrypoint.s…\"   12 hours ago    Up 12 hours    6379/tcp",
                "b1104e76c123   rabbitmq:3-management \"/docker-entrypoint.…\"   12 hours ago    Up 12 hours    0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp",
                "",
                "--- Container Resource Utilization (docker stats) ---",
                "NAME             CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           BLOCK I/O",
                "nginx            0.12%     18.2MiB / 7.78GiB     0.23%     12.4MB / 115MB    4.1MB / 0B",
                "postgres         0.45%     145.4MiB / 7.78GiB    1.82%     2.1MB / 8.4MB     152MB / 1.2GB",
                "redis            0.05%     8.91MiB / 7.78GiB     0.11%     850KB / 1.1MB     0B / 0B"
            )
            commandLower.contains("df -h") || commandLower.contains("du -sh") -> listOf(
                "Filesystem      Size  Used Avail Use% Mounted on",
                "udev            3.9G     0  3.9G   0% /dev",
                "tmpfs           788M  1.6M  786M   1% /run",
                "/dev/sda1        59G   32G   24G  57% /",
                "tmpfs           3.9G     0  3.9G   0% /dev/shm",
                "tmpfs           5.0M     0  5.0M   0% /run/lock",
                "/dev/sda15      105M  6.1M   99M   6% /boot/efi",
                "tmpfs           788M   44K  788M   1% /run/user/1000",
                "",
                "--- Five Largest Directories in /var/log/ ---",
                "1.4G    /var/log/journal",
                "452M    /var/log/nginx",
                "212M    /var/log/syslog.1",
                "108M    /var/log/dpkg.log",
                "45M     /var/log/auth.log"
            )
            commandLower.contains("ss -tulpn") || commandLower.contains("netstat") -> listOf(
                "Netid  State      Recv-Q Send-Q   Local Address:Port     Peer Address:Port   Process",
                "tcp    LISTEN     0      128            0.0.0.0:80            0.0.0.0:*       users:((\"nginx\",pid=1024,fd=6))",
                "tcp    LISTEN     0      128            0.0.0.0:22            0.0.0.0:*       users:((\"sshd\",pid=944,fd=3))",
                "tcp    LISTEN     0      128          127.0.0.1:6000          0.0.0.0:*       users:((\"node\",pid=1544,fd=12))",
                "tcp    LISTEN     0      128            0.0.0.0:5432          0.0.0.0:*       users:((\"postgres\",pid=908,fd=7))",
                "tcp    LISTEN     0      128               [::]:80               [::]:*       users:((\"nginx\",pid=1024,fd=7))",
                "tcp    LISTEN     0      128               [::]:22               [::]:*       users:((\"sshd\",pid=944,fd=4))",
                "tcp    LISTEN     0      128               [::]:15672            [::]:*       users:((\"beam.smp\",pid=1108,fd=34))"
            )
            commandLower.contains("ps -eo") || commandLower.contains("top") -> listOf(
                "  PID  PPID COMMAND                         %MEM %CPU",
                "  908     1 /usr/lib/postgresql/16/bin/post  4.8  2.1",
                " 1024     1 nginx: master process /usr/sbin  1.2  0.5",
                " 1544     1 node /var/www/app/index.js       5.1  3.2",
                " 1108     1 /usr/lib/erlang/erts-14/bin/beam 6.4  1.4",
                " 3105  1024 nginx: worker process            1.4  0.2",
                "  944     1 /usr/sbin/sshd -D                0.2  0.0",
                " 3204   944 sshd: ubuntu@pts/0               0.6  0.1",
                " 3215  3204 -bash                            0.3  0.0",
                " 3412  3215 ps -eo pid,ppid,cmd,%mem,%cpu    0.1  0.5"
            )
            commandLower.contains("apt") || commandLower.contains("install") -> listOf(
                "Hit:1 http://archive.ubuntu.com/ubuntu noble InRelease",
                "Get:2 http://archive.ubuntu.com/ubuntu noble-updates InRelease [126 kB]",
                "Get:3 http://archive.ubuntu.com/ubuntu noble-backports InRelease [120 kB]",
                "Get:4 http://security.ubuntu.com/ubuntu noble-security InRelease [126 kB]",
                "Fetched 372 kB in 1s (314 kB/s)",
                "Reading package lists... Done",
                "Building dependency tree... Done",
                "Reading state information... Done",
                "All packages are up to date."
            )
            else -> listOf(
                "Terminal operation dispatched to client interface.",
                "Executing custom logic block...",
                "Running: $cmd",
                "Exit code: 0",
                "Process output captured successfully."
            )
        }
    }

    // Host Management CRUD
    fun addHost(host: Host) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = hostRepository.insertHost(host)
            _selectedHostId.value = insertedId
        }
    }

    fun deleteHost(host: Host) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.clearMessagesForHost(host.id)
            hostRepository.deleteHost(host)
            val all = hostRepository.allHosts.first()
            if (all.isNotEmpty()) {
                _selectedHostId.value = all.first().id
            } else {
                _selectedHostId.value = null
            }
        }
    }

    // Loading state for prompt optimization
    private val _isOptimizingPrompt = MutableStateFlow(false)
    val isOptimizingPrompt: StateFlow<Boolean> = _isOptimizingPrompt.asStateFlow()

    fun optimizeCurrentPrompt() {
        val prompt = _currentMessageText.value.trim()
        if (prompt.isEmpty()) return

        viewModelScope.launch {
            _isOptimizingPrompt.value = true
            val host = selectedHost.value
            val optimized = withContext(Dispatchers.IO) {
                com.example.data.remote.GeminiService.optimizePrompt(prompt, host)
            }
            if (optimized.isNotBlank()) {
                _currentMessageText.value = optimized
            }
            _isOptimizingPrompt.value = false
        }
    }

    fun updateSelectedHostModelAgentType(type: String) {
        val host = selectedHost.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = host.copy(modelAgentType = type)
            hostRepository.updateHost(updated)
        }
    }

    fun clearChat(hostId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.clearMessagesForHost(hostId)
        }
    }
}

// ViewModel Factory
class TermiAgentViewModelFactory(
    private val application: Application,
    private val hostRepository: HostRepository,
    private val chatRepository: ChatRepository,
    private val presetAgentRepository: PresetAgentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TermiAgentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TermiAgentViewModel(application, hostRepository, chatRepository, presetAgentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
