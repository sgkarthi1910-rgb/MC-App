package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val TgzDarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldDim,
    onPrimaryContainer = EmeraldBright,
    secondary = MinecraftGold,
    onSecondary = OnGoldDark,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = MinecraftGold,
    tertiary = DiscordBlurple,
    onTertiary = Color.White,
    tertiaryContainer = DiscordBlurpleContainer,
    onTertiaryContainer = Color(0xFFDCE0FF),
    error = NetherRed,
    onError = Color.White,
    errorContainer = NetherRedContainer,
    onErrorContainer = NetherRedBright,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = DeepCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = Color(0xFF0D1210),
    surfaceContainer = DeepCharcoal,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle
)

val TgzShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun TgzLifestealTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = TgzDarkColorScheme,
        typography = TgzTypography,
        shapes = TgzShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    TgzLifestealTheme(content = content)
}
