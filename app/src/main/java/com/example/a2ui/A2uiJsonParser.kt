package com.example.a2ui

import org.json.JSONArray
import org.json.JSONObject

object A2uiJsonParser {

  val trustedCatalogTypes =
    setOf(
      "Column",
      "Row",
      "Card",
      "Text",
      "MetricTile",
      "StatusBadge",
      "TextField",
      "NumberField",
      "Select",
      "Button",
      "Divider",
    )

  fun parseStructuredGeminiPayload(
    jsonString: String,
    sourceLabel: String = "Gemini A2UI",
    defaultSelectOptions: Map<String, List<SelectOption>> = emptyMap(),
  ): A2uiEnvelope {
    val root = JSONObject(jsonString)
    val title = root.optString("surfaceTitle", "ERPClaw A2UI Surface")
    val surfaceId = root.optString("surfaceId", "gemini_${System.currentTimeMillis()}")

    val dataMap = linkedMapOf<String, String>()
    val dataFields = root.optJSONArray("dataFields") ?: JSONArray()
    for (i in 0 until dataFields.length()) {
      val field = dataFields.optJSONObject(i) ?: continue
      val path = field.optString("path").trim()
      val value = field.optString("value", "")
      if (path.startsWith("/")) {
        dataMap[path] = value
      }
    }

    val compMap = linkedMapOf<String, A2uiComponent>()
    var dropped = 0
    val compArray = root.optJSONArray("components") ?: JSONArray()
    for (i in 0 until compArray.length()) {
      val obj = compArray.optJSONObject(i) ?: continue
      val parsed = parseComponentObject(obj, defaultSelectOptions)
      if (parsed != null) {
        compMap[parsed.id] = parsed
      } else {
        dropped++
      }
    }

    val rootId =
      if (compMap.containsKey("root")) {
        "root"
      } else {
        compMap.keys.firstOrNull() ?: "root"
      }

    return A2uiEnvelope(
      surfaceId = surfaceId,
      title = title,
      sourceLabel = sourceLabel,
      rootId = rootId,
      components = compMap,
      dataModel = dataMap,
      droppedUnsupportedCount = dropped,
    )
  }

  private fun parseComponentObject(
    obj: JSONObject,
    defaultSelectOptions: Map<String, List<SelectOption>>,
  ): A2uiComponent? {
    val id = obj.optString("id").trim()
    val type = obj.optString("component").trim()
    if (id.isEmpty() || type !in trustedCatalogTypes) return null

    return when (type) {
      "Column" -> A2uiComponent.Column(id = id, children = jsonArrayToStrings(obj.optJSONArray("children")))
      "Row" -> A2uiComponent.Row(id = id, children = jsonArrayToStrings(obj.optJSONArray("children")))
      "Card" ->
        A2uiComponent.Card(
          id = id,
          title = obj.optString("label", obj.optString("title", "")),
          children = jsonArrayToStrings(obj.optJSONArray("children")),
        )
      "Text" ->
        A2uiComponent.Text(
          id = id,
          text = parseValue(obj.optString("text"), obj.optString("path")),
          variant = obj.optString("variant", "body"),
        )
      "MetricTile" ->
        A2uiComponent.MetricTile(
          id = id,
          label = obj.optString("label", "Metric"),
          value = parseValue(obj.optString("text"), obj.optString("path")),
          tone = parseTone(obj.optString("tone")),
        )
      "StatusBadge" ->
        A2uiComponent.StatusBadge(
          id = id,
          label = parseValue(obj.optString("text", obj.optString("label")), obj.optString("path")),
          tone = parseTone(obj.optString("tone")),
        )
      "TextField" -> {
        val path = obj.optString("path").ifBlank { "/form/$id" }
        A2uiComponent.TextField(
          id = id,
          label = obj.optString("label", id),
          path = path,
          placeholder = obj.optString("text", ""),
          multiline = obj.optString("variant") == "multiline",
        )
      }
      "NumberField" -> {
        val path = obj.optString("path").ifBlank { "/form/$id" }
        A2uiComponent.NumberField(
          id = id,
          label = obj.optString("label", id),
          path = path,
          prefix = obj.optString("prefix", if (obj.optString("variant") == "integer") "" else "$"),
          integerOnly = obj.optString("variant") == "integer",
        )
      }
      "Select" -> {
        val path = obj.optString("path").ifBlank { "/form/$id" }
        val optsJson = obj.optJSONArray("options")
        val parsedOpts = buildList {
          if (optsJson != null) {
            for (j in 0 until optsJson.length()) {
              val item = optsJson.optJSONObject(j)
              if (item != null) {
                val v = item.optString("value")
                val l = item.optString("label", v)
                if (v.isNotBlank()) add(SelectOption(v, l))
              } else {
                val s = optsJson.optString(j)
                if (s.isNotBlank()) add(SelectOption(s, s))
              }
            }
          }
        }
        val finalOpts = parsedOpts.ifEmpty { defaultSelectOptions[path].orEmpty() }
        A2uiComponent.Select(
          id = id,
          label = obj.optString("label", id),
          path = path,
          options = finalOpts,
        )
      }
      "Button" ->
        A2uiComponent.Button(
          id = id,
          label = obj.optString("label", "Submit"),
          actionId = obj.optString("actionId", id),
          style =
            when (obj.optString("tone").uppercase()) {
              "TONAL" -> A2uiButtonStyle.TONAL
              "OUTLINED" -> A2uiButtonStyle.OUTLINED
              else -> A2uiButtonStyle.PRIMARY
            },
        )
      "Divider" -> A2uiComponent.Divider(id = id)
      else -> null
    }
  }

  private fun parseValue(literalOrRef: String, explicitPath: String): A2uiValue {
    if (explicitPath.startsWith("/")) {
      return A2uiValue.PathBinding(explicitPath, literalOrRef)
    }
    if (literalOrRef.startsWith("@path:")) {
      return A2uiValue.PathBinding(literalOrRef.removePrefix("@path:").trim())
    }
    return A2uiValue.Literal(literalOrRef)
  }

  private fun parseTone(raw: String): A2uiTone {
    return when (raw.uppercase()) {
      "SUCCESS" -> A2uiTone.SUCCESS
      "WARNING" -> A2uiTone.WARNING
      "ERROR" -> A2uiTone.ERROR
      "NEUTRAL" -> A2uiTone.NEUTRAL
      else -> A2uiTone.PRIMARY
    }
  }

  private fun jsonArrayToStrings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() } }
  }
}
