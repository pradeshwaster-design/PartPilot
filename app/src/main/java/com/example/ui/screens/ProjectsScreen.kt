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
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import java.util.Locale

@Composable
fun ProjectsScreen(
    viewModel: PartPilotViewModel,
    onNavigateToProjectDetail: (Long) -> Unit
) {
    val allProjects by viewModel.allProjects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var budgetCapInput by remember { mutableStateOf("50.0") }
    var notesInput by remember { mutableStateOf("") }

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

            // Screen Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hardware Projects",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = WorkshopText
                    )
                    Text(
                        text = "${allProjects.size} projects in workshop",
                        fontFamily = IbmPlexMonoFamily,
                        fontSize = 12.sp,
                        color = WorkshopTextMuted
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project", fontFamily = IbmPlexSansFamily, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (allProjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No projects yet. Tap 'New Project' to start!",
                        fontFamily = IbmPlexSansFamily,
                        color = WorkshopTextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(allProjects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onNavigateToProjectDetail(project.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Hardware Project", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Project Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetCapInput,
                        onValueChange = { budgetCapInput = it },
                        label = { Text("Budget Cap ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Technical Notes / Pin Plan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val cap = budgetCapInput.toDoubleOrNull() ?: 0.0
                            viewModel.createProject(nameInput, descInput, cap, notesInput)
                            nameInput = ""
                            descInput = ""
                            notesInput = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    val isCompleted = project.status == ProjectStatus.COMPLETED
    val statusBg = if (isCompleted) WorkshopBorder.copy(alpha = 0.5f) else ChipHaveBg
    val statusText = if (isCompleted) WorkshopTextMuted else ChipHaveText

    WorkshopCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WorkshopText
                )

                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.status.displayName.uppercase(),
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusText
                    )
                }
            }

            Text(
                text = project.description,
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
                    text = "Budget: $${String.format(Locale.ROOT, "%.2f", project.budgetCap)}",
                    fontFamily = IbmPlexMonoFamily,
                    fontSize = 12.sp,
                    color = WorkshopText
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View BOM & Checklist",
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
