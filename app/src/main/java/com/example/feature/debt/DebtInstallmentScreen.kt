package com.example.feature.debt

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.core.database.DebtEntity
import com.example.core.database.DebtType
import com.example.core.money.MoneyFormatter
import com.example.core.money.MoneyParser
import com.example.core.time.DateUtils
import com.example.core.ui.EmptyStateView
import com.example.core.ui.GlassCard
import com.example.core.ui.NumericKeypad
import com.example.core.ui.TacticalButton
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.DeepCarmine
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Composable
fun DebtInstallmentScreen(
    debts: List<DebtEntity>,
    accounts: List<AccountWithBalance>,
    onAddDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (String) -> Unit,
    onRecordPayment: (debt: DebtEntity, paymentAmount: Long, accountId: String, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var isAddingDebt by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }

    // Summary calculations
    val totalHutang = remember(debts) {
        debts.filter { it.type == DebtType.HUTANG.name && !it.isSettled }
            .sumOf { it.totalAmount - it.paidAmount }
    }
    val totalPiutang = remember(debts) {
        debts.filter { it.type == DebtType.PIUTANG.name && !it.isSettled }
            .sumOf { it.totalAmount - it.paidAmount }
    }
    val totalCicilan = remember(debts) {
        debts.filter { it.type == DebtType.CICILAN.name && !it.isSettled }
            .sumOf { it.totalAmount - it.paidAmount }
    }

    val filteredDebts = remember(debts, selectedFilter) {
        if (selectedFilter == "ALL") debts else debts.filter { it.type == selectedFilter }
    }

    // If Add Debt Wizard is Active
    if (isAddingDebt) {
        AddDebtWizard(
            accounts = accounts,
            onDismiss = { isAddingDebt = false },
            onConfirm = { newDebt ->
                onAddDebt(newDebt)
                isAddingDebt = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Kewajiban '${newDebt.title}' berhasil ditambahkan!")
                }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingDebt = true },
                containerColor = NeonCrimson,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(58.dp)
                    .testTag("fab_add_debt")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kewajiban", modifier = Modifier.size(28.dp))
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
            Text(
                text = "Anggaran & Kewajiban",
                fontFamily = SpaceGrotesk,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Pantau hutang, piutang, dan cicilan angsuran rutin",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = SpaceGrotesk
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Telemetry Cards in GlassCard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hutang
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14,
                    borderColor = NeonCrimson.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "HUTANG SAYA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyFormatter.format(totalHutang),
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCrimson
                        )
                    }
                }

                // Piutang
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14,
                    borderColor = TacticalMint.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PIUTANG SAYA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyFormatter.format(totalPiutang),
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalMint
                        )
                    }
                }

                // Cicilan
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14,
                    borderColor = WarningAmber.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SISA CICILAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyFormatter.format(totalCicilan),
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips (Enlarged to 48dp with pill shape)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val filters = listOf(
                    "ALL" to "Semua",
                    DebtType.HUTANG.name to "Hutang",
                    DebtType.PIUTANG.name to "Piutang",
                    DebtType.CICILAN.name to "Cicilan Barang"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) NeonCrimson else Color(0xEE1E1E2C))
                            .border(1.dp, if (isSelected) NeonCrimson else Color(0x33FFFFFF), RoundedCornerShape(9999.dp))
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 20.dp)
                            .testTag("filter_debt_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SpaceGrotesk,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Debts List
            if (filteredDebts.isEmpty()) {
                EmptyStateView(
                    title = "Tidak Ada Catatan",
                    subtitle = "Tekan tombol + di bawah untuk mencatat hutang, piutang, atau cicilan Anda.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onPay = { debtToPay = debt },
                            onDelete = {
                                onDeleteDebt(debt.id)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Catatan kewajiban berhasil dihapus.")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Record Payment Dialog
        debtToPay?.let { debt ->
            PaymentDialog(
                debt = debt,
                accounts = accounts,
                onDismiss = { debtToPay = null },
                onConfirm = { amount, accountId, note ->
                    onRecordPayment(debt, amount, accountId, note)
                    debtToPay = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Pembayaran berhasil dicatat dan memotong saldo akun!")
                    }
                }
            )
        }
    }
}

/**
 * WIZARD 3 LANGKAH: Tambah Kewajiban & Anggaran
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtWizard(
    accounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onConfirm: (DebtEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf(DebtType.CICILAN) }
    var amount by remember { mutableLongStateOf(0L) }
    var tenorMonths by remember { mutableIntStateOf(12) }

    var title by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler {
        if (step > 1) {
            step -= 1
        } else {
            onDismiss()
        }
    }

    Scaffold(
        containerColor = BackgroundRoot,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tambah Anggaran & Kewajiban",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                        Text(
                            text = when (step) {
                                1 -> "Langkah 1/3: Pilih Jenis Kewajiban"
                                2 -> "Langkah 2/3: Masukkan Nominal & Tenor"
                                else -> "Langkah 3/3: Detail Pihak & Simpan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step -= 1 else onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundRoot)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "DebtWizardAnimation"
            ) { curStep ->
                when (curStep) {
                    // LANGKAH 1: Pilih Jenis
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Pilih jenis kewajiban keuangan:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = SpaceGrotesk
                        )

                        // Cicilan Barang
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = DebtType.CICILAN
                                    step = 2
                                },
                            cornerRadius = 16,
                            borderColor = WarningAmber.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(WarningAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cicilan Barang / Angsuran", fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Kredit motor, laptop, ponsel, atau KPR bulanan", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Hutang Saya
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = DebtType.HUTANG
                                    step = 2
                                },
                            cornerRadius = 16,
                            borderColor = NeonCrimson.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NeonCrimson.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = NeonCrimson, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hutang Saya", fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Uang yang Anda pinjam dari teman, bank, atau pihak lain", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Piutang
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = DebtType.PIUTANG
                                    step = 2
                                },
                            cornerRadius = 16,
                            borderColor = TacticalMint.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(TacticalMint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TacticalMint, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Piutang (Uang Saya)", fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Uang Anda yang dipinjam orang lain dan perlu ditagih", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // LANGKAH 2: Masukkan Nominal & Tenor
                    2 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Amount Card
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL POKOK ${selectedType.displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = MoneyFormatter.format(amount),
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (selectedType) {
                                        DebtType.HUTANG -> NeonCrimson
                                        DebtType.PIUTANG -> TacticalMint
                                        DebtType.CICILAN -> WarningAmber
                                    }
                                )
                                if (selectedType == DebtType.CICILAN && tenorMonths > 0 && amount > 0) {
                                    val perMonth = amount / tenorMonths
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Estimasi: ${MoneyFormatter.format(perMonth)} / bulan (${tenorMonths}x)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TacticalMint,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }
                        }

                        // Tenor Selector if Cicilan
                        if (selectedType == DebtType.CICILAN) {
                            Text(
                                text = "PILIH DURASI / TENOR ANGSURAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3, 6, 12, 24, 36).forEach { t ->
                                    val isSelected = tenorMonths == t
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) WarningAmber.copy(alpha = 0.2f) else Color(0xEE1E1E2C))
                                            .border(1.dp, if (isSelected) WarningAmber else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                            .clickable { tenorMonths = t },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${t} Bln",
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) WarningAmber else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // DANA Keypad with '000'
                        NumericKeypad(
                            onDigitClick = { digit ->
                                val curStr = amount.toString()
                                if (curStr.length < 12) {
                                    val newStr = if (amount == 0L) {
                                        if (digit == "000") "0" else digit
                                    } else curStr + digit
                                    amount = newStr.toLongOrNull() ?: amount
                                }
                            },
                            onBackspaceClick = {
                                val curStr = amount.toString()
                                amount = if (curStr.length <= 1) 0L else curStr.dropLast(1).toLongOrNull() ?: 0L
                            },
                            onQuickAdd = { add -> amount += add },
                            onClear = { amount = 0L }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TacticalButton(
                            text = "Lanjut: Pihak & Detail ➔",
                            onClick = { step = 3 },
                            enabled = amount > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // LANGKAH 3: Lengkapi Pihak & Detail
                    3 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Capsule
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(selectedType.displayName, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = SpaceGrotesk)
                                    Text(MoneyFormatter.format(amount), fontFamily = SpaceGrotesk, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                if (selectedType == DebtType.CICILAN) {
                                    Text("${tenorMonths} Bulan", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, color = WarningAmber)
                                }
                            }
                        }

                        // Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Judul (misal: Cicilan Motor Vario, Pinjaman Bank)", color = TextSecondary, fontFamily = SpaceGrotesk) },
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

                        // Contact Person
                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Pihak / Lembaga (misal: FIF, Andi, BCA)", color = TextSecondary, fontFamily = SpaceGrotesk) },
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

                        // Due Date Picker Button
                        Text("TANGGAL JATUH TEMPO", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceInput)
                                .border(1.dp, OutlineHairline, RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(DateUtils.formatDisplayDate(selectedDate), color = TextPrimary, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Note Input
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Catatan Tambahan (opsional)", color = TextSecondary, fontFamily = SpaceGrotesk) },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save Button (56dp)
                        TacticalButton(
                            text = "SIMPAN ANGGARAN ✓",
                            onClick = {
                                if (title.isBlank()) {
                                    title = if (contactPerson.isNotBlank()) "${selectedType.displayName} ($contactPerson)" else selectedType.displayName
                                }
                                val monthly = if (selectedType == DebtType.CICILAN && tenorMonths > 0) amount / tenorMonths else 0L
                                val entity = DebtEntity(
                                    id = UUID.randomUUID().toString(),
                                    type = selectedType.name,
                                    title = title,
                                    contactPerson = contactPerson.ifBlank { null },
                                    totalAmount = amount,
                                    paidAmount = 0L,
                                    dueDateEpoch = DateUtils.toEpochDay(selectedDate),
                                    totalTenorMonths = if (selectedType == DebtType.CICILAN) tenorMonths else 1,
                                    monthlyPayment = monthly,
                                    accountId = selectedAccountId,
                                    note = note.ifBlank { null },
                                    isSettled = false,
                                    createdAt = System.currentTimeMillis()
                                )
                                onConfirm(entity)
                            },
                            enabled = amount > 0,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Date Picker
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text("Pilih", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal", color = TextSecondary, fontFamily = SpaceGrotesk)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
    val ratio = if (debt.totalAmount > 0) (debt.paidAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f) else 0f
    val percentage = ((ratio) * 100).toInt()

    val typeColor = when (debt.type) {
        DebtType.HUTANG.name -> NeonCrimson
        DebtType.PIUTANG.name -> TacticalMint
        DebtType.CICILAN.name -> WarningAmber
        else -> TextPrimary
    }

    val typeLabel = when (debt.type) {
        DebtType.HUTANG.name -> "Hutang"
        DebtType.PIUTANG.name -> "Piutang"
        DebtType.CICILAN.name -> "Cicilan"
        else -> debt.type
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16,
        borderColor = if (debt.isSettled) TacticalMint.copy(alpha = 0.4f) else Color(0x33FFFFFF)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(typeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = typeLabel.uppercase(),
                                color = typeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (debt.isSettled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TacticalMint.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "LUNAS",
                                    color = TacticalMint,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = debt.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = SpaceGrotesk
                    )

                    if (!debt.contactPerson.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = debt.contactPerson,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (debt.isSettled) "Lunas" else "Sisa ${MoneyFormatter.format(remaining)}",
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.isSettled) TacticalMint else typeColor
                    )
                    Text(
                        text = "Total ${MoneyFormatter.format(debt.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontFamily = SpaceGrotesk
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = typeColor,
                trackColor = Color(0xFF242434)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$percentage% terbayar",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!debt.isSettled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222230))
                                .border(1.dp, OutlineHairline, RoundedCornerShape(8.dp))
                                .clickable { onPay() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Bayar / Cicil",
                                color = NeonCrimson,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    debt: DebtEntity,
    accounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String?) -> Unit
) {
    val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
    var paymentAmount by remember {
        mutableLongStateOf(debt.monthlyPayment ?: remaining)
    }
    var selectedAccountId by remember {
        mutableStateOf(debt.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var note by remember { mutableStateOf("Pembayaran ${debt.title}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "Catat Pembayaran",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sisa yang harus dibayar: ${MoneyFormatter.format(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontFamily = SpaceGrotesk
                )

                // Quick full pay or monthly pay
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (debt.monthlyPayment != null && debt.monthlyPayment > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF222230))
                                .clickable { paymentAmount = debt.monthlyPayment }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("1x Angsuran", color = WarningAmber, fontSize = 11.sp, fontFamily = SpaceGrotesk)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF222230))
                            .clickable { paymentAmount = remaining }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Lunasi Semua", color = TacticalMint, fontSize = 11.sp, fontFamily = SpaceGrotesk)
                    }
                }

                Text(
                    text = "Nominal: ${MoneyFormatter.format(paymentAmount)}",
                    fontFamily = SpaceGrotesk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCrimson
                )

                // Select Account
                Text(
                    text = "POTONG DARI AKUN:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccountId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCrimson.copy(alpha = 0.2f) else Color(0xFF1E1E28))
                                .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(8.dp))
                                .clickable { selectedAccountId = acc.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = acc.name,
                                color = if (isSelected) NeonCrimson else TextPrimary,
                                fontFamily = SpaceGrotesk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan", color = TextSecondary, fontFamily = SpaceGrotesk) },
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
                    if (paymentAmount > 0 && selectedAccountId.isNotBlank()) {
                        onConfirm(paymentAmount, selectedAccountId, note.ifBlank { null })
                    }
                }
            ) {
                Text("Bayar Sekarang", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary, fontFamily = SpaceGrotesk)
            }
        }
    )
}
