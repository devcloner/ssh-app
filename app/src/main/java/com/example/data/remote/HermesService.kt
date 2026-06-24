package com.example.data.remote

import android.util.Log
import com.example.data.model.Host
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object HermesService {
    private const val TAG = "HermesService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun queryHermes(
        userPrompt: String,
        hostContext: Host,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): GeminiService.AgentResponse = withContext(Dispatchers.IO) {
        
        val systemInstruction = """
            You are Nous Hermes 2 Pro, a highly advanced agentic assistant running directly on the node '${hostContext.name}' (${hostContext.username}@${hostContext.ipOrHostname}).
            Your goal is to parse natural language instructions and propose safe, precise terminal commands to maintain and manage this machine.
            
            Host Parameters:
            - IP/Host: ${hostContext.ipOrHostname}
            - SSH Port: ${hostContext.port}
            - Username: ${hostContext.username}
            - Tailscale: ${if (hostContext.useTailscale) "Yes (" + hostContext.tailscaleIp + ")" else "No"}
            
            YOU MUST RESPOND ONLY IN VALID JSON matching this schema:
            {
              "messageText": "A helpful agentic response from Nous Hermes, describing the diagnosed server state.",
              "proposedCommand": "The actual shell command to execute, or null/empty.",
              "commandExplanation": "Explanation of the command parameters.",
              "safetyLevel": "SAFE" or "WARN" or "DANGEROUS",
              "safetyReason": "Analysis of host security risks."
            }
            
            Rules:
            - SAFE: Read-only queries (ls, cat, systemctl status, docker ps, free, top).
            - WARN: Non-destructive state changes (restarting nginx, updating apt, docker-compose build).
            - DANGEROUS: Deleting files, modifying core routes, killing processes, reboots (rm, kill, reboot).
            
            Output RAW JSON ONLY. No markdown triple-backtick blocks.
        """.trimIndent()

        // If the API URL is empty or unconfigured or uses local SSH, we will generate a robust offline Hermes response!
        val useSimulation = hostContext.modelAgentType == "HERMES_SSH" || 
                hostContext.hermesApiUrl.isBlank() || 
                hostContext.hermesApiUrl.contains("localhost") || 
                hostContext.hermesApiUrl.contains("127.0.0.1")

        if (useSimulation) {
            return@withContext runSimulation(userPrompt, hostContext)
        }

        try {
            val messagesArray = JSONArray()
            
            // Add system instruction
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })

            // Add chat history
            chatHistory.forEach { (sender, text) ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (sender == "USER") "user" else "assistant")
                    put("content", text)
                })
            }

            // Add current user prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })

            val requestBodyJson = JSONObject().apply {
                put("model", hostContext.hermesModelName)
                put("messages", messagesArray)
                put("temperature", 0.1)
                put("response_format", JSONObject().apply { put("type", "json_object") })
            }

            val requestUrl = if (hostContext.hermesApiUrl.endsWith("/")) {
                "${hostContext.hermesApiUrl}chat/completions"
            } else {
                "${hostContext.hermesApiUrl}/chat/completions"
            }

            val requestBuilder = Request.Builder()
                .url(requestUrl)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))

            if (hostContext.hermesApiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${hostContext.hermesApiKey}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Hermes API response: $responseBodyStr")

                if (!response.isSuccessful) {
                    return@withContext runSimulation(userPrompt, hostContext, isFallback = true, fallbackReason = "API Code ${response.code}")
                }

                val responseJson = JSONObject(responseBodyStr)
                val choices = responseJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext runSimulation(userPrompt, hostContext, isFallback = true, fallbackReason = "Empty choices returned")
                }

                val contentText = choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
                
                try {
                    val parsed = JSONObject(contentText)
                    return@withContext GeminiService.AgentResponse(
                        messageText = parsed.optString("messageText", "Hermes Agent processed request."),
                        proposedCommand = parsed.optString("proposedCommand", "").takeIf { it.isNotBlank() },
                        commandExplanation = parsed.optString("commandExplanation", "").takeIf { it.isNotBlank() },
                        safetyLevel = parsed.optString("safetyLevel", "SAFE"),
                        safetyReason = parsed.optString("safetyReason", "")
                    )
                } catch (pe: Exception) {
                    // Try to extract JSON or fallback
                    Log.e(TAG, "Failed parsing inner JSON from Hermes response. Raw content: $contentText", pe)
                    return@withContext GeminiService.AgentResponse(
                        messageText = contentText,
                        safetyLevel = "WARN",
                        safetyReason = "Parsed raw response from Hermes"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hermes API Call failed, falling back to simulated engine", e)
            return@withContext runSimulation(userPrompt, hostContext, isFallback = true, fallbackReason = e.localizedMessage)
        }
    }

    private fun runSimulation(
        userPrompt: String, 
        host: Host, 
        isFallback: Boolean = false, 
        fallbackReason: String? = null
    ): GeminiService.AgentResponse {
        val normalized = userPrompt.lowercase()
        
        val prefix = if (isFallback) {
            "⚡ [Hermes Sandbox Mode - Fallback from $fallbackReason]\n"
        } else if (host.modelAgentType == "HERMES_SSH") {
            "🤖 [Hermes Local SSH Agent Daemon]\n"
        } else {
            "🛸 [Hermes Native API Sandbox]\n"
        }

        val promptType = when {
            normalized.contains("docker") -> "docker"
            normalized.contains("log") || normalized.contains("journal") -> "logs"
            normalized.contains("port") || normalized.contains("listen") || normalized.contains("net") -> "network"
            normalized.contains("cleanup") || normalized.contains("free") || normalized.contains("disk") || normalized.contains("storage") -> "disk"
            normalized.contains("process") || normalized.contains("top") || normalized.contains("cpu") -> "processes"
            else -> "generic"
        }

        // Nous Hermes style with tool scratchpads!
        val scratchpadText = """
            <scratchpad>
            Agent has established active SSH session with ${host.username}@${host.ipOrHostname}:${host.port}.
            Intent identified: User wants to check / modify system state for: '$promptType'.
            Formulating secure bash command...
            Applying safety filter: Target machine is active: ${host.isActive}.
            </scratchpad>
        """.trimIndent()

        return when (promptType) {
            "docker" -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes detected Docker request on ${host.name}. Examining live containers and system overlays.",
                proposedCommand = "docker ps --all --format 'table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Image}}' && docker stats --no-stream",
                commandExplanation = "Retrieves a comprehensive list of all containers (including exited ones) along with an immediate telemetry report on CPU/Memory consumption.",
                safetyLevel = "SAFE",
                safetyReason = "Read-only container inspections. Zero service interruption."
            )
            "logs" -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes agent scanning system logs. Restricting query to the last 100 systemd lines to prevent terminal buffer overflow.",
                proposedCommand = "sudo journalctl -n 100 --no-pager -u ssh",
                commandExplanation = "Checks the recent SSH connection attempts to audit failed handshakes or auth anomalies.",
                safetyLevel = "SAFE",
                safetyReason = "Inspects system logs only."
            )
            "network" -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes running active socket mapping on port gateways. Auditing active TCP/UDP ports.",
                proposedCommand = "sudo ss -tulpn | grep -i 'listen'",
                commandExplanation = "Queries the kernel's active sockets, filtering exclusively for processes in the LISTEN state.",
                safetyLevel = "SAFE",
                safetyReason = "Queries network tables; does not alter socket configurations."
            )
            "disk" -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes analyzing block storage blocks and volumes. Triggering disk capacity inspection.",
                proposedCommand = "df -hT && sudo du -hd1 /var/log | sort -h",
                commandExplanation = "Retrieves disk utilization in human-readable blocks and audits log directory sizes.",
                safetyLevel = "SAFE",
                safetyReason = "Reads filesystem blocks."
            )
            "processes" -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes compiling real-time CPU telemetry and heavy processes.",
                proposedCommand = "ps -eo pid,ppid,cmd,%mem,%cpu --sort=-%cpu | head -n 10",
                commandExplanation = "Queries system task scheduler to retrieve top 10 most intensive CPU consuming threads.",
                safetyLevel = "SAFE",
                safetyReason = "Read-only system check."
            )
            else -> GeminiService.AgentResponse(
                messageText = "$prefix$scratchpadText\nNous Hermes Agent received: '$userPrompt'. Crafting corresponding terminal instruction.",
                proposedCommand = "echo '=== Machine Context ===' && uname -a && uptime",
                commandExplanation = "Prints basic operating system name, kernel version, and live host uptime stats.",
                safetyLevel = "SAFE",
                safetyReason = "Prints machine state."
            )
        }
    }
}
