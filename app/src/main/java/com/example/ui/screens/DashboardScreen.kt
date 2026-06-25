package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Host
import com.example.ui.theme.*
import com.example.ui.viewmodel.TermiAgentViewModel

@Composable
fun DashboardScreen(
    viewModel: TermiAgentViewModel,
    onNavigateToChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hosts by viewModel.hosts.collectAsState()
    val selectedHost by viewModel.selectedHost.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()

    var showHostSelector by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header / Host quick switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                    .testTag("dashboard_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
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
                                    text = "CORE LINK AGENT",
                                    color = SophisticatedPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.clickable { showHostSelector = !showHostSelector },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedHost?.name ?: "No Host Configured",
                                        color = SophisticatedText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = if (showHostSelector) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Host",
                                        tint = SophisticatedText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Low Latency Ping Button
                        Button(
                            onClick = { viewModel.testConnection() },
                            enabled = selectedHost != null && !isPinging,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedAccent,
                                contentColor = SophisticatedOnPrimary,
                                disabledContainerColor = SophisticatedSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("ping_host_button")
                        ) {
                            if (isPinging) {
                                CircularProgressIndicator(
                                    color = SophisticatedOnPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pinging...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Ping",
                                    tint = SophisticatedOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Host Switcher Dropdown list
                    AnimatedVisibility(visible = showHostSelector) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (hosts.isEmpty()) {
                                Text(
                                    text = "No other hosts found. Go to Nodes tab to add.",
                                    color = SophisticatedTextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            } else {
                                hosts.forEach { host ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (host.id == selectedHost?.id) SophisticatedSurfaceVariant else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectHost(host.id)
                                                showHostSelector = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Dns,
                                                contentDescription = "Server",
                                                tint = when (host.connectionStatus) {
                                                    "ACTIVE" -> SophisticatedAccent
                                                    "AUTHENTICATING" -> Color(0xFFF59E0B)
                                                    else -> SophisticatedTextMuted
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(host.name, color = SophisticatedText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(host.ipOrHostname, color = SophisticatedTextMuted, fontSize = 12.sp)
                                            }
                                        }
                                        val dotColor = when (host.connectionStatus) {
                                            "ACTIVE" -> SophisticatedAccent
                                            "AUTHENTICATING" -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            if (host.connectionStatus == "AUTHENTICATING") {
                                                CircularProgressIndicator(
                                                    color = dotColor,
                                                    modifier = Modifier.size(10.dp),
                                                    strokeWidth = 1.5.dp
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(dotColor)
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

        // Active node metrics
        selectedHost?.let { host ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "CPU UTILITY",
                        value = host.cpuUsage,
                        color = when {
                            host.cpuUsage < 50 -> SophisticatedAccent
                            host.cpuUsage < 80 -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "MEMORY ALLOC",
                        value = host.ramUsage,
                        color = when {
                            host.ramUsage < 60 -> SophisticatedAccent
                            host.ramUsage < 85 -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "DISK BLOCKS",
                        value = host.diskUsage,
                        color = when {
                            host.diskUsage < 70 -> SophisticatedAccent
                            host.diskUsage < 90 -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Connection health and Tailscale stats
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(28.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SECURE NETWORK & CRYPTO METRICS",
                            color = SophisticatedPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Tailscale section
                        NetworkDetailRow(
                            icon = Icons.Outlined.Lan,
                            title = "Tailscale Mesh Connection",
                            value = if (host.useTailscale) "ACTIVE" else "OFFLINE / BYPASSED",
                            valueColor = if (host.useTailscale) SophisticatedAccent else SophisticatedTextMuted
                        )
                        if (host.useTailscale && host.tailscaleIp.isNotEmpty()) {
                            NetworkDetailRow(
                                icon = Icons.Default.Info,
                                title = "Tailscale Mesh IP Address",
                                value = host.tailscaleIp,
                                valueColor = SophisticatedText
                            )
                        }

                        HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                        // SSH / Encryption Section
                        NetworkDetailRow(
                            icon = Icons.Outlined.Shield,
                            title = "Encryption Standard",
                            value = if (host.authType == "SSH_KEY") "RSA-ED25519 (Asymmetric)" else "SSH SHA-256 Symmetric",
                            valueColor = SophisticatedPrimary
                        )
                        NetworkDetailRow(
                            icon = Icons.Default.Lock,
                            title = "Active Agent Shell Gate",
                            value = "SAFE AUDIT / SECURE SSHD",
                            valueColor = SophisticatedAccent
                        )
                        NetworkDetailRow(
                            icon = Icons.Default.Sync,
                            title = "Last Tunnel Handshake",
                            value = if (host.lastChecked > 0) {
                                val elapsedSec = (System.currentTimeMillis() - host.lastChecked) / 1000
                                if (elapsedSec < 60) "Just now" else "${elapsedSec / 60}m ago"
                            } else "Never checked",
                            valueColor = SophisticatedTextMuted
                        )
                    }
                }
            }

            // Interactive agent launcher banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSecondary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                        .clickable { onNavigateToChat() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask CoreLink Agent Pro",
                                color = SophisticatedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Translate instructions to safe commands. Double check security logs and perform quick low-latency executions.",
                                color = SophisticatedTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Chat",
                            tint = SophisticatedPrimary,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        } ?: item {
            // Empty state for host
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(SophisticatedSecondary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = "No node",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Connection Node Configured",
                        color = SophisticatedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Navigate to the 'SSH Nodes' tab to link your terminal or server.",
                        color = SophisticatedTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = SophisticatedTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Gauge/Arc drawing
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                val animatedValue by animateFloatAsState(
                    targetValue = value.toFloat(),
                    animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                )

                Canvas(modifier = Modifier.size(54.dp)) {
                    // Gray background track
                    drawArc(
                        color = SophisticatedSurfaceVariant,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Animated active progress arc
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = (animatedValue / 100f) * 270f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${value}%",
                    color = SophisticatedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun NetworkDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SophisticatedTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = SophisticatedTextMuted,
                fontSize = 13.sp
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
