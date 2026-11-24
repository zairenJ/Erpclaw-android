package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.a2ui.A2uiSurfaceRenderer
import com.example.ui.A2uiUiState
import com.example.ui.theme.LocalErpThemeConfig

private data class QuickSkill(val key: String, val label: String)

private val quickSkills =
  listOf(
    QuickSkill("expense", "Expense Claim"),
    QuickSkill("invoice", "Sales Invoice"),
    QuickSkill("stock", "Stock Adjust"),
    QuickSkill("bill", "Vendor Bill"),
    QuickSkill("journal", "Journal Entry"),
    QuickSkill("payroll", "Run Payroll"),
    QuickSkill("trial_balance", "Trial Balance"),
    QuickSkill("new_customer", "+ Customer"),
    QuickSkill("new_vendor", "+ Vendor"),
    QuickSkill("new_item", "+ SKU"),
    QuickSkill("new_employee", "+ Employee"),
  )

@Composable
fun CommandHubScreen(
  isOnline: Boolean,
  useGeminiOnline: Boolean,
  pendingOutboxCount: Int,
  a2uiState: A2uiUiState,
  receiptUri: Uri?,
  onToggleGeminiOnline: (Boolean) -> Unit,
  onAttachReceipt: (Uri?) -> Unit,
  onSubmitCommand: (String) -> Unit,
  onSelectQuickSkill: (String) -> Unit,
  onDataChange: (String, String) -> Unit,
  onA2uiAction: (String) -> Unit,
  onTriggerSync: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var promptText by rememberSaveable { mutableStateOf("") }
  val isAmoled = LocalErpThemeConfig.current.isAmoledActive

  val photoPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onAttachReceipt(uri)
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Top Connectivity & Outbox Status Header
      Surface(
        shape = MaterialTheme.shapes.large,
        color =
          if (isOnline) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainerHigh,
        border =
          if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
              contentDescription = null,
              tint =
                if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
              Text(
                text =
                  if (isOnline) "Online • Sync & Gemini Ready"
                  else "Offline • Local A2UI Active",
                style = MaterialTheme.typography.titleSmall,
                color =
                  if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (pendingOutboxCount > 0) "$pendingOutboxCount pending in sync outbox"
                  else "All local SQLite records reconciled",
                style = MaterialTheme.typography.bodySmall,
                color =
                  if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (pendingOutboxCount > 0 && isOnline) {
            TextButton(
              onClick = onTriggerSync,
              modifier = Modifier.testTag("sync_now_banner_button"),
            ) {
              Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(stringResource(R.string.sync_now))
            }
          }
        }
      }

      // Conversational A2UI Command Composer
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
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "A2UI Command Bar",
              style = MaterialTheme.typography.titleMedium,
            )
            FilterChip(
              selected = useGeminiOnline && isOnline,
              onClick = { onToggleGeminiOnline(!useGeminiOnline) },
              enabled = isOnline,
              leadingIcon = {
                Icon(
                  Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = {
                Text(
                  when {
                    !isOnline -> "Offline Engine"
                    useGeminiOnline -> "Gemini AI"
                    else -> "Local Templates"
                  },
                  style = MaterialTheme.typography.labelSmall,
                )
              },
              modifier = Modifier.testTag("toggle_gemini_chip"),
            )
          }

          OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            placeholder = { Text(stringResource(R.string.prompt_hint)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("command_prompt_input"),
          )

          AnimatedVisibility(visible = receiptUri != null) {
            if (receiptUri != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                AsyncImage(
                  model = receiptUri,
                  contentDescription = stringResource(R.string.attach_receipt),
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                )
                Text(
                  text = "Receipt photo attached",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = { onAttachReceipt(null) },
                  modifier = Modifier.testTag("clear_receipt_button"),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = "Remove receipt")
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.testTag("attach_receipt_button"),
            ) {
              Icon(
                Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.attach_receipt))
            }

            Button(
              onClick = { onSubmitCommand(promptText) },
              enabled = promptText.isNotBlank() || receiptUri != null,
              modifier = Modifier.testTag("generate_a2ui_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.send_command))
            }
          }
        }
      }

      // Quick ERPClaw Skills Ribbon
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        quickSkills.forEach { skill ->
          AssistChip(
            onClick = { onSelectQuickSkill(skill.key) },
            label = { Text(skill.label, style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier.testTag("skill_chip_${skill.key}"),
          )
        }
      }

      // Live A2UI Surface Canvas
      when (a2uiState) {
        is A2uiUiState.Generating -> {
          OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator()
              Text(
                text = "Synthesizing A2UI Surface…",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = a2uiState.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        is A2uiUiState.Error -> {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = a2uiState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
          SurfaceHeaderBanner(
            title = a2uiState.fallbackEnvelope.title,
            sourceLabel = a2uiState.fallbackEnvelope.sourceLabel,
            banner = null,
            droppedCount = a2uiState.fallbackEnvelope.droppedUnsupportedCount,
          )
          A2uiSurfaceRenderer(
            envelope = a2uiState.fallbackEnvelope,
            onDataChange = onDataChange,
            onAction = onA2uiAction,
          )
        }

        is A2uiUiState.Ready -> {
          SurfaceHeaderBanner(
            title = a2uiState.envelope.title,
            sourceLabel = a2uiState.envelope.sourceLabel,
            banner = a2uiState.bannerMessage,
            droppedCount = a2uiState.envelope.droppedUnsupportedCount,
          )
          A2uiSurfaceRenderer(
            envelope = a2uiState.envelope,
            onDataChange = onDataChange,
            onAction = onA2uiAction,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SurfaceHeaderBanner(
  title: String,
  sourceLabel: String,
  banner: String?,
  droppedCount: Int,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.weight(1f),
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
      ) {
        Text(
          text = sourceLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
    if (!banner.isNullOrBlank()) {
      Text(
        text = banner,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (droppedCount > 0) {
      Text(
        text = "Catalog guardrail filtered $droppedCount unsupported node(s).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
      )
    }
  }
}
