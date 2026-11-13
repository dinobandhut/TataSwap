package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.DexRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies TataSwap app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TataSwap", appName)
    }

    @Test
    fun `verify official DEX owner wallet and listing fee configuration`() {
        val repo = DexRepository()
        val stats = repo.protocolStats.value
        assertEquals("0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4", stats.officialFeeReceiverWallet)
        assertEquals(5.0, stats.officialListingFeeUsdt, 0.001)
    }

    @Test
    fun `verify permissionless token listing pays 5 USDT fee to official receiver`() {
        val repo = DexRepository()
        val initialUsdt = repo.tokens.value.first { it.symbol == "USDT" }.userBalance

        val res = repo.listNewTokenPermissionless(
            contractAddress = "0x1234567890123456789012345678901234567890",
            name = "Test Community Token",
            symbol = "TEST",
            decimals = 18,
            totalSupply = "10,000,000",
            initialPriceUsd = 0.50
        )

        assertTrue(res.isSuccess)
        val tx = res.getOrNull()
        assertNotNull(tx)
        assertTrue(tx!!.description.contains("0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4"))

        val postUsdt = repo.tokens.value.first { it.symbol == "USDT" }.userBalance
        assertEquals(initialUsdt - 5.0, postUsdt, 0.001)
    }
}
