package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.StatusChip
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    viewModel: PartPilotViewModel,
    onBack: () -> Unit,
    onNavigateToPartDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val project by viewModel.repository.getProjectFlow(projectId).collectAsState(initial = null)
    val bomItems by viewModel.repository.getProjectPartsFlow(projectId).collectAsState(initial = emptyList())
    val allParts by viewModel.repository.allParts.collectAsState(initial = emptyList())

    // Dialogs
    var showAddBomDialog by remember { mutableStateOf(false) }
    var selectedPartIdForBom by remember { mutableStateOf<Long?>(null) }
    var customPartNameInput by remember { mutableStateOf("") }
    var neededQtyInput by remember { mutableStateOf("1") }
    var estPriceInput by remember { mutableStateOf("5.00") }

    var showFinishDialog by remember { mutableStateOf(false) }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WorkshopTeal)
        }
        return
    }

    val currentProject = project!!
    val totalEstCost = bomItems.sumOf { it.qtyNeeded * it.estUnitPrice }
    val isOverBudget = currentProject.budgetCap > 0 && totalEstCost > currentProject.budgetCap

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentProject.name,
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentProject.status == ProjectStatus.ACTIVE) {
                        TextButton(onClick = { showFinishDialog = true }) {
                            Text("Finish Project", color = WorkshopTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WorkshopBg,
                    titleContentColor = WorkshopText
                )
            )
        },
        containerColor = WorkshopBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Project Overview Card & Budget
            item {
                WorkshopCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROJECT STATUS",
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = WorkshopTeal
                            )

                            Box(
                                modifier = Modifier
                                    .background(if (currentProject.status == ProjectStatus.COMPLETED) WorkshopBorder else ChipHaveBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentProject.status.displayName.uppercase(),
                                    color = if (currentProject.status == ProjectStatus.COMPLETED) WorkshopTextMuted else ChipHaveText,
                                    fontFamily = IbmPlexSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = currentProject.description,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 14.sp,
                            color = WorkshopText
                        )

                        if (currentProject.notes.isNotBlank()) {
                            Text(
                                text = "Notes: ${currentProject.notes}",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 12.sp,
                                color = WorkshopTextMuted
                            )
                        }

                        Divider(color = WorkshopBorder)

                        // Budget Tracker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Estimated Total Cost", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                                Text(
                                    text = "$${String.format(Locale.ROOT, "%.2f", totalEstCost)}",
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isOverBudget) ChipNeedText else WorkshopTeal
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Budget Cap", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                                Text(
                                    text = "$${String.format(Locale.ROOT, "%.2f", currentProject.budgetCap)}",
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = WorkshopText
                                )
                            }
                        }

                        if (isOverBudget) {
                            Text(
                                text = "⚠️ Over budget cap by $${String.format(Locale.ROOT, "%.2f", totalEstCost - currentProject.budgetCap)}",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 12.sp,
                                color = ChipNeedText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // BOM Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BILL OF MATERIALS (BOM)",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = WorkshopTextMuted
                    )

                    if (currentProject.status == ProjectStatus.ACTIVE) {
                        Button(
                            onClick = { showAddBomDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Part", fontFamily = IbmPlexSansFamily, fontSize = 12.sp)
                        }
                    }
                }
            }

            // BOM Items List with live computed status chips & checklists
            items(bomItems, key = { it.id }) { item ->
                BomItemCard(
                    item = item,
                    viewModel = viewModel,
                    isProjectActive = currentProject.status == ProjectStatus.ACTIVE,
                    onNavigateToPart = { item.partId?.let { onNavigateToPartDetail(it) } }
                )
            }
        }
    }

    // Add BOM Item Dialog
    if (showAddBomDialog) {
        AlertDialog(
            onDismissRequest = { showAddBomDialog = false },
            title = { Text("Add Part to BOM", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select from owned parts or enter a new need:", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTextMuted)

                    OutlinedTextField(
                        value = customPartNameInput,
                        onValueChange = { customPartNameInput = it },
                        label = { Text("Part Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = neededQtyInput,
                            onValueChange = { neededQtyInput = it },
                            label = { Text("Qty Needed") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = estPriceInput,
                            onValueChange = { estPriceInput = it },
                            label = { Text("Est Unit Price ($)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Owned Parts Quick Suggestions
                    Text("Or pick from workshop stock:", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(allParts.take(6)) { p ->
                            TextButton(
                                onClick = {
                                    customPartNameInput = p.name
                                    selectedPartIdForBom = p.id
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.name, fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopText)
                                    Text("${p.quantity} in stock", fontFamily = IbmPlexMonoFamily, fontSize = 11.sp, color = WorkshopTeal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPartNameInput.isNotBlank()) {
                            val qty = neededQtyInput.toIntOrNull() ?: 1
                            val price = estPriceInput.toDoubleOrNull() ?: 0.0
                            val matchedPart = allParts.find { it.name.equals(customPartNameInput, ignoreCase = true) }
                            viewModel.addPartToProject(
                                projectId = currentProject.id,
                                partId = selectedPartIdForBom ?: matchedPart?.id,
                                name = customPartNameInput,
                                qty = qty,
                                price = price
                            )
                            customPartNameInput = ""
                            selectedPartIdForBom = null
                            showAddBomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Add to BOM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBomDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Finish Project Confirmation Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish & Complete Project?", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Marking '${currentProject.name}' as completed will automatically release all held reservations and return parts back to your available stock.",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Final Total BOM Cost: $${String.format(Locale.ROOT, "%.2f", totalEstCost)}",
                        fontFamily = IbmPlexMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WorkshopTeal
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishProject(currentProject.id)
                        showFinishDialog = false
                        Toast.makeText(context, "Project completed & stock returned!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Finish Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BomItemCard(
    item: ProjectPartEntity,
    viewModel: PartPilotViewModel,
    isProjectActive: Boolean,
    onNavigateToPart: () -> Unit
) {
    var computedStatus by remember { mutableStateOf(ComputedPartStatus.NEED) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(item) {
        coroutineScope.launch {
            computedStatus = viewModel.repository.computeProjectPartStatus(item)
        }
    }

    WorkshopCard(onClick = if (item.partId != null) onNavigateToPart else null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WorkshopText
                    )
                    Text(
                        text = "Need: ${item.qtyNeeded} • Est Unit: $${item.estUnitPrice}",
                        fontFamily = IbmPlexMonoFamily,
                        fontSize = 12.sp,
                        color = WorkshopTextMuted
                    )
                }

                StatusChip(status = computedStatus)
            }

            // Reservation & Checklist State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Reservation Status
                if (item.partId != null && isProjectActive) {
                    if (item.reservedQty >= item.qtyNeeded) {
                        Text(
                            text = "🔒 ${item.reservedQty} units reserved",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            color = WorkshopTealDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.reserveStock(item.id, item.qtyNeeded)
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Hold & Reserve", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTeal)
                        }
                    }
                } else if (item.partId == null) {
                    Text(
                        text = "Not owned in inventory",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 11.sp,
                        color = ChipNeedText
                    )
                }

                // Checklist State selector
                ChecklistStateSelector(
                    currentState = item.checklistState,
                    onSelect = { newState ->
                        viewModel.updateChecklist(item.id, newState)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistStateSelector(
    currentState: ChecklistState,
    onSelect: (ChecklistState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = WorkshopTeal)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentState.displayName,
                fontFamily = IbmPlexSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = WorkshopText
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ChecklistState.values().forEach { state ->
                DropdownMenuItem(
                    text = {
                        Text(
                            state.displayName,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            fontWeight = if (state == currentState) FontWeight.Bold else FontWeight.Normal,
                            color = if (state == currentState) WorkshopTeal else WorkshopText
                        )
                    },
                    onClick = {
                        onSelect(state)
                        expanded = false
                    }
                )
            }
        }
    }
}
