package com.example.feature.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.money.MoneyFormatter
import com.example.core.money.MoneyParser
import com.example.core.ui.OfflineStatusBadge
import com.example.core.ui.TacticalButton
import com.example.core.ui.TacticalCard
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun OnboardingScreen(
    onComplete: (startDay: Int, accountName: String, initialBalance: Long) -> Unit
) {
    var selectedStartDay by remember { mutableIntStateOf(1) }
    var accountName by remember { mutableStateOf("Dompet Tunai") }
    var initialBalanceText by remember { mutableStateOf("500000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundRoot)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Badge & Title
        OfflineStatusBadge()
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated)
                .border(1.dp, NeonCrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = NeonCrimson,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "KEPENG",
            fontFamily = PlusJakartaSans,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = (-0.02).sp
        )

        Text(
            text = "Sovereign Offline Personal Finance",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step 1: Financial Period Start Day
        TacticalCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = OutlineHairline
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TANGGAL AWAL PERIODE KEUANGAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Pilih tanggal mulai perhitungan bulanan (misal: tanggal 1 atau hari gajian)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 25, 27, 28).forEach { day ->
                        val isSelected = selectedStartDay == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCrimson.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCrimson else OutlineHairline,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedStartDay = day }
                                .testTag("start_day_$day"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tgl $day",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) NeonCrimson else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: First Account & Initial Balance
        TacticalCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = OutlineHairline
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = TacticalMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AKUN PERTAMA & SALDO AWAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Nama Akun (misal: Dompet Tunai / BCA)", color = TextSecondary) },
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
                        .testTag("onboarding_account_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Saldo Awal (Rp)", color = TextSecondary) },
                    supportingText = {
                        val parsed = MoneyParser.parse(initialBalanceText)
                        Text(
                            text = MoneyFormatter.format(parsed),
                            color = TacticalMint,
                            style = MaterialTheme.typography.bodySmall
                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_initial_balance")
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        TacticalButton(
            text = "MULAI KEPENG",
            onClick = {
                val initialBal = MoneyParser.parse(initialBalanceText)
                onComplete(selectedStartDay, accountName.ifBlank { "Dompet Utama" }, initialBal)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "onboarding_start_button"
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Data disimpan 100% di perangkat Anda tanpa cloud / akun eksternal.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            fontSize = 11.sp
        )
    }
}
