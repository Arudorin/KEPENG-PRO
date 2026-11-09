package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val displayName: String) {
    CASH("Kas / Tunai"),
    BANK("Rekening Bank"),
    EWALLET("Dompet Digital (E-Wallet)"),
    CREDIT("Kartu Kredit / PayLater"),
    SAVINGS("Tabungan / Deposito"),
    OTHER("Lainnya")
}

enum class CategoryKind {
    EXPENSE,
    INCOME
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan"),
    TRANSFER("Transfer")
}

enum class DebtType(val displayName: String) {
    HUTANG("Hutang Saya"),       // Uang yang kita pinjam dari orang / bank
    PIUTANG("Piutang"),          // Uang kita yang dipinjam orang lain
    CICILAN("Cicilan Barang")    // Angsuran PayLater / KPR / Elektronik
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String, // UUID
    val name: String,
    val type: String, // AccountType enum name
    val initialBalance: Long = 0L,
    val includeInTotal: Boolean = true,
    val colorHex: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [
        Index("kind"),
        Index("parentId")
    ]
)
data class Category(
    @PrimaryKey
    val id: String, // UUID
    val name: String,
    val kind: String, // CategoryKind enum name (EXPENSE or INCOME)
    val parentId: String? = null,
    val colorHex: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isSystem: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("accountId", "date"),
        Index("categoryId", "date")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String, // UUID
    val type: String, // TransactionType: EXPENSE, INCOME, TRANSFER
    val amount: Long, // Positive amount > 0
    val accountId: String, // Source account
    val toAccountId: String? = null, // Destination for TRANSFER
    val categoryId: String? = null, // Null for TRANSFER
    val date: Int, // LocalDate.toEpochDay()
    val note: String? = null,
    val recurringRuleId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["periodKey", "categoryId"], unique = true)
    ]
)
data class Budget(
    @PrimaryKey
    val id: String, // UUID
    val periodKey: String, // e.g. "2026-11"
    val categoryId: String?, // Null indicates overall total budget
    val amount: Long
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val type: String, // DebtType: HUTANG, PIUTANG, CICILAN
    val contactPerson: String? = null, // Nama pihak pemberi / penerima hutang
    val totalAmount: Long, // Total nominal kewajiban
    val paidAmount: Long = 0L, // Total yang telah dibayarkan / diterima kembali
    val dueDateEpoch: Int? = null, // Tanggal jatuh tempo (epochDay)
    val monthlyPayment: Long = 0L, // Nominal angsuran per bulan (jika cicilan)
    val totalTenorMonths: Int = 1, // Berapa bulan tenor
    val paidTenorMonths: Int = 0, // Berapa bulan angsuran terbayar
    val accountId: String? = null, // Akun penampung / pembayar
    val isSettled: Boolean = false, // Status lunas
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey
    val id: String,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amount: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val note: String? = null,
    val frequency: String, // DAILY, WEEKLY, MONTHLY
    val interval: Int = 1,
    val startDate: Int, // epochDay
    val autoPost: Boolean = true,
    val lastGeneratedDate: Int? = null,
    val isActive: Boolean = true
)
