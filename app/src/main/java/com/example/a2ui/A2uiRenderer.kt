package com.example.a2ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalErpThemeConfig
import com.example.ui.theme.TabularNumberStyle

@Composable
fun A2uiSurfaceRenderer(
  envelope: A2uiEnvelope,
  onDataChange: (path: String, value: String) -> Unit,
  onAction: (actionId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RenderNode(
      nodeId = envelope.rootId,
      envelope = envelope,
      onDataChange = onDataChange,
      onAction = onAction,
      depth = 0,
    )
  }
}

@Composable
private fun RenderNode(
  nodeId: String,
  envelope: A2uiEnvelope,
  onDataChange: (String, String) -> Unit,
  onAction: (String) -> Unit,
  depth: Int,
  modifier: Modifier = Modifier,
) {
  if (depth > 8) return
  val component = envelope.components[nodeId] ?: return

  when (component) {
    is A2uiComponent.Column -> {
      Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        component.children.forEach { childId ->
          RenderNode(childId, envelope, onDataChange, onAction, depth + 1)
        }
      }
    }

    is A2uiComponent.Row -> {
      Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        component.children.forEach { childId ->
          Box(modifier = Modifier.weight(1f)) {
            RenderNode(childId, envelope, onDataChange, onAction, depth + 1)
          }
        }
      }
    }

    is A2uiComponent.Card -> {
      val isAmoled = LocalErpThemeConfig.current.isAmoledActive
      OutlinedCard(
        modifier = modifier.fillMaxWidth().testTag("a2ui_${component.id}"),
        colors =
          CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
        border =
          BorderStroke(
            width = 1.dp,
            color =
              if (isAmoled) MaterialTheme.colorScheme.outlineVariant
              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (component.title.isNotBlank()) {
            Text(
              text = component.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          component.children.forEach { childId ->
            RenderNode(childId, envelope, onDataChange, onAction, depth + 1)
          }
        }
      }
    }

    is A2uiComponent.Text -> {
      val resolved = envelope.resolve(component.text)
      val style =
        when (component.variant) {
          "h1" -> MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle)
          "h2" -> MaterialTheme.typography.titleLarge
          "h3" -> MaterialTheme.typography.titleMedium
          "caption" -> MaterialTheme.typography.bodySmall
          "mono" -> TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
          else -> MaterialTheme.typography.bodyMedium
        }
      val color =
        if (component.variant == "caption") {
          MaterialTheme.colorScheme.onSurfaceVariant
        } else {
          MaterialTheme.colorScheme.onSurface
        }
      Text(
        text = resolved,
        style = style,
        color = color,
        modifier = modifier.testTag("a2ui_${component.id}"),
      )
    }

    is A2uiComponent.MetricTile -> {
      val resolved = envelope.resolve(component.value)
      val (bg, fg) = toneColors(component.tone)
      Surface(
        modifier = modifier.fillMaxWidth().testTag("a2ui_${component.id}"),
        shape = MaterialTheme.shapes.medium,
        color = bg,
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = component.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = resolved,
            style = MaterialTheme.typography.titleLarge.merge(TabularNumberStyle),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    is A2uiComponent.StatusBadge -> {
      val resolved = envelope.resolve(component.label)
      val (bg, fg) = toneColors(component.tone)
      Surface(
        modifier = modifier.testTag("a2ui_${component.id}"),
        shape = MaterialTheme.shapes.small,
        color = bg,
      ) {
        Text(
          text = resolved,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = fg,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    is A2uiComponent.TextField -> {
      val currentValue = envelope.dataModel[component.path].orEmpty()
      OutlinedTextField(
        value = currentValue,
        onValueChange = { onDataChange(component.path, it) },
        label = { Text(component.label) },
        placeholder = {
          if (component.placeholder.isNotBlank()) Text(component.placeholder)
        },
        singleLine = !component.multiline,
        minLines = if (component.multiline) 2 else 1,
        modifier = modifier.fillMaxWidth().testTag("a2ui_${component.id}"),
      )
    }

    is A2uiComponent.NumberField -> {
      val currentValue = envelope.dataModel[component.path].orEmpty()
      OutlinedTextField(
        value = currentValue,
        onValueChange = { raw ->
          val filtered =
            if (component.integerOnly) {
              raw.filterIndexed { index, ch ->
                ch.isDigit() || (index == 0 && (ch == '-' || ch == '+'))
              }
            } else {
              raw.filter { it.isDigit() || it == '.' }
            }
          onDataChange(component.path, filtered)
        },
        label = { Text(component.label) },
        prefix =
          if (component.prefix.isNotBlank()) {
            { Text(component.prefix, style = TabularNumberStyle) }
          } else {
            null
          },
        textStyle = TabularNumberStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions =
          KeyboardOptions(
            keyboardType =
              if (component.integerOnly) KeyboardType.Number else KeyboardType.Decimal
          ),
        singleLine = true,
        modifier = modifier.fillMaxWidth().testTag("a2ui_${component.id}"),
      )
    }

    is A2uiComponent.Select -> {
      A2uiDropdownField(
        component = component,
        selectedValue = envelope.dataModel[component.path].orEmpty(),
        onSelect = { onDataChange(component.path, it) },
        modifier = modifier,
      )
    }

    is A2uiComponent.Button -> {
      val btnModifier =
        modifier.fillMaxWidth().widthIn(min = 48.dp).testTag("a2ui_${component.id}")
      when (component.style) {
        A2uiButtonStyle.PRIMARY ->
          Button(onClick = { onAction(component.actionId) }, modifier = btnModifier) {
            Text(component.label)
          }
        A2uiButtonStyle.TONAL ->
          FilledTonalButton(onClick = { onAction(component.actionId) }, modifier = btnModifier) {
            Text(component.label)
          }
        A2uiButtonStyle.OUTLINED ->
          OutlinedButton(onClick = { onAction(component.actionId) }, modifier = btnModifier) {
            Text(component.label)
          }
      }
    }

    is A2uiComponent.Divider -> {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun A2uiDropdownField(
  component: A2uiComponent.Select,
  selectedValue: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel =
    component.options.firstOrNull { it.value == selectedValue }?.label
      ?: component.options.firstOrNull()?.label
      ?: selectedValue

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedLabel,
      onValueChange = {},
      readOnly = true,
      label = { Text(component.label) },
      trailingIcon = {
        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
      },
      modifier =
        Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag("a2ui_${component.id}"),
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      component.options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.label) },
          onClick = {
            onSelect(option.value)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun toneColors(tone: A2uiTone) =
  when (tone) {
    A2uiTone.PRIMARY ->
      MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    A2uiTone.SUCCESS ->
      MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    A2uiTone.WARNING ->
      MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    A2uiTone.ERROR ->
      MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    A2uiTone.NEUTRAL ->
      MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
  }
