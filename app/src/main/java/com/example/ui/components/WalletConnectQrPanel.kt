package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.DexRepository
import com.example.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WalletConnectQrPanel(
    projectId: String = DexRepository.WALLETCONNECT_PROJECT_ID,
    onConnectedAddress: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var sessionNonce by remember { mutableIntStateOf(0) }
    var wcUri by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedUri by remember { mutableStateOf(false) }

    LaunchedEffect(wcUri) {
        val uri = wcUri
        if (uri.isNullOrBlank()) {
            qrBitmap = null
        } else {
            qrBitmap = withContext(Dispatchers.Default) {
                encodeQrBitmap(uri, sizePx = 560)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("walletconnect_qr_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "WalletConnect QR",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "WalletConnect QR & WC URL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Scan QR or open wc: link in mobile wallet",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = {
                        isGenerating = true
                        errorMessage = null
                        wcUri = null
                        copiedUri = false
                        sessionNonce++
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_wc_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh WalletConnect QR",
                        tint = NeonCyan
                    )
                }
            }

            // Hidden WebView pairing bridge generating live WalletConnect v2 wc:... URI
            key(sessionNonce, projectId) {
                WalletConnectPairingBridge(
                    projectId = projectId,
                    onUriGenerated = { uri ->
                        wcUri = uri
                        isGenerating = false
                        errorMessage = null
                    },
                    onConnected = { addr ->
                        onConnectedAddress(addr)
                    },
                    onError = { err ->
                        isGenerating = false
                        if (wcUri.isNullOrBlank()) {
                            errorMessage = err
                        }
                    }
                )
            }

            if (isGenerating && wcUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Generating WalletConnect QR & wc: URL...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            } else if (errorMessage != null && wcUri.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CryptoRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CryptoRed.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "Unable to connect to WalletConnect relay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoRed
                        )
                        OutlinedButton(
                            onClick = {
                                isGenerating = true
                                errorMessage = null
                                sessionNonce++
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Retry WC Pairing", color = NeonCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            val currentUri = wcUri
            val bitmap = qrBitmap
            if (!currentUri.isNullOrBlank()) {
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "WalletConnect v2 Pairing QR Code",
                                modifier = Modifier
                                    .size(184.dp)
                                    .testTag("walletconnect_qr_image")
                            )
                            Text(
                                text = "Scan with MetaMask, Trust, Binance, OKX or Bitget",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // WC URL Readout Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "WalletConnect URL (wc:...)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = currentUri,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = NeonCyan,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("walletconnect_wc_url_text")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentUri))
                            copiedUri = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_wc_url_button")
                    ) {
                        Icon(
                            imageVector = if (copiedUri) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy WC URL",
                            tint = if (copiedUri) CryptoGreen else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copiedUri) "Copied" else "Copy WC URL",
                            fontSize = 12.sp,
                            color = if (copiedUri) CryptoGreen else NeonCyan
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUri))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                errorMessage = "No installed wallet app handles wc: links directly; scan the QR or copy WC URL."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("open_wc_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open WC URL",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Wallet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WalletConnectPairingBridge(
    projectId: String,
    onUriGenerated: (String) -> Unit,
    onConnected: (String) -> Unit,
    onError: (String) -> Unit
) {
    val latestOnUri by rememberUpdatedState(onUriGenerated)
    val latestOnConnected by rememberUpdatedState(onConnected)
    val latestOnError by rememberUpdatedState(onError)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    val safeProjectId = remember(projectId) {
        projectId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    }

    val bridgeHtml = remember(safeProjectId) {
        buildWalletConnectBridgeHtml(safeProjectId)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            latestOnError("Network error while initializing WalletConnect.")
                        }
                    }
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onDisplayUri(uri: String) {
                            post { latestOnUri(uri) }
                        }

                        @JavascriptInterface
                        fun onWalletConnected(address: String) {
                            post { latestOnConnected(address) }
                        }

                        @JavascriptInterface
                        fun onPairingError(msg: String) {
                            post { latestOnError(msg) }
                        }
                    },
                    "AndroidWcBridge"
                )
                loadDataWithBaseURL("https://tataswap.onrender.com", bridgeHtml, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
    )
}

private fun buildWalletConnectBridgeHtml(projectId: String): String = """
<!DOCTYPE html>
<html>
<head><meta charset="utf-8" /></head>
<body>
<script type="module">
  async function loadProviderModule() {
    const urls = [
      "https://esm.sh/@walletconnect/ethereum-provider@2.26.1",
      "https://cdn.jsdelivr.net/npm/@walletconnect/ethereum-provider@2.26.1/+esm"
    ];
    let lastErr = null;
    for (const url of urls) {
      try {
        return await import(url);
      } catch (e) {
        lastErr = e;
      }
    }
    throw lastErr || new Error("Could not load WalletConnect provider module.");
  }

  (async () => {
    try {
      const { EthereumProvider } = await loadProviderModule();
      const provider = await EthereumProvider.init({
        projectId: "$projectId",
        chains: [810161],
        optionalChains: [1, 56, 137, 1116, 42161, 8453],
        rpcMap: {
          810161: "https://rpc.tataseed.org",
          1: "https://eth.llamarpc.com",
          56: "https://bsc-dataseed.binance.org",
          137: "https://polygon-rpc.com",
          1116: "https://rpc.coredao.org",
          42161: "https://arb1.arbitrum.io/rpc",
          8453: "https://mainnet.base.org"
        },
        showQrModal: false,
        metadata: {
          name: "TataSwap DEX",
          description: "Multi-Chain Non-Custodial EVM DEX",
          url: "https://tataswap.onrender.com",
          icons: ["https://tataswap.onrender.com/favicon.ico"]
        }
      });

      provider.on("display_uri", (uri) => {
        if (uri && window.AndroidWcBridge) {
          window.AndroidWcBridge.onDisplayUri(uri);
        }
      });

      const accounts = await provider.connect();
      if (Array.isArray(accounts) && accounts.length > 0 && window.AndroidWcBridge) {
        window.AndroidWcBridge.onWalletConnected(accounts[0]);
      }
    } catch (err) {
      if (window.AndroidWcBridge) {
        window.AndroidWcBridge.onPairingError(err?.message || "WalletConnect pairing failed.");
      }
    }
  })();
</script>
</body>
</html>
""".trimIndent()

private fun encodeQrBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        val dark = android.graphics.Color.rgb(15, 23, 42)
        val light = android.graphics.Color.WHITE
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) dark else light
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (_: Exception) {
        null
    }
}
