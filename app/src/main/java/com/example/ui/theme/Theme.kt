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

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedText,
    tertiary = SophisticatedAccent,
    background = SophisticatedBackground,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    onSurfaceVariant = SophisticatedTextMuted
  )

private val LightColorScheme = DarkColorScheme // Strictly enforce Sophisticated Dark!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Dynamic color is disabled by default to preserve custom theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> DarkColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
