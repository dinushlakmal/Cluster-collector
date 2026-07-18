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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
  )

// Royal Purple Schemes
private val RoyalPurpleLightColorScheme = lightColorScheme(
    primary = RoyalPurplePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E8FF),
    onPrimaryContainer = Color(0xFF5B21B6),
    secondary = RoyalPurpleSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF3730A3),
    background = Color(0xFFFAF5FF),
    surface = Color.White,
    onBackground = Color(0xFF1E1B4B),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563)
)

private val RoyalPurpleDarkColorScheme = darkColorScheme(
    primary = RoyalPurplePrimaryDark,
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = RoyalPurpleSecondaryDark,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0F0B1E),
    surface = Color(0xFF1A152E),
    onBackground = Color(0xFFF3E8FF),
    onSurface = Color(0xFFF3E8FF),
    surfaceVariant = Color(0xFF2C2547),
    onSurfaceVariant = Color(0xFFD1D5DB)
)

// Emerald Green Schemes
private val EmeraldGreenLightColorScheme = lightColorScheme(
    primary = EmeraldGreenPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = EmeraldGreenSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onBackground = Color(0xFF062F4F),
    onSurface = Color(0xFF062F4F),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

private val EmeraldGreenDarkColorScheme = darkColorScheme(
    primary = EmeraldGreenPrimaryDark,
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = EmeraldGreenSecondaryDark,
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF061A12),
    surface = Color(0xFF0E2A1E),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF1D3B2F),
    onSurfaceVariant = Color(0xFF91A19A)
)

// Sunset Crimson Schemes
private val SunsetCrimsonLightColorScheme = lightColorScheme(
    primary = SunsetCrimsonPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF9F1239),
    secondary = SunsetCrimsonSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    background = Color(0xFFFFF5F5),
    surface = Color.White,
    onBackground = Color(0xFF271313),
    onSurface = Color(0xFF271313),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF5F5F5F)
)

private val SunsetCrimsonDarkColorScheme = darkColorScheme(
    primary = SunsetCrimsonPrimaryDark,
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF9F1239),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = SunsetCrimsonSecondaryDark,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF1A0B0B),
    surface = Color(0xFF2D1515),
    onBackground = Color(0xFFFFE4E6),
    onSurface = Color(0xFFFFE4E6),
    surfaceVariant = Color(0xFF3F1F1F),
    onSurfaceVariant = Color(0xFFE2B2B2)
)

// Midnight Neon Schemes
private val MidnightNeonLightColorScheme = lightColorScheme(
    primary = MidnightNeonPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF9D174D),
    secondary = MidnightNeonSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF075985),
    background = Color(0xFFFFF1F2),
    surface = Color.White,
    onBackground = Color(0xFF3F0B24),
    onSurface = Color(0xFF3F0B24),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563)
)

private val MidnightNeonDarkColorScheme = darkColorScheme(
    primary = MidnightNeonPrimaryDark,
    onPrimary = Color(0xFF500724),
    primaryContainer = Color(0xFF9D174D),
    onPrimaryContainer = Color(0xFFFCE7F3),
    secondary = MidnightNeonSecondaryDark,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF14080E),
    surface = Color(0xFF240E1B),
    onBackground = Color(0xFFFCE7F3),
    onSurface = Color(0xFFFCE7F3),
    surfaceVariant = Color(0xFF381427),
    onSurfaceVariant = Color(0xFFECA3C2)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  themeName: String = "DEFAULT_TEAL",
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeName) {
    "ROYAL_PURPLE" -> if (darkTheme) RoyalPurpleDarkColorScheme else RoyalPurpleLightColorScheme
    "EMERALD_GREEN" -> if (darkTheme) EmeraldGreenDarkColorScheme else EmeraldGreenLightColorScheme
    "SUNSET_CRIMSON" -> if (darkTheme) SunsetCrimsonDarkColorScheme else SunsetCrimsonLightColorScheme
    "MIDNIGHT_NEON" -> if (darkTheme) MidnightNeonDarkColorScheme else MidnightNeonLightColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
