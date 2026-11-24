package com.example.sync

import android.util.Log
import com.example.ai.GeminiA2uiService
import com.example.data.ErpRepository
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.SyncStatus
import com.example.data.prefs.ThemeAndSyncPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SyncCoordinator"

data class SyncRunSummary(
  val isSyncing: Boolean = false,
  val lastResultMessage: String = "Idle",
)

class SyncCoordinator(
  private val repository: ErpRepository,
  private val preferences: ThemeAndSyncPreferences,
  private val geminiService: GeminiA2uiService,
) {
  private val mutex = Mutex()
  private val httpClient = OkHttpClient()

  private val _status = MutableStateFlow(SyncRunSummary())
  val status: StateFlow<SyncRunSummary> = _status.asStateFlow()

  suspend fun processPendingOutbox(isOnline: Boolean, isManualTrigger: Boolean = false): String {
    if (!isOnline) {
      val msg = "Offline — mutations queued locally for reconnect sync."
      _status.value = SyncRunSummary(isSyncing = false, lastResultMessage = msg)
      return msg
    }

    return mutex.withLock {
      val pending = repository.getPendingOutboxItems()
      if (pending.isEmpty()) {
        val msg = "All ERPClaw records are up to date."
        _status.value = SyncRunSummary(isSyncing = false, lastResultMessage = msg)
        return@withLock msg
      }

      _status.value =
        SyncRunSummary(isSyncing = true, lastResultMessage = "Syncing ${pending.size} queued item(s)…")

      val settings = preferences.syncSettings.first()
      var syncedCount = 0
      var failedCount = 0

      // 1. Process any deferred Gemini AI enrichments queued while offline
      for (item in pending) {
        if (item.aiEnrichmentPending && item.aiPrompt.isNotBlank() && item.entityType == "expense_claim") {
          try {
            val result = geminiService.enrichOfflineExpense(item.aiPrompt)
            repository.updateExpenseAiNote(
              claimId = item.entityId,
              note = result.taxNote,
              suggestedCategory = result.suggestedCategory,
            )
            repository.updateOutboxItem(item.copy(aiEnrichmentPending = false))
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Log.e(TAG, "Deferred AI enrichment failed for outbox #${item.id}", e)
          }
        }
      }

      // 2. If user configured a self-hosted ERPClaw/OpenClaw sync webhook URL, push JSON delta batch
      val webhookUrl = settings.syncWebhookUrl.trim()
      if (webhookUrl.isNotBlank()) {
        val latestPending = repository.getPendingOutboxItems()
        val batchSuccess = pushBatchToWebhook(webhookUrl, latestPending)
        val now = System.currentTimeMillis()
        latestPending.forEach { entry ->
          if (batchSuccess.first) {
            repository.updateOutboxItem(
              entry.copy(
                status = SyncStatus.SYNCED.name,
                syncedAtMillis = now,
                errorMessage = "",
                aiEnrichmentPending = false,
              )
            )
            syncedCount++
          } else {
            repository.updateOutboxItem(
              entry.copy(
                status = SyncStatus.FAILED.name,
                retryCount = entry.retryCount + 1,
                errorMessage = batchSuccess.second,
              )
            )
            failedCount++
          }
        }
      } else {
        // Local-first mode with no remote webhook: AI tasks reconciled, records marked synced locally
        val now = System.currentTimeMillis()
        repository.getPendingOutboxItems().forEach { entry ->
          repository.updateOutboxItem(
            entry.copy(
              status = SyncStatus.SYNCED.name,
              syncedAtMillis = now,
              errorMessage = "",
              aiEnrichmentPending = false,
            )
          )
          syncedCount++
        }
      }

      preferences.updateLastSyncedAt(System.currentTimeMillis())
      val finalMsg =
        when {
          failedCount > 0 -> "Sync finished with $failedCount error(s); tap Retry or export locally."
          webhookUrl.isNotBlank() -> "Synced $syncedCount record(s) to ERPClaw endpoint."
          isManualTrigger -> "Reconciled $syncedCount offline record(s) locally (ready for export)."
          else -> "Auto-reconciled $syncedCount offline record(s) on reconnect."
        }
      _status.value = SyncRunSummary(isSyncing = false, lastResultMessage = finalMsg)
      finalMsg
    }
  }

  private suspend fun pushBatchToWebhook(
    url: String,
    items: List<SyncOutboxEntity>,
  ): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
      try {
        val array = JSONArray()
        items.forEach { item ->
          array.put(
            JSONObject()
              .put("id", item.id)
              .put("entityType", item.entityType)
              .put("entityId", item.entityId)
              .put("operation", item.operation)
              .put("payload", JSONObject(item.payloadJson))
              .put("createdAtMillis", item.createdAtMillis)
          )
        }
        val bodyJson =
          JSONObject().put("source", "erpclaw-android").put("deltas", array).toString()
        val request =
          Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            true to ""
          } else {
            false to "HTTP ${response.code}: ${response.message}"
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Webhook sync failed", e)
        false to (e.localizedMessage ?: "Network request failed")
      }
    }
}
