package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ErpThemeConfig(
  val seedPreset: ThemeSeedPreset = ThemeSeedPreset.EMERALD,
  val surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.SYSTEM,
  val useDynamicColor: Boolean = true,
  val isAmoledActive: Boolean = false,
)

val LocalErpThemeConfig = staticCompositionLocalOf { ErpThemeConfig() }

val ErpShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun ErpClawTheme(
  config: ErpThemeConfig = ErpThemeConfig(),
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (config.surfaceMode) {
      ThemeSurfaceMode.SYSTEM -> systemDark
      ThemeSurfaceMode.LIGHT -> false
      ThemeSurfaceMode.DARK, ThemeSurfaceMode.AMOLED -> true
    }
  val isAmoled = config.surfaceMode == ThemeSurfaceMode.AMOLED

  val colorScheme =
    when {
      config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        val dynamic =
          if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (isAmoled) dynamic.toAmoledScheme() else dynamic
      }
      isDark -> buildDarkScheme(config.seedPreset, amoled = isAmoled)
      else -> buildLightScheme(config.seedPreset)
    }

  CompositionLocalProvider(
    LocalErpThemeConfig provides config.copy(isAmoledActive = isAmoled)
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = ErpShapes,
      content = content,
    )
  }
}
