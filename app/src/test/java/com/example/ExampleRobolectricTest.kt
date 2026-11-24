package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ErpRepository
import com.example.data.JournalPostingLine
import com.example.data.local.ErpDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: ErpDatabase
  private lateinit var repo: ErpRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, ErpDatabase::class.java).allowMainThreadQueries().build()
    repo = ErpRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun appName_matchesErpClaw() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("ERPClaw", context.getString(R.string.app_name))
  }

  @Test
  fun doubleEntryJournal_rejectsImbalancedAndPostsBalancedWithOutbox() = runBlocking {
    repo.ensureChartOfAccountsInitialized()
    val accounts = repo.getAccountsOnce()
    val cash = accounts.first { it.code == "1000" }
    val equity = accounts.first { it.code == "3000" }

    var threw = false
    try {
      repo.postBalancedJournal(
        memo = "Imbalanced test",
        moduleSource = "TEST",
        lines =
          listOf(
            JournalPostingLine(cash.id, debitCents = 1000L, creditCents = 0L),
            JournalPostingLine(equity.id, debitCents = 0L, creditCents = 900L),
          ),
      )
    } catch (_: IllegalArgumentException) {
      threw = true
    }
    assertTrue("Imbalanced journal entry must throw", threw)

    repo.postBalancedJournal(
      memo = "Owner investment",
      moduleSource = "TEST",
      lines =
        listOf(
          JournalPostingLine(cash.id, debitCents = 50000L, creditCents = 0L),
          JournalPostingLine(equity.id, debitCents = 0L, creditCents = 50000L),
        ),
    )

    val updatedAccounts = repo.getAccountsOnce()
    assertEquals(50000L, updatedAccounts.first { it.code == "1000" }.balanceCents)
    assertEquals(50000L, updatedAccounts.first { it.code == "3000" }.balanceCents)
    assertTrue(repo.getPendingOutboxItems().isNotEmpty())
  }

  @Test
  fun loadSampleWorkspace_populatesAllFiveModulesAndBalancesTrialBalance() = runBlocking {
    repo.loadSampleWorkspace()
    assertTrue(repo.getInventoryItemsOnce().isNotEmpty())
    assertTrue(repo.getCustomersOnce().isNotEmpty())
    assertTrue(repo.getInvoicesOnce().isNotEmpty())
    assertTrue(repo.getVendorsOnce().isNotEmpty())
    assertTrue(repo.getPurchaseBillsOnce().isNotEmpty())
    assertTrue(repo.getEmployeesOnce().isNotEmpty())
    assertTrue(repo.getExpenseClaimsOnce().isNotEmpty())

    val journals = repo.getJournalEntriesOnce()
    val totalDebits = journals.sumOf { entry -> entry.lines.sumOf { it.debitCents } }
    val totalCredits = journals.sumOf { entry -> entry.lines.sumOf { it.creditCents } }
    assertTrue(totalDebits > 0L)
    assertEquals(totalDebits, totalCredits)
  }

  @Test
  fun a2uiJsonParser_enforcesTrustedCatalogGuardrail() {
    val payload =
      """
      {
        "surfaceTitle": "AI Expense",
        "dataFields": [{"path": "/expense/amount", "value": "99.25"}],
        "components": [
          {"id": "root", "component": "Column", "children": ["amt", "unsafe_script", "go"]},
          {"id": "amt", "component": "NumberField", "label": "Amount", "path": "/expense/amount"},
          {"id": "unsafe_script", "component": "WebViewScript", "text": "alert(1)"},
          {"id": "go", "component": "Button", "label": "Save", "actionId": "SUBMIT_EXPENSE"}
        ]
      }
      """.trimIndent()

    val envelope = com.example.a2ui.A2uiJsonParser.parseStructuredGeminiPayload(payload)
    assertEquals("AI Expense", envelope.title)
    assertEquals("99.25", envelope.dataModel["/expense/amount"])
    assertEquals(1, envelope.droppedUnsupportedCount)
    org.junit.Assert.assertNotNull(envelope.components["amt"])
    org.junit.Assert.assertNull(envelope.components["unsafe_script"])
  }
}
