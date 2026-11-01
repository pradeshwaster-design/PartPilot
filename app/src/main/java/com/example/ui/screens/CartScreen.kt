package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItemEntity
import com.example.ui.components.LocationBadge
import com.example.ui.components.WorkshopCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartPilotViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: PartPilotViewModel
) {
    val cartItems by viewModel.allCartItems.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    val projMap = remember(allProjects) { allProjects.associateBy { it.id } }

    var showAddDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var qtyInput by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("5.00") }
    var shopInput by remember { mutableStateOf("") }

    var buyingItem by remember { mutableStateOf<CartItemEntity?>(null) }
    var selectedLocationId by remember { mutableStateOf<Long?>(null) }

    val totalCartCost = cartItems.sumOf { it.qty * it.unitPrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Shopping Cart & Wishlist",
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cart Item", tint = WorkshopTeal)
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
                .padding(horizontal = 16.dp)
        ) {
            // Cart Summary Card
            WorkshopCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${cartItems.size} ITEMS TO PURCHASE",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = WorkshopTextMuted
                        )
                        Text(
                            text = "Estimated Budget: $${String.format(Locale.ROOT, "%.2f", totalCartCost)}",
                            fontFamily = IbmPlexMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WorkshopTeal
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(ChipCartBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "BUYING LIST",
                            fontFamily = IbmPlexSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ChipCartText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = WorkshopTextMuted, modifier = Modifier.size(48.dp))
                        Text("Cart is currently empty", fontFamily = IbmPlexSansFamily, color = WorkshopTextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        val projName = item.projectId?.let { projMap[it]?.name }

                        WorkshopCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontFamily = IbmPlexSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = WorkshopText
                                        )
                                        if (projName != null) {
                                            Text(
                                                text = "For Project: $projName",
                                                fontFamily = IbmPlexSansFamily,
                                                fontSize = 12.sp,
                                                color = WorkshopTeal
                                            )
                                        }
                                        if (item.shop.isNotBlank()) {
                                            Text(
                                                text = "Shop: ${item.shop}",
                                                fontFamily = IbmPlexSansFamily,
                                                fontSize = 12.sp,
                                                color = WorkshopTextMuted
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format(Locale.ROOT, "%.2f", item.qty * item.unitPrice)}",
                                            fontFamily = IbmPlexMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = WorkshopText
                                        )
                                        Text(
                                            text = "${item.qty}x @ $${item.unitPrice}",
                                            fontFamily = IbmPlexMonoFamily,
                                            fontSize = 11.sp,
                                            color = WorkshopTextMuted
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { viewModel.removeCartItem(item) }) {
                                        Text("Remove", color = ChipNeedText, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            buyingItem = item
                                            selectedLocationId = allLocations.firstOrNull()?.id
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark as Bought", fontFamily = IbmPlexSansFamily, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Cart Item Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item to Cart", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Part Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("Unit Price ($)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = shopInput,
                        onValueChange = { shopInput = it },
                        label = { Text("Vendor / Shop (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val qty = qtyInput.toIntOrNull() ?: 1
                            val price = priceInput.toDoubleOrNull() ?: 0.0
                            viewModel.addCartItem(nameInput, qty, price, shop = shopInput)
                            nameInput = ""
                            shopInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Add to Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Mark as Bought Location Picker Dialog
    if (buyingItem != null) {
        val targetItem = buyingItem!!
        AlertDialog(
            onDismissRequest = { buyingItem = null },
            title = { Text("Transfer to Stock Bin", fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select storage box to place ${targetItem.qty}x '${targetItem.name}':",
                        fontFamily = IbmPlexSansFamily,
                        fontSize = 13.sp
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allLocations) { loc ->
                            val isSelected = loc.id == selectedLocationId
                            WorkshopCard(
                                borderColor = if (isSelected) WorkshopTeal else WorkshopBorder,
                                backgroundColor = if (isSelected) WorkshopTealLight else WorkshopSurface,
                                onClick = { selectedLocationId = loc.id }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedLocationId?.let { locId ->
                            viewModel.markCartItemBought(targetItem, locId)
                            buyingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkshopTeal)
                ) {
                    Text("Place in Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { buyingItem = null }) { Text("Cancel") }
            }
        )
    }
}
