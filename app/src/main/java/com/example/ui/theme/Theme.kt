package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    DARK,        // Default Cinematic Dark
    BLACK,       // AMOLED Pure Black
    BLUE_ACCENT, // Electric Blue Accent Dark
    LIGHT,       // Clean Light
    SYSTEM
}

private val CinematicDarkColorScheme = darkColorScheme(
    primary = GMKElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = GMKSubtlePurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A148C),
    onSecondaryContainer = Color(0xFFE1BEE7),
    background = GMKBackgroundDark,
    onBackground = GMKTextWhite,
    surface = GMKSurfaceDark,
    onSurface = GMKTextWhite,
    surfaceVariant = GMKSurfaceVariantDark,
    onSurfaceVariant = GMKTextMuted,
    outline = Color(0xFF1E283A)
)

private val AmoledBlackColorScheme = darkColorScheme(
    primary = GMKNeonBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0039CB),
    onPrimaryContainer = Color.White,
    secondary = GMKSubtlePurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF311B92),
    onSecondaryContainer = Color.White,
    background = Color(0xFF000000),
    onBackground = GMKTextWhite,
    surface = Color(0xFF070A0F),
    onSurface = GMKTextWhite,
    surfaceVariant = Color(0xFF10141D),
    onSurfaceVariant = GMKTextMuted,
    outline = Color(0xFF1A1F2C)
)

private val BlueAccentColorScheme = darkColorScheme(
    primary = Color(0xFF00B0FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF01579B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2979FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = Color(0xFFBBDEFB),
    background = Color(0xFF050B14),
    onBackground = GMKTextWhite,
    surface = Color(0xFF0C1422),
    onSurface = GMKTextWhite,
    surfaceVariant = Color(0xFF131F33),
    onSurfaceVariant = Color(0xFF90CAF9),
    outline = Color(0xFF1E355B)
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
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> CinematicDarkColorScheme
        AppThemeMode.BLACK -> AmoledBlackColorScheme
        AppThemeMode.BLUE_ACCENT -> BlueAccentColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.SYSTEM -> if (isSystemDark) CinematicDarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
