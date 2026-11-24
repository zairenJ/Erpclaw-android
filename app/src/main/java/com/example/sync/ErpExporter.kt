package com.example.sync

import android.content.Context
import android.net.Uri
import com.example.data.ErpRepository
import com.example.data.MoneyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ErpExporter {

  private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

  suspend fun buildFullJsonSnapshot(repository: ErpRepository): String =
    withContext(Dispatchers.IO) {
      val root = JSONObject()
      root.put("exportedAt", isoFormat.format(Date()))
      root.put("format", "erpclaw-mobile-v1")

      val accountsArr = JSONArray()
      repository.getAccountsOnce().forEach { a ->
        accountsArr.put(
          JSONObject()
            .put("code", a.code)
            .put("name", a.name)
            .put("type", a.type)
            .put("balanceCents", a.balanceCents)
        )
      }
      root.put("accounts", accountsArr)

      val journalsArr = JSONArray()
      repository.getJournalEntriesOnce().forEach { j ->
        val linesArr = JSONArray()
        j.lines.forEach { l ->
          linesArr.put(
            JSONObject()
              .put("accountCode", l.accountCode)
              .put("accountName", l.accountName)
              .put("debitCents", l.debitCents)
              .put("creditCents", l.creditCents)
          )
        }
        journalsArr.put(
          JSONObject()
            .put("entryNumber", j.entry.entryNumber)
            .put("dateMillis", j.entry.dateMillis)
            .put("memo", j.entry.memo)
            .put("moduleSource", j.entry.moduleSource)
            .put("lines", linesArr)
        )
      }
      root.put("journalEntries", journalsArr)

      val invArr = JSONArray()
      repository.getInventoryItemsOnce().forEach { i ->
        invArr.put(
          JSONObject()
            .put("sku", i.sku)
            .put("name", i.name)
            .put("category", i.category)
            .put("unitCostCents", i.unitCostCents)
            .put("salePriceCents", i.salePriceCents)
            .put("quantityOnHand", i.quantityOnHand)
            .put("reorderPoint", i.reorderPoint)
        )
      }
      root.put("inventoryItems", invArr)

      val salesArr = JSONArray()
      repository.getInvoicesOnce().forEach { inv ->
        salesArr.put(
          JSONObject()
            .put("invoiceNumber", inv.invoiceNumber)
            .put("customerName", inv.customerName)
            .put("status", inv.status)
            .put("totalCents", inv.totalCents)
        )
      }
      root.put("invoices", salesArr)

      val billsArr = JSONArray()
      repository.getPurchaseBillsOnce().forEach { bill ->
        billsArr.put(
          JSONObject()
            .put("billNumber", bill.billNumber)
            .put("vendorName", bill.vendorName)
            .put("status", bill.status)
            .put("totalCents", bill.totalCents)
        )
      }
      root.put("purchaseBills", billsArr)

      val expArr = JSONArray()
      repository.getExpenseClaimsOnce().forEach { exp ->
        expArr.put(
          JSONObject()
            .put("employeeName", exp.employeeName)
            .put("category", exp.category)
            .put("amountCents", exp.amountCents)
            .put("description", exp.description)
            .put("status", exp.status)
            .put("aiCategoryNote", exp.aiCategoryNote)
        )
      }
      root.put("expenseClaims", expArr)

      val payArr = JSONArray()
      repository.getPayrollRunsOnce().forEach { p ->
        payArr.put(
          JSONObject()
            .put("periodLabel", p.periodLabel)
            .put("employeeName", p.employeeName)
            .put("grossCents", p.grossCents)
            .put("taxCents", p.taxCents)
            .put("netCents", p.netCents)
        )
      }
      root.put("payrollRuns", payArr)

      root.toString(2)
    }

  suspend fun buildLedgerCsv(repository: ErpRepository): String =
    withContext(Dispatchers.IO) {
      val sb = StringBuilder()
      sb.appendLine("EntryNumber,Date,Module,Memo,AccountCode,AccountName,Debit,Credit")
      repository.getJournalEntriesOnce().forEach { j ->
        val dateStr = isoFormat.format(Date(j.entry.dateMillis))
        j.lines.forEach { l ->
          sb.appendLine(
            listOf(
              csvEscape(j.entry.entryNumber),
              csvEscape(dateStr),
              csvEscape(j.entry.moduleSource),
              csvEscape(j.entry.memo),
              csvEscape(l.accountCode),
              csvEscape(l.accountName),
              csvEscape(MoneyUtils.formatCents(l.debitCents)),
              csvEscape(MoneyUtils.formatCents(l.creditCents)),
            )
              .joinToString(",")
          )
        }
      }
      sb.toString()
    }

  suspend fun buildInventoryCsv(repository: ErpRepository): String =
    withContext(Dispatchers.IO) {
      val sb = StringBuilder()
      sb.appendLine("SKU,Name,Category,QuantityOnHand,ReorderPoint,UnitCost,SalePrice")
      repository.getInventoryItemsOnce().forEach { item ->
        sb.appendLine(
          listOf(
            csvEscape(item.sku),
            csvEscape(item.name),
            csvEscape(item.category),
            item.quantityOnHand.toString(),
            item.reorderPoint.toString(),
            csvEscape(MoneyUtils.formatCents(item.unitCostCents)),
            csvEscape(MoneyUtils.formatCents(item.salePriceCents)),
          )
            .joinToString(",")
        )
      }
      sb.toString()
    }

  suspend fun writeTextToUri(context: Context, uri: Uri, content: String) =
    withContext(Dispatchers.IO) {
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(content.toByteArray(Charsets.UTF_8))
      } ?: throw IllegalStateException("Unable to open destination file.")
    }

  private fun csvEscape(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return "\"$clean\""
  }
}
