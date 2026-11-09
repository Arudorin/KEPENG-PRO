package com.example.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.GlassCard
import com.example.core.ui.TacticalCard
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
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    startDay: Int,
    onStartDayChange: (Int) -> Unit,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: (Boolean) -> Unit,
    isAppLockEnabled: Boolean,
    onSetAppPin: (String?) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onExportCsv: () -> String,
    onSeedSampleData: () -> Unit,
    onNavigateAccounts: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showStartDayDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonString by remember { mutableStateOf("") }
    var restoreInputString by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundRoot
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Back Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Pengaturan & Akun",
                            fontFamily = SpaceGrotesk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Kelola rekening, kedaulatan data & privasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            }

            // 1. Rekening & Akun Keuangan (Moved here from main tabs)
            item {
                SettingsSectionHeader("REKENING & DOMPET KEUANGAN")
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateAccounts() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF22222E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = NeonCrimson,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kelola Akun & Dompet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = SpaceGrotesk
                                    )
                                    Text(
                                        text = "Atur bank, e-wallet, uang tunai, atau hapus akun",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Financial Period
            item {
                SettingsSectionHeader("PERIODE KEUANGAN")
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStartDayDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = NeonCrimson,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Tanggal Mulai Periode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = SpaceGrotesk
                                    )
                                    Text(
                                        text = "Siklus bulanan dimulai tiap tanggal $startDay",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }
                            Text(
                                text = "Tgl $startDay",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCrimson,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }

            // 3. Security & Privacy
            item {
                SettingsSectionHeader("KEAMANAN & PRIVASI")
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Balance hiding toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TacticalMint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Sembunyikan Saldo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = SpaceGrotesk
                                    )
                                    Text(
                                        text = "Samarkan nominal saldo secara bawaan di Beranda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }
                            Switch(
                                checked = isBalanceHidden,
                                onCheckedChange = onToggleBalanceHidden,
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCrimson, checkedTrackColor = DeepCarmine)
                            )
                        }

                        // App PIN lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = NeonCrimson,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kunci Aplikasi (PIN)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = SpaceGrotesk
                                    )
                                    Text(
                                        text = if (isAppLockEnabled) "PIN aktif dan mengamankan aplikasi" else "Belum dipasang PIN keamanan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        showPinDialog = true
                                    } else {
                                        onSetAppPin(null)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCrimson, checkedTrackColor = DeepCarmine)
                            )
                        }
                    }
                }
            }

            // 4. Backup, Restore, Export
            item {
                SettingsSectionHeader("DATA & CADANGAN LOKAL")
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Backup JSON
                        SettingsActionRow(
                            icon = Icons.Default.FileUpload,
                            iconTint = TacticalMint,
                            title = "Cadangkan Data (JSON)",
                            subtitle = "Salin seluruh data akun, kategori, transaksi & anggaran",
                            onClick = {
                                backupJsonString = onExportJson()
                                showBackupDialog = true
                            }
                        )

                        // Restore JSON
                        SettingsActionRow(
                            icon = Icons.Default.FileDownload,
                            iconTint = NeonCrimson,
                            title = "Pulihkan Data (JSON)",
                            subtitle = "Impor data dari berkas cadangan JSON",
                            onClick = {
                                restoreInputString = ""
                                showRestoreDialog = true
                            }
                        )

                        // Export CSV
                        SettingsActionRow(
                            icon = Icons.Default.Download,
                            iconTint = Color(0xFF64B5F6),
                            title = "Ekspor Transaksi (CSV)",
                            subtitle = "Format tabel kompatibel dengan Excel & Google Sheets",
                            onClick = {
                                val csv = onExportCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("KEPENG_CSV", csv))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("CSV berhasil disalin ke papan klip (Clipboard)!")
                                }
                            }
                        )

                        // Seed Sample Data
                        SettingsActionRow(
                            icon = Icons.Default.Refresh,
                            iconTint = TextTertiary,
                            title = "Isi Data Simulasi Contoh",
                            subtitle = "Tambahkan beberapa contoh transaksi untuk eksplorasi",
                            onClick = {
                                onSeedSampleData()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Data simulasi berhasil dimuat!")
                                }
                            }
                        )
                    }
                }
            }

            // 5. About App
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KEPENG PRO v1.1",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Aplikasi Manajemen Keuangan Pribadi Berdaulat",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "100% Offline • Zero Tracking • Data Milik Anda Sepenuhnya",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontFamily = SpaceGrotesk,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Start Day Dialog
        if (showStartDayDialog) {
            StartDayDialog(
                currentDay = startDay,
                onDismiss = { showStartDayDialog = false },
                onSelect = { day ->
                    onStartDayChange(day)
                    showStartDayDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Tanggal mulai periode diubah menjadi tanggal $day")
                    }
                }
            )
        }

        // PIN Dialog
        if (showPinDialog) {
            PinSetupDialog(
                onDismiss = { showPinDialog = false },
                onConfirm = { pin ->
                    onSetAppPin(pin)
                    showPinDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("PIN aplikasi berhasil diaktifkan!")
                    }
                }
            )
        }

        // Backup Dialog
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Cadangan JSON KEPENG PRO", color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk) },
                text = {
                    Column {
                        Text(
                            text = "Data cadangan siap disalin. Simpan teks ini dengan aman:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupJsonString,
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceInput,
                                unfocusedContainerColor = SurfaceInput
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("KEPENG_BACKUP", backupJsonString))
                        showBackupDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cadangan JSON disalin ke papan klip!")
                        }
                    }) {
                        Text("Salin ke Clipboard", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Tutup", color = TextSecondary, fontFamily = SpaceGrotesk)
                    }
                }
            )
        }

        // Restore Dialog
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Pulihkan Cadangan JSON", color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk) },
                text = {
                    Column {
                        Text(
                            text = "Tempel teks cadangan JSON di bawah ini:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreInputString,
                            onValueChange = { restoreInputString = it },
                            placeholder = { Text("{\"version\":1,...}", color = TextTertiary) },
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceInput,
                                unfocusedContainerColor = SurfaceInput
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val success = onImportJson(restoreInputString)
                        showRestoreDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Data berhasil dipulihkan!" else "Format cadangan tidak valid."
                            )
                        }
                    }) {
                        Text("Pulihkan", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Batal", color = TextSecondary, fontFamily = SpaceGrotesk)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontWeight = FontWeight.Bold,
        fontFamily = SpaceGrotesk,
        letterSpacing = 0.08.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    }
}

@Composable
fun StartDayDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text("Pilih Tanggal Mulai Periode", color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih tanggal awal penghitungan (misal tanggal gajian):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = SpaceGrotesk
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 20, 25, 27).forEach { day ->
                        val isSelected = selected == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCrimson.copy(alpha = 0.2f) else SurfaceInput)
                                .border(1.dp, if (isSelected) NeonCrimson else OutlineHairline, RoundedCornerShape(8.dp))
                                .clickable { selected = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tgl $day",
                                color = if (isSelected) NeonCrimson else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text("Terapkan", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary, fontFamily = SpaceGrotesk)
            }
        }
    )
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Pasang PIN Aplikasi", color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk) },
        text = {
            Column {
                Text(
                    text = "Masukkan 4 digit PIN pengaman:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = SpaceGrotesk
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("4-Digit PIN", color = TextSecondary, fontFamily = SpaceGrotesk) },
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
                onClick = { if (pin.length == 4) onConfirm(pin) }
            ) {
                Text("Aktifkan", color = NeonCrimson, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary, fontFamily = SpaceGrotesk)
            }
        }
    )
}
