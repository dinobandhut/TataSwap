package com.example

import com.example.data.model.SupportedNetworks
import com.example.data.repository.DexRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `verify constant product AMM swap calculation`() {
        val repo = DexRepository()
        val bnb = repo.tokens.value.first { it.symbol == "BNB" }
        val usdt = repo.tokens.value.first { it.symbol == "USDT" }

        val quote = repo.calculateSwapQuote(bnb, usdt, 1.0, 0.5)
        assertNotNull(quote)
        assertTrue(quote!!.amountOut > 600.0) // 1 BNB is ~$624.50 USDT
        assertTrue(quote.minimumReceived < quote.amountOut)
    }

    @Test
    fun `verify mainnet-only supported networks configuration`() {
        assertEquals(5, SupportedNetworks.size)
        assertTrue(SupportedNetworks.none { it.isTestnet })

        val byId = SupportedNetworks.associateBy { it.id }
        assertEquals("https://rpctata.duckdns.org/rpc", byId["tataservice"]?.rpcUrl)
        assertEquals(810161L, byId["tataservice"]?.chainId)
        assertEquals("https://public.1rpc.io/eth", byId["eth_mainnet"]?.rpcUrl)
        assertEquals("https://public.1rpc.io/bnb", byId["bsc_mainnet"]?.rpcUrl)
        assertEquals("https://public.1rpc.io/matic", byId["polygon_mainnet"]?.rpcUrl)
        assertEquals("https://rpc.coredao.org", byId["core_mainnet"]?.rpcUrl)
        assertFalse(byId.containsKey("solana_mainnet"))
    }

    @Test
    fun `verify default network and official fee receiver configuration`() {
        val repo = DexRepository()
        assertEquals("tataservice", repo.currentNetwork.value.id)
        assertEquals("0x3f48248FFE235a5205d1D781fB6c852c71fc1556", repo.protocolStats.value.officialFeeReceiverWallet)
        assertEquals(5.0, repo.protocolStats.value.officialListingFeeUsdt, 0.001)
    }

    @Test
    fun `verify app owner pays zero fee for token listing while regular user pays 5 USDT`() {
        val repo = DexRepository()
        val initialUsdt = repo.tokens.value.first { it.symbol == "USDT" }.userBalance
        val initialFeesCollected = repo.protocolStats.value.totalListingFeesCollectedUsdt

        // 1. Connect as App Owner (official owner/receiver address)
        repo.connectWallet("0x3f48248FFE235a5205d1D781fB6c852c71fc1556")
        assertTrue(repo.isAppOwnerWallet(repo.connectedWallet.value))

        val ownerRes = repo.listNewTokenPermissionless(
            contractAddress = "0x1111111111111111111111111111111111111111",
            name = "Owner Token",
            symbol = "OWN",
            decimals = 18,
            totalSupply = "1000000",
            initialPriceUsd = 2.0
        )
        assertTrue(ownerRes.isSuccess)
        assertEquals(0.0, ownerRes.getOrThrow().amountIn ?: -1.0, 0.0001)
        assertEquals(initialUsdt, repo.tokens.value.first { it.symbol == "USDT" }.userBalance, 0.0001)
        assertEquals(initialFeesCollected, repo.protocolStats.value.totalListingFeesCollectedUsdt, 0.0001)
        assertTrue(repo.tokens.value.first { it.symbol == "OWN" }.isVerified)

        // 2. Connect as regular user -> pays 5 USDT fee
        repo.connectWallet("0x2222222222222222222222222222222222222222")
        assertFalse(repo.isAppOwnerWallet(repo.connectedWallet.value))

        val userRes = repo.listNewTokenPermissionless(
            contractAddress = "0x3333333333333333333333333333333333333333",
            name = "User Token",
            symbol = "USR",
            decimals = 18,
            totalSupply = "1000000",
            initialPriceUsd = 1.0
        )
        assertTrue(userRes.isSuccess)
        assertEquals(5.0, userRes.getOrThrow().amountIn ?: -1.0, 0.0001)
        assertEquals(initialUsdt - 5.0, repo.tokens.value.first { it.symbol == "USDT" }.userBalance, 0.0001)
        assertEquals(initialFeesCollected + 5.0, repo.protocolStats.value.totalListingFeesCollectedUsdt, 0.0001)
    }

    @Test
    fun `verify wallet deep links and real crypto transfer URI builder`() {
        val repo = DexRepository()
        assertTrue(repo.buildWalletDappDeepLink("metamask").startsWith("https://link.metamask.io/dapp/"))
        assertTrue(repo.buildWalletDappDeepLink("trust").startsWith("https://link.trustwallet.com/open_url"))
        assertTrue(repo.buildWalletDappDeepLink("coinbase").startsWith("https://go.cb-w.com/dapp"))
        assertTrue(repo.buildWalletDappDeepLink("binance").startsWith("bnc://app.binance.com/"))
        assertTrue(repo.buildWalletDappDeepLink("okx").startsWith("https://www.okx.com/download"))
        assertTrue(repo.buildWalletDappDeepLink("bitget").startsWith("https://bkcode.vip"))
        assertTrue(repo.buildWalletDappDeepLink("safepal").startsWith("https://link.safepal.io/dapp"))

        val sendUri = repo.buildNativeTransferDeepLink("0x3f48248FFE235a5205d1D781fB6c852c71fc1556", 1.0, "metamask")
        assertTrue(sendUri.contains("0x3f48248FFE235a5205d1D781fB6c852c71fc1556@810161?value=1000000000000000000"))
    }

    @Test
    fun `verify 2-step create pool supports fee tiers, hook address, initial price, and custom range`() {
        val repo = DexRepository()
        val pol = repo.tokens.value.firstOrNull { it.symbol == "USDT" } ?: repo.tokens.value[0]
        val tata = repo.tokens.value.firstOrNull { it.symbol == "TATA" } ?: repo.tokens.value[1]

        val res = repo.createLiquidityPool(
            tokenA = pol,
            tokenB = tata,
            feeTier = 0.9,
            amountA = 100.0,
            amountB = 145.0,
            hookAddress = "0x000000000000000000000000000000000000H001",
            initialPrice = 1.45,
            isFullRange = false,
            minPrice = 1.1,
            maxPrice = 1.9
        )

        assertTrue(res.isSuccess)
        val created = repo.pools.value.first()
        assertEquals(0.9, created.feeTierPercent, 0.0001)
        assertEquals("0x000000000000000000000000000000000000H001", created.hookAddress)
        assertEquals(1.45, created.initialPrice ?: 0.0, 0.0001)
        assertFalse(created.isFullRange)
        assertEquals(1.1, created.minPrice ?: 0.0, 0.0001)
        assertEquals(1.9, created.maxPrice ?: 0.0, 0.0001)
        assertTrue(created.isUserDeployed)
    }
}
