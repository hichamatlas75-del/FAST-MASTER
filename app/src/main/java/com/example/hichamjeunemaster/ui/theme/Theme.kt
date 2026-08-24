package com.example.hichamjeunemaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryDark,
    tertiary = Accent,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextLightSec,
    outline = GlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = SecondaryDark,
    onSecondary = Color.White,
    tertiary = Accent,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextDarkSec,
    outline = Color(0xFFDEE2E6)
)

@Composable
fun JeuneMasterTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}