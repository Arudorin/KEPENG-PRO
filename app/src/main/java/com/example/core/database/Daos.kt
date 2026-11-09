package com.example.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AccountWithBalance(
    val id: String,
    val name: String,
    val type: String,
    val initialBalance: Long,
    val includeInTotal: Boolean,
    val colorHex: String?,
    val icon: String?,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val currentBalance: Long
)

data class CategorySpend(
    val categoryId: String,
    val totalAmount: Long
)

@Dao
interface AccountDao {
    @Query("""
        SELECT a.id, a.name, a.type, a.initialBalance, a.includeInTotal, a.colorHex, a.icon, a.sortOrder, a.isArchived, a.createdAt,
        (a.initialBalance 
         + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'INCOME' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.toAccountId = a.id)), 0)
         - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.accountId = a.id)), 0)
        ) AS currentBalance
        FROM accounts a
        WHERE a.isArchived = 0
        ORDER BY a.sortOrder ASC, a.name ASC
    """)
    fun getActiveAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>>

    @Query("""
        SELECT a.id, a.name, a.type, a.initialBalance, a.includeInTotal, a.colorHex, a.icon, a.sortOrder, a.isArchived, a.createdAt,
        (a.initialBalance 
         + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'INCOME' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.toAccountId = a.id)), 0)
         - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.accountId = a.id)), 0)
        ) AS currentBalance
        FROM accounts a
        ORDER BY a.isArchived ASC, a.sortOrder ASC, a.name ASC
    """)
    fun getAllAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): Account?

    @Query("""
        SELECT (a.initialBalance 
         + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'INCOME' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.toAccountId = a.id)), 0)
         - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' AND t.accountId = a.id) OR (t.type = 'TRANSFER' AND t.accountId = a.id)), 0)
        ) AS currentBalance
        FROM accounts a WHERE a.id = :id
    """)
    suspend fun getAccountCurrentBalance(id: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE kind = :kind AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getActiveCategoriesByKindFlow(kind: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsBetweenDatesFlow(startDate: Int, endDate: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: String): Int

    @Query("""
        SELECT categoryId, SUM(amount) AS totalAmount 
        FROM transactions 
        WHERE type = 'EXPENSE' 
          AND date BETWEEN :startDate AND :endDate
          AND categoryId IS NOT NULL
        GROUP BY categoryId
    """)
    fun getExpenseSumsByCategoryFlow(startDate: Int, endDate: Int): Flow<List<CategorySpend>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :type 
          AND date BETWEEN :startDate AND :endDate
          AND categoryId NOT IN (SELECT id FROM categories WHERE isSystem = 1)
    """)
    fun getTotalAmountByTypeFlow(type: String, startDate: Int, endDate: Int): Flow<Long?>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE periodKey = :periodKey")
    fun getBudgetsForPeriodFlow(periodKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE periodKey = :periodKey AND categoryId IS NULL LIMIT 1")
    fun getTotalBudgetForPeriodFlow(periodKey: String): Flow<Budget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String)

    @Query("SELECT * FROM budgets WHERE periodKey = :periodKey")
    suspend fun getBudgetsListForPeriod(periodKey: String): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, createdAt DESC")
    fun getAllDebtsFlow(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY isSettled ASC, createdAt DESC")
    fun getDebtsByTypeFlow(type: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: String): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: String)
}
