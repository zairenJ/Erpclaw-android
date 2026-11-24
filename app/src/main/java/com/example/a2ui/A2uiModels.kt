package com.example.a2ui

sealed interface A2uiValue {
  data class Literal(val value: String) : A2uiValue

  data class PathBinding(val path: String, val fallback: String = "") : A2uiValue
}

data class SelectOption(
  val value: String,
  val label: String,
)

enum class A2uiTone {
  PRIMARY,
  SUCCESS,
  WARNING,
  ERROR,
  NEUTRAL,
}

enum class A2uiButtonStyle {
  PRIMARY,
  TONAL,
  OUTLINED,
}

sealed interface A2uiComponent {
  val id: String

  data class Column(
    override val id: String,
    val children: List<String>,
  ) : A2uiComponent

  data class Row(
    override val id: String,
    val children: List<String>,
  ) : A2uiComponent

  data class Card(
    override val id: String,
    val title: String,
    val children: List<String>,
  ) : A2uiComponent

  data class Text(
    override val id: String,
    val text: A2uiValue,
    val variant: String = "body", // h1, h2, h3, body, caption, mono
  ) : A2uiComponent

  data class MetricTile(
    override val id: String,
    val label: String,
    val value: A2uiValue,
    val tone: A2uiTone = A2uiTone.PRIMARY,
  ) : A2uiComponent

  data class StatusBadge(
    override val id: String,
    val label: A2uiValue,
    val tone: A2uiTone = A2uiTone.NEUTRAL,
  ) : A2uiComponent

  data class TextField(
    override val id: String,
    val label: String,
    val path: String,
    val placeholder: String = "",
    val multiline: Boolean = false,
  ) : A2uiComponent

  data class NumberField(
    override val id: String,
    val label: String,
    val path: String,
    val prefix: String = "",
    val integerOnly: Boolean = false,
  ) : A2uiComponent

  data class Select(
    override val id: String,
    val label: String,
    val path: String,
    val options: List<SelectOption>,
  ) : A2uiComponent

  data class Button(
    override val id: String,
    val label: String,
    val actionId: String,
    val style: A2uiButtonStyle = A2uiButtonStyle.PRIMARY,
  ) : A2uiComponent

  data class Divider(override val id: String) : A2uiComponent
}

data class A2uiEnvelope(
  val surfaceId: String,
  val title: String,
  val sourceLabel: String,
  val rootId: String = "root",
  val components: Map<String, A2uiComponent>,
  val dataModel: Map<String, String>,
  val droppedUnsupportedCount: Int = 0,
) {
  fun resolve(value: A2uiValue): String {
    return when (value) {
      is A2uiValue.Literal -> value.value
      is A2uiValue.PathBinding -> dataModel[value.path] ?: value.fallback
    }
  }

  fun withUpdatedData(path: String, newValue: String): A2uiEnvelope {
    return copy(dataModel = dataModel + (path to newValue))
  }
}
