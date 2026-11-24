package com.example.data

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BillStatus
import com.example.data.local.CrmStage
import com.example.data.local.CustomerEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.ErpDatabase
import com.example.data.local.ExpenseClaimEntity
import com.example.data.local.ExpenseStatus
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceLineEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.JournalEntryEntity
import com.example.data.local.JournalEntryWithLines
import com.example.data.local.JournalLineEntity
import com.example.data.local.PayrollRunEntity
import com.example.data.local.PurchaseBillEntity
import com.example.data.local.PurchaseBillLineEntity
import com.example.data.local.StockMovementEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.SyncStatus
import com.example.data.local.VendorEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

data class JournalPostingLine(
  val accountId: Long,
  val debitCents: Long,
  val creditCents: Long,
)

object MoneyUtils {
  private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

  fun formatCents(cents: Long): String {
    val decimal = BigDecimal(cents).movePointLeft(2)
    return currencyFormat.format(decimal)
  }

  fun parseDollarsToCents(raw: String): Long? {
    val cleaned = raw.trim().replace("$", "").replace(",", "")
    if (cleaned.isEmpty()) return null
    return try {
      val bd = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
      bd.movePointRight(2).longValueExact()
    } catch (_: Exception) {
      null
    }
  }
}

class ErpRepository(private val db: ErpDatabase) {
  private val dao = db.erpDao()

  val accounts: Flow<List<AccountEntity>> = dao.observeAccounts()
  val journalEntries: Flow<List<JournalEntryWithLines>> = dao.observeJournalEntriesWithLines()
  val inventoryItems: Flow<List<InventoryItemEntity>> = dao.observeInventoryItems()
  val stockMovements: Flow<List<StockMovementEntity>> = dao.observeStockMovements()
  val customers: Flow<List<CustomerEntity>> = dao.observeCustomers()
  val invoices: Flow<List<InvoiceEntity>> = dao.observeInvoices()
  val vendors: Flow<List<VendorEntity>> = dao.observeVendors()
  val purchaseBills: Flow<List<PurchaseBillEntity>> = dao.observePurchaseBills()
  val employees: Flow<List<EmployeeEntity>> = dao.observeEmployees()
  val expenseClaims: Flow<List<ExpenseClaimEntity>> = dao.observeExpenseClaims()
  val payrollRuns: Flow<List<PayrollRunEntity>> = dao.observePayrollRuns()
  val syncOutbox: Flow<List<SyncOutboxEntity>> = dao.observeSyncOutbox()

  suspend fun ensureChartOfAccountsInitialized() {
    if (dao.getAccountsOnce().isNotEmpty()) return
    val defaults =
      listOf(
        AccountEntity(code = "1000", name = "Cash & Bank", type = AccountType.ASSET.name),
        AccountEntity(code = "1100", name = "Accounts Receivable", type = AccountType.ASSET.name),
        AccountEntity(code = "1200", name = "Inventory Asset", type = AccountType.ASSET.name),
        AccountEntity(code = "2000", name = "Accounts Payable", type = AccountType.LIABILITY.name),
        AccountEntity(
          code = "2100",
          name = "Payroll & Tax Payable",
          type = AccountType.LIABILITY.name,
        ),
        AccountEntity(code = "3000", name = "Owner Equity", type = AccountType.EQUITY.name),
        AccountEntity(code = "4000", name = "Sales Revenue", type = AccountType.REVENUE.name),
        AccountEntity(code = "5000", name = "Cost of Goods Sold", type = AccountType.EXPENSE.name),
        AccountEntity(code = "5100", name = "Operating Expenses", type = AccountType.EXPENSE.name),
        AccountEntity(code = "5200", name = "Payroll Expense", type = AccountType.EXPENSE.name),
      )
    dao.insertAccounts(defaults)
  }

