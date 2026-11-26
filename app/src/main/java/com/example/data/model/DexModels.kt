package com.example.data.model

enum class TransactionType {
    SWAP,
    ADD_LIQUIDITY,
    REMOVE_LIQUIDITY,
    TOKEN_LISTING
}

data class BlockchainNetwork(
    val id: String,
    val name: String,
    val chainId: Long,
    val symbol: String,
    val rpcUrl: String,
    val explorerUrl: String,
    val isTestnet: Boolean = false,
    val isEvm: Boolean = true
)

val SupportedNetworks = listOf(
    BlockchainNetwork(
        id = "tataservice",
        name = "TataService Blockchain",
        chainId = 810161,
        symbol = "TATA",
        rpcUrl = "https://rpctata.duckdns.org/rpc",
        explorerUrl = "https://exploretata.duckdns.org",
        isTestnet = false,
        isEvm = true
    ),
    BlockchainNetwork(
        id = "eth_mainnet",
        name = "Ethereum Mainnet",
        chainId = 1,
        symbol = "ETH",
        rpcUrl = "https://public.1rpc.io/eth",
        explorerUrl = "https://etherscan.io",
        isTestnet = false,
        isEvm = true
    ),
    BlockchainNetwork(
        id = "bsc_mainnet",
        name = "BNB Smart Chain",
        chainId = 56,
        symbol = "BNB",
        rpcUrl = "https://public.1rpc.io/bnb",
        explorerUrl = "https://bscscan.com",
        isTestnet = false,
        isEvm = true
    ),
    BlockchainNetwork(
        id = "polygon_mainnet",
        name = "Polygon Mainnet",
        chainId = 137,
        symbol = "POL",
        rpcUrl = "https://public.1rpc.io/matic",
        explorerUrl = "https://polygonscan.com",
        isTestnet = false,
        isEvm = true
    ),
    BlockchainNetwork(
        id = "core_mainnet",
        name = "Core Blockchain",
        chainId = 1116,
        symbol = "CORE",
        rpcUrl = "https://rpc.coredao.org",
        explorerUrl = "https://scan.coredao.org",
        isTestnet = false,
        isEvm = true
    )
)

data class Token(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int = 18,
    val priceUsd: Double,
    val change24h: Double,
    val volume24h: Double,
    val liquidityUsd: Double,
    val isVerified: Boolean = true,
    val isCustom: Boolean = false,
    val userBalance: Double = 0.0,
    val contractBytecodeExists: Boolean = true,
    val totalSupply: String = "1,000,000,000"
)

data class LiquidityPool(
    val id: String,
    val tokenA: Token,
    val tokenB: Token,
    val feeTierPercent: Double = 0.25, // 0.0075%, 0.0375%, 0.25%, 0.9%, or custom
    val reserveA: Double,
    val reserveB: Double,
    val tvlUsd: Double,
    val volume24h: Double,
    val feeAprPercent: Double,
    val userLpBalance: Double = 0.0,
    val userSharePercent: Double = 0.0,
    val networkId: String = "tataservice",
    val hookAddress: String? = null,
    val initialPrice: Double? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val isFullRange: Boolean = true,
    val isUserDeployed: Boolean = false
)

data class SwapQuote(
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: Double,
    val amountOut: Double,
    val executionRate: Double,
    val priceImpactPercent: Double,
    val minimumReceived: Double,
    val lpFeeAmount: Double,
    val protocolFeeAmount: Double,
    val gasEstimateNative: Double,
    val route: List<String>
)

data class TransactionRecord(
    val id: String,
    val txHash: String,
    val type: TransactionType,
    val title: String,
    val description: String,
    val tokenInSymbol: String? = null,
    val tokenOutSymbol: String? = null,
    val amountIn: Double? = null,
    val amountOut: Double? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val status: String = "Confirmed",
    val networkName: String = "TataService Blockchain"
)

data class DexProtocolStats(
    val tvlUsd: Double = 12450800.0,
    val volume24hUsd: Double = 3840250.0,
    val totalSwaps: Long = 48219,
    val activePoolsCount: Int = 24,
    val totalListingFeesCollectedUsdt: Double = 625.0,
    val totalProtocolFeesUsd: Double = 19201.25,
    val officialFeeReceiverWallet: String = "0x3f48248FFE235a5205d1D781fB6c852c71fc1556",
    val officialListingFeeUsdt: Double = 5.0,
    val isEmergencyPaused: Boolean = false
)
