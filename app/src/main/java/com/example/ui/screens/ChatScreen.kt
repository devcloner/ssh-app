package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.ui.viewmodel.TermiAgentViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: TermiAgentViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedHost by viewModel.selectedHost.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentText by viewModel.currentMessageText.collectAsState()
    val isQuerying by viewModel.isQueryingAgent.collectAsState()
    val useHighThinking by viewModel.useHighThinking.collectAsState()
    val isOptimizing by viewModel.isOptimizingPrompt.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showHistoryPanel by remember { mutableStateOf(false) }

    val commandHistory = remember(messages) {
        messages.filter { it.sender == "USER" && !it.messageText.startsWith("Preset Run:") }
            .map { it.messageText }
            .distinct()
            .reversed()
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Chat Header with Host connection status and thinking config
            ChatHeader(
                selectedHostName = selectedHost?.name,
                selectedHostUser = selectedHost?.username,
                selectedHostIp = selectedHost?.ipOrHostname,
                selectedHostModel = selectedHost?.modelAgentType ?: "GEMINI",
                useHighThinking = useHighThinking,
                onHighThinkingToggle = { viewModel.setUseHighThinking(it) },
                onClearChat = { selectedHost?.id?.let { viewModel.clearChat(it) } },
                onOpenDrawer = onOpenDrawer,
                onToggleHistory = { showHistoryPanel = !showHistoryPanel },
                onModelTypeChange = { viewModel.updateSelectedHostModelAgentType(it) },
                historyCount = commandHistory.size
            )

            // Main Chat List or Empty State
            if (selectedHost == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select or Link a Node first to query AI agents.",
                        color = SophisticatedTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (messages.isEmpty()) {
                EmptyChatPlaceholder(
                    hostName = selectedHost?.name ?: "",
                    onQuickPrompt = { prompt ->
                        viewModel.updateMessageText(prompt)
                        viewModel.sendMessage()
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        if (message.sender == "USER") {
                            UserBubble(message)
                        } else {
                            AgentResponseCard(
                                message = message,
                                onExecute = { viewModel.runShellCommand(message) }
                            )
                        }
                    }

                    if (isQuerying) {
                        item {
                            AgentThinkingPlaceholder(useHighThinking = useHighThinking)
                        }
                    }
                }
            }

            // Bottom Input Row
            if (selectedHost != null) {
                CommandTemplatesRow(
                    onSelectTemplate = { template ->
                        viewModel.updateMessageText(template.promptDraft)
                    },
                    onInjectAndOptimize = { template ->
                        viewModel.updateMessageText(template.promptDraft)
                        viewModel.optimizeCurrentPrompt()
                    }
                )

                ChatInputBar(
                    text = currentText,
                    onTextChange = { viewModel.updateMessageText(it) },
                    onSend = { viewModel.sendMessage() },
                    enabled = !isQuerying,
                    isOptimizing = isOptimizing,
                    onOptimize = { viewModel.optimizeCurrentPrompt() }
                )
            }
        }

        AnimatedVisibility(
            visible = showHistoryPanel,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            HistoryPanelContent(
                history = commandHistory,
                onSelect = { instruction ->
                    viewModel.updateMessageText(instruction)
                },
                onReRun = { instruction ->
                    viewModel.updateMessageText(instruction)
                    viewModel.sendMessage()
                    showHistoryPanel = false
                },
                onClose = { showHistoryPanel = false }
            )
        }
    }
}

