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

    suspend fun getBlockNumber(rpcUrl: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_blockNumber")
                put("params", JSONArray())
                put("id", 1)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            if (resObj.has("result")) {
                val hex = resObj.getString("result")
                val blockNum = java.lang.Long.decode(hex)
                Result.success(blockNum)
            } else {
                Result.failure(Exception(resObj.optJSONObject("error")?.optString("message") ?: "Unknown RPC error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNativeBalance(rpcUrl: String, walletAddress: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_getBalance")
                put("params", JSONArray().put(walletAddress).put("latest"))
                put("id", 2)
            }
            val responseStr = executeRpc(rpcUrl, payload.toString())
            val resObj = JSONObject(responseStr)
            if (resObj.has("result")) {
                val hex = resObj.getString("result").removePrefix("0x")
                val wei = if (hex.isEmpty()) BigInteger.ZERO else BigInteger(hex, 16)
                val eth = BigDecimal(wei).divide(BigDecimal("1000000000000000000"), 4, java.math.RoundingMode.HALF_UP)
                Result.success(eth.toDouble())
            } else {
                Result.failure(Exception("Failed to fetch balance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectTokenContract(rpcUrl: String, contractAddress: String): Result<TokenContractInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanAddr = contractAddress.trim()
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
