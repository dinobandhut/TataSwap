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
    val isTestnet: Boolean
)

val SupportedNetworks = listOf(
    BlockchainNetwork(
        id = "bsc_testnet",
        name = "BNB Smart Chain Testnet",
        chainId = 97,
        symbol = "tBNB",
        rpcUrl = "https://data-seed-prebsc-1-s1.binance.org:8545",
        explorerUrl = "https://testnet.bscscan.com",
        isTestnet = true
    ),
    BlockchainNetwork(
        id = "bsc_mainnet",
        name = "BNB Smart Chain",
        chainId = 56,
        symbol = "BNB",
        rpcUrl = "https://bsc-dataseed.binance.org",
        explorerUrl = "https://bscscan.com",
        isTestnet = false
    ),
    BlockchainNetwork(
        id = "eth_sepolia",
        name = "Ethereum Sepolia",
        chainId = 11155111,
        symbol = "SepoliaETH",
        rpcUrl = "https://rpc.sepolia.org",
        explorerUrl = "https://sepolia.etherscan.io",
        isTestnet = true
    ),
    BlockchainNetwork(
        id = "polygon_amoy",
        name = "Polygon Amoy",
        chainId = 80002,
        symbol = "POL",
        rpcUrl = "https://rpc-amoy.polygon.technology",
        explorerUrl = "https://amoy.polygonscan.com",
        isTestnet = true
    ),
    BlockchainNetwork(
        id = "arbitrum_sepolia",
        name = "Arbitrum Sepolia",
        chainId = 421614,
        symbol = "ETH",
        rpcUrl = "https://sepolia-rollup.arbitrum.io/rpc",
        explorerUrl = "https://sepolia.arbiscan.io",
        isTestnet = true
    ),
    BlockchainNetwork(
        id = "base_sepolia",
        name = "Base Sepolia",
        chainId = 84532,
        symbol = "ETH",
        rpcUrl = "https://sepolia.base.org",
        explorerUrl = "https://sepolia.basescan.org",
        isTestnet = true
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
    val feeTierPercent: Double = 0.3, // 0.05%, 0.3%, 1.0%
    val reserveA: Double,
    val reserveB: Double,
    val tvlUsd: Double,
    val volume24h: Double,
    val feeAprPercent: Double,
    val userLpBalance: Double = 0.0,
    val userSharePercent: Double = 0.0
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
    val networkName: String = "BNB Smart Chain Testnet"
)

data class DexProtocolStats(
    val tvlUsd: Double = 12450800.0,
    val volume24hUsd: Double = 3840250.0,
    val totalSwaps: Long = 48219,
    val activePoolsCount: Int = 24,
    val totalListingFeesCollectedUsdt: Double = 625.0,
    val totalProtocolFeesUsd: Double = 19201.25,
    val officialFeeReceiverWallet: String = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4",
    val officialListingFeeUsdt: Double = 5.0,
    val isEmergencyPaused: Boolean = false
)
