package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
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

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AgentResponse(
        val messageText: String,
        val proposedCommand: String? = null,
        val commandExplanation: String? = null,
        val safetyLevel: String? = "SAFE",
        val safetyReason: String? = null
    )

    suspend fun queryAgent(
        userPrompt: String,
        hostContext: Host,
        useHighThinking: Boolean,
        chatHistory: List<Pair<String, String>> = emptyList() // sender to message text
    ): AgentResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AgentResponse(
                messageText = "⚠️ Gemini API Key is not configured. Please add your key in the Secrets Panel in AI Studio.",
                safetyLevel = "WARN",
                safetyReason = "API Key Missing"
            )
        }

        // Determine the model
        val modelName = if (useHighThinking) "gemini-3.1-pro-preview" else "gemini-3.1-flash-lite-preview"
        val url = "$BASE_URL$modelName:generateContent?key=$apiKey"

        val systemInstruction = """
            You are TermiAgent, an advanced natural-language-to-terminal interface. Your purpose is to act as a secure agent helping administrators manage host systems through safe command proposals.
            
            Current Active Host Configuration:
            - Name: ${hostContext.name}
            - IP/Host: ${hostContext.ipOrHostname}
            - Port: ${hostContext.port}
            - Username: ${hostContext.username}
            - Auth Method: ${hostContext.authType}
            - Tailscale Network Enabled: ${hostContext.useTailscale}
            - Tailscale IP: ${if (hostContext.useTailscale) hostContext.tailscaleIp else "N/A"}
            
            Translate the user's instructions into correct, safe, and modern shell commands (primarily Linux Bash compatible).
            
            YOU MUST RESPOND ONLY IN VALID JSON matching this schema:
            {
              "messageText": "A friendly conversational explanation of what is going on, and recommendations.",
              "proposedCommand": "The actual shell command or script, or null/empty if the request doesn't warrant executing a command (e.g. general questions).",
              "commandExplanation": "Line-by-line or conceptual explanation of what the command does.",
              "safetyLevel": "SAFE" or "WARN" or "DANGEROUS",
              "safetyReason": "An analysis of the risks. E.g. 'This is read-only', 'This restarts nginx causing brief downtime', or 'This removes files and is destructive!'"
            }
            
            Classification rules:
            - SAFE: Only queries state, reads files, lists processes, displays memory (e.g., cat, grep, ls, df, free, top, systemctl status, docker ps).
            - WARN: Modifies system packages, edits non-critical files, restarts web or application services (e.g., apt update, systemctl restart nginx, docker-compose build).
            - DANGEROUS: Deletes files, modifies critical routing, updates core system configs, kills processes, restarts/reboots the host (e.g., rm, mkfs, dd, kill, reboot, shutdown, modifying /etc/fstab).
            
            Always output clean, raw JSON. Do not wrap the JSON in Markdown code blocks like ```json ... ```. Just return the raw JSON string.
        """.trimIndent()

        try {
            val jsonBody = JSONObject()

            // Contents array
            val contentsArray = JSONArray()

            // Include system instruction as systemInstruction parameter (gemini-2.5+ / v1beta style)
            val systemInstructionObj = JSONObject()
            systemInstructionObj.put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            jsonBody.put("systemInstruction", systemInstructionObj)

            // History & prompt
            chatHistory.forEach { (sender, text) ->
                val role = if (sender == "USER") "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                contentObj.put("parts", JSONArray().put(JSONObject().put("text", text)))
                contentsArray.put(contentObj)
            }

            // Current user message
            val currentMsg = JSONObject()
            currentMsg.put("role", "user")
            currentMsg.put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            contentsArray.put(currentMsg)

            jsonBody.put("contents", contentsArray)

            // Config
            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            generationConfig.put("temperature", 0.1)

            // If using high thinking (gemini-3.1-pro-preview), apply high thinking level
            if (useHighThinking) {
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "high")
                generationConfig.put("thinkingConfig", thinkingConfig)
            }

            jsonBody.put("generationConfig", generationConfig)

            val requestBodyStr = jsonBody.toString()
            Log.d(TAG, "Request payload ($modelName): $requestBodyStr")

            val requestBody = requestBodyStr.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Response payload ($modelName): $bodyStr")

                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    Log.e(TAG, errMsg)
                    return@withContext AgentResponse(
                        messageText = "Failed to connect to Gemini API. $errMsg\nResponse: $bodyStr",
                        safetyLevel = "WARN",
                        safetyReason = "Connection Failure"
                    )
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext AgentResponse(
                        messageText = "No response candidates returned by Gemini model.",
                        safetyLevel = "WARN",
                        safetyReason = "Empty Model Result"
                    )
                }

                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val rawText = parts.getJSONObject(0).getString("text").trim()

                // Parse the JSON returned by the agent
                try {
                    val parsedResponse = JSONObject(rawText)
                    return@withContext AgentResponse(
                        messageText = parsedResponse.optString("messageText", "No conversational message provided."),
                        proposedCommand = parsedResponse.optString("proposedCommand", "").takeIf { it.isNotBlank() },
                        commandExplanation = parsedResponse.optString("commandExplanation", "").takeIf { it.isNotBlank() },
                        safetyLevel = parsedResponse.optString("safetyLevel", "SAFE"),
                        safetyReason = parsedResponse.optString("safetyReason", "")
                    )
                } catch (pe: Exception) {
                    Log.e(TAG, "Failed to parse model's inner JSON structure. Raw string was: $rawText", pe)
                    // Fallback to presenting raw text if model failed to follow schema perfectly
                    return@withContext AgentResponse(
                        messageText = rawText,
                        safetyLevel = "WARN",
                        safetyReason = "Model Response Format Error"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during queryAgent call", e)
            return@withContext AgentResponse(
                messageText = "An error occurred while connecting to the Gemini engine: ${e.localizedMessage}",
                safetyLevel = "WARN",
                safetyReason = "Exception Triggered"
            )
        }
    }

    suspend fun optimizePrompt(
        prompt: String,
        hostContext: Host?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext prompt
        }

        val url = "${BASE_URL}gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
        
        val systemInstruction = """
            You are an expert prompt optimizer for TermiAgent, an AI-powered terminal assistant.
            The user wants to write a terminal instruction or action. Your task is to refine and optimize their brief, draft, or informal query into a precise, professional, and clear system administration instruction.
            
            Host context:
            - Name: ${hostContext?.name ?: "N/A"}
            - IP/Host: ${hostContext?.ipOrHostname ?: "N/A"}
            - Username: ${hostContext?.username ?: "N/A"}
            - Auth Type: ${hostContext?.authType ?: "N/A"}
            - Tailscale Enabled: ${hostContext?.useTailscale ?: "false"}
            
            Rules:
            1. Respond ONLY with the optimized instruction text itself.
            2. Keep it concise, clear, and professional.
            3. Do NOT include any introductory chatter, explanations, quotes, or markdown format code blocks. Just return the raw optimized query.
            4. Make it highly specific for a terminal assistant context.
            
            Examples:
            - Input: "clear docker cache" -> "Clean up dangling Docker images, stopped containers, and unused volumes safely to optimize disk space."
            - Input: "is nginx running" -> "Diagnose the Nginx service status, inspect configuration syntax correctness, and list active ports."
            - Input: "check heavy files in logs" -> "Identify the top 5 largest log files in the syslog directories and display their disk utilization."
        """.trimIndent()

        try {
            val jsonBody = JSONObject()
            val contentsArray = JSONArray()

            val systemInstructionObj = JSONObject()
            systemInstructionObj.put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            jsonBody.put("systemInstruction", systemInstructionObj)

            val currentMsg = JSONObject()
            currentMsg.put("role", "user")
            currentMsg.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            contentsArray.put(currentMsg)

            jsonBody.put("contents", contentsArray)

            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            jsonBody.put("generationConfig", generationConfig)

            val requestBodyStr = jsonBody.toString()
            val requestBody = requestBodyStr.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext prompt
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext prompt
                }

                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val optimizedText = parts.getJSONObject(0).getString("text").trim()
                return@withContext optimizedText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during optimizePrompt call", e)
            return@withContext prompt
        }
    }
}
