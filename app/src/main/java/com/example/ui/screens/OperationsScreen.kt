package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MoneyUtils
import com.example.data.local.AccountEntity
import com.example.data.local.BillStatus
import com.example.data.local.CustomerEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.ExpenseClaimEntity
import com.example.data.local.ExpenseStatus
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PayrollRunEntity
import com.example.data.local.PurchaseBillEntity
import com.example.data.local.VendorEntity
import com.example.ui.ErpModuleTab
import com.example.ui.theme.LocalErpThemeConfig
import com.example.ui.theme.TabularNumberStyle

@Composable
fun OperationsScreen(
  accounts: List<AccountEntity>,
  inventoryItems: List<InventoryItemEntity>,
  customers: List<CustomerEntity>,
  invoices: List<InvoiceEntity>,
  vendors: List<VendorEntity>,
  purchaseBills: List<PurchaseBillEntity>,
  employees: List<EmployeeEntity>,
  expenseClaims: List<ExpenseClaimEntity>,
  payrollRuns: List<PayrollRunEntity>,
  onOpenSkillInCommandHub: (String) -> Unit,
  onAdjustStock: (Long, Int, String) -> Unit,
  onMarkInvoicePaid: (Long) -> Unit,
  onAdvanceCustomerStage: (Long, String) -> Unit,
  onPayBill: (Long) -> Unit,
  onApproveExpense: (Long) -> Unit,
  onRunPayroll: (Long) -> Unit,
  onLoadSampleWorkspace: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable { mutableStateOf(ErpModuleTab.ACCOUNTING) }
  var searchQuery by rememberSaveable { mutableStateOf("") }

  val fabSkillKey =
    when (selectedTab) {
      ErpModuleTab.ACCOUNTING -> "journal"
      ErpModuleTab.INVENTORY -> "new_item"
      ErpModuleTab.SALES -> "invoice"
      ErpModuleTab.PURCHASING -> "bill"
      ErpModuleTab.HR -> "expense"
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { onOpenSkillInCommandHub(fabSkillKey) },
        modifier = Modifier.testTag("operations_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Create in A2UI")
      }
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      val isWide = maxWidth >= 840.dp

      Column(modifier = Modifier.fillMaxSize()) {
        // Module Selector & Search
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ErpModuleTab.entries.forEach { tab ->
              FilterChip(
                selected = selectedTab == tab,
                onClick = { selectedTab = tab },
                label = { Text(tab.label) },
                leadingIcon = {
                  Icon(
                    imageVector =
                      when (tab) {
                        ErpModuleTab.ACCOUNTING -> Icons.Rounded.AccountBalance
                        ErpModuleTab.INVENTORY -> Icons.Rounded.Inventory2
                        ErpModuleTab.SALES -> Icons.Rounded.PointOfSale
                        ErpModuleTab.PURCHASING -> Icons.Rounded.ShoppingBag
                        ErpModuleTab.HR -> Icons.Rounded.Badge
                      },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                modifier = Modifier.testTag("module_tab_${tab.name}"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Filter ${selectedTab.label}…") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("operations_search_input"),
            )
            if (inventoryItems.isEmpty() && customers.isEmpty() && vendors.isEmpty()) {
              FilledTonalButton(
                onClick = onLoadSampleWorkspace,
                modifier = Modifier.testTag("load_sample_workspace_button"),
              ) {
                Text(stringResource(R.string.load_sample_workspace))
              }
            }
          }
        }

        // Content List / Adaptive Two-Pane
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            ModuleSummaryRail(
              selectedTab = selectedTab,
              accounts = accounts,
              inventoryItems = inventoryItems,
              invoices = invoices,
              purchaseBills = purchaseBills,
              expenseClaims = expenseClaims,
              onOpenSkillInCommandHub = onOpenSkillInCommandHub,
              modifier = Modifier.width(300.dp),
            )
            ModuleListPane(
              selectedTab = selectedTab,
              query = searchQuery.trim().lowercase(),
              accounts = accounts,
              inventoryItems = inventoryItems,
              customers = customers,
              invoices = invoices,
              vendors = vendors,
              purchaseBills = purchaseBills,
              employees = employees,
              expenseClaims = expenseClaims,
              payrollRuns = payrollRuns,
              onOpenSkillInCommandHub = onOpenSkillInCommandHub,
              onAdjustStock = onAdjustStock,
              onMarkInvoicePaid = onMarkInvoicePaid,
              onAdvanceCustomerStage = onAdvanceCustomerStage,
              onPayBill = onPayBill,
              onApproveExpense = onApproveExpense,
              onRunPayroll = onRunPayroll,
              onLoadSampleWorkspace = onLoadSampleWorkspace,
              modifier = Modifier.weight(1f),
            )
          }
        } else {
          ModuleListPane(
            selectedTab = selectedTab,
            query = searchQuery.trim().lowercase(),
            accounts = accounts,
            inventoryItems = inventoryItems,
            customers = customers,
            invoices = invoices,
            vendors = vendors,
            purchaseBills = purchaseBills,
            employees = employees,
            expenseClaims = expenseClaims,
            payrollRuns = payrollRuns,
            onOpenSkillInCommandHub = onOpenSkillInCommandHub,
            onAdjustStock = onAdjustStock,
            onMarkInvoicePaid = onMarkInvoicePaid,
            onAdvanceCustomerStage = onAdvanceCustomerStage,
            onPayBill = onPayBill,
            onApproveExpense = onApproveExpense,
            onRunPayroll = onRunPayroll,
            onLoadSampleWorkspace = onLoadSampleWorkspace,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun ModuleSummaryRail(
  selectedTab: ErpModuleTab,
  accounts: List<AccountEntity>,
  inventoryItems: List<InventoryItemEntity>,
  invoices: List<InvoiceEntity>,
  purchaseBills: List<PurchaseBillEntity>,
  expenseClaims: List<ExpenseClaimEntity>,
  onOpenSkillInCommandHub: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  ErpCard(modifier = modifier.padding(top = 8.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(text = "${selectedTab.label} Snapshot", style = MaterialTheme.typography.titleMedium)
      when (selectedTab) {
        ErpModuleTab.ACCOUNTING -> {
          val cash = accounts.firstOrNull { it.code == "1000" }?.balanceCents ?: 0L
          Text("Cash & Bank: ${MoneyUtils.formatCents(cash)}", style = TabularNumberStyle)
          FilledTonalButton(
            onClick = { onOpenSkillInCommandHub("journal") },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Post Balanced Journal")
          }
        }
        ErpModuleTab.INVENTORY -> {
          val lowStock = inventoryItems.count { it.quantityOnHand <= it.reorderPoint }
          Text("${inventoryItems.size} SKUs • $lowStock Low Stock", style = TabularNumberStyle)
          FilledTonalButton(
            onClick = { onOpenSkillInCommandHub("stock") },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Adjust Stock")
          }
        }
        ErpModuleTab.SALES -> {
          val openAr =
            invoices.filter { it.status != InvoiceStatus.PAID.name }.sumOf { it.totalCents }
          Text("Open Receivables: ${MoneyUtils.formatCents(openAr)}", style = TabularNumberStyle)
          FilledTonalButton(
            onClick = { onOpenSkillInCommandHub("new_customer") },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("+ Add Customer")
          }
        }
        ErpModuleTab.PURCHASING -> {
          val openAp =
            purchaseBills.filter { it.status != BillStatus.PAID.name }.sumOf { it.totalCents }
          Text("Open Payables: ${MoneyUtils.formatCents(openAp)}", style = TabularNumberStyle)
          FilledTonalButton(
            onClick = { onOpenSkillInCommandHub("new_vendor") },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("+ Add Vendor")
          }
        }
        ErpModuleTab.HR -> {
          val pendingExp =
            expenseClaims.filter { it.status != ExpenseStatus.PAID.name }.sumOf { it.amountCents }
          Text(
            "Pending Claims: ${MoneyUtils.formatCents(pendingExp)}",
            style = TabularNumberStyle,
          )
          FilledTonalButton(
            onClick = { onOpenSkillInCommandHub("payroll") },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Run Payroll")
          }
        }
      }
    }
  }
}

@Composable
private fun ModuleListPane(
  selectedTab: ErpModuleTab,
  query: String,
  accounts: List<AccountEntity>,
  inventoryItems: List<InventoryItemEntity>,
  customers: List<CustomerEntity>,
  invoices: List<InvoiceEntity>,
  vendors: List<VendorEntity>,
  purchaseBills: List<PurchaseBillEntity>,
  employees: List<EmployeeEntity>,
  expenseClaims: List<ExpenseClaimEntity>,
  payrollRuns: List<PayrollRunEntity>,
  onOpenSkillInCommandHub: (String) -> Unit,
  onAdjustStock: (Long, Int, String) -> Unit,
  onMarkInvoicePaid: (Long) -> Unit,
  onAdvanceCustomerStage: (Long, String) -> Unit,
  onPayBill: (Long) -> Unit,
  onApproveExpense: (Long) -> Unit,
  onRunPayroll: (Long) -> Unit,
  onLoadSampleWorkspace: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    when (selectedTab) {
      ErpModuleTab.ACCOUNTING -> {
        val filtered =
          accounts.filter {
            query.isEmpty() ||
              it.name.lowercase().contains(query) ||
              it.code.lowercase().contains(query)
          }
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Chart of Accounts (${filtered.size})", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onOpenSkillInCommandHub("trial_balance") }) {
              Text("A2UI Trial Balance")
            }
          }
        }
        items(filtered, key = { it.id }) { acct ->
          ErpCard {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${acct.code} • ${acct.name}", style = MaterialTheme.typography.titleSmall)
                Text(
                  acct.type,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = MoneyUtils.formatCents(acct.balanceCents),
                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      ErpModuleTab.INVENTORY -> {
        val filtered =
          inventoryItems.filter {
            query.isEmpty() ||
              it.name.lowercase().contains(query) ||
              it.sku.lowercase().contains(query)
          }
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Catalog & Stock (${filtered.size})", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onOpenSkillInCommandHub("new_item") }) { Text("+ New SKU") }
          }
        }
        if (filtered.isEmpty()) {
          item {
            EmptyErpState(
              title = "No inventory SKUs yet",
              subtitle = "Add your first SKU via A2UI or load the sample workspace.",
              primaryActionLabel = "Add SKU in A2UI",
              onPrimaryAction = { onOpenSkillInCommandHub("new_item") },
              onLoadSample = onLoadSampleWorkspace,
            )
          }
        } else {
          items(filtered, key = { it.id }) { item ->
            val lowStock = item.quantityOnHand <= item.reorderPoint
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(item.sku, style = MaterialTheme.typography.labelMedium)
                    if (lowStock) {
                      StatusPill("LOW STOCK", isError = true)
                    }
                  }
                  Text(item.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "Cost ${MoneyUtils.formatCents(item.unitCostCents)} • Sale ${MoneyUtils.formatCents(item.salePriceCents)}",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { onAdjustStock(item.id, -1, "Quick issue") },
                    enabled = item.quantityOnHand > 0,
                  ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                  }
                  Text(
                    text = item.quantityOnHand.toString(),
                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                    modifier = Modifier.padding(horizontal = 4.dp),
                  )
                  IconButton(onClick = { onAdjustStock(item.id, 1, "Quick restock") }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                  }
                }
              }
            }
          }
        }
      }

      ErpModuleTab.SALES -> {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("CRM Pipeline (${customers.size})", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onOpenSkillInCommandHub("new_customer") }) {
              Text("+ Customer")
            }
          }
        }
        val filteredCustomers =
          customers.filter { query.isEmpty() || it.name.lowercase().contains(query) }
        if (filteredCustomers.isEmpty() && invoices.isEmpty()) {
          item {
            EmptyErpState(
              title = "No CRM accounts or invoices",
              subtitle = "Add a customer or issue an invoice offline.",
              primaryActionLabel = "New Customer",
              onPrimaryAction = { onOpenSkillInCommandHub("new_customer") },
              onLoadSample = onLoadSampleWorkspace,
            )
          }
        } else {
          items(filteredCustomers, key = { "c_${it.id}" }) { cust ->
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(cust.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    cust.email.ifBlank { cust.phone.ifBlank { "No contact details" } },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (cust.balanceDueCents > 0) {
                    Text(
                      "Balance due: ${MoneyUtils.formatCents(cust.balanceDueCents)}",
                      style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                      color = MaterialTheme.colorScheme.tertiary,
                    )
                  }
                }
                FilledTonalButton(
                  onClick = { onAdvanceCustomerStage(cust.id, cust.stage) }
                ) {
                  Text(cust.stage)
                }
              }
            }
          }

          item {
            Spacer(Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Invoices (${invoices.size})", style = MaterialTheme.typography.titleMedium)
              OutlinedButton(onClick = { onOpenSkillInCommandHub("invoice") }) {
                Text("+ Invoice")
              }
            }
          }
          val filteredInvoices =
            invoices.filter {
              query.isEmpty() ||
                it.invoiceNumber.lowercase().contains(query) ||
                it.customerName.lowercase().contains(query)
            }
          items(filteredInvoices, key = { "i_${it.id}" }) { inv ->
            val isPaid = inv.status == InvoiceStatus.PAID.name
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "${inv.invoiceNumber} • ${inv.customerName}",
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    MoneyUtils.formatCents(inv.totalCents),
                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                  )
                }
                if (isPaid) {
                  StatusPill("PAID", isSuccess = true)
                } else {
                  Button(onClick = { onMarkInvoicePaid(inv.id) }) {
                    Icon(
                      Icons.Rounded.Payments,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Paid")
                  }
                }
              }
            }
          }
        }
      }

      ErpModuleTab.PURCHASING -> {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Vendors (${vendors.size})", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onOpenSkillInCommandHub("new_vendor") }) {
              Text("+ Vendor")
            }
          }
        }
        val filteredVendors =
          vendors.filter { query.isEmpty() || it.name.lowercase().contains(query) }
        if (filteredVendors.isEmpty() && purchaseBills.isEmpty()) {
          item {
            EmptyErpState(
              title = "No vendors or purchase bills",
              subtitle = "Register a supplier to track Accounts Payable and stock receipts.",
              primaryActionLabel = "Add Vendor",
              onPrimaryAction = { onOpenSkillInCommandHub("new_vendor") },
              onLoadSample = onLoadSampleWorkspace,
            )
          }
        } else {
          items(filteredVendors, key = { "v_${it.id}" }) { vendor ->
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(vendor.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${vendor.paymentTerms} • ${vendor.email.ifBlank { "No email" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  MoneyUtils.formatCents(vendor.payableCents),
                  style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                )
              }
            }
          }

          item {
            Spacer(Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Vendor Bills (${purchaseBills.size})",
                style = MaterialTheme.typography.titleMedium,
              )
              OutlinedButton(onClick = { onOpenSkillInCommandHub("bill") }) { Text("+ Bill") }
            }
          }
          val filteredBills =
            purchaseBills.filter {
              query.isEmpty() ||
                it.billNumber.lowercase().contains(query) ||
                it.vendorName.lowercase().contains(query)
            }
          items(filteredBills, key = { "b_${it.id}" }) { bill ->
            val isPaid = bill.status == BillStatus.PAID.name
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "${bill.billNumber} • ${bill.vendorName}",
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    MoneyUtils.formatCents(bill.totalCents),
                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                  )
                }
                if (isPaid) {
                  StatusPill("PAID", isSuccess = true)
                } else {
                  Button(onClick = { onPayBill(bill.id) }) { Text("Pay Bill") }
                }
              }
            }
          }
        }
      }

      ErpModuleTab.HR -> {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Team Roster (${employees.size})", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onOpenSkillInCommandHub("new_employee") }) {
              Text("+ Employee")
            }
          }
        }
        val filteredEmps =
          employees.filter { query.isEmpty() || it.name.lowercase().contains(query) }
        if (filteredEmps.isEmpty() && expenseClaims.isEmpty()) {
          item {
            EmptyErpState(
              title = "No HR or expense records",
              subtitle = "Onboard team members or submit an expense claim.",
              primaryActionLabel = "Submit Expense",
              onPrimaryAction = { onOpenSkillInCommandHub("expense") },
              onLoadSample = onLoadSampleWorkspace,
            )
          }
        } else {
          items(filteredEmps, key = { "e_${it.id}" }) { emp ->
            ErpCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(emp.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${emp.role} • ${emp.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    "${MoneyUtils.formatCents(emp.monthlySalaryCents)}/mo",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                  )
                }
                FilledTonalButton(onClick = { onRunPayroll(emp.id) }) { Text("Run Payroll") }
              }
            }
          }

          item {
            Spacer(Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Expense Claims (${expenseClaims.size})",
                style = MaterialTheme.typography.titleMedium,
              )
              OutlinedButton(onClick = { onOpenSkillInCommandHub("expense") }) {
                Text("+ Expense")
              }
            }
          }
          val filteredExp =
            expenseClaims.filter {
              query.isEmpty() ||
                it.description.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }
          items(filteredExp, key = { "x_${it.id}" }) { claim ->
            val isPaid = claim.status == ExpenseStatus.PAID.name
            ErpCard {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      "${claim.category} • ${claim.employeeName}",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      claim.description,
                      style = MaterialTheme.typography.bodySmall,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Text(
                    MoneyUtils.formatCents(claim.amountCents),
                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                  )
                }
                if (claim.aiCategoryNote.isNotBlank()) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      Icons.Rounded.AutoAwesome,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      claim.aiCategoryNote,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                ) {
                  if (isPaid) {
                    StatusPill("REIMBURSED", isSuccess = true)
                  } else {
                    Button(onClick = { onApproveExpense(claim.id) }) { Text("Reimburse") }
                  }
                }
              }
            }
          }

          if (payrollRuns.isNotEmpty()) {
            item {
              Spacer(Modifier.height(6.dp))
              Text(
                "Posted Payroll Runs (${payrollRuns.size})",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            items(payrollRuns, key = { "p_${it.id}" }) { run ->
              ErpCard {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("${run.employeeName} • ${run.periodLabel}", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "Gross ${MoneyUtils.formatCents(run.grossCents)} • Tax ${MoneyUtils.formatCents(run.taxCents)}",
                      style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    "Net ${MoneyUtils.formatCents(run.netCents)}",
                    style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ErpCard(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val isAmoled = LocalErpThemeConfig.current.isAmoledActive
  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
    border =
      BorderStroke(
        1.dp,
        if (isAmoled) MaterialTheme.colorScheme.outlineVariant
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
      ),
    content = { content() },
  )
}

@Composable
private fun StatusPill(
  text: String,
  isSuccess: Boolean = false,
  isError: Boolean = false,
) {
  val bg =
    when {
      isError -> MaterialTheme.colorScheme.errorContainer
      isSuccess -> MaterialTheme.colorScheme.primaryContainer
      else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
  val fg =
    when {
      isError -> MaterialTheme.colorScheme.onErrorContainer
      isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
      else -> MaterialTheme.colorScheme.onSurface
    }
  Surface(shape = MaterialTheme.shapes.small, color = bg) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = fg,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun EmptyErpState(
  title: String,
  subtitle: String,
  primaryActionLabel: String,
  onPrimaryAction: () -> Unit,
  onLoadSample: () -> Unit,
) {
  ErpCard {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        Icons.Rounded.ReceiptLong,
        contentDescription = null,
        modifier = Modifier.size(36.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPrimaryAction) { Text(primaryActionLabel) }
        OutlinedButton(onClick = onLoadSample) { Text("Load Sample") }
      }
    }
  }
}
