package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlockchainNetwork
import com.example.data.model.DexProtocolStats
import com.example.data.model.LiquidityPool
import com.example.data.model.SwapQuote
import com.example.data.model.Token
import com.example.data.model.TransactionRecord
import com.example.data.repository.DexRepository
import com.example.data.web3.TokenContractInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DexScreen(val title: String) {
    HOME("Home"),
    SWAP("Swap"),
    EXPLORE_TOKENS("Explore"),
    TOKEN_DETAILS("Token Details"),
    CREATE_LISTING("List Token"),
    CREATE_POOL("Create Pool"),
    ADD_LIQUIDITY("Add Liquidity"),
    REMOVE_LIQUIDITY("Remove Liquidity"),
    PORTFOLIO("Portfolio"),
    TRANSACTION_HISTORY("History"),
    ANALYTICS("Analytics"),
    ADMIN_DASHBOARD("Admin"),
    WEB_DEX_VIEW("Web DEX")
}

data class SwapUiState(
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: String = "",
    val amountOut: String = "",
    val slippagePercent: Double = 0.5,
    val quote: SwapQuote? = null,
    val isEstimating: Boolean = false,
    val isSwapping: Boolean = false,
    val lastSwapTx: TransactionRecord? = null
)

data class ListingUiState(
    val contractAddress: String = "",
    val isInspecting: Boolean = false,
    val detectedInfo: TokenContractInfo? = null,
    val initialPriceUsd: String = "1.00",
    val requiredFeeUsdt: Double = 5.0,
    val feeReceiverAddress: String = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4",
    val isSubmitting: Boolean = false,
    val listingSuccessTx: TransactionRecord? = null,
    val errorMessage: String? = null
)

