package com.example.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.core.database.DebtEntity
import com.example.core.database.DebtType
import com.example.core.database.TransactionEntity
import com.example.core.money.MoneyFormatter
import com.example.core.repository.PeriodSummary
import com.example.core.time.FinancialPeriod
import com.example.core.ui.EmptyStateView
import com.example.core.ui.GlassCard
import com.example.core.ui.TacticalButton
import com.example.core.ui.TacticalCard
import com.example.feature.transaction.TransactionRowItem
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    totalBalance: Long,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    periodSummary: PeriodSummary,
    currentPeriod: FinancialPeriod,
    debts: List<DebtEntity>,
    recentTransactions: List<TransactionEntity>,
    accounts: List<AccountWithBalance>,
    categories: List<Category>,
    onAddTransaction: () -> Unit,
    onNavigateTransactions: () -> Unit,
    onNavigateDebts: () -> Unit,
    onNavigateSettings: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalHutang = remember(debts) {
        debts.filter { (it.type == DebtType.HUTANG.name || it.type == DebtType.CICILAN.name) && !it.isSettled }
            .sumOf { it.totalAmount - it.paidAmount }
    }
    val totalPiutang = remember(debts) {
        debts.filter { it.type == DebtType.PIUTANG.name && !it.isSettled }
            .sumOf { it.totalAmount - it.paidAmount }
    }

    val today = remember { LocalDate.now() }
    val remainingDays = remember(currentPeriod, today) {
        val days = ChronoUnit.DAYS.between(today, currentPeriod.endDate).toInt() + 1
        maxOf(1, days)
    }
    val dailySafeAllowance = remember(totalBalance, remainingDays) {
        if (totalBalance > 0) totalBalance / remainingDays else 0L
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundRoot)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top bar: Modern Clean Header ("Halo, Bos!") & Settings Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Halo, Bos!",
                        fontFamily = SpaceGrotesk,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.02).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale("id"))),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                }

                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineHairline, CircleShape)
                        .testTag("btn_home_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Balance is the anchor; supporting cash-flow figures sit in one open row.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SALDO TERSEDIA",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpaceGrotesk,
                        letterSpacing = 1.1.sp
                    )
                    IconButton(
                        onClick = onToggleBalanceHidden,
                        modifier = Modifier.size(40.dp).testTag("btn_toggle_balance_hide")
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Sembunyikan Saldo",
                            tint = TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Text(
                    text = if (isBalanceHidden) "Rp ••••••••" else MoneyFormatter.format(totalBalance),
                    fontFamily = SpaceGrotesk,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.6).sp,
                    modifier = Modifier.testTag("home_total_balance")
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aman per hari", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isBalanceHidden) "Rp •••••" else MoneyFormatter.format(dailySafeAllowance),
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalMint,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("· $remainingDays hari tersisa", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(OutlineHairline, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MASUK BULAN INI", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("+${MoneyFormatter.format(periodSummary.totalIncome)}", style = MaterialTheme.typography.titleMedium, color = TacticalMint, fontWeight = FontWeight.SemiBold)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("KELUAR BULAN INI", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("−${MoneyFormatter.format(periodSummary.totalExpense)}", style = MaterialTheme.typography.titleMedium, color = NeonCrimson, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Obligations are secondary: compact, directly scannable and linked to management.
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated).clickable { onNavigateDebts() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hutang & cicilan", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Sisa ${MoneyFormatter.format(totalHutang)}  ·  Piutang ${MoneyFormatter.format(totalPiutang)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = "Buka anggaran", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }

        // 4. Quick Execution CTA Button (Large 56dp)
        item {
            TacticalButton(
                text = "+ CATAT TRANSAKSI",
                onClick = onAddTransaction,
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_quick_add_btn"
            )
        }

        // 5. Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "5 Transaksi Terakhir",
                    fontFamily = SpaceGrotesk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.clickable { onNavigateTransactions() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Semua",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCrimson,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent transactions list
        if (recentTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum Ada Transaksi",
                    subtitle = "Mulai catat transaksi pertamamu untuk melacak arus kas.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                val category = categories.find { it.id == tx.categoryId }
                val sourceAccount = accounts.find { it.id == tx.accountId }
                val destAccount = accounts.find { it.id == tx.toAccountId }

                TransactionRowItem(
                    transaction = tx,
                    category = category,
                    sourceAccount = sourceAccount,
                    destAccount = destAccount,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
