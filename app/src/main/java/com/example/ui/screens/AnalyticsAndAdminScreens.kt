package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DexProtocolStats
import com.example.data.model.LiquidityPool
import com.example.data.model.Token
import com.example.ui.components.PriceChartPreview
import com.example.ui.theme.*

@Composable
fun AnalyticsScreen(
    stats: DexProtocolStats,
    pools: List<LiquidityPool>,
    tokens: List<Token>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TataSwap Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Real-time AMM metrics, trading volumes, and protocol treasury revenues.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        // Metrics Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Protocol TVL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%,.0f".format(stats.tvlUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text("+4.8% (7d)", style = MaterialTheme.typography.labelSmall, color = CryptoGreen)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("24h Volume", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%,.0f".format(stats.volume24hUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = BnbGold)
                        Text("${stats.totalSwaps} swaps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Protocol Fees", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%,.0f".format(stats.totalProtocolFeesUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = NeonCyan)
                        Text("Treasury revenue", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Listing Fees Collected", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${"%,.0f".format(stats.totalListingFeesCollectedUsdt)} USDT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = UniPink)
                        Text("Direct to Owner", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        // Volume Trend Chart Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Protocol Volume Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    PriceChartPreview(
                        isPositive = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }
        }

        // Top Pools Table
        item {
            Text("Top Pools by Liquidity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(pools) { pool ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${pool.tokenA.symbol} / ${pool.tokenB.symbol}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("24h Vol: $${"%,.0f".format(pool.volume24h)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("TVL: $${"%,.0f".format(pool.tvlUsd)}", fontWeight = FontWeight.Bold, color = BnbGold)
                        Text("${"%.1f".format(pool.feeAprPercent)}% APR", style = MaterialTheme.typography.labelSmall, color = CryptoGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    stats: DexProtocolStats,
    tokens: List<Token>,
    onUpdateListingFee: (Double) -> Unit,
    onUpdateFeeReceiver: (String) -> Unit,
    onToggleEmergencyPause: () -> Unit,
    onToggleTokenVerification: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showFeeDialog by remember { mutableStateOf(false) }
    var showReceiverDialog by remember { mutableStateOf(false) }
    var newFeeStr by remember { mutableStateOf("${stats.officialListingFeeUsdt}") }
    var newReceiverStr by remember { mutableStateOf(stats.officialFeeReceiverWallet) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BnbGold)
                Text("DEX Admin Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Text("Owner controls for listing fee parameters, contract safety, and token verification.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        // DEX Owner Identity Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BnbGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Official DEX Owner & Fee Receiver", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Surface(shape = RoundedCornerShape(6.dp), color = BnbGold.copy(alpha = 0.2f)) {
                            Text("Authorized", style = MaterialTheme.typography.labelSmall, color = BnbGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stats.officialFeeReceiverWallet,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(stats.officialFeeReceiverWallet)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Text(
                        text = "Notice: All automated permissionless listing fees are sent directly to this address via smart contract event TataSwapListingFee.TokenListed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Configurable Parameters Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Protocol Parameters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Listing fee setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Listing Fee", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${stats.officialListingFeeUsdt} USDT", fontWeight = FontWeight.Bold, color = BnbGold, fontSize = 18.sp)
                        }
                        OutlinedButton(
                            onClick = { showFeeDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BnbGold),
                            modifier = Modifier.testTag("admin_change_fee_button")
                        ) {
                            Text("Change Fee")
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    // Change Fee Receiver
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fee Receiver Address", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${stats.officialFeeReceiverWallet.take(8)}...${stats.officialFeeReceiverWallet.takeLast(6)}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = { showReceiverDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                        ) {
                            Text("Change Receiver")
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    // Emergency Pause
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Emergency Pause Switch", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = if (stats.isEmergencyPaused) "CONTRACTS PAUSED" else "SYSTEM ACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isEmergencyPaused) CryptoRed else CryptoGreen
                            )
                        }
                        Switch(
                            checked = stats.isEmergencyPaused,
                            onCheckedChange = { onToggleEmergencyPause() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CryptoRed,
                                checkedTrackColor = CryptoRedBg,
                                uncheckedThumbColor = CryptoGreen,
                                uncheckedTrackColor = DarkSurface
                            ),
                            modifier = Modifier.testTag("admin_pause_switch")
                        )
                    }
                }
            }
        }

        // Manage Token Verification Badges
        item {
            Text("Token Verification Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(tokens) { token ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(token.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(token.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text("${token.address.take(8)}...${token.address.takeLast(6)}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }

                    OutlinedButton(
                        onClick = { onToggleTokenVerification(token.address) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (token.isVerified) CryptoRed else CryptoGreen
                        )
                    ) {
                        Text(if (token.isVerified) "Unverify" else "Verify", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showFeeDialog) {
        AlertDialog(
            onDismissRequest = { showFeeDialog = false },
            containerColor = DarkSurface,
            title = { Text("Update Listing Fee", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new fee in USDT required to list a token on TataSwap:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedTextField(
                        value = newFeeStr,
                        onValueChange = { newFeeStr = it },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = newFeeStr.toDoubleOrNull()
                        if (fee != null && fee >= 0.0) {
                            onUpdateListingFee(fee)
                        }
                        showFeeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black)
                ) {
                    Text("Save Fee", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeeDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    if (showReceiverDialog) {
        AlertDialog(
            onDismissRequest = { showReceiverDialog = false },
            containerColor = DarkSurface,
            title = { Text("Update Fee Receiver Wallet", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new 0x wallet address to receive 100% of listing fees:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedTextField(
                        value = newReceiverStr,
                        onValueChange = { newReceiverStr = it },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newReceiverStr.startsWith("0x") && newReceiverStr.length == 42) {
                            onUpdateFeeReceiver(newReceiverStr.trim())
                        }
                        showReceiverDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Save Receiver", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceiverDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}