  suspend fun postBalancedJournal(
    memo: String,
    moduleSource: String,
    lines: List<JournalPostingLine>,
    queueSync: Boolean = true,
  ): Long {
    require(memo.isNotBlank()) { "Journal memo cannot be blank." }
    require(lines.size >= 2) { "Double-entry journal requires at least two lines." }
    var totalDebit = 0L
    var totalCredit = 0L
    for (line in lines) {
      require(line.debitCents >= 0L && line.creditCents >= 0L) {
        "Debit and credit amounts must be non-negative."
      }
      require((line.debitCents > 0L) xor (line.creditCents > 0L)) {
        "Each journal line must have either a positive debit or a positive credit."
      }
      totalDebit += line.debitCents
      totalCredit += line.creditCents
    }
    require(totalDebit == totalCredit && totalDebit > 0L) {
      "Double-entry invariant violated: Debits (${MoneyUtils.formatCents(totalDebit)}) != Credits (${MoneyUtils.formatCents(totalCredit)})."
    }

    return db.withTransaction {
      ensureChartOfAccountsInitialized()
      val now = System.currentTimeMillis()
      val entryNum = "JE-${now % 1_000_000}"
      val entryId =
        dao.insertJournalEntry(
          JournalEntryEntity(
            entryNumber = entryNum,
            dateMillis = now,
            memo = memo.trim(),
            moduleSource = moduleSource,
          )
        )

      val resolvedLines =
        lines.map { line ->
          val account =
            dao.getAccountById(line.accountId)
              ?: throw IllegalArgumentException("Account ID ${line.accountId} not found.")
          val accountType = AccountType.valueOf(account.type)
          val delta =
            if (accountType.normalDebit) {
              line.debitCents - line.creditCents
            } else {
              line.creditCents - line.debitCents
            }
          dao.updateAccount(account.copy(balanceCents = account.balanceCents + delta))
          JournalLineEntity(
            entryId = entryId,
            accountId = account.id,
            accountCode = account.code,
            accountName = account.name,
            debitCents = line.debitCents,
            creditCents = line.creditCents,
          )
        }
      dao.insertJournalLines(resolvedLines)

      if (queueSync) {
        enqueueOutbox(
          entityType = "journal_entry",
          entityId = entryId,
          operation = "POST",
          summary = "$entryNum • ${memo.trim()} (${MoneyUtils.formatCents(totalDebit)})",
          payload =
            JSONObject()
              .put("entryNumber", entryNum)
              .put("memo", memo.trim())
              .put("moduleSource", moduleSource)
              .put("totalCents", totalDebit),
        )
      }
      entryId
    }
  }

  suspend fun addAccount(code: String, name: String, type: AccountType): Long {
    require(code.isNotBlank() && name.isNotBlank()) { "Account code and name are required." }
    return db.withTransaction {
      val account = AccountEntity(code = code.trim(), name = name.trim(), type = type.name)
      dao.insertAccounts(listOf(account))
      val inserted = dao.getAccountByCode(code.trim()) ?: account
      enqueueOutbox(
        entityType = "account",
        entityId = inserted.id,
        operation = "CREATE",
        summary = "Account ${inserted.code} ${inserted.name}",
        payload =
          JSONObject().put("code", inserted.code).put("name", inserted.name).put("type", type.name),
      )
      inserted.id
    }
  }

