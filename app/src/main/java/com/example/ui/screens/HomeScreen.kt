package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartCondition
import com.example.data.model.ProjectStatus
import com.example.ui.components.LocationBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.navigation.MainDestination
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: PartPilotViewModel,
    onNavigateToStock: () -> Unit,
    onNavigateToAddPart: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToProjectDetail: (Long) -> Unit,
    onNavigateToPartDetail: (Long) -> Unit,
    onNavigateToAskAi: (prefilledQuery: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val stockUiState by viewModel.stockUiState.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()

    var aiBarText by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedCsv by remember { mutableStateOf("") }

    val activeProjects = allProjects.filter { it.status == ProjectStatus.ACTIVE }
    val latestActiveProject = activeProjects.firstOrNull()
    val totalPartsCount = stockUiState.parts.sumOf { it.quantity }
    val lowStockParts = stockUiState.parts.filter { it.quantity <= it.minStock && it.quantity > 0 }
    val damagedParts = stockUiState.parts.filter { it.condition == PartCondition.DAMAGED || it.condition == PartCondition.FAULTY }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkshopBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workshop Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PartPilot",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = WorkshopTeal
                    )
                    Text(
                        text = "Electronics Inventory & Companion",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp,
                        color = WorkshopTextMuted
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            exportedCsv = viewModel.repository.exportInventoryCsv()
                            showExportDialog = true
                        }
                    },
                    modifier = Modifier
                        .background(WorkshopSurface, CircleShape)
                        .border(1.dp, WorkshopBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = WorkshopTeal
                    )
                }
            }
        }

        // Grounded Ask-AI Bar
        item {
            WorkshopCard(
                backgroundColor = WorkshopSurface,
                borderColor = WorkshopTeal.copy(alpha = 0.4f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = WorkshopTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask Workshop AI",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = WorkshopTeal
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiBarText,
                            onValueChange = { aiBarText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = "e.g. Where is my MPU6050?",
                                    fontFamily = IbmPlexSansFamily,
                                    fontSize = 13.sp,
                                    color = WorkshopTextMuted
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = WorkshopBg,
                                unfocusedContainerColor = WorkshopBg,
                                focusedBorderColor = WorkshopTeal,
                                unfocusedBorderColor = WorkshopBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val query = aiBarText.ifBlank { "Where is my MPU6050?" }
                                onNavigateToAskAi(query)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Ask",
                                tint = WorkshopTeal
                            )
                        }
                    }
                }
            }
        }

        // Inventory Quick Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Parts",
                    value = "$totalPartsCount",
                    caption = "${stockUiState.parts.size} unique types",
                    onClick = onNavigateToStock
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Projects",
                    value = "${activeProjects.size}",
                    caption = "in development",
                    onClick = {
                        latestActiveProject?.let { onNavigateToProjectDetail(it.id) }
                    }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Storage Bins",
                    value = "${allLocations.size}",
                    caption = "QR labeled",
                    onClick = onNavigateToLocations
                )
            }
        }

        // Continue Building Card
        if (latestActiveProject != null) {
            item {
                Text(
                    text = "CONTINUE BUILDING",
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = WorkshopTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                WorkshopCard(
                    onClick = { onNavigateToProjectDetail(latestActiveProject.id) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = latestActiveProject.name,
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WorkshopText
                            )
                            Box(
                                modifier = Modifier
                                    .background(ChipHaveBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = ChipHaveText,
                                    fontFamily = IbmPlexSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = latestActiveProject.description,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 13.sp,
                            color = WorkshopTextMuted,
                            maxLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Budget Cap: $${String.format(java.util.Locale.ROOT, "%.2f", latestActiveProject.budgetCap)}",
                                fontFamily = IbmPlexMonoFamily,
                                fontSize = 12.sp,
                                color = WorkshopText
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Open BOM & Checklist",
                                    fontFamily = IbmPlexSansFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WorkshopTeal
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = WorkshopTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "QUICK ACTIONS",
                fontFamily = IbmPlexSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = WorkshopTextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    modifier = Modifier.weight(1f),
                    title = "Fast Add",
                    icon = Icons.Default.CameraAlt,
                    onClick = onNavigateToAddPart
                )
                QuickActionItem(
                    modifier = Modifier.weight(1f),
                    title = "Boxes & QR",
                    icon = Icons.Default.QrCode2,
                    onClick = onNavigateToLocations
                )
                QuickActionItem(
                    modifier = Modifier.weight(1f),
                    title = "Stock List",
                    icon = Icons.Default.ListAlt,
                    onClick = onNavigateToStock
                )
            }
        }

        // Needs Attention Section
        if (lowStockParts.isNotEmpty() || damagedParts.isNotEmpty()) {
            item {
                Text(
                    text = "NEEDS ATTENTION",
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = WorkshopTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lowStockParts.take(3).forEach { part ->
                        val loc = stockUiState.locations[part.locationId]
                        WorkshopCard(
                            borderColor = ChipLowText.copy(alpha = 0.3f),
                            onClick = { onNavigateToPartDetail(part.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = part.name,
                                        fontFamily = IbmPlexSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = WorkshopText
                                    )
                                    Text(
                                        text = "Low Stock: only ${part.quantity} remaining (Min: ${part.minStock})",
                                        fontFamily = IbmPlexSansFamily,
                                        fontSize = 12.sp,
                                        color = ChipLowText
                                    )
                                }
                                loc?.let {
                                    LocationBadge(code = it.code)
                                }
                            }
                        }
                    }

                    damagedParts.take(2).forEach { part ->
                        WorkshopCard(
                            borderColor = ChipNeedText.copy(alpha = 0.3f),
                            onClick = { onNavigateToPartDetail(part.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = part.name,
                                        fontFamily = IbmPlexSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = WorkshopText
                                    )
                                    Text(
                                        text = "Status: ${part.condition.displayName} — ${part.remarks.ifBlank { "Needs testing or replacement" }}",
                                        fontFamily = IbmPlexSansFamily,
                                        fontSize = 12.sp,
                                        color = ChipNeedText
                                    )
                                }
                                Text(
                                    text = "Inspect",
                                    fontFamily = IbmPlexSansFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkshopTeal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export CSV Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Export Inventory to CSV", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Full workshop CSV generated (${stockUiState.parts.size} records with free stock and location codes).",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 14.sp
                    )
                    Text(
                        text = exportedCsv.lines().take(5).joinToString("\n") + "\n...",
                        fontFamily = IbmPlexMonoFamily,
                        fontSize = 11.sp,
                        color = WorkshopTextMuted,
                        modifier = Modifier
                            .background(WorkshopBg, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedCsv))
                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Copy CSV")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportedCsv)
                            type = "text/csv"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Inventory CSV")
                        context.startActivity(shareIntent)
                        showExportDialog = false
                    }
                ) {
                    Text("Share", color = WorkshopTeal)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    caption: String,
    onClick: () -> Unit
) {
    WorkshopCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontFamily = IbmPlexSansFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = WorkshopTextMuted
            )
            Text(
                text = value,
                fontFamily = IbmPlexMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WorkshopTeal
            )
            Text(
                text = caption,
                fontFamily = IbmPlexSansFamily,
                fontSize = 10.sp,
                color = WorkshopTextMuted
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    WorkshopCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(WorkshopTealLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = WorkshopTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontFamily = IbmPlexSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = WorkshopText
            )
        }
    }
}
