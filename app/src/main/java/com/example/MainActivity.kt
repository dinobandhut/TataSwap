package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DexTopAppBar
import com.example.ui.components.NetworkSelectorDialog
import com.example.ui.components.WalletConnectDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DexScreen
import com.example.ui.viewmodel.DexViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TataSwapApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TataSwapApp(viewModel: DexViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentNetwork by viewModel.currentNetwork.collectAsStateWithLifecycle()
    val connectedWallet by viewModel.connectedWallet.collectAsStateWithLifecycle()
    val nativeBalance by viewModel.walletNativeBalance.collectAsStateWithLifecycle()
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val pools by viewModel.pools.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val protocolStats by viewModel.protocolStats.collectAsStateWithLifecycle()
    val swapState by viewModel.swapState.collectAsStateWithLifecycle()
    val listingState by viewModel.listingState.collectAsStateWithLifecycle()
    val selectedToken by viewModel.selectedToken.collectAsStateWithLifecycle()
    val selectedPool by viewModel.selectedPool.collectAsStateWithLifecycle()
    val showWalletModal by viewModel.showWalletModal.collectAsStateWithLifecycle()
    val showNetworkModal by viewModel.showNetworkModal.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe snackbars
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // BackHandler per guidelines
    BackHandler(enabled = currentScreen != DexScreen.HOME) {
        viewModel.navigateBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BnbGold,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("T", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.Black)
                            }
                        }
                        Column {
                            Text("TataSwap DEX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Uniswap V3 on BSC", style = MaterialTheme.typography.labelSmall, color = BnbGold)
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    // Navigation Items
                    val navItems = listOf(
                        Triple(DexScreen.HOME, "Home Dashboard", Icons.Default.Home),
                        Triple(DexScreen.SWAP, "Swap Tokens", Icons.Default.SwapHoriz),
                        Triple(DexScreen.EXPLORE_TOKENS, "Explore Tokens", Icons.Default.Search),
                        Triple(DexScreen.CREATE_LISTING, "List Token (5 USDT)", Icons.Default.AddBusiness),
                        Triple(DexScreen.CREATE_POOL, "Create Liquidity Pool", Icons.Default.WaterDrop),
                        Triple(DexScreen.ADD_LIQUIDITY, "Add Liquidity", Icons.Default.AddCircle),
                        Triple(DexScreen.REMOVE_LIQUIDITY, "Remove Liquidity", Icons.Default.RemoveCircle),
                        Triple(DexScreen.PORTFOLIO, "My Portfolio", Icons.Default.AccountBalanceWallet),
                        Triple(DexScreen.TRANSACTION_HISTORY, "Transaction History", Icons.Default.History),
                        Triple(DexScreen.ANALYTICS, "Analytics & Volume", Icons.Default.BarChart),
                        Triple(DexScreen.ADMIN_DASHBOARD, "Admin Dashboard", Icons.Default.AdminPanelSettings),
                        Triple(DexScreen.WEB_DEX_VIEW, "Web DEX View", Icons.Default.Language)
                    )

                    navItems.forEach { (screen, title, icon) ->
                        val isSel = currentScreen == screen
                        NavigationDrawerItem(
                            label = { Text(title, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            selected = isSel,
                            onClick = {
                                viewModel.navigateTo(screen)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(icon, contentDescription = null, tint = if (isSel) BnbGold else TextSecondary) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = DarkSurfaceElevated,
                                selectedTextColor = BnbGold,
                                unselectedTextColor = TextPrimary,
                                unselectedIconColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // DEX Owner Wallet Info Footer
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Official Fee Receiver:", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("0xD9e5...CA4", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                            Text("Listing Fee: 5 USDT", style = MaterialTheme.typography.labelSmall, color = BnbGold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                DexTopAppBar(
                    currentNetwork = currentNetwork,
                    connectedWallet = connectedWallet,
                    nativeBalance = nativeBalance,
                    onNetworkClick = { viewModel.openNetworkModal() },
                    onWalletClick = { viewModel.openWalletModal() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val bottomItems = listOf(
                        Triple(DexScreen.HOME, "Home", Icons.Default.Home),
                        Triple(DexScreen.SWAP, "Swap", Icons.Default.SwapHoriz),
                        Triple(DexScreen.EXPLORE_TOKENS, "Explore", Icons.Default.Search),
                        Triple(DexScreen.CREATE_LISTING, "List (5$)", Icons.Default.AddBusiness),
                        Triple(DexScreen.PORTFOLIO, "Portfolio", Icons.Default.AccountBalanceWallet)
                    )

                    bottomItems.forEach { (screen, label, icon) ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(screen) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = BnbGold,
                                indicatorColor = BnbGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_item_${label.lowercase()}")
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary,
                        actionColor = BnbGold,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    DexScreen.HOME -> HomeDashboardScreen(
                        stats = protocolStats,
                        tokens = tokens,
                        pools = pools,
                        recentTransactions = transactions,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSelectToken = { viewModel.selectTokenForDetails(it) },
                        onSelectPool = { viewModel.selectPoolForAdd(it) }
                    )
                    DexScreen.SWAP -> SwapScreen(
                        swapState = swapState,
                        allTokens = tokens,
                        currentNetwork = currentNetwork,
                        connectedWallet = connectedWallet,
                        onAmountInChange = { viewModel.updateSwapAmountIn(it) },
                        onTokenInSelect = { viewModel.setSwapTokenIn(it) },
                        onTokenOutSelect = { viewModel.setSwapTokenOut(it) },
                        onSwitchTokens = { viewModel.switchSwapTokens() },
                        onSlippageChange = { viewModel.setSlippage(it) },
                        onExecuteSwap = { viewModel.executeSwap() },
                        onConnectWallet = { viewModel.openWalletModal() }
                    )
                    DexScreen.EXPLORE_TOKENS -> ExploreTokensScreen(
                        tokens = tokens,
                        onSelectToken = { viewModel.selectTokenForDetails(it) },
                        onNavigateToListing = { viewModel.navigateTo(DexScreen.CREATE_LISTING) }
                    )
                    DexScreen.TOKEN_DETAILS -> TokenDetailsScreen(
                        token = selectedToken,
                        currentNetwork = currentNetwork,
                        onSwapToken = {
                            viewModel.setSwapTokenIn(it)
                            viewModel.navigateTo(DexScreen.SWAP)
                        },
                        onAddLiquidityForToken = {
                            viewModel.navigateTo(DexScreen.CREATE_POOL)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                    DexScreen.CREATE_LISTING -> CreateListingScreen(
                        listingState = listingState,
                        currentNetwork = currentNetwork,
                        connectedWallet = connectedWallet,
                        onAddressChange = { viewModel.updateListingAddress(it) },
                        onInspectContract = { viewModel.inspectContract() },
                        onInitialPriceChange = { viewModel.updateListingInitialPrice(it) },
                        onSubmitListing = { viewModel.submitTokenListing() },
                        onConnectWallet = { viewModel.openWalletModal() },
                        onViewExplorer = { viewModel.navigateTo(DexScreen.EXPLORE_TOKENS) }
                    )
                    DexScreen.CREATE_POOL -> CreatePoolScreen(
                        allTokens = tokens,
                        onCreatePool = { tokenA, tokenB, feeTier, amountA, amountB ->
                            viewModel.createPool(tokenA, tokenB, feeTier, amountA, amountB)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                    DexScreen.ADD_LIQUIDITY -> AddLiquidityScreen(
                        pool = selectedPool,
                        onAddLiquidity = { poolId, a, b ->
                            viewModel.addLiquidity(poolId, a, b)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                    DexScreen.REMOVE_LIQUIDITY -> RemoveLiquidityScreen(
                        pool = selectedPool,
                        onRemoveLiquidity = { poolId, percent ->
                            viewModel.removeLiquidity(poolId, percent)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                    DexScreen.PORTFOLIO -> PortfolioScreen(
                        connectedWallet = connectedWallet,
                        nativeBalance = nativeBalance,
                        network = currentNetwork,
                        tokens = tokens,
                        pools = pools,
                        onAddLiquidityForPool = { viewModel.selectPoolForAdd(it) },
                        onRemoveLiquidityForPool = { viewModel.selectPoolForRemove(it) },
                        onConnectWallet = { viewModel.openWalletModal() }
                    )
                    DexScreen.TRANSACTION_HISTORY -> TransactionHistoryScreen(
                        transactions = transactions,
                        currentNetwork = currentNetwork
                    )
                    DexScreen.ANALYTICS -> AnalyticsScreen(
                        stats = protocolStats,
                        pools = pools,
                        tokens = tokens
                    )
                    DexScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                        stats = protocolStats,
                        tokens = tokens,
                        onUpdateListingFee = { viewModel.updateOfficialListingFee(it) },
                        onUpdateFeeReceiver = { viewModel.updateFeeReceiver(it) },
                        onToggleEmergencyPause = { viewModel.toggleEmergencyPause() },
                        onToggleTokenVerification = { viewModel.toggleTokenVerification(it) }
                    )
                    DexScreen.WEB_DEX_VIEW -> WebDexViewScreen()
                }
            }
        }
    }

    // Global Network Selector Dialog
    if (showNetworkModal) {
        NetworkSelectorDialog(
            currentNetwork = currentNetwork,
            onSelectNetwork = { viewModel.switchNetwork(it) },
            onDismiss = { viewModel.closeNetworkModal() }
        )
    }

    // Global Wallet Connect Dialog
    if (showWalletModal) {
        WalletConnectDialog(
            connectedAddress = connectedWallet,
            nativeBalance = nativeBalance,
            networkSymbol = currentNetwork.symbol,
            onConnectCustom = { viewModel.connectWalletWithAddress(it) },
            onDisconnect = { viewModel.disconnectWallet() },
            onDismiss = { viewModel.closeWalletModal() }
        )
    }
}
