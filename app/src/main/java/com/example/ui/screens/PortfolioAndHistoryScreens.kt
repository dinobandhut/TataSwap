package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainNetwork
import com.example.data.model.LiquidityPool
import com.example.data.model.Token
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.*

@Composable
fun PortfolioScreen(
    connectedWallet: String?,
    nativeBalance: Double,
    network: BlockchainNetwork,
    tokens: List<Token>,
    pools: List<LiquidityPool>,
    onAddLiquidityForPool: (LiquidityPool) -> Unit,
    onRemoveLiquidityForPool: (LiquidityPool) -> Unit,
    onConnectWallet: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val totalTokenUsd = tokens.sumOf { it.userBalance * it.priceUsd }
    val totalNetWorth = totalTokenUsd + (nativeBalance * (tokens.firstOrNull { it.symbol == "BNB" }?.priceUsd ?: 620.0))

    val myPools = pools.filter { it.userLpBalance > 0.0 || it.userSharePercent > 0.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("My Web3 Portfolio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Net Worth Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BnbGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Net Worth", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text("$${"%,.2f".format(totalNetWorth)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = TextPrimary)

                    HorizontalDivider(color = DarkSurfaceBorder)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Wallet Address", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = connectedWallet ?: "Not Connected",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedWallet != null) BnbGold else CryptoRed
                            )
                        }
                        if (connectedWallet != null) {
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(connectedWallet)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BnbGold, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = onConnectWallet,
                                colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Connect", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Tokens Balance Section
        item {
            Text("Token Holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(tokens.filter { it.userBalance > 0.0 }) { token ->
            val holdingUsd = token.userBalance * token.priceUsd
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BnbGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(token.symbol.take(2), fontWeight = FontWeight.Bold, color = BnbGold, fontSize = 12.sp)
                        }
                        Column {
                            Text(token.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(token.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("${"%.4f".format(token.userBalance)} ${token.symbol}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("$${"%.2f".format(holdingUsd)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        // Active LP Positions Section
        item {
            Text("Active Liquidity Positions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (myPools.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Text("No active liquidity positions", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Provide liquidity to pools to earn 0.25% on all swaps.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            items(myPools) { pool ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${pool.tokenA.symbol} / ${pool.tokenB.symbol} LP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(shape = RoundedCornerShape(4.dp), color = DarkBackground) {
                                Text(
                                    text = "${pool.feeTierPercent}% Tier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Pool Share", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${"%.2f".format(pool.userSharePercent)}%", fontWeight = FontWeight.Bold, color = BnbGold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("24h Fees Earned", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("+$${"%.2f".format(pool.volume24h * 0.002 * (pool.userSharePercent / 100.0))}", fontWeight = FontWeight.Bold, color = CryptoGreen)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onAddLiquidityForPool(pool) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Add More", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onRemoveLiquidityForPool(pool) },
                                colors = ButtonDefaults.buttonColors(containerColor = CryptoRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Remove", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionRecord>,
    currentNetwork: BlockchainNetwork
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val filtered = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "Swaps" -> transactions.filter { it.type == TransactionType.SWAP }
            "Liquidity" -> transactions.filter { it.type == TransactionType.ADD_LIQUIDITY || it.type == TransactionType.REMOVE_LIQUIDITY }
            "Listings" -> transactions.filter { it.type == TransactionType.TOKEN_LISTING }
            else -> transactions
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("All on-chain activity verified on ${currentNetwork.name}.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        // Filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("All", "Swaps", "Liquidity", "Listings")
                items(filters) { f ->
                    val isSel = selectedFilter == f
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFilter = f },
                        label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BnbGold,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Text("No transactions found", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        } else {
            items(filtered) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = when (tx.type) {
                                        TransactionType.SWAP -> Icons.Default.SwapHoriz
                                        TransactionType.ADD_LIQUIDITY -> Icons.Default.WaterDrop
                                        TransactionType.REMOVE_LIQUIDITY -> Icons.Default.VerticalAlignBottom
                                        TransactionType.TOKEN_LISTING -> Icons.Default.AddBusiness
                                    },
                                    contentDescription = null,
                                    tint = when (tx.type) {
                                        TransactionType.SWAP -> BnbGold
                                        TransactionType.ADD_LIQUIDITY -> NeonCyan
                                        TransactionType.REMOVE_LIQUIDITY -> CryptoRed
                                        TransactionType.TOKEN_LISTING -> UniPink
                                    }
                                )
                                Text(tx.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = CryptoGreenBg) {
                                Text(
                                    text = tx.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CryptoGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(tx.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                        HorizontalDivider(color = DarkSurfaceBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hash: ${tx.txHash.take(10)}...${tx.txHash.takeLast(6)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BnbGold
                            )

                            Row {
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(tx.txHash)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val url = "${currentNetwork.explorerUrl}/tx/${tx.txHash}"
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "Explorer", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
