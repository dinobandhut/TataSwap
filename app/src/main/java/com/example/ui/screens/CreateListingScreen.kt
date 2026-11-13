package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainNetwork
import com.example.ui.theme.*
import com.example.ui.viewmodel.DexScreen
import com.example.ui.viewmodel.ListingUiState

@Composable
fun CreateListingScreen(
    listingState: ListingUiState,
    currentNetwork: BlockchainNetwork,
    connectedWallet: String?,
    onAddressChange: (String) -> Unit,
    onInspectContract: () -> Unit,
    onInitialPriceChange: (String) -> Unit,
    onSubmitListing: () -> Unit,
    onConnectWallet: () -> Unit,
    onViewExplorer: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Description
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = BnbGold)
                Text(
                    text = "Permissionless Token Listing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "List any ERC-20 or BEP-20 token instantly on TataSwap without manual admin approval.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Stepper Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Automated Listing Process",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepItem(step = "1", label = "Enter Address", isDone = listingState.contractAddress.isNotEmpty())
                    StepDivider()
                    StepItem(step = "2", label = "RPC Verify", isDone = listingState.detectedInfo != null)
                    StepDivider()
                    StepItem(step = "3", label = "Pay 5 USDT", isDone = listingState.listingSuccessTx != null)
                    StepDivider()
                    StepItem(step = "4", label = "Listed!", isDone = listingState.listingSuccessTx != null)
                }
            }
        }

        // STEP 1: Contract Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "1. Enter Token Contract Address",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Target Network: ${currentNetwork.name} (Chain ID: ${currentNetwork.chainId})",
                    style = MaterialTheme.typography.labelSmall,
                    color = BnbGold
                )

                OutlinedTextField(
                    value = listingState.contractAddress,
                    onValueChange = onAddressChange,
                    label = { Text("Token Contract Address (0x...)") },
                    placeholder = { Text("e.g. 0x337610d27c682E347C9cD60BE4b3b107C9d34dDd") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // Paste sample BSC testnet or test address
                                onAddressChange("0x337610d27c682E347C9cD60BE4b3b107C9d34dDd")
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Demo", tint = BnbGold)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_contract_address_input")
                )

                Button(
                    onClick = onInspectContract,
                    enabled = listingState.contractAddress.isNotBlank() && !listingState.isInspecting,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_inspect_button")
                ) {
                    if (listingState.isInspecting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Querying Blockchain RPC...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inspect Token on Chain", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // STEP 2: Detected Token Information Card
        AnimatedVisibility(visible = listingState.detectedInfo != null) {
            listingState.detectedInfo?.let { info ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (info.isVerifiedContract) NeonCyan else BnbGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2. Detected Token Metadata",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (info.isVerifiedContract) CryptoGreenBg else BnbGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (info.isVerifiedContract) "Bytecode Verified" else "EVM Compatible",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (info.isVerifiedContract) CryptoGreen else BnbGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Token Name", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(info.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Symbol", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(info.symbol, fontWeight = FontWeight.Bold, color = BnbGold)
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Decimals", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${info.decimals}", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Supply", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(info.totalSupply, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        // Initial Reference Price
                        OutlinedTextField(
                            value = listingState.initialPriceUsd,
                            onValueChange = onInitialPriceChange,
                            label = { Text("Initial USD Price Reference") },
                            leadingIcon = { Text("$", color = TextSecondary, modifier = Modifier.padding(start = 12.dp)) },
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
            }
        }

        // STEP 3: Automated On-Chain Listing Fee Collection
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, BnbGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = BnbGold)
                    Text(
                        text = "3. On-Chain Listing Fee Collection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Every user listing a token pays the fixed DEX listing fee. The smart contract automatically transfers 100% of this fee directly to the DEX Owner's official wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Required Fee Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Listing Fee Amount:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                text = "${listingState.requiredFeeUsdt} USDT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = BnbGold
                            )
                        }

                        HorizontalDivider(color = DarkSurfaceBorder)

                        Text("Official Fee Receiver Wallet:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = listingState.feeReceiverAddress,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(listingState.feeReceiverAddress)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = "Smart Contract: TataSwapListingFee.sol (Non-custodial reentrancy guarded)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoGreen,
                            fontSize = 10.sp
                        )
                    }
                }

                // Error message if any
                listingState.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CryptoRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Submit Listing Button
                Button(
                    onClick = {
                        if (connectedWallet == null) onConnectWallet()
                        else onSubmitListing()
                    },
                    enabled = connectedWallet == null || (listingState.detectedInfo != null && !listingState.isSubmitting),
                    colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_token_listing_button")
                ) {
                    if (listingState.isSubmitting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Collecting Fee & Registering...")
                    } else if (connectedWallet == null) {
                        Text("Connect Wallet to List Token", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay 5 USDT & Register Token", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Listing Success Confirmation Card
        AnimatedVisibility(visible = listingState.listingSuccessTx != null) {
            listingState.listingSuccessTx?.let { tx ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = CryptoGreen)
                            Text("Token Successfully Listed on TataSwap!", fontWeight = FontWeight.Bold, color = CryptoGreen)
                        }
                        Text(tx.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(tx.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("Tx Hash: ${tx.txHash}", style = MaterialTheme.typography.labelSmall, color = BnbGold)

                        Button(
                            onClick = onViewExplorer,
                            colors = ButtonDefaults.buttonColors(containerColor = CryptoGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View in Token Explorer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(step: String, label: String, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDone) CryptoGreen else DarkSurfaceBorder),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            } else {
                Text(step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDone) CryptoGreen else TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun StepDivider() {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(DarkSurfaceBorder)
    )
}
