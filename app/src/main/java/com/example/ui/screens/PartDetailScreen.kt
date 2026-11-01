package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(
    partId: Long,
    viewModel: PartPilotViewModel,
    onBack: () -> Unit,
    onNavigateToProject: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val part by viewModel.repository.getPartFlow(partId).collectAsState(initial = null)
    val events by viewModel.repository.getEventsForPart(partId).collectAsState(initial = emptyList())
    val allLocations by viewModel.allLocations.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    var freeStock by remember { mutableStateOf(0) }
    var projectsUsingPart by remember { mutableStateOf<List<Pair<ProjectEntity, Int>>>(emptyList()) }

    // Dialog states
    var showAdjustQtyDialog by remember { mutableStateOf(false) }
    var adjustQtyInput by remember { mutableStateOf("") }
    var adjustNoteInput by remember { mutableStateOf("") }

    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedMoveLocId by remember { mutableStateOf<Long?>(null) }

    var showDamageDialog by remember { mutableStateOf(false) }
    var damageReasonInput by remember { mutableStateOf("") }
    var damageReduceQtyInput by remember { mutableStateOf("0") }

    var showConditionDialog by remember { mutableStateOf(false) }
    var selectedCondition by remember { mutableStateOf(PartCondition.WORKING) }
    var conditionRemarksInput by remember { mutableStateOf("") }

    var showUseInProjectDialog by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var useQtyInput by remember { mutableStateOf("1") }

    val locMap = remember(allLocations) { allLocations.associateBy { it.id } }

    LaunchedEffect(part) {
        part?.let { p ->
            freeStock = viewModel.repository.computePartFreeStock(p.id)
            selectedMoveLocId = p.locationId
            selectedCondition = p.condition

            // Find projects using this part
            val projectParts = viewModel.repository.getProjectPartsForPart(p.id)
            val projMap = allProjects.associateBy { it.id }
            projectsUsingPart = projectParts.mapNotNull { pp ->
                projMap[pp.projectId]?.let { pr -> Pair(pr, pp.qtyNeeded) }
            }
        }
    }

    if (part == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Component Details", fontFamily = IbmPlexSansFamily) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WorkshopTeal)
            }
        }
        return
    }

    val currentPart = part!!
    val reserved = currentPart.quantity - freeStock
    val currentLoc = currentPart.locationId?.let { locMap[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentPart.name,
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
            // Header Info Card
            item {
                WorkshopCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentPart.group.displayName.uppercase(),
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WorkshopTeal
                            )
                            ConditionChip(condition = currentPart.condition)
                        }

                        Text(
                            text = currentPart.name,
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WorkshopText
                        )

                        Text(
                            text = currentPart.specs,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 14.sp,
                            color = WorkshopText
                        )

                        if (currentPart.remarks.isNotBlank()) {
                            Text(
                                text = "Remarks: ${currentPart.remarks}",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 13.sp,
                                color = WorkshopTextMuted
                            )
                        }

                        if (currentPart.datasheetUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentPart.datasheetUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopTeal.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = WorkshopTeal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Datasheet", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTeal)
                            }
                        }
                    }
                }
            }

            // Inventory & Location Numbers Card
            item {
                WorkshopCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "STOCK & STORAGE",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WorkshopTextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${currentPart.quantity}",
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = WorkshopText
                                )
                                Text("Total In Stock", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                            }

                            Column {
                                Text(
                                    text = "$freeStock",
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = WorkshopTeal
                                )
                                Text("Free to Use", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                            }

                            Column {
                                Text(
                                    text = "$reserved",
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = if (reserved > 0) ChipCartText else WorkshopTextMuted
                                )
                                Text("Reserved", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                            }
                        }

                        Divider(color = WorkshopBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Location Bin", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                                Text(
                                    text = currentLoc?.let { "Shelf ${it.shelf}, Level ${it.level}, Box ${it.box}" } ?: "Not assigned",
                                    fontFamily = IbmPlexSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = WorkshopText
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                currentLoc?.let { LocationBadge(code = it.code) }
                                OutlinedButton(
                                    onClick = { showMoveDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder)
                                ) {
                                    Text("Move", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTeal)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            adjustQtyInput = "${currentPart.quantity}"
                            adjustNoteInput = ""
                            showAdjustQtyDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WorkshopSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Adjust Qty", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopText)
                    }

                    Button(
                        onClick = { showDamageDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WorkshopSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Mark Damaged", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = ChipNeedText)
                    }

                    Button(
                        onClick = {
                            conditionRemarksInput = currentPart.remarks
                            showConditionDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WorkshopSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Test / Condition", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTeal)
                    }
                }
            }

            // Projects using this Part
            item {
                Text(
                    text = "PROJECTS USING THIS PART",
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = WorkshopTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (projectsUsingPart.isEmpty()) {
                    WorkshopCard {
                        Text(
                            text = "No active projects are currently using this part.",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 13.sp,
                            color = WorkshopTextMuted
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        projectsUsingPart.forEach { (project, needed) ->
                            WorkshopCard(
                                onClick = { onNavigateToProject(project.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = project.name,
                                            fontFamily = IbmPlexSansFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = WorkshopText
                                        )
                                        Text(
                                            text = "Status: ${project.status.displayName}",
                                            fontFamily = IbmPlexSansFamily,
                                            fontSize = 12.sp,
                                            color = WorkshopTextMuted
                                        )
                                    }
                                    Text(
                                        text = "$needed needed",
                                        fontFamily = IbmPlexMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = WorkshopTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Events Timeline
            item {
                Text(
                    text = "PART EVENT TIMELINE",
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = WorkshopTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (events.isEmpty()) {
                    WorkshopCard {
                        Text("No recorded events for this component.", fontFamily = IbmPlexSansFamily, fontSize = 13.sp, color = WorkshopTextMuted)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.forEach { event ->
                            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(event.createdAt))
                            WorkshopCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = event.type.displayName.uppercase(),
                                            fontFamily = IbmPlexSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = WorkshopTeal
                                        )
                                        Text(
                                            text = dateStr,
                                            fontFamily = IbmPlexMonoFamily,
                                            fontSize = 11.sp,
                                            color = WorkshopTextMuted
                                        )
                                    }
                                    Text(
                                        text = event.note,
                                        fontFamily = IbmPlexSansFamily,
                                        fontSize = 13.sp,
                                        color = WorkshopText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Adjust Quantity Dialog
    if (showAdjustQtyDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustQtyDialog = false },
            title = { Text("Adjust Quantity", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = adjustQtyInput,
                        onValueChange = { adjustQtyInput = it },
                        label = { Text("New Quantity") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adjustNoteInput,
                        onValueChange = { adjustNoteInput = it },
                        label = { Text("Reason / Note (optional)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = adjustQtyInput.toIntOrNull()
                        if (newQty != null && newQty >= 0) {
                            coroutineScope.launch {
                                viewModel.repository.adjustPartQuantity(currentPart.id, newQty, adjustNoteInput)
                                showAdjustQtyDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustQtyDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Move Location Dialog
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to Location", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allLocations) { loc ->
                        val isSelected = loc.id == selectedMoveLocId
                        WorkshopCard(
                            borderColor = if (isSelected) WorkshopTeal else WorkshopBorder,
                            backgroundColor = if (isSelected) WorkshopTealLight else WorkshopSurface,
                            onClick = { selectedMoveLocId = loc.id }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Shelf ${loc.shelf}, Level ${loc.level}, Box ${loc.box}", fontFamily = IbmPlexSansFamily, fontSize = 13.sp)
                                    Text(loc.notes, fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted)
                                }
                                LocationBadge(code = loc.code)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMoveLocId?.let { locId ->
                            coroutineScope.launch {
                                viewModel.repository.movePart(currentPart.id, locId)
                                showMoveDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Confirm Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Mark Damaged Dialog
    if (showDamageDialog) {
        AlertDialog(
            onDismissRequest = { showDamageDialog = false },
            title = { Text("Mark Component Damaged", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Record what went wrong (e.g. burnt trace, wrong polarity, broken pin) to keep a clear audit history.",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp,
                        color = WorkshopTextMuted
                    )
                    OutlinedTextField(
                        value = damageReasonInput,
                        onValueChange = { damageReasonInput = it },
                        label = { Text("Failure Reason *") }
                    )
                    OutlinedTextField(
                        value = damageReduceQtyInput,
                        onValueChange = { damageReduceQtyInput = it },
                        label = { Text("Reduce working stock by (units)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (damageReasonInput.isNotBlank()) {
                            val reduce = damageReduceQtyInput.toIntOrNull() ?: 0
                            coroutineScope.launch {
                                viewModel.repository.markPartDamaged(currentPart.id, damageReasonInput, reduce)
                                showDamageDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChipNeedText)
                ) {
                    Text("Mark Damaged")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDamageDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Condition Dialog
    if (showConditionDialog) {
        AlertDialog(
            onDismissRequest = { showConditionDialog = false },
            title = { Text("Update Condition & Testing", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PartCondition.values().forEach { cond ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedCondition == cond,
                                onClick = { selectedCondition = cond }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cond.displayName, fontFamily = IbmPlexSansFamily, fontSize = 14.sp)
                        }
                    }
                    OutlinedTextField(
                        value = conditionRemarksInput,
                        onValueChange = { conditionRemarksInput = it },
                        label = { Text("Testing Remarks / Notes") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.updateCondition(currentPart.id, selectedCondition, conditionRemarksInput)
                            showConditionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConditionDialog = false }) { Text("Cancel") }
            }
        )
    }
}
