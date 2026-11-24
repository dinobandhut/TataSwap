package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TataSwapColorScheme = darkColorScheme(
    primary = BnbGold,
    onPrimary = Color(0xFF1E1600),
    primaryContainer = Color(0xFF3B2F00),
    onPrimaryContainer = Color(0xFFFFDF7A),
    
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF70F5FF),
    
    tertiary = UniPink,
    onTertiary = Color(0xFF4C004F),
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    outline = DarkSurfaceBorder,
    outlineVariant = DividerDark,
    
    error = CryptoRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // TataSwap defaults to dark Web3 DEX theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TataSwapColorScheme,
        typography = Typography,
        content = content
    )
}
