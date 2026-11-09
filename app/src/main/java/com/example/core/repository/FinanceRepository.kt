package com.example.core.repository

import com.example.core.database.Account
import com.example.core.database.AccountDao
import com.example.core.database.AccountWithBalance
import com.example.core.database.AppDatabase
import com.example.core.database.Budget
import com.example.core.database.BudgetDao
import com.example.core.database.Category
import com.example.core.database.CategoryDao
import com.example.core.database.CategoryKind
import com.example.core.database.CategorySpend
import com.example.core.database.DebtDao
import com.example.core.database.DebtEntity
import com.example.core.database.DebtType
import com.example.core.database.DefaultCategories
import com.example.core.database.TransactionDao
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.time.DateUtils
import com.example.core.time.FinancialPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class PeriodSummary(
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long
)

class FinanceRepository(private val database: AppDatabase) {
    private val accountDao: AccountDao = database.accountDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val transactionDao: TransactionDao = database.transactionDao()
    private val budgetDao: BudgetDao = database.budgetDao()
    private val debtDao: DebtDao = database.debtDao()

    val activeAccounts: Flow<List<AccountWithBalance>> = accountDao.getActiveAccountsWithBalanceFlow()
    val allAccounts: Flow<List<AccountWithBalance>> = accountDao.getAllAccountsWithBalanceFlow()

    val totalBalance: Flow<Long> = activeAccounts.map { list ->
        list.filter { it.includeInTotal }.sumOf { it.currentBalance }
    }

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategoriesFlow()
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebtsFlow()

    fun getActiveCategories(kind: CategoryKind): Flow<List<Category>> =
        categoryDao.getActiveCategoriesByKindFlow(kind.name)

    fun getTransactionsForPeriod(period: FinancialPeriod): Flow<List<TransactionEntity>> {
        val startEpoch = DateUtils.toEpochDay(period.startDate)
        val endEpoch = DateUtils.toEpochDay(period.endDate)
        return transactionDao.getTransactionsBetweenDatesFlow(startEpoch, endEpoch)
    }

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactionsFlow(limit)

    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow()

    fun getPeriodSummary(period: FinancialPeriod): Flow<PeriodSummary> {
        val startEpoch = DateUtils.toEpochDay(period.startDate)
        val endEpoch = DateUtils.toEpochDay(period.endDate)

        val incomeFlow = transactionDao.getTotalAmountByTypeFlow(TransactionType.INCOME.name, startEpoch, endEpoch)
        val expenseFlow = transactionDao.getTotalAmountByTypeFlow(TransactionType.EXPENSE.name, startEpoch, endEpoch)

        return combine(incomeFlow, expenseFlow) { inc, exp ->
            val income = inc ?: 0L
            val expense = exp ?: 0L
            PeriodSummary(
                totalIncome = income,
                totalExpense = expense,
                netBalance = income - expense
            )
        }
    }

    fun getExpenseSumsByCategory(period: FinancialPeriod): Flow<List<CategorySpend>> {
        val startEpoch = DateUtils.toEpochDay(period.startDate)
        val endEpoch = DateUtils.toEpochDay(period.endDate)
        return transactionDao.getExpenseSumsByCategoryFlow(startEpoch, endEpoch)
    }

