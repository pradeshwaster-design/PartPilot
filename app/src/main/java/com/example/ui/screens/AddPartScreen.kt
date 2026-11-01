package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ai.ParsedPartCandidate
import com.example.data.model.PartGroup
import com.example.ui.components.LocationBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartScreen(
    viewModel: PartPilotViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val candidates by viewModel.addCandidates.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()

    var inputModeTab by remember { mutableStateOf(0) } // 0: Photo / Vision, 1: Type / Voice, 2: Manual
    var voiceOrTextInput by remember { mutableStateOf("") }

    // Manual form state
    var manualName by remember { mutableStateOf("") }
    var manualGroup by remember { mutableStateOf(PartGroup.MICROCONTROLLER) }
    var manualSpecs by remember { mutableStateOf("") }
    var manualQty by remember { mutableStateOf("1") }
    var manualLocId by remember { mutableStateOf<Long?>(1L) }

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                voiceOrTextInput = spokenText
                viewModel.parseVoiceOrTextInput(spokenText)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fast Component Intake",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Mode Tabs: Photo/Vision, Type/Voice, Manual
            TabRow(
                selectedTabIndex = inputModeTab,
                containerColor = WorkshopSurface,
                contentColor = WorkshopTeal,
                modifier = Modifier
                    .border(1.dp, WorkshopBorder, RoundedCornerShape(10.dp))
                    .padding(2.dp)
            ) {
                Tab(
                    selected = inputModeTab == 0,
                    onClick = { inputModeTab = 0 },
                    text = { Text("Photo Scan", fontFamily = IbmPlexSansFamily, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = inputModeTab == 1,
                    onClick = { inputModeTab = 1 },
                    text = { Text("Type / Voice", fontFamily = IbmPlexSansFamily, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = inputModeTab == 2,
                    onClick = { inputModeTab = 2 },
                    text = { Text("Manual Form", fontFamily = IbmPlexSansFamily, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (inputModeTab) {
                0 -> {
                    // Photo / Table Mode
                    WorkshopCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "WORKBENCH PHOTO SCAN",
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WorkshopTeal
                            )
                            Text(
                                text = "Photograph a single electronic part or a whole messy workbench table. The AI vision model classifies each part, suggests the best storage box, and flags any low-confidence reads.",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 13.sp,
                                color = WorkshopText
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.parsePhotoScan("Workbench components detection")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze Table Photo", fontFamily = IbmPlexSansFamily, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Type or Voice Mode
                    WorkshopCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "NATURAL LANGUAGE INTAKE",
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WorkshopTeal
                            )
                            Text(
                                text = "Type or speak natural phrases like \"Add 10 resistors 220 ohm to Shelf B level 1 box 2\".",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 13.sp,
                                color = WorkshopTextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = voiceOrTextInput,
                                    onValueChange = { voiceOrTextInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. Add 5x SG90 servos to Box B-1-B2", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what parts you want to add...")
                                        }
                                        try {
                                            speechLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            // Fallback prompt simulation
                                            voiceOrTextInput = "Add 10 resistors 220 ohm to Shelf B level 1 box 2"
                                            viewModel.parseVoiceOrTextInput(voiceOrTextInput)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(WorkshopTealLight, CircleShape)
                                        .size(44.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = WorkshopTeal)
                                }
                            }

                            Button(
                                onClick = {
                                    if (voiceOrTextInput.isNotBlank()) {
                                        viewModel.parseVoiceOrTextInput(voiceOrTextInput)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Parse & Review", fontFamily = IbmPlexSansFamily, fontSize = 13.sp)
                            }
                        }
                    }
                }
                2 -> {
                    // Manual Form
                    WorkshopCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("MANUAL ENTRY", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WorkshopTeal)
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("Component Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualSpecs,
                                onValueChange = { manualSpecs = it },
                                label = { Text("Specifications / Package") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualQty,
                                    onValueChange = { manualQty = it },
                                    label = { Text("Quantity") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = {
                                    if (manualName.isNotBlank()) {
                                        val qty = manualQty.toIntOrNull() ?: 1
                                        val cand = ParsedPartCandidate(
                                            name = manualName,
                                            group = manualGroup,
                                            specs = manualSpecs,
                                            quantity = qty,
                                            confidence = 1.0f,
                                            suggestedLocationId = manualLocId
                                        )
                                        viewModel.updateCandidate(cand.id) { cand }
                                        manualName = ""
                                        manualSpecs = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add to Review List", fontFamily = IbmPlexSansFamily)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Candidates Review Header
            if (candidates.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REVIEW DRAFT PARTS (${candidates.size})",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = WorkshopTextMuted
                    )

                    Button(
                        onClick = {
                            viewModel.commitCandidatesToStock()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All to Stock", fontFamily = IbmPlexSansFamily, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Editable Candidates List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(candidates, key = { it.id }) { candidate ->
                        CandidateCard(
                            candidate = candidate,
                            locations = allLocations,
                            onUpdate = { updated -> viewModel.updateCandidate(candidate.id) { updated } },
                            onRemove = { viewModel.removeCandidate(candidate.id) },
                            onMerge = { existingId -> viewModel.mergeCandidateWithExisting(candidate.id, existingId, candidate.quantity) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ParsedPartCandidate,
    locations: List<com.example.data.model.LocationEntity>,
    onUpdate: (ParsedPartCandidate) -> Unit,
    onRemove: () -> Unit,
    onMerge: (Long) -> Unit
) {
    val isLowConfidence = candidate.confidence < 0.7f
    val borderColor = if (isLowConfidence) ChipLowText.copy(alpha = 0.5f) else WorkshopBorder
    val bgColor = if (isLowConfidence) ChipLowBg.copy(alpha = 0.25f) else WorkshopSurface

    WorkshopCard(
        borderColor = borderColor,
        backgroundColor = bgColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header with Confidence Badge & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Confidence: ${(candidate.confidence * 100).toInt()}%",
                        fontFamily = IbmPlexMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isLowConfidence) ChipLowText else WorkshopTeal
                    )
                    if (isLowConfidence) {
                        Text(
                            text = "(Low — please verify)",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 11.sp,
                            color = ChipLowText
                        )
                    }
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = WorkshopTextMuted)
                }
            }

            // Editable Name
            OutlinedTextField(
                value = candidate.name,
                onValueChange = { onUpdate(candidate.copy(name = it)) },
                label = { Text("Component Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Specs & Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = candidate.specs,
                    onValueChange = { onUpdate(candidate.copy(specs = it)) },
                    label = { Text("Specs") },
                    modifier = Modifier.weight(2f)
                )

                OutlinedTextField(
                    value = "${candidate.quantity}",
                    onValueChange = {
                        val q = it.toIntOrNull() ?: 1
                        onUpdate(candidate.copy(quantity = q))
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Suggested Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suggested Box:",
                    fontFamily = IbmPlexSansFamily,
                    fontSize = 12.sp,
                    color = WorkshopTextMuted
                )
                LocationBadge(code = candidate.suggestedLocationCode)
            }

            // Duplicate Warning & Merge Option
            if (candidate.potentialDuplicates.isNotEmpty()) {
                val dup = candidate.potentialDuplicates.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChipLowBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Duplicate Detected: '${dup.name}' already in stock (${dup.quantity} units).",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            color = ChipLowText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onMerge(dup.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ChipLowText),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Merge Stock (+${candidate.quantity})", fontFamily = IbmPlexSansFamily, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onUpdate(candidate.copy(potentialDuplicates = emptyList())) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Keep Separate", fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopText)
                            }
                        }
                    }
                }
            }
        }
    }
}
