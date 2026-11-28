package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainNetwork
import com.example.data.model.SupportedNetworks
import com.example.data.model.Token
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexTopAppBar(
    currentNetwork: BlockchainNetwork,
    connectedWallet: String?,
    nativeBalance: Double,
    onNetworkClick: () -> Unit,
    onWalletClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = TextPrimary
        ),
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = TextPrimary
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BnbGold, NeonCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = "TataSwap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "DEX v3.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = BnbGold
                    )
                }
            }
        },
        actions = {
            // Network Pill
            Surface(
                onClick = onNetworkClick,
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag("network_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (currentNetwork.isTestnet) BnbGold else CryptoGreen)
                    )
                    Text(
                        text = if (currentNetwork.id.startsWith("bsc")) "BNB" else currentNetwork.symbol,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Network",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Wallet Pill
            Surface(
                onClick = onWalletClick,
                shape = RoundedCornerShape(20.dp),
                color = if (connectedWallet != null) DarkSurfaceElevated else BnbGold,
                border = if (connectedWallet != null) androidx.compose.foundation.BorderStroke(1.dp, BnbGold.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("wallet_connect_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (connectedWallet != null) {
                        Text(
                            text = "${"%.2f".format(nativeBalance)} ${currentNetwork.symbol.take(4)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BnbGold,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(DarkSurfaceBorder)
                        )
                        Text(
                            text = "${connectedWallet.take(4)}...${connectedWallet.takeLast(3)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Connect",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun NetworkSelectorDialog(
    currentNetwork: BlockchainNetwork,
    onSelectNetwork: (BlockchainNetwork) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = BnbGold)
                Text("Select Blockchain Network", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SupportedNetworks) { net ->
                    val isSelected = net.id == currentNetwork.id
                    Surface(
                        onClick = { onSelectNetwork(net) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BnbGold else DarkSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (net.isTestnet) BnbGold else CryptoGreen)
                                )
                                Column {
                                    Text(
                                        text = net.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Chain ID: ${net.chainId} • Mainnet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = BnbGold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun WalletConnectDialog(
    connectedAddress: String?,
    nativeBalance: Double,
    networkSymbol: String,
    onConnectCustom: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputAddress by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf<String?>(null) }
    var showMoreWallets by remember { mutableStateOf(false) }
    var forcePicker by remember { mutableStateOf(false) }
    var copiedAddress by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val targetDexUrl = "https://tataswap.onrender.com/dex.html?autoConnect=1"
    val encodedDexUrl = remember { java.net.URLEncoder.encode(targetDexUrl, "UTF-8") }
    val encodedOrigin = remember { java.net.URLEncoder.encode("https://tataswap.onrender.com", "UTF-8") }

    fun openExternalUrl(url: String, fallbackUrl: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            if (fallbackUrl != null) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                } catch (_: Exception) {
                }
            }
        }
    }

    val showConnectedSummary = connectedAddress != null && !forcePicker

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BnbGold)
                Text(
                    text = if (showConnectedSummary) "Connected Wallet" else "Connect a wallet",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showConnectedSummary && connectedAddress != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Connected Address", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = connectedAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(connectedAddress))
                                        copiedAddress = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (copiedAddress) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = if (copiedAddress) "Copied" else "Copy",
                                        tint = if (copiedAddress) CryptoGreen else BnbGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Native Balance: ${"%.4f".format(nativeBalance)} $networkSymbol", style = MaterialTheme.typography.bodySmall, color = CryptoGreen)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openExternalUrl("https://metamask.app.link/dapp/tataswap.onrender.com/dex.html?autoConnect=1") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MetaMask", fontSize = 11.sp, color = BnbGold)
                        }
                        OutlinedButton(
                            onClick = { openExternalUrl("https://link.trustwallet.com/open_url?coin_id=60&url=$encodedDexUrl") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Trust", fontSize = 11.sp, color = NeonCyan)
                        }
                        OutlinedButton(
                            onClick = { openExternalUrl(targetDexUrl) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Chrome", fontSize = 11.sp, color = CryptoGreen)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { forcePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_wallet_button")
                        ) {
                            Text("Switch Wallet", fontSize = 12.sp, color = NeonCyan)
                        }
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = CryptoRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("disconnect_wallet_button")
                        ) {
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                } else {
                    if (connectedAddress != null) {
                        TextButton(
                            onClick = { forcePicker = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Back to Connected Wallet", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    // Open in Google Chrome banner
                    Surface(
                        onClick = { openExternalUrl(targetDexUrl) },
                        shape = RoundedCornerShape(12.dp),
                        color = CryptoGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Open in Google Chrome", fontWeight = FontWeight.Bold, color = CryptoGreen, fontSize = 13.sp)
                                Text("Auto-connects MetaMask, Trust, OKX, Phantom & WalletConnect QR", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Google Chrome", tint = CryptoGreen, modifier = Modifier.size(20.dp))
                        }
                    }

                    WalletOptionRow(
                        emoji = "🦊",
                        badgeColor = Color(0xFFF6851B),
                        borderColor = Color(0xFFF6851B).copy(alpha = 0.6f),
                        title = "MetaMask",
                        subtitle = "Browser extension or MetaMask Mobile DApp Browser",
                        actionColor = BnbGold,
                        onClick = {
                            openExternalUrl("https://metamask.app.link/dapp/tataswap.onrender.com/dex.html?autoConnect=1")
                        }
                    )

                    WalletOptionRow(
                        emoji = "🛡️",
                        badgeColor = Color(0xFF0500FF),
                        borderColor = NeonCyan.copy(alpha = 0.5f),
                        title = "Trust Wallet Mobile & Extension",
                        subtitle = "Tap to open in Trust Wallet DApp Browser",
                        actionColor = NeonCyan,
                        onClick = {
                            openExternalUrl("https://link.trustwallet.com/open_url?coin_id=60&url=$encodedDexUrl")
                        }
                    )

                    WalletOptionRow(
                        emoji = "🔵",
                        badgeColor = Color(0xFF0052FF),
                        borderColor = Color(0xFF0052FF).copy(alpha = 0.55f),
                        title = "Coinbase Wallet",
                        subtitle = "Tap to open in Coinbase Wallet App",
                        actionColor = NeonCyan,
                        onClick = {
                            openExternalUrl("https://go.cb-w.com/dapp?cb_url=$encodedDexUrl")
                        }
                    )

                    WalletOptionRow(
                        emoji = "🟡",
                        badgeColor = BnbGold,
                        borderColor = BnbGold.copy(alpha = 0.55f),
                        title = "Binance Wallet",
                        subtitle = "Browser extension or WalletConnect pairing",
                        actionColor = BnbGold,
                        onClick = {
                            openExternalUrl(
                                targetDexUrl,
                                "https://www.binance.com/en/web3wallet"
                            )
                        }
                    )

                    WalletOptionRow(
                        emoji = "🔗",
                        badgeColor = Color(0xFF3B99FC),
                        borderColor = Color(0xFF3B99FC).copy(alpha = 0.55f),
                        title = "WalletConnect & More Wallets",
                        subtitle = if (showMoreWallets) "Hide OKX, Bitget, Phantom & SafePal" else "ID: 58ad2600...28d50 • OKX, Bitget, Phantom, SafePal",
                        actionColor = Color(0xFF3B99FC),
                        actionLabel = if (showMoreWallets) "Hide" else "Select",
                        onClick = { showMoreWallets = !showMoreWallets }
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WalletConnect Project ID", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = com.example.data.repository.DexRepository.WALLETCONNECT_PROJECT_ID,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(com.example.data.repository.DexRepository.WALLETCONNECT_PROJECT_ID))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Project ID", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (showMoreWallets) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            WalletOptionRow(
                                emoji = "⚫",
                                badgeColor = Color(0xFF1F2937),
                                borderColor = DarkSurfaceBorder,
                                title = "OKX Wallet",
                                subtitle = "Tap to open in OKX Web3 DApp Browser",
                                actionColor = NeonCyan,
                                onClick = {
                                    val okxInner = java.net.URLEncoder.encode("okx://wallet/dapp/details?dappUrl=$encodedDexUrl", "UTF-8")
                                    openExternalUrl("https://www.okx.com/download?deeplink=$okxInner")
                                }
                            )
                            WalletOptionRow(
                                emoji = "💠",
                                badgeColor = Color(0xFF00F0FF),
                                borderColor = DarkSurfaceBorder,
                                title = "Bitget Wallet",
                                subtitle = "Tap to open in Bitget DApp Browser",
                                actionColor = NeonCyan,
                                onClick = {
                                    openExternalUrl("https://bkcode.vip?action=dapp&url=$encodedDexUrl")
                                }
                            )
                            WalletOptionRow(
                                emoji = "👻",
                                badgeColor = Color(0xFF5346E0),
                                borderColor = DarkSurfaceBorder,
                                title = "Phantom",
                                subtitle = "Browser extension or Phantom Mobile Browser",
                                actionColor = NeonCyan,
                                onClick = {
                                    openExternalUrl("https://phantom.app/ul/browse/$encodedDexUrl?ref=$encodedOrigin")
                                }
                            )
                            WalletOptionRow(
                                emoji = "🔐",
                                badgeColor = Color(0xFF4A40FF),
                                borderColor = DarkSurfaceBorder,
                                title = "SafePal Wallet",
                                subtitle = "Tap to open in SafePal App",
                                actionColor = NeonCyan,
                                onClick = {
                                    openExternalUrl("https://link.safepal.io/dapp?url=$encodedDexUrl")
                                }
                            )
                            WalletOptionRow(
                                emoji = "🌈",
                                badgeColor = Color(0xFF001E59),
                                borderColor = DarkSurfaceBorder,
                                title = "Rainbow Wallet",
                                subtitle = "Tap to launch Rainbow Web3 App",
                                actionColor = NeonCyan,
                                onClick = {
                                    openExternalUrl("https://rnbwapp.com")
                                }
                            )
                        }
                    }

                    // App Owner Wallet Option (0 Listing Fee)
                    Surface(
                        onClick = {
                            forcePicker = false
                            onConnectCustom("0x3f48248FFE235a5205d1D781fB6c852c71fc1556")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = CryptoGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoGreen.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_wallet_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CryptoGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = "Owner", tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("App Owner Wallet (0 Fee)", fontWeight = FontWeight.Bold, color = CryptoGreen, fontSize = 13.sp)
                                Text("0x3f48...1556 • Free Token Listing", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    // Watch-Only / Direct address input
                    OutlinedTextField(
                        value = inputAddress,
                        onValueChange = {
                            inputAddress = it
                            addressError = null
                        },
                        label = { Text("Watch-Only / Paste 0x EVM Address") },
                        placeholder = { Text("0x...") },
                        singleLine = true,
                        isError = addressError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BnbGold,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_address_input")
                    )

                    if (addressError != null) {
                        Text(
                            text = addressError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoRed
                        )
                    }

                    Button(
                        onClick = {
                            val clean = inputAddress.trim()
                            val isValidEvm = clean.startsWith("0x") && clean.length == 42
                            if (isValidEvm) {
                                forcePicker = false
                                onConnectCustom(clean)
                            } else {
                                addressError = "Enter a valid 42-char 0x EVM wallet address."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_custom_wallet_button")
                    ) {
                        Text("Connect & Sync Live Balance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun WalletOptionRow(
    emoji: String,
    badgeColor: Color,
    borderColor: Color,
    title: String,
    subtitle: String,
    actionColor: Color,
    actionLabel: String = "Connect",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = actionColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun TokenSelectorModal(
    tokens: List<Token>,
    onSelectToken: (Token) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, tokens) {
        if (searchQuery.isBlank()) tokens
        else tokens.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Select a Token", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or paste 0x address") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick tokens row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickSymbols = listOf("BNB", "USDT", "TATA", "CAKE")
                    tokens.filter { it.symbol in quickSymbols }.forEach { token ->
                        Surface(
                            onClick = { onSelectToken(token) },
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                        ) {
                            Text(
                                text = token.symbol,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = DividerDark)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered) { token ->
                        Surface(
                            onClick = { onSelectToken(token) },
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (token.symbol) {
                                                    "BNB" -> BnbGold
                                                    "USDT" -> Color(0xFF26A17B)
                                                    "TATA" -> NeonCyan
                                                    "CAKE" -> Color(0xFFD1884F)
                                                    "BTCB" -> Color(0xFFF7931A)
                                                    "ETH" -> Color(0xFF627EEA)
                                                    else -> DarkSurfaceElevated
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = token.symbol.take(3),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(token.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            if (token.isVerified) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Surface(
                                                    color = CryptoRedBg,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "Community",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CryptoRed,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(token.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${"%.4f".format(token.userBalance)} ${token.symbol}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$${"%.2f".format(token.priceUsd)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SlippageSettingsDialog(
    currentSlippage: Double,
    onSelectSlippage: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var customSlippage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = BnbGold)
                Text("Transaction Settings", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Slippage Tolerance", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    "Your transaction will revert if the price changes unfavorably by more than this percentage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(0.1, 0.5, 1.0)
                    presets.forEach { slip ->
                        val isSelected = currentSlippage == slip
                        Surface(
                            onClick = { onSelectSlippage(slip) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BnbGold else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BnbGold else DarkSurfaceBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "$slip%",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customSlippage,
                    onValueChange = { customSlippage = it },
                    placeholder = { Text("Custom %") },
                    trailingIcon = { Text("%", color = TextSecondary, modifier = Modifier.padding(end = 12.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BnbGold,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val parsed = customSlippage.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            onSelectSlippage(parsed)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BnbGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Slippage", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun PriceChartPreview(
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path()
        val fillPath = Path()

        // Generate smooth wave pattern
        val points = listOf(0.7f, 0.65f, 0.68f, 0.55f, 0.58f, 0.42f, 0.48f, 0.35f, 0.28f, 0.20f)
        val stepX = width / (points.size - 1)

        path.moveTo(0f, points[0] * height)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, points[0] * height)

        for (i in 1 until points.size) {
            val x = i * stepX
            val y = (if (isPositive) points[i] else (1.0f - points[i])) * height
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        val strokeColor = if (isPositive) CryptoGreen else CryptoRed
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(strokeColor.copy(alpha = 0.35f), Color.Transparent),
            startY = 0f,
            endY = height
        )

        drawPath(fillPath, brush = gradientBrush)
        drawPath(path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))
    }
}
