package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FoundryOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = FoundryCharcoalCard,
    onPrimaryContainer = FoundryOrangePrimary,
    secondary = FoundryPurpleSecondary,
    onSecondary = Color.White,
    secondaryContainer = FoundryCharcoalCard,
    onSecondaryContainer = FoundryPurpleSecondary,
    tertiary = FoundryMagentaAccent,
    background = FoundryCharcoalDark,
    onBackground = FoundryTextPrimaryDark,
    surface = FoundryCharcoalSurface,
    onSurface = FoundryTextPrimaryDark,
    surfaceVariant = FoundryCharcoalCard,
    onSurfaceVariant = FoundryTextSecondaryDark,
    outline = FoundryBorderDark,
    outlineVariant = FoundryBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = FoundryOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE0),
    onPrimaryContainer = FoundryOrangePrimary,
    secondary = FoundryPurpleSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = FoundryPurpleSecondary,
    tertiary = FoundryMagentaAccent,
    background = FoundryLightBg,
    onBackground = FoundryTextPrimaryLight,
    surface = FoundryLightSurface,
    onSurface = FoundryTextPrimaryLight,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = FoundryTextSecondaryLight,
    outline = FoundryLightBorder,
    outlineVariant = FoundryLightBorder
)

@Composable
fun FoundryTheme(
    themePreference: String = "DARK", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

