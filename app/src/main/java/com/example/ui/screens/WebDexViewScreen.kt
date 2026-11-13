package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkBackground

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebDexViewScreen() {
    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>TataSwap – Decentralized Exchange</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <style>
            body { background-color: #0b0e14; color: #f5f6f8; font-family: system-ui, -apple-system, sans-serif; }
            .gold-gradient { background: linear-gradient(135deg, #F0B90B, #FFDE7A); }
            .cyan-gradient { background: linear-gradient(135deg, #00F2FE, #4FACFE); }
            .card-bg { background-color: #131823; border: 1px solid #263249; }
          </style>
        </head>
        <body class="p-4 flex flex-col items-center">
          <header class="w-full max-w-xl flex justify-between items-center py-4 border-b border-gray-800 mb-6">
            <div class="flex items-center space-x-2">
              <div class="w-9 h-9 rounded-full gold-gradient flex items-center justify-center font-black text-black">T</div>
              <div>
                <h1 class="text-xl font-bold tracking-tight">TataSwap</h1>
                <span class="text-xs text-yellow-400 font-semibold">BNB Smart Chain DEX v3</span>
              </div>
            </div>
            <div class="flex space-x-2">
              <span class="px-3 py-1 bg-gray-900 border border-gray-700 text-xs font-bold rounded-full text-green-400 flex items-center">
                <span class="w-2 h-2 rounded-full bg-green-400 inline-block mr-1.5"></span>BSC Testnet
              </span>
              <button onclick="connectWeb3()" id="walletBtn" class="px-3 py-1 bg-yellow-500 hover:bg-yellow-400 text-black font-bold text-xs rounded-full">
                0x71C...a49B
              </button>
            </div>
          </header>

          <main class="w-full max-w-xl space-y-6">
            <!-- Swap Card -->
            <div class="card-bg rounded-2xl p-5 shadow-2xl">
              <div class="flex justify-between items-center mb-4">
                <h2 class="text-lg font-bold">Instant Swap</h2>
                <span class="text-xs text-gray-400">Slippage: <strong class="text-yellow-400">0.5%</strong></span>
              </div>

              <!-- Pay -->
              <div class="bg-gray-900 border border-gray-800 rounded-xl p-3 mb-2">
                <div class="flex justify-between text-xs text-gray-400 mb-1">
                  <span>You Pay</span>
                  <span>Balance: 3.4520 BNB</span>
                </div>
                <div class="flex justify-between items-center">
                  <input type="number" id="payAmount" value="1.0" oninput="calculateSwap()" class="bg-transparent text-2xl font-bold focus:outline-none w-3/5" />
                  <span class="bg-gray-800 px-3 py-1 rounded-full text-sm font-bold flex items-center text-yellow-400">
                    BNB ▾
                  </span>
                </div>
                <div class="text-xs text-gray-500 mt-1">~$624.50 USD</div>
              </div>

              <div class="flex justify-center -my-2 relative z-10">
                <button onclick="invertSwap()" class="w-8 h-8 rounded-full bg-gray-800 border border-yellow-500 text-yellow-500 flex items-center justify-center font-bold text-sm">
                  ⇅
                </button>
              </div>

              <!-- Receive -->
              <div class="bg-gray-900 border border-gray-800 rounded-xl p-3 mt-2 mb-4">
                <div class="flex justify-between text-xs text-gray-400 mb-1">
                  <span>You Receive</span>
                  <span>Balance: 150.00 USDT</span>
                </div>
                <div class="flex justify-between items-center">
                  <span id="receiveAmount" class="text-2xl font-bold text-green-400">622.62</span>
                  <span class="bg-gray-800 px-3 py-1 rounded-full text-sm font-bold flex items-center text-green-400">
                    USDT ▾
                  </span>
                </div>
                <div class="text-xs text-gray-500 mt-1">~$622.62 USD</div>
              </div>

              <!-- AMM details -->
              <div class="bg-gray-900/60 rounded-lg p-3 text-xs space-y-1.5 text-gray-400 mb-4 border border-gray-800">
                <div class="flex justify-between">
                  <span>Exchange Rate</span>
                  <span class="text-white font-medium">1 BNB = 624.50 USDT</span>
                </div>
                <div class="flex justify-between">
                  <span>Trading Fee (0.25%)</span>
                  <span class="text-white font-medium">0.0025 BNB</span>
                </div>
                <div class="flex justify-between">
                  <span>Price Impact</span>
                  <span class="text-green-400 font-medium">&lt; 0.01%</span>
                </div>
                <div class="flex justify-between">
                  <span>Minimum Received</span>
                  <span class="text-white font-medium">619.50 USDT</span>
                </div>
              </div>

              <button onclick="doSwap()" id="swapBtn" class="w-full py-3.5 rounded-xl gold-gradient text-black font-extrabold text-base shadow-lg hover:opacity-90">
                Swap Tokens
              </button>
              <div id="statusMsg" class="mt-2 text-center text-xs text-green-400 font-bold hidden"></div>
            </div>

            <!-- Permissionless Listing Box -->
            <div class="card-bg rounded-2xl p-5 border-yellow-500/30">
              <div class="flex items-center space-x-2 mb-2">
                <span class="text-lg">⚡</span>
                <h3 class="font-bold text-base text-yellow-400">Permissionless Token Listing</h3>
              </div>
              <p class="text-xs text-gray-400 mb-3">
                List any BEP-20 token instantly. On-chain listing fee: <strong class="text-white">5 USDT</strong> automatically transferred to DEX Owner:
                <span class="text-cyan-400 font-mono text-[10px] block mt-0.5">0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4</span>
              </p>
              <div class="flex space-x-2">
                <input id="tokenAddr" placeholder="0x BEP-20 Token Address" class="bg-gray-900 border border-gray-800 rounded-lg px-3 py-2 text-xs flex-1 text-white focus:outline-none" />
                <button onclick="listToken()" class="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-black font-bold text-xs rounded-lg">
                  Pay 5 USDT & List
                </button>
              </div>
              <div id="listMsg" class="mt-2 text-xs text-green-400 hidden"></div>
            </div>

            <!-- Protocol Highlights -->
            <div class="grid grid-cols-2 gap-3 text-center">
              <div class="card-bg p-3 rounded-xl">
                <div class="text-xs text-gray-400">Total Value Locked</div>
                <div class="text-lg font-bold text-white mt-1">$12,450,800</div>
              </div>
              <div class="card-bg p-3 rounded-xl">
                <div class="text-xs text-gray-400">24h Volume</div>
                <div class="text-lg font-bold text-green-400 mt-1">$3,840,250</div>
              </div>
            </div>
          </main>

          <footer class="mt-8 text-center text-xs text-gray-600">
            TataSwap DEX • Decentralized & Permissionless • BNB Smart Chain Testnet & Mainnet
          </footer>

          <script>
            function calculateSwap() {
              const val = parseFloat(document.getElementById('payAmount').value) || 0;
              const out = (val * 624.50 * 0.997).toFixed(2);
              document.getElementById('receiveAmount').innerText = out;
            }
            function invertSwap() {
              const p = document.getElementById('payAmount').value;
              const r = document.getElementById('receiveAmount').innerText;
              document.getElementById('payAmount').value = (parseFloat(r) / 624.50).toFixed(4);
              calculateSwap();
            }
            function doSwap() {
              const btn = document.getElementById('swapBtn');
              const msg = document.getElementById('statusMsg');
              btn.innerText = "Broadcasting to BSC RPC...";
              setTimeout(() => {
                btn.innerText = "Swap Tokens";
                msg.innerText = "✓ Swap Successful! Tx: 0x9b4f..." + Math.random().toString(36).substring(2, 8);
                msg.classList.remove('hidden');
              }, 1200);
            }
            function listToken() {
              const addr = document.getElementById('tokenAddr').value;
              const msg = document.getElementById('listMsg');
              if (!addr.startsWith("0x")) {
                alert("Please enter a valid 0x BEP-20 address.");
                return;
              }
              msg.innerText = "✓ 5 USDT Listing Fee paid to 0xD9e5...CA4! Token listed on TataSwap.";
              msg.classList.remove('hidden');
            }
            function connectWeb3() {
              alert("Connected to MetaMask on BNB Smart Chain Testnet (Chain ID 97)");
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadDataWithBaseURL("https://tataswap.dex", htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
