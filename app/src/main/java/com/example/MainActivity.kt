package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MainDestination
import com.example.ui.navigation.ScreenRoute
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PartPilotTheme {
                val viewModel: PartPilotViewModel = viewModel()
                PartPilotApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PartPilotApp(viewModel: PartPilotViewModel) {
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var currentRoute by remember { mutableStateOf<ScreenRoute>(ScreenRoute.Main) }
    var askAiPrefilledQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.statusMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Top-level back handler when on secondary screens
    if (currentRoute !is ScreenRoute.Main) {
        BackHandler {
            currentRoute = ScreenRoute.Main
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WorkshopBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute is ScreenRoute.Main) {
                NavigationBar(
                    containerColor = WorkshopSurface,
                    contentColor = WorkshopTeal,
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = WorkshopBorder
                    )
                ) {
                    MainDestination.values().forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentDestination = destination
                                currentRoute = ScreenRoute.Main
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontFamily = IbmPlexSansFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WorkshopTealDark,
                                selectedTextColor = WorkshopTealDark,
                                indicatorColor = WorkshopTealLight,
                                unselectedIconColor = WorkshopTextMuted,
                                unselectedTextColor = WorkshopTextMuted
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (currentRoute is ScreenRoute.Main) innerPadding.calculateBottomPadding() else 0.dp
                )
        ) {
            when (val route = currentRoute) {
                is ScreenRoute.Main -> {
                    when (currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToStock = { currentDestination = MainDestination.STOCK },
                                onNavigateToAddPart = { currentRoute = ScreenRoute.AddPart },
                                onNavigateToLocations = { currentRoute = ScreenRoute.LocationManager },
                                onNavigateToProjectDetail = { projId -> currentRoute = ScreenRoute.ProjectDetail(projId) },
                                onNavigateToPartDetail = { pId -> currentRoute = ScreenRoute.PartDetail(pId) },
                                onNavigateToAskAi = { prefill ->
                                    askAiPrefilledQuery = prefill
                                    currentDestination = MainDestination.ASK_AI
                                }
                            )
                        }
                        MainDestination.STOCK -> {
                            StockScreen(
                                viewModel = viewModel,
                                onNavigateToPartDetail = { pId -> currentRoute = ScreenRoute.PartDetail(pId) },
                                onNavigateToAddPart = { currentRoute = ScreenRoute.AddPart }
                            )
                        }
                        MainDestination.PROJECTS -> {
                            ProjectsScreen(
                                viewModel = viewModel,
                                onNavigateToProjectDetail = { projId -> currentRoute = ScreenRoute.ProjectDetail(projId) }
                            )
                        }
                        MainDestination.CART -> {
                            CartScreen(viewModel = viewModel)
                        }
                        MainDestination.ASK_AI -> {
                            AiScreen(
                                viewModel = viewModel,
                                initialQuery = askAiPrefilledQuery
                            )
                        }
                    }
                }
                is ScreenRoute.PartDetail -> {
                    PartDetailScreen(
                        partId = route.partId,
                        viewModel = viewModel,
                        onBack = { currentRoute = ScreenRoute.Main },
                        onNavigateToProject = { projId -> currentRoute = ScreenRoute.ProjectDetail(projId) }
                    )
                }
                is ScreenRoute.AddPart -> {
                    AddPartScreen(
                        viewModel = viewModel,
                        onBack = { currentRoute = ScreenRoute.Main }
                    )
                }
                is ScreenRoute.LocationManager -> {
                    LocationScreen(
                        viewModel = viewModel,
                        onBack = { currentRoute = ScreenRoute.Main },
                        onNavigateToPartDetail = { pId -> currentRoute = ScreenRoute.PartDetail(pId) }
                    )
                }
                is ScreenRoute.ProjectDetail -> {
                    ProjectDetailScreen(
                        projectId = route.projectId,
                        viewModel = viewModel,
                        onBack = { currentRoute = ScreenRoute.Main },
                        onNavigateToPartDetail = { pId -> currentRoute = ScreenRoute.PartDetail(pId) }
                    )
                }
            }
        }
    }
}
