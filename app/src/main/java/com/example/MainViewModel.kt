package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.Account
import com.example.core.database.AccountType
import com.example.core.database.AccountWithBalance
import com.example.core.database.AppDatabase
import com.example.core.database.Budget
import com.example.core.database.Category
import com.example.core.database.CategorySpend
import com.example.core.database.DebtEntity
import com.example.core.database.DebtType
import com.example.core.database.DefaultCategories
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.datastore.PreferencesManager
import com.example.core.money.MoneyFormatter
import com.example.core.repository.FinanceRepository
import com.example.core.repository.PeriodSummary
import com.example.core.time.DateUtils
import com.example.core.time.FinancialPeriod
import com.example.core.time.PeriodResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = FinanceRepository(database)
    val preferencesManager = PreferencesManager(application)

    val startDay: StateFlow<Int> = preferencesManager.startDayFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val isBalanceHidden: StateFlow<Boolean> = preferencesManager.isBalanceHiddenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isOnboardingDone: StateFlow<Boolean> = preferencesManager.isOnboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val appPin: StateFlow<String?> = preferencesManager.appPinFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Period Navigation
    private val _currentPeriodMonth = MutableStateFlow(YearMonth.now())

    val currentPeriod: StateFlow<FinancialPeriod> = combine(_currentPeriodMonth, startDay) { ym, sDay ->
        val dateInMonth = ym.atDay(sDay.coerceIn(1, ym.lengthOfMonth()))
        PeriodResolver.resolve(dateInMonth, sDay)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PeriodResolver.resolve(LocalDate.now(), 1))

    val activeAccounts: StateFlow<List<AccountWithBalance>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountWithBalance>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Long> = repository.totalBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodTransactions: StateFlow<List<TransactionEntity>> = currentPeriod.flatMapLatest { period ->
        repository.getTransactionsForPeriod(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodSummary: StateFlow<PeriodSummary> = currentPeriod.flatMapLatest { period ->
        repository.getPeriodSummary(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary(0L, 0L, 0L))

    val periodBudgets: StateFlow<List<Budget>> = currentPeriod.flatMapLatest { period ->
        repository.getBudgetsForPeriod(period.key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBudget: StateFlow<Budget?> = currentPeriod.flatMapLatest { period ->
        repository.getTotalBudgetForPeriod(period.key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categorySpends: StateFlow<List<CategorySpend>> = currentPeriod.flatMapLatest { period ->
        repository.getExpenseSumsByCategory(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastUsedAccountId: StateFlow<String?> = preferencesManager.lastUsedAccountIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastUsedCategoryId: StateFlow<String?> = preferencesManager.lastUsedCategoryIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.populateDefaultData(database)
        }
    }

    fun completeOnboarding(selectedStartDay: Int, accountName: String, initialBalance: Long) {
        viewModelScope.launch {
            preferencesManager.setStartDay(selectedStartDay)
            val newAcc = Account(
                id = UUID.randomUUID().toString(),
                name = accountName,
                type = AccountType.CASH.name,
                initialBalance = initialBalance,
                includeInTotal = true,
                icon = "wallet",
                sortOrder = 1
            )
            repository.insertAccount(newAcc)
            preferencesManager.setOnboardingDone(true)
        }
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun toggleBalanceHidden() {
        viewModelScope.launch {
            preferencesManager.setBalanceHidden(!isBalanceHidden.value)
        }
    }

    fun setStartDay(day: Int) {
        viewModelScope.launch {
            preferencesManager.setStartDay(day)
        }
    }

    fun setAppPin(pin: String?) {
        viewModelScope.launch {
            preferencesManager.setAppPin(pin)
            if (pin != null) _isUnlocked.value = true
        }
    }

    fun previousPeriod() {
        _currentPeriodMonth.value = _currentPeriodMonth.value.minusMonths(1)
    }

    fun nextPeriod() {
        _currentPeriodMonth.value = _currentPeriodMonth.value.plusMonths(1)
    }

    fun addTransaction(transaction: TransactionEntity, addAnother: Boolean) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            preferencesManager.setLastUsed(transaction.accountId, transaction.categoryId)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
        }
    }

    fun restoreTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun archiveAccount(accountId: String) {
        viewModelScope.launch {
            repository.archiveAccount(accountId)
        }
    }

    fun unarchiveAccount(accountId: String) {
        viewModelScope.launch {
            repository.unarchiveAccount(accountId)
        }
    }

    fun deleteAccountPermanently(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccountPermanently(accountId)
        }
    }

    fun reconcileAccount(accountId: String, actualBalance: Long) {
        viewModelScope.launch {
            repository.reconcileAccount(accountId, actualBalance)
        }
    }

    // Debt & Installment Actions
    fun addDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.insertDebt(debt)
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch {
            repository.deleteDebt(id)
        }
    }

    fun recordDebtPayment(debt: DebtEntity, paymentAmount: Long, accountId: String, note: String?) {
        viewModelScope.launch {
            repository.recordDebtPayment(debt, paymentAmount, accountId, note)
        }
    }

    fun upsertBudget(budget: Budget) {
        viewModelScope.launch {
            repository.upsertBudget(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun copyBudgetsFromPreviousPeriod() {
        viewModelScope.launch {
            val prevKey = _currentPeriodMonth.value.minusMonths(1).toString()
            val curKey = currentPeriod.value.key
            repository.copyBudgetsFromPreviousPeriod(prevKey, curKey)
        }
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "KEPENG PRO")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val accArray = JSONArray()
        allAccounts.value.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("includeInTotal", acc.includeInTotal)
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val txArray = JSONArray()
        periodTransactions.value.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("accountId", tx.accountId)
            obj.put("toAccountId", tx.toAccountId ?: "")
            obj.put("categoryId", tx.categoryId ?: "")
            obj.put("date", tx.date)
            obj.put("note", tx.note ?: "")
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val debtArray = JSONArray()
        allDebts.value.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("title", d.title)
            obj.put("type", d.type)
            obj.put("contactPerson", d.contactPerson ?: "")
            obj.put("totalAmount", d.totalAmount)
            obj.put("paidAmount", d.paidAmount)
            obj.put("monthlyPayment", d.monthlyPayment)
            obj.put("totalTenorMonths", d.totalTenorMonths)
            obj.put("paidTenorMonths", d.paidTenorMonths)
            obj.put("isSettled", d.isSettled)
            debtArray.put(obj)
        }
        root.put("debts", debtArray)

        return root.toString(2)
    }

    fun importJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                viewModelScope.launch {
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val entity = TransactionEntity(
                            id = obj.getString("id"),
                            type = obj.getString("type"),
                            amount = obj.getLong("amount"),
                            accountId = obj.getString("accountId"),
                            toAccountId = obj.optString("toAccountId").ifEmpty { null },
                            categoryId = obj.optString("categoryId").ifEmpty { null },
                            date = obj.getInt("date"),
                            note = obj.optString("note").ifEmpty { null }
                        )
                        repository.insertTransaction(entity)
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("ID;Tanggal;Tipe;Kategori;Akun;Nominal;Catatan\n")
        val categoriesMap = allCategories.value.associateBy { it.id }
        val accountsMap = allAccounts.value.associateBy { it.id }

        periodTransactions.value.forEach { tx ->
            val dateStr = DateUtils.formatFullDate(DateUtils.fromEpochDay(tx.date))
            val catName = categoriesMap[tx.categoryId]?.name ?: if (tx.type == "TRANSFER") "Transfer" else "-"
            val accName = accountsMap[tx.accountId]?.name ?: "-"
            sb.append("${tx.id};$dateStr;${tx.type};$catName;$accName;${tx.amount};\"${tx.note ?: ""}\"\n")
        }
        return sb.toString()
    }

    fun seedSampleData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val todayEpoch = DateUtils.toEpochDay(today)
            val yesterdayEpoch = DateUtils.toEpochDay(today.minusDays(1))
            val twoDaysAgoEpoch = DateUtils.toEpochDay(today.minusDays(2))

            val accList = activeAccounts.value
            if (accList.isNotEmpty()) {
                val primaryAcc = accList.first().id
                val bankAcc = accList.getOrNull(1)?.id ?: primaryAcc

                val sampleTransactions = listOf(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = TransactionType.INCOME.name,
                        amount = 7500000L,
                        accountId = bankAcc,
                        categoryId = "CAT_INC_0", // Gaji
                        date = twoDaysAgoEpoch,
                        note = "Gaji Bulanan"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = TransactionType.EXPENSE.name,
                        amount = 45000L,
                        accountId = primaryAcc,
                        categoryId = "CAT_FOOD",
                        date = todayEpoch,
                        note = "Makan siang ayam geprek & es teh"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = TransactionType.EXPENSE.name,
                        amount = 28000L,
                        accountId = primaryAcc,
                        categoryId = "CAT_FOOD",
                        date = todayEpoch,
                        note = "Kopi susu sore"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = TransactionType.EXPENSE.name,
                        amount = 50000L,
                        accountId = primaryAcc,
                        categoryId = "CAT_TRANSPORT",
                        date = yesterdayEpoch,
                        note = "Isi Pertamax motor"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = TransactionType.EXPENSE.name,
                        amount = 175000L,
                        accountId = bankAcc,
                        categoryId = "CAT_BILLS",
                        date = twoDaysAgoEpoch,
                        note = "Token listrik PLN"
                    )
                )

                sampleTransactions.forEach { repository.insertTransaction(it) }

                // Sample Debts & Installments
                val sampleDebts = listOf(
                    DebtEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Cicilan Laptop Kantor",
                        type = DebtType.CICILAN.name,
                        contactPerson = "BCA Finance",
                        totalAmount = 12000000L,
                        paidAmount = 5000000L,
                        monthlyPayment = 1000000L,
                        totalTenorMonths = 12,
                        paidTenorMonths = 5,
                        accountId = bankAcc,
                        note = "Cicilan 0% 12 bulan"
                    ),
                    DebtEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Pinjaman ke Rekan Kerja",
                        type = DebtType.PIUTANG.name,
                        contactPerson = "Budi Santoso",
                        totalAmount = 1500000L,
                        paidAmount = 500000L,
                        note = "Dipinjam untuk biaya darurat"
                    ),
                    DebtEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Kredit Kendaraan Bermotor",
                        type = DebtType.HUTANG.name,
                        contactPerson = "Adira Finance",
                        totalAmount = 18000000L,
                        paidAmount = 12000000L,
                        monthlyPayment = 1500000L,
                        totalTenorMonths = 12,
                        paidTenorMonths = 8,
                        accountId = bankAcc,
                        note = "Jatuh tempo tgl 25 tiap bulan"
                    )
                )

                sampleDebts.forEach { repository.insertDebt(it) }
            }
        }
    }
}
