package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ErpThemeConfig
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.ThemeSeedPreset
import com.example.ui.theme.ThemeSurfaceMode
import kotlinx.coroutines.launch

@Composable
fun ThemeAndExportScreen(
  themeConfig: ErpThemeConfig,
  onSelectSeedPreset: (ThemeSeedPreset) -> Unit,
  onSelectSurfaceMode: (ThemeSurfaceMode) -> Unit,
  onToggleDynamicColor: (Boolean) -> Unit,
  onExportJsonUri: (Uri) -> Unit,
  onExportLedgerCsvUri: (Uri) -> Unit,
  onExportInventoryCsvUri: (Uri) -> Unit,
  onBuildShareJson: suspend () -> String,
  onLoadSampleWorkspace: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isAmoled = themeConfig.isAmoledActive

  val jsonExportLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      if (uri != null) onExportJsonUri(uri)
    }

  val ledgerCsvLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) {
      uri ->
      if (uri != null) onExportLedgerCsvUri(uri)
    }

  val inventoryCsvLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) {
      uri ->
      if (uri != null) onExportInventoryCsvUri(uri)
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Material You Theme Builder Section
      OutlinedCard(
        colors =
          CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
        border =
          BorderStroke(
            1.dp,
            if (isAmoled) MaterialTheme.colorScheme.outlineVariant
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Rounded.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text("Material You Theme Builder", style = MaterialTheme.typography.titleLarge)
          }

          // Live Preview Token Card
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  "LIVE THEME PREVIEW",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                  "$24,850.00",
                  style = MaterialTheme.typography.headlineSmall.merge(TabularNumberStyle),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer,
              ) {
                Text(
                  text = themeConfig.surfaceMode.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Wallpaper Dynamic Color", style = MaterialTheme.typography.titleSmall)
                Text(
                  "Follow Android Material You system palette (off to use custom seed below)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = themeConfig.useDynamicColor,
                onCheckedChange = onToggleDynamicColor,
                modifier = Modifier.testTag("dynamic_color_switch"),
              )
            }
          }

          Text("Seed Color Palette", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            ThemeSeedPreset.entries.forEach { preset ->
              val selected = !themeConfig.useDynamicColor && themeConfig.seedPreset == preset
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                  Modifier.clip(MaterialTheme.shapes.small)
                    .clickable {
                      onToggleDynamicColor(false)
                      onSelectSeedPreset(preset)
                    }
                    .padding(4.dp)
                    .testTag("seed_preset_${preset.name}"),
              ) {
                Box(
                  modifier =
                    Modifier.size(48.dp)
                      .clip(CircleShape)
                      .background(preset.seedColor)
                      .border(
                        width = if (selected) 3.dp else 1.dp,
                        color =
                          if (selected) MaterialTheme.colorScheme.onSurface
                          else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape,
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  if (selected) {
                    Icon(
                      Icons.Rounded.Check,
                      contentDescription = preset.label,
                      tint = Color.White,
                    )
                  }
                }
                Text(
                  preset.label.substringBefore(" "),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Text("Surface & Contrast Mode", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThemeSurfaceMode.entries.forEach { mode ->
              FilterChip(
                selected = themeConfig.surfaceMode == mode,
                onClick = { onSelectSurfaceMode(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("surface_mode_${mode.name}"),
              )
            }
          }
        }
      }

      // Manual Export & Backup Section
      OutlinedCard(
        colors =
          CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
        border =
          BorderStroke(
            1.dp,
            if (isAmoled) MaterialTheme.colorScheme.outlineVariant
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Manual Export & Local Backup", style = MaterialTheme.typography.titleLarge)
          Text(
            "Save portable ERPClaw backups directly to device storage or share them with accounting tools using Android Storage Access Framework (no broad storage permissions).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Button(
            onClick = { jsonExportLauncher.launch("erpclaw_backup.json") },
            modifier = Modifier.fillMaxWidth().testTag("export_json_button"),
          ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_json))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = { ledgerCsvLauncher.launch("erpclaw_ledger.csv") },
              modifier = Modifier.weight(1f).testTag("export_ledger_csv_button"),
            ) {
              Icon(
                Icons.Rounded.TableChart,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("Ledger CSV")
            }
            FilledTonalButton(
              onClick = { inventoryCsvLauncher.launch("erpclaw_inventory.csv") },
              modifier = Modifier.weight(1f).testTag("export_inventory_csv_button"),
            ) {
              Icon(
                Icons.Rounded.TableChart,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("Stock CSV")
            }
          }

          OutlinedButton(
            onClick = {
              scope.launch {
                val payload = onBuildShareJson()
                val sendIntent =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "ERPClaw Workspace Snapshot")
                    putExtra(Intent.EXTRA_TEXT, payload)
                  }
                context.startActivity(Intent.createChooser(sendIntent, "Share ERPClaw JSON"))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("share_json_button"),
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.share_snapshot))
          }
        }
      }

      // Workspace Utility
      OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Starter ERP Workspace", style = MaterialTheme.typography.titleSmall)
            Text(
              "Populate sample SKUs, CRM leads, invoices, bills, and payroll if database is empty.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(12.dp))
          OutlinedButton(
            onClick = onLoadSampleWorkspace,
            modifier = Modifier.testTag("studio_load_sample_button"),
          ) {
            Text("Load")
          }
        }
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}
