package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ErpThemeConfig
import com.example.ui.theme.ThemeSeedPreset
import com.example.ui.theme.ThemeSurfaceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.erpDataStore: DataStore<Preferences> by preferencesDataStore(name = "erpclaw_prefs")

data class SyncSettings(
  val autoSyncOnReconnect: Boolean = true,
  val syncWebhookUrl: String = "",
  val lastSyncedAtMillis: Long = 0L,
  val useGeminiOnline: Boolean = true,
)

class ThemeAndSyncPreferences(private val context: Context) {

  private object Keys {
    val SEED_PRESET = stringPreferencesKey("theme_seed_preset")
    val SURFACE_MODE = stringPreferencesKey("theme_surface_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("theme_dynamic_color")
    val AUTO_SYNC = booleanPreferencesKey("sync_auto_reconnect")
    val WEBHOOK_URL = stringPreferencesKey("sync_webhook_url")
    val LAST_SYNCED_AT = longPreferencesKey("sync_last_synced_at")
    val USE_GEMINI_ONLINE = booleanPreferencesKey("a2ui_use_gemini_online")
  }

  val themeConfig: Flow<ErpThemeConfig> =
    context.erpDataStore.data.map { prefs ->
      val preset =
        prefs[Keys.SEED_PRESET]?.let { name ->
          ThemeSeedPreset.entries.firstOrNull { it.name == name }
        } ?: ThemeSeedPreset.EMERALD
      val mode =
        prefs[Keys.SURFACE_MODE]?.let { name ->
          ThemeSurfaceMode.entries.firstOrNull { it.name == name }
        } ?: ThemeSurfaceMode.SYSTEM
      val dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true
      ErpThemeConfig(
        seedPreset = preset,
        surfaceMode = mode,
        useDynamicColor = dynamicColor,
        isAmoledActive = mode == ThemeSurfaceMode.AMOLED,
      )
    }

  val syncSettings: Flow<SyncSettings> =
    context.erpDataStore.data.map { prefs ->
      SyncSettings(
        autoSyncOnReconnect = prefs[Keys.AUTO_SYNC] ?: true,
        syncWebhookUrl = prefs[Keys.WEBHOOK_URL] ?: "",
        lastSyncedAtMillis = prefs[Keys.LAST_SYNCED_AT] ?: 0L,
        useGeminiOnline = prefs[Keys.USE_GEMINI_ONLINE] ?: true,
      )
    }

  suspend fun setSeedPreset(preset: ThemeSeedPreset) {
    context.erpDataStore.edit { it[Keys.SEED_PRESET] = preset.name }
  }

  suspend fun setSurfaceMode(mode: ThemeSurfaceMode) {
    context.erpDataStore.edit { it[Keys.SURFACE_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    context.erpDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setAutoSyncOnReconnect(enabled: Boolean) {
    context.erpDataStore.edit { it[Keys.AUTO_SYNC] = enabled }
  }

  suspend fun setSyncWebhookUrl(url: String) {
    context.erpDataStore.edit { it[Keys.WEBHOOK_URL] = url.trim() }
  }

  suspend fun setUseGeminiOnline(enabled: Boolean) {
    context.erpDataStore.edit { it[Keys.USE_GEMINI_ONLINE] = enabled }
  }

  suspend fun updateLastSyncedAt(timestampMillis: Long) {
    context.erpDataStore.edit { it[Keys.LAST_SYNCED_AT] = timestampMillis }
  }
}
