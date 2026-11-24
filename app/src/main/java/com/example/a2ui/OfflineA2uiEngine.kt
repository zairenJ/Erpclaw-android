package com.example.a2ui

import com.example.data.MoneyUtils
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.CustomerEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.VendorEntity
import java.util.Locale

data class ErpSnapshotForA2ui(
  val accounts: List<AccountEntity> = emptyList(),
  val items: List<InventoryItemEntity> = emptyList(),
  val customers: List<CustomerEntity> = emptyList(),
  val vendors: List<VendorEntity> = emptyList(),
  val employees: List<EmployeeEntity> = emptyList(),
)

object OfflineA2uiEngine {

  fun interpretCommand(command: String, snapshot: ErpSnapshotForA2ui): A2uiEnvelope {
    val lower = command.trim().lowercase(Locale.US)
    val amountMatch = Regex("""\$?\s*(\d+(?:\.\d{1,2})?)""").find(lower)?.groupValues?.get(1)
    val intMatch = Regex("""([+-]?\d+)""").find(lower)?.groupValues?.get(1)

    return when {
      "trial" in lower || "balance" in lower || "report" in lower || "ledger" in lower && "new" !in lower ->
        buildTrialBalanceSurface(snapshot)
      "payroll" in lower || "salary" in lower ->
        buildPayrollSurface(snapshot)
      "customer" in lower || "lead" in lower || "crm" in lower ->
        buildNewCustomerSurface(prefillName = extractNameHint(command))
      "vendor" in lower || "supplier" in lower ->
        buildNewVendorSurface(prefillName = extractNameHint(command))
      "employee" in lower || "staff" in lower || "hire" in lower ->
        buildNewEmployeeSurface(prefillName = extractNameHint(command))
      "new item" in lower || "add sku" in lower || "create item" in lower ->
        buildNewInventoryItemSurface()
      "stock" in lower || "inventory" in lower || "receive" in lower || "adjust" in lower -> {
        val matchedItem =
          snapshot.items.firstOrNull {
            it.sku.lowercase(Locale.US) in lower || it.name.lowercase(Locale.US) in lower
          }
        buildStockAdjustmentSurface(
          snapshot = snapshot,
          prefillItemId = matchedItem?.id?.toString(),
          prefillDelta = intMatch ?: "10",
          prefillReason = command.trim(),
        )
      }
      "bill" in lower || "purchase" in lower || "po" in lower -> {
        val matchedVendor =
          snapshot.vendors.firstOrNull { it.name.lowercase(Locale.US) in lower }
        buildPurchaseBillSurface(
          snapshot = snapshot,
          prefillVendorId = matchedVendor?.id?.toString(),
          prefillAmount = amountMatch ?: "250.00",
          prefillDesc = command.trim(),
        )
      }
      "invoice" in lower || "sale" in lower || "quote" in lower -> {
        val matchedCustomer =
          snapshot.customers.firstOrNull { it.name.lowercase(Locale.US) in lower }
        buildInvoiceSurface(
          snapshot = snapshot,
          prefillCustomerId = matchedCustomer?.id?.toString(),
          prefillUnitPrice = amountMatch ?: "199.00",
          prefillDesc = command.trim(),
        )
      }
      "journal" in lower || "debit" in lower || "credit" in lower ->
        buildJournalEntrySurface(snapshot, prefillMemo = command.trim(), prefillAmount = amountMatch ?: "100.00")
      else ->
        buildExpenseSurface(
          snapshot = snapshot,
          prefillAmount = amountMatch ?: "45.00",
          prefillDesc = command.trim().ifBlank { "Team field expense" },
        )
    }
  }

