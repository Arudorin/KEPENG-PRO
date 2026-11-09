package com.example.feature.account

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.core.database.Account
import com.example.core.database.AccountType
import com.example.core.database.AccountWithBalance
import com.example.core.money.MoneyFormatter
import com.example.core.money.MoneyParser
import com.example.core.ui.CategoryGlyph
import com.example.core.ui.TacticalButton
import com.example.core.ui.TacticalCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.DeepCarmine
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AccountListScreen(
    accounts: List<AccountWithBalance>,
    onAddAccount: (Account) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onArchiveAccount: (String) -> Unit,
    onUnarchiveAccount: (String) -> Unit,
    onDeleteAccountPermanently: (String) -> Unit,
    onReconcile: (accountId: String, actualBalance: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountWithBalance?>(null) }
    var accountToReconcile by remember { mutableStateOf<AccountWithBalance?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountWithBalance?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCrimson,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("fab_add_account")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Akun")
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
                text = "Akun Keuangan",
                fontFamily = SpaceGrotesk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Kas, Rekening Bank, E-Wallet & Kartu Kredit",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { acc ->
                    AccountCardItem(
                        account = acc,
                        onEdit = { accountToEdit = acc },
                        onReconcile = { accountToReconcile = acc },
                        onDelete = { accountToDelete = acc },
                        onArchiveToggle = {
                            if (acc.isArchived) {
                                onUnarchiveAccount(acc.id)
                            } else {
                                if (acc.currentBalance != 0L) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Akun hanya bisa diarsipkan jika saldonya Rp 0!")
                                    }
                                } else {
                                    onArchiveAccount(acc.id)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Add Account Dialog
        if (showAddDialog) {
            AccountFormDialog(
                title = "Tambah Akun Baru",
                initialName = "",
                initialType = AccountType.CASH,
                initialBalance = 0L,
                initialIncludeInTotal = true,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, balance, includeInTotal ->
                    val newAcc = Account(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type.name,
                        initialBalance = balance,
                        includeInTotal = includeInTotal,
                        icon = when (type) {
                            AccountType.CASH -> "wallet"
                            AccountType.BANK -> "account_balance"
                            AccountType.EWALLET -> "smartphone"
                            AccountType.CREDIT -> "credit_card"
                            else -> "account_balance_wallet"
                        }
                    )
                    onAddAccount(newAcc)
                    showAddDialog = false
                }
            )
        }

        // Edit Account Dialog
        accountToEdit?.let { acc ->
            val curType = try { AccountType.valueOf(acc.type) } catch (e: Exception) { AccountType.CASH }
            AccountFormDialog(
                title = "Ubah Akun",
                initialName = acc.name,
                initialType = curType,
                initialBalance = acc.initialBalance,
                initialIncludeInTotal = acc.includeInTotal,
                isEdit = true,
                onDismiss = { accountToEdit = null },
                onConfirm = { name, type, _, includeInTotal ->
                    val updated = Account(
                        id = acc.id,
                        name = name,
                        type = type.name,
                        initialBalance = acc.initialBalance,
                        includeInTotal = includeInTotal,
                        colorHex = acc.colorHex,
                        icon = acc.icon,
                        sortOrder = acc.sortOrder,
                        isArchived = acc.isArchived,
                        createdAt = acc.createdAt
                    )
                    onUpdateAccount(updated)
                    accountToEdit = null
                }
            )
        }

        // Reconciliation Dialog
        accountToReconcile?.let { acc ->
            ReconcileDialog(
                account = acc,
                onDismiss = { accountToReconcile = null },
                onConfirm = { actualBalance ->
                    onReconcile(acc.id, actualBalance)
                    accountToReconcile = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Saldo akun ${acc.name} berhasil direkonsiliasi!")
                    }
                }
            )
        }

        // Delete Account Confirmation Dialog
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                containerColor = SurfaceElevated,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Hapus Akun '${acc.name}'?",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PERINGATAN: Menghapus akun ini akan menghapus saldo (${MoneyFormatter.format(acc.currentBalance)}) dan seluruh riwayat transaksi yang terhubung dengan akun ini secara permanen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tindakan ini tidak dapat dibatalkan. Apakah Anda yakin ingin melanjutkan?",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteAccountPermanently(acc.id)
                            accountToDelete = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Akun '${acc.name}' dan seluruh riwayatnya berhasil dihapus.")
                            }
                        }
                    ) {
                        Text("Ya, Hapus Permanen", color = NeonCrimson, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun AccountCardItem(
    account: AccountWithBalance,
    onEdit: () -> Unit,
    onReconcile: () -> Unit,
    onDelete: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    TacticalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (account.isArchived) OutlineHairline.copy(alpha = 0.5f) else OutlineHairline,
        backgroundColor = if (account.isArchived) SurfaceElevated.copy(alpha = 0.5f) else SurfaceBase
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryGlyph(
                        iconName = account.icon ?: "wallet",
                        kind = if (account.currentBalance >= 0) "INCOME" else "EXPENSE"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (account.isArchived) TextTertiary else TextPrimary
                            )
                            if (account.isArchived) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Arsip)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                        }

                        val typeDisplayName = try {
                            AccountType.valueOf(account.type).displayName
                        } catch (e: Exception) {
                            account.type
                        }
                        Text(
                            text = typeDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val isNegative = account.currentBalance < 0
                    Text(
                        text = MoneyFormatter.format(account.currentBalance),
                        fontFamily = SpaceGrotesk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative) NeonCrimson else TextPrimary
                    )
                    if (!account.includeInTotal) {
                        Text(
                            text = "Tidak dihitung",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rekonsiliasi Button
                TextButton(onClick = onReconcile) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Rekonsiliasi",
                        tint = TacticalMint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rekonsiliasi", color = TacticalMint, fontSize = 12.sp)
                }

                // Edit Button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ubah",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button (NEW!)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Akun",
                        tint = NeonCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Archive / Unarchive Button
                IconButton(onClick = onArchiveToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (account.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (account.isArchived) "Pulihkan" else "Arsipkan",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountFormDialog(
    title: String,
    initialName: String,
    initialType: AccountType,
    initialBalance: Long,
    initialIncludeInTotal: Boolean,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balance: Long, includeInTotal: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedType by remember { mutableStateOf(initialType) }
    var balanceText by remember { mutableStateOf(if (isEdit) initialBalance.toString() else "") }
    var includeInTotal by remember { mutableStateOf(initialIncludeInTotal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Akun", color = TextSecondary) },
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

                Text(
                    text = "Tipe Akun",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AccountType.values()) { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) NeonCrimson.copy(alpha = 0.2f) else SurfaceInput)
                                .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(4.dp))
                                .clickable { selectedType = type }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) NeonCrimson else TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (!isEdit) {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Saldo Awal (Rp)", color = TextSecondary) },
                        supportingText = {
                            val parsed = MoneyParser.parse(balanceText)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hitung dalam Total Saldo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = includeInTotal,
                        onCheckedChange = { includeInTotal = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCrimson,
                            checkedTrackColor = DeepCarmine
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val bal = MoneyParser.parse(balanceText)
                        onConfirm(name, selectedType, bal, includeInTotal)
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

@Composable
fun ReconcileDialog(
    account: AccountWithBalance,
    onDismiss: () -> Unit,
    onConfirm: (actualBalance: Long) -> Unit
) {
    var actualBalanceText by remember { mutableStateOf(account.currentBalance.toString()) }
    val parsedActual = MoneyParser.parse(actualBalanceText)
    val diff = parsedActual - account.currentBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "Rekonsiliasi Saldo",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Akun: ${account.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saldo di aplikasi saat ini: ${MoneyFormatter.format(account.currentBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = actualBalanceText,
                    onValueChange = { actualBalanceText = it },
                    label = { Text("Saldo Sebenarnya (Real)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TacticalMint,
                        unfocusedBorderColor = OutlineHairline,
                        focusedContainerColor = SurfaceInput,
                        unfocusedContainerColor = SurfaceInput
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Difference indicator
                val diffColor = if (diff >= 0) TacticalMint else NeonCrimson
                val diffSign = if (diff >= 0) "+" else ""
                Text(
                    text = "Penyesuaian: $diffSign${MoneyFormatter.format(diff)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )

                Text(
                    text = "Sistem akan membuat transaksi penyesuaian otomatis bertanggal hari ini. Penyesuaian ini tidak mempengaruhi laporan pengeluaran Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsedActual) }
            ) {
                Text("Terapkan", color = TacticalMint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}
