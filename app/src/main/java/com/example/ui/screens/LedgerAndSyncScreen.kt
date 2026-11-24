package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MoneyUtils
import com.example.data.local.JournalEntryWithLines
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.SyncStatus
import com.example.data.prefs.SyncSettings
import com.example.sync.SyncRunSummary
import com.example.ui.theme.LocalErpThemeConfig
import com.example.ui.theme.TabularNumberStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerAndSyncScreen(
  isOnline: Boolean,
  syncSettings: SyncSettings,
  syncRunSummary: SyncRunSummary,
  outboxItems: List<SyncOutboxEntity>,
  journalEntries: List<JournalEntryWithLines>,
  onTriggerSync: () -> Unit,
  onToggleAutoSync: (Boolean) -> Unit,
  onSaveWebhookUrl: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showJournalView by rememberSaveable { mutableStateOf(false) }
  var webhookInput by rememberSaveable(syncSettings.syncWebhookUrl) {
    mutableStateOf(syncSettings.syncWebhookUrl)
  }
  val isAmoled = LocalErpThemeConfig.current.isAmoledActive
  val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)

  val pendingCount =
    outboxItems.count { it.status == SyncStatus.PENDING.name || it.status == SyncStatus.FAILED.name }
  val totalDebit = journalEntries.sumOf { entry -> entry.lines.sumOf { it.debitCents } }
  val totalCredit = journalEntries.sumOf { entry -> entry.lines.sumOf { it.creditCents } }
  val ledgerBalanced = totalDebit == totalCredit

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                  contentDescription = null,
                  tint =
                    if (isOnline) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                  Text(
                    if (isOnline) "Connected • Auto-Sync Active" else "Offline • Queuing Deltas",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    syncRunSummary.lastResultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Button(
                onClick = onTriggerSync,
                enabled = isOnline && !syncRunSummary.isSyncing,
                modifier = Modifier.testTag("sync_now_button"),
              ) {
                if (syncRunSummary.isSyncing) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                } else {
                  Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sync_now))
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Auto-reconcile when internet connects",
                style = MaterialTheme.typography.bodyMedium,
              )
              Switch(
                checked = syncSettings.autoSyncOnReconnect,
                onCheckedChange = onToggleAutoSync,
                modifier = Modifier.testTag("auto_sync_switch"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = webhookInput,
                onValueChange = { webhookInput = it },
                label = { Text("Optional ERPClaw Sync Webhook URL") },
                placeholder = { Text("https://your-erpclaw-node/sync") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("webhook_url_input"),
              )
              OutlinedButton(
                onClick = { onSaveWebhookUrl(webhookInput) },
                modifier = Modifier.testTag("save_webhook_button"),
              ) {
                Text("Save")
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = !showJournalView,
            onClick = { showJournalView = false },
            label = { Text("Sync Outbox ($pendingCount pending)") },
            modifier = Modifier.testTag("tab_sync_outbox"),
          )
          FilterChip(
            selected = showJournalView,
            onClick = { showJournalView = true },
            label = { Text("Journal Ledger (${journalEntries.size})") },
            modifier = Modifier.testTag("tab_journal_ledger"),
          )
        }
      }

      if (!showJournalView) {
        if (outboxItems.isEmpty()) {
          item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "No sync events queued yet. Offline or online ERP mutations appear here for audit and sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
              )
            }
          }
        } else {
          items(outboxItems, key = { it.id }) { item ->
            OutlinedCard(
              colors =
                CardDefaults.outlinedCardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              border =
                BorderStroke(
                  1.dp,
                  if (isAmoled) MaterialTheme.colorScheme.outlineVariant
                  else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    "${item.operation} • ${item.entityType.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  OutboxStatusBadge(item.status)
                }
                Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    dateFormat.format(Date(item.createdAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (item.aiEnrichmentPending) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                      )
                      Text(
                        "AI Enrichment Queued",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                      )
                    }
                  }
                }
                if (item.errorMessage.isNotBlank()) {
                  Text(
                    item.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      } else {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color =
              if (ledgerBalanced) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (ledgerBalanced) "Trial Balance Guardrail: Debits == Credits"
                  else "Trial Balance Mismatch",
                style = MaterialTheme.typography.titleSmall,
                color =
                  if (ledgerBalanced) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = MoneyUtils.formatCents(totalDebit),
                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                color =
                  if (ledgerBalanced) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }

        items(journalEntries, key = { it.entry.id }) { j ->
          OutlinedCard(
            colors =
              CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  "${j.entry.entryNumber} • ${j.entry.moduleSource}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  dateFormat.format(Date(j.entry.dateMillis)),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(j.entry.memo, style = MaterialTheme.typography.titleSmall)
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              j.lines.forEach { line ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    "${line.accountCode} ${line.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    if (line.debitCents > 0) "DR ${MoneyUtils.formatCents(line.debitCents)}"
                    else "CR ${MoneyUtils.formatCents(line.creditCents)}",
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                    color =
                      if (line.debitCents > 0) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.tertiary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OutboxStatusBadge(status: String) {
  val (icon, bg, fg) =
    when (status) {
      SyncStatus.SYNCED.name, SyncStatus.EXPORTED.name ->
        Triple(
          Icons.Rounded.CheckCircle,
          MaterialTheme.colorScheme.primaryContainer,
          MaterialTheme.colorScheme.onPrimaryContainer,
        )
      SyncStatus.FAILED.name ->
        Triple(
          Icons.Rounded.ErrorOutline,
          MaterialTheme.colorScheme.errorContainer,
          MaterialTheme.colorScheme.onErrorContainer,
        )
      else ->
        Triple(
          Icons.Rounded.Schedule,
          MaterialTheme.colorScheme.tertiaryContainer,
          MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }

  Surface(shape = MaterialTheme.shapes.small, color = bg) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
      Text(
        status,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
      )
    }
  }
}