  suspend fun upsertInventoryItem(
    sku: String,
    name: String,
    category: String,
    unitCostCents: Long,
    salePriceCents: Long,
    initialQty: Int,
    reorderPoint: Int = 5,
  ): Long {
    require(sku.isNotBlank() && name.isNotBlank()) { "SKU and item name are required." }
    require(initialQty >= 0 && unitCostCents >= 0L && salePriceCents >= 0L) {
      "Quantity and prices must be non-negative."
    }
    return db.withTransaction {
      ensureChartOfAccountsInitialized()
      val existing = dao.getInventoryItemBySku(sku.trim())
      val itemId =
        dao.upsertInventoryItem(
          InventoryItemEntity(
            id = existing?.id ?: 0,
            sku = sku.trim().uppercase(Locale.US),
            name = name.trim(),
            category = category.trim().ifBlank { "General" },
            unitCostCents = unitCostCents,
            salePriceCents = salePriceCents,
            quantityOnHand = initialQty,
            reorderPoint = reorderPoint.coerceAtLeast(0),
          )
        )
      if (existing == null && initialQty > 0) {
        dao.insertStockMovement(
          StockMovementEntity(
            itemId = itemId,
            itemSku = sku.trim().uppercase(Locale.US),
            itemName = name.trim(),
            deltaQty = initialQty,
            reason = "Initial Stock",
          )
        )
        val totalCost = unitCostCents * initialQty
        if (totalCost > 0L) {
          val invAcct = dao.getAccountByCode("1200")!!
          val equityAcct = dao.getAccountByCode("3000")!!
          postBalancedJournal(
            memo = "Opening stock for ${sku.trim().uppercase(Locale.US)}",
            moduleSource = "INVENTORY",
            lines =
              listOf(
                JournalPostingLine(invAcct.id, debitCents = totalCost, creditCents = 0L),
                JournalPostingLine(equityAcct.id, debitCents = 0L, creditCents = totalCost),
              ),
            queueSync = false,
          )
        }
      }
      enqueueOutbox(
        entityType = "inventory_item",
        entityId = itemId,
        operation = if (existing == null) "CREATE" else "UPDATE",
        summary = "${sku.trim().uppercase(Locale.US)} • ${name.trim()} (Qty: $initialQty)",
        payload =
          JSONObject()
            .put("sku", sku.trim().uppercase(Locale.US))
            .put("name", name.trim())
            .put("qty", initialQty)
            .put("unitCostCents", unitCostCents)
            .put("salePriceCents", salePriceCents),
      )
      itemId
    }
  }

  suspend fun adjustStock(itemId: Long, deltaQty: Int, reason: String) {
    require(deltaQty != 0) { "Stock adjustment quantity must be non-zero." }
    db.withTransaction {
      ensureChartOfAccountsInitialized()
      val item =
        dao.getInventoryItemById(itemId)
          ?: throw IllegalArgumentException("Inventory item not found.")
      val newQty = item.quantityOnHand + deltaQty
      require(newQty >= 0) {
        "Insufficient stock for ${item.sku}: on hand ${item.quantityOnHand}, requested delta $deltaQty."
      }
      dao.upsertInventoryItem(item.copy(quantityOnHand = newQty))
      dao.insertStockMovement(
        StockMovementEntity(
          itemId = item.id,
          itemSku = item.sku,
          itemName = item.name,
          deltaQty = deltaQty,
          reason = reason.ifBlank { "Manual Adjustment" },
        )
      )
      val valueDelta = kotlin.math.abs(deltaQty.toLong()) * item.unitCostCents
      if (valueDelta > 0L) {
        val invAcct = dao.getAccountByCode("1200")!!
        val cogsAcct = dao.getAccountByCode("5000")!!
        val lines =
          if (deltaQty > 0) {
            listOf(
              JournalPostingLine(invAcct.id, debitCents = valueDelta, creditCents = 0L),
              JournalPostingLine(cogsAcct.id, debitCents = 0L, creditCents = valueDelta),
            )
          } else {
            listOf(
              JournalPostingLine(cogsAcct.id, debitCents = valueDelta, creditCents = 0L),
              JournalPostingLine(invAcct.id, debitCents = 0L, creditCents = valueDelta),
            )
          }
        postBalancedJournal(
          memo = "Stock adjustment (${if (deltaQty > 0) "+$deltaQty" else deltaQty}) ${item.sku}: $reason",
          moduleSource = "INVENTORY",
          lines = lines,
          queueSync = false,
        )
      }
      enqueueOutbox(
        entityType = "stock_movement",
        entityId = item.id,
        operation = "ADJUST",
        summary = "${item.sku} ${if (deltaQty > 0) "+$deltaQty" else deltaQty} → $newQty on hand",
        payload =
          JSONObject()
            .put("sku", item.sku)
            .put("deltaQty", deltaQty)
            .put("newQty", newQty)
            .put("reason", reason),
      )
    }
  }

