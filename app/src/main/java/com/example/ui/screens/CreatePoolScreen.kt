package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainNetwork
import com.example.data.model.LiquidityPool
import com.example.data.model.Token
import com.example.ui.components.TokenSelectorModal
import com.example.ui.theme.*
import kotlin.math.max

private data class FeeTierOption(
    val percent: Double,
    val label: String,
    val description: String
)

private val DefaultFeeTiers = listOf(
    FeeTierOption(0.0075, "0.0075%", "Commonly used for very stable pairs."),
    FeeTierOption(0.0375, "0.0375%", "Commonly used for stable pairs."),
    FeeTierOption(0.25, "0.25%", "Commonly used for most pairs."),
    FeeTierOption(0.9, "0.9%", "Commonly used for exotic pairs.")
)

private data class HookPreset(
    val title: String,
    val subtitle: String,
    val address: String
)

private val HookPresets = listOf(
    HookPreset("Dynamic Volatility Fee Hook", "Adjusts swap fee dynamically on volatile pairs", "0x000000000000000000000000000000000000H001"),
    HookPreset("TWAMM Time-Weighted Hook", "Long-duration streaming order execution hook", "0x000000000000000000000000000000000000H002"),
    HookPreset("On-Chain Limit Order Hook", "Automated tick-crossing limit fill hook", "0x000000000000000000000000000000000000H003")
)

