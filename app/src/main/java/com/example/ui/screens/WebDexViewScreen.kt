package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebDexViewScreen() {
    val context = LocalContext.current
    val liveDexUrl = "https://tataswap.onrender.com/dex.html"
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun launchExternalUri(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Quick Web3 Launcher Bar (opens native Chrome / MetaMask / Trust Wallet)
        Surface(
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TataSwap Live Web DEX",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BnbGold
                    )
                    Text(
                        text = "tataswap.onrender.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoGreen
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { launchExternalUri("https://link.metamask.io/dapp/tataswap.onrender.com/dex.html") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("MetaMask", fontSize = 11.sp, color = BnbGold, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { launchExternalUri("https://link.trustwallet.com/open_url?coin_id=20000714&url=$liveDexUrl") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Trust", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { launchExternalUri(liveDexUrl) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Chrome", tint = BnbGold, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            val walletPrefixes = listOf(
                                "metamask:",
                                "trust:",
                                "cbwallet:",
                                "bnc:",
                                "okx:",
                                "bitkeep:",
                                "rainbow:",
                                "safepalwallet:",
                                "tpoutside:",
                                "tpdapp:",
                                "wc:",
                                "ethereum:",
                                "intent:",
                                "https://link.metamask.io/",
                                "https://link.trustwallet.com/",
                                "https://go.cb-w.com/",
                                "https://www.okx.com/download",
                                "https://bkcode.vip",
                                "https://rnbwapp.com",
                                "https://link.safepal.io/",
                                "https://www.binance.com/en/web3wallet"
                            )
                            if (walletPrefixes.any { url.startsWith(it, ignoreCase = true) }) {
                                launchExternalUri(url)
                                return true
                            }
                            return false
                        }
                    }
                    webViewRef = this
                    loadUrl(liveDexUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
