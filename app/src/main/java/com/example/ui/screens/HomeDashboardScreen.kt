package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DexProtocolStats
import com.example.data.model.LiquidityPool
import com.example.data.model.Token
import com.example.data.model.TransactionRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.DexScreen

@Composable
fun HomeDashboardScreen(
    stats: DexProtocolStats,
    tokens: List<Token>,
    pools: List<LiquidityPool>,
    recentTransactions: List<TransactionRecord>,
    onNavigate: (DexScreen) -> Unit,
    onSelectToken: (Token) -> Unit,
    onSelectPool: (LiquidityPool) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(BnbGold.copy(alpha = 0.12f), Color.Transparent),
                                radius = 600f
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CryptoGreen)
                                )
                                Text(
                                    text = "BNB Smart Chain DEX",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BnbGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                            ) {
                                Text(
                                    text = "v3 AMM Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Trade & Earn on TataSwap",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Text(
                            text = "Permissionless AMM on BNB Smart Chain. Swap any BEP-20 token, provide liquidity, or list your own token in seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Key Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Value Locked", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$${"%,.0f".format(stats.tvlUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("24h Volume", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$${"%,.0f".format(stats.volume24hUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CryptoGreen)
                            }
                            Column {
                                Text("Active Pools", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${stats.activePoolsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Swap Action Button
                Surface(
                    onClick = { onNavigate(DexScreen.SWAP) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BnbGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_swap_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BnbGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = BnbGold)
                        }
                        Text("Swap Tokens", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Instant AMM trade", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                // List Token Action Button (Permissionless 5 USDT)
                Surface(
                    onClick = { onNavigate(DexScreen.CREATE_LISTING) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_list_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, tint = NeonCyan)
                        }
                        Text("List Token", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Permissionless (5 USDT)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pools Action
                Surface(
                    onClick = { onNavigate(DexScreen.CREATE_POOL) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(UniPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = UniPink)
                        }
                        Text("Liquidity Pools", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Earn 0.25% swap fees", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                // Explore Tokens
                Surface(
                    onClick = { onNavigate(DexScreen.EXPLORE_TOKENS) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue)
                        }
                        Text("Token Explorer", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Explore 50+ tokens", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        // Hot Tokens Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hot Tokens",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = { onNavigate(DexScreen.EXPLORE_TOKENS) }) {
                    Text("View All", color = BnbGold)
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tokens) { token ->
                    Surface(
                        onClick = { onSelectToken(token) },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(BnbGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(token.symbol.take(2), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BnbGold)
                                }
                                Text(token.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$${"%.2f".format(token.priceUsd)}", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            val isPos = token.change24h >= 0
                            Text(
                                text = "${if (isPos) "+" else ""}${"%.2f".format(token.change24h)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPos) CryptoGreen else CryptoRed
                            )
                        }
                    }
                }
            }
        }

        // Top Liquidity Pools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Top Liquidity Pools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = { onNavigate(DexScreen.PORTFOLIO) }) {
                    Text("My Positions", color = BnbGold)
                }
            }
        }

        items(pools) { pool ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPool(pool) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${pool.tokenA.symbol} / ${pool.tokenB.symbol}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkBackground
                            ) {
                                Text(
                                    text = "${pool.feeTierPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BnbGold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TVL: $${"%,.0f".format(pool.tvlUsd)} • 24h Vol: $${"%,.0f".format(pool.volume24h)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.1f".format(pool.feeAprPercent)}% APR",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CryptoGreen
                        )
                        TextButton(
                            onClick = { onSelectPool(pool) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+ Add Liquidity", style = MaterialTheme.typography.labelSmall, color = BnbGold)
                        }
                    }
                }
            }
        }

        // Recent Activity
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(recentTransactions.take(3)) { tx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = CryptoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(tx.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                    }
                    Text(
                        text = "${tx.txHash.take(6)}...${tx.txHash.takeLast(4)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BnbGold
                    )
                }
            }
        }
    }
}
