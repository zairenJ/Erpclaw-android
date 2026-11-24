package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val label: String, val normalDebit: Boolean) {
  ASSET("Asset", true),
  LIABILITY("Liability", false),
  EQUITY("Equity", false),
  REVENUE("Revenue", false),
  EXPENSE("Expense", true),
}

enum class CrmStage(val label: String) {
  LEAD("Lead"),
  QUALIFIED("Qualified"),
  ACTIVE("Active"),
}

enum class InvoiceStatus(val label: String) {
  DRAFT("Draft"),
  SENT("Sent"),
  PAID("Paid"),
}

enum class BillStatus(val label: String) {
  OPEN("Open"),
  RECEIVED("Received"),
  PAID("Paid"),
}

enum class ExpenseStatus(val label: String) {
  SUBMITTED("Submitted"),
  APPROVED("Approved"),
  PAID("Paid"),
}

enum class SyncStatus(val label: String) {
  PENDING("Pending"),
  SYNCING("Syncing"),
  SYNCED("Synced"),
  EXPORTED("Exported"),
  FAILED("Failed"),
}

@Entity(tableName = "accounts", indices = [Index(value = ["code"], unique = true)])
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val code: String,
  val name: String,
  val type: String, // AccountType.name
  val balanceCents: Long = 0L,
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val entryNumber: String,
  val dateMillis: Long,
  val memo: String,
  val moduleSource: String,
  val status: String = "POSTED",
)

@Entity(
  tableName = "journal_lines",
  foreignKeys =
    [
      ForeignKey(
        entity = JournalEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("entryId"), Index("accountId")],
)
data class JournalLineEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val entryId: Long,
  val accountId: Long,
  val accountCode: String,
  val accountName: String,
  val debitCents: Long = 0L,
  val creditCents: Long = 0L,
)

@Entity(tableName = "inventory_items", indices = [Index(value = ["sku"], unique = true)])
data class InventoryItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sku: String,
  val name: String,
  val category: String,
  val unitCostCents: Long,
  val salePriceCents: Long,
  val quantityOnHand: Int,
  val reorderPoint: Int = 5,
)

@Entity(tableName = "stock_movements", indices = [Index("itemId")])
data class StockMovementEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val itemId: Long,
  val itemSku: String,
  val itemName: String,
  val deltaQty: Int,
  val reason: String,
  val referenceId: String = "",
  val timestampMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val email: String,
  val phone: String = "",
  val stage: String = CrmStage.LEAD.name,
  val balanceDueCents: Long = 0L,
)

@Entity(tableName = "invoices", indices = [Index("customerId")])
data class InvoiceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val invoiceNumber: String,
  val customerId: Long,
  val customerName: String,
  val status: String = InvoiceStatus.SENT.name,
  val totalCents: Long,
  val dueDateMillis: Long,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "invoice_lines",
  foreignKeys =
    [
      ForeignKey(
        entity = InvoiceEntity::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("invoiceId")],
)
data class InvoiceLineEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val invoiceId: Long,
  val itemId: Long? = null,
  val description: String,
  val quantity: Int,
  val unitPriceCents: Long,
)

@Entity(tableName = "vendors")
data class VendorEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val email: String,
  val paymentTerms: String = "Net 30",
  val payableCents: Long = 0L,
)

@Entity(tableName = "purchase_bills", indices = [Index("vendorId")])
data class PurchaseBillEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val billNumber: String,
  val vendorId: Long,
  val vendorName: String,
  val status: String = BillStatus.OPEN.name,
  val totalCents: Long,
  val dueDateMillis: Long,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "purchase_bill_lines",
  foreignKeys =
    [
      ForeignKey(
        entity = PurchaseBillEntity::class,
        parentColumns = ["id"],
        childColumns = ["billId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("billId")],
)
data class PurchaseBillLineEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val billId: Long,
  val itemId: Long? = null,
  val description: String,
  val quantity: Int,
  val unitCostCents: Long,
)

@Entity(tableName = "employees")
data class EmployeeEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val role: String,
  val department: String,
  val monthlySalaryCents: Long,
)

@Entity(tableName = "expense_claims", indices = [Index("employeeId")])
data class ExpenseClaimEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val employeeId: Long? = null,
  val employeeName: String,
  val category: String,
  val amountCents: Long,
  val description: String,
  val receiptUri: String? = null,
  val status: String = ExpenseStatus.SUBMITTED.name,
  val aiCategoryNote: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "payroll_runs", indices = [Index("employeeId")])
data class PayrollRunEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val periodLabel: String,
  val employeeId: Long,
  val employeeName: String,
  val grossCents: Long,
  val taxCents: Long,
  val netCents: Long,
  val status: String = "POSTED",
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val entityType: String,
  val entityId: Long,
  val operation: String,
  val summary: String,
  val payloadJson: String,
  val status: String = SyncStatus.PENDING.name,
  val aiEnrichmentPending: Boolean = false,
  val aiPrompt: String = "",
  val errorMessage: String = "",
  val retryCount: Int = 0,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val syncedAtMillis: Long? = null,
)
