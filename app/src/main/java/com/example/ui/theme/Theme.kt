package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighDensityColorScheme = lightColorScheme(
    primary = DensePrimary,
    secondary = DenseAccent,
    tertiary = DenseGreenText,
    background = DenseBg,
    surface = DenseSurface,
    surfaceVariant = DenseBorder,
    onPrimary = Color.White,
    onSecondary = DenseTextPrimary,
    onTertiary = Color.White,
    onBackground = DenseTextPrimary,
    onSurface = DenseTextPrimary
)

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberBlue,
    secondary = CyberTeal,
    tertiary = CyberLime,
    background = SlateBg,
    surface = SlateCard,
    surfaceVariant = SlateCardVariant,
    onPrimary = SlateBg,
    onSecondary = SlateBg,
    onTertiary = SlateBg,
    onBackground = TextWhite,
    onSurface = TextWhite
  )

private val LightColorScheme = HighDensityColorScheme // High Density Light theme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to maintain our branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = HighDensityColorScheme // Strictly enforce High Density styling

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