  suspend fun createCustomer(name: String, email: String, phone: String, stage: CrmStage): Long {
    require(name.isNotBlank()) { "Customer name is required." }
    return db.withTransaction {
      val id =
        dao.upsertCustomer(
          CustomerEntity(
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            stage = stage.name,
          )
        )
      enqueueOutbox(
        entityType = "customer",
        entityId = id,
        operation = "CREATE",
        summary = "CRM ${stage.label}: ${name.trim()}",
        payload = JSONObject().put("name", name.trim()).put("stage", stage.name),
      )
      id
    }
  }

  suspend fun updateCustomerStage(customerId: Long, stage: CrmStage) {
    db.withTransaction {
      val customer = dao.getCustomerById(customerId) ?: return@withTransaction
      dao.upsertCustomer(customer.copy(stage = stage.name))
      enqueueOutbox(
        entityType = "customer",
        entityId = customerId,
        operation = "STAGE_UPDATE",
        summary = "${customer.name} moved to ${stage.label}",
        payload = JSONObject().put("customerId", customerId).put("stage", stage.name),
      )
    }
  }

  suspend fun createInvoice(
    customerId: Long,
    itemId: Long?,
    description: String,
    quantity: Int,
    unitPriceCents: Long,
  ): Long {
    require(quantity > 0 && unitPriceCents > 0L) {
      "Invoice quantity and unit price must be positive."
    }
    return db.withTransaction {
      ensureChartOfAccountsInitialized()
      val customer =
        dao.getCustomerById(customerId)
          ?: throw IllegalArgumentException("Selected customer not found.")
      val totalCents = quantity.toLong() * unitPriceCents
      val now = System.currentTimeMillis()
      val invNum = "INV-${now % 1_000_000}"

      val invoiceId =
        dao.upsertInvoice(
          InvoiceEntity(
            invoiceNumber = invNum,
            customerId = customer.id,
            customerName = customer.name,
            status = InvoiceStatus.SENT.name,
            totalCents = totalCents,
            dueDateMillis = now + 30L * 24 * 60 * 60 * 1000,
            createdAtMillis = now,
          )
        )
      dao.insertInvoiceLines(
        listOf(
          InvoiceLineEntity(
            invoiceId = invoiceId,
            itemId = itemId,
            description = description.ifBlank { "Goods / Services" },
            quantity = quantity,
            unitPriceCents = unitPriceCents,
          )
        )
      )
      dao.upsertCustomer(
        customer.copy(
          stage = CrmStage.ACTIVE.name,
          balanceDueCents = customer.balanceDueCents + totalCents,
        )
      )

      if (itemId != null) {
        val item = dao.getInventoryItemById(itemId)
        if (item != null && item.quantityOnHand >= quantity) {
          adjustStock(item.id, -quantity, "Sold on $invNum")
        }
      }

      val arAcct = dao.getAccountByCode("1100")!!
      val revAcct = dao.getAccountByCode("4000")!!
      postBalancedJournal(
        memo = "$invNum issued to ${customer.name}",
        moduleSource = "SALES",
        lines =
          listOf(
            JournalPostingLine(arAcct.id, debitCents = totalCents, creditCents = 0L),
            JournalPostingLine(revAcct.id, debitCents = 0L, creditCents = totalCents),
          ),
        queueSync = false,
      )

      enqueueOutbox(
        entityType = "invoice",
        entityId = invoiceId,
        operation = "CREATE",
        summary = "$invNum • ${customer.name} (${MoneyUtils.formatCents(totalCents)})",
        payload =
          JSONObject()
            .put("invoiceNumber", invNum)
            .put("customer", customer.name)
            .put("totalCents", totalCents),
      )
      invoiceId
    }
  }

