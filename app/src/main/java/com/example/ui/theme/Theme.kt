package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalCobalt,
    onPrimary = Color.White,
    primaryContainer = SoftSkyIce,
    onPrimaryContainer = DeepMidnightNavy,
    secondary = EmeraldProfit,
    onSecondary = Color.White,
    secondaryContainer = MintFrost,
    onSecondaryContainer = DeepForest,
    tertiary = AmberCommission,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberTint,
    onTertiaryContainer = DeepAmberBrown,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = SoftRose,
    onErrorContainer = DeepCrimson,
    background = AlabasterBackground,
    onBackground = InkPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = SoftSlateContainer,
    onSurfaceVariant = InkSecondaryLight,
    outline = SubtleOutlineLight,
    outlineVariant = SubtleOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricSky,
    onPrimary = DeepMidnightNavy,
    primaryContainer = RoyalNavyDark,
    onPrimaryContainer = SoftSkyIce,
    secondary = EmeraldGlow,
    onSecondary = DeepForest,
    secondaryContainer = DeepForest,
    onSecondaryContainer = MintFrost,
    tertiary = GoldAmber,
    onTertiary = DeepAmberBrown,
    tertiaryContainer = Color(0xFF5A3106),
    onTertiaryContainer = WarmAmberTint,
    error = LightCoral,
    onError = DeepCrimson,
    errorContainer = DeepCrimson,
    onErrorContainer = SoftRose,
    background = ObsidianBackground,
    onBackground = InkPrimaryDark,
    surface = SlateNavySurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = MutedNavyContainer,
    onSurfaceVariant = InkSecondaryDark,
    outline = SubtleOutlineDark,
    outlineVariant = SubtleOutlineDark
)

val JioPayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JioPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = JioPayTypography,
        shapes = JioPayShapes,
        content = content
    )
}
