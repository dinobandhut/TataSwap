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
}