  suspend fun markInvoicePaid(invoiceId: Long) {
    db.withTransaction {
      ensureChartOfAccountsInitialized()
      val invoice = dao.getInvoiceById(invoiceId) ?: return@withTransaction
      if (invoice.status == InvoiceStatus.PAID.name) return@withTransaction

      dao.upsertInvoice(invoice.copy(status = InvoiceStatus.PAID.name))
      dao.getCustomerById(invoice.customerId)?.let { customer ->
        dao.upsertCustomer(
          customer.copy(balanceDueCents = (customer.balanceDueCents - invoice.totalCents).coerceAtLeast(0L))
        )
      }

      val cashAcct = dao.getAccountByCode("1000")!!
      val arAcct = dao.getAccountByCode("1100")!!
      postBalancedJournal(
        memo = "Payment received for ${invoice.invoiceNumber} (${invoice.customerName})",
        moduleSource = "SALES",
        lines =
          listOf(
            JournalPostingLine(cashAcct.id, debitCents = invoice.totalCents, creditCents = 0L),
            JournalPostingLine(arAcct.id, debitCents = 0L, creditCents = invoice.totalCents),
          ),
        queueSync = false,
      )
      enqueueOutbox(
        entityType = "invoice_payment",
        entityId = invoiceId,
        operation = "PAY",
        summary = "${invoice.invoiceNumber} paid (${MoneyUtils.formatCents(invoice.totalCents)})",
        payload =
          JSONObject()
            .put("invoiceNumber", invoice.invoiceNumber)
            .put("amountCents", invoice.totalCents),
      )
    }
  }

  suspend fun createVendor(name: String, email: String, paymentTerms: String): Long {
    require(name.isNotBlank()) { "Vendor name is required." }
    return db.withTransaction {
      val id =
        dao.upsertVendor(
          VendorEntity(
            name = name.trim(),
            email = email.trim(),
            paymentTerms = paymentTerms.ifBlank { "Net 30" },
          )
        )
      enqueueOutbox(
        entityType = "vendor",
        entityId = id,
        operation = "CREATE",
        summary = "Vendor: ${name.trim()}",
        payload = JSONObject().put("name", name.trim()).put("terms", paymentTerms),
      )
      id
    }
  }

  suspend fun createPurchaseBill(
    vendorId: Long,
    itemId: Long?,
    description: String,
    quantity: Int,
    unitCostCents: Long,
  ): Long {
    require(quantity > 0 && unitCostCents > 0L) {
      "Purchase quantity and unit cost must be positive."
    }
    return db.withTransaction {
      ensureChartOfAccountsInitialized()
      val vendor =
        dao.getVendorById(vendorId) ?: throw IllegalArgumentException("Vendor not found.")
      val totalCents = quantity.toLong() * unitCostCents
      val now = System.currentTimeMillis()
      val billNum = "BILL-${now % 1_000_000}"

      val billId =
        dao.upsertPurchaseBill(
          PurchaseBillEntity(
            billNumber = billNum,
            vendorId = vendor.id,
            vendorName = vendor.name,
            status = BillStatus.OPEN.name,
            totalCents = totalCents,
            dueDateMillis = now + 30L * 24 * 60 * 60 * 1000,
            createdAtMillis = now,
          )
        )
      dao.insertPurchaseBillLines(
        listOf(
          PurchaseBillLineEntity(
            billId = billId,
            itemId = itemId,
            description = description.ifBlank { "Vendor Supply" },
            quantity = quantity,
            unitCostCents = unitCostCents,
          )
        )
      )
      dao.upsertVendor(vendor.copy(payableCents = vendor.payableCents + totalCents))

      val debitAcct =
        if (itemId != null) {
          val item = dao.getInventoryItemById(itemId)
          if (item != null) {
            dao.upsertInventoryItem(item.copy(quantityOnHand = item.quantityOnHand + quantity))
            dao.insertStockMovement(
              StockMovementEntity(
                itemId = item.id,
                itemSku = item.sku,
                itemName = item.name,
                deltaQty = quantity,
                reason = "Received on $billNum",
              )
            )
          }
          dao.getAccountByCode("1200")!!
        } else {
          dao.getAccountByCode("5100")!!
        }
      val apAcct = dao.getAccountByCode("2000")!!
      postBalancedJournal(
        memo = "$billNum from ${vendor.name}",
        moduleSource = "PURCHASING",
        lines =
          listOf(
            JournalPostingLine(debitAcct.id, debitCents = totalCents, creditCents = 0L),
            JournalPostingLine(apAcct.id, debitCents = 0L, creditCents = totalCents),
          ),
        queueSync = false,
      )

      enqueueOutbox(
        entityType = "purchase_bill",
        entityId = billId,
        operation = "CREATE",
        summary = "$billNum • ${vendor.name} (${MoneyUtils.formatCents(totalCents)})",
        payload =
          JSONObject()
            .put("billNumber", billNum)
            .put("vendor", vendor.name)
            .put("totalCents", totalCents),
      )
      billId
    }
  }

