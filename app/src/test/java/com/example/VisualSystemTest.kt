package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BackgroundRoot
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.OnAccent
import com.example.ui.theme.OutlineHairline
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TacticalMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualSystemTest {
    @Test
    fun `shared palette uses dark graphite surfaces and semantic accents`() {
        assertEquals(Color(0xFF12120F), BackgroundRoot)
        assertEquals(Color(0xFF1B1B17), SurfaceBase)
        assertEquals(Color(0xFF242420), SurfaceElevated)
        assertEquals(Color(0xFFF2735C), NeonCrimson)
        assertEquals(Color(0xFF55D6A0), TacticalMint)
        assertEquals(Color(0xFFE8B45C), WarningAmber)
        assertEquals(Color(0xFFF3F0E8), TextPrimary)
        assertEquals(Color(0xFFC2BFB4), TextSecondary)
        assertEquals(Color(0xFFA8A59A), TextTertiary)
        assertEquals(Color(0xFF35352F), OutlineHairline)
        assertEquals(Color(0xFF1A0E0A), OnAccent)
    }

    @Test
    fun `important palette combinations meet readable contrast`() {
        assertTrue("primary text contrast", contrastRatio(TextPrimary, BackgroundRoot) >= 7.0)
        assertTrue("secondary text contrast", contrastRatio(TextSecondary, BackgroundRoot) >= 7.0)
        assertTrue("coral action contrast", contrastRatio(NeonCrimson, BackgroundRoot) >= 4.5)
        assertTrue("mint status contrast", contrastRatio(TacticalMint, BackgroundRoot) >= 4.5)
        // Accent fills (buttons, FABs, selected chips) carry a dark label, not white.
        assertTrue("label on accent contrast", contrastRatio(OnAccent, NeonCrimson) >= 4.5)
        assertTrue("text on card contrast", contrastRatio(TextPrimary, SurfaceBase) >= 7.0)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = luminance(foreground)
        val backgroundLuminance = luminance(background)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
