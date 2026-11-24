package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui.A2uiEnvelope
import com.example.a2ui.ErpSnapshotForA2ui
import com.example.a2ui.OfflineA2uiEngine
import com.example.ai.GeminiA2uiService
import com.example.data.ErpRepository
import com.example.data.JournalPostingLine
import com.example.data.MoneyUtils
import com.example.data.local.AccountType
import com.example.data.local.CrmStage
import com.example.data.local.ErpDatabase
import com.example.data.prefs.SyncSettings
import com.example.data.prefs.ThemeAndSyncPreferences
import com.example.sync.ConnectivityObserver
import com.example.sync.ErpExporter
import com.example.sync.SyncCoordinator
import com.example.sync.SyncRunSummary
import com.example.ui.theme.ErpThemeConfig
import com.example.ui.theme.ThemeSeedPreset
import com.example.ui.theme.ThemeSurfaceMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface A2uiUiState {
  data class Ready(
    val envelope: A2uiEnvelope,
    val bannerMessage: String? = null,
  ) : A2uiUiState

  data class Generating(val prompt: String) : A2uiUiState

  data class Error(
    val message: String,
    val fallbackEnvelope: A2uiEnvelope,
  ) : A2uiUiState
}

enum class ErpModuleTab(val label: String) {
  ACCOUNTING("Accounting"),
  INVENTORY("Inventory"),
  SALES("Sales & CRM"),
  PURCHASING("Purchasing"),
  HR("HR & Payroll"),
}

class ErpViewModel(application: Application) : AndroidViewModel(application) {

  private val db = ErpDatabase.getInstance(application)
  private val repository = ErpRepository(db)
  private val preferences = ThemeAndSyncPreferences(application)
  private val connectivityObserver = ConnectivityObserver(application)
  private val geminiService = GeminiA2uiService()
  private val syncCoordinator = SyncCoordinator(repository, preferences, geminiService)

  val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

