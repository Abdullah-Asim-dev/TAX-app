package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KineticLedgerColorScheme = darkColorScheme(
    primary = PrimaryCore,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = TextPrimary,
    inversePrimary = PrimaryLight,
    secondary = SecondaryEmerald,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = ElectricCyan,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TextPrimary,
    error = VividCrimson,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimary,
    background = CanvasDefault,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = TextSecondary,
    surfaceTint = PrimaryLight,
    outline = BorderInteractive,
    outlineVariant = BorderSubtle,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

@Composable
fun FreelanceTaxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KineticLedgerColorScheme,
        typography = Typography,
        content = content
    )
}