    fun getBudgetsForPeriod(periodKey: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForPeriodFlow(periodKey)

    fun getTotalBudgetForPeriod(periodKey: String): Flow<Budget?> =
        budgetDao.getTotalBudgetForPeriodFlow(periodKey)

    // Mutation operations
    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(id: String): TransactionEntity? {
        val existing = transactionDao.getTransactionById(id)
        if (existing != null) {
            transactionDao.deleteById(id)
        }
        return existing
    }

    suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun archiveAccount(accountId: String) {
        val account = accountDao.getAccountById(accountId) ?: return
        val currentBalance = accountDao.getAccountCurrentBalance(accountId) ?: 0L
        if (currentBalance == 0L) {
            accountDao.updateAccount(account.copy(isArchived = true))
        }
    }

    suspend fun unarchiveAccount(accountId: String) {
        val account = accountDao.getAccountById(accountId) ?: return
        accountDao.updateAccount(account.copy(isArchived = false))
    }

    /**
     * Delete account permanently along with all related transactions
     */
    suspend fun deleteAccountPermanently(accountId: String) {
        transactionDao.deleteTransactionsByAccountId(accountId)
        accountDao.deleteAccountById(accountId)
    }

    suspend fun getAccountById(id: String): Account? = accountDao.getAccountById(id)

    suspend fun countTransactionsForAccount(accountId: String): Int {
        return transactionDao.countTransactionsForAccount(accountId)
    }

    suspend fun canDeleteCategory(categoryId: String): Boolean {
        val count = transactionDao.countTransactionsForCategory(categoryId)
        return count == 0
    }

    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun upsertBudget(budget: Budget) = budgetDao.upsertBudget(budget)
    suspend fun deleteBudget(id: String) = budgetDao.deleteBudgetById(id)

    suspend fun copyBudgetsFromPreviousPeriod(fromKey: String, toKey: String) {
        val previousBudgets = budgetDao.getBudgetsListForPeriod(fromKey)
        val copied = previousBudgets.map {
            it.copy(
                id = UUID.randomUUID().toString(),
                periodKey = toKey
            )
        }
        budgetDao.insertAll(copied)
    }

    // Debt & Installment Operations
    suspend fun insertDebt(debt: DebtEntity) = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: DebtEntity) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(id: String) = debtDao.deleteDebtById(id)

    /**
     * Records an installment / repayment for a debt or loan.
     * Automatically adjusts paid amount and creates a corresponding transaction.
     */
    suspend fun recordDebtPayment(
        debt: DebtEntity,
        paymentAmount: Long,
        accountId: String,
        note: String?
    ) {
        val newPaid = debt.paidAmount + paymentAmount
        val isNowSettled = newPaid >= debt.totalAmount
        val newPaidTenor = if (debt.monthlyPayment > 0) {
            (newPaid / debt.monthlyPayment).toInt().coerceAtMost(debt.totalTenorMonths)
        } else debt.paidTenorMonths + 1

        val updatedDebt = debt.copy(
            paidAmount = newPaid,
            isSettled = isNowSettled,
            paidTenorMonths = newPaidTenor
        )
        debtDao.updateDebt(updatedDebt)

        // Create transaction: if paying HUTANG or CICILAN -> Expense. If receiving PIUTANG -> Income.
        val isExpense = debt.type == DebtType.HUTANG.name || debt.type == DebtType.CICILAN.name
        val txType = if (isExpense) TransactionType.EXPENSE.name else TransactionType.INCOME.name
        val categoryId = if (isExpense) "CAT_DEBT" else "CAT_INC_4" // Cicilan & Hutang or Hadiah/Lain-lain

        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = txType,
            amount = paymentAmount,
            accountId = accountId,
            categoryId = categoryId,
            date = DateUtils.toEpochDay(LocalDate.now()),
            note = note ?: "Pembayaran: ${debt.title}"
        )
        transactionDao.insertTransaction(transaction)
    }

    /**
     * Account balance reconciliation: creates an adjustment transaction bertanggal hari ini
     * using the system category.
     */
    suspend fun reconcileAccount(accountId: String, actualBalance: Long) {
        val currentBalance = accountDao.getAccountCurrentBalance(accountId) ?: 0L
        val diff = actualBalance - currentBalance
        if (diff == 0L) return

        val todayEpoch = DateUtils.toEpochDay(LocalDate.now())
        if (diff > 0) {
            // Surplus adjustment -> Income
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = TransactionType.INCOME.name,
                amount = diff,
                accountId = accountId,
                categoryId = DefaultCategories.SYSTEM_ADJUST_INCOME,
                date = todayEpoch,
                note = "Penyesuaian saldo (Rekonsiliasi)"
            )
            transactionDao.insertTransaction(transaction)
        } else {
            // Deficit adjustment -> Expense
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE.name,
                amount = kotlin.math.abs(diff),
                accountId = accountId,
                categoryId = DefaultCategories.SYSTEM_ADJUST_EXPENSE,
                date = todayEpoch,
                note = "Penyesuaian saldo (Rekonsiliasi)"
            )
            transactionDao.insertTransaction(transaction)
        }
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "KEPENG PRO")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        return root.toString()
    }
}
