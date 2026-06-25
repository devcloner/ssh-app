package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

@Composable
fun HostsScreen(
    viewModel: TermiAgentViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hosts by viewModel.hosts.collectAsState()
    val selectedHost by viewModel.selectedHost.collectAsState()

    val isScanningTailscale by viewModel.isScanningTailscale.collectAsState()
    val tailscaleLocalIp by viewModel.tailscaleLocalIp.collectAsState()
    val discoveredTailscaleNodes by viewModel.discoveredTailscaleNodes.collectAsState()
    val tailscaleScanMessage by viewModel.tailscaleScanMessage.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("termiagent_ssh_prefs", Context.MODE_PRIVATE) }
    val savedPrivateKey = remember(prefs) { prefs.getString("private_key", "") ?: "" }

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

            // Tailscale Quick Connect Component
            TailscaleQuickConnectComponent(
                viewModel = viewModel,
                isScanning = isScanningTailscale,
                localIp = tailscaleLocalIp,
                nodes = discoveredTailscaleNodes,
                scanMessage = tailscaleScanMessage,
                devicePrivateKey = savedPrivateKey
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                            onDelete = { viewModel.deleteHost(host) },
                            onConnect = { viewModel.connectHost(host.id) },
                            onDisconnect = { viewModel.disconnectHost(host.id) }
                        )
                    }
                }
            }

            // Device SSH Keypair Manager
            DeviceSshKeypairManagerComponent()

            Spacer(modifier = Modifier.height(6.dp))

            // Connection Helper and Setup Guide for Windows/WSL/Ubuntu
            ConnectionGuideComponent()
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
fun DeviceSshKeypairManagerComponent() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val prefs = remember { context.getSharedPreferences("termiagent_ssh_prefs", Context.MODE_PRIVATE) }
    
    var publicKey by remember { mutableStateOf(prefs.getString("public_key", "") ?: "") }
    var privateKey by remember { mutableStateOf(prefs.getString("private_key", "") ?: "") }
    var expanded by remember { mutableStateOf(false) }
    
    fun encodePublicKeyToOpenSSH(pubKey: RSAPublicKey): String {
        try {
            val byteStream = ByteArrayOutputStream()
            val dataStream = DataOutputStream(byteStream)
            
            val typeBytes = "ssh-rsa".toByteArray(Charsets.US_ASCII)
            dataStream.writeInt(typeBytes.size)
            dataStream.write(typeBytes)
            
            val exponentBytes = pubKey.publicExponent.toByteArray()
            dataStream.writeInt(exponentBytes.size)
            dataStream.write(exponentBytes)
            
            val modulusBytes = pubKey.modulus.toByteArray()
            dataStream.writeInt(modulusBytes.size)
            dataStream.write(modulusBytes)
            
            val keyBlob = Base64.getEncoder().encodeToString(byteStream.toByteArray())
            return "ssh-rsa $keyBlob termiagent@android"
        } catch (e: Exception) {
            return ""
        }
    }
    
    fun encodePrivateKeyToPEM(privKey: PrivateKey): String {
        try {
            val encoded = Base64.getMimeEncoder().encodeToString(privKey.encoded)
            return "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----"
        } catch (e: Exception) {
            return ""
        }
    }
    
    fun generateKeys() {
        try {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()
            
            val privPem = encodePrivateKeyToPEM(kp.private)
            val pubSsh = encodePublicKeyToOpenSSH(kp.public as RSAPublicKey)
            
            prefs.edit()
                .putString("private_key", privPem)
                .putString("public_key", pubSsh)
                .apply()
                
            publicKey = pubSsh
            privateKey = privPem
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Device SSH Keypair Manager",
                            color = SophisticatedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (publicKey.isEmpty()) "Passwordless auth is currently unconfigured" else "🟢 Secure SSH keypair is active & ready",
                            color = if (publicKey.isEmpty()) SophisticatedTextMuted else SophisticatedAccent,
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = SophisticatedTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (publicKey.isEmpty()) {
                    Text(
                        text = "Generate a secure RSA-2048 cryptographic keypair on this phone. You can append the public key on Windows/WSL to authenticate automatically with zero passwords.",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { generateKeys() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedAccent,
                            contentColor = SophisticatedOnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Secure Keypair", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Your device's secure public key (OpenSSH format):",
                        color = SophisticatedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = publicKey,
                            color = SophisticatedAccent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(10.dp)
                                .heightIn(max = 100.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                clipboardManager.setText(AnnotatedString(publicKey))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedSecondary,
                                contentColor = SophisticatedText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Public Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { generateKeys() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SophisticatedTextMuted
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("Regenerate", fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 HOW TO INSTALL THE KEY ON REMOTE HOST",
                                color = SophisticatedPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Copy the public key above.\n" +
                                        "2. On your host (Ubuntu/WSL/Linux), run:\n" +
                                        "   mkdir -p ~/.ssh && echo \"[paste_here]\" >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys\n" +
                                        "3. On Windows (Powershell), append it to:\n" +
                                        "   \$HOME\\.ssh\\authorized_keys",
                                color = SophisticatedTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionGuideComponent() {
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: WSL, 1: Windows, 2: Tailscale

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Connection Setup Guides",
                            color = SophisticatedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!expanded) {
                            Text(
                                text = "How to configure Windows, WSL (Ubuntu) or Linux for SSH & Hermes",
                                color = SophisticatedTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = SophisticatedTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Guide type selection tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("🐧 WSL Ubuntu", "🖥️ Windows Host", "🛡️ Tailscale & IP").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Button(
                            onClick = { selectedTab = index },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) SophisticatedSecondary else SophisticatedSurfaceVariant,
                                contentColor = if (isSelected) SophisticatedAccent else SophisticatedTextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> WslGuide()
                        1 -> WindowsGuide()
                        2 -> TailscaleGuide()
                    }
                }
            }
        }
    }
}

@Composable
fun WslGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Configure WSL 2 (Ubuntu/Debian) to accept incoming terminal sessions:", color = SophisticatedText, fontSize = 11.sp)
        
        GuideStep(
            number = "1",
            title = "Install SSH Server",
            command = "sudo apt update && sudo apt install -y openssh-server"
        )
        GuideStep(
            number = "2",
            title = "Allow Password Auth & Config Port",
            desc = "Edit /etc/ssh/sshd_config and ensure these options are set:",
            command = "Port 2222\nPasswordAuthentication yes"
        )
        GuideStep(
            number = "3",
            title = "Restart & Verify SSH Service",
            command = "sudo service ssh restart"
        )
        GuideStep(
            number = "4",
            title = "Find WSL IP",
            desc = "Locate your WSL internal IP to link with TermiAgent:",
            command = "hostname -I"
        )
    }
}

@Composable
fun WindowsGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Configure Windows Native OpenSSH daemon for secure shell control:", color = SophisticatedText, fontSize = 11.sp)
        
        GuideStep(
            number = "1",
            title = "Install OpenSSH Server Feature",
            desc = "Execute in Administrator PowerShell:",
            command = "Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0"
        )
        GuideStep(
            number = "2",
            title = "Start & Automate Service Startup",
            desc = "Configure the service to auto-start at boot:",
            command = "Start-Service sshd\nSet-Service -Name sshd -StartupType 'Automatic'"
        )
        GuideStep(
            number = "3",
            title = "Configure Inbound Firewall Access",
            desc = "Allow port 22 in Windows Defender Firewall:",
            command = "New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22"
        )
        GuideStep(
            number = "4",
            title = "Find Windows LAN IP",
            desc = "Run in Command Prompt or PowerShell:",
            command = "ipconfig"
        )
    }
}

