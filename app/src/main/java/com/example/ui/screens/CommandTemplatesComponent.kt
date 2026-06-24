package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CommandTemplate
import com.example.data.model.CommandTemplateLibrary
import com.example.ui.theme.*

@Composable
fun CommandTemplatesRow(
    onSelectTemplate: (CommandTemplate) -> Unit,
    onInjectAndOptimize: (CommandTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLibraryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SophisticatedViewportBg)
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recurring Workflows",
                    color = SophisticatedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = { showLibraryDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("open_templates_library_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = SophisticatedAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Browse Library",
                    color = SophisticatedAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CommandTemplateLibrary.templates) { template ->
                TemplateQuickChip(
                    template = template,
                    onClick = { onSelectTemplate(template) },
                    onLongClick = { onInjectAndOptimize(template) }
                )
            }
        }
    }

    if (showLibraryDialog) {
        CommandTemplatesLibraryDialog(
            onDismiss = { showLibraryDialog = false },
            onSelectTemplate = { template ->
                onSelectTemplate(template)
                showLibraryDialog = false
            },
            onInjectAndOptimize = { template ->
                onInjectAndOptimize(template)
                showLibraryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateQuickChip(
    template: CommandTemplate,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val categoryColor = when (template.category) {
        "System" -> Color(0xFF10B981)
        "Security" -> Color(0xFFEF4444)
        "Storage" -> Color(0xFFF59E0B)
        "Health" -> Color(0xFF3B82F6)
        "Performance" -> Color(0xFF8B5CF6)
        "Docker" -> Color(0xFF06B6D4)
        else -> SophisticatedPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("template_chip_${template.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(categoryColor, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = template.title,
                color = SophisticatedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandTemplatesLibraryDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (CommandTemplate) -> Unit,
    onInjectAndOptimize: (CommandTemplate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "System", "Security", "Storage", "Health", "Performance", "Docker")

    val filteredTemplates = remember(searchQuery, selectedCategory) {
        CommandTemplateLibrary.templates.filter { template ->
            val matchesSearch = template.title.contains(searchQuery, ignoreCase = true) ||
                    template.shortDescription.contains(searchQuery, ignoreCase = true) ||
                    template.promptDraft.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || template.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
                .testTag("command_templates_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = SophisticatedAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Templates Library",
                            color = SophisticatedText,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = SophisticatedTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a predefined system administration template to inject into your prompt optimizer workspace.",
                    color = SophisticatedTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates...", fontSize = 13.sp, color = SophisticatedTextMuted) },
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
                        .height(52.dp)
                        .testTag("templates_search_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SophisticatedTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Categories Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        val categoryColor = when (category) {
                            "System" -> Color(0xFF10B981)
                            "Security" -> Color(0xFFEF4444)
                            "Storage" -> Color(0xFFF59E0B)
                            "Health" -> Color(0xFF3B82F6)
                            "Performance" -> Color(0xFF8B5CF6)
                            "Docker" -> Color(0xFF06B6D4)
                            else -> SophisticatedPrimary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SophisticatedViewportBg,
                                selectedContainerColor = categoryColor.copy(alpha = 0.2f),
                                labelColor = SophisticatedTextMuted,
                                selectedLabelColor = categoryColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SophisticatedBorder,
                                selectedBorderColor = categoryColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Templates List
                if (filteredTemplates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No templates match your search criteria.",
                            color = SophisticatedTextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTemplates) { template ->
                            TemplateListItemCard(
                                template = template,
                                onSelect = { onSelectTemplate(template) },
                                onInjectAndOptimize = { onInjectAndOptimize(template) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateListItemCard(
    template: CommandTemplate,
    onSelect: () -> Unit,
    onInjectAndOptimize: () -> Unit
) {
    val categoryColor = when (template.category) {
        "System" -> Color(0xFF10B981)
        "Security" -> Color(0xFFEF4444)
        "Storage" -> Color(0xFFF59E0B)
        "Health" -> Color(0xFF3B82F6)
        "Performance" -> Color(0xFF8B5CF6)
        "Docker" -> Color(0xFF06B6D4)
        else -> SophisticatedPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SophisticatedViewportBg),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.title,
                    color = SophisticatedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Category Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = template.category.uppercase(),
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.fullExplanation,
                color = SophisticatedTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Draft prompt container
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "PROMPT TEMPLATE",
                        color = SophisticatedTextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = template.promptDraft,
                        color = SophisticatedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Load button (Simple load to field)
                TextButton(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit in input",
                        tint = SophisticatedText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inject Draft", fontSize = 11.sp, color = SophisticatedText)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Inject and Optimize button
                Button(
                    onClick = onInjectAndOptimize,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedAccent.copy(alpha = 0.2f),
                        contentColor = SophisticatedAccent
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .border(1.dp, SophisticatedAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Inject and optimize",
                        tint = SophisticatedAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inject & Optimize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
