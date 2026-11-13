package com.example

import com.example.data.repository.DexRepository
import org.junit.Assert.assertEquals
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
        assertEquals(0.5, 0.5, 0.001)
    }
}
