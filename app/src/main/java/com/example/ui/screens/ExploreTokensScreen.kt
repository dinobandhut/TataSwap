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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Token
import com.example.ui.theme.*
import com.example.ui.viewmodel.DexScreen

@Composable
fun ExploreTokensScreen(
    tokens: List<Token>,
    onSelectToken: (Token) -> Unit,
    onNavigateToListing: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTokens = remember(tokens, searchQuery, selectedFilter) {
        tokens.filter { token ->
            val matchesQuery = searchQuery.isBlank() ||
                token.name.contains(searchQuery, ignoreCase = true) ||
                token.symbol.contains(searchQuery, ignoreCase = true) ||
                token.address.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Verified" -> token.isVerified
                "Community" -> !token.isVerified
                "Gainers" -> token.change24h > 0
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Token Explorer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Browse, search and verify all tokens traded across TataSwap DEX.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, symbol, or 0x contract address") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("token_explorer_search")
                )
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("All", "Verified", "Community", "Gainers")
                    items(filters) { f ->
                        val isSel = selectedFilter == f
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedFilter = f },
                            label = { Text(f, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BnbGold,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = if (isSel) BnbGold else DarkSurfaceBorder
                            )
                        )
                    }
                }
            }

            // Token count info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredTokens.size} tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Real-time AMM Pricing",
                        style = MaterialTheme.typography.labelSmall,
                        color = BnbGold
                    )
                }
            }

            items(filteredTokens) { token ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectToken(token) }
                        .testTag("token_item_${token.symbol}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (token.symbol) {
                                            "BNB" -> BnbGold
                                            "USDT" -> Color(0xFF26A17B)
                                            "TATA" -> NeonCyan
                                            "CAKE" -> Color(0xFFD1884F)
                                            "BTCB" -> Color(0xFFF7931A)
                                            "ETH" -> Color(0xFF627EEA)
                                            else -> DarkSurfaceBorder
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(token.symbol.take(3), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(token.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (token.isVerified) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    } else {
                                        Surface(
                                            color = CryptoRedBg,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Unverified",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CryptoRed,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = token.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${token.address.take(6)}...${token.address.takeLast(4)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${"%.2f".format(token.priceUsd)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val isPos = token.change24h >= 0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPos) CryptoGreenBg else CryptoRedBg,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "${if (isPos) "+" else ""}${"%.2f".format(token.change24h)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPos) CryptoGreen else CryptoRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Vol: $${"%,.0f".format(token.volume24h)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button to List Token
        ExtendedFloatingActionButton(
            onClick = onNavigateToListing,
            containerColor = BnbGold,
            contentColor = Color.Black,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("List Token (5 USDT)", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_list_token")
        )
    }
}
