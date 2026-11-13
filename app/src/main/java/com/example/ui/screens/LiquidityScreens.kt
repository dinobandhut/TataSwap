package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiquidityPool
import com.example.data.model.Token
import com.example.ui.components.TokenSelectorModal
import com.example.ui.theme.*

@Composable
fun CreatePoolScreen(
    allTokens: List<Token>,
    onCreatePool: (tokenA: Token, tokenB: Token, feeTier: Double, amountA: Double, amountB: Double) -> Unit,
    onBack: () -> Unit
) {
    var tokenA by remember { mutableStateOf(allTokens.firstOrNull { it.symbol == "BNB" } ?: allTokens[0]) }
    var tokenB by remember { mutableStateOf(allTokens.firstOrNull { it.symbol == "TATA" } ?: allTokens[1]) }
    var selectedFeeTier by remember { mutableStateOf(0.3) }
    var amountAStr by remember { mutableStateOf("1.0") }
    var amountBStr by remember { mutableStateOf("430.0") }

    var showTokenAModal by remember { mutableStateOf(false) }
    var showTokenBModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
            Text("Create Liquidity Pool", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Text("Deploy a new AMM pair with custom fee tier on TataSwap.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        // Select Pair Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Select Token Pair", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { showTokenAModal = true },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tokenA.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    Surface(
                        onClick = { showTokenBModal = true },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tokenB.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Fee Tier Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Fee Tier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tiers = listOf(0.05, 0.3, 1.0)
                    tiers.forEach { tier ->
                        val isSel = selectedFeeTier == tier
                        Surface(
                            onClick = { selectedFeeTier = tier },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) BnbGold else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BnbGold else DarkSurfaceBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$tier%", fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextPrimary)
                                Text(
                                    when (tier) {
                                        0.05 -> "Stable"
                                        0.3 -> "Standard"
                                        else -> "Exotic"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Color.Black else TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Deposit Amounts Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("3. Deposit Initial Liquidity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                OutlinedTextField(
                    value = amountAStr,
                    onValueChange = { amountAStr = it },
                    label = { Text("Amount ${tokenA.symbol}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountBStr,
                    onValueChange = { amountBStr = it },
                    label = { Text("Amount ${tokenB.symbol}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                val a = amountAStr.toDoubleOrNull() ?: 1.0
                val b = amountBStr.toDoubleOrNull() ?: 1.0
                onCreatePool(tokenA, tokenB, selectedFeeTier, a, b)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("create_pool_submit_button")
        ) {
            Text("Create Pool & Supply Initial Liquidity", fontWeight = FontWeight.Bold)
        }
    }

    if (showTokenAModal) {
        TokenSelectorModal(tokens = allTokens, onSelectToken = { tokenA = it; showTokenAModal = false }, onDismiss = { showTokenAModal = false })
    }
    if (showTokenBModal) {
        TokenSelectorModal(tokens = allTokens, onSelectToken = { tokenB = it; showTokenBModal = false }, onDismiss = { showTokenBModal = false })
    }
}

@Composable
fun AddLiquidityScreen(
    pool: LiquidityPool?,
    onAddLiquidity: (poolId: String, amountA: Double, amountB: Double) -> Unit,
    onBack: () -> Unit
) {
    if (pool == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pool selected", color = TextSecondary)
        }
        return
    }

    var amountAStr by remember { mutableStateOf("0.5") }
    // Auto ratio based on pool reserves
    val ratio = pool.reserveB / pool.reserveA.coerceAtLeast(0.0001)
    val amountB = (amountAStr.toDoubleOrNull() ?: 0.0) * ratio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
            Text("Add Liquidity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${pool.tokenA.symbol} / ${pool.tokenB.symbol} Pool (Tier: ${pool.feeTierPercent}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pool TVL:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("$${"%,.0f".format(pool.tvlUsd)}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fee APR:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("${"%.1f".format(pool.feeAprPercent)}%", color = CryptoGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(color = DarkSurfaceBorder)

                OutlinedTextField(
                    value = amountAStr,
                    onValueChange = { amountAStr = it },
                    label = { Text("Deposit ${pool.tokenA.symbol}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Required ${pool.tokenB.symbol}:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("${"%.4f".format(amountB)} ${pool.tokenB.symbol}", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Button(
            onClick = {
                val a = amountAStr.toDoubleOrNull() ?: 0.0
                if (a > 0.0) onAddLiquidity(pool.id, a, amountB)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("add_liquidity_confirm_button")
        ) {
            Text("Supply Liquidity", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RemoveLiquidityScreen(
    pool: LiquidityPool?,
    onRemoveLiquidity: (poolId: String, percentage: Double) -> Unit,
    onBack: () -> Unit
) {
    if (pool == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pool selected", color = TextSecondary)
        }
        return
    }

    var selectedPercent by remember { mutableStateOf(50.0) }
    val fraction = selectedPercent / 100.0
    val userShareFraction = pool.userSharePercent / 100.0
    val receiveA = pool.reserveA * userShareFraction * fraction
    val receiveB = pool.reserveB * userShareFraction * fraction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
            Text("Remove Liquidity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Pool: ${pool.tokenA.symbol} / ${pool.tokenB.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text("Your Pool Share: ${"%.2f".format(pool.userSharePercent)}%", color = BnbGold, fontWeight = FontWeight.Bold)

                Text("Amount to Remove: ${selectedPercent.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

                Slider(
                    value = selectedPercent.toFloat(),
                    onValueChange = { selectedPercent = it.toDouble() },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = BnbGold,
                        activeTrackColor = BnbGold,
                        inactiveTrackColor = DarkSurfaceBorder
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(25.0, 50.0, 75.0, 100.0).forEach { p ->
                        Surface(
                            onClick = { selectedPercent = p },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPercent == p) BnbGold else DarkSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("${p.toInt()}%", fontWeight = FontWeight.Bold, color = if (selectedPercent == p) Color.Black else TextSecondary)
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkSurfaceBorder)

                Text("You will receive:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pool.tokenA.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${"%.4f".format(receiveA)}", fontWeight = FontWeight.Bold, color = NeonCyan)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pool.tokenB.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${"%.4f".format(receiveB)}", fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }

        Button(
            onClick = { onRemoveLiquidity(pool.id, selectedPercent) },
            colors = ButtonDefaults.buttonColors(containerColor = CryptoRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("remove_liquidity_confirm_button")
        ) {
            Text("Confirm Removal", fontWeight = FontWeight.Bold)
        }
    }
}
