package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.data.local.SyncStatus
import com.example.ui.ErpViewModel
import com.example.ui.screens.CommandHubScreen
import com.example.ui.screens.LedgerAndSyncScreen
import com.example.ui.screens.OperationsScreen
import com.example.ui.screens.ThemeAndExportScreen
import com.example.ui.theme.ErpClawTheme

object ErpRoutes {
  const val COMMAND = "command"
  const val OPERATIONS = "operations"
  const val LEDGER_SYNC = "ledger_sync"
  const val STUDIO = "studio"
}

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val topDestinations =
  listOf(
    NavDestinationItem(
      route = ErpRoutes.COMMAND,
      labelRes = R.string.nav_command,
      selectedIcon = Icons.Filled.AutoAwesome,
      unselectedIcon = Icons.Outlined.AutoAwesome,
    ),
    NavDestinationItem(
      route = ErpRoutes.OPERATIONS,
      labelRes = R.string.nav_operations,
      selectedIcon = Icons.Filled.GridView,
      unselectedIcon = Icons.Outlined.GridView,
    ),
    NavDestinationItem(
      route = ErpRoutes.LEDGER_SYNC,
      labelRes = R.string.nav_ledger_sync,
      selectedIcon = Icons.Filled.SyncAlt,
      unselectedIcon = Icons.Outlined.SyncAlt,
    ),
    NavDestinationItem(
      route = ErpRoutes.STUDIO,
      labelRes = R.string.nav_studio,
      selectedIcon = Icons.Filled.Palette,
      unselectedIcon = Icons.Outlined.Palette,
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandatory Step 1: Configure Firebase App Check with launch intent before any UI or ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      val viewModel: ErpViewModel = viewModel()
      val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

      ErpClawTheme(config = themeConfig) {
        ErpClawRootApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErpClawRootApp(viewModel: ErpViewModel) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ErpRoutes.COMMAND
  val snackbarHostState = remember { SnackbarHostState() }

  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
  val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
  val syncSettings by viewModel.syncSettings.collectAsStateWithLifecycle()
  val syncRunSummary by viewModel.syncRunSummary.collectAsStateWithLifecycle()
  val a2uiState by viewModel.a2uiState.collectAsStateWithLifecycle()
  val selectedReceiptUri by viewModel.selectedReceiptUri.collectAsStateWithLifecycle()

  val accounts by viewModel.accounts.collectAsStateWithLifecycle()
  val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
  val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val invoices by viewModel.invoices.collectAsStateWithLifecycle()
  val vendors by viewModel.vendors.collectAsStateWithLifecycle()
  val purchaseBills by viewModel.purchaseBills.collectAsStateWithLifecycle()
  val employees by viewModel.employees.collectAsStateWithLifecycle()
  val expenseClaims by viewModel.expenseClaims.collectAsStateWithLifecycle()
  val payrollRuns by viewModel.payrollRuns.collectAsStateWithLifecycle()
  val outboxItems by viewModel.syncOutbox.collectAsStateWithLifecycle()

  val pendingCount =
    outboxItems.count { it.status == SyncStatus.PENDING.name || it.status == SyncStatus.FAILED.name }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
      },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            topDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  NavIconWithBadge(
                    icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    label = label,
                    badgeCount = if (dest.route == ErpRoutes.LEDGER_SYNC) pendingCount else 0,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            topDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  NavIconWithBadge(
                    icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    label = label,
                    badgeCount = if (dest.route == ErpRoutes.LEDGER_SYNC) pendingCount else 0,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ErpRoutes.COMMAND,
          modifier = Modifier.weight(1f),
        ) {
          composable(ErpRoutes.COMMAND) {
            CommandHubScreen(
              isOnline = isOnline,
              useGeminiOnline = syncSettings.useGeminiOnline,
              pendingOutboxCount = pendingCount,
              a2uiState = a2uiState,
              receiptUri = selectedReceiptUri,
              onToggleGeminiOnline = viewModel::setUseGeminiOnline,
              onAttachReceipt = viewModel::attachReceiptUri,
              onSubmitCommand = viewModel::submitCommand,
              onSelectQuickSkill = viewModel::openQuickSkill,
              onDataChange = viewModel::updateA2uiData,
              onA2uiAction = viewModel::executeA2uiAction,
              onTriggerSync = viewModel::triggerManualSync,
            )
          }
          composable(ErpRoutes.OPERATIONS) {
            OperationsScreen(
              accounts = accounts,
              inventoryItems = inventoryItems,
              customers = customers,
              invoices = invoices,
              vendors = vendors,
              purchaseBills = purchaseBills,
              employees = employees,
              expenseClaims = expenseClaims,
              payrollRuns = payrollRuns,
              onOpenSkillInCommandHub = { skillKey ->
                viewModel.openQuickSkill(skillKey)
                navigateTopLevel(ErpRoutes.COMMAND)
              },
              onAdjustStock = viewModel::adjustInventoryStock,
              onMarkInvoicePaid = viewModel::markInvoicePaid,
              onAdvanceCustomerStage = viewModel::advanceCustomerStage,
              onPayBill = viewModel::payPurchaseBill,
              onApproveExpense = viewModel::approveAndPayExpense,
              onRunPayroll = viewModel::runPayroll,
              onLoadSampleWorkspace = viewModel::loadSampleWorkspace,
            )
          }
          composable(ErpRoutes.LEDGER_SYNC) {
            LedgerAndSyncScreen(
              isOnline = isOnline,
              syncSettings = syncSettings,
              syncRunSummary = syncRunSummary,
              outboxItems = outboxItems,
              journalEntries = journalEntries,
              onTriggerSync = viewModel::triggerManualSync,
              onToggleAutoSync = viewModel::setAutoSyncOnReconnect,
              onSaveWebhookUrl = viewModel::setSyncWebhookUrl,
            )
          }
          composable(ErpRoutes.STUDIO) {
            ThemeAndExportScreen(
              themeConfig = themeConfig,
              onSelectSeedPreset = viewModel::setSeedPreset,
              onSelectSurfaceMode = viewModel::setSurfaceMode,
              onToggleDynamicColor = viewModel::setUseDynamicColor,
              onExportJsonUri = viewModel::exportJsonToUri,
              onExportLedgerCsvUri = viewModel::exportLedgerCsvToUri,
              onExportInventoryCsvUri = viewModel::exportInventoryCsvToUri,
              onBuildShareJson = viewModel::buildShareJsonPayload,
              onLoadSampleWorkspace = viewModel::loadSampleWorkspace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NavIconWithBadge(
  icon: ImageVector,
  label: String,
  badgeCount: Int,
) {
  if (badgeCount > 0) {
    BadgedBox(
      badge = { Badge { Text(badgeCount.coerceAtMost(99).toString()) } }
    ) {
      Icon(imageVector = icon, contentDescription = label)
    }
  } else {
    Icon(imageVector = icon, contentDescription = label)
  }
}
