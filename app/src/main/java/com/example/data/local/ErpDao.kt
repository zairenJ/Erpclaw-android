package com.example.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class JournalEntryWithLines(
  @Embedded val entry: JournalEntryEntity,
  @Relation(parentColumn = "id", entityColumn = "entryId") val lines: List<JournalLineEntity>,
)

@Dao
interface ErpDao {
  // Accounting
  @Query("SELECT * FROM accounts ORDER BY code ASC")
  fun observeAccounts(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts ORDER BY code ASC")
  suspend fun getAccountsOnce(): List<AccountEntity>

  @Query("SELECT * FROM accounts WHERE code = :code LIMIT 1")
  suspend fun getAccountByCode(code: String): AccountEntity?

  @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
  suspend fun getAccountById(id: Long): AccountEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAccounts(accounts: List<AccountEntity>)

  @Update suspend fun updateAccount(account: AccountEntity)

  @Transaction
  @Query("SELECT * FROM journal_entries ORDER BY dateMillis DESC, id DESC")
  fun observeJournalEntriesWithLines(): Flow<List<JournalEntryWithLines>>

  @Transaction
  @Query("SELECT * FROM journal_entries ORDER BY dateMillis DESC, id DESC")
  suspend fun getJournalEntriesOnce(): List<JournalEntryWithLines>

  @Insert suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

  @Insert suspend fun insertJournalLines(lines: List<JournalLineEntity>)

  // Inventory
  @Query("SELECT * FROM inventory_items ORDER BY name ASC")
  fun observeInventoryItems(): Flow<List<InventoryItemEntity>>

  @Query("SELECT * FROM inventory_items ORDER BY name ASC")
  suspend fun getInventoryItemsOnce(): List<InventoryItemEntity>

  @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
  suspend fun getInventoryItemById(id: Long): InventoryItemEntity?

  @Query("SELECT * FROM inventory_items WHERE LOWER(sku) = LOWER(:sku) LIMIT 1")
  suspend fun getInventoryItemBySku(sku: String): InventoryItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInventoryItem(item: InventoryItemEntity): Long

  @Query("SELECT * FROM stock_movements ORDER BY timestampMillis DESC LIMIT 100")
  fun observeStockMovements(): Flow<List<StockMovementEntity>>

  @Insert suspend fun insertStockMovement(movement: StockMovementEntity): Long

  // Sales & CRM
  @Query("SELECT * FROM customers ORDER BY name ASC")
  fun observeCustomers(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers ORDER BY name ASC")
  suspend fun getCustomersOnce(): List<CustomerEntity>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: Long): CustomerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCustomer(customer: CustomerEntity): Long

  @Query("SELECT * FROM invoices ORDER BY createdAtMillis DESC, id DESC")
  fun observeInvoices(): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoices ORDER BY createdAtMillis DESC, id DESC")
  suspend fun getInvoicesOnce(): List<InvoiceEntity>

  @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
  suspend fun getInvoiceById(id: Long): InvoiceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInvoice(invoice: InvoiceEntity): Long

  @Insert suspend fun insertInvoiceLines(lines: List<InvoiceLineEntity>)

  @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId")
  suspend fun getInvoiceLines(invoiceId: Long): List<InvoiceLineEntity>

  // Purchasing & Vendors
  @Query("SELECT * FROM vendors ORDER BY name ASC")
  fun observeVendors(): Flow<List<VendorEntity>>

  @Query("SELECT * FROM vendors ORDER BY name ASC")
  suspend fun getVendorsOnce(): List<VendorEntity>

  @Query("SELECT * FROM vendors WHERE id = :id LIMIT 1")
  suspend fun getVendorById(id: Long): VendorEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertVendor(vendor: VendorEntity): Long

  @Query("SELECT * FROM purchase_bills ORDER BY createdAtMillis DESC, id DESC")
  fun observePurchaseBills(): Flow<List<PurchaseBillEntity>>

  @Query("SELECT * FROM purchase_bills ORDER BY createdAtMillis DESC, id DESC")
  suspend fun getPurchaseBillsOnce(): List<PurchaseBillEntity>

  @Query("SELECT * FROM purchase_bills WHERE id = :id LIMIT 1")
  suspend fun getPurchaseBillById(id: Long): PurchaseBillEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPurchaseBill(bill: PurchaseBillEntity): Long

  @Insert suspend fun insertPurchaseBillLines(lines: List<PurchaseBillLineEntity>)

  @Query("SELECT * FROM purchase_bill_lines WHERE billId = :billId")
  suspend fun getPurchaseBillLines(billId: Long): List<PurchaseBillLineEntity>

  // HR, Expenses & Payroll
  @Query("SELECT * FROM employees ORDER BY name ASC")
  fun observeEmployees(): Flow<List<EmployeeEntity>>

  @Query("SELECT * FROM employees ORDER BY name ASC")
  suspend fun getEmployeesOnce(): List<EmployeeEntity>

  @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
  suspend fun getEmployeeById(id: Long): EmployeeEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertEmployee(employee: EmployeeEntity): Long

  @Query("SELECT * FROM expense_claims ORDER BY createdAtMillis DESC, id DESC")
  fun observeExpenseClaims(): Flow<List<ExpenseClaimEntity>>

  @Query("SELECT * FROM expense_claims ORDER BY createdAtMillis DESC, id DESC")
  suspend fun getExpenseClaimsOnce(): List<ExpenseClaimEntity>

  @Query("SELECT * FROM expense_claims WHERE id = :id LIMIT 1")
  suspend fun getExpenseClaimById(id: Long): ExpenseClaimEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExpenseClaim(claim: ExpenseClaimEntity): Long

  @Query("SELECT * FROM payroll_runs ORDER BY createdAtMillis DESC, id DESC")
  fun observePayrollRuns(): Flow<List<PayrollRunEntity>>

  @Query("SELECT * FROM payroll_runs ORDER BY createdAtMillis DESC, id DESC")
  suspend fun getPayrollRunsOnce(): List<PayrollRunEntity>

  @Insert suspend fun insertPayrollRun(run: PayrollRunEntity): Long

  // Sync Outbox
  @Query("SELECT * FROM sync_outbox ORDER BY createdAtMillis DESC, id DESC")
  fun observeSyncOutbox(): Flow<List<SyncOutboxEntity>>

  @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAtMillis ASC")
  suspend fun getPendingOutboxItems(): List<SyncOutboxEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOutboxItem(item: SyncOutboxEntity): Long

  @Query("UPDATE sync_outbox SET status = 'EXPORTED', syncedAtMillis = :timestamp WHERE status = 'PENDING'")
  suspend fun markPendingOutboxExported(timestamp: Long)
}
