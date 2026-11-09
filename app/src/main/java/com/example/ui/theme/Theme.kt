package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KepengDarkColorScheme = darkColorScheme(
    primary = NeonCrimson,
    onPrimary = TextPrimary,
    primaryContainer = DeepCarmine,
    onPrimaryContainer = TextPrimary,
    secondary = DeepCarmine,
    onSecondary = TextPrimary,
    secondaryContainer = CrimsonContainer,
    onSecondaryContainer = NeonCrimson,
    tertiary = TacticalMint,
    onTertiary = BackgroundRoot,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = TacticalMint,
    background = BackgroundRoot,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineHairline,
    outlineVariant = OutlineBorder,
    error = NeonCrimson,
    onError = TextPrimary
)

@Composable
fun KepengTheme(
    darkTheme: Boolean = true, // KEPENG is built with a sovereign dark cockpit design system
    content: @Composable () -> Unit
) {
    val colorScheme = KepengDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BackgroundRoot.toArgb()
                window.navigationBarColor = BackgroundRoot.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KepengTypography,
        content = content
    )
}
