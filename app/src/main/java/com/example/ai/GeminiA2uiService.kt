package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.a2ui.A2uiEnvelope
import com.example.a2ui.A2uiJsonParser
import com.example.a2ui.ErpSnapshotForA2ui
import com.example.a2ui.SelectOption
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiA2uiService"

data class ExpenseEnrichmentResult(
  val suggestedCategory: String,
  val taxNote: String,
)

class GeminiA2uiService {

  private val a2uiSchema =
    Schema.obj(
      properties =
        mapOf(
          "surfaceTitle" to Schema.string(description = "Short title of the ERP A2UI surface"),
          "summaryMessage" to
            Schema.string(description = "Brief 1-sentence ERP insight or confirmation note"),
          "dataFields" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "path" to
                        Schema.string(
                          description =
                            "RFC 6901 JSON pointer path matching the component path, e.g. /expense/amount"
                        ),
                      "value" to Schema.string(description = "Initial string value for the path"),
                    )
                ),
              description = "Key-value initial state bound to component paths",
            ),
          "components" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "id" to Schema.string(description = "Unique component ID; root must be 'root'"),
                      "component" to
                        Schema.enumeration(
                          values =
                            listOf(
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
                            ),
                          description = "Trusted A2UI catalog component type",
                        ),
                      "label" to Schema.string(description = "Label or Card title"),
                      "text" to Schema.string(description = "Literal text or placeholder"),
                      "path" to Schema.string(description = "JSON pointer binding path starting with /"),
                      "actionId" to
                        Schema.string(
                          description =
                            "One of SUBMIT_EXPENSE, CREATE_INVOICE, ADJUST_STOCK, CREATE_BILL, POST_JOURNAL, RUN_PAYROLL, CREATE_CUSTOMER, CREATE_VENDOR, CREATE_ITEM, CREATE_EMPLOYEE"
                        ),
                      "variant" to
                        Schema.string(
                          description = "h1, h2, h3, body, caption, mono, integer, or multiline"
                        ),
                      "tone" to
                        Schema.string(
                          description = "PRIMARY, SUCCESS, WARNING, ERROR, NEUTRAL, TONAL, or OUTLINED"
                        ),
                      "children" to
                        Schema.array(
                          items = Schema.string(),
                          description = "Child component IDs for Column, Row, or Card",
                        ),
                    ),
                  optionalProperties =
                    listOf("label", "text", "path", "actionId", "variant", "tone", "children"),
                ),
              description = "Flat adjacency list of A2UI components starting with id='root'",
            ),
        ),
      optionalProperties = listOf("summaryMessage"),
    )

  private val a2uiModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = a2uiSchema
        },
    )
  }

  private val enrichmentSchema =
    Schema.obj(
      properties =
        mapOf(
          "suggestedCategory" to
            Schema.enumeration(
              values =
                listOf(
                  "Travel & Meals",
                  "Software & Cloud",
                  "Hardware & Supplies",
                  "Logistics & Fuel",
                  "Operations",
                )
            ),
          "taxNote" to
            Schema.string(description = "Concise 1-sentence accounting classification note"),
        )
    )

  private val enrichmentModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = enrichmentSchema
        },
    )
  }

  suspend fun generateA2uiSurface(
    userPrompt: String,
    snapshot: ErpSnapshotForA2ui,
    receiptBitmap: Bitmap? = null,
  ): Pair<A2uiEnvelope, String> {
    val contextSummary = buildString {
      appendLine("Available Accounts: ${snapshot.accounts.joinToString { "${it.id}:${it.code}-${it.name}" }}")
      appendLine("Inventory SKUs: ${snapshot.items.joinToString { "${it.id}:${it.sku}(${it.name},qty=${it.quantityOnHand})" }}")
      appendLine("Customers: ${snapshot.customers.joinToString { "${it.id}:${it.name}" }}")
      appendLine("Vendors: ${snapshot.vendors.joinToString { "${it.id}:${it.name}" }}")
      appendLine("Employees: ${snapshot.employees.joinToString { "${it.id}:${it.name}" }}")
    }

    val systemInstruction =
      """
      You are ERPClaw's A2UI v1.0 surface generator. Convert the user's ERP request (and optional receipt image) into an interactive native A2UI form or report.
      Rules:
      1. Root component MUST have id="root" (Column) referencing child IDs.
      2. Use Card containers to group form fields cleanly.
      3. Standard path conventions so ERPClaw actions execute cleanly:
         - Expense (actionId=SUBMIT_EXPENSE): /expense/employeeId, /expense/category, /expense/amount, /expense/description
         - Invoice (actionId=CREATE_INVOICE): /invoice/customerId, /invoice/itemId, /invoice/quantity, /invoice/unitPrice, /invoice/description
         - Stock Adjustment (actionId=ADJUST_STOCK): /stock/itemId, /stock/delta, /stock/reason
         - Purchase Bill (actionId=CREATE_BILL): /bill/vendorId, /bill/itemId, /bill/quantity, /bill/unitCost, /bill/description
         - Journal Entry (actionId=POST_JOURNAL): /journal/debitAccountId, /journal/creditAccountId, /journal/amount, /journal/memo
         - Payroll (actionId=RUN_PAYROLL): /payroll/employeeId, /payroll/period, /payroll/taxPercent
         - New Customer (actionId=CREATE_CUSTOMER): /customer/name, /customer/email, /customer/phone, /customer/stage
         - New Vendor (actionId=CREATE_VENDOR): /vendor/name, /vendor/email, /vendor/terms
         - New Item (actionId=CREATE_ITEM): /item/sku, /item/name, /item/category, /item/unitCost, /item/salePrice, /item/qty, /item/reorder
      4. Populate dataFields with extracted values from the prompt or receipt image (using matching entity IDs when possible).
      5. Keep the UI concise: 1 summary badge/metric + 1 form Card + 1 primary submit Button.

      Current ERPClaw Workspace Context:
      $contextSummary
      User Request: $userPrompt
      """.trimIndent()

    return try {
      val response =
        withTimeout(60_000L) {
          if (receiptBitmap != null) {
            a2uiModel.generateContent(
              content {
                image(receiptBitmap)
                text(systemInstruction)
              }
            )
          } else {
            a2uiModel.generateContent(systemInstruction)
          }
        }
      val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
      val defaultSelects = buildDefaultSelectMap(snapshot)
      val envelope =
        A2uiJsonParser.parseStructuredGeminiPayload(
          jsonString = jsonText,
          sourceLabel = "Gemini A2UI • gemini-3.8-flash",
          defaultSelectOptions = defaultSelects,
        )
      val summary = JSONObject(jsonText).optString("summaryMessage", "Interactive A2UI surface ready.")
      envelope to summary
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Gemini A2UI generation failed", e)
      throw e
    }
  }

  suspend fun enrichOfflineExpense(prompt: String): ExpenseEnrichmentResult {
    return try {
      val response =
        withTimeout(30_000L) {
          enrichmentModel.generateContent(prompt)
        }
      val json = JSONObject(response.text ?: "{}")
      ExpenseEnrichmentResult(
        suggestedCategory = json.optString("suggestedCategory", "Operations"),
        taxNote = json.optString("taxNote", "Categorized automatically on reconnect."),
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Expense enrichment failed", e)
      throw e
    }
  }

  private fun buildDefaultSelectMap(snapshot: ErpSnapshotForA2ui): Map<String, List<SelectOption>> {
    return mapOf(
      "/expense/employeeId" to
        (listOf(SelectOption("0", "Owner / Field Operator")) +
          snapshot.employees.map { SelectOption(it.id.toString(), it.name) }),
      "/expense/category" to
        listOf(
          SelectOption("Travel & Meals", "Travel & Meals"),
          SelectOption("Software & Cloud", "Software & Cloud"),
          SelectOption("Hardware & Supplies", "Hardware & Supplies"),
          SelectOption("Logistics & Fuel", "Logistics & Fuel"),
          SelectOption("Operations", "Operations"),
        ),
      "/invoice/customerId" to snapshot.customers.map { SelectOption(it.id.toString(), it.name) },
      "/invoice/itemId" to
        (listOf(SelectOption("0", "Custom Service")) +
          snapshot.items.map { SelectOption(it.id.toString(), "${it.sku} • ${it.name}") }),
      "/stock/itemId" to
        snapshot.items.map { SelectOption(it.id.toString(), "${it.sku} • ${it.name}") },
      "/bill/vendorId" to snapshot.vendors.map { SelectOption(it.id.toString(), it.name) },
      "/bill/itemId" to
        (listOf(SelectOption("0", "Operating Expense")) +
          snapshot.items.map { SelectOption(it.id.toString(), "${it.sku} • ${it.name}") }),
      "/journal/debitAccountId" to
        snapshot.accounts.map { SelectOption(it.id.toString(), "${it.code} • ${it.name}") },
      "/journal/creditAccountId" to
        snapshot.accounts.map { SelectOption(it.id.toString(), "${it.code} • ${it.name}") },
      "/payroll/employeeId" to snapshot.employees.map { SelectOption(it.id.toString(), it.name) },
      "/customer/stage" to
        listOf(
          SelectOption("LEAD", "Lead"),
          SelectOption("QUALIFIED", "Qualified"),
          SelectOption("ACTIVE", "Active"),
        ),
      "/vendor/terms" to
        listOf(
          SelectOption("Due on Receipt", "Due on Receipt"),
          SelectOption("Net 15", "Net 15"),
          SelectOption("Net 30", "Net 30"),
          SelectOption("Net 60", "Net 60"),
        ),
    )
  }
}
