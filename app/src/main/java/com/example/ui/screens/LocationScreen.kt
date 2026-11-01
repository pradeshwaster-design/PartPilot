package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationEntity
import com.example.data.model.PartEntity
import com.example.ui.components.LocationBadge
import com.example.ui.components.QrCodeView
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: PartPilotViewModel,
    onBack: () -> Unit,
    onNavigateToPartDetail: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allLocations by viewModel.allLocations.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var shelfInput by remember { mutableStateOf("") }
    var levelInput by remember { mutableStateOf("") }
    var boxInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    var selectedQrLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var selectedBoxContents by remember { mutableStateOf<Pair<LocationEntity, List<PartEntity>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workshop Storage Bins",
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
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Location", tint = WorkshopTeal)
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
            Text(
                text = "ORGANIZATION CODE FORMAT: SHELF-LEVEL-BOX (e.g. A-2-B3)",
                fontFamily = IbmPlexMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = WorkshopTeal
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (allLocations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No storage bins configured yet.", fontFamily = IbmPlexSansFamily, color = WorkshopTextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(allLocations, key = { it.id }) { loc ->
                        WorkshopCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Shelf ${loc.shelf}  •  Level ${loc.level}  •  Box ${loc.box}",
                                            fontFamily = IbmPlexSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = WorkshopText
                                        )
                                        if (loc.notes.isNotBlank()) {
                                            Text(
                                                text = loc.notes,
                                                fontFamily = IbmPlexSansFamily,
                                                fontSize = 12.sp,
                                                color = WorkshopTextMuted
                                            )
                                        }
                                    }

                                    LocationBadge(code = loc.code)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val parts = viewModel.repository.getPartsByLocation(loc.id)
                                                selectedBoxContents = Pair(loc, parts)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = WorkshopTeal)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Contents", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTeal)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedQrLocation = loc },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = WorkshopTeal)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Print QR Label", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Location Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Storage Location", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shelfInput,
                            onValueChange = { shelfInput = it },
                            label = { Text("Shelf (e.g. A)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = levelInput,
                            onValueChange = { levelInput = it },
                            label = { Text("Level (e.g. 2)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = boxInput,
                            onValueChange = { boxInput = it },
                            label = { Text("Box (e.g. B3)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Bin Description / Categories") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (shelfInput.isNotBlank() && levelInput.isNotBlank() && boxInput.isNotBlank()) {
                            viewModel.createLocation(shelfInput, levelInput, boxInput, notesInput)
                            shelfInput = ""
                            levelInput = ""
                            boxInput = ""
                            notesInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Create Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // QR Code Label Dialog
    if (selectedQrLocation != null) {
        val loc = selectedQrLocation!!
        AlertDialog(
            onDismissRequest = { selectedQrLocation = null },
            title = {
                Text(
                    text = "Box QR Code Label",
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Scan or print this label for your physical storage bin:",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp,
                        color = WorkshopTextMuted
                    )

                    QrCodeView(data = "PARTPILOT://${loc.code}", sizeDp = 180)

                    Text(
                        text = "Shelf ${loc.shelf} • Level ${loc.level} • Box ${loc.box}",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = WorkshopText
                    )
                    if (loc.notes.isNotBlank()) {
                        Text(
                            text = loc.notes,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            color = WorkshopTextMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedQrLocation = null },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Box Contents Dialog
    if (selectedBoxContents != null) {
        val (loc, parts) = selectedBoxContents!!
        AlertDialog(
            onDismissRequest = { selectedBoxContents = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Box ${loc.code}", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
                    LocationBadge(code = loc.code)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${parts.size} component types stored inside:",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp,
                        color = WorkshopTextMuted
                    )

                    if (parts.isEmpty()) {
                        Text("This storage bin is empty.", fontFamily = IbmPlexSansFamily, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(parts) { part ->
                                WorkshopCard(
                                    onClick = {
                                        selectedBoxContents = null
                                        onNavigateToPartDetail(part.id)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(part.name, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(part.specs, fontFamily = IbmPlexSansFamily, fontSize = 11.sp, color = WorkshopTextMuted, maxLines = 1)
                                        }
                                        Text("${part.quantity}x", fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WorkshopTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBoxContents = null },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Done")
                }
            }
        )
    }
}
