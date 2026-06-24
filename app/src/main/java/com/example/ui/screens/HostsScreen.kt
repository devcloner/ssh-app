package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Host
import com.example.ui.theme.*
import com.example.ui.viewmodel.TermiAgentViewModel

@Composable
fun HostsScreen(
    viewModel: TermiAgentViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hosts by viewModel.hosts.collectAsState()
    val selectedHost by viewModel.selectedHost.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
                        Text(
                            text = "SSH & Mesh Nodes",
                            color = SophisticatedText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Manage server terminals and SSH hosts securely",
                            color = SophisticatedTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // Add Node FAB-styled button
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedAccent,
                        contentColor = SophisticatedOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_host_trigger")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Node", tint = SophisticatedOnPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hosts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 48.dp)
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(32.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(SophisticatedSecondary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Computer,
                                contentDescription = null,
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Connection Nodes Linked",
                            color = SophisticatedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click 'Add Node' to link a host machine using SSH or Tailscale configuration.",
                            color = SophisticatedTextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(hosts, key = { it.id }) { host ->
                        HostItemCard(
                            host = host,
                            isSelected = host.id == selectedHost?.id,
                            onSelect = { viewModel.selectHost(host.id) },
                            onDelete = { viewModel.deleteHost(host) }
                        )
                    }
                }
            }
        }

        // Add Host Dialog
        if (showAddDialog) {
            AddHostDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newHost ->
                    viewModel.addHost(newHost)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun HostItemCard(
    host: Host,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SophisticatedSurfaceVariant else SophisticatedSurface
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) SophisticatedAccent else SophisticatedBorder,
                RoundedCornerShape(24.dp)
            )
            .clickable { onSelect() }
            .testTag("host_item_card_${host.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = if (host.isActive) SophisticatedAccent else SophisticatedTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = host.name,
                                color = SophisticatedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (host.useTailscale) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SophisticatedSecondary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Tailscale",
                                        color = SophisticatedPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (host.modelAgentType == "GEMINI") SophisticatedPrimary.copy(alpha = 0.15f)
                                        else Color(0xFF6366F1).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (host.modelAgentType) {
                                        "GEMINI" -> "Gemini"
                                        "HERMES_API" -> "Hermes API"
                                        else -> "Hermes SSH"
                                    },
                                    color = if (host.modelAgentType == "GEMINI") SophisticatedPrimary else Color(0xFF6366F1),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "${host.username}@${host.ipOrHostname}:${host.port}",
                            color = SophisticatedTextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_host_button_${host.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Node",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info row showing ping latency and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (host.isActive) SophisticatedAccent else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (host.isActive) "SSH Session: Active" else "Session: Offline",
                        color = if (host.isActive) SophisticatedAccent else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (host.isActive && host.lastChecked > 0) {
                    Text(
                        text = "CPU: ${host.cpuUsage}% | RAM: ${host.ramUsage}%",
                        color = SophisticatedTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHostDialog(
    onDismiss: () -> Unit,
    onAdd: (Host) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ipOrHostname by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf("PASSWORD") } // PASSWORD or SSH_KEY
    var secretValue by remember { mutableStateOf("") }
    var useTailscale by remember { mutableStateOf(false) }
    var tailscaleIp by remember { mutableStateOf("") }

    var secretVisible by remember { mutableStateOf(false) }

    var modelAgentType by remember { mutableStateOf("GEMINI") } // GEMINI, HERMES_API, HERMES_SSH
    var hermesApiUrl by remember { mutableStateOf("https://api.openrouter.ai/api/v1") }
    var hermesApiKey by remember { mutableStateOf("") }
    var hermesModelName by remember { mutableStateOf("nousresearch/hermes-2-pro-llama-3-8b") }
    var hermesApiKeyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Link Connection Node",
                color = SophisticatedText,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        containerColor = SophisticatedSurface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Node Name (e.g. Production VM)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophisticatedText,
                        unfocusedTextColor = SophisticatedText,
                        focusedBorderColor = SophisticatedPrimary,
                        unfocusedBorderColor = SophisticatedBorder,
                        focusedLabelColor = SophisticatedPrimary,
                        unfocusedLabelColor = SophisticatedTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_host_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ipOrHostname,
                        onValueChange = { ipOrHostname = it },
                        label = { Text("IP / Hostname") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SophisticatedText,
                            unfocusedTextColor = SophisticatedText,
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedLabelColor = SophisticatedPrimary,
                            unfocusedLabelColor = SophisticatedTextMuted
                        ),
                        modifier = Modifier.weight(2f).testTag("input_host_ip")
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SophisticatedText,
                            unfocusedTextColor = SophisticatedText,
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedLabelColor = SophisticatedPrimary,
                            unfocusedLabelColor = SophisticatedTextMuted
                        ),
                        modifier = Modifier.weight(1f).testTag("input_host_port")
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("SSH Username") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophisticatedText,
                        unfocusedTextColor = SophisticatedText,
                        focusedBorderColor = SophisticatedPrimary,
                        unfocusedBorderColor = SophisticatedBorder,
                        focusedLabelColor = SophisticatedPrimary,
                        unfocusedLabelColor = SophisticatedTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_host_username")
                )

                // Auth type picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = authType == "PASSWORD",
                        onClick = { authType = "PASSWORD" },
                        label = { Text("Password Auth") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SophisticatedSecondary,
                            selectedLabelColor = SophisticatedText,
                            containerColor = SophisticatedSurface,
                            labelColor = SophisticatedTextMuted
                        )
                    )
                    FilterChip(
                        selected = authType == "SSH_KEY",
                        onClick = { authType = "SSH_KEY" },
                        label = { Text("SSH Private Key") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SophisticatedSecondary,
                            selectedLabelColor = SophisticatedText,
                            containerColor = SophisticatedSurface,
                            labelColor = SophisticatedTextMuted
                        )
                    )
                }

                // Password / Secret key field
                OutlinedTextField(
                    value = secretValue,
                    onValueChange = { secretValue = it },
                    label = { Text(if (authType == "PASSWORD") "SSH Password" else "Private SSH Key (PEM)") },
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { secretVisible = !secretVisible }) {
                            Icon(
                                imageVector = if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (secretVisible) "Hide secret" else "Show secret"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophisticatedText,
                        unfocusedTextColor = SophisticatedText,
                        focusedBorderColor = SophisticatedPrimary,
                        unfocusedBorderColor = SophisticatedBorder,
                        focusedLabelColor = SophisticatedPrimary,
                        unfocusedLabelColor = SophisticatedTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_host_secret")
                )

                // Tailscale support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Tailscale Mesh VPN", color = SophisticatedText, fontSize = 14.sp)
                    Switch(
                        checked = useTailscale,
                        onCheckedChange = { useTailscale = it },
                        modifier = Modifier.testTag("toggle_tailscale"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SophisticatedAccent,
                            checkedTrackColor = SophisticatedSecondary,
                            uncheckedThumbColor = SophisticatedTextMuted,
                            uncheckedTrackColor = SophisticatedSurfaceVariant
                        )
                    )
                }

                AnimatedVisibility(visible = useTailscale) {
                    OutlinedTextField(
                        value = tailscaleIp,
                        onValueChange = { tailscaleIp = it },
                        label = { Text("Tailscale Node IP (e.g. 100.x.y.z)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SophisticatedText,
                            unfocusedTextColor = SophisticatedText,
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedLabelColor = SophisticatedPrimary,
                            unfocusedLabelColor = SophisticatedTextMuted
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_tailscale_ip")
                    )
                }

                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)

                // Model selection
                Text("Select Main Model Agent", color = SophisticatedText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = modelAgentType == "GEMINI",
                            onClick = { modelAgentType = "GEMINI" },
                            label = { Text("Gemini Core", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedAccent,
                                selectedLabelColor = SophisticatedOnPrimary,
                                containerColor = SophisticatedSurface,
                                labelColor = SophisticatedTextMuted
                            )
                        )
                        FilterChip(
                            selected = modelAgentType == "HERMES_API",
                            onClick = { modelAgentType = "HERMES_API" },
                            label = { Text("Nous Hermes API", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.3f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedAccent,
                                selectedLabelColor = SophisticatedOnPrimary,
                                containerColor = SophisticatedSurface,
                                labelColor = SophisticatedTextMuted
                            )
                        )
                        FilterChip(
                            selected = modelAgentType == "HERMES_SSH",
                            onClick = { modelAgentType = "HERMES_SSH" },
                            label = { Text("Hermes Local SSH", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.3f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedAccent,
                                selectedLabelColor = SophisticatedOnPrimary,
                                containerColor = SophisticatedSurface,
                                labelColor = SophisticatedTextMuted
                            )
                        )
                    }
                    Text(
                        text = when (modelAgentType) {
                            "GEMINI" -> "Uses Gemini 1.5/2.5 models as the main natural language orchestration agent."
                            "HERMES_API" -> "Uses external OpenRouter/Ollama/LM Studio API connection for Nous Hermes."
                            else -> "Runs the Hermes agent CLI directly as a native host daemon over SSH."
                        },
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp
                    )
                }

                AnimatedVisibility(visible = modelAgentType == "HERMES_API") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hermesApiUrl,
                            onValueChange = { hermesApiUrl = it },
                            label = { Text("Hermes API Endpoint URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SophisticatedText,
                                unfocusedTextColor = SophisticatedText,
                                focusedBorderColor = SophisticatedPrimary,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedLabelColor = SophisticatedPrimary,
                                unfocusedLabelColor = SophisticatedTextMuted
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_hermes_url")
                        )
                        OutlinedTextField(
                            value = hermesApiKey,
                            onValueChange = { hermesApiKey = it },
                            label = { Text("Hermes API Key (Optional)") },
                            visualTransformation = if (hermesApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { hermesApiKeyVisible = !hermesApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (hermesApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SophisticatedText,
                                unfocusedTextColor = SophisticatedText,
                                focusedBorderColor = SophisticatedPrimary,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedLabelColor = SophisticatedPrimary,
                                unfocusedLabelColor = SophisticatedTextMuted
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_hermes_key")
                        )
                        OutlinedTextField(
                            value = hermesModelName,
                            onValueChange = { hermesModelName = it },
                            label = { Text("Model Identifier") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SophisticatedText,
                                unfocusedTextColor = SophisticatedText,
                                focusedBorderColor = SophisticatedPrimary,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedLabelColor = SophisticatedPrimary,
                                unfocusedLabelColor = SophisticatedTextMuted
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_hermes_model")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ipOrHostname.isNotBlank() && username.isNotBlank()) {
                        val parsedPort = port.toIntOrNull() ?: 22
                        onAdd(
                            Host(
                                name = name,
                                ipOrHostname = ipOrHostname,
                                port = parsedPort,
                                username = username,
                                authType = authType,
                                secretValue = secretValue,
                                useTailscale = useTailscale,
                                tailscaleIp = if (useTailscale) tailscaleIp else "",
                                isActive = false,
                                modelAgentType = modelAgentType,
                                hermesApiUrl = hermesApiUrl,
                                hermesApiKey = hermesApiKey,
                                hermesModelName = hermesModelName
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && ipOrHostname.isNotBlank() && username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedAccent,
                    contentColor = SophisticatedOnPrimary
                ),
                modifier = Modifier.testTag("submit_add_host")
            ) {
                Text("Confirm Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SophisticatedTextMuted)
            }
        }
    )
}
