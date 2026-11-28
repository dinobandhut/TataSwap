package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OhlcCandle
import com.example.data.model.OhlcChartUiState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SwapOhlcChartCard(
    ohlcState: OhlcChartUiState,
    onSelectInterval: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCandles by remember { mutableStateOf(true) }
    var selectedIndex by remember(ohlcState.history) { mutableStateOf<Int?>(null) }

    val history = ohlcState.history
    val candles = history?.candles.orEmpty()
    val inspectedCandle: OhlcCandle? = selectedIndex?.let { idx ->
        candles.getOrNull(idx)
    } ?: candles.lastOrNull()

    val isPositive = (history?.changePercent ?: 0.0) >= 0.0
    val changeColor = if (isPositive) CryptoGreen else CryptoRed

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("swap_ohlc_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pair & Price Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = history?.pair ?: ohlcState.pair,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        if (history != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = changeColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${if (isPositive) "+" else ""}${"%.2f".format(history.changePercent)}%",
                                    color = changeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = history?.let { "${formatOhlcNumber(it.currentPrice)} ${it.quoteSymbol} per ${it.baseSymbol}" }
                            ?: "Fetching backend OHLC history...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BnbGold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = when (history?.source) {
                                "binance" -> "Backend • Binance"
                                "cryptocompare" -> "Backend • CC"
                                "coingecko" -> "Backend • CG"
                                "protocol_pool" -> "Pool Feed"
                                else -> "OHLC Feed"
                            },
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("ohlc_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh OHLC chart",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Timeframes + Chart Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(2.dp)
                ) {
                    listOf("15m" to "15m", "1h" to "1H", "4h" to "4H", "1d" to "1D").forEach { (key, label) ->
                        val active = ohlcState.interval == key
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) BnbGold else Color.Transparent)
                                .clickable { onSelectInterval(key) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("ohlc_interval_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(2.dp)
                ) {
                    listOf(true to "Candles", false to "Line").forEach { (candlesMode, label) ->
                        val active = showCandles == candlesMode
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { showCandles = candlesMode }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("ohlc_mode_${label.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // O / H / L / C Readout Bar
            if (inspectedCandle != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OhlcMetric("O", formatOhlcNumber(inspectedCandle.open), TextPrimary)
                        OhlcMetric("H", formatOhlcNumber(inspectedCandle.high), CryptoGreen)
                        OhlcMetric("L", formatOhlcNumber(inspectedCandle.low), CryptoRed)
                        OhlcMetric("C", formatOhlcNumber(inspectedCandle.close), BnbGold)
                        Text(
                            text = formatOhlcTimestamp(inspectedCandle.timestampMs, ohlcState.interval),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Interactive OHLC Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBackground)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    ohlcState.isLoading && candles.isEmpty() -> {
                        CircularProgressIndicator(color = BnbGold, modifier = Modifier.size(28.dp))
                    }
                    candles.isEmpty() -> {
                        Text(
                            text = ohlcState.errorMessage ?: "No OHLC candles available for this pair",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    else -> {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(candles) {
                                    detectTapGestures { offset ->
                                        val idx = ((offset.x / size.width.coerceAtLeast(1)) * candles.size)
                                            .toInt()
                                            .coerceIn(0, candles.lastIndex)
                                        selectedIndex = idx
                                    }
                                }
                                .pointerInput(candles) {
                                    detectDragGestures(
                                        onDrag = { change, _ ->
                                            val idx = ((change.position.x / size.width.coerceAtLeast(1)) * candles.size)
                                                .toInt()
                                                .coerceIn(0, candles.lastIndex)
                                            selectedIndex = idx
                                        },
                                        onDragEnd = { selectedIndex = null },
                                        onDragCancel = { selectedIndex = null }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val minP = candles.minOf { it.low }
                            val maxP = candles.maxOf { it.high }
                            val pad = maxOf((maxP - minP) * 0.14, maxP * 0.004, 1e-6)
                            val lowBound = maxOf(0.0, minP - pad * 1.4)
                            val highBound = maxP + pad
                            val range = (highBound - lowBound).coerceAtLeast(1e-9)

                            fun yOf(price: Double): Float {
                                val ratio = ((price - lowBound) / range).toFloat()
                                return h - (ratio * h)
                            }

                            // Horizontal gridlines
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            for (step in 1..3) {
                                val gy = h * (step / 4f)
                                drawLine(
                                    color = DarkSurfaceBorder,
                                    start = Offset(0f, gy),
                                    end = Offset(w, gy),
                                    strokeWidth = 1.5f,
                                    pathEffect = dashEffect
                                )
                            }

                            val maxVol = candles.maxOf { it.volume }.coerceAtLeast(1.0)
                            val slotW = w / candles.size.coerceAtLeast(1)
                            val bodyW = (slotW * 0.62f).coerceAtLeast(3f)

                            // Volume bars at bottom
                            candles.forEachIndexed { idx, c ->
                                val barH = ((c.volume / maxVol) * (h * 0.18)).toFloat().coerceAtLeast(2f)
                                val isBull = c.close >= c.open
                                drawRect(
                                    color = (if (isBull) CryptoGreen else CryptoRed).copy(alpha = 0.18f),
                                    topLeft = Offset(idx * slotW + (slotW - bodyW) / 2f, h - barH),
                                    size = Size(bodyW, barH)
                                )
                            }

                            if (showCandles) {
                                candles.forEachIndexed { idx, c ->
                                    val centerX = idx * slotW + slotW / 2f
                                    val isBull = c.close >= c.open
                                    val color = if (isBull) CryptoGreen else CryptoRed
                                    drawLine(
                                        color = color,
                                        start = Offset(centerX, yOf(c.high)),
                                        end = Offset(centerX, yOf(c.low)),
                                        strokeWidth = 2f,
                                        cap = StrokeCap.Round
                                    )
                                    val topY = yOf(maxOf(c.open, c.close))
                                    val botY = yOf(minOf(c.open, c.close))
                                    val candleH = (botY - topY).coerceAtLeast(3f)
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(centerX - bodyW / 2f, topY),
                                        size = Size(bodyW, candleH)
                                    )
                                }
                            } else {
                                val lineColor = if (isPositive) CryptoGreen else CryptoRed
                                val linePath = Path()
                                val fillPath = Path()
                                candles.forEachIndexed { idx, c ->
                                    val px = idx * slotW + slotW / 2f
                                    val py = yOf(c.close)
                                    if (idx == 0) {
                                        linePath.moveTo(px, py)
                                        fillPath.moveTo(px, h)
                                        fillPath.lineTo(px, py)
                                    } else {
                                        linePath.lineTo(px, py)
                                        fillPath.lineTo(px, py)
                                    }
                                    if (idx == candles.lastIndex) {
                                        fillPath.lineTo(px, h)
                                        fillPath.close()
                                    }
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(lineColor.copy(alpha = 0.32f), lineColor.copy(alpha = 0.02f))
                                    )
                                )
                                drawPath(
                                    path = linePath,
                                    color = lineColor,
                                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                                )
                            }

                            // Highlight selected crosshair if scrubbing
                            selectedIndex?.let { selIdx ->
                                candles.getOrNull(selIdx)?.let { selCandle ->
                                    val sx = selIdx * slotW + slotW / 2f
                                    val sy = yOf(selCandle.close)
                                    drawLine(
                                        color = BnbGold.copy(alpha = 0.7f),
                                        start = Offset(sx, 0f),
                                        end = Offset(sx, h),
                                        strokeWidth = 2f,
                                        pathEffect = dashEffect
                                    )
                                    drawCircle(color = BnbGold, radius = 6f, center = Offset(sx, sy))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OhlcMetric(label: String, value: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

private fun formatOhlcNumber(v: Double): String {
    return when {
        v >= 1000.0 -> "%,.2f".format(v)
        v >= 1.0 -> "%.4f".format(v)
        v >= 0.01 -> "%.5f".format(v)
        else -> "%.6f".format(v)
    }
}

private fun formatOhlcTimestamp(tsMs: Long, interval: String): String {
    if (tsMs <= 0L) return "--"
    val pattern = if (interval == "1d") "MMM dd" else "HH:mm"
    return SimpleDateFormat(pattern, Locale.US).format(Date(tsMs))
}
