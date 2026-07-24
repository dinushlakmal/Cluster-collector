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

// Ocean Blue Schemes
private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBluePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = OceanBlueSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    background = Color(0xFFF0F9FF),
    surface = Color.White,
    onBackground = Color(0xFF0C4A6E),
    onSurface = Color(0xFF0C4A6E),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0369A1)
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBluePrimaryDark,
    onPrimary = Color(0xFF0C4A6E),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = OceanBlueSecondaryDark,
    onSecondary = Color(0xFF155E75),
    secondaryContainer = Color(0xFF0891B2),
    onSecondaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF031420),
    surface = Color(0xFF082235),
    onBackground = Color(0xFFE0F2FE),
    onSurface = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFF0F3854),
    onSurfaceVariant = Color(0xFF7DD3FC)
)

// Amber Gold Schemes
private val AmberGoldLightColorScheme = lightColorScheme(
    primary = AmberGoldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = AmberGoldSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    background = Color(0xFFFFFBEB),
    surface = Color.White,
    onBackground = Color(0xFF451A03),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFF92400E)
)

private val AmberGoldDarkColorScheme = darkColorScheme(
    primary = AmberGoldPrimaryDark,
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = AmberGoldSecondaryDark,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF9A3412),
    onSecondaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF1C1004),
    surface = Color(0xFF2C1C0A),
    onBackground = Color(0xFFFEF3C7),
    onSurface = Color(0xFFFEF3C7),
    surfaceVariant = Color(0xFF422812),
    onSurfaceVariant = Color(0xFFFDE68A)
)

// Cherry Blossom Schemes
private val CherryBlossomLightColorScheme = lightColorScheme(
    primary = CherryBlossomPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = CherryBlossomSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    background = Color(0xFFFFF1F2),
    surface = Color.White,
    onBackground = Color(0xFF4C0519),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFF9F1239)
)

private val CherryBlossomDarkColorScheme = darkColorScheme(
    primary = CherryBlossomPrimaryDark,
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = CherryBlossomSecondaryDark,
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFF1F040A),
    surface = Color(0xFF2E0A13),
    onBackground = Color(0xFFFFE4E6),
    onSurface = Color(0xFFFFE4E6),
    surfaceVariant = Color(0xFF481220),
    onSurfaceVariant = Color(0xFFFECDD3)
)

// Cyber Matrix Schemes
private val CyberMatrixLightColorScheme = lightColorScheme(
    primary = CyberMatrixPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = CyberMatrixSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onBackground = Color(0xFF052E16),
    onSurface = Color(0xFF052E16),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF166534)
)

private val CyberMatrixDarkColorScheme = darkColorScheme(
    primary = CyberMatrixPrimaryDark,
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = CyberMatrixSecondaryDark,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF02140A),
    surface = Color(0xFF082615),
    onBackground = Color(0xFFDCFCE7),
    onSurface = Color(0xFFDCFCE7),
    surfaceVariant = Color(0xFF0F3B22),
    onSurfaceVariant = Color(0xFF86EFAC)
)

// Nebula Indigo Schemes
private val NebulaIndigoLightColorScheme = lightColorScheme(
    primary = NebulaIndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = NebulaIndigoSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    background = Color(0xFFEEF2FF),
    surface = Color.White,
    onBackground = Color(0xFF1E1B4B),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFF3730A3)
)

private val NebulaIndigoDarkColorScheme = darkColorScheme(
    primary = NebulaIndigoPrimaryDark,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = NebulaIndigoSecondaryDark,
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF0B0A26),
    surface = Color(0xFF151442),
    onBackground = Color(0xFFE0E7FF),
    onSurface = Color(0xFFE0E7FF),
    surfaceVariant = Color(0xFF22205C),
    onSurfaceVariant = Color(0xFFC7D2FE)
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
    "OCEAN_BLUE" -> if (darkTheme) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
    "AMBER_GOLD" -> if (darkTheme) AmberGoldDarkColorScheme else AmberGoldLightColorScheme
    "CHERRY_BLOSSOM" -> if (darkTheme) CherryBlossomDarkColorScheme else CherryBlossomLightColorScheme
    "CYBER_MATRIX" -> if (darkTheme) CyberMatrixDarkColorScheme else CyberMatrixLightColorScheme
    "NEBULA_INDIGO" -> if (darkTheme) NebulaIndigoDarkColorScheme else NebulaIndigoLightColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

