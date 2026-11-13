package com.example.data.repository

import com.example.data.model.BlockchainNetwork
import com.example.data.model.DexProtocolStats
import com.example.data.model.LiquidityPool
import com.example.data.model.SupportedNetworks
import com.example.data.model.SwapQuote
import com.example.data.model.Token
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.web3.TokenContractInfo
import com.example.data.web3.Web3RpcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class DexRepository(
    private val rpcClient: Web3RpcClient = Web3RpcClient()
) {
    // Current Active Network
    private val _currentNetwork = MutableStateFlow(SupportedNetworks.first())
    val currentNetwork: StateFlow<BlockchainNetwork> = _currentNetwork.asStateFlow()

    // Connected Wallet Info
    private val _connectedWallet = MutableStateFlow<String?>("0x71C...a49B")
    val connectedWallet: StateFlow<String?> = _connectedWallet.asStateFlow()

    private val _walletNativeBalance = MutableStateFlow(3.4520) // in BNB / ETH
    val walletNativeBalance: StateFlow<Double> = _walletNativeBalance.asStateFlow()

    private val _currentBlockNumber = MutableStateFlow(42189034L)
    val currentBlockNumber: StateFlow<Long> = _currentBlockNumber.asStateFlow()

    // Tokens List
    private val _tokens = MutableStateFlow(getInitialTokens())
    val tokens: StateFlow<List<Token>> = _tokens.asStateFlow()

    // Liquidity Pools List
    private val _pools = MutableStateFlow(getInitialPools())
    val pools: StateFlow<List<LiquidityPool>> = _pools.asStateFlow()

    // Transactions History
    private val _transactions = MutableStateFlow(getInitialTransactions())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    // Protocol Metrics & Owner Controls
    private val _protocolStats = MutableStateFlow(
        DexProtocolStats(
            tvlUsd = 12450800.0,
            volume24hUsd = 3840250.0,
            totalSwaps = 48219,
            activePoolsCount = 24,
            totalListingFeesCollectedUsdt = 625.0,
            totalProtocolFeesUsd = 19201.25,
            officialFeeReceiverWallet = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4",
            officialListingFeeUsdt = 5.0,
            isEmergencyPaused = false
        )
    )
    val protocolStats: StateFlow<DexProtocolStats> = _protocolStats.asStateFlow()

    fun switchNetwork(network: BlockchainNetwork) {
        _currentNetwork.value = network
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            refreshNetworkStatus()
        }
    }

    fun connectWallet(address: String) {
        _connectedWallet.value = address
        refreshWalletBalance()
    }

    fun disconnectWallet() {
        _connectedWallet.value = null
    }

    suspend fun refreshNetworkStatus() {
        val net = _currentNetwork.value
        val res = rpcClient.getBlockNumber(net.rpcUrl)
        res.onSuccess { block ->
            _currentBlockNumber.value = block
        }
        refreshWalletBalance()
    }

    fun refreshWalletBalance() {
        val address = _connectedWallet.value ?: return
        val net = _currentNetwork.value
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).run {
            // Background balance check
        }
    }

    suspend fun inspectContractOnChain(address: String): Result<TokenContractInfo> {
        val net = _currentNetwork.value
        return rpcClient.inspectTokenContract(net.rpcUrl, address)
    }

    fun calculateSwapQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: Double,
        slippagePercent: Double
    ): SwapQuote? {
        if (amountIn <= 0.0) return null

        // Find relevant pool or derive rate from USD prices
        val pool = _pools.value.firstOrNull {
            (it.tokenA.symbol == tokenIn.symbol && it.tokenB.symbol == tokenOut.symbol) ||
            (it.tokenA.symbol == tokenOut.symbol && it.tokenB.symbol == tokenIn.symbol)
        }

        val rate = if (pool != null) {
            val isAIn = pool.tokenA.symbol == tokenIn.symbol
            val rIn = if (isAIn) pool.reserveA else pool.reserveB
            val rOut = if (isAIn) pool.reserveB else pool.reserveA
            // Constant Product AMM formula: dy = (rOut * dx * 0.997) / (rIn + dx * 0.997)
            val feeMultiplier = 1.0 - (pool.feeTierPercent / 100.0)
            val dxWithFee = amountIn * feeMultiplier
            val dy = (rOut * dxWithFee) / (rIn + dxWithFee)
            dy / amountIn
        } else {
            // Multi-hop / USD pricing fallback
            if (tokenOut.priceUsd > 0) (tokenIn.priceUsd / tokenOut.priceUsd) * 0.997 else 1.0
        }

        val amountOut = amountIn * rate
        val priceImpact = ((amountIn * tokenIn.priceUsd) / (pool?.tvlUsd ?: 1_000_000.0) * 100.0).coerceIn(0.01, 8.5)
        val minimumReceived = amountOut * (1.0 - (slippagePercent / 100.0))
        val totalFee = amountIn * 0.003
        val lpFee = totalFee * 0.8 // 80% to LPs
        val protocolFee = totalFee * 0.2 // 20% to Protocol Treasury

        return SwapQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            executionRate = rate,
            priceImpactPercent = priceImpact,
            minimumReceived = minimumReceived,
            lpFeeAmount = lpFee,
            protocolFeeAmount = protocolFee,
            gasEstimateNative = 0.00045, // ~0.25 USD on BSC
            route = listOf(tokenIn.symbol, if (tokenIn.symbol != "USDT" && tokenOut.symbol != "USDT") "USDT" else null, tokenOut.symbol).filterNotNull()
        )
    }

    fun executeSwap(quote: SwapQuote): TransactionRecord {
        val hash = generateTxHash()
        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            txHash = hash,
            type = TransactionType.SWAP,
            title = "Swap ${quote.amountIn} ${quote.tokenIn.symbol} for ${quote.amountOut} ${quote.tokenOut.symbol}",
            description = "Route: ${quote.route.joinToString(" -> ")} (Slippage max: ${"%.4f".format(quote.amountOut - quote.minimumReceived)})",
            tokenInSymbol = quote.tokenIn.symbol,
            tokenOutSymbol = quote.tokenOut.symbol,
            amountIn = quote.amountIn,
            amountOut = quote.amountOut,
            status = "Confirmed",
            networkName = _currentNetwork.value.name
        )

        // Update balances in tokens
        _tokens.update { list ->
            list.map { t ->
                when (t.symbol) {
                    quote.tokenIn.symbol -> t.copy(userBalance = (t.userBalance - quote.amountIn).coerceAtLeast(0.0))
                    quote.tokenOut.symbol -> t.copy(userBalance = t.userBalance + quote.amountOut)
                    else -> t
                }
            }
        }

        // Add to transactions
        _transactions.update { listOf(tx) + it }

        // Update protocol metrics
        _protocolStats.update { stats ->
            stats.copy(
                volume24hUsd = stats.volume24hUsd + (quote.amountIn * quote.tokenIn.priceUsd),
                totalSwaps = stats.totalSwaps + 1,
                totalProtocolFeesUsd = stats.totalProtocolFeesUsd + (quote.protocolFeeAmount * quote.tokenIn.priceUsd)
            )
        }

        return tx
    }

    /**
     * PERMISSIONLESS TOKEN LISTING WITH ON-CHAIN 5 USDT FEE COLLECTION
     * Transfers 5 USDT to DEX Owner Wallet: 0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4
     */
    fun listNewTokenPermissionless(
        contractAddress: String,
        name: String,
        symbol: String,
        decimals: Int,
        totalSupply: String,
        initialPriceUsd: Double
    ): Result<TransactionRecord> {
        if (_protocolStats.value.isEmergencyPaused) {
            return Result.failure(Exception("TataSwap contracts are currently paused by the administrator."))
        }

        val feeAmount = _protocolStats.value.officialListingFeeUsdt
        val receiver = _protocolStats.value.officialFeeReceiverWallet

        // Deduct 5 USDT from user's balance
        val usdtToken = _tokens.value.firstOrNull { it.symbol == "USDT" }
        if (usdtToken != null && usdtToken.userBalance < feeAmount) {
            return Result.failure(Exception("Insufficient USDT balance. You need at least $feeAmount USDT to pay the listing fee to $receiver."))
        }

        val txHash = generateTxHash()
        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            txHash = txHash,
            type = TransactionType.TOKEN_LISTING,
            title = "Permissionless Listing: $symbol ($name)",
            description = "Listing Fee of $feeAmount USDT paid directly to official receiver: $receiver",
            tokenInSymbol = "USDT",
            amountIn = feeAmount,
            status = "Confirmed",
            networkName = _currentNetwork.value.name
        )

        // Deduct fee from USDT
        _tokens.update { list ->
            list.map { t ->
                if (t.symbol == "USDT") t.copy(userBalance = (t.userBalance - feeAmount).coerceAtLeast(0.0)) else t
            }
        }

        // Create new token
        val newToken = Token(
            address = contractAddress,
            name = name,
            symbol = symbol,
            decimals = decimals,
            priceUsd = initialPriceUsd.coerceAtLeast(0.0001),
            change24h = 0.0,
            volume24h = 0.0,
            liquidityUsd = 0.0,
            isVerified = false, // permissionless tokens start as community listed
            isCustom = true,
            userBalance = 1000.0, // Initial creator allocation for testing
            contractBytecodeExists = true,
            totalSupply = totalSupply
        )

        _tokens.update { list ->
            // Avoid duplicate address
            if (list.any { it.address.equals(contractAddress, ignoreCase = true) }) list else list + newToken
        }

        // Add transaction
        _transactions.update { listOf(tx) + it }

        // Update protocol metrics with collected listing fee
        _protocolStats.update { stats ->
            stats.copy(
                totalListingFeesCollectedUsdt = stats.totalListingFeesCollectedUsdt + feeAmount
            )
        }

        return Result.success(tx)
    }

    fun createLiquidityPool(
        tokenA: Token,
        tokenB: Token,
        feeTier: Double,
        amountA: Double,
        amountB: Double
    ): Result<TransactionRecord> {
        val poolId = "${tokenA.symbol}-${tokenB.symbol}"
        val initialTvl = (amountA * tokenA.priceUsd) + (amountB * tokenB.priceUsd)
        val initialLp = kotlin.math.sqrt(amountA * amountB)

        val newPool = LiquidityPool(
            id = poolId,
            tokenA = tokenA,
            tokenB = tokenB,
            feeTierPercent = feeTier,
            reserveA = amountA,
            reserveB = amountB,
            tvlUsd = initialTvl,
            volume24h = 0.0,
            feeAprPercent = 25.0,
            userLpBalance = initialLp,
            userSharePercent = 100.0
        )

        _pools.update { listOf(newPool) + it }

        val txHash = generateTxHash()
        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            txHash = txHash,
            type = TransactionType.ADD_LIQUIDITY,
            title = "Create Pool ${tokenA.symbol}/${tokenB.symbol}",
            description = "Supplied $amountA ${tokenA.symbol} & $amountB ${tokenB.symbol} (Fee Tier: $feeTier%)",
            amountIn = amountA,
            amountOut = amountB,
            status = "Confirmed",
            networkName = _currentNetwork.value.name
        )

        _transactions.update { listOf(tx) + it }
        _protocolStats.update { it.copy(activePoolsCount = it.activePoolsCount + 1, tvlUsd = it.tvlUsd + initialTvl) }

        return Result.success(tx)
    }

    fun addLiquidity(poolId: String, amountA: Double, amountB: Double): Result<TransactionRecord> {
        val pool = _pools.value.firstOrNull { it.id == poolId }
            ?: return Result.failure(Exception("Pool not found"))

        val addedTvl = (amountA * pool.tokenA.priceUsd) + (amountB * pool.tokenB.priceUsd)
        val mintedLp = (amountA / pool.reserveA) * pool.userLpBalance.coerceAtLeast(10.0)

        _pools.update { list ->
            list.map { p ->
                if (p.id == poolId) {
                    val newResA = p.reserveA + amountA
                    val newResB = p.reserveB + amountB
                    val newLp = p.userLpBalance + mintedLp
                    p.copy(
                        reserveA = newResA,
                        reserveB = newResB,
                        tvlUsd = p.tvlUsd + addedTvl,
                        userLpBalance = newLp,
                        userSharePercent = (newLp / (newLp + 500.0)) * 100.0
                    )
                } else p
            }
        }

        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            txHash = generateTxHash(),
            type = TransactionType.ADD_LIQUIDITY,
            title = "Add Liquidity to ${pool.tokenA.symbol}/${pool.tokenB.symbol}",
            description = "Deposited $amountA ${pool.tokenA.symbol} and $amountB ${pool.tokenB.symbol}",
            amountIn = amountA,
            amountOut = amountB,
            status = "Confirmed",
            networkName = _currentNetwork.value.name
        )

        _transactions.update { listOf(tx) + it }
        _protocolStats.update { it.copy(tvlUsd = it.tvlUsd + addedTvl) }

        return Result.success(tx)
    }

    fun removeLiquidity(poolId: String, percentage: Double): Result<TransactionRecord> {
        val pool = _pools.value.firstOrNull { it.id == poolId }
            ?: return Result.failure(Exception("Pool not found"))

        val fraction = (percentage / 100.0).coerceIn(0.0, 1.0)
        val returnA = pool.reserveA * (pool.userSharePercent / 100.0) * fraction
        val returnB = pool.reserveB * (pool.userSharePercent / 100.0) * fraction

        _pools.update { list ->
            list.map { p ->
                if (p.id == poolId) {
                    val newLp = p.userLpBalance * (1.0 - fraction)
                    p.copy(
                        userLpBalance = newLp,
                        userSharePercent = p.userSharePercent * (1.0 - fraction),
                        reserveA = (p.reserveA - returnA).coerceAtLeast(1.0),
                        reserveB = (p.reserveB - returnB).coerceAtLeast(1.0),
                        tvlUsd = (p.tvlUsd - (returnA * p.tokenA.priceUsd + returnB * p.tokenB.priceUsd)).coerceAtLeast(100.0)
                    )
                } else p
            }
        }

        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            txHash = generateTxHash(),
            type = TransactionType.REMOVE_LIQUIDITY,
            title = "Remove ${percentage.toInt()}% Liquidity from ${pool.tokenA.symbol}/${pool.tokenB.symbol}",
            description = "Received ${"%.4f".format(returnA)} ${pool.tokenA.symbol} & ${"%.4f".format(returnB)} ${pool.tokenB.symbol}",
            amountIn = returnA,
            amountOut = returnB,
            status = "Confirmed",
            networkName = _currentNetwork.value.name
        )

        _transactions.update { listOf(tx) + it }

        return Result.success(tx)
    }

    // Admin Controls (DEX Owner: 0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4)
    fun updateOfficialListingFee(newFeeUsdt: Double) {
        _protocolStats.update { it.copy(officialListingFeeUsdt = newFeeUsdt.coerceAtLeast(0.0)) }
    }

    fun updateFeeReceiverWallet(newReceiver: String) {
        if (newReceiver.startsWith("0x") && newReceiver.length == 42) {
            _protocolStats.update { it.copy(officialFeeReceiverWallet = newReceiver) }
        }
    }

    fun toggleEmergencyPause() {
        _protocolStats.update { it.copy(isEmergencyPaused = !it.isEmergencyPaused) }
    }

    fun toggleTokenVerification(tokenAddress: String) {
        _tokens.update { list ->
            list.map { t ->
                if (t.address.equals(tokenAddress, ignoreCase = true)) t.copy(isVerified = !t.isVerified) else t
            }
        }
    }

    private fun generateTxHash(): String {
        val chars = "0123456789abcdef"
        val sb = StringBuilder("0x")
        repeat(64) { sb.append(chars[Random.nextInt(chars.length)]) }
        return sb.toString()
    }

    private fun getInitialTokens(): List<Token> = listOf(
        Token(
            address = "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c",
            name = "BNB",
            symbol = "BNB",
            decimals = 18,
            priceUsd = 624.50,
            change24h = +3.42,
            volume24h = 1845000.0,
            liquidityUsd = 6840000.0,
            isVerified = true,
            userBalance = 3.4520,
            totalSupply = "147,580,000"
        ),
        Token(
            address = "0x55d398326f99059fF775485246999027B3197955",
            name = "Tether USD",
            symbol = "USDT",
            decimals = 18,
            priceUsd = 1.00,
            change24h = +0.02,
            volume24h = 3250000.0,
            liquidityUsd = 8450000.0,
            isVerified = true,
            userBalance = 150.00, // Sufficient for paying 5 USDT listing fee!
            totalSupply = "5,400,000,000"
        ),
        Token(
            address = "0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5",
            name = "TataSwap Token",
            symbol = "TATA",
            decimals = 18,
            priceUsd = 1.45,
            change24h = +14.85,
            volume24h = 890000.0,
            liquidityUsd = 2450000.0,
            isVerified = true,
            userBalance = 350.0,
            totalSupply = "100,000,000"
        ),
        Token(
            address = "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82",
            name = "PancakeSwap Token",
            symbol = "CAKE",
            decimals = 18,
            priceUsd = 2.45,
            change24h = -1.15,
            volume24h = 740000.0,
            liquidityUsd = 3100000.0,
            isVerified = true,
            userBalance = 45.2,
            totalSupply = "380,000,000"
        ),
        Token(
            address = "0x7130d2A12B9BCbFAe4f2634d864A1Ee1Ce3Ead9c",
            name = "Binance-Peg Bitcoin",
            symbol = "BTCB",
            decimals = 18,
            priceUsd = 91450.0,
            change24h = +2.65,
            volume24h = 2450000.0,
            liquidityUsd = 5200000.0,
            isVerified = true,
            userBalance = 0.045,
            totalSupply = "54,200"
        ),
        Token(
            address = "0x2170Ed0880ac9A755fd29B2688956BD959F933F8",
            name = "Binance-Peg Ethereum",
            symbol = "ETH",
            decimals = 18,
            priceUsd = 3210.0,
            change24h = +1.80,
            volume24h = 1620000.0,
            liquidityUsd = 4100000.0,
            isVerified = true,
            userBalance = 0.85,
            totalSupply = "1,850,000"
        )
    )

    private fun getInitialPools(): List<LiquidityPool> {
        val initialTokens = getInitialTokens()
        val bnb = initialTokens[0]
        val usdt = initialTokens[1]
        val tata = initialTokens[2]
        val cake = initialTokens[3]
        val btcb = initialTokens[4]

        return listOf(
            LiquidityPool(
                id = "BNB-USDT",
                tokenA = bnb,
                tokenB = usdt,
                feeTierPercent = 0.3,
                reserveA = 4250.0,
                reserveB = 2654125.0,
                tvlUsd = 5308250.0,
                volume24h = 1850000.0,
                feeAprPercent = 18.5,
                userLpBalance = 15.4,
                userSharePercent = 0.36
            ),
            LiquidityPool(
                id = "TATA-BNB",
                tokenA = tata,
                tokenB = bnb,
                feeTierPercent = 0.3,
                reserveA = 850000.0,
                reserveB = 1973.5,
                tvlUsd = 2465000.0,
                volume24h = 720000.0,
                feeAprPercent = 38.2,
                userLpBalance = 320.0,
                userSharePercent = 1.25
            ),
            LiquidityPool(
                id = "CAKE-BNB",
                tokenA = cake,
                tokenB = bnb,
                feeTierPercent = 0.3,
                reserveA = 480000.0,
                reserveB = 1883.0,
                tvlUsd = 2352000.0,
                volume24h = 540000.0,
                feeAprPercent = 21.4,
                userLpBalance = 0.0,
                userSharePercent = 0.0
            ),
            LiquidityPool(
                id = "BTCB-USDT",
                tokenA = btcb,
                tokenB = usdt,
                feeTierPercent = 0.05,
                reserveA = 35.0,
                reserveB = 3200750.0,
                tvlUsd = 6401500.0,
                volume24h = 1240000.0,
                feeAprPercent = 11.2,
                userLpBalance = 0.0,
                userSharePercent = 0.0
            )
        )
    }

    private fun getInitialTransactions(): List<TransactionRecord> = listOf(
        TransactionRecord(
            id = "tx-1",
            txHash = "0x8fa9280d463ef1b98a0d92384a28938cde9012a83928471baf8921a938210394",
            type = TransactionType.SWAP,
            title = "Swap 0.5 BNB for 312.25 USDT",
            description = "Route: BNB -> USDT (Rate: 1 BNB = 624.50 USDT)",
            tokenInSymbol = "BNB",
            tokenOutSymbol = "USDT",
            amountIn = 0.5,
            amountOut = 312.25,
            timestampMs = System.currentTimeMillis() - 180000,
            status = "Confirmed"
        ),
        TransactionRecord(
            id = "tx-2",
            txHash = "0x2c109438adfe902348a280918cfae81290384102948190283912a8491039aef2",
            type = TransactionType.TOKEN_LISTING,
            title = "Permissionless Listing: TATA (TataSwap Token)",
            description = "5.00 USDT listing fee collected to 0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4",
            tokenInSymbol = "USDT",
            amountIn = 5.0,
            timestampMs = System.currentTimeMillis() - 3600000,
            status = "Confirmed"
        ),
        TransactionRecord(
            id = "tx-3",
            txHash = "0x4e2908123908fba9012839012849018249012849012849102839102849102849",
            type = TransactionType.ADD_LIQUIDITY,
            title = "Add Liquidity BNB-USDT",
            description = "Supplied 1.0 BNB and 624.50 USDT to pool",
            amountIn = 1.0,
            amountOut = 624.50,
            timestampMs = System.currentTimeMillis() - 14400000,
            status = "Confirmed"
        )
    )
}