@Composable
fun CreatePoolScreen(
    allTokens: List<Token>,
    existingPools: List<LiquidityPool>,
    currentNetwork: BlockchainNetwork,
    connectedWallet: String?,
    onOpenWalletModal: () -> Unit,
    onCreatePool: (
        tokenA: Token,
        tokenB: Token,
        feeTier: Double,
        amountA: Double,
        amountB: Double,
        hookAddress: String?,
        initialPrice: Double?,
        isFullRange: Boolean,
        minPrice: Double?,
        maxPrice: Double?
    ) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var tokenA by remember { mutableStateOf<Token?>(null) }
    var tokenB by remember { mutableStateOf<Token?>(null) }
    var showTokenAModal by remember { mutableStateOf(false) }
    var showTokenBModal by remember { mutableStateOf(false) }

    // Hook state
    var showHookPanel by remember { mutableStateOf(false) }
    var hookInput by remember { mutableStateOf("") }
    var activeHookAddress by remember { mutableStateOf<String?>(null) }

    // Fee Tier state
    var selectedFeeTier by remember { mutableDoubleStateOf(0.25) }
    var feeGridExpanded by remember { mutableStateOf(true) }
    var showCustomFeeInput by remember { mutableStateOf(false) }
    var customFeeInput by remember { mutableStateOf("") }

    // Step 2: Price, Range & Deposit
    var priceQuotedInB by remember { mutableStateOf(true) } // true => TokenB per 1 TokenA
    var initialPriceStr by remember { mutableStateOf("0") }
    var isFullRange by remember { mutableStateOf(true) }
    var minPriceStr by remember { mutableStateOf("0.0") }
    var maxPriceStr by remember { mutableStateOf("∞") }

    var amountAStr by remember { mutableStateOf("0") }
    var amountBStr by remember { mutableStateOf("0") }
    var poolsRefreshTick by remember { mutableIntStateOf(0) }

    BackHandler(enabled = step == 2) {
        step = 1
    }

    val networkPools = remember(existingPools, currentNetwork.id, poolsRefreshTick) {
        existingPools.filter { it.networkId == currentNetwork.id }
    }

    val isPoolAlreadyCreated: (Double) -> Boolean = { tier ->
        val a = tokenA
        val b = tokenB
        if (a == null || b == null) false
        else existingPools.any { p ->
            ((p.tokenA.symbol == a.symbol && p.tokenB.symbol == b.symbol) ||
                (p.tokenA.symbol == b.symbol && p.tokenB.symbol == a.symbol)) &&
                kotlin.math.abs(p.feeTierPercent - tier) < 0.0001
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Breadcrumb + Header
        Text(
            text = "Your positions  ›  Create Pool",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (step == 2) step = 1 else onBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Create Pool",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Main Step Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step Indicator Header
                Row(
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
                        Text(
                            text = step.toString(),
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Step $step of 2",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = if (step == 1) "Select token pair and fees" else "Set initial price and deposit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                HorizontalDivider(color = DarkSurfaceBorder)

                if (step == 1) {
                    // STEP 1: SELECT PAIR
                    Text(
                        text = "Select pair",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose the tokens you want to provide liquidity for. You can select tokens on all supported networks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Token A Selector
                    Surface(
                        onClick = { showTokenAModal = true },
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("create_pool_token_a_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (tokenA == null) CryptoGreenBg else BnbGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tokenA?.symbol?.take(2) ?: "?",
                                        color = if (tokenA == null) CryptoGreen else BnbGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = tokenA?.let { "${it.symbol} • ${it.name}" } ?: "Choose token",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select first token", tint = TextSecondary)
                        }
                    }

                    // Token B Selector (Green accent matching screenshot)
                    val tokenBSelected = tokenB != null
                    Surface(
                        onClick = { showTokenBModal = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (tokenBSelected) DarkSurfaceElevated else CryptoGreen,
                        border = if (tokenBSelected) BorderStroke(1.dp, CryptoGreen) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("create_pool_token_b_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (tokenBSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(CryptoGreenBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tokenB!!.symbol.take(2),
                                            color = CryptoGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Text(
                                    text = tokenB?.let { "${it.symbol} • ${it.name}" } ?: "Choose token",
                                    color = if (tokenBSelected) TextPrimary else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select second token",
                                tint = if (tokenBSelected) TextSecondary else Color.Black
                            )
                        }
                    }

                    // Add a hook (optional)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, if (activeHookAddress != null) CryptoGreen else DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Add a hook",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "(optional)",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (activeHookAddress != null) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = CryptoGreenBg) {
                                                Text(
                                                    text = "Hooked",
                                                    color = CryptoGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = activeHookAddress ?: "Hooks allow for advanced position customization.",
                                        color = if (activeHookAddress != null) CryptoGreen else TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showHookPanel = !showHookPanel },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DarkSurfaceBorder),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("create_pool_hook_search_btn")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Search", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            AnimatedVisibility(visible = showHookPanel) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = hookInput,
                                        onValueChange = { hookInput = it },
                                        label = { Text("Paste 0x Hook Contract Address") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CryptoGreen,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HookPresets.forEach { preset ->
                                        Surface(
                                            onClick = {
                                                hookInput = preset.address
                                                activeHookAddress = preset.address
                                                showHookPanel = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(preset.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text(preset.subtitle, color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            hookInput = ""
                                            activeHookAddress = null
                                            showHookPanel = false
                                        }) {
                                            Text("Clear Hook", color = CryptoRed)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Button(
                                            onClick = {
                                                activeHookAddress = hookInput.trim().takeIf { it.isNotEmpty() }
                                                showHookPanel = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CryptoGreen, contentColor = Color.Black)
                                        ) {
                                            Text("Apply Hook", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // FEE TIER
                    Text(
                        text = "Fee tier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "The amount earned providing liquidity. Choose an amount that suits your risk tolerance and strategy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Fee Tier Summary / Less-More Toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${selectedFeeTier}% fee tier",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "The % you will earn in fees",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(
                                onClick = { feeGridExpanded = !feeGridExpanded },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkSurfaceBorder),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    text = if (feeGridExpanded) "Less" else "More",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (feeGridExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle fee tiers",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 2x2 Grid of Fee Tiers
                    AnimatedVisibility(visible = feeGridExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DefaultFeeTiers.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { option ->
                                        val isSelected = kotlin.math.abs(selectedFeeTier - option.percent) < 0.0001
                                        val created = isPoolAlreadyCreated(option.percent)
                                        Surface(
                                            onClick = { selectedFeeTier = option.percent },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) CryptoGreenBg else DarkBackground,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) CryptoGreen else DarkSurfaceBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 124.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = option.label,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                                .background(CryptoGreen),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = option.description,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = if (created) "Created" else "Not created",
                                                    color = if (created) CryptoGreen else TextTertiary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Advanced custom fee tier toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showCustomFeeInput = !showCustomFeeInput }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Search or create other fee tiers (Advanced)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            AnimatedVisibility(visible = showCustomFeeInput) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customFeeInput,
                                        onValueChange = { customFeeInput = it },
                                        label = { Text("Custom fee % (e.g. 0.05, 0.3, 1.0)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CryptoGreen,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val parsed = customFeeInput.toDoubleOrNull()
                                            if (parsed != null && parsed in 0.0001..10.0) {
                                                selectedFeeTier = parsed
                                                showCustomFeeInput = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CryptoGreen, contentColor = Color.Black),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("Apply", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Creating New Pool Info Notice
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Creating new pool", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "Your selections will create a new liquidity pool which may result in lower initial liquidity and increased volatility. Consider adding to an existing pool to minimize these risks.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    // Continue CTA
                    val pairValid = tokenA != null && tokenB != null && tokenA!!.symbol != tokenB!!.symbol
                    Button(
                        onClick = {
                            if (pairValid) {
                                // Pre-fill suggested live price if both tokens have USD prices
                                val a = tokenA!!
                                val b = tokenB!!
                                if (a.priceUsd > 0 && b.priceUsd > 0) {
                                    val ratio = if (priceQuotedInB) a.priceUsd / b.priceUsd else b.priceUsd / a.priceUsd
                                    initialPriceStr = "%.4f".format(ratio)
                                    if (!isFullRange) {
                                        minPriceStr = "%.4f".format(ratio * 0.8)
                                        maxPriceStr = "%.4f".format(ratio * 1.2)
                                    }
                                }
                                step = 2
                            }
                        },
                        enabled = pairValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CryptoGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = DarkSurfaceElevated,
                            disabledContentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_pool_continue_button")
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // STEP 2: INITIAL PRICE, PRICE RANGE & DEPOSIT
                    val tokA = tokenA ?: allTokens.first()
                    val tokB = tokenB ?: allTokens.last()
                    val baseTok = if (priceQuotedInB) tokA else tokB
                    val quoteTok = if (priceQuotedInB) tokB else tokA

                    // Pair Summary Bar + Edit
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${tokA.symbol} / ${tokB.symbol}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(shape = RoundedCornerShape(8.dp), color = DarkSurfaceElevated) {
                                    Text(
                                        text = "$selectedFeeTier%",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                if (activeHookAddress != null) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = CryptoGreenBg) {
                                        Text(
                                            text = "Hooked",
                                            color = CryptoGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = { step = 1 },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Edit", color = CryptoGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Set Initial Price
                    Text("Set initial price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "When creating a new pool, you must set the starting exchange rate for both tokens. This rate will reflect the initial market price.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Initial price", color = TextSecondary, fontSize = 13.sp)
                                // Token Toggle Pill
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                                ) {
                                    Row(modifier = Modifier.padding(3.dp)) {
                                        Surface(
                                            onClick = { priceQuotedInB = true },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (priceQuotedInB) CryptoGreen else Color.Transparent
                                        ) {
                                            Text(
                                                text = tokA.symbol,
                                                color = if (priceQuotedInB) Color.Black else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                        Surface(
                                            onClick = { priceQuotedInB = false },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (!priceQuotedInB) CryptoGreen else Color.Transparent
                                        ) {
                                            Text(
                                                text = tokB.symbol,
                                                color = if (!priceQuotedInB) Color.Black else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = initialPriceStr,
                                onValueChange = { initialPriceStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CryptoGreen,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${quoteTok.symbol} = 1 ${baseTok.symbol}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (tokA.priceUsd > 0 && tokB.priceUsd > 0) {
                                    TextButton(onClick = {
                                        val liveRate = if (priceQuotedInB) tokA.priceUsd / tokB.priceUsd else tokB.priceUsd / tokA.priceUsd
                                        initialPriceStr = "%.4f".format(liveRate)
                                    }) {
                                        Text("Use Market Rate", color = CryptoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Market Price Warning Banner
                    val hasBothPrices = tokA.priceUsd > 0 && tokB.priceUsd > 0 && !tokA.isCustom && !tokB.isCustom
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasBothPrices) DarkBackground else WarningAmberBg,
                        border = BorderStroke(1.dp, if (hasBothPrices) DarkSurfaceBorder else WarningAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (hasBothPrices) {
                                    val liveRatio = if (priceQuotedInB) tokA.priceUsd / tokB.priceUsd else tokB.priceUsd / tokA.priceUsd
                                    "Live market reference: 1 ${baseTok.symbol} ≈ ${"%.4f".format(liveRatio)} ${quoteTok.symbol}. Verify initial rate before deploying."
                                } else {
                                    "Market price not found. Please do your own research to avoid loss of funds."
                                },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Set Price Range
                    Text("Set price range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Surface(
                                onClick = { isFullRange = true },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isFullRange) DarkSurfaceElevated else Color.Transparent,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Full range",
                                        color = if (isFullRange) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Surface(
                                onClick = { isFullRange = false },
                                shape = RoundedCornerShape(20.dp),
                                color = if (!isFullRange) DarkSurfaceElevated else Color.Transparent,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Custom range",
                                        color = if (!isFullRange) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !isFullRange) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Min Price Card
                            PriceBoundStepperCard(
                                label = "Min price",
                                value = minPriceStr,
                                unitLabel = "${quoteTok.symbol} per ${baseTok.symbol}",
                                onValueChange = { minPriceStr = it },
                                onDecrement = {
                                    val v = max(0.0, (minPriceStr.toDoubleOrNull() ?: 0.0) * 0.9)
                                    minPriceStr = "%.4f".format(v)
                                },
                                onIncrement = {
                                    val base = initialPriceStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                                    val cur = minPriceStr.toDoubleOrNull() ?: 0.0
                                    val next = if (cur <= 0.0) base * 0.5 else cur * 1.1
                                    minPriceStr = "%.4f".format(next)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // Max Price Card
                            PriceBoundStepperCard(
                                label = "Max price",
                                value = maxPriceStr,
                                unitLabel = "${quoteTok.symbol} per ${baseTok.symbol}",
                                onValueChange = { maxPriceStr = it },
                                onDecrement = {
                                    val base = initialPriceStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                                    val cur = maxPriceStr.toDoubleOrNull() ?: (base * 2.0)
                                    maxPriceStr = "%.4f".format(max(0.0001, cur * 0.9))
                                },
                                onIncrement = {
                                    val base = initialPriceStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                                    val cur = maxPriceStr.toDoubleOrNull() ?: base
                                    maxPriceStr = "%.4f".format(cur * 1.1)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = if (isFullRange) {
                            "Setting full range liquidity when creating a pool ensures continuous market participation across all possible prices, offering simplicity but with potential for higher impermanent loss."
                        } else {
                            "Custom range concentrates liquidity between your chosen min and max prices for higher capital efficiency."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    HorizontalDivider(color = DarkSurfaceBorder)

                    // Deposit Tokens
                    Text("Deposit tokens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "Specify the token amounts for your liquidity contribution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    DepositTokenCard(
                        token = tokA,
                        amountStr = amountAStr,
                        onAmountChange = { input ->
                            amountAStr = input
                            val aNum = input.toDoubleOrNull()
                            val pNum = initialPriceStr.toDoubleOrNull()
                            if (aNum != null && aNum > 0 && pNum != null && pNum > 0) {
                                val bCalc = if (priceQuotedInB) aNum * pNum else aNum / pNum
                                amountBStr = "%.4f".format(bCalc)
                            }
                        },
                        onPercentClick = { pct ->
                            val bal = tokA.userBalance
                            val valNum = bal * (pct / 100.0)
                            amountAStr = "%.4f".format(valNum)
                            val pNum = initialPriceStr.toDoubleOrNull()
                            if (valNum > 0 && pNum != null && pNum > 0) {
                                val bCalc = if (priceQuotedInB) valNum * pNum else valNum / pNum
                                amountBStr = "%.4f".format(bCalc)
                            }
                        }
                    )

                    DepositTokenCard(
                        token = tokB,
                        amountStr = amountBStr,
                        onAmountChange = { amountBStr = it },
                        onPercentClick = { pct ->
                            val bal = tokB.userBalance
                            amountBStr = "%.4f".format(bal * (pct / 100.0))
                        }
                    )

                    // Primary Submit / Connect Wallet CTA
                    Button(
                        onClick = {
                            if (connectedWallet.isNullOrBlank()) {
                                onOpenWalletModal()
                            } else {
                                val a = amountAStr.toDoubleOrNull() ?: 0.0
                                val b = amountBStr.toDoubleOrNull() ?: 0.0
                                val initP = initialPriceStr.toDoubleOrNull()?.takeIf { it > 0 }
                                val minP = if (isFullRange) null else minPriceStr.toDoubleOrNull()
                                val maxP = if (isFullRange) null else maxPriceStr.toDoubleOrNull()
                                onCreatePool(
                                    tokA,
                                    tokB,
                                    selectedFeeTier,
                                    if (a > 0) a else 1.0,
                                    if (b > 0) b else 1.0,
                                    activeHookAddress,
                                    initP,
                                    isFullRange,
                                    minP,
                                    maxP
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_pool_submit_button")
                    ) {
                        Text(
                            text = if (connectedWallet.isNullOrBlank()) "Connect Wallet" else "Create Pool & Deposit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // On-Chain Liquidity Pools Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "On-Chain Liquidity Pools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Real reserves synced from ${currentNetwork.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { poolsRefreshTick++ },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                if (networkPools.isEmpty()) {
                    Text(
                        text = "No pools deployed on this network yet. No simulated liquidity is displayed—create the first trading pair to seed real reserves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    networkPools.forEach { pool ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${pool.tokenA.symbol} / ${pool.tokenB.symbol}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Fee ${pool.feeTierPercent}% • ${if (pool.isFullRange) "Full Range" else "Custom Range"}${if (!pool.hookAddress.isNullOrBlank()) " • Hooked" else ""}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "$${"%,.2f".format(pool.tvlUsd)} TVL",
                                    color = CryptoGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTokenAModal) {
        TokenSelectorModal(
            tokens = allTokens,
            onSelectToken = {
                tokenA = it
                showTokenAModal = false
            },
            onDismiss = { showTokenAModal = false }
        )
    }
    if (showTokenBModal) {
        TokenSelectorModal(
            tokens = allTokens,
            onSelectToken = {
                tokenB = it
                showTokenBModal = false
            },
            onDismiss = { showTokenBModal = false }
        )
    }
}

@Composable
private fun PriceBoundStepperCard(
    label: String,
    value: String,
    unitLabel: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkBackground,
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onDecrement,
                    shape = CircleShape,
                    color = DarkSurfaceElevated,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("−", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = onIncrement,
                    shape = CircleShape,
                    color = DarkSurfaceElevated,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Text(unitLabel, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DepositTokenCard(
    token: Token,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    onPercentClick: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkBackground,
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = onAmountChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(CryptoGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = token.symbol.take(2),
                                color = CryptoGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(token.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(25 to "25%", 50 to "50%", 75 to "75%", 100 to "Max").forEach { (pct, label) ->
                        Surface(
                            onClick = { onPercentClick(pct) },
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkSurfaceBorder)
                        ) {
                            Text(
                                text = label,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${"%.4f".format(token.userBalance)} ${token.symbol}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
