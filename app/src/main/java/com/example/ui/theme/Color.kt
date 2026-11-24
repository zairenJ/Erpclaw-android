package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeSeedPreset(
  val label: String,
  val seedColor: Color,
  val accentColor: Color,
) {
  EMERALD("Emerald Ledger", Color(0xFF0F766E), Color(0xFFD97706)),
  COBALT("Cobalt Ops", Color(0xFF1D4ED8), Color(0xFF0EA5E9)),
  TERRACOTTA("Terracotta Studio", Color(0xFFB45309), Color(0xFF0D9488)),
  GRAPHITE("Graphite Mono", Color(0xFF334155), Color(0xFF10B981)),
  PLUM("Plum AI", Color(0xFF6D28D9), Color(0xFFF59E0B)),
}

enum class ThemeSurfaceMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
  AMOLED("AMOLED"),
}

// Semantic status tokens derived around financial precision
val SemanticSuccessLight = Color(0xFF0F766E)
val SemanticSuccessDark = Color(0xFF5EEAD4)
val SemanticWarningLight = Color(0xFFB45309)
val SemanticWarningDark = Color(0xFFFCD34D)

fun buildLightScheme(preset: ThemeSeedPreset): ColorScheme {
  return when (preset) {
    ThemeSeedPreset.EMERALD ->
      lightColorScheme(
        primary = Color(0xFF0F766E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCFBF1),
        onPrimaryContainer = Color(0xFF042F2E),
        secondary = Color(0xFF3F6661),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD9ECE9),
        onSecondaryContainer = Color(0xFF132624),
        tertiary = Color(0xFFB45309),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF451A03),
        background = Color(0xFFF6FAF9),
        onBackground = Color(0xFF101C1A),
        surface = Color(0xFFF6FAF9),
        onSurface = Color(0xFF101C1A),
        surfaceVariant = Color(0xFFDCE7E4),
        onSurfaceVariant = Color(0xFF3F4E4B),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEFF5F3),
        surfaceContainer = Color(0xFFE9F0EE),
        surfaceContainerHigh = Color(0xFFE1E9E6),
        surfaceContainerHighest = Color(0xFFD8E2DF),
        outline = Color(0xFF6F7E7B),
        outlineVariant = Color(0xFFBEC9C6),
      )
    ThemeSeedPreset.COBALT ->
      lightColorScheme(
        primary = Color(0xFF1D4ED8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF0F1E54),
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2E8F0),
        onSecondaryContainer = Color(0xFF1E293B),
        tertiary = Color(0xFF0284C7),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0F2FE),
        onTertiaryContainer = Color(0xFF082F49),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFF8FAFC),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F5F9),
        surfaceContainer = Color(0xFFEAF0F6),
        surfaceContainerHigh = Color(0xFFE2E8F0),
        surfaceContainerHighest = Color(0xFFCBD5E1),
        outline = Color(0xFF64748B),
        outlineVariant = Color(0xFFCBD5E1),
      )
    ThemeSeedPreset.TERRACOTTA ->
      lightColorScheme(
        primary = Color(0xFFB45309),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF451A03),
        secondary = Color(0xFF785A40),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF5E6D8),
        onSecondaryContainer = Color(0xFF2D1E10),
        tertiary = Color(0xFF0F766E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCCFBF1),
        onTertiaryContainer = Color(0xFF042F2E),
        background = Color(0xFFFFFBF7),
        onBackground = Color(0xFF1F160F),
        surface = Color(0xFFFFFBF7),
        onSurface = Color(0xFF1F160F),
        surfaceVariant = Color(0xFFF1E4D8),
        onSurfaceVariant = Color(0xFF534438),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAF3EB),
        surfaceContainer = Color(0xFFF4EBE1),
        surfaceContainerHigh = Color(0xFFEDE2D6),
        surfaceContainerHighest = Color(0xFFE5D7C8),
        outline = Color(0xFF847364),
        outlineVariant = Color(0xFFD5C4B4),
      )
    ThemeSeedPreset.GRAPHITE ->
      lightColorScheme(
        primary = Color(0xFF334155),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2E8F0),
        onPrimaryContainer = Color(0xFF0F172A),
        secondary = Color(0xFF4B5563),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5E7EB),
        onSecondaryContainer = Color(0xFF111827),
        tertiary = Color(0xFF059669),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD1FAE5),
        onTertiaryContainer = Color(0xFF064E3B),
        background = Color(0xFFF9FAFB),
        onBackground = Color(0xFF111827),
        surface = Color(0xFFF9FAFB),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFE5E7EB),
        onSurfaceVariant = Color(0xFF4B5563),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF3F4F6),
        surfaceContainer = Color(0xFFECEEF1),
        surfaceContainerHigh = Color(0xFFE5E7EB),
        surfaceContainerHighest = Color(0xFFD1D5DB),
        outline = Color(0xFF6B7280),
        outlineVariant = Color(0xFFD1D5DB),
      )
    ThemeSeedPreset.PLUM ->
      lightColorScheme(
        primary = Color(0xFF6D28D9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF2E1065),
        secondary = Color(0xFF5B507A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8E2F6),
        onSecondaryContainer = Color(0xFF1F1638),
        tertiary = Color(0xFFD97706),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF451A03),
        background = Color(0xFFFAF9FE),
        onBackground = Color(0xFF1A1625),
        surface = Color(0xFFFAF9FE),
        onSurface = Color(0xFF1A1625),
        surfaceVariant = Color(0xFFE7E2F1),
        onSurfaceVariant = Color(0xFF4B4559),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF4F1FA),
        surfaceContainer = Color(0xFFEEEAF6),
        surfaceContainerHigh = Color(0xFFE6E1F0),
        surfaceContainerHighest = Color(0xFFDDD6E9),
        outline = Color(0xFF79718A),
        outlineVariant = Color(0xFFC9C1D8),
      )
  }
}

