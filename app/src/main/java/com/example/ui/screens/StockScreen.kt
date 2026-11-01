package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartCondition
import com.example.data.model.PartEntity
import com.example.data.model.PartGroup
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

@Composable
fun StockScreen(
    viewModel: PartPilotViewModel,
    onNavigateToPartDetail: (Long) -> Unit,
    onNavigateToAddPart: () -> Unit
) {
    val uiState by viewModel.stockUiState.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val distinctShelves = remember(allLocations) {
        allLocations.map { it.shelf }.distinct().sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkshopBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Title & Add Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Parts Inventory",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = WorkshopText
                    )
                    Text(
                        text = "${uiState.parts.size} items matching",
                        fontFamily = IbmPlexMonoFamily,
                        fontSize = 12.sp,
                        color = WorkshopTextMuted
                    )
                }

                // Sort Dropdown Trigger
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = WorkshopSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = WorkshopTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.sortOption.displayName,
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            color = WorkshopText
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontFamily = IbmPlexSansFamily,
                                        fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.sortOption == option) WorkshopTeal else WorkshopText
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_parts_input"),
                placeholder = {
                    Text(
                        text = "Search by name, specs, or box code...",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 14.sp,
                        color = WorkshopTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = WorkshopTextMuted
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WorkshopSurface,
                    unfocusedContainerColor = WorkshopSurface,
                    focusedBorderColor = WorkshopTeal,
                    unfocusedBorderColor = WorkshopBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Filter Chips Row (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shelf Filter
                FilterChipPill(
                    label = if (uiState.selectedShelf == null) "All Shelves" else "Shelf ${uiState.selectedShelf}",
                    isSelected = uiState.selectedShelf != null,
                    onClick = {
                        val next = when (uiState.selectedShelf) {
                            null -> distinctShelves.firstOrNull()
                            else -> {
                                val idx = distinctShelves.indexOf(uiState.selectedShelf)
                                if (idx != -1 && idx + 1 < distinctShelves.size) distinctShelves[idx + 1] else null
                            }
                        }
                        viewModel.setSelectedShelf(next)
                    }
                )

                // Group Filter (Cycles popular or opens dropdown)
                FilterChipPill(
                    label = uiState.selectedGroup?.displayName ?: "All Groups",
                    isSelected = uiState.selectedGroup != null,
                    onClick = { showFilterSheet = true }
                )

                // Condition Filter
                FilterChipPill(
                    label = uiState.selectedCondition?.displayName ?: "All Conditions",
                    isSelected = uiState.selectedCondition != null,
                    onClick = {
                        val next = when (uiState.selectedCondition) {
                            null -> PartCondition.WORKING
                            PartCondition.WORKING -> PartCondition.NOT_TESTED
                            PartCondition.NOT_TESTED -> PartCondition.DAMAGED
                            else -> null
                        }
                        viewModel.setSelectedCondition(next)
                    }
                )

                if (uiState.selectedGroup != null || uiState.selectedShelf != null || uiState.selectedCondition != null || uiState.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 12.sp,
                            color = WorkshopTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Parts List
            if (uiState.parts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = WorkshopTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No components match your filters",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkshopText
                        )
                        Text(
                            text = "Try clearing filters or adding a new part",
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 13.sp,
                            color = WorkshopTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(uiState.parts, key = { it.id }) { part ->
                        PartItemCard(
                            part = part,
                            locationCode = uiState.locations[part.locationId]?.code ?: "Unassigned",
                            viewModel = viewModel,
                            onClick = { onNavigateToPartDetail(part.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Part
        FloatingActionButton(
            onClick = onNavigateToAddPart,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .testTag("fab_add_part"),
            containerColor = WorkshopTeal,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Part",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Filter Sheet Dialog for Groups
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = {
                Text("Filter by Group", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.setSelectedGroup(null)
                                showFilterSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "All Groups",
                                color = if (uiState.selectedGroup == null) WorkshopTeal else WorkshopText,
                                fontWeight = if (uiState.selectedGroup == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    items(PartGroup.values()) { group ->
                        TextButton(
                            onClick = {
                                viewModel.setSelectedGroup(group)
                                showFilterSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                group.displayName,
                                color = if (uiState.selectedGroup == group) WorkshopTeal else WorkshopText,
                                fontWeight = if (uiState.selectedGroup == group) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterSheet = false }) {
                    Text("Done", color = WorkshopTeal)
                }
            }
        )
    }
}

@Composable
private fun PartItemCard(
    part: PartEntity,
    locationCode: String,
    viewModel: PartPilotViewModel,
    onClick: () -> Unit
) {
    var freeStock by remember(part.id, part.quantity) { mutableStateOf(part.quantity) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(part.id, part.quantity) {
        coroutineScope.launch {
            freeStock = viewModel.repository.computePartFreeStock(part.id)
        }
    }

    val reserved = part.quantity - freeStock

    WorkshopCard(
        modifier = Modifier.testTag("part_item_${part.id}"),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = WorkshopText
                    )
                    Text(
                        text = part.group.displayName,
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 12.sp,
                        color = WorkshopTextMuted
                    )
                }

                LocationBadge(code = locationCode)
            }

            Text(
                text = part.specs,
                fontFamily = IbmPlexSansFamily,
                fontSize = 13.sp,
                color = WorkshopText,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConditionChip(condition = part.condition)

                    if (reserved > 0) {
                        Box(
                            modifier = Modifier
                                .background(ChipCartBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$reserved reserved",
                                color = ChipCartText,
                                fontFamily = IbmPlexSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                QuantityBadge(
                    quantity = part.quantity,
                    freeStock = if (reserved > 0) freeStock else null
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) WorkshopTealLight else WorkshopSurface,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) WorkshopTeal else WorkshopBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontFamily = IbmPlexSansFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (isSelected) WorkshopTealDark else WorkshopText
        )
    }
}