  suspend fun payPurchaseBill(billId: Long) {
    db.withTransaction {
      ensureChartOfAccountsInitialized()
      val bill = dao.getPurchaseBillById(billId) ?: return@withTransaction
      if (bill.status == BillStatus.PAID.name) return@withTransaction

      dao.upsertPurchaseBill(bill.copy(status = BillStatus.PAID.name))
      dao.getVendorById(bill.vendorId)?.let { vendor ->
        dao.upsertVendor(
          vendor.copy(payableCents = (vendor.payableCents - bill.totalCents).coerceAtLeast(0L))
        )
      }

      val apAcct = dao.getAccountByCode("2000")!!
      val cashAcct = dao.getAccountByCode("1000")!!
      postBalancedJournal(
        memo = "Paid ${bill.billNumber} to ${bill.vendorName}",
        moduleSource = "PURCHASING",
        lines =
          listOf(
            JournalPostingLine(apAcct.id, debitCents = bill.totalCents, creditCents = 0L),
            JournalPostingLine(cashAcct.id, debitCents = 0L, creditCents = bill.totalCents),
          ),
        queueSync = false,
      )

      enqueueOutbox(
        entityType = "bill_payment",
        entityId = billId,
        operation = "PAY",
        summary = "${bill.billNumber} settled (${MoneyUtils.formatCents(bill.totalCents)})",
        payload =
          JSONObject().put("billNumber", bill.billNumber).put("amountCents", bill.totalCents),
      )
    }
  }

  suspend fun createEmployee(
    name: String,
    role: String,
    department: String,
    monthlySalaryCents: Long,
  ): Long {
    require(name.isNotBlank() && monthlySalaryCents > 0L) {
      "Employee name and positive salary are required."
    }
    return db.withTransaction {
      val id =
        dao.upsertEmployee(
          EmployeeEntity(
            name = name.trim(),
            role = role.ifBlank { "Associate" },
            department = department.ifBlank { "Operations" },
            monthlySalaryCents = monthlySalaryCents,
          )
        )
      enqueueOutbox(
        entityType = "employee",
        entityId = id,
        operation = "CREATE",
        summary = "HR Onboard: ${name.trim()} (${role.ifBlank { "Associate" }})",
        payload =
          JSONObject()
            .put("name", name.trim())
            .put("department", department)
            .put("monthlySalaryCents", monthlySalaryCents),
      )
      id
    }
  }

  suspend fun submitExpenseClaim(
    employeeId: Long?,
    employeeName: String,
    category: String,
    amountCents: Long,
    description: String,
    receiptUri: String? = null,
    queueAiEnrichment: Boolean = false,
  ): Long {
    require(amountCents > 0L && description.isNotBlank()) {
      "Expense amount and description are required."
    }
    return db.withTransaction {
      val resolvedName =
        if (employeeId != null) {
          dao.getEmployeeById(employeeId)?.name ?: employeeName
        } else {
          employeeName.ifBlank { "Owner / Field Operator" }
        }
      val claimId =
        dao.upsertExpenseClaim(
          ExpenseClaimEntity(
            employeeId = employeeId,
            employeeName = resolvedName,
            category = category.ifBlank { "Operations" },
            amountCents = amountCents,
            description = description.trim(),
            receiptUri = receiptUri,
            status = ExpenseStatus.SUBMITTED.name,
          )
        )
      enqueueOutbox(
        entityType = "expense_claim",
        entityId = claimId,
        operation = "SUBMIT",
        summary = "${category.ifBlank { "Expense" }}: ${MoneyUtils.formatCents(amountCents)} — ${description.trim()}",
        payload =
          JSONObject()
            .put("employee", resolvedName)
            .put("category", category)
            .put("amountCents", amountCents)
            .put("description", description.trim()),
        aiEnrichmentPending = queueAiEnrichment,
        aiPrompt =
          if (queueAiEnrichment) {
            "Classify accounting category and tax-deductibility note for expense: '${description.trim()}' (${MoneyUtils.formatCents(amountCents)})."
          } else {
            ""
          },
      )
      claimId
    }
  }