@Composable
fun TailscaleGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Solve cellular and cross-network firewalls with ease:", color = SophisticatedText, fontSize = 11.sp)
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "⚠️ THE 'LOCALHOST' TRAP",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When running this app on your phone, do NOT use '127.0.0.1' or 'localhost'. That refers to the phone itself. You MUST use your computer's local Wi-Fi IP (e.g., 192.168.1.150) or its Tailscale IP address.",
                    color = SophisticatedTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "🌐 TAILSCALE MESH (RECOMMENDED)",
                    color = SophisticatedAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To connect securely when on cellular data or public subnets:\n" +
                            "1. Download & log into Tailscale on both phone and PC.\n" +
                            "2. Copy the 100.x.y.z Tailscale IP of your PC.\n" +
                            "3. Toggle 'Enable Tailscale Mesh VPN' in the app and paste the IP.",
                    color = SophisticatedTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun GuideStep(
    number: String,
    title: String,
    desc: String? = null,
    command: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .background(SophisticatedSecondary, RoundedCornerShape(9.dp))
                ) {
                    Text(number, color = SophisticatedAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = SophisticatedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (desc != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = SophisticatedTextMuted, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SophisticatedBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = command,
                    color = SophisticatedAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun HostItemCard(
    host: Host,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
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

            // Info row showing connection status & action toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Chip
                val statusColor = when (host.connectionStatus) {
                    "ACTIVE" -> SophisticatedAccent
                    "AUTHENTICATING" -> Color(0xFFF59E0B) // Amber
                    else -> Color(0xFFEF4444) // Red
                }
                
                val statusText = when (host.connectionStatus) {
                    "ACTIVE" -> "Active"
                    "AUTHENTICATING" -> "Connecting..."
                    else -> "Offline"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (host.connectionStatus == "AUTHENTICATING") {
                                CircularProgressIndicator(
                                    color = statusColor,
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(statusColor)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Connect / Disconnect interactive action
                    TextButton(
                        onClick = {
                            if (host.connectionStatus == "ACTIVE") {
                                onDisconnect()
                            } else if (host.connectionStatus == "OFFLINE") {
                                onConnect()
                            }
                        },
                        enabled = host.connectionStatus != "AUTHENTICATING",
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("host_connect_toggle_${host.id}")
                    ) {
                        Icon(
                            imageVector = if (host.connectionStatus == "ACTIVE") Icons.Default.PowerOff else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (host.connectionStatus == "ACTIVE") Color(0xFFEF4444) else SophisticatedAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (host.connectionStatus == "ACTIVE") "Disconnect" else "Connect Now",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (host.connectionStatus == "ACTIVE") Color(0xFFEF4444) else SophisticatedAccent
                        )
                    }
                }

                if (host.connectionStatus == "ACTIVE" && host.lastChecked > 0) {
                    Text(
                        text = "CPU:${host.cpuUsage}% RAM:${host.ramUsage}%",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHostDialog(
    onDismiss: () -> Unit,
    onAdd: (Host) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("termiagent_ssh_prefs", Context.MODE_PRIVATE) }
    val savedPrivateKey = remember(prefs) { prefs.getString("private_key", "") ?: "" }

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
                    visualTransformation = if (authType == "PASSWORD" && !secretVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = authType == "PASSWORD",
                    maxLines = if (authType == "PASSWORD") 1 else 8,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = if (authType == "SSH_KEY") FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (authType == "SSH_KEY") 11.sp else 14.sp
                    ),
                    trailingIcon = if (authType == "PASSWORD") {
                        {
                            IconButton(onClick = { secretVisible = !secretVisible }) {
                                Icon(
                                    imageVector = if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (secretVisible) "Hide secret" else "Show secret"
                                )
                            }
                        }
                    } else null,
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

                AnimatedVisibility(visible = authType == "SSH_KEY" && savedPrivateKey.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { secretValue = savedPrivateKey },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = SophisticatedAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Device Generated SSH Key", color = SophisticatedAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

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

@Composable
fun TailscaleQuickConnectComponent(
    viewModel: TermiAgentViewModel,
    isScanning: Boolean,
    localIp: String?,
    nodes: List<com.example.ui.viewmodel.TailscaleNode>,
    scanMessage: String,
    devicePrivateKey: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, SophisticatedAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SophisticatedAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Tailscale Mesh 'Quick Connect'",
                            color = SophisticatedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (localIp != null) "🟢 Tailscale Active ($localIp)" else "🔍 Tap to scan & auto-bridge nodes",
                            color = if (localIp != null) SophisticatedAccent else SophisticatedTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = SophisticatedTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Automatically discover active SSH-enabled servers on your Tailscale mesh network and configure the secure terminal bridge instantly.",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.scanTailscaleMesh() },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedAccent,
                            contentColor = SophisticatedOnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = SophisticatedOnPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Mesh...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Local Mesh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (scanMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanMessage,
                        color = SophisticatedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                }

                if (nodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Discovered Mesh Nodes:",
                        color = SophisticatedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        nodes.forEach { node ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = node.hostname,
                                                color = SophisticatedText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(SophisticatedSecondary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = node.osType,
                                                    color = SophisticatedPrimary,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "IP: ${node.ipAddress} • Port: ${node.port}",
                                            color = SophisticatedTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (node.isReachable) {
                                        Button(
                                            onClick = { viewModel.quickConnectTailscaleNode(node, devicePrivateKey) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SophisticatedSecondary,
                                                contentColor = SophisticatedAccent
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Connect", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "Not Reachable",
                                            color = Color.Red.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
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
