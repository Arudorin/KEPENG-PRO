package com.example

import com.example.core.money.MoneyFormatter
import com.example.core.money.MoneyParser
import com.example.core.time.PeriodResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
    @Test
    fun testMoneyParser_shortcutsAndFormats() {
        assertEquals(25_000L, MoneyParser.parse("25rb"))
        assertEquals(25_000L, MoneyParser.parse("25k"))
        assertEquals(50_000L, MoneyParser.parse("50ribu"))
        assertEquals(1_500_000L, MoneyParser.parse("1,5jt"))
        assertEquals(1_500_000L, MoneyParser.parse("1.5jt"))
        assertEquals(2_000_000L, MoneyParser.parse("2juta"))
        assertEquals(125_000L, MoneyParser.parse("125.000"))
        assertEquals(75_000L, MoneyParser.parse("Rp 75.000"))
        assertEquals(0L, MoneyParser.parse(""))
    }

    @Test
    fun testMoneyFormatter_rupiahFormatting() {
        assertEquals("Rp 1.250.000", MoneyFormatter.format(1_250_000L))
        assertEquals("Rp 50.000", MoneyFormatter.format(50_000L))
        assertEquals("Rp 0", MoneyFormatter.format(0L))
        assertEquals("-Rp 25.000", MoneyFormatter.formatWithSign(25_000L, isExpense = true))
        assertEquals("+Rp 100.000", MoneyFormatter.formatWithSign(100_000L, isExpense = false))
    }

    @Test
    fun testPeriodResolver_standardAndPaydayCycles() {
        // Test standard start day 1
        val date1 = LocalDate.of(2026, 11, 8)
        val period1 = PeriodResolver.resolve(date1, startDay = 1)
        assertEquals("2026-11", period1.key)
        assertEquals(LocalDate.of(2026, 11, 1), period1.startDate)
        assertEquals(LocalDate.of(2026, 11, 30), period1.endDate)

        // Test payday cycle start day 25
        // On 8 Nov, since 8 < 25, period started on 25 Oct and ends on 24 Nov
        val periodPayday = PeriodResolver.resolve(date1, startDay = 25)
        assertEquals("2026-10", periodPayday.key)
        assertEquals(LocalDate.of(2026, 10, 25), periodPayday.startDate)
        assertEquals(LocalDate.of(2026, 11, 24), periodPayday.endDate)

        // On 26 Nov, since 26 >= 25, period starts on 25 Nov and ends on 24 Dec
        val date2 = LocalDate.of(2026, 11, 26)
        val periodPayday2 = PeriodResolver.resolve(date2, startDay = 25)
        assertEquals("2026-11", periodPayday2.key)
        assertEquals(LocalDate.of(2026, 11, 25), periodPayday2.startDate)
        assertEquals(LocalDate.of(2026, 12, 24), periodPayday2.endDate)
    }
}