  suspend fun approveAndPayExpense(claimId: Long) {
    db.withTransaction {
      ensureChartOfAccountsInitialized()
      val claim = dao.getExpenseClaimById(claimId) ?: return@withTransaction
      if (claim.status == ExpenseStatus.PAID.name) return@withTransaction

      dao.upsertExpenseClaim(claim.copy(status = ExpenseStatus.PAID.name))
      val expAcct = dao.getAccountByCode("5100")!!
      val cashAcct = dao.getAccountByCode("1000")!!
      postBalancedJournal(
        memo = "Expense reimbursed: ${claim.description} (${claim.employeeName})",
        moduleSource = "HR_EXPENSE",
        lines =
          listOf(
            JournalPostingLine(expAcct.id, debitCents = claim.amountCents, creditCents = 0L),
            JournalPostingLine(cashAcct.id, debitCents = 0L, creditCents = claim.amountCents),
          ),
        queueSync = false,
      )
      enqueueOutbox(
        entityType = "expense_reimbursement",
        entityId = claimId,
        operation = "PAY",
        summary = "Paid expense #${claimId} (${MoneyUtils.formatCents(claim.amountCents)})",
        payload = JSONObject().put("claimId", claimId).put("amountCents", claim.amountCents),
      )
    }
  }

  suspend fun runPayrollForEmployee(
    employeeId: Long,
    periodLabel: String,
    taxPercent: Int = 20,
  ): Long {
    return db.withTransaction {
      ensureChartOfAccountsInitialized()
      val emp =
        dao.getEmployeeById(employeeId) ?: throw IllegalArgumentException("Employee not found.")
      val gross = emp.monthlySalaryCents
      val taxBd =
        BigDecimal(gross)
          .multiply(BigDecimal(taxPercent.coerceIn(0, 50)))
          .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
      val taxCents = taxBd.longValueExact()
      val netCents = gross - taxCents

      val runId =
        dao.insertPayrollRun(
          PayrollRunEntity(
            periodLabel = periodLabel.ifBlank { "Current Period" },
            employeeId = emp.id,
            employeeName = emp.name,
            grossCents = gross,
            taxCents = taxCents,
            netCents = netCents,
          )
        )

      val payrollExp = dao.getAccountByCode("5200")!!
      val cashAcct = dao.getAccountByCode("1000")!!
      val taxLiability = dao.getAccountByCode("2100")!!
      val lines = buildList {
        add(JournalPostingLine(payrollExp.id, debitCents = gross, creditCents = 0L))
        if (netCents > 0L) {
          add(JournalPostingLine(cashAcct.id, debitCents = 0L, creditCents = netCents))
        }
        if (taxCents > 0L) {
          add(JournalPostingLine(taxLiability.id, debitCents = 0L, creditCents = taxCents))
        }
      }
      postBalancedJournal(
        memo = "Payroll $periodLabel — ${emp.name}",
        moduleSource = "PAYROLL",
        lines = lines,
        queueSync = false,
      )
      enqueueOutbox(
        entityType = "payroll_run",
        entityId = runId,
        operation = "POST",
        summary = "Payroll $periodLabel • ${emp.name} (Net ${MoneyUtils.formatCents(netCents)})",
        payload =
          JSONObject()
            .put("employee", emp.name)
            .put("period", periodLabel)
            .put("grossCents", gross)
            .put("taxCents", taxCents)
            .put("netCents", netCents),
      )
      runId
    }
  }

  suspend fun updateExpenseAiNote(claimId: Long, note: String, suggestedCategory: String?) {
    db.withTransaction {
      val claim = dao.getExpenseClaimById(claimId) ?: return@withTransaction
      dao.upsertExpenseClaim(
        claim.copy(
          aiCategoryNote = note,
          category = suggestedCategory?.takeIf { it.isNotBlank() } ?: claim.category,
        )
      )
    }
  }

