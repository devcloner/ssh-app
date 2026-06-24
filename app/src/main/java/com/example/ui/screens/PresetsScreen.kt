package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PresetAgent
import com.example.ui.theme.*
import com.example.ui.viewmodel.TermiAgentViewModel

@Composable
fun PresetsScreen(
    viewModel: TermiAgentViewModel,
    onNavigateToChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsState()
    val selectedHost by viewModel.selectedHost.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
            Column {
                Text(
                    text = "Preset AI Checklists",
                    color = SophisticatedText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "One-tap diagnosis templates executed via safe agent tunnels.",
                    color = SophisticatedTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning or note about selected host
        Card(
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Active Target Node",
                        color = SophisticatedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = selectedHost?.let { "${it.name} (${it.username}@${it.ipOrHostname})" }
                            ?: "No node selected. Please link and select a node.",
                        color = if (selectedHost != null) SophisticatedAccent else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(presets, key = { it.id }) { preset ->
                PresetCard(
                    preset = preset,
                    enabled = selectedHost != null,
                    onClick = {
                        viewModel.executePreset(preset)
                        onNavigateToChat()
                    }
                )
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: PresetAgent,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = when (preset.category) {
        "DOCKER" -> SophisticatedPrimary
        "CLEANUP" -> SophisticatedAccent
        "NETWORK" -> SophisticatedPrimary
        else -> SophisticatedPrimary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) SophisticatedSurface else SophisticatedSurfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                SophisticatedBorder,
                RoundedCornerShape(24.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .testTag("preset_card_${preset.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = 0.15f))
                    .border(1.dp, categoryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = when (preset.iconName) {
                        "view_module" -> Icons.Outlined.GridView
                        "cleaning_services" -> Icons.Outlined.CleaningServices
                        "lan" -> Icons.Outlined.Lan
                        "speed" -> Icons.Outlined.Speed
                        else -> Icons.Outlined.SmartButton
                    },
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.title,
                        color = if (enabled) SophisticatedText else SophisticatedTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = preset.category,
                            color = categoryColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = preset.description,
                    color = if (enabled) SophisticatedTextMuted else SophisticatedBorder,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Shows command template
                Text(
                    text = "Script: ${preset.commandTemplate}",
                    color = if (enabled) SophisticatedAccent else SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (enabled) SophisticatedViewportBg else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}
