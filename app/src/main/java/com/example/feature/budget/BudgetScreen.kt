package com.example.feature.budget

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.Budget
import com.example.core.database.Category
import com.example.core.database.CategorySpend
import com.example.core.money.MoneyFormatter
import com.example.core.money.MoneyParser
import com.example.core.time.FinancialPeriod
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.EmptyStateView
import com.example.core.ui.TacticalButton
import com.example.core.ui.TacticalCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

@Composable
fun BudgetScreen(
    currentPeriod: FinancialPeriod,
    budgets: List<Budget>,
    totalBudget: Budget?,
    totalSpent: Long,
    categorySpends: List<CategorySpend>,
    categories: List<Category>,
    onUpsertBudget: (Budget) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onCopyFromPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<Category?>(null) }
    var initialAmountForBudget by remember { mutableStateOf(0L) }
    var isTotalBudgetSetting by remember { mutableStateOf(false) }

    val expenseCategories = remember(categories) {
        categories.filter { it.kind == "EXPENSE" && !it.isSystem && !it.isArchived }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isTotalBudgetSetting = false
                    selectedCategoryForBudget = expenseCategories.firstOrNull()
                    initialAmountForBudget = 0L
                    showBudgetDialog = true
                },
                containerColor = NeonCrimson,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("fab_add_budget")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Anggaran")
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
                        text = "Anggaran Keuangan",
                        fontFamily = PlusJakartaSans,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Periode: ${currentPeriod.displayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Copy from previous period button
                TextButton(
                    onClick = onCopyFromPrevious,
                    modifier = Modifier.testTag("btn_copy_budget")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Lalu", color = NeonCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Total Budget Card
            TacticalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = OutlineHairline,
                backgroundColor = SurfaceBase
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ANGGARAN TOTAL PERIODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (totalBudget != null) "Ubah" else "+ Buat",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                isTotalBudgetSetting = true
                                selectedCategoryForBudget = null
                                initialAmountForBudget = totalBudget?.amount ?: 0L
                                showBudgetDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (totalBudget != null && totalBudget.amount > 0) {
                        val spent = totalSpent
                        val remaining = totalBudget.amount - spent
                        val ratio = (spent.toFloat() / totalBudget.amount.toFloat()).coerceIn(0f, 1.5f)
                        val percentage = ((spent.toDouble() / totalBudget.amount.toDouble()) * 100).toInt()

                        val statusColor = when {
                            percentage > 100 -> NeonCrimson
                            percentage >= 80 -> WarningAmber
                            else -> TacticalMint
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (remaining >= 0) "Sisa ${MoneyFormatter.format(remaining)}" else "Over-budget ${MoneyFormatter.format(kotlin.math.abs(remaining))}",
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Text(
                                    text = "Terpakai ${MoneyFormatter.format(spent)} dari ${MoneyFormatter.format(totalBudget.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                            Text(
                                text = "$percentage%",
                                fontFamily = PlusJakartaSans,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { ratio.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = SurfaceElevated
                        )
                    } else {
                        Text(
                            text = "Belum ada anggaran total untuk periode ${currentPeriod.key}. Buat anggaran untuk melacak batasan pengeluaran Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Budgets List
            Text(
                text = "Anggaran Per Kategori",
                fontFamily = PlusJakartaSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val categoryBudgets = remember(budgets) {
                budgets.filter { it.categoryId != null }
            }

            if (categoryBudgets.isEmpty()) {
                EmptyStateView(
                    title = "Belum Ada Anggaran Kategori",
                    subtitle = "Tekan tombol + untuk menambahkan batas anggaran per kategori seperti Makan, Belanja, atau Transportasi.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryBudgets, key = { it.id }) { bgt ->
                        val category = categories.find { it.id == bgt.categoryId }
                        val spent = categorySpends.find { it.categoryId == bgt.categoryId }?.totalAmount ?: 0L
                        val remaining = bgt.amount - spent
                        val ratio = (spent.toFloat() / bgt.amount.toFloat()).coerceIn(0f, 1.5f)
                        val percentage = ((spent.toDouble() / bgt.amount.toDouble()) * 100).toInt()

                        val statusColor = when {
                            percentage > 100 -> NeonCrimson
                            percentage >= 80 -> WarningAmber
                            else -> TacticalMint
                        }

                        TacticalCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = OutlineHairline
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryGlyph(
                                            iconName = category?.icon,
                                            kind = "EXPENSE",
                                            colorHex = category?.colorHex
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = category?.name ?: "Kategori",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Terpakai: ${MoneyFormatter.format(spent)} / ${MoneyFormatter.format(bgt.amount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$percentage%",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            fontSize = 14.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                isTotalBudgetSetting = false
                                                selectedCategoryForBudget = category
                                                initialAmountForBudget = bgt.amount
                                                showBudgetDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Ubah",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteBudget(bgt.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = TextTertiary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { ratio.coerceAtMost(1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = statusColor,
                                    trackColor = SurfaceElevated
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (remaining >= 0) "Sisa ${MoneyFormatter.format(remaining)}" else "Over-budget ${MoneyFormatter.format(kotlin.math.abs(remaining))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Budget Dialog (for Total or Category)
        if (showBudgetDialog) {
            BudgetFormDialog(
                isTotal = isTotalBudgetSetting,
                selectedCategory = selectedCategoryForBudget,
                availableCategories = expenseCategories,
                initialAmount = initialAmountForBudget,
                onDismiss = { showBudgetDialog = false },
                onConfirm = { catId, amount ->
                    val budget = Budget(
                        id = UUID.randomUUID().toString(),
                        periodKey = currentPeriod.key,
                        categoryId = catId,
                        amount = amount
                    )
                    onUpsertBudget(budget)
                    showBudgetDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Anggaran berhasil disimpan!")
                    }
                }
            )
        }
    }
}

@Composable
fun BudgetFormDialog(
    isTotal: Boolean,
    selectedCategory: Category?,
    availableCategories: List<Category>,
    initialAmount: Long,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String?, amount: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var currentCategory by remember { mutableStateOf(selectedCategory ?: availableCategories.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = if (isTotal) "Atur Anggaran Total" else "Atur Anggaran Kategori",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isTotal) {
                    Text(
                        text = "Pilih Kategori",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        items(availableCategories) { cat ->
                            val isSelected = currentCategory?.id == cat.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) NeonCrimson.copy(alpha = 0.2f) else SurfaceInput)
                                    .clickable { currentCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryGlyph(cat.icon, "EXPENSE", cat.colorHex)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) NeonCrimson else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Batas Anggaran (Rp)", color = TextSecondary) },
                    supportingText = {
                        val parsed = MoneyParser.parse(amountText)
                        Text(MoneyFormatter.format(parsed), color = TacticalMint, fontSize = 11.sp)
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = MoneyParser.parse(amountText)
                    if (amount > 0) {
                        val catId = if (isTotal) null else currentCategory?.id
                        onConfirm(catId, amount)
                    }
                }
            ) {
                Text("Simpan", color = NeonCrimson, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}
