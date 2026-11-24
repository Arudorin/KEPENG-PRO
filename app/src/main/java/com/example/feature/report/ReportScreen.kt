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
import androidx.compose.material3.HorizontalDivider
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
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import com.example.core.database.AccountWithBalance
import com.example.core.database.Category
import com.example.core.export.ReportPdfExporter
import com.example.core.export.ReportPdfModel
import com.example.core.database.CategorySpend
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.money.MoneyFormatter
import com.example.core.repository.PeriodSummary
import com.example.core.time.DateUtils
import com.example.core.time.FinancialPeriod
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.TacticalButton
import com.example.core.ui.EmptyStateView
import com.example.core.ui.GlassCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.WarningAmber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun ReportScreen(
    currentPeriod: FinancialPeriod,
    periodSummary: PeriodSummary,
    categorySpends: List<CategorySpend>,
    categories: List<Category>,
    allPeriodTransactions: List<TransactionEntity>,
    accounts: List<AccountWithBalance>,
    totalBalance: Long,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCategoryDrillDown: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    // Period span, pace and per-day flows — all derived from the period's own transactions.
    val periodDays = remember(currentPeriod) {
        maxOf(1, ChronoUnit.DAYS.between(currentPeriod.startDate, currentPeriod.endDate).toInt() + 1)
    }
    val elapsedDays = remember(currentPeriod, periodDays) {
        val today = LocalDate.now()
        when {
            today.isBefore(currentPeriod.startDate) -> 1
            today.isAfter(currentPeriod.endDate) -> periodDays
            else -> (ChronoUnit.DAYS.between(currentPeriod.startDate, today).toInt() + 1).coerceIn(1, periodDays)
        }
    }
    val avgExpensePerDay = totalExpense / maxOf(1, elapsedDays)

    data class DayFlow(val day: LocalDate, val expense: Long)

    val dailyFlows = remember(allPeriodTransactions, currentPeriod, periodDays) {
        val byDay = allPeriodTransactions.groupBy { it.date }
        (0 until periodDays).map { index ->
            val day = currentPeriod.startDate.plusDays(index.toLong())
            val dayTxs = byDay[day.toEpochDay().toInt()].orEmpty()
            DayFlow(
                day = day,
                expense = dayTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            )
        }
    }
    val maxDayExpense = remember(dailyFlows) { maxOf(1L, dailyFlows.maxOfOrNull { it.expense } ?: 0L) }
    val busiestDay = remember(dailyFlows) { dailyFlows.maxByOrNull { it.expense }?.takeIf { it.expense > 0 } }
    val activeDayCount = remember(dailyFlows) { dailyFlows.count { it.expense > 0 } }
    val shortDayFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID")) }

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
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineHairline, CircleShape)
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
                        text = "Laporan",
                        fontFamily = SpaceGrotesk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Arus kas · ${currentPeriod.displayLabel}",
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
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineHairline, CircleShape)
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
                                color = if (savingsRate >= 30) TacticalMint else if (savingsRate > 0) WarningAmber else NeonCrimson,
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
                            trackColor = SurfaceHighlight,
                        )
                    }
                }
            }
        }

        // 2b. Export laporan ke PDF
        item {
            TacticalButton(
                text = "EKSPOR LAPORAN (PDF)",
                onClick = {
                    val model = ReportPdfModel(
                        periodLabel = currentPeriod.displayLabel,
                        periodStartLabel = shortDayFormatter.format(currentPeriod.startDate),
                        periodEndLabel = shortDayFormatter.format(currentPeriod.endDate),
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        netBalance = netBalance,
                        savingsRatioPercent = savingsRate,
                        avgExpensePerDay = avgExpensePerDay,
                        activeDays = activeDayCount,
                        elapsedDays = elapsedDays,
                        busiestDayLabel = busiestDay?.let { shortDayFormatter.format(it.day) },
                        busiestDayAmount = busiestDay?.expense ?: 0L,
                        categories = sortedCategorySpends.map { spend ->
                            val name = categories.find { it.id == spend.categoryId }?.name ?: "Lainnya"
                            ReportPdfModel.CategoryLine(
                                name = name,
                                amount = spend.totalAmount,
                                percent = if (totalExpense > 0) {
                                    ((spend.totalAmount.toDouble() / totalExpense) * 100).toInt()
                                } else 0
                            )
                        },
                        dailyExpenses = dailyFlows.map { flow ->
                            ReportPdfModel.DayBar(
                                dayOfMonth = flow.day.dayOfMonth,
                                amount = flow.expense
                            )
                        },
                        balanceTotal = totalBalance,
                        balances = accounts.map { acc ->
                            ReportPdfModel.AccountLine(
                                name = acc.name,
                                amount = acc.currentBalance,
                                includeInTotal = acc.includeInTotal
                            )
                        }
                    )
                    val file = ReportPdfExporter.export(context, model)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan KEPENG ${currentPeriod.displayLabel}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Simpan / bagikan laporan PDF"))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Period summary tiles — pace, coverage, peak day, biggest expense
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RINGKASAN PERIODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk,
                        letterSpacing = 0.08.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            label = "RATA-RATA / HARI",
                            value = MoneyFormatter.format(avgExpensePerDay),
                            caption = "dari $elapsedDays hari berjalan",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "HARI AKTIF",
                            value = "$activeDayCount hari",
                            caption = "ada pengeluaran",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            label = "HARI TERBOROS",
                            value = busiestDay?.day?.format(shortDayFormatter) ?: "—",
                            caption = busiestDay?.let { MoneyFormatter.format(it.expense) } ?: "belum ada data",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "SISA SALDO",
                            value = MoneyFormatter.format(totalBalance),
                            caption = "${accounts.size} akun aktif",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Daily spending rhythm for the whole period
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18, backgroundColor = SurfaceBase) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "POLA PENGELUARAN HARIAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            letterSpacing = 0.08.sp
                        )
                        Text(
                            text = "$periodDays hari",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontFamily = SpaceGrotesk
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val peakDay = busiestDay?.day
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyFlows.forEach { flow ->
                            val fraction = (flow.expense.toFloat() / maxDayExpense.toFloat()).coerceIn(0f, 1f)
                            val isPeak = peakDay != null && flow.day == peakDay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(if (flow.expense <= 0L) 0.02f else fraction.coerceAtLeast(0.06f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        when {
                                            isPeak -> NeonCrimson
                                            flow.expense > 0L -> NeonCrimson.copy(alpha = 0.45f)
                                            else -> OutlineHairline
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = currentPeriod.startDate.format(shortDayFormatter),
                            fontSize = 10.sp,
                            color = TextTertiary,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "Puncak: ${peakDay?.format(shortDayFormatter) ?: "—"}",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = currentPeriod.endDate.format(shortDayFormatter),
                            fontSize = 10.sp,
                            color = TextTertiary,
                            fontFamily = SpaceGrotesk
                        )
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
                            trackColor = SurfaceHighlight
                        )
                    }
                }
            }
        }

        // 5. Sisa saldo seluruh akun
        item {
            Text(
                text = "Sisa Saldo",
                fontFamily = SpaceGrotesk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DI SEMUA AKUN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk,
                                letterSpacing = 0.08.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${accounts.size} akun terpantau",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontFamily = SpaceGrotesk
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(totalBalance),
                            fontFamily = SpaceGrotesk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalBalance < 0) NeonCrimson else TacticalMint
                        )
                    }

                    if (accounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = OutlineHairline, thickness = 1.dp)
                    }

                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (acc.includeInTotal) "Masuk total" else "Tidak dihitung",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                            Text(
                                text = MoneyFormatter.format(acc.currentBalance),
                                fontFamily = SpaceGrotesk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (acc.currentBalance < 0) NeonCrimson else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextTertiary,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk,
            letterSpacing = 0.06.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = SpaceGrotesk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            text = caption,
            fontSize = 10.sp,
            color = TextSecondary,
            fontFamily = SpaceGrotesk,
            maxLines = 1
        )
    }
}
