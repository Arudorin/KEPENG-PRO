package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.core.database.TransactionEntity
import com.example.feature.account.AccountListScreen
import com.example.feature.debt.DebtInstallmentScreen
import com.example.feature.home.HomeScreen
import com.example.feature.onboarding.OnboardingScreen
import com.example.feature.report.ReportScreen
import com.example.feature.settings.PinLockScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.transaction.TransactionFormScreen
import com.example.feature.transaction.TransactionListScreen
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.Rajdhani
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Default.Home),
    TRANSACTIONS("Transaksi", Icons.Default.Receipt),
    BUDGET("Anggaran", Icons.Default.CreditCard),
    REPORTS("Laporan", Icons.Default.Assessment)
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val isOnboardingDone by viewModel.isOnboardingDone.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    // Overlay screen states
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAccountsOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var isAddingTransaction by remember { mutableStateOf(false) }

    // If lock is enabled and not unlocked yet
    if (isAppLockEnabled && !isUnlocked && !appPin.isNullOrBlank()) {
        PinLockScreen(
            correctPin = appPin!!,
            onUnlocked = { viewModel.unlockApp() }
        )
        return
    }

    // If onboarding not completed yet
    if (!isOnboardingDone) {
        OnboardingScreen(
            onComplete = { startDay, accountName, initialBalance ->
                viewModel.completeOnboarding(startDay, accountName, initialBalance)
            }
        )
        return
    }

    // If Transaction Form is open (Add or Edit)
    if (isAddingTransaction || transactionToEdit != null) {
        BackHandler {
            isAddingTransaction = false
            transactionToEdit = null
        }
        val activeAccounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
        val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
        val lastUsedAcc by viewModel.lastUsedAccountId.collectAsStateWithLifecycle()
        val lastUsedCat by viewModel.lastUsedCategoryId.collectAsStateWithLifecycle()

        TransactionFormScreen(
            accounts = activeAccounts,
            categories = allCategories,
            lastUsedAccountId = lastUsedAcc,
            lastUsedCategoryId = lastUsedCat,
            initialTransaction = transactionToEdit,
            onSave = { entity, addAnother ->
                if (transactionToEdit != null) {
                    viewModel.updateTransaction(entity)
                    transactionToEdit = null
                } else {
                    viewModel.addTransaction(entity, addAnother)
                    if (!addAnother) {
                        isAddingTransaction = false
                    }
                }
            },
            onBack = {
                isAddingTransaction = false
                transactionToEdit = null
            }
        )
        return
    }

    // If Account Management Screen is open (accessible from Settings)
    if (isAccountsOpen) {
        val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()

        BackHandler { isAccountsOpen = false }

        AccountListScreen(
            accounts = allAccounts,
            onAddAccount = { acc -> viewModel.addAccount(acc) },
            onUpdateAccount = { acc -> viewModel.updateAccount(acc) },
            onArchiveAccount = { accId -> viewModel.archiveAccount(accId) },
            onUnarchiveAccount = { accId -> viewModel.unarchiveAccount(accId) },
            onDeleteAccountPermanently = { accId -> viewModel.deleteAccountPermanently(accId) },
            onReconcile = { accId, actual -> viewModel.reconcileAccount(accId, actual) }
        )
        return
    }

    // If Settings Screen is open (accessible from Home top right)
    if (isSettingsOpen) {
        val startDay by viewModel.startDay.collectAsStateWithLifecycle()
        val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

        BackHandler { isSettingsOpen = false }

        SettingsScreen(
            startDay = startDay,
            onStartDayChange = { day -> viewModel.setStartDay(day) },
            isBalanceHidden = isBalanceHidden,
            onToggleBalanceHidden = { viewModel.toggleBalanceHidden() },
            isAppLockEnabled = isAppLockEnabled,
            onSetAppPin = { pin -> viewModel.setAppPin(pin) },
            onExportJson = { viewModel.exportJson() },
            onImportJson = { json -> viewModel.importJson(json) },
            onExportCsv = { viewModel.exportCsv() },
            onSeedSampleData = { viewModel.seedSampleData() },
            onNavigateAccounts = { isAccountsOpen = true },
            onNavigateBack = { isSettingsOpen = false }
        )
        return
    }

    // Main 4-Tab Interface with HorizontalPager Swipe Support
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    // Handle back button when on sub-tabs
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val periodSummary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val currentPeriod by viewModel.currentPeriod.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val activeAccounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allDebts by viewModel.allDebts.collectAsStateWithLifecycle()
    val categorySpends by viewModel.categorySpends.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundRoot,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xF508080E),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0x40FF003C),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .navigationBarsPadding()
            ) {
                MainTab.values().forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontFamily = Rajdhani,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            selectedTextColor = NeonCrimson,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = NeonCrimson.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> {
                    HomeScreen(
                        totalBalance = totalBalance,
                        isBalanceHidden = isBalanceHidden,
                        onToggleBalanceHidden = { viewModel.toggleBalanceHidden() },
                        periodSummary = periodSummary,
                        currentPeriod = currentPeriod,
                        debts = allDebts,
                        recentTransactions = recentTransactions,
                        accounts = activeAccounts,
                        categories = allCategories,
                        onAddTransaction = { isAddingTransaction = true },
                        onNavigateTransactions = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onNavigateDebts = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onNavigateSettings = { isSettingsOpen = true },
                        onEditTransaction = { tx -> transactionToEdit = tx },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                    )
                }

                1 -> {
                    TransactionListScreen(
                        transactions = periodTransactions,
                        accounts = allAccounts,
                        categories = allCategories,
                        currentPeriod = currentPeriod,
                        onAddTransaction = { isAddingTransaction = true },
                        onEditTransaction = { tx -> transactionToEdit = tx },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onRestoreTransaction = { tx -> viewModel.restoreTransaction(tx) }
                    )
                }

                2 -> {
                    // Tab 3: "Anggaran" (Hutang, Piutang, Cicilan)
                    DebtInstallmentScreen(
                        debts = allDebts,
                        accounts = activeAccounts,
                        onAddDebt = { debt -> viewModel.addDebt(debt) },
                        onDeleteDebt = { id -> viewModel.deleteDebt(id) },
                        onRecordPayment = { debt, amount, accId, note ->
                            viewModel.recordDebtPayment(debt, amount, accId, note)
                        }
                    )
                }

                3 -> {
                    ReportScreen(
                        currentPeriod = currentPeriod,
                        periodSummary = periodSummary,
                        categorySpends = categorySpends,
                        categories = allCategories,
                        allPeriodTransactions = periodTransactions,
                        onPreviousPeriod = { viewModel.previousPeriod() },
                        onNextPeriod = { viewModel.nextPeriod() },
                        onCategoryDrillDown = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
            }
        }
    }
}
