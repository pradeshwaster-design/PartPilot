package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiChatMessage
import com.example.ai.ProposedWriteAction
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: PartPilotViewModel,
    initialQuery: String = ""
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val allAiLogs by viewModel.allAiLogs.collectAsState()

    var textInput by remember { mutableStateOf(initialQuery) }
    var activeTab by remember { mutableStateOf(0) } // 0: Chat, 1: AI Action Audit Log

    // Speech Recognizer launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                textInput = spokenText
                viewModel.sendAiQuery(spokenText)
                textInput = ""
            }
        }
    }

    var lastProcessedQuery by remember { mutableStateOf("") }
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != lastProcessedQuery) {
            lastProcessedQuery = initialQuery
            viewModel.sendAiQuery(initialQuery)
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WorkshopTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PartPilot AI Assistant",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WorkshopBg, titleContentColor = WorkshopText)
            )
        },
        containerColor = WorkshopBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row: Chat vs Audit Log
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = WorkshopSurface,
                contentColor = WorkshopTeal,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, WorkshopBorder, RoundedCornerShape(10.dp))
                    .padding(2.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Grounded Assistant", fontFamily = IbmPlexSansFamily, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Action Log (${allAiLogs.size})", fontFamily = IbmPlexSansFamily, fontSize = 12.sp) }
                )
            }

            if (activeTab == 0) {
                // Grounded Assistant Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Quick query suggestion chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(text = "Do I have an L298N?") {
                            viewModel.sendAiQuery("Do I have an L298N?")
                        }
                        SuggestionChip(text = "Where is my MPU6050?") {
                            viewModel.sendAiQuery("Where is my MPU6050?")
                        }
                        SuggestionChip(text = "What is in box A-2-B3?") {
                            viewModel.sendAiQuery("What is in box A-2-B3?")
                        }
                        SuggestionChip(text = "What do I still need for Line Follower v2?") {
                            viewModel.sendAiQuery("What do I still need for Line Follower v2?")
                        }
                    }

                    // Messages LazyColumn
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                onConfirmAction = { action ->
                                    viewModel.executeAiWriteAction(msg.id, action)
                                }
                            )
                        }

                        if (isThinking) {
                            item {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = WorkshopTeal)
                                    Text("Querying workshop database...", fontFamily = IbmPlexSansFamily, fontSize = 12.sp, color = WorkshopTextMuted)
                                }
                            }
                        }
                    }

                    // Chat Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(WorkshopSurface, RoundedCornerShape(24.dp))
                            .border(1.dp, WorkshopBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_input_field"),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 14.sp,
                                color = WorkshopText
                            ),
                            decorationBox = { innerTextField ->
                                if (textInput.isEmpty()) {
                                    Text(
                                        text = "Ask inventory or propose an edit...",
                                        fontFamily = IbmPlexSansFamily,
                                        fontSize = 14.sp,
                                        color = WorkshopTextMuted
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Mic button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask PartPilot...")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    textInput = "Where is my MPU6050?"
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = WorkshopTeal)
                        }

                        // Send button
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendAiQuery(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            enabled = textInput.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank()) WorkshopTeal else WorkshopTextMuted
                            )
                        }
                    }
                }
            } else {
                // Action Audit Log
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allAiLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No actions executed yet. Write actions proposed by the AI require your confirmation and will be logged here.",
                                fontFamily = IbmPlexSansFamily,
                                fontSize = 13.sp,
                                color = WorkshopTextMuted
                            )
                        }
                    } else {
                        items(allAiLogs) { log ->
                            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(log.executedAt))
                            WorkshopCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.actionType,
                                            fontFamily = IbmPlexMonoFamily,
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
                                        text = log.summary,
                                        fontFamily = IbmPlexSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = WorkshopText
                                    )
                                    Text(
                                        text = log.details,
                                        fontFamily = IbmPlexSansFamily,
                                        fontSize = 12.sp,
                                        color = WorkshopTextMuted
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

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(WorkshopSurface, RoundedCornerShape(14.dp))
            .border(1.dp, WorkshopBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontFamily = IbmPlexSansFamily,
            fontSize = 11.sp,
            color = WorkshopTealDark
        )
    }
}

@Composable
private fun ChatMessageItem(
    message: AiChatMessage,
    onConfirmAction: (ProposedWriteAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        val bubbleBg = if (message.isUser) WorkshopTeal else WorkshopSurface
        val bubbleText = if (message.isUser) Color.White else WorkshopText
        val border = if (message.isUser) null else androidx.compose.foundation.BorderStroke(1.dp, WorkshopBorder)

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (message.isUser) 14.dp else 2.dp,
                bottomEnd = if (message.isUser) 2.dp else 14.dp
            ),
            color = bubbleBg,
            border = border,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    fontFamily = IbmPlexSansFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = bubbleText
                )
            }
        }

        // Before / After Proposed Write Action Diff Card
        message.proposedAction?.let { action ->
            Spacer(modifier = Modifier.height(8.dp))
            WorkshopCard(
                borderColor = if (message.actionExecuted) WorkshopTeal else ChipLowText,
                backgroundColor = if (message.actionExecuted) WorkshopTealLight.copy(alpha = 0.3f) else WorkshopSurface,
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROPOSED INVENTORY ACTION",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (message.actionExecuted) WorkshopTeal else ChipLowText
                        )

                        if (message.actionExecuted) {
                            Box(
                                modifier = Modifier
                                    .background(ChipHaveBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EXECUTED", color = ChipHaveText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = action.title,
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WorkshopText
                    )

                    // Before vs After diff
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WorkshopBg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "BEFORE:",
                            fontFamily = IbmPlexMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = WorkshopTextMuted
                        )
                        Text(
                            text = action.beforeState,
                            fontFamily = IbmPlexMonoFamily,
                            fontSize = 12.sp,
                            color = ChipNeedText
                        )

                        Divider(color = WorkshopBorder, modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "AFTER:",
                            fontFamily = IbmPlexMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = WorkshopTextMuted
                        )
                        Text(
                            text = action.afterState,
                            fontFamily = IbmPlexMonoFamily,
                            fontSize = 12.sp,
                            color = ChipHaveText
                        )
                    }

                    if (!message.actionExecuted) {
                        Button(
                            onClick = { onConfirmAction(action) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Execute Action", fontFamily = IbmPlexSansFamily, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
