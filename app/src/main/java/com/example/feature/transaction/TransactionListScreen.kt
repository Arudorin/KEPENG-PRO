package com.example.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.AccountWithBalance
import com.example.core.database.Category
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.money.MoneyFormatter
import com.example.core.time.DateUtils
import com.example.core.time.FinancialPeriod
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.EmptyStateView
import com.example.core.ui.TacticalCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TransactionListScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountWithBalance>,
    categories: List<Category>,
    currentPeriod: FinancialPeriod,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onRestoreTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>("ALL") }

    // Filter transactions
    val filteredTransactions = remember(transactions, searchQuery, selectedTypeFilter) {
        transactions.filter { tx ->
            val matchesType = selectedTypeFilter == "ALL" || tx.type == selectedTypeFilter
            val category = categories.find { it.id == tx.categoryId }
            val matchesSearch = searchQuery.isBlank() ||
                (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                (category?.name?.contains(searchQuery, ignoreCase = true) == true)
            matchesType && matchesSearch
        }
    }

    // Group filtered transactions by date (epochDay)
    val groupedByDate = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = NeonCrimson,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Transaksi",
                        fontFamily = PlusJakartaSans,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentPeriod.displayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari catatan atau kategori...", color = TextTertiary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCrimson,
                    unfocusedBorderColor = OutlineHairline,
                    focusedContainerColor = SurfaceInput,
                    unfocusedContainerColor = SurfaceInput
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("transaction_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row (Enlarged to 48dp for easy thumb touch)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val filters = listOf(
                    "ALL" to "Semua",
                    TransactionType.EXPENSE.name to "Pengeluaran",
                    TransactionType.INCOME.name to "Pemasukan",
                    TransactionType.TRANSFER.name to "Transfer"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(
                                if (isSelected) NeonCrimson else Color(0xEE1E1E2C)
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonCrimson else Color(0x33FFFFFF),
                                RoundedCornerShape(9999.dp)
                            )
                            .clickable { selectedTypeFilter = key }
                            .padding(horizontal = 20.dp)
                            .testTag("filter_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SpaceGrotesk,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction Groups List
            if (groupedByDate.isEmpty()) {
                EmptyStateView(
                    title = "Belum Ada Transaksi",
                    subtitle = "Ketuk tombol + di bawah untuk mencatat pengeluaran atau pemasukan baru.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedByDate.forEach { (dateEpoch, dayTransactions) ->
                        val localDate = DateUtils.fromEpochDay(dateEpoch)
                        val dayExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                        val dayIncome = dayTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

                        item(key = "header_$dateEpoch") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateUtils.formatDisplayDate(localDate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dayIncome > 0) {
                                        Text(
                                            text = "+${MoneyFormatter.format(dayIncome)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TacticalMint,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (dayExpense > 0) {
                                        Text(
                                            text = "-${MoneyFormatter.format(dayExpense)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        items(dayTransactions, key = { it.id }) { tx ->
                            val category = categories.find { it.id == tx.categoryId }
                            val sourceAccount = accounts.find { it.id == tx.accountId }
                            val destAccount = accounts.find { it.id == tx.toAccountId }

                            TransactionRowItem(
                                transaction = tx,
                                category = category,
                                sourceAccount = sourceAccount,
                                destAccount = destAccount,
                                onClick = { onEditTransaction(tx) },
                                onDelete = {
                                    onDeleteTransaction(tx)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Transaksi berhasil dihapus",
                                            actionLabel = "Urungkan",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onRestoreTransaction(tx)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    category: Category?,
    sourceAccount: AccountWithBalance?,
    destAccount: AccountWithBalance?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    TacticalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_item_${transaction.id}"),
        borderColor = OutlineHairline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (transaction.type == TransactionType.TRANSFER.name) {
                CategoryGlyph(iconName = "account_balance", kind = "TRANSFER", colorHex = "#64B5F6")
            } else {
                CategoryGlyph(iconName = category?.icon, kind = transaction.type, colorHex = category?.colorHex)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                val title = when (transaction.type) {
                    TransactionType.TRANSFER.name -> "Transfer: ${sourceAccount?.name ?: "?"} → ${destAccount?.name ?: "?"}"
                    else -> category?.name ?: "Tanpa Kategori"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                val subline = buildString {
                    if (!transaction.note.isNullOrBlank()) {
                        append(transaction.note)
                        append(" • ")
                    }
                    if (transaction.type != TransactionType.TRANSFER.name) {
                        append(sourceAccount?.name ?: "Akun")
                    }
                }
                if (subline.isNotBlank()) {
                    Text(
                        text = subline,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }

            // Amount
            val amountColor = when (transaction.type) {
                TransactionType.EXPENSE.name -> NeonCrimson
                TransactionType.INCOME.name -> TacticalMint
                else -> Color(0xFF64B5F6)
            }
            val amountPrefix = when (transaction.type) {
                TransactionType.EXPENSE.name -> "-"
                TransactionType.INCOME.name -> "+"
                else -> ""
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${MoneyFormatter.format(transaction.amount)}",
                    fontFamily = PlusJakartaSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_tx_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