fun buildDarkScheme(preset: ThemeSeedPreset, amoled: Boolean = false): ColorScheme {
  val base =
    when (preset) {
      ThemeSeedPreset.EMERALD ->
        darkColorScheme(
          primary = Color(0xFF5EEAD4),
          onPrimary = Color(0xFF003731),
          primaryContainer = Color(0xFF0B4F48),
          onPrimaryContainer = Color(0xFF99F6E4),
          secondary = Color(0xFFB1CCC7),
          onSecondary = Color(0xFF1C3531),
          secondaryContainer = Color(0xFF324B47),
          onSecondaryContainer = Color(0xFFCCE8E2),
          tertiary = Color(0xFFFBBF24),
          onTertiary = Color(0xFF422C00),
          tertiaryContainer = Color(0xFF5F4100),
          onTertiaryContainer = Color(0xFFFDE68A),
          background = Color(0xFF0E1514),
          onBackground = Color(0xFFE0E8E6),
          surface = Color(0xFF0E1514),
          onSurface = Color(0xFFE0E8E6),
          surfaceVariant = Color(0xFF3E4947),
          onSurfaceVariant = Color(0xFFBEC9C6),
          surfaceContainerLowest = Color(0xFF090F0E),
          surfaceContainerLow = Color(0xFF151D1C),
          surfaceContainer = Color(0xFF1A2321),
          surfaceContainerHigh = Color(0xFF242D2B),
          surfaceContainerHighest = Color(0xFF2E3836),
          outline = Color(0xFF889390),
          outlineVariant = Color(0xFF3E4947),
        )
      ThemeSeedPreset.COBALT ->
        darkColorScheme(
          primary = Color(0xFF93C5FD),
          onPrimary = Color(0xFF0B2A6B),
          primaryContainer = Color(0xFF1E3A8A),
          onPrimaryContainer = Color(0xFFDBEAFE),
          secondary = Color(0xFFCBD5E1),
          onSecondary = Color(0xFF1E293B),
          secondaryContainer = Color(0xFF334155),
          onSecondaryContainer = Color(0xFFE2E8F0),
          tertiary = Color(0xFF38BDF8),
          onTertiary = Color(0xFF082F49),
          tertiaryContainer = Color(0xFF075985),
          onTertiaryContainer = Color(0xFFE0F2FE),
          background = Color(0xFF0B111E),
          onBackground = Color(0xFFE2E8F0),
          surface = Color(0xFF0B111E),
          onSurface = Color(0xFFE2E8F0),
          surfaceVariant = Color(0xFF334155),
          onSurfaceVariant = Color(0xFFCBD5E1),
          surfaceContainerLowest = Color(0xFF070B14),
          surfaceContainerLow = Color(0xFF111827),
          surfaceContainer = Color(0xFF172033),
          surfaceContainerHigh = Color(0xFF1E293B),
          surfaceContainerHighest = Color(0xFF283548),
          outline = Color(0xFF64748B),
          outlineVariant = Color(0xFF334155),
        )
      ThemeSeedPreset.TERRACOTTA ->
        darkColorScheme(
          primary = Color(0xFFFBBF24),
          onPrimary = Color(0xFF451A03),
          primaryContainer = Color(0xFF78350F),
          onPrimaryContainer = Color(0xFFFEF3C7),
          secondary = Color(0xFFD6C2B0),
          onSecondary = Color(0xFF3B2B1D),
          secondaryContainer = Color(0xFF534132),
          onSecondaryContainer = Color(0xFFF5E6D8),
          tertiary = Color(0xFF5EEAD4),
          onTertiary = Color(0xFF042F2E),
          tertiaryContainer = Color(0xFF115E59),
          onTertiaryContainer = Color(0xFFCCFBF1),
          background = Color(0xFF17120E),
          onBackground = Color(0xFFEDE2D6),
          surface = Color(0xFF17120E),
          onSurface = Color(0xFFEDE2D6),
          surfaceVariant = Color(0xFF4C3F34),
          onSurfaceVariant = Color(0xFFD5C4B4),
          surfaceContainerLowest = Color(0xFF100C09),
          surfaceContainerLow = Color(0xFF1E1813),
          surfaceContainer = Color(0xFF241D17),
          surfaceContainerHigh = Color(0xFF2E261F),
          surfaceContainerHighest = Color(0xFF393027),
          outline = Color(0xFF9C8B7B),
          outlineVariant = Color(0xFF4C3F34),
        )
      ThemeSeedPreset.GRAPHITE ->
        darkColorScheme(
          primary = Color(0xFFCBD5E1),
          onPrimary = Color(0xFF0F172A),
          primaryContainer = Color(0xFF334155),
          onPrimaryContainer = Color(0xFFF1F5F9),
          secondary = Color(0xFFD1D5DB),
          onSecondary = Color(0xFF1F2937),
          secondaryContainer = Color(0xFF374151),
          onSecondaryContainer = Color(0xFFF3F4F6),
          tertiary = Color(0xFF34D399),
          onTertiary = Color(0xFF064E3B),
          tertiaryContainer = Color(0xFF065F46),
          onTertiaryContainer = Color(0xFFD1FAE5),
          background = Color(0xFF111315),
          onBackground = Color(0xFFE5E7EB),
          surface = Color(0xFF111315),
          onSurface = Color(0xFFE5E7EB),
          surfaceVariant = Color(0xFF374151),
          onSurfaceVariant = Color(0xFFD1D5DB),
          surfaceContainerLowest = Color(0xFF0A0C0E),
          surfaceContainerLow = Color(0xFF181B1F),
          surfaceContainer = Color(0xFF1F2328),
          surfaceContainerHigh = Color(0xFF272C33),
          surfaceContainerHighest = Color(0xFF313740),
          outline = Color(0xFF6B7280),
          outlineVariant = Color(0xFF374151),
        )
      ThemeSeedPreset.PLUM ->
        darkColorScheme(
          primary = Color(0xFFC4B5FD),
          onPrimary = Color(0xFF2E1065),
          primaryContainer = Color(0xFF4C1D95),
          onPrimaryContainer = Color(0xFFEDE9FE),
          secondary = Color(0xFFCBC1DF),
          onSecondary = Color(0xFF322A45),
          secondaryContainer = Color(0xFF483F5E),
          onSecondaryContainer = Color(0xFFE8E2F6),
          tertiary = Color(0xFFFBBF24),
          onTertiary = Color(0xFF451A03),
          tertiaryContainer = Color(0xFF78350F),
          onTertiaryContainer = Color(0xFFFEF3C7),
          background = Color(0xFF13111C),
          onBackground = Color(0xFFEAE5F3),
          surface = Color(0xFF13111C),
          onSurface = Color(0xFFEAE5F3),
          surfaceVariant = Color(0xFF433C54),
          onSurfaceVariant = Color(0xFFC9C1D8),
          surfaceContainerLowest = Color(0xFF0D0B14),
          surfaceContainerLow = Color(0xFF1B1826),
          surfaceContainer = Color(0xFF211E2E),
          surfaceContainerHigh = Color(0xFF2B273A),
          surfaceContainerHighest = Color(0xFF363148),
          outline = Color(0xFF938AA5),
          outlineVariant = Color(0xFF433C54),
        )
    }
  return if (amoled) {
    base.toAmoledScheme()
  } else {
    base
  }
}

fun ColorScheme.toAmoledScheme(): ColorScheme =
  copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080A0A),
    surfaceContainer = Color(0xFF0F1212),
    surfaceContainerHigh = Color(0xFF171B1B),
    surfaceContainerHighest = Color(0xFF202626),
    outlineVariant = Color(0xFF2E3535),
  )
