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
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceBase
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
    onDeleteTransaction: (TransactionEntity) -> Unit,
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
                        .background(Color(0xEE1E1E2C))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
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

        // 2. Cockpit Card: Total Balance + Daily Safe Spending Limit (Dark Glassmorphism)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL SALDO KAS BERSIH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            letterSpacing = 0.08.sp
                        )

                        IconButton(
                            onClick = onToggleBalanceHidden,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_toggle_balance_hide")
                        ) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Sembunyikan Saldo",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Huge balance text
                    Text(
                        text = if (isBalanceHidden) "Rp ••••••••" else MoneyFormatter.format(totalBalance),
                        fontFamily = SpaceGrotesk,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.testTag("home_total_balance")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Safe Limit & Remaining Days Dual Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Jatah Belanja Hari Ini
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B1B24))
                                .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "JATAH BELANJA HARI INI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = TacticalMint,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBalanceHidden) "Rp •••••" else "${MoneyFormatter.format(dailySafeAllowance)} / hari",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Batas aman pengeluaran",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }

                        // Sisa Hari Periode
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B1B24))
                                .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "SISA PERIODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$remainingDays Hari Lagi",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "s/d ${currentPeriod.endDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Integrated Cash Flow Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(TacticalMint, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Masuk: +${MoneyFormatter.format(periodSummary.totalIncome)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalMint,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SpaceGrotesk
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonCrimson, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Keluar: -${MoneyFormatter.format(periodSummary.totalExpense)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCrimson,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }

        // 3. Hutang, Piutang & Cicilan Widget
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KEWAJIBAN & CICILAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }

                        Text(
                            text = "Buka Anggaran →",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCrimson,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            modifier = Modifier.clickable { onNavigateDebts() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Sisa Hutang/Cicilan",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                text = MoneyFormatter.format(totalHutang),
                                fontFamily = SpaceGrotesk,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalHutang > 0) NeonCrimson else TextPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Piutang Anda",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                text = MoneyFormatter.format(totalPiutang),
                                fontFamily = SpaceGrotesk,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalMint
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Execution CTA Button (Large 56dp)
        item {
            TacticalButton(
                text = "+ CATAT TRANSAKSI CEPAT",
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
                    onClick = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
