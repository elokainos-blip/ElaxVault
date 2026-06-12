package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CustomDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = BackgroundDark,
    primaryContainer = BorderSlate,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = BackgroundDark,
    tertiary = AccentOrange,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = BorderSlate,
    onSurfaceVariant = TextSecondary,
    outline = SoftGray
)

private val CustomLightColorScheme = lightColorScheme(
    primary = PrimaryCyanDim,
    onPrimary = TextPrimary,
    primaryContainer = BorderSlate,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = BackgroundDark,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force custom dark aesthetic by default for optimal photo grids contrast
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CustomDarkColorScheme
        else -> CustomLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
