package com.example.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.Category
import com.example.core.database.CategorySpend
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.money.MoneyFormatter
import com.example.core.repository.PeriodSummary
import com.example.core.time.FinancialPeriod
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.EmptyStateView
import com.example.core.ui.GlassCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ReportScreen(
    currentPeriod: FinancialPeriod,
    periodSummary: PeriodSummary,
    categorySpends: List<CategorySpend>,
    categories: List<Category>,
    allPeriodTransactions: List<TransactionEntity>,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCategoryDrillDown: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = periodSummary.totalIncome
    val totalExpense = periodSummary.totalExpense
    val netBalance = periodSummary.netBalance

    // Savings rate calculation
    val savingsRate = remember(totalIncome, netBalance) {
        if (totalIncome > 0) {
            ((netBalance.toDouble() / totalIncome.toDouble()) * 100).toInt()
        } else {
            0
        }
    }

    val sortedCategorySpends = remember(categorySpends) {
        categorySpends.sortedByDescending { it.totalAmount }
    }

    // Weekly data breakdown for the bar chart (4 weeks)
    data class WeeklyFlow(val label: String, val income: Long, val expense: Long)
    val weeklyStats = remember(allPeriodTransactions, currentPeriod) {
        val totalDays = maxOf(1, java.time.temporal.ChronoUnit.DAYS.between(currentPeriod.startDate, currentPeriod.endDate).toInt() + 1)
        val daysPerWeek = (totalDays / 4).coerceAtLeast(7)
        val startEpoch = currentPeriod.startDate.toEpochDay()

        (0..3).map { weekIdx ->
            val wStart = startEpoch + (weekIdx * daysPerWeek)
            val wEnd = if (weekIdx == 3) currentPeriod.endDate.toEpochDay() else wStart + daysPerWeek - 1

            val weekTxs = allPeriodTransactions.filter { it.date in wStart..wEnd }
            val wIncome = weekTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val wExpense = weekTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

            WeeklyFlow(label = "M${weekIdx + 1}", income = wIncome, expense = wExpense)
        }
    }

    val maxWeeklyFlow = remember(weeklyStats) {
        maxOf(1L, weeklyStats.maxOf { maxOf(it.income, it.expense) })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundRoot)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Period Selector Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousPeriod,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE1E1E2C))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Periode Sebelumnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Laporan Arus Kas",
                        fontFamily = SpaceGrotesk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentPeriod.displayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                }

                IconButton(
                    onClick = onNextPeriod,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE1E1E2C))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Periode Berikutnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Financial Health Matrix (Glassmorphism Card)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KESEHATAN ARUS KAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            letterSpacing = 0.08.sp
                        )

                        val isSurplus = netBalance >= 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSurplus) TacticalMint.copy(alpha = 0.15f) else NeonCrimson.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSurplus) "SURPLUS KEUANGAN" else "DEFISIT PERIODE",
                                color = if (isSurplus) TacticalMint else NeonCrimson,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total Income
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TacticalMint, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pemasukan", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MoneyFormatter.format(totalIncome),
                                fontFamily = SpaceGrotesk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalMint
                            )
                        }

                        // Total Expense
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = NeonCrimson, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pengeluaran", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MoneyFormatter.format(totalExpense),
                                fontFamily = SpaceGrotesk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCrimson
                            )
                        }

                        // Net
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Sisa Bersih", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MoneyFormatter.format(netBalance),
                                fontFamily = SpaceGrotesk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) TacticalMint else NeonCrimson
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Savings rate bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rasio Tabungan: $savingsRate%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                text = if (savingsRate >= 30) "Sangat Baik (≥30%)" else if (savingsRate > 0) "Cukup Baik" else "Waspada",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (savingsRate >= 30) TacticalMint else if (savingsRate > 0) Color(0xFFFFA000) else NeonCrimson,
                                fontFamily = SpaceGrotesk
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (savingsRate.coerceIn(0, 100) / 100f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = TacticalMint,
                            trackColor = Color(0xFF242434),
                        )
                    }
                }
            }
        }

        // 3. Visual Cash Flow Bar Chart (Grafik Batang 4 Minggu)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAFIK ARUS KAS MINGGUAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            letterSpacing = 0.08.sp
                        )

                        // Legend
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(TacticalMint, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Masuk", fontSize = 10.sp, color = TextSecondary, fontFamily = SpaceGrotesk)
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.size(8.dp).background(NeonCrimson, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar", fontSize = 10.sp, color = TextSecondary, fontFamily = SpaceGrotesk)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4-Column Bar Visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyStats.forEach { week ->
                            val incomeFraction = if (maxWeeklyFlow > 0) (week.income.toFloat() / maxWeeklyFlow.toFloat()).coerceIn(0.04f, 1f) else 0.04f
                            val expenseFraction = if (maxWeeklyFlow > 0) (week.expense.toFloat() / maxWeeklyFlow.toFloat()).coerceIn(0.04f, 1f) else 0.04f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(100.dp)
                                ) {
                                    // Income Bar
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .fillMaxHeight(fraction = incomeFraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(TacticalMint)
                                    )
                                    // Expense Bar
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .fillMaxHeight(fraction = expenseFraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(NeonCrimson)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = week.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Category Breakdown Section
        item {
            Text(
                text = "Komposisi Pengeluaran per Kategori",
                fontFamily = SpaceGrotesk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (sortedCategorySpends.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum Ada Pengeluaran",
                    subtitle = "Belum ada transaksi pengeluaran yang tercatat pada periode ini.",
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(sortedCategorySpends, key = { it.categoryId }) { spend ->
                val category = categories.find { it.id == spend.categoryId }
                val fraction = if (totalExpense > 0) (spend.totalAmount.toFloat() / totalExpense.toFloat()).coerceIn(0f, 1f) else 0f
                val percentage = (fraction * 100).toInt()

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { category?.let { onCategoryDrillDown(it) } },
                    cornerRadius = 14
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = category?.name ?: "Lainnya",
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$percentage% dari total pengeluaran",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = MoneyFormatter.format(spend.totalAmount),
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCrimson
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category proportional progress bar
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCrimson,
                            trackColor = Color(0xFF242434)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
