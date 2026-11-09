package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onQuickAdd: (Long) -> Unit,
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick add chips row with generous height
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickAmounts = listOf(
                "+10rb" to 10_000L,
                "+50rb" to 50_000L,
                "+100rb" to 100_000L,
                "+500rb" to 500_000L
            )
            quickAmounts.forEach { (label, value) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineHairline, RoundedCornerShape(10.dp))
                        .clickable { onQuickAdd(value) }
                        .testTag("quick_add_$label"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }
            }
        }

        // Numeric keypad grid ala DANA (1-9, 000, 0, Backspace)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("000", "0", "BACK")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (key == "BACK" || key == "000") SurfaceElevated else SurfaceHighlight)
                            .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                            .clickable {
                                when (key) {
                                    "BACK" -> onBackspaceClick()
                                    "000" -> onDigitClick("000")
                                    else -> onDigitClick(key)
                                }
                            }
                            .testTag("keypad_btn_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "BACK") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Hapus",
                                tint = NeonCrimson,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                color = TextPrimary,
                                fontFamily = SpaceGrotesk,
                                fontSize = if (key == "000") 20.sp else 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
