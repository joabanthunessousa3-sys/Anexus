package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnexusColorScheme = darkColorScheme(
    primary = AnexusPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = AnexusPurpleContainer,
    onPrimaryContainer = Color(0xFFE9D5FF),
    
    secondary = AnexusBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = AnexusBlueContainer,
    onSecondaryContainer = Color(0xFFDBEAFE),
    
    tertiary = AnexusBlueCyan,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF083344),
    onTertiaryContainer = Color(0xFFA5F3FC),
    
    error = AnexusRecordRed,
    onError = Color.White,
    
    background = AnexusDeepBlack,
    onBackground = AnexusTextPrimary,
    
    surface = AnexusDarkSurface,
    onSurface = AnexusTextPrimary,
    surfaceVariant = AnexusSurfaceContainer,
    onSurfaceVariant = AnexusTextSecondary,
    
    outline = AnexusSurfaceBorder,
    outlineVariant = Color(0xFF1E2438)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnexusColorScheme,
        typography = Typography,
        content = content
    )
}
