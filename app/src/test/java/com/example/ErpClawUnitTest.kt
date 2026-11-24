package com.example

import com.example.a2ui.ErpSnapshotForA2ui
import com.example.a2ui.OfflineA2uiEngine
import com.example.data.MoneyUtils
import com.example.data.local.InventoryItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErpClawUnitTest {

  @Test
  fun moneyUtils_parsesFixedPointCentsWithoutRoundingDrift() {
    assertEquals(4850L, MoneyUtils.parseDollarsToCents("48.50"))
    assertEquals(123456L, MoneyUtils.parseDollarsToCents("$1,234.56"))
    assertEquals(10000L, MoneyUtils.parseDollarsToCents("100"))
    assertNull(MoneyUtils.parseDollarsToCents("abc"))
    assertTrue(MoneyUtils.formatCents(123456L).contains("1,234.56"))
  }

  @Test
  fun offlineA2uiEngine_interpretsExpenseAndStockCommands() {
    val snap =
      ErpSnapshotForA2ui(
        items =
          listOf(
            InventoryItemEntity(
              id = 7L,
              sku = "CLAW-SENS-01",
              name = "Edge IoT Gateway",
              category = "Hardware",
              unitCostCents = 8500L,
              salePriceCents = 19900L,
              quantityOnHand = 24,
            )
          )
      )

    val expenseSurface = OfflineA2uiEngine.interpretCommand("expense $48.50 client lunch", snap)
    assertEquals("offline_expense", expenseSurface.surfaceId)
    assertEquals("48.50", expenseSurface.dataModel["/expense/amount"])
    assertEquals("expense $48.50 client lunch", expenseSurface.dataModel["/expense/description"])

    val stockSurface = OfflineA2uiEngine.interpretCommand("stock +15 CLAW-SENS-01", snap)
    assertEquals("offline_stock", stockSurface.surfaceId)
    assertEquals("7", stockSurface.dataModel["/stock/itemId"])
    assertEquals("+15", stockSurface.dataModel["/stock/delta"])
  }
}