@Composable
fun ChatHeader(
    selectedHostName: String?,
    selectedHostUser: String?,
    selectedHostIp: String?,
    selectedHostModel: String,
    useHighThinking: Boolean,
    onHighThinkingToggle: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleHistory: () -> Unit,
    onModelTypeChange: (String) -> Unit,
    historyCount: Int
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Hosts Drawer",
                            tint = SophisticatedPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedHostName ?: "No Host Selected",
                                color = SophisticatedText,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            if (selectedHostName != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box {
                                    IconButton(
                                        onClick = { modelMenuExpanded = true },
                                        modifier = Modifier.size(24.dp).testTag("model_agent_dropdown_trigger")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Agent",
                                            tint = SophisticatedTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = modelMenuExpanded,
                                        onDismissRequest = { modelMenuExpanded = false },
                                        modifier = Modifier.background(SophisticatedSurface).border(1.dp, SophisticatedBorder)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Gemini Core", color = SophisticatedText, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SophisticatedPrimary, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                onModelTypeChange("GEMINI")
                                                modelMenuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Nous Hermes API", color = SophisticatedText, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                onModelTypeChange("HERMES_API")
                                                modelMenuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hermes Local SSH", color = SophisticatedText, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                onModelTypeChange("HERMES_SSH")
                                                modelMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (selectedHostIp != null) {
                            Text(
                                text = "Secure Gate: $selectedHostUser@$selectedHostIp",
                                color = SophisticatedAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedHostName != null) {
                        // History Toggle Button
                        IconButton(
                            onClick = onToggleHistory,
                            modifier = Modifier.testTag("toggle_history_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (historyCount > 0) {
                                        Badge(
                                            containerColor = SophisticatedAccent,
                                            contentColor = SophisticatedOnPrimary
                                        ) {
                                            Text(historyCount.toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Toggle History",
                                    tint = SophisticatedPrimary
                                )
                            }
                        }
                    }

                    // Clear history button
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = SophisticatedTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Thinking Selector / Active Agent Banner
            if (selectedHostModel == "GEMINI") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (useHighThinking) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                            contentDescription = "Speed mode",
                            tint = if (useHighThinking) SophisticatedAccent else SophisticatedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useHighThinking) "Deep Reasoning (Pro)" else "Direct AI Flash (Lite)",
                            color = if (useHighThinking) SophisticatedAccent else SophisticatedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Enable Reasoning Mode",
                            color = SophisticatedTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useHighThinking,
                            onCheckedChange = onHighThinkingToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SophisticatedAccent,
                                checkedTrackColor = SophisticatedSecondary,
                                uncheckedThumbColor = SophisticatedTextMuted,
                                uncheckedTrackColor = SophisticatedSurfaceVariant
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("toggle_reasoning_mode")
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Hermes Active",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedHostModel == "HERMES_API") "Nous Hermes Agent (API Tunnel)" else "Nous Hermes SSH Agent Daemon",
                            color = Color(0xFF818CF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Tool-use Engaged",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(
    hostName: String,
    onQuickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = SophisticatedPrimary,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI-Driven Host Assistant",
            color = SophisticatedText,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Text(
            text = "Connected to Node: $hostName",
            color = SophisticatedAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Describe what you'd like to perform. The agent will formulate the required bash command, explain its consequences, and run it safely with low-latency terminal outputs.",
            color = SophisticatedTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SUGGESTED CHECKS",
            color = SophisticatedBorder,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        val prompts = listOf(
            "Show current disk blocks and search for files larger than 100MB",
            "Are there any running Docker containers? Check their CPU / memory weights",
            "Verify which internet ports are currently listening for traffic",
            "List the heaviest 10 processes running right now"
        )

        prompts.forEach { prompt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onQuickPrompt(prompt) }
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = SophisticatedAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        color = SophisticatedTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun UserBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SophisticatedSecondary),
            shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
            modifier = Modifier.border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
        ) {
            Text(
                text = message.messageText,
                color = SophisticatedText,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun AgentResponseCard(
    message: ChatMessage,
    onExecute: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
            .testTag("agent_response_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Conversational response
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.SmartButton,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.messageText,
                    color = SophisticatedText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // If a terminal command was proposed, show the action card!
            if (!message.proposedCommand.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Proposed command block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROPOSED COMMAND",
                        color = SophisticatedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.proposedCommand)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Command",
                            tint = SophisticatedTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linux code block container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedViewportBg)
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.proposedCommand,
                        color = SophisticatedAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Command Explanation
                if (!message.commandExplanation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = SophisticatedTextMuted,
                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.commandExplanation,
                            color = SophisticatedTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Safety Assessment Level indicator
                val safetyColor = when (message.safetyLevel) {
                    "SAFE" -> SophisticatedAccent
                    "WARN" -> Color(0xFFFBBF24)
                    "DANGEROUS" -> Color(0xFFEF4444)
                    else -> SophisticatedTextMuted
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(safetyColor.copy(alpha = 0.1f))
                        .border(1.dp, safetyColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Safety Status",
                        tint = safetyColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SECURITY RANK: ${message.safetyLevel ?: "UNAUDITED"}",
                            color = safetyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        if (!message.safetyReason.isNullOrBlank()) {
                            Text(
                                text = message.safetyReason,
                                color = SophisticatedTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SSH Trigger Button or Execution Active Console Output container
                if (message.isExecuting) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = SophisticatedText, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Streaming Terminal Tunnel Logs...", color = SophisticatedText, fontSize = 13.sp)
                    }
                } else if (!message.isExecuted) {
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (message.safetyLevel == "DANGEROUS") Color(0xFFEF4444) else SophisticatedAccent,
                            contentColor = if (message.safetyLevel == "DANGEROUS") Color.White else SophisticatedOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_command_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Execute")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (message.safetyLevel == "DANGEROUS") "I understand risk, Execute Command" else "Execute Command Safely",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Success badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = SophisticatedAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Executed securely via SSH session tunnel.", color = SophisticatedAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Terminal console output stream drawer
                if (!message.commandOutput.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SophisticatedViewportBg)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TERMINAL STDOUT / STDERR",
                                color = SophisticatedTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            if (message.isExecuting) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SophisticatedAccent)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message.commandOutput + if (message.isExecuting) " █" else "",
                            color = SophisticatedAccent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentThinkingPlaceholder(useHighThinking: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = if (useHighThinking) SophisticatedAccent else SophisticatedPrimary,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (useHighThinking) "Agent is analyzing system configuration and reasoning safety models..." else "Agent is formulating shell parameters...",
                    color = SophisticatedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (useHighThinking) "Using gemini-3.1-pro-preview with Deep Reasoning" else "Using gemini-3.1-flash-lite-preview low-latency API",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    isOptimizing: Boolean,
    onOptimize: () -> Unit
) {
    Surface(
        color = SophisticatedSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder)
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Instruct the SSH Agent (e.g. Check CPU stress logs)", color = SophisticatedTextMuted) },
                enabled = enabled && !isOptimizing,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SophisticatedText,
                    unfocusedTextColor = SophisticatedText,
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedBorder,
                    focusedContainerColor = SophisticatedViewportBg,
                    unfocusedContainerColor = SophisticatedViewportBg
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                leadingIcon = {
                    IconButton(
                        onClick = onOptimize,
                        enabled = enabled && text.isNotBlank() && !isOptimizing,
                        modifier = Modifier.testTag("optimize_prompt_button")
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(
                                color = SophisticatedAccent,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Optimize Prompt",
                                tint = if (enabled && text.isNotBlank()) SophisticatedPrimary else SophisticatedTextMuted
                            )
                        }
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = onSend,
                        enabled = enabled && text.isNotBlank() && !isOptimizing,
                        modifier = Modifier.testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (enabled && text.isNotBlank() && !isOptimizing) SophisticatedAccent else SophisticatedTextMuted
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPanelContent(
    history: List<String>,
    onSelect: (String) -> Unit,
    onReRun: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .testTag("persistent_command_history_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SophisticatedAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "History Tunnel",
                        color = SophisticatedText,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Panel",
                        tint = SophisticatedTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Re-run previous natural language queries sent to this SSH host.",
                color = SophisticatedTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search instructions...", fontSize = 12.sp, color = SophisticatedTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SophisticatedText,
                    unfocusedTextColor = SophisticatedText,
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedBorder,
                    focusedContainerColor = SophisticatedViewportBg,
                    unfocusedContainerColor = SophisticatedViewportBg
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("history_search_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SophisticatedTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No command history." else "No matches found.",
                        color = SophisticatedTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHistory) { instruction ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onSelect(instruction) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = instruction,
                                    color = SophisticatedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 3,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Load Button
                                    TextButton(
                                        onClick = { onSelect(instruction) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = SophisticatedPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 10.sp, color = SophisticatedPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Quick Run Button
                                    Button(
                                        onClick = { onReRun(instruction) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SophisticatedAccent.copy(alpha = 0.15f),
                                            contentColor = SophisticatedAccent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Run",
                                            tint = SophisticatedAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