  fun buildExpenseSurface(
    snapshot: ErpSnapshotForA2ui,
    prefillAmount: String = "45.00",
    prefillDesc: String = "",
    prefillCategory: String = "Travel & Meals",
    receiptAttached: Boolean = false,
  ): A2uiEnvelope {
    val empOptions =
      listOf(SelectOption("0", "Owner / Field Operator")) +
        snapshot.employees.map { SelectOption(it.id.toString(), "${it.name} (${it.department})") }
    val catOptions =
      listOf(
        SelectOption("Travel & Meals", "Travel & Meals"),
        SelectOption("Software & Cloud", "Software & Cloud"),
        SelectOption("Hardware & Supplies", "Hardware & Supplies"),
        SelectOption("Logistics & Fuel", "Logistics & Fuel"),
        SelectOption("Operations", "Operations"),
      )

    return A2uiEnvelope(
      surfaceId = "offline_expense",
      title = "Expense Claim Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("header_row", "form_card", "submit_btn"),
            ),
          "header_row" to
            A2uiComponent.Row(id = "header_row", children = listOf("badge_mode", "badge_receipt")),
          "badge_mode" to
            A2uiComponent.StatusBadge(
              id = "badge_mode",
              label = A2uiValue.Literal("HR & Expenses • Offline-Ready"),
              tone = A2uiTone.PRIMARY,
            ),
          "badge_receipt" to
            A2uiComponent.StatusBadge(
              id = "badge_receipt",
              label =
                A2uiValue.Literal(
                  if (receiptAttached) "Receipt Attached" else "Optional Receipt"
                ),
              tone = if (receiptAttached) A2uiTone.SUCCESS else A2uiTone.NEUTRAL,
            ),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "Submit Expense Claim",
              children = listOf("emp_select", "cat_select", "amt_field", "desc_field"),
            ),
          "emp_select" to
            A2uiComponent.Select(
              id = "emp_select",
              label = "Submitted By",
              path = "/expense/employeeId",
              options = empOptions,
            ),
          "cat_select" to
            A2uiComponent.Select(
              id = "cat_select",
              label = "Category",
              path = "/expense/category",
              options = catOptions,
            ),
          "amt_field" to
            A2uiComponent.NumberField(
              id = "amt_field",
              label = "Amount (USD)",
              path = "/expense/amount",
              prefix = "$",
            ),
          "desc_field" to
            A2uiComponent.TextField(
              id = "desc_field",
              label = "Description / Merchant",
              path = "/expense/description",
              placeholder = "e.g., Client lunch or hardware parts",
              multiline = true,
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Save Claim & Queue Sync",
              actionId = "SUBMIT_EXPENSE",
              style = A2uiButtonStyle.PRIMARY,
            ),
        ),
      dataModel =
        mapOf(
          "/expense/employeeId" to (snapshot.employees.firstOrNull()?.id?.toString() ?: "0"),
          "/expense/category" to prefillCategory,
          "/expense/amount" to prefillAmount,
          "/expense/description" to prefillDesc,
        ),
    )
  }

  fun buildInvoiceSurface(
    snapshot: ErpSnapshotForA2ui,
    prefillCustomerId: String? = null,
    prefillUnitPrice: String = "199.00",
    prefillDesc: String = "Edge IoT Deployment",
  ): A2uiEnvelope {
    val custOptions =
      if (snapshot.customers.isEmpty()) {
        listOf(SelectOption("", "No customers yet — create one first"))
      } else {
        snapshot.customers.map { SelectOption(it.id.toString(), "${it.name} (${it.stage})") }
      }
    val itemOptions =
      listOf(SelectOption("0", "Custom Service / Non-Inventory")) +
        snapshot.items.map {
          SelectOption(it.id.toString(), "${it.sku} • ${it.name} (${it.quantityOnHand} avail)")
        }

    return A2uiEnvelope(
      surfaceId = "offline_invoice",
      title = "Sales Invoice Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("info_text", "form_card", "submit_btn"),
            ),
          "info_text" to
            A2uiComponent.Text(
              id = "info_text",
              text =
                A2uiValue.Literal(
                  "Issuing an invoice posts Debit Accounts Receivable / Credit Sales Revenue and deducts linked inventory."
                ),
              variant = "caption",
            ),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "New Customer Invoice",
              children = listOf("cust_select", "item_select", "row_qty_price", "desc_field"),
            ),
          "cust_select" to
            A2uiComponent.Select(
              id = "cust_select",
              label = "Customer",
              path = "/invoice/customerId",
              options = custOptions,
            ),
          "item_select" to
            A2uiComponent.Select(
              id = "item_select",
              label = "Catalog Item (Optional)",
              path = "/invoice/itemId",
              options = itemOptions,
            ),
          "row_qty_price" to
            A2uiComponent.Row(id = "row_qty_price", children = listOf("qty_field", "price_field")),
          "qty_field" to
            A2uiComponent.NumberField(
              id = "qty_field",
              label = "Qty",
              path = "/invoice/quantity",
              integerOnly = true,
            ),
          "price_field" to
            A2uiComponent.NumberField(
              id = "price_field",
              label = "Unit Price",
              path = "/invoice/unitPrice",
              prefix = "$",
            ),
          "desc_field" to
            A2uiComponent.TextField(
              id = "desc_field",
              label = "Line Description",
              path = "/invoice/description",
              placeholder = "Goods or service description",
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Issue Invoice & Post Ledger",
              actionId = "CREATE_INVOICE",
            ),
        ),
      dataModel =
        mapOf(
          "/invoice/customerId" to (prefillCustomerId ?: snapshot.customers.firstOrNull()?.id?.toString() ?: ""),
          "/invoice/itemId" to (snapshot.items.firstOrNull()?.id?.toString() ?: "0"),
          "/invoice/quantity" to "1",
          "/invoice/unitPrice" to prefillUnitPrice,
          "/invoice/description" to prefillDesc,
        ),
    )
  }

  fun buildStockAdjustmentSurface(
    snapshot: ErpSnapshotForA2ui,
    prefillItemId: String? = null,
    prefillDelta: String = "10",
    prefillReason: String = "Warehouse restock",
  ): A2uiEnvelope {
    val itemOptions =
      if (snapshot.items.isEmpty()) {
        listOf(SelectOption("", "No SKUs yet — add an item first"))
      } else {
        snapshot.items.map {
          SelectOption(it.id.toString(), "${it.sku} • ${it.name} (${it.quantityOnHand} on hand)")
        }
      }

    return A2uiEnvelope(
      surfaceId = "offline_stock",
      title = "Stock Movement Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("form_card", "submit_btn"),
            ),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "Adjust Inventory Quantity",
              children = listOf("item_select", "delta_field", "reason_field"),
            ),
          "item_select" to
            A2uiComponent.Select(
              id = "item_select",
              label = "Inventory SKU",
              path = "/stock/itemId",
              options = itemOptions,
            ),
          "delta_field" to
            A2uiComponent.NumberField(
              id = "delta_field",
              label = "Quantity Delta (+ receive / - deduct)",
              path = "/stock/delta",
              integerOnly = true,
            ),
          "reason_field" to
            A2uiComponent.TextField(
              id = "reason_field",
              label = "Movement Reason",
              path = "/stock/reason",
              placeholder = "Count reconciliation, damaged, restock",
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Record Stock Movement",
              actionId = "ADJUST_STOCK",
            ),
        ),
      dataModel =
        mapOf(
          "/stock/itemId" to (prefillItemId ?: snapshot.items.firstOrNull()?.id?.toString() ?: ""),
          "/stock/delta" to prefillDelta,
          "/stock/reason" to prefillReason,
        ),
    )
  }

  fun buildPurchaseBillSurface(
    snapshot: ErpSnapshotForA2ui,
    prefillVendorId: String? = null,
    prefillAmount: String = "250.00",
    prefillDesc: String = "Supplier Inventory Order",
  ): A2uiEnvelope {
    val vendorOptions =
      if (snapshot.vendors.isEmpty()) {
        listOf(SelectOption("", "No vendors yet — create one first"))
      } else {
        snapshot.vendors.map { SelectOption(it.id.toString(), "${it.name} (${it.paymentTerms})") }
      }
    val itemOptions =
      listOf(SelectOption("0", "Operating Expense (No SKU)")) +
        snapshot.items.map { SelectOption(it.id.toString(), "${it.sku} • ${it.name}") }

    return A2uiEnvelope(
      surfaceId = "offline_bill",
      title = "Vendor Bill Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("form_card", "submit_btn"),
            ),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "Record Vendor Purchase Bill",
              children = listOf("vendor_select", "item_select", "row_qty_cost", "desc_field"),
            ),
          "vendor_select" to
            A2uiComponent.Select(
              id = "vendor_select",
              label = "Vendor",
              path = "/bill/vendorId",
              options = vendorOptions,
            ),
          "item_select" to
            A2uiComponent.Select(
              id = "item_select",
              label = "Receive Into Inventory SKU",
              path = "/bill/itemId",
              options = itemOptions,
            ),
          "row_qty_cost" to
            A2uiComponent.Row(id = "row_qty_cost", children = listOf("qty_field", "cost_field")),
          "qty_field" to
            A2uiComponent.NumberField(
              id = "qty_field",
              label = "Qty",
              path = "/bill/quantity",
              integerOnly = true,
            ),
          "cost_field" to
            A2uiComponent.NumberField(
              id = "cost_field",
              label = "Unit Cost",
              path = "/bill/unitCost",
              prefix = "$",
            ),
          "desc_field" to
            A2uiComponent.TextField(
              id = "desc_field",
              label = "Bill Memo",
              path = "/bill/description",
              placeholder = "PO / supplier invoice description",
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Post Purchase Bill",
              actionId = "CREATE_BILL",
            ),
        ),
      dataModel =
        mapOf(
          "/bill/vendorId" to (prefillVendorId ?: snapshot.vendors.firstOrNull()?.id?.toString() ?: ""),
          "/bill/itemId" to "0",
          "/bill/quantity" to "1",
          "/bill/unitCost" to prefillAmount,
          "/bill/description" to prefillDesc,
        ),
    )
  }

  fun buildJournalEntrySurface(
    snapshot: ErpSnapshotForA2ui,
    prefillMemo: String = "Manual Ledger Entry",
    prefillAmount: String = "100.00",
  ): A2uiEnvelope {
    val accountOptions =
      snapshot.accounts.map { SelectOption(it.id.toString(), "${it.code} • ${it.name}") }

    return A2uiEnvelope(
      surfaceId = "offline_journal",
      title = "Double-Entry Journal Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("guardrail_badge", "form_card", "submit_btn"),
            ),
          "guardrail_badge" to
            A2uiComponent.StatusBadge(
              id = "guardrail_badge",
              label = A2uiValue.Literal("Invariant: Total Debits = Total Credits"),
              tone = A2uiTone.SUCCESS,
            ),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "Balanced Journal Entry",
              children = listOf("debit_select", "credit_select", "amt_field", "memo_field"),
            ),
          "debit_select" to
            A2uiComponent.Select(
              id = "debit_select",
              label = "Debit Account",
              path = "/journal/debitAccountId",
              options = accountOptions,
            ),
          "credit_select" to
            A2uiComponent.Select(
              id = "credit_select",
              label = "Credit Account",
              path = "/journal/creditAccountId",
              options = accountOptions,
            ),
          "amt_field" to
            A2uiComponent.NumberField(
              id = "amt_field",
              label = "Amount (USD)",
              path = "/journal/amount",
              prefix = "$",
            ),
          "memo_field" to
            A2uiComponent.TextField(
              id = "memo_field",
              label = "Journal Memo",
              path = "/journal/memo",
              placeholder = "Reason for adjustment or transfer",
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Post Balanced Journal",
              actionId = "POST_JOURNAL",
            ),
        ),
      dataModel =
        mapOf(
          "/journal/debitAccountId" to (snapshot.accounts.firstOrNull()?.id?.toString() ?: ""),
          "/journal/creditAccountId" to
            (snapshot.accounts.getOrNull(5)?.id?.toString()
              ?: snapshot.accounts.lastOrNull()?.id?.toString()
              ?: ""),
          "/journal/amount" to prefillAmount,
          "/journal/memo" to prefillMemo,
        ),
    )
  }

  fun buildPayrollSurface(snapshot: ErpSnapshotForA2ui): A2uiEnvelope {
    val empOptions =
      if (snapshot.employees.isEmpty()) {
        listOf(SelectOption("", "No employees yet — onboard staff first"))
      } else {
        snapshot.employees.map {
          SelectOption(
            it.id.toString(),
            "${it.name} (${MoneyUtils.formatCents(it.monthlySalaryCents)}/mo)",
          )
        }
      }

    return A2uiEnvelope(
      surfaceId = "offline_payroll",
      title = "Payroll Run Surface",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to A2uiComponent.Column(id = "root", children = listOf("form_card", "submit_btn")),
          "form_card" to
            A2uiComponent.Card(
              id = "form_card",
              title = "Post Employee Payroll Run",
              children = listOf("emp_select", "period_field", "tax_field"),
            ),
          "emp_select" to
            A2uiComponent.Select(
              id = "emp_select",
              label = "Employee",
              path = "/payroll/employeeId",
              options = empOptions,
            ),
          "period_field" to
            A2uiComponent.TextField(
              id = "period_field",
              label = "Pay Period Label",
              path = "/payroll/period",
              placeholder = "e.g., Nov 2026",
            ),
          "tax_field" to
            A2uiComponent.NumberField(
              id = "tax_field",
              label = "Withholding Tax (%)",
              path = "/payroll/taxPercent",
              integerOnly = true,
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Run Payroll & Post Liabilities",
              actionId = "RUN_PAYROLL",
            ),
        ),
      dataModel =
        mapOf(
          "/payroll/employeeId" to (snapshot.employees.firstOrNull()?.id?.toString() ?: ""),
          "/payroll/period" to "Nov 2026",
          "/payroll/taxPercent" to "20",
        ),
    )
  }

  fun buildTrialBalanceSurface(snapshot: ErpSnapshotForA2ui): A2uiEnvelope {
    val assets =
      snapshot.accounts
        .filter { it.type == AccountType.ASSET.name }
        .sumOf { it.balanceCents }
    val liabilities =
      snapshot.accounts
        .filter { it.type == AccountType.LIABILITY.name }
        .sumOf { it.balanceCents }
    val revenue =
      snapshot.accounts
        .filter { it.type == AccountType.REVENUE.name }
        .sumOf { it.balanceCents }
    val expenses =
      snapshot.accounts
        .filter { it.type == AccountType.EXPENSE.name }
        .sumOf { it.balanceCents }
    val netIncome = revenue - expenses

    return A2uiEnvelope(
      surfaceId = "offline_trial_balance",
      title = "Financial Position Snapshot",
      sourceLabel = "Offline A2UI Live Ledger",
      components =
        linkedMapOf(
          "root" to
            A2uiComponent.Column(
              id = "root",
              children = listOf("row_1", "row_2", "net_card", "sample_btn"),
            ),
          "row_1" to A2uiComponent.Row(id = "row_1", children = listOf("tile_assets", "tile_liab")),
          "tile_assets" to
            A2uiComponent.MetricTile(
              id = "tile_assets",
              label = "Total Assets",
              value = A2uiValue.Literal(MoneyUtils.formatCents(assets)),
              tone = A2uiTone.PRIMARY,
            ),
          "tile_liab" to
            A2uiComponent.MetricTile(
              id = "tile_liab",
              label = "Liabilities",
              value = A2uiValue.Literal(MoneyUtils.formatCents(liabilities)),
              tone = A2uiTone.WARNING,
            ),
          "row_2" to A2uiComponent.Row(id = "row_2", children = listOf("tile_rev", "tile_exp")),
          "tile_rev" to
            A2uiComponent.MetricTile(
              id = "tile_rev",
              label = "Revenue",
              value = A2uiValue.Literal(MoneyUtils.formatCents(revenue)),
              tone = A2uiTone.SUCCESS,
            ),
          "tile_exp" to
            A2uiComponent.MetricTile(
              id = "tile_exp",
              label = "Expenses",
              value = A2uiValue.Literal(MoneyUtils.formatCents(expenses)),
              tone = A2uiTone.ERROR,
            ),
          "net_card" to
            A2uiComponent.Card(
              id = "net_card",
              title = "Operating Net Income",
              children = listOf("net_text", "inv_text"),
            ),
          "net_text" to
            A2uiComponent.Text(
              id = "net_text",
              text = A2uiValue.Literal(MoneyUtils.formatCents(netIncome)),
              variant = "h1",
            ),
          "inv_text" to
            A2uiComponent.Text(
              id = "inv_text",
              text =
                A2uiValue.Literal(
                  "${snapshot.items.size} SKUs tracked • ${snapshot.customers.size} CRM accounts • ${snapshot.vendors.size} Vendors"
                ),
              variant = "caption",
            ),
          "sample_btn" to
            A2uiComponent.Button(
              id = "sample_btn",
              label = "Open Double-Entry Journal Form",
              actionId = "OPEN_JOURNAL_SURFACE",
              style = A2uiButtonStyle.TONAL,
            ),
        ),
      dataModel = emptyMap(),
    )
  }

  fun buildNewCustomerSurface(prefillName: String = ""): A2uiEnvelope =
    A2uiEnvelope(
      surfaceId = "offline_new_customer",
      title = "CRM Customer Onboarding",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to A2uiComponent.Column(id = "root", children = listOf("card", "submit_btn")),
          "card" to
            A2uiComponent.Card(
              id = "card",
              title = "Add Customer / Lead",
              children = listOf("name_f", "email_f", "phone_f", "stage_s"),
            ),
          "name_f" to
            A2uiComponent.TextField(
              id = "name_f",
              label = "Company or Contact Name",
              path = "/customer/name",
              placeholder = "Acme Logistics",
            ),
          "email_f" to
            A2uiComponent.TextField(
              id = "email_f",
              label = "Email",
              path = "/customer/email",
              placeholder = "billing@company.com",
            ),
          "phone_f" to
            A2uiComponent.TextField(
              id = "phone_f",
              label = "Phone",
              path = "/customer/phone",
              placeholder = "555-0140",
            ),
          "stage_s" to
            A2uiComponent.Select(
              id = "stage_s",
              label = "CRM Pipeline Stage",
              path = "/customer/stage",
              options =
                listOf(
                  SelectOption("LEAD", "Lead"),
                  SelectOption("QUALIFIED", "Qualified"),
                  SelectOption("ACTIVE", "Active"),
                ),
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Save Customer",
              actionId = "CREATE_CUSTOMER",
            ),
        ),
      dataModel =
        mapOf(
          "/customer/name" to prefillName,
          "/customer/email" to "",
          "/customer/phone" to "",
          "/customer/stage" to "LEAD",
        ),
    )

  fun buildNewVendorSurface(prefillName: String = ""): A2uiEnvelope =
    A2uiEnvelope(
      surfaceId = "offline_new_vendor",
      title = "Vendor Onboarding",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to A2uiComponent.Column(id = "root", children = listOf("card", "submit_btn")),
          "card" to
            A2uiComponent.Card(
              id = "card",
              title = "Add Supplier / Vendor",
              children = listOf("name_f", "email_f", "terms_f"),
            ),
          "name_f" to
            A2uiComponent.TextField(
              id = "name_f",
              label = "Vendor Name",
              path = "/vendor/name",
              placeholder = "Apex Foundry",
            ),
          "email_f" to
            A2uiComponent.TextField(
              id = "email_f",
              label = "Billing Email",
              path = "/vendor/email",
              placeholder = "ap@vendor.com",
            ),
          "terms_f" to
            A2uiComponent.Select(
              id = "terms_f",
              label = "Payment Terms",
              path = "/vendor/terms",
              options =
                listOf(
                  SelectOption("Due on Receipt", "Due on Receipt"),
                  SelectOption("Net 15", "Net 15"),
                  SelectOption("Net 30", "Net 30"),
                  SelectOption("Net 60", "Net 60"),
                ),
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Save Vendor",
              actionId = "CREATE_VENDOR",
            ),
        ),
      dataModel =
        mapOf(
          "/vendor/name" to prefillName,
          "/vendor/email" to "",
          "/vendor/terms" to "Net 30",
        ),
    )

  fun buildNewInventoryItemSurface(): A2uiEnvelope =
    A2uiEnvelope(
      surfaceId = "offline_new_item",
      title = "New Inventory SKU",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to A2uiComponent.Column(id = "root", children = listOf("card", "submit_btn")),
          "card" to
            A2uiComponent.Card(
              id = "card",
              title = "Register Catalog SKU",
              children = listOf("sku_f", "name_f", "cat_f", "price_row", "qty_row"),
            ),
          "sku_f" to
            A2uiComponent.TextField(
              id = "sku_f",
              label = "SKU Code",
              path = "/item/sku",
              placeholder = "CLAW-MOD-01",
            ),
          "name_f" to
            A2uiComponent.TextField(
              id = "name_f",
              label = "Item Name",
              path = "/item/name",
              placeholder = "Industrial Telemetry Unit",
            ),
          "cat_f" to
            A2uiComponent.TextField(
              id = "cat_f",
              label = "Category",
              path = "/item/category",
              placeholder = "Hardware",
            ),
          "price_row" to A2uiComponent.Row(id = "price_row", children = listOf("cost_f", "sale_f")),
          "cost_f" to
            A2uiComponent.NumberField(
              id = "cost_f",
              label = "Unit Cost",
              path = "/item/unitCost",
              prefix = "$",
            ),
          "sale_f" to
            A2uiComponent.NumberField(
              id = "sale_f",
              label = "Sale Price",
              path = "/item/salePrice",
              prefix = "$",
            ),
          "qty_row" to A2uiComponent.Row(id = "qty_row", children = listOf("qty_f", "reorder_f")),
          "qty_f" to
            A2uiComponent.NumberField(
              id = "qty_f",
              label = "Initial Qty",
              path = "/item/qty",
              integerOnly = true,
            ),
          "reorder_f" to
            A2uiComponent.NumberField(
              id = "reorder_f",
              label = "Reorder Point",
              path = "/item/reorder",
              integerOnly = true,
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Create SKU",
              actionId = "CREATE_ITEM",
            ),
        ),
      dataModel =
        mapOf(
          "/item/sku" to "",
          "/item/name" to "",
          "/item/category" to "Hardware",
          "/item/unitCost" to "50.00",
          "/item/salePrice" to "120.00",
          "/item/qty" to "10",
          "/item/reorder" to "5",
        ),
    )

  fun buildNewEmployeeSurface(prefillName: String = ""): A2uiEnvelope =
    A2uiEnvelope(
      surfaceId = "offline_new_employee",
      title = "HR Employee Onboarding",
      sourceLabel = "Offline A2UI Template",
      components =
        linkedMapOf(
          "root" to A2uiComponent.Column(id = "root", children = listOf("card", "submit_btn")),
          "card" to
            A2uiComponent.Card(
              id = "card",
              title = "Add Team Member",
              children = listOf("name_f", "role_f", "dept_f", "sal_f"),
            ),
          "name_f" to
            A2uiComponent.TextField(
              id = "name_f",
              label = "Full Name",
              path = "/employee/name",
              placeholder = "Jordan Rivera",
            ),
          "role_f" to
            A2uiComponent.TextField(
              id = "role_f",
              label = "Role / Title",
              path = "/employee/role",
              placeholder = "Operations Engineer",
            ),
          "dept_f" to
            A2uiComponent.TextField(
              id = "dept_f",
              label = "Department",
              path = "/employee/department",
              placeholder = "Operations",
            ),
          "sal_f" to
            A2uiComponent.NumberField(
              id = "sal_f",
              label = "Monthly Salary (USD)",
              path = "/employee/salary",
              prefix = "$",
            ),
          "submit_btn" to
            A2uiComponent.Button(
              id = "submit_btn",
              label = "Save Employee",
              actionId = "CREATE_EMPLOYEE",
            ),
        ),
      dataModel =
        mapOf(
          "/employee/name" to prefillName,
          "/employee/role" to "Operations Specialist",
          "/employee/department" to "Operations",
          "/employee/salary" to "5500.00",
        ),
    )

  private fun extractNameHint(command: String): String {
    val cleaned =
      command
        .replace(Regex("(?i)(new|add|create|customer|vendor|supplier|employee|staff|hire|lead)"), "")
        .trim()
    return if (cleaned.length in 2..40) cleaned else ""
  }
}
