package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        Account::class,
        Category::class,
        TransactionEntity::class,
        Budget::class,
        DebtEntity::class,
        RecurringRule::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kepeng_finance.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default data on first creation
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDefaultData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDefaultData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()

            if (categoryDao.getCategoryCount() == 0) {
                val categories = DefaultCategories.getSeedCategories()
                categoryDao.insertAll(categories)
            }

            if (accountDao.getAccountCount() == 0) {
                val defaultAccounts = listOf(
                    Account(
                        id = UUID.randomUUID().toString(),
                        name = "Dompet Tunai",
                        type = AccountType.CASH.name,
                        initialBalance = 250000L,
                        includeInTotal = true,
                        colorHex = "#FF2A3B",
                        icon = "wallet",
                        sortOrder = 1
                    ),
                    Account(
                        id = UUID.randomUUID().toString(),
                        name = "BCA / Bank Utama",
                        type = AccountType.BANK.name,
                        initialBalance = 3500000L,
                        includeInTotal = true,
                        colorHex = "#00E599",
                        icon = "account_balance",
                        sortOrder = 2
                    ),
                    Account(
                        id = UUID.randomUUID().toString(),
                        name = "GoPay / E-Wallet",
                        type = AccountType.EWALLET.name,
                        initialBalance = 150000L,
                        includeInTotal = true,
                        colorHex = "#00A56D",
                        icon = "smartphone",
                        sortOrder = 3
                    )
                )
                defaultAccounts.forEach { accountDao.insertAccount(it) }
            }
        }
    }
}

object DefaultCategories {
    // Lampiran A Categories
    const val SYSTEM_ADJUST_INCOME = "SYS_ADJUST_INCOME"
    const val SYSTEM_ADJUST_EXPENSE = "SYS_ADJUST_EXPENSE"

    fun getSeedCategories(): List<Category> {
        val list = mutableListOf<Category>()

        // System categories
        list.add(
            Category(
                id = SYSTEM_ADJUST_INCOME,
                name = "Penyesuaian saldo (pemasukan)",
                kind = CategoryKind.INCOME.name,
                parentId = null,
                isSystem = true,
                sortOrder = 99
            )
        )
        list.add(
            Category(
                id = SYSTEM_ADJUST_EXPENSE,
                name = "Penyesuaian saldo (pengeluaran)",
                kind = CategoryKind.EXPENSE.name,
                parentId = null,
                isSystem = true,
                sortOrder = 99
            )
        )

        // Clean Essential Expense Categories
        val expenseCategories = listOf(
            Triple("Makan & Minum", "restaurant", "#FF5355"),
            Triple("Belanja Kebutuhan", "shopping_bag", "#9C27B0"),
            Triple("Transportasi", "directions_car", "#00E599"),
            Triple("Tagihan & Utilitas", "receipt_long", "#FFB300"),
            Triple("Hiburan & Hobi", "movie", "#00BCD4"),
            Triple("Kesehatan", "medical_services", "#E91E63"),
            Triple("Lain-lain", "category", "#607D8B")
        )

        expenseCategories.forEachIndexed { idx, (name, icon, color) ->
            list.add(
                Category(
                    id = "CAT_EXP_${idx + 1}",
                    name = name,
                    kind = CategoryKind.EXPENSE.name,
                    parentId = null,
                    colorHex = color,
                    icon = icon,
                    sortOrder = idx + 1
                )
            )
        }

        // Clean Essential Income Categories
        val incomeCategories = listOf(
            "Gaji" to "payments",
            "Bonus & Usaha" to "work",
            "Investasi" to "trending_up",
            "Lain-lain" to "category"
        )
        incomeCategories.forEachIndexed { index, (name, icon) ->
            list.add(
                Category(
                    id = "CAT_INC_${index + 1}",
                    name = name,
                    kind = CategoryKind.INCOME.name,
                    parentId = null,
                    colorHex = "#00E599",
                    icon = icon,
                    sortOrder = index + 1
                )
            )
        }

        return list
    }
}