class DexViewModel(
    val repository: DexRepository = DexRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(DexScreen.HOME)
    val currentScreen: StateFlow<DexScreen> = _currentScreen.asStateFlow()

    // Screen navigation stack
    private val screenHistory = mutableListOf<DexScreen>()

    // Selected Token for Details Screen
    private val _selectedToken = MutableStateFlow<Token?>(null)
    val selectedToken: StateFlow<Token?> = _selectedToken.asStateFlow()

    // Selected Pool for Add/Remove
    private val _selectedPool = MutableStateFlow<LiquidityPool?>(null)
    val selectedPool: StateFlow<LiquidityPool?> = _selectedPool.asStateFlow()

    // Swap State
    private val _swapState = MutableStateFlow<SwapUiState?>(null)
    val swapState: StateFlow<SwapUiState?> = _swapState.asStateFlow()

    // Token Listing State
    private val _listingState = MutableStateFlow(ListingUiState())
    val listingState: StateFlow<ListingUiState> = _listingState.asStateFlow()

    // Snackbar notifications
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Wallet connect dialog
    private val _showWalletModal = MutableStateFlow(false)
    val showWalletModal: StateFlow<Boolean> = _showWalletModal.asStateFlow()

    // Network selector modal
    private val _showNetworkModal = MutableStateFlow(false)
    val showNetworkModal: StateFlow<Boolean> = _showNetworkModal.asStateFlow()

    // Exposed repository flows
    val currentNetwork: StateFlow<BlockchainNetwork> = repository.currentNetwork
    val connectedWallet: StateFlow<String?> = repository.connectedWallet
    val walletNativeBalance: StateFlow<Double> = repository.walletNativeBalance
    val currentBlockNumber: StateFlow<Long> = repository.currentBlockNumber
    val tokens: StateFlow<List<Token>> = repository.tokens
    val pools: StateFlow<List<LiquidityPool>> = repository.pools
    val transactions: StateFlow<List<TransactionRecord>> = repository.transactions
    val protocolStats: StateFlow<DexProtocolStats> = repository.protocolStats

    init {
        // Initialize default swap tokens (BNB -> USDT)
        val initialTokens = repository.tokens.value
        val bnb = initialTokens.firstOrNull { it.symbol == "BNB" } ?: initialTokens[0]
        val usdt = initialTokens.firstOrNull { it.symbol == "USDT" } ?: initialTokens[1]
        _swapState.value = SwapUiState(tokenIn = bnb, tokenOut = usdt)
        _selectedToken.value = bnb
        _selectedPool.value = repository.pools.value.firstOrNull()

        // Sync initial network status
        viewModelScope.launch {
            repository.refreshNetworkStatus()
        }
    }

    fun navigateTo(screen: DexScreen) {
        if (_currentScreen.value != screen) {
            screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenHistory.isNotEmpty()) {
            _currentScreen.value = screenHistory.removeAt(screenHistory.size - 1)
            true
        } else if (_currentScreen.value != DexScreen.HOME) {
            _currentScreen.value = DexScreen.HOME
            true
        } else {
            false
        }
    }

    fun selectTokenForDetails(token: Token) {
        _selectedToken.value = token
        navigateTo(DexScreen.TOKEN_DETAILS)
    }

    fun selectPoolForAdd(pool: LiquidityPool) {
        _selectedPool.value = pool
        navigateTo(DexScreen.ADD_LIQUIDITY)
    }

    fun selectPoolForRemove(pool: LiquidityPool) {
        _selectedPool.value = pool
        navigateTo(DexScreen.REMOVE_LIQUIDITY)
    }

    // --- SWAP ACTIONS ---
    fun updateSwapAmountIn(amount: String) {
        val curr = _swapState.value ?: return
        val parsed = amount.toDoubleOrNull() ?: 0.0
        val quote = if (parsed > 0.0) {
            repository.calculateSwapQuote(curr.tokenIn, curr.tokenOut, parsed, curr.slippagePercent)
        } else null

        val outStr = quote?.let { "%.4f".format(it.amountOut) } ?: ""
        _swapState.value = curr.copy(
            amountIn = amount,
            amountOut = outStr,
            quote = quote
        )
    }

    fun setSwapTokenIn(token: Token) {
        val curr = _swapState.value ?: return
        if (token.symbol == curr.tokenOut.symbol) {
            // Invert tokens
            _swapState.value = curr.copy(tokenIn = token, tokenOut = curr.tokenIn)
        } else {
            _swapState.value = curr.copy(tokenIn = token)
        }
        updateSwapAmountIn(_swapState.value?.amountIn ?: "")
    }

    fun setSwapTokenOut(token: Token) {
        val curr = _swapState.value ?: return
        if (token.symbol == curr.tokenIn.symbol) {
            _swapState.value = curr.copy(tokenIn = curr.tokenOut, tokenOut = token)
        } else {
            _swapState.value = curr.copy(tokenOut = token)
        }
        updateSwapAmountIn(_swapState.value?.amountIn ?: "")
    }

    fun switchSwapTokens() {
        val curr = _swapState.value ?: return
        _swapState.value = curr.copy(
            tokenIn = curr.tokenOut,
            tokenOut = curr.tokenIn,
            amountIn = "",
            amountOut = "",
            quote = null
        )
    }

    fun setSlippage(slippage: Double) {
        val curr = _swapState.value ?: return
        _swapState.value = curr.copy(slippagePercent = slippage)
        updateSwapAmountIn(curr.amountIn)
    }

    fun executeSwap() {
        val quote = _swapState.value?.quote ?: return
        viewModelScope.launch {
            _swapState.update { it?.copy(isSwapping = true) }
            val tx = repository.executeSwap(quote)
            _swapState.update {
                it?.copy(
                    isSwapping = false,
                    lastSwapTx = tx,
                    amountIn = "",
                    amountOut = "",
                    quote = null
                )
            }
            _snackbarMessage.emit("Swap successful! Tx: ${tx.txHash.take(10)}...${tx.txHash.takeLast(6)}")
        }
    }

    // --- PERMISSIONLESS LISTING ACTIONS ---
    fun updateListingAddress(address: String) {
        _listingState.update { it.copy(contractAddress = address, detectedInfo = null, errorMessage = null) }
    }

    fun updateListingInitialPrice(price: String) {
        _listingState.update { it.copy(initialPriceUsd = price) }
    }

    fun inspectContract() {
        val addr = _listingState.value.contractAddress.trim()
        if (addr.isEmpty()) return

        viewModelScope.launch {
            _listingState.update { it.copy(isInspecting = true, errorMessage = null) }
            val result = repository.inspectContractOnChain(addr)
            result.onSuccess { info ->
                _listingState.update { it.copy(isInspecting = false, detectedInfo = info) }
            }.onFailure { err ->
                // Still allow proceeding with detected or testnet fallback
                _listingState.update {
                    it.copy(
                        isInspecting = false,
                        detectedInfo = TokenContractInfo(
                            address = addr,
                            name = "Community Token",
                            symbol = "COMM",
                            decimals = 18,
                            totalSupply = "10,000,000",
                            isVerifiedContract = false
                        ),
                        errorMessage = "Notice: ${err.message}. Configured with default standard ERC20 interface."
                    )
                }
            }
        }
    }

    fun submitTokenListing() {
        val state = _listingState.value
        val info = state.detectedInfo ?: return

        viewModelScope.launch {
            _listingState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val price = state.initialPriceUsd.toDoubleOrNull() ?: 1.0

            val result = repository.listNewTokenPermissionless(
                contractAddress = info.address,
                name = info.name,
                symbol = info.symbol,
                decimals = info.decimals,
                totalSupply = info.totalSupply,
                initialPriceUsd = price
            )

            result.onSuccess { tx ->
                _listingState.update {
                    it.copy(
                        isSubmitting = false,
                        listingSuccessTx = tx,
                        contractAddress = "",
                        detectedInfo = null
                    )
                }
                _snackbarMessage.emit("Token ${info.symbol} successfully listed! Fee sent to 0xD9e5...CA4")
            }.onFailure { err ->
                _listingState.update { it.copy(isSubmitting = false, errorMessage = err.message) }
                _snackbarMessage.emit("Listing Error: ${err.message}")
            }
        }
    }

    // --- LIQUIDITY ACTIONS ---
    fun createPool(tokenA: Token, tokenB: Token, feeTier: Double, amountA: Double, amountB: Double) {
        viewModelScope.launch {
            val res = repository.createLiquidityPool(tokenA, tokenB, feeTier, amountA, amountB)
            res.onSuccess { tx ->
                _snackbarMessage.emit("Liquidity Pool Created! Tx: ${tx.txHash.take(10)}...")
                navigateTo(DexScreen.EXPLORE_TOKENS)
            }.onFailure {
                _snackbarMessage.emit("Pool Creation Failed: ${it.message}")
            }
        }
    }

    fun addLiquidity(poolId: String, amountA: Double, amountB: Double) {
        viewModelScope.launch {
            val res = repository.addLiquidity(poolId, amountA, amountB)
            res.onSuccess { tx ->
                _snackbarMessage.emit("Liquidity added successfully!")
                navigateTo(DexScreen.PORTFOLIO)
            }.onFailure {
                _snackbarMessage.emit("Failed to add liquidity: ${it.message}")
            }
        }
    }

    fun removeLiquidity(poolId: String, percent: Double) {
        viewModelScope.launch {
            val res = repository.removeLiquidity(poolId, percent)
            res.onSuccess {
                _snackbarMessage.emit("Liquidity removed successfully!")
                navigateTo(DexScreen.PORTFOLIO)
            }.onFailure {
                _snackbarMessage.emit("Failed to remove liquidity: ${it.message}")
            }
        }
    }

    // --- ADMIN DASHBOARD ACTIONS ---
    fun updateOfficialListingFee(newFee: Double) {
        repository.updateOfficialListingFee(newFee)
        viewModelScope.launch {
            _snackbarMessage.emit("Listing Fee updated to $newFee USDT")
        }
    }

    fun updateFeeReceiver(newAddr: String) {
        repository.updateFeeReceiverWallet(newAddr)
        viewModelScope.launch {
            _snackbarMessage.emit("Fee Receiver Wallet updated to $newAddr")
        }
    }

    fun toggleEmergencyPause() {
        repository.toggleEmergencyPause()
        viewModelScope.launch {
            val status = if (repository.protocolStats.value.isEmergencyPaused) "PAUSED" else "ACTIVE"
            _snackbarMessage.emit("Protocol Emergency Status: $status")
        }
    }

    fun toggleTokenVerification(tokenAddress: String) {
        repository.toggleTokenVerification(tokenAddress)
        viewModelScope.launch {
            _snackbarMessage.emit("Token verification status updated.")
        }
    }

    // --- WALLET & NETWORK MODALS ---
    fun openWalletModal() { _showWalletModal.value = true }
    fun closeWalletModal() { _showWalletModal.value = false }
    fun connectWalletWithAddress(address: String) {
        repository.connectWallet(address)
        closeWalletModal()
        viewModelScope.launch {
            _snackbarMessage.emit("Wallet connected: ${address.take(6)}...${address.takeLast(4)}")
        }
    }
    fun disconnectWallet() {
        repository.disconnectWallet()
        closeWalletModal()
    }

    fun openNetworkModal() { _showNetworkModal.value = true }
    fun closeNetworkModal() { _showNetworkModal.value = false }
    fun switchNetwork(net: BlockchainNetwork) {
        repository.switchNetwork(net)
        closeNetworkModal()
        viewModelScope.launch {
            _snackbarMessage.emit("Switched to ${net.name}")
        }
    }
}
