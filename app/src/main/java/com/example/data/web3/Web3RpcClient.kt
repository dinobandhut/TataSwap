package com.example.data.web3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class Web3RpcClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getBlockNumber(rpcUrl: String, isEvm: Boolean = true): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", if (isEvm) "eth_blockNumber" else "getSlot")
                put("params", JSONArray())
                put("id", 1)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            if (resObj.has("result")) {
                val blockNum = if (isEvm) {
                    val hex = resObj.getString("result")
                    java.lang.Long.decode(hex)
                } else {
                    resObj.getLong("result")
                }
                Result.success(blockNum)
            } else {
                Result.failure(Exception(resObj.optJSONObject("error")?.optString("message") ?: "Unknown RPC error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNativeBalance(rpcUrl: String, walletAddress: String, isEvm: Boolean = true): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", if (isEvm) "eth_getBalance" else "getBalance")
                put("params", if (isEvm) JSONArray().put(walletAddress).put("latest") else JSONArray().put(walletAddress))
                put("id", 2)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            if (resObj.has("result")) {
                if (isEvm) {
                    val hex = resObj.getString("result").removePrefix("0x")
                    val wei = if (hex.isEmpty()) BigInteger.ZERO else BigInteger(hex, 16)
                    val eth = BigDecimal(wei).divide(BigDecimal("1000000000000000000"), 4, java.math.RoundingMode.HALF_UP)
                    Result.success(eth.toDouble())
                } else {
                    val lamports = resObj.getJSONObject("result").optLong("value", 0L)
                    val sol = BigDecimal(lamports).divide(BigDecimal("1000000000"), 4, java.math.RoundingMode.HALF_UP)
                    Result.success(sol.toDouble())
                }
            } else {
                Result.failure(Exception("Failed to fetch balance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getErc20Balance(
        rpcUrl: String,
        tokenAddress: String,
        walletAddress: String,
        decimals: Int = 18
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = tokenAddress.trim()
            val cleanWallet = walletAddress.trim().removePrefix("0x").lowercase()
            if (!cleanToken.startsWith("0x") || cleanToken.length != 42 || cleanWallet.length != 40) {
                return@withContext Result.failure(Exception("Invalid EVM contract or wallet address"))
            }
            val data = "0x70a08231" + cleanWallet.padStart(64, '0')
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_call")
                put("params", JSONArray().put(JSONObject().apply {
                    put("to", cleanToken)
                    put("data", data)
                }).put("latest"))
                put("id", 3)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            if (!resObj.has("result")) {
                return@withContext Result.failure(Exception("ERC20 balanceOf failed"))
            }
            val hex = resObj.getString("result").removePrefix("0x")
            val raw = if (hex.isEmpty()) BigInteger.ZERO else BigInteger(hex, 16)
            val divisor = BigDecimal.TEN.pow(decimals.coerceIn(0, 18))
            val human = BigDecimal(raw).divide(divisor, 4, java.math.RoundingMode.HALF_UP)
            Result.success(human.toDouble())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectTokenContract(rpcUrl: String, contractAddress: String, isEvm: Boolean = true): Result<TokenContractInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanAddr = contractAddress.trim()
            if (!isEvm) {
                if (cleanAddr.length !in 32..44) {
                    return@withContext Result.failure(Exception("Invalid Solana SPL Mint address format."))
                }
                val solPayload = JSONObject().apply {
                    put("jsonrpc", "2.0")
                    put("method", "getAccountInfo")
                    put("params", JSONArray().put(cleanAddr).put(JSONObject().apply { put("encoding", "jsonParsed") }))
                    put("id", 10)
                }
                val solResp = JSONObject(executeRpc(rpcUrl, solPayload.toString()))
                val valueObj = solResp.optJSONObject("result")?.optJSONObject("value")
                val parsedInfo = valueObj?.optJSONObject("data")?.optJSONObject("parsed")?.optJSONObject("info")
                val decimals = parsedInfo?.optInt("decimals", 9) ?: 9
                val supply = parsedInfo?.optString("supply", "1000000000") ?: "1,000,000,000"
                return@withContext Result.success(
                    TokenContractInfo(
                        address = cleanAddr,
                        name = "Solana SPL Token",
                        symbol = "SPL",
                        decimals = decimals,
                        totalSupply = supply,
                        isVerifiedContract = valueObj != null
                    )
                )
            }

            if (!cleanAddr.startsWith("0x") || cleanAddr.length != 42) {
                return@withContext Result.failure(Exception("Invalid address format. Must be 42-character 0x hex address."))
            }

            // 1. Check getCode to see if contract exists
            val codePayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_getCode")
                put("params", JSONArray().put(cleanAddr).put("latest"))
                put("id", 10)
            }
            val codeResp = JSONObject(executeRpc(rpcUrl, codePayload.toString()))
            val bytecode = codeResp.optString("result", "0x")
            val isContract = bytecode.length > 2 && bytecode != "0x"

            // 2. Fetch name() -> selector 0x06fdde03
            val name = callStringMethod(rpcUrl, cleanAddr, "0x06fdde03") ?: "Custom Token"

            // 3. Fetch symbol() -> selector 0x95d89b41
            val symbol = callStringMethod(rpcUrl, cleanAddr, "0x95d89b41") ?: "CUST"

            // 4. Fetch decimals() -> selector 0x313ce567
            val decimals = callUint8Method(rpcUrl, cleanAddr, "0x313ce567") ?: 18

            // 5. Fetch totalSupply() -> selector 0x18160ddd
            val totalSupply = callUint256Method(rpcUrl, cleanAddr, "0x18160ddd") ?: "1,000,000,000"

            Result.success(
                TokenContractInfo(
                    address = cleanAddr,
                    name = name,
                    symbol = symbol,
                    decimals = decimals,
                    totalSupply = totalSupply,
                    isVerifiedContract = isContract
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callStringMethod(rpcUrl: String, contractAddress: String, selector: String): String? {
        return try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_call")
                put("params", JSONArray().put(JSONObject().apply {
                    put("to", contractAddress)
                    put("data", selector)
                }).put("latest"))
                put("id", 20)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            val hexResult = resObj.optString("result", "").removePrefix("0x")
            if (hexResult.length >= 64) {
                decodeAbiString(hexResult)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun callUint8Method(rpcUrl: String, contractAddress: String, selector: String): Int? {
        return try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_call")
                put("params", JSONArray().put(JSONObject().apply {
                    put("to", contractAddress)
                    put("data", selector)
                }).put("latest"))
                put("id", 21)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            val hexResult = resObj.optString("result", "").removePrefix("0x")
            if (hexResult.isNotEmpty()) {
                BigInteger(hexResult, 16).toInt()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun callUint256Method(rpcUrl: String, contractAddress: String, selector: String): String? {
        return try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_call")
                put("params", JSONArray().put(JSONObject().apply {
                    put("to", contractAddress)
                    put("data", selector)
                }).put("latest"))
                put("id", 22)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            val hexResult = resObj.optString("result", "").removePrefix("0x")
            if (hexResult.isNotEmpty()) {
                val value = BigInteger(hexResult, 16)
                formatBigNumber(value)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeAbiString(hex: String): String {
        return try {
            // ABI string format: offset (32 bytes), length (32 bytes), data (length bytes padded)
            if (hex.length < 128) {
                // simple direct ASCII fallback
                val clean = hex.replace("00", "")
                return String(clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()).trim()
            }
            val lengthHex = hex.substring(64, 128)
            val length = BigInteger(lengthHex, 16).toInt()
            if (length <= 0 || length > 100) return "Token"
            val dataHex = hex.substring(128, 128 + (length * 2).coerceAtMost(hex.length - 128))
            val bytes = dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            String(bytes, Charsets.UTF_8).filter { it.isLetterOrDigit() || it.isWhitespace() || it == '-' || it == '_' }.trim()
        } catch (_: Exception) {
            "Custom Token"
        }
    }

    private fun formatBigNumber(bi: BigInteger): String {
        val str = bi.toString()
        return if (str.length > 18) {
            val normal = str.substring(0, str.length - 18)
            "%,d".format(normal.toLongOrNull() ?: 1000000L)
        } else {
            "%,d".format(bi.toLong())
        }
    }

    suspend fun fetchLiveMarketPrices(): Result<Map<String, LiveTokenPrice>> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, LiveTokenPrice>()

        // 1. Primary: Binance Data Vision Public Market API (fast, reliable, no API key required)
        try {
            val binanceUrl = "https://data-api.binance.vision/api/v3/ticker/24hr?symbols=%5B%22ETHUSDT%22,%22BNBUSDT%22,%22SOLUSDT%22,%22POLUSDT%22,%22BTCUSDT%22,%22CAKEUSDT%22%5D"
            val body = executeGet(binanceUrl)
            val arr = JSONArray(body)
            val pairToSymbol = mapOf(
                "ETHUSDT" to "ETH",
                "BNBUSDT" to "BNB",
                "SOLUSDT" to "SOL",
                "POLUSDT" to "POL",
                "BTCUSDT" to "BTCB",
                "CAKEUSDT" to "CAKE"
            )
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val pair = item.optString("symbol")
                val sym = pairToSymbol[pair] ?: continue
                val usd = item.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0
                val chg = item.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0
                if (usd > 0.0) {
                    map[sym] = LiveTokenPrice(usd = usd, change24h = chg)
                }
            }
            if (map.isNotEmpty()) {
                map["USDT"] = LiveTokenPrice(usd = 1.0, change24h = 0.01)
            }
        } catch (_: Exception) {
            // Continue to CoinGecko / CryptoCompare for missing symbols
        }

        // 2. Secondary: CoinGecko Public Simple Price API (fills CORE, USDT & any missing pairs)
        if (!map.containsKey("CORE") || !map.containsKey("ETH") || !map.containsKey("BNB")) {
            try {
                val cgUrl = "https://api.coingecko.com/api/v3/simple/price?ids=ethereum,binancecoin,polygon-ecosystem-token,matic-network,coredaoorg,solana,tether,bitcoin,pancakeswap-token&vs_currencies=usd&include_24hr_change=true"
                val body = executeGet(cgUrl)
                val json = JSONObject(body)

                fun putIfMissing(symbol: String, id: String) {
                    if (map.containsKey(symbol)) return
                    val obj = json.optJSONObject(id) ?: return
                    val usd = obj.optDouble("usd", 0.0)
                    if (usd > 0.0) {
                        val chg = obj.optDouble("usd_24h_change", 0.0)
                        map[symbol] = LiveTokenPrice(usd = usd, change24h = chg)
                    }
                }

                putIfMissing("ETH", "ethereum")
                putIfMissing("BNB", "binancecoin")
                putIfMissing("POL", "polygon-ecosystem-token")
                putIfMissing("POL", "matic-network")
                putIfMissing("CORE", "coredaoorg")
                putIfMissing("SOL", "solana")
                putIfMissing("USDT", "tether")
                putIfMissing("BTCB", "bitcoin")
                putIfMissing("CAKE", "pancakeswap-token")
            } catch (_: Exception) {
                // Fall through to CryptoCompare fallback
            }
        }

        // 3. Fallback: CryptoCompare Multi-Symbol Price API
        if (!map.containsKey("ETH") || !map.containsKey("BNB") || !map.containsKey("CORE")) {
            try {
                val ccUrl = "https://min-api.cryptocompare.com/data/pricemultifull?fsyms=ETH,BNB,POL,CORE,SOL,USDT,BTC,CAKE&tsyms=USD"
                val body = executeGet(ccUrl)
                val raw = JSONObject(body).optJSONObject("RAW")
                if (raw != null) {
                    for (sym in listOf("ETH", "BNB", "POL", "CORE", "SOL", "USDT", "CAKE")) {
                        if (map.containsKey(sym)) continue
                        val usdObj = raw.optJSONObject(sym)?.optJSONObject("USD") ?: continue
                        val usd = usdObj.optDouble("PRICE", 0.0)
                        if (usd > 0.0) {
                            map[sym] = LiveTokenPrice(
                                usd = usd,
                                change24h = usdObj.optDouble("CHANGEPCT24HOUR", 0.0)
                            )
                        }
                    }
                    if (!map.containsKey("BTCB")) {
                        raw.optJSONObject("BTC")?.optJSONObject("USD")?.let { btcObj ->
                            val usd = btcObj.optDouble("PRICE", 0.0)
                            if (usd > 0.0) {
                                map["BTCB"] = LiveTokenPrice(
                                    usd = usd,
                                    change24h = btcObj.optDouble("CHANGEPCT24HOUR", 0.0)
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Handled below
            }
        }

        if (map.isNotEmpty()) {
            Result.success(map)
        } else {
            Result.failure(Exception("Live price feeds unavailable"))
        }
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Empty response body")
        }
    }

    private fun executeRpc(rpcUrl: String, jsonBody: String): String {
        val request = Request.Builder()
            .url(rpcUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Empty response body")
        }
    }
}

data class TokenContractInfo(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val totalSupply: String,
    val isVerifiedContract: Boolean
)

data class LiveTokenPrice(
    val usd: Double,
    val change24h: Double
)
