package com.erdalgunes.fidan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Neutral80,
    onTertiary = Neutral20,
    tertiaryContainer = Neutral30,
    onTertiaryContainer = Neutral90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    outline = Neutral60,
    inverseOnSurface = Neutral10,
    inverseSurface = Neutral90,
    inversePrimary = Teal40,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Teal99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Sage40,
    onSecondary = Sage99,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Neutral40,
    onTertiary = Neutral99,
    tertiaryContainer = Neutral90,
    onTertiaryContainer = Neutral10,
    error = Error40,
    onError = Error99,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral30,
    outline = Neutral50,
    inverseOnSurface = Neutral95,
    inverseSurface = Neutral20,
    inversePrimary = Teal80,
)

/**
 * Fidan Material 3 theme with productivity-optimized colors.
 * 
 * Color scheme features:
 * - Teal primary colors for enhanced focus and concentration
 * - Sage green secondary colors for balance and nature connection
 * - Follows WCAG AA accessibility guidelines for contrast ratios
 * - Supports dynamic colors on Android 12+
 */
@Composable
fun FidanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use deprecated API for older Android versions, modern API for newer ones
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.primary.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}