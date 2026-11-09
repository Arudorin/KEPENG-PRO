package com.example.feature.transaction

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.database.Category
import com.example.core.database.TransactionEntity
import com.example.core.database.TransactionType
import com.example.core.money.MoneyFormatter
import com.example.core.time.DateUtils
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.GlassCard
import com.example.core.ui.NumericKeypad
import com.example.core.ui.TacticalButton
import com.example.core.ui.TacticalCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.SurfaceInput
import androidx.activity.compose.BackHandler
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    accounts: List<AccountWithBalance>,
    categories: List<Category>,
    lastUsedAccountId: String?,
    lastUsedCategoryId: String?,
    initialTransaction: TransactionEntity? = null,
    onSave: (TransactionEntity, Boolean) -> Unit, // (entity, addAnother)
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Step state: 1 = Pilih Jenis, 2 = Masukkan Nominal (DANA keyboard), 3 = Detail & Simpan
    var currentStep by remember {
        mutableIntStateOf(if (initialTransaction != null) 2 else 1)
    }

    // Intercept hardware and gesture back navigation
    BackHandler {
        if (currentStep > 1 && initialTransaction == null) {
            currentStep -= 1
        } else {
            onBack()
        }
    }

    var selectedType by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                try { TransactionType.valueOf(initialTransaction.type) } catch (e: Exception) { TransactionType.EXPENSE }
            } else TransactionType.EXPENSE
        )
    }

    var amount by remember { mutableLongStateOf(initialTransaction?.amount ?: 0L) }
    var selectedAccountId by remember {
        mutableStateOf(
            initialTransaction?.accountId
                ?: lastUsedAccountId
                ?: accounts.firstOrNull()?.id
                ?: ""
        )
    }
    var selectedToAccountId by remember {
        mutableStateOf(
            initialTransaction?.toAccountId
                ?: accounts.getOrNull(1)?.id
                ?: accounts.firstOrNull()?.id
                ?: ""
        )
    }
    var selectedCategoryId by remember {
        mutableStateOf(
            initialTransaction?.categoryId
                ?: lastUsedCategoryId
                ?: categories.firstOrNull { it.kind == selectedType.name && !it.isSystem }?.id
        )
    }

    var selectedDate by remember {
        mutableStateOf(
            if (initialTransaction != null) DateUtils.fromEpochDay(initialTransaction.date) else LocalDate.now()
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Filter categories by selected transaction type
    val availableCategories = remember(categories, selectedType) {
        categories.filter { it.kind == selectedType.name && !it.isSystem && !it.isArchived }
    }

    // When switching types, ensure selectedCategoryId matches kind
    LaunchedEffect(selectedType) {
        if (selectedType != TransactionType.TRANSFER) {
            if (availableCategories.none { it.id == selectedCategoryId }) {
                selectedCategoryId = availableCategories.firstOrNull()?.id
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialTransaction != null) "Ubah Transaksi" else "Catat Cepat",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Langkah 1/3: Pilih Jenis Transaksi"
                                2 -> "Langkah 2/3: Masukkan Nominal"
                                else -> "Langkah 3/3: Isi Detail & Simpan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep > 1 && initialTransaction == null) {
                                currentStep -= 1
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundRoot
                )
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
                targetState = currentStep,
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
                label = "WizardStepAnimation"
            ) { step ->
                when (step) {
                    // STEP 1: Pilih Jenis Transaksi
                    1 -> Step1ChooseType(
                        onSelectType = { type ->
                            selectedType = type
                            currentStep = 2
                        }
                    )

                    // STEP 2: Masukkan Nominal (Keyboard DANA dengan 000)
                    2 -> Step2InputAmount(
                        selectedType = selectedType,
                        amount = amount,
                        onChangeAmount = { amount = it },
                        onNext = {
                            if (amount > 0) {
                                currentStep = 3
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nominal transaksi harus lebih dari Rp 0!")
                                }
                            }
                        },
                        onChangeType = { currentStep = 1 }
                    )

                    // STEP 3: Isi Detail & Simpan
                    3 -> Step3FillDetails(
                        selectedType = selectedType,
                        amount = amount,
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelectAccountId = { selectedAccountId = it },
                        selectedToAccountId = selectedToAccountId,
                        onSelectToAccountId = { selectedToAccountId = it },
                        categories = availableCategories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategoryId = { selectedCategoryId = it },
                        selectedDate = selectedDate,
                        onOpenDatePicker = { showDatePicker = true },
                        note = note,
                        onNoteChange = { note = it },
                        onSave = {
                            val isSaveEnabled = amount > 0 && selectedAccountId.isNotBlank() &&
                                (selectedType != TransactionType.TRANSFER || (selectedToAccountId.isNotBlank() && selectedAccountId != selectedToAccountId))

                            if (!isSaveEnabled) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Lengkapi akun dan nominal yang valid!")
                                }
                                return@Step3FillDetails
                            }

                            val finalTx = (initialTransaction ?: TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                type = selectedType.name,
                                amount = amount,
                                accountId = selectedAccountId,
                                toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                                categoryId = if (selectedType != TransactionType.TRANSFER) selectedCategoryId else null,
                                date = DateUtils.toEpochDay(selectedDate),
                                note = note.ifBlank { null }
                            )).copy(
                                type = selectedType.name,
                                amount = amount,
                                accountId = selectedAccountId,
                                toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                                categoryId = if (selectedType != TransactionType.TRANSFER) selectedCategoryId else null,
                                date = DateUtils.toEpochDay(selectedDate),
                                note = note.ifBlank { null }
                            )

                            onSave(finalTx, false)
                        }
                    )
                }
            }
        }

        // Date Picker Dialog
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

/**
 * LANGKAH 1: Pemilihan Jenis Transaksi
 */
@Composable
private fun Step1ChooseType(
    onSelectType: (TransactionType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mau catat transaksi apa hari ini?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = SpaceGrotesk
        )

        // 1. Pengeluaran Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectType(TransactionType.EXPENSE) }
                .testTag("choose_type_expense"),
            cornerRadius = 16,
            borderColor = NeonCrimson.copy(alpha = 0.4f)
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
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengeluaran",
                        fontFamily = SpaceGrotesk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Uang keluar untuk belanja, makan, cicilan, tagihan",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Pemasukan Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectType(TransactionType.INCOME) }
                .testTag("choose_type_income"),
            cornerRadius = 16,
            borderColor = TacticalMint.copy(alpha = 0.4f)
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
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = TacticalMint,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pemasukan",
                        fontFamily = SpaceGrotesk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Uang masuk dari gaji, bonus, usaha, piutang",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Transfer Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectType(TransactionType.TRANSFER) }
                .testTag("choose_type_transfer"),
            cornerRadius = 16,
            borderColor = Color(0xFF64B5F6).copy(alpha = 0.4f)
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
                        .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transfer Antar-Akun",
                        fontFamily = SpaceGrotesk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pindah dana antar rekening bank atau e-wallet Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * LANGKAH 2: Masukkan Nominal dengan Keyboard DANA
 */
@Composable
private fun Step2InputAmount(
    selectedType: TransactionType,
    amount: Long,
    onChangeAmount: (Long) -> Unit,
    onNext: () -> Unit,
    onChangeType: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Type Badge with change button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeColor = when (selectedType) {
                TransactionType.EXPENSE -> NeonCrimson
                TransactionType.INCOME -> TacticalMint
                TransactionType.TRANSFER -> Color(0xFF64B5F6)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(typeColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedType.displayName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    fontFamily = SpaceGrotesk
                )
            }

            Text(
                text = "Ubah Jenis",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                modifier = Modifier.clickable { onChangeType() }
            )
        }

        // Amount Display Cockpit
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOMINAL TRANSAKSI",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk,
                    letterSpacing = 0.08.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = MoneyFormatter.format(amount),
                    fontFamily = SpaceGrotesk,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (selectedType) {
                        TransactionType.EXPENSE -> NeonCrimson
                        TransactionType.INCOME -> TacticalMint
                        TransactionType.TRANSFER -> Color(0xFF64B5F6)
                    },
                    modifier = Modifier.testTag("amount_display")
                )
            }
        }

        // DANA-Style Numeric Keypad with '000'
        NumericKeypad(
            onDigitClick = { digit ->
                val curStr = amount.toString()
                if (curStr.length < 12) {
                    val newStr = if (amount == 0L) {
                        if (digit == "000") "0" else digit
                    } else {
                        curStr + digit
                    }
                    onChangeAmount(newStr.toLongOrNull() ?: amount)
                }
            },
            onBackspaceClick = {
                val curStr = amount.toString()
                onChangeAmount(if (curStr.length <= 1) 0L else curStr.dropLast(1).toLongOrNull() ?: 0L)
            },
            onQuickAdd = { add ->
                onChangeAmount(amount + add)
            },
            onClear = {
                onChangeAmount(0L)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Next Button (Large 56dp)
        TacticalButton(
            text = "Lanjut: Isi Detail Transaksi ➔",
            onClick = onNext,
            enabled = amount > 0,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_step2_next"
        )
    }
}

/**
 * LANGKAH 3: Lengkapi Detail (Akun, Kategori, Tanggal, Catatan) & Simpan
 */
@Composable
private fun Step3FillDetails(
    selectedType: TransactionType,
    amount: Long,
    accounts: List<AccountWithBalance>,
    selectedAccountId: String,
    onSelectAccountId: (String) -> Unit,
    selectedToAccountId: String,
    onSelectToAccountId: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategoryId: (String) -> Unit,
    selectedDate: LocalDate,
    onOpenDatePicker: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Capsule
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = SpaceGrotesk
                    )
                    Text(
                        text = MoneyFormatter.format(amount),
                        fontFamily = SpaceGrotesk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            TransactionType.EXPENSE -> NeonCrimson
                            TransactionType.INCOME -> TacticalMint
                            TransactionType.TRANSFER -> Color(0xFF64B5F6)
                        }
                    )
                }
            }
        }

        // Account Selector
        if (selectedType == TransactionType.TRANSFER) {
            Text(
                text = "DARI REKENING / DOMPET ASAL",
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCrimson.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectAccountId(acc.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = acc.name,
                            color = if (isSelected) NeonCrimson else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "KE REKENING / DOMPET TUJUAN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { acc ->
                    val isSelected = selectedToAccountId == acc.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) TacticalMint.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) TacticalMint else OutlineHairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectToAccountId(acc.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = acc.name,
                            color = if (isSelected) TacticalMint else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            }
        } else {
            Text(
                text = "PILIH REKENING / DOMPET",
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCrimson.copy(alpha = 0.15f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectAccountId(acc.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = acc.name,
                                color = if (isSelected) NeonCrimson else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                text = MoneyFormatter.format(acc.currentBalance),
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }

        // Category Selector (For Expense & Income)
        if (selectedType != TransactionType.TRANSFER) {
            Text(
                text = "PILIH KATEGORI",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCrimson.copy(alpha = 0.15f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectCategoryId(cat.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryGlyph(iconName = cat.icon, kind = cat.kind, colorHex = cat.colorHex)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cat.name,
                            color = if (isSelected) NeonCrimson else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            }
        }

        // Date Picker Button
        Text(
            text = "TANGGAL TRANSAKSI",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceInput)
                .border(1.dp, OutlineHairline, RoundedCornerShape(10.dp))
                .clickable { onOpenDatePicker() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = DateUtils.formatDisplayDate(selectedDate),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Note Input
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Catatan / Keterangan (Opsional)", color = TextSecondary, fontFamily = SpaceGrotesk) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonCrimson,
                unfocusedBorderColor = OutlineHairline,
                focusedContainerColor = SurfaceInput,
                unfocusedContainerColor = SurfaceInput
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_note_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Save Button (Large 56dp)
        TacticalButton(
            text = "SIMPAN TRANSAKSI ✓",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            testTag = "save_transaction_btn"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
