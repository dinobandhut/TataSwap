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
        assertEquals(6, SupportedNetworks.size)
        assertTrue(SupportedNetworks.none { it.isTestnet })

        val byId = SupportedNetworks.associateBy { it.id }
        assertEquals("https://rpctata.duckdns.org/rpc", byId["tataservice"]?.rpcUrl)
        assertEquals(810161L, byId["tataservice"]?.chainId)
        assertEquals("https://public.1rpc.io/eth", byId["eth_mainnet"]?.rpcUrl)
        assertEquals("https://public.1rpc.io/bnb", byId["bsc_mainnet"]?.rpcUrl)
        assertEquals("https://public.1rpc.io/matic", byId["polygon_mainnet"]?.rpcUrl)
        assertEquals("https://rpc.coredao.org", byId["core_mainnet"]?.rpcUrl)
        assertEquals("https://api.mainnet.solana.com", byId["solana_mainnet"]?.rpcUrl)
        assertFalse(byId["solana_mainnet"]!!.isEvm)
    }

    @Test
    fun `verify default network and official fee receiver configuration`() {
        val repo = DexRepository()
        assertEquals("tataservice", repo.currentNetwork.value.id)
        assertEquals("0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4", repo.protocolStats.value.officialFeeReceiverWallet)
        assertEquals(5.0, repo.protocolStats.value.officialListingFeeUsdt, 0.001)
    }

    @Test
    fun `verify app owner pays zero fee for token listing while regular user pays 5 USDT`() {
        val repo = DexRepository()
        val initialUsdt = repo.tokens.value.first { it.symbol == "USDT" }.userBalance
        val initialFeesCollected = repo.protocolStats.value.totalListingFeesCollectedUsdt

        // 1. Connect as App Owner (official owner/receiver address)
        repo.connectWallet("0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4")
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
}
