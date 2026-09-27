package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class GlassColors(
    val surfaceGlass: Color,
    val borderGlass: Color,
    val canvasBackground: Color,
    val incomingBubble: Color,
    val outgoingBubble: Color,
    val incomingText: Color,
    val outgoingText: Color,
    val accentColor: Color,
    val isDark: Boolean = false
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        surfaceGlass = LightSurfaceGlass,
        borderGlass = LightBorderGlass,
        canvasBackground = LightCanvas,
        incomingBubble = LightBubbleIncoming,
        outgoingBubble = LightBubbleOutgoing,
        incomingText = LightTextIncoming,
        outgoingText = LightTextOutgoing,
        accentColor = Color(0xFF000000),
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    tertiary = SalimGreen,
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSubtle,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderGlass,
    outlineVariant = LightBorderSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color(0xFF1C1C1E),
    tertiary = SalimGreen,
    onTertiary = Color.White,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSubtle,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderGlass,
    outlineVariant = DarkBorderSubtle
)

private val AsglColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF0D0F12),
    primaryContainer = Color(0xFF1A232A),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0D0F12),
    tertiary = SalimEmerald,
    onTertiary = Color.White,
    background = AsglCanvas,
    onBackground = AsglTextPrimary,
    surface = AsglSurface,
    onSurface = AsglTextPrimary,
    surfaceVariant = AsglSurfaceSubtle,
    onSurfaceVariant = AsglTextSecondary,
    outline = AsglBorderGlass,
    outlineVariant = AsglBorderSubtle
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, SALIM_ASGL
}

@Composable
fun SalimTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.SALIM_ASGL -> true
    }
    val isAsgl = themeMode == ThemeMode.SALIM_ASGL

    val context = LocalContext.current
    val colorScheme = when {
        isAsgl -> AsglColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val glassColors = when {
        isAsgl -> GlassColors(
            surfaceGlass = AsglSurfaceGlass,
            borderGlass = AsglBorderGlass,
            canvasBackground = AsglCanvas,
            incomingBubble = AsglBubbleIncoming,
            outgoingBubble = AsglBubbleOutgoing,
            incomingText = AsglTextIncoming,
            outgoingText = AsglTextOutgoing,
            accentColor = SalimEmerald,
            isDark = true
        )
        isDark -> GlassColors(
            surfaceGlass = DarkSurfaceGlass,
            borderGlass = DarkBorderGlass,
            canvasBackground = DarkCanvas,
            incomingBubble = DarkBubbleIncoming,
            outgoingBubble = DarkBubbleOutgoing,
            incomingText = DarkTextIncoming,
            outgoingText = DarkTextOutgoing,
            accentColor = Color(0xFFFFFFFF),
            isDark = true
        )
        else -> GlassColors(
            surfaceGlass = LightSurfaceGlass,
            borderGlass = LightBorderGlass,
            canvasBackground = LightCanvas,
            incomingBubble = LightBubbleIncoming,
            outgoingBubble = LightBubbleOutgoing,
            incomingText = LightTextIncoming,
            outgoingText = LightTextOutgoing,
            accentColor = Color(0xFF1C1C1E),
            isDark = false
        )
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
