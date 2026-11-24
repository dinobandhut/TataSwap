package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainNetwork
import com.example.data.model.Token
import com.example.ui.components.SlippageSettingsDialog
import com.example.ui.components.TokenSelectorModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.SwapUiState

@Composable
fun SwapScreen(
    swapState: SwapUiState?,
    allTokens: List<Token>,
    currentNetwork: BlockchainNetwork,
    connectedWallet: String?,
    onAmountInChange: (String) -> Unit,
    onTokenInSelect: (Token) -> Unit,
    onTokenOutSelect: (Token) -> Unit,
    onSwitchTokens: () -> Unit,
    onSlippageChange: (Double) -> Unit,
    onExecuteSwap: () -> Unit,
    onConnectWallet: () -> Unit
) {
    if (swapState == null) return

    var showTokenInModal by remember { mutableStateOf(false) }
    var showTokenOutModal by remember { mutableStateOf(false) }
    var showSlippageModal by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current

    val amountInNum = swapState.amountIn.toDoubleOrNull() ?: 0.0
    val hasSufficientBalance = amountInNum <= swapState.tokenIn.userBalance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Swap Header Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Instant Swap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Automated Market Maker on ${currentNetwork.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = { showSlippageModal = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .testTag("swap_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = BnbGold
                )
            }
        }

        // SWAP CONTAINER CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TOKEN IN INPUT BOX
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("You Pay", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Balance: ${"%.4f".format(swapState.tokenIn.userBalance)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                TextButton(
                                    onClick = { onAmountInChange(swapState.tokenIn.userBalance.toString()) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("MAX", color = BnbGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextField(
                                value = swapState.amountIn,
                                onValueChange = onAmountInChange,
                                placeholder = { Text("0.0", color = TextTertiary, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("swap_amount_in_input")
                            )

                            // Token selector button
                            Surface(
                                onClick = { showTokenInModal = true },
                                shape = RoundedCornerShape(20.dp),
                                color = DarkBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                                modifier = Modifier.testTag("token_in_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(BnbGold.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(swapState.tokenIn.symbol.take(2), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BnbGold)
                                    }
                                    Text(swapState.tokenIn.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        }

                        // USD Estimate
                        Text(
                            text = "~$${"%.2f".format(amountInNum * swapState.tokenIn.priceUsd)} USD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // INVERT SWITCH BUTTON
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(color = DarkSurfaceBorder)
                    IconButton(
                        onClick = onSwitchTokens,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, BnbGold, CircleShape)
                            .testTag("swap_invert_tokens_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Switch Tokens",
                            tint = BnbGold
                        )
                    }
                }

                // TOKEN OUT INPUT BOX
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("You Receive", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = "Balance: ${"%.4f".format(swapState.tokenOut.userBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (swapState.amountOut.isNotEmpty()) swapState.amountOut else "0.0",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (swapState.amountOut.isNotEmpty()) TextPrimary else TextTertiary,
                                modifier = Modifier.weight(1f)
                            )

                            // Token selector button
                            Surface(
                                onClick = { showTokenOutModal = true },
                                shape = RoundedCornerShape(20.dp),
                                color = DarkBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                                modifier = Modifier.testTag("token_out_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(swapState.tokenOut.symbol.take(2), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    }
                                    Text(swapState.tokenOut.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        }

                        val outNum = swapState.amountOut.toDoubleOrNull() ?: 0.0
                        Text(
                            text = "~$${"%.2f".format(outNum * swapState.tokenOut.priceUsd)} USD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // SWAP DETAILS ACCORDION
                swapState.quote?.let { quote ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exchange Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("1 ${quote.tokenIn.symbol} = ${"%.4f".format(quote.executionRate)} ${quote.tokenOut.symbol}", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Minimum Received", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${"%.4f".format(quote.minimumReceived)} ${quote.tokenOut.symbol}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price Impact", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${"%.2f".format(quote.priceImpactPercent)}%", style = MaterialTheme.typography.labelSmall, color = if (quote.priceImpactPercent > 3.0) CryptoRed else CryptoGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Slippage Tolerance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${swapState.slippagePercent}%", style = MaterialTheme.typography.labelSmall, color = BnbGold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Network Fee (Gas)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("~${quote.gasEstimateNative} BNB (~$0.28)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Routing Path", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(quote.route.joinToString(" ➔ "), style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SWAP ACTION BUTTON
                Button(
                    onClick = {
                        if (connectedWallet == null) onConnectWallet()
                        else onExecuteSwap()
                    },
                    enabled = connectedWallet == null || (amountInNum > 0.0 && hasSufficientBalance && !swapState.isSwapping),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BnbGold,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = TextTertiary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("swap_execute_button")
                ) {
                    if (swapState.isSwapping) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else if (connectedWallet == null) {
                        Text("Connect Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else if (amountInNum <= 0.0) {
                        Text("Enter an Amount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else if (!hasSufficientBalance) {
                        Text("Insufficient ${swapState.tokenIn.symbol} Balance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text("Swap ${swapState.tokenIn.symbol}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // SWAP SUCCESS CARD
        AnimatedVisibility(visible = swapState.lastSwapTx != null) {
            swapState.lastSwapTx?.let { tx ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CryptoGreen)
                            Text("Transaction Confirmed on BSC!", fontWeight = FontWeight.Bold, color = CryptoGreen)
                        }
                        Text(tx.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(tx.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tx: ${tx.txHash.take(12)}...${tx.txHash.takeLast(6)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BnbGold
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(tx.txHash)) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Hash", tint = BnbGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modals
    if (showTokenInModal) {
        TokenSelectorModal(
            tokens = allTokens,
            onSelectToken = {
                onTokenInSelect(it)
                showTokenInModal = false
            },
            onDismiss = { showTokenInModal = false }
        )
    }

    if (showTokenOutModal) {
        TokenSelectorModal(
            tokens = allTokens,
            onSelectToken = {
                onTokenOutSelect(it)
                showTokenOutModal = false
            },
            onDismiss = { showTokenOutModal = false }
        )
    }

    if (showSlippageModal) {
        SlippageSettingsDialog(
            currentSlippage = swapState.slippagePercent,
            onSelectSlippage = {
                onSlippageChange(it)
                showSlippageModal = false
            },
            onDismiss = { showSlippageModal = false }
        )
    }
}
