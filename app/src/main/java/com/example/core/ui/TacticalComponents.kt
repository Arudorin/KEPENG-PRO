package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepCarmine
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun OfflineStatusBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(SurfaceElevated)
            .border(1.dp, OutlineHairline, RoundedCornerShape(9999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha)
                .background(TacticalMint, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "TERSIMPAN LOKAL",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk
        )
    }
}

/**
 * Dark Glassmorphism Card with frosted translucent gradient and glowing specular neon border
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    borderColor: Color = Color(0x66FF003C),
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val brush = if (backgroundColor != null) {
        Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.85f)))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF2141420),
                Color(0xE608080E)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(
                width = 1.3.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.35f),
                        Color(0x0CFFFFFF)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = OutlineHairline,
    backgroundColor: Color = SurfaceBase,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun CategoryGlyph(
    iconName: String?,
    kind: String = "EXPENSE",
    colorHex: String? = null,
    modifier: Modifier = Modifier
) {
    val icon = resolveCategoryIcon(iconName)
    val tint = when {
        colorHex != null -> try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            if (kind == "INCOME") TacticalMint else NeonCrimson
        }
        kind == "INCOME" -> TacticalMint
        else -> NeonCrimson
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .border(1.dp, OutlineHairline, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun resolveCategoryIcon(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "restaurant", "food", "makan" -> Icons.Default.Restaurant
        "directions_car", "transport", "transportasi" -> Icons.Default.DirectionsCar
        "receipt_long", "bills", "tagihan" -> Icons.Default.ReceiptLong
        "shopping_bag", "shopping", "belanja" -> Icons.Default.ShoppingBag
        "medical_services", "health", "kesehatan" -> Icons.Default.MedicalServices
        "school", "education", "pendidikan" -> Icons.Default.School
        "movie", "entertainment", "hiburan" -> Icons.Default.Movie
        "family_restroom", "family", "keluarga" -> Icons.Default.FamilyRestroom
        "volunteer_activism", "social", "donasi" -> Icons.Default.VolunteerActivism
        "credit_card", "debt", "cicilan" -> Icons.Default.CreditCard
        "account_balance_wallet", "admin", "pajak" -> Icons.Default.AccountBalanceWallet
        "payments", "gaji" -> Icons.Default.Payments
        "work", "freelance" -> Icons.Default.Work
        "trending_up", "investasi" -> Icons.Default.TrendingUp
        "storefront", "penjualan" -> Icons.Default.Storefront
        "wallet" -> Icons.Default.AccountBalanceWallet
        "account_balance", "bank" -> Icons.Default.AccountBalance
        "smartphone", "ewallet" -> Icons.Default.Smartphone
        else -> Icons.Default.Category
    }
}

/**
 * Large, accessible 56dp action button
 */
@Composable
fun TacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    testTag: String = "tactical_button"
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCrimson,
                contentColor = TextPrimary,
                disabledContainerColor = DeepCarmine.copy(alpha = 0.5f),
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .height(56.dp)
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineHairline),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .height(56.dp)
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SpaceGrotesk
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Category,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated)
                .border(1.dp, OutlineHairline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontFamily = SpaceGrotesk,
            textAlign = TextAlign.Center
        )
    }
}