  suspend fun loadSampleWorkspace() {
    if (dao.getInventoryItemsOnce().isNotEmpty() || dao.getCustomersOnce().isNotEmpty()) return
    db.withTransaction {
      ensureChartOfAccountsInitialized()
      val cash = dao.getAccountByCode("1000")!!
      val equity = dao.getAccountByCode("3000")!!
      postBalancedJournal(
        memo = "Initial Operating Capital",
        moduleSource = "ACCOUNTING",
        lines =
          listOf(
            JournalPostingLine(cash.id, debitCents = 2500000L, creditCents = 0L),
            JournalPostingLine(equity.id, debitCents = 0L, creditCents = 2500000L),
          ),
        queueSync = false,
      )
      val sku1 =
        upsertInventoryItem(
          sku = "CLAW-SENS-01",
          name = "Edge IoT Gateway",
          category = "Hardware",
          unitCostCents = 8500L,
          salePriceCents = 19900L,
          initialQty = 24,
          reorderPoint = 8,
        )
      upsertInventoryItem(
        sku = "CLAW-CBL-04",
        name = "Braided USB-C Telemetry Kit",
        category = "Accessories",
        unitCostCents = 1200L,
        salePriceCents = 3400L,
        initialQty = 4,
        reorderPoint = 10,
      )
      val cust1 = createCustomer("Acme Logistics", "ap@acmelogistics.io", "555-0142", CrmStage.ACTIVE)
      createCustomer("Northstar Coldchain", "ops@northstar.co", "555-0198", CrmStage.QUALIFIED)
      createInvoice(cust1, sku1, "Edge IoT Gateway", 5, 19900L)
      val vendor1 = createVendor("Apex Silicon Foundry", "billing@apexsilicon.com", "Net 30")
      createPurchaseBill(vendor1, null, "PCB Fabrication Batch #42", 1, 64000L)
      val emp1 = createEmployee("Elena Vance", "Field Operations Lead", "Operations", 620000L)
      submitExpenseClaim(emp1, "Elena Vance", "Travel", 4850L, "Client site transit & lunch")
    }
  }

  suspend fun getPendingOutboxItems(): List<SyncOutboxEntity> = dao.getPendingOutboxItems()

  suspend fun updateOutboxItem(item: SyncOutboxEntity) = dao.upsertOutboxItem(item)

  suspend fun markPendingOutboxExported() =
    dao.markPendingOutboxExported(System.currentTimeMillis())

  suspend fun getAccountsOnce() = dao.getAccountsOnce()

  suspend fun getJournalEntriesOnce() = dao.getJournalEntriesOnce()

  suspend fun getInventoryItemsOnce() = dao.getInventoryItemsOnce()

  suspend fun getCustomersOnce() = dao.getCustomersOnce()

  suspend fun getInvoicesOnce() = dao.getInvoicesOnce()

  suspend fun getVendorsOnce() = dao.getVendorsOnce()

  suspend fun getPurchaseBillsOnce() = dao.getPurchaseBillsOnce()

  suspend fun getEmployeesOnce() = dao.getEmployeesOnce()

  suspend fun getExpenseClaimsOnce() = dao.getExpenseClaimsOnce()

  suspend fun getPayrollRunsOnce() = dao.getPayrollRunsOnce()

  private suspend fun enqueueOutbox(
    entityType: String,
    entityId: Long,
    operation: String,
    summary: String,
    payload: JSONObject,
    aiEnrichmentPending: Boolean = false,
    aiPrompt: String = "",
  ) {
    dao.upsertOutboxItem(
      SyncOutboxEntity(
        entityType = entityType,
        entityId = entityId,
        operation = operation,
        summary = summary,
        payloadJson = payload.toString(),
        status = SyncStatus.PENDING.name,
        aiEnrichmentPending = aiEnrichmentPending,
        aiPrompt = aiPrompt,
      )
    )
  }
}