  val themeConfig: StateFlow<ErpThemeConfig> =
    preferences.themeConfig.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      ErpThemeConfig(),
    )

  val syncSettings: StateFlow<SyncSettings> =
    preferences.syncSettings.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      SyncSettings(),
    )

  val syncRunSummary: StateFlow<SyncRunSummary> = syncCoordinator.status

  val accounts =
    repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val journalEntries =
    repository.journalEntries.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )
  val inventoryItems =
    repository.inventoryItems.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )
  val stockMovements =
    repository.stockMovements.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )
  val customers =
    repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val invoices =
    repository.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val vendors =
    repository.vendors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val purchaseBills =
    repository.purchaseBills.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )
  val employees =
    repository.employees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val expenseClaims =
    repository.expenseClaims.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )
  val payrollRuns =
    repository.payrollRuns.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val syncOutbox =
    repository.syncOutbox.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedReceiptUri = MutableStateFlow<Uri?>(null)
  val selectedReceiptUri: StateFlow<Uri?> = _selectedReceiptUri.asStateFlow()

  private val _a2uiState =
    MutableStateFlow<A2uiUiState>(
      A2uiUiState.Ready(
        OfflineA2uiEngine.buildExpenseSurface(ErpSnapshotForA2ui()),
        bannerMessage = "Offline-first A2UI surface ready. Type any ERP command or pick a quick skill.",
      )
    )
  val a2uiState: StateFlow<A2uiUiState> = _a2uiState.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  init {
    viewModelScope.launch {
      repository.ensureChartOfAccountsInitialized()
      val snap = currentSnapshot()
      _a2uiState.value =
        A2uiUiState.Ready(
          OfflineA2uiEngine.buildExpenseSurface(snap),
          bannerMessage = "ERPClaw A2UI ready (${snap.accounts.size} ledger accounts active).",
        )
    }

    // Automatically trigger connectivity sync when internet returns
    viewModelScope.launch {
      isOnline
        .combine(preferences.syncSettings) { online, settings -> online to settings.autoSyncOnReconnect }
        .distinctUntilChanged()
        .collect { (online, autoSync) ->
          if (online && autoSync) {
            val pending = repository.getPendingOutboxItems()
            if (pending.isNotEmpty()) {
              val msg = syncCoordinator.processPendingOutbox(isOnline = true, isManualTrigger = false)
              _snackbarEvents.tryEmit(msg)
            }
          }
        }
    }
  }

  override fun onCleared() {
    super.onCleared()
    connectivityObserver.dispose()
  }

  private suspend fun currentSnapshot(): ErpSnapshotForA2ui {
    return ErpSnapshotForA2ui(
      accounts = repository.getAccountsOnce(),
      items = repository.getInventoryItemsOnce(),
      customers = repository.getCustomersOnce(),
      vendors = repository.getVendorsOnce(),
      employees = repository.getEmployeesOnce(),
    )
  }

  fun attachReceiptUri(uri: Uri?) {
    _selectedReceiptUri.value = uri
  }

  fun openQuickSkill(skillKey: String) {
    viewModelScope.launch {
      val snap = currentSnapshot()
      val receiptAttached = _selectedReceiptUri.value != null
      val envelope =
        when (skillKey) {
          "expense" -> OfflineA2uiEngine.buildExpenseSurface(snap, receiptAttached = receiptAttached)
          "invoice" -> OfflineA2uiEngine.buildInvoiceSurface(snap)
          "stock" -> OfflineA2uiEngine.buildStockAdjustmentSurface(snap)
          "bill" -> OfflineA2uiEngine.buildPurchaseBillSurface(snap)
          "journal" -> OfflineA2uiEngine.buildJournalEntrySurface(snap)
          "payroll" -> OfflineA2uiEngine.buildPayrollSurface(snap)
          "trial_balance" -> OfflineA2uiEngine.buildTrialBalanceSurface(snap)
          "new_customer" -> OfflineA2uiEngine.buildNewCustomerSurface()
          "new_vendor" -> OfflineA2uiEngine.buildNewVendorSurface()
          "new_item" -> OfflineA2uiEngine.buildNewInventoryItemSurface()
          "new_employee" -> OfflineA2uiEngine.buildNewEmployeeSurface()
          else -> OfflineA2uiEngine.buildExpenseSurface(snap)
        }
      _a2uiState.value = A2uiUiState.Ready(envelope)
    }
  }

  fun submitCommand(prompt: String) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isEmpty() && _selectedReceiptUri.value == null) return

    viewModelScope.launch {
      val snap = currentSnapshot()
      val online = isOnline.value
      val useGemini = syncSettings.value.useGeminiOnline

      if (online && useGemini) {
        _a2uiState.value = A2uiUiState.Generating(cleanPrompt.ifBlank { "Analyzing receipt…" })
        try {
          val bitmap = _selectedReceiptUri.value?.let { loadScaledBitmap(it) }
          val (envelope, summary) =
            geminiService.generateA2uiSurface(
              userPrompt = cleanPrompt.ifBlank { "Extract expense details from this receipt" },
              snapshot = snap,
              receiptBitmap = bitmap,
            )
          _a2uiState.value = A2uiUiState.Ready(envelope, bannerMessage = summary)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          val fallback = OfflineA2uiEngine.interpretCommand(cleanPrompt, snap)
          _a2uiState.value =
            A2uiUiState.Error(
              message = "Gemini AI error: ${e.localizedMessage ?: "Request failed"}. Showing offline A2UI fallback.",
              fallbackEnvelope = fallback,
            )
        }
      } else {
        val envelope = OfflineA2uiEngine.interpretCommand(cleanPrompt, snap)
        val banner =
          if (!online) {
            "Offline mode: rendered deterministic A2UI surface locally."
          } else {
            "Local A2UI template rendered."
          }
        _a2uiState.value = A2uiUiState.Ready(envelope, bannerMessage = banner)
      }
    }
  }

  fun updateA2uiData(path: String, value: String) {
    when (val current = _a2uiState.value) {
      is A2uiUiState.Ready -> {
        _a2uiState.value = current.copy(envelope = current.envelope.withUpdatedData(path, value))
      }
      is A2uiUiState.Error -> {
        _a2uiState.value =
          current.copy(fallbackEnvelope = current.fallbackEnvelope.withUpdatedData(path, value))
      }
      is A2uiUiState.Generating -> Unit
    }
  }

  fun executeA2uiAction(actionId: String) {
    val envelope =
      when (val current = _a2uiState.value) {
        is A2uiUiState.Ready -> current.envelope
        is A2uiUiState.Error -> current.fallbackEnvelope
        is A2uiUiState.Generating -> return
      }
    val data = envelope.dataModel

    viewModelScope.launch {
      try {
        when (actionId) {
          "OPEN_JOURNAL_SURFACE" -> {
            openQuickSkill("journal")
            return@launch
          }

          "SUBMIT_EXPENSE" -> {
            val empId = data["/expense/employeeId"]?.toLongOrNull()?.takeIf { it > 0L }
            val category = data["/expense/category"].orEmpty().ifBlank { "Operations" }
            val amountCents =
              MoneyUtils.parseDollarsToCents(data["/expense/amount"].orEmpty())
                ?: throw IllegalArgumentException("Enter a valid expense dollar amount.")
            val desc = data["/expense/description"].orEmpty()
            val receiptStr = _selectedReceiptUri.value?.toString()
            repository.submitExpenseClaim(
              employeeId = empId,
              employeeName = "Field Operator",
              category = category,
              amountCents = amountCents,
              description = desc,
              receiptUri = receiptStr,
              queueAiEnrichment = !isOnline.value,
            )
            _selectedReceiptUri.value = null
            notifySuccess("Expense claim saved & queued (${MoneyUtils.formatCents(amountCents)}).")
          }

          "CREATE_INVOICE" -> {
            val customerId =
              data["/invoice/customerId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select or create a customer first.")
            val itemId = data["/invoice/itemId"]?.toLongOrNull()?.takeIf { it > 0L }
            val qty =
              data["/invoice/quantity"]?.toIntOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("Enter a positive quantity.")
            val unitPriceCents =
              MoneyUtils.parseDollarsToCents(data["/invoice/unitPrice"].orEmpty())
                ?: throw IllegalArgumentException("Enter a valid unit price.")
            val desc = data["/invoice/description"].orEmpty()
            repository.createInvoice(customerId, itemId, desc, qty, unitPriceCents)
            notifySuccess("Invoice issued & posted to double-entry ledger.")
          }

          "ADJUST_STOCK" -> {
            val itemId =
              data["/stock/itemId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select an inventory SKU first.")
            val delta =
              data["/stock/delta"]?.toIntOrNull()?.takeIf { it != 0 }
                ?: throw IllegalArgumentException("Enter a non-zero integer quantity delta.")
            val reason = data["/stock/reason"].orEmpty()
            repository.adjustStock(itemId, delta, reason)
            notifySuccess("Stock movement recorded ($delta units) & ledger updated.")
          }

          "CREATE_BILL" -> {
            val vendorId =
              data["/bill/vendorId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select or create a vendor first.")
            val itemId = data["/bill/itemId"]?.toLongOrNull()?.takeIf { it > 0L }
            val qty =
              data["/bill/quantity"]?.toIntOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("Enter a positive quantity.")
            val costCents =
              MoneyUtils.parseDollarsToCents(data["/bill/unitCost"].orEmpty())
                ?: throw IllegalArgumentException("Enter a valid unit cost.")
            val desc = data["/bill/description"].orEmpty()
            repository.createPurchaseBill(vendorId, itemId, desc, qty, costCents)
            notifySuccess("Purchase bill posted to Accounts Payable.")
          }

          "POST_JOURNAL" -> {
            val debitId =
              data["/journal/debitAccountId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select a debit account.")
            val creditId =
              data["/journal/creditAccountId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select a credit account.")
            require(debitId != creditId) { "Debit and credit accounts must be different." }
            val amountCents =
              MoneyUtils.parseDollarsToCents(data["/journal/amount"].orEmpty())
                ?: throw IllegalArgumentException("Enter a valid journal amount.")
            val memo = data["/journal/memo"].orEmpty()
            repository.postBalancedJournal(
              memo = memo,
              moduleSource = "ACCOUNTING",
              lines =
                listOf(
                  JournalPostingLine(debitId, debitCents = amountCents, creditCents = 0L),
                  JournalPostingLine(creditId, debitCents = 0L, creditCents = amountCents),
                ),
            )
            notifySuccess("Balanced journal entry posted (${MoneyUtils.formatCents(amountCents)}).")
          }

          "RUN_PAYROLL" -> {
            val empId =
              data["/payroll/employeeId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Select or onboard an employee first.")
            val period = data["/payroll/period"].orEmpty()
            val tax = data["/payroll/taxPercent"]?.toIntOrNull() ?: 20
            repository.runPayrollForEmployee(empId, period, tax)
            notifySuccess("Payroll run posted to Wage Expense & Tax Liabilities.")
          }

          "CREATE_CUSTOMER" -> {
            val stage =
              data["/customer/stage"]?.let { s ->
                CrmStage.entries.firstOrNull { it.name == s }
              } ?: CrmStage.LEAD
            repository.createCustomer(
              name = data["/customer/name"].orEmpty(),
              email = data["/customer/email"].orEmpty(),
              phone = data["/customer/phone"].orEmpty(),
              stage = stage,
            )
            notifySuccess("Customer saved to CRM pipeline.")
          }

          "CREATE_VENDOR" -> {
            repository.createVendor(
              name = data["/vendor/name"].orEmpty(),
              email = data["/vendor/email"].orEmpty(),
              paymentTerms = data["/vendor/terms"].orEmpty(),
            )
            notifySuccess("Vendor added to Purchasing directory.")
          }

          "CREATE_ITEM" -> {
            val cost = MoneyUtils.parseDollarsToCents(data["/item/unitCost"].orEmpty()) ?: 0L
            val sale = MoneyUtils.parseDollarsToCents(data["/item/salePrice"].orEmpty()) ?: 0L
            val qty = data["/item/qty"]?.toIntOrNull() ?: 0
            val reorder = data["/item/reorder"]?.toIntOrNull() ?: 5
            repository.upsertInventoryItem(
              sku = data["/item/sku"].orEmpty(),
              name = data["/item/name"].orEmpty(),
              category = data["/item/category"].orEmpty(),
              unitCostCents = cost,
              salePriceCents = sale,
              initialQty = qty,
              reorderPoint = reorder,
            )
            notifySuccess("Inventory SKU registered.")
          }

          "CREATE_EMPLOYEE" -> {
            val salary =
              MoneyUtils.parseDollarsToCents(data["/employee/salary"].orEmpty())
                ?: throw IllegalArgumentException("Enter a valid monthly salary.")
            repository.createEmployee(
              name = data["/employee/name"].orEmpty(),
              role = data["/employee/role"].orEmpty(),
              department = data["/employee/department"].orEmpty(),
              monthlySalaryCents = salary,
            )
            notifySuccess("Employee onboarded to HR roster.")
          }

          else -> _snackbarEvents.tryEmit("Action $actionId completed.")
        }

        // Refresh surface banner and run auto-sync if online
        val snap = currentSnapshot()
        if (isOnline.value && syncSettings.value.autoSyncOnReconnect) {
          syncCoordinator.processPendingOutbox(isOnline = true, isManualTrigger = false)
        }
        _a2uiState.value =
          A2uiUiState.Ready(
            OfflineA2uiEngine.buildTrialBalanceSurface(snap),
            bannerMessage = "Committed to local ledger. Viewing updated financial snapshot.",
          )
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Could not complete action.")
      }
    }
  }

  private fun notifySuccess(message: String) {
    _snackbarEvents.tryEmit(message)
  }

  // Direct Operations Screen actions
  fun adjustInventoryStock(itemId: Long, delta: Int, reason: String) {
    viewModelScope.launch {
      try {
        repository.adjustStock(itemId, delta, reason)
        _snackbarEvents.tryEmit("Stock updated ($delta).")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Stock update failed.")
      }
    }
  }

  fun markInvoicePaid(invoiceId: Long) {
    viewModelScope.launch {
      try {
        repository.markInvoicePaid(invoiceId)
        _snackbarEvents.tryEmit("Invoice marked Paid • Cash & AR journal posted.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to record payment.")
      }
    }
  }

  fun advanceCustomerStage(customerId: Long, currentStage: String) {
    viewModelScope.launch {
      val next =
        when (currentStage) {
          CrmStage.LEAD.name -> CrmStage.QUALIFIED
          CrmStage.QUALIFIED.name -> CrmStage.ACTIVE
          else -> CrmStage.LEAD
        }
      repository.updateCustomerStage(customerId, next)
      _snackbarEvents.tryEmit("Customer moved to ${next.label}.")
    }
  }

  fun payPurchaseBill(billId: Long) {
    viewModelScope.launch {
      try {
        repository.payPurchaseBill(billId)
        _snackbarEvents.tryEmit("Bill paid • Accounts Payable settled.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to pay bill.")
      }
    }
  }

  fun approveAndPayExpense(claimId: Long) {
    viewModelScope.launch {
      try {
        repository.approveAndPayExpense(claimId)
        _snackbarEvents.tryEmit("Expense reimbursed & posted to Operating Expenses.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to reimburse expense.")
      }
    }
  }

  fun runPayroll(employeeId: Long) {
    viewModelScope.launch {
      try {
        repository.runPayrollForEmployee(employeeId, "Current Cycle", 20)
        _snackbarEvents.tryEmit("Payroll run posted.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Payroll run failed.")
      }
    }
  }

  fun addLedgerAccount(code: String, name: String, type: AccountType) {
    viewModelScope.launch {
      try {
        repository.addAccount(code, name, type)
        _snackbarEvents.tryEmit("Account $code $name added.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Could not add account.")
      }
    }
  }

  fun loadSampleWorkspace() {
    viewModelScope.launch {
      try {
        repository.loadSampleWorkspace()
        val snap = currentSnapshot()
        _a2uiState.value =
          A2uiUiState.Ready(
            OfflineA2uiEngine.buildTrialBalanceSurface(snap),
            bannerMessage = "Sample ERPClaw workspace loaded across all 5 modules.",
          )
        _snackbarEvents.tryEmit("Sample workspace loaded.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to load sample data.")
      }
    }
  }

  fun triggerManualSync() {
    viewModelScope.launch {
      val msg = syncCoordinator.processPendingOutbox(isOnline = isOnline.value, isManualTrigger = true)
      _snackbarEvents.tryEmit(msg)
    }
  }

  // Export actions
  fun exportJsonToUri(uri: Uri) {
    viewModelScope.launch {
      try {
        val json = ErpExporter.buildFullJsonSnapshot(repository)
        ErpExporter.writeTextToUri(getApplication(), uri, json)
        repository.markPendingOutboxExported()
        _snackbarEvents.tryEmit("Exported ERPClaw JSON backup successfully.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "JSON export failed.")
      }
    }
  }

  fun exportLedgerCsvToUri(uri: Uri) {
    viewModelScope.launch {
      try {
        val csv = ErpExporter.buildLedgerCsv(repository)
        ErpExporter.writeTextToUri(getApplication(), uri, csv)
        _snackbarEvents.tryEmit("Exported Double-Entry Ledger CSV.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "CSV export failed.")
      }
    }
  }

  fun exportInventoryCsvToUri(uri: Uri) {
    viewModelScope.launch {
      try {
        val csv = ErpExporter.buildInventoryCsv(repository)
        ErpExporter.writeTextToUri(getApplication(), uri, csv)
        _snackbarEvents.tryEmit("Exported Inventory CSV.")
      } catch (e: Exception) {
        _snackbarEvents.tryEmit(e.localizedMessage ?: "Inventory CSV export failed.")
      }
    }
  }

  suspend fun buildShareJsonPayload(): String {
    val json = ErpExporter.buildFullJsonSnapshot(repository)
    repository.markPendingOutboxExported()
    return json
  }

  // Theme & Sync preferences
  fun setSeedPreset(preset: ThemeSeedPreset) =
    viewModelScope.launch { preferences.setSeedPreset(preset) }

  fun setSurfaceMode(mode: ThemeSurfaceMode) =
    viewModelScope.launch { preferences.setSurfaceMode(mode) }

  fun setUseDynamicColor(enabled: Boolean) =
    viewModelScope.launch { preferences.setUseDynamicColor(enabled) }

  fun setAutoSyncOnReconnect(enabled: Boolean) =
    viewModelScope.launch { preferences.setAutoSyncOnReconnect(enabled) }

  fun setSyncWebhookUrl(url: String) =
    viewModelScope.launch {
      preferences.setSyncWebhookUrl(url)
      _snackbarEvents.tryEmit(
        if (url.isBlank()) "Cleared remote sync URL (local-first export mode)."
        else "Saved ERPClaw sync endpoint."
      )
    }

  fun setUseGeminiOnline(enabled: Boolean) =
    viewModelScope.launch { preferences.setUseGeminiOnline(enabled) }

  private suspend fun loadScaledBitmap(uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
      try {
        val resolver = getApplication<Application>().contentResolver
        val raw =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri))
          } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(resolver, uri)
          }
        val maxDim = 1024
        val scale = minOf(1f, maxDim.toFloat() / maxOf(raw.width, raw.height))
        if (scale < 1f) {
          Bitmap.createScaledBitmap(
            raw,
            (raw.width * scale).toInt().coerceAtLeast(1),
            (raw.height * scale).toInt().coerceAtLeast(1),
            true,
          )
        } else {
          raw
        }
      } catch (_: Exception) {
        null
      }
    }
}
