package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

private val DarkColorScheme = darkColorScheme(
    primary = NeutralDarkPrimary,
    onPrimary = Color(0xFF041E49),
    primaryContainer = NeutralDarkPrimaryContainer,
    onPrimaryContainer = NeutralDarkOnPrimaryContainer,
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF062E6F),
    secondaryContainer = Color(0xFF0842A0),
    onSecondaryContainer = Color(0xFFD3E3FD),
    background = NeutralDarkBackground,
    onBackground = NeutralDarkOnBackground,
    surface = NeutralDarkSurface,
    onSurface = NeutralDarkOnSurface,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = NeutralDarkOnSurfaceVariant,
    outline = NeutralDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NeutralLightPrimary,
    onPrimary = Color.White,
    primaryContainer = NeutralLightPrimaryContainer,
    onPrimaryContainer = NeutralLightOnPrimaryContainer,
    secondary = Color(0xFF4285F4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF041E49),
    background = NeutralLightBackground,
    onBackground = NeutralLightOnBackground,
    surface = NeutralLightSurface,
    onSurface = NeutralLightOnSurface,
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = NeutralLightOnSurfaceVariant,
    outline = NeutralLightOutline
)

@Composable
fun GMKMediaPlayerTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
