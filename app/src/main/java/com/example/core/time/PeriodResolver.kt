package com.example.core.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FinancialPeriod(
    val key: String, // e.g. "2026-11"
    val startDate: LocalDate,
    val endDate: LocalDate,
    val displayLabel: String // e.g. "1 Nov - 30 Nov 2026" or "25 Okt - 24 Nov 2026"
)

object PeriodResolver {
    private val indonesianLocale = Locale("id", "ID")
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indonesianLocale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", indonesianLocale)

    /**
     * Resolves the financial period containing the given date based on startDay (1..28).
     */
    fun resolve(date: LocalDate, startDay: Int = 1): FinancialPeriod {
        val safeStartDay = startDay.coerceIn(1, 28)

        val startDate: LocalDate
        val endDate: LocalDate
        val periodKey: String

        if (safeStartDay == 1) {
            val ym = YearMonth.from(date)
            startDate = ym.atDay(1)
            endDate = ym.atEndOfMonth()
            periodKey = ym.toString() // "YYYY-MM"
        } else {
            if (date.dayOfMonth >= safeStartDay) {
                val currentYm = YearMonth.from(date)
                startDate = currentYm.atDay(safeStartDay)
                val nextYm = currentYm.plusMonths(1)
                endDate = nextYm.atDay(safeStartDay).minusDays(1)
                periodKey = currentYm.toString()
            } else {
                val prevYm = YearMonth.from(date).minusMonths(1)
                startDate = prevYm.atDay(safeStartDay)
                val currentYm = YearMonth.from(date)
                endDate = currentYm.atDay(safeStartDay).minusDays(1)
                periodKey = prevYm.toString()
            }
        }

        val displayLabel = if (safeStartDay == 1) {
            startDate.format(monthYearFormatter)
        } else {
            "${startDate.format(shortMonthFormatter)} - ${endDate.format(shortMonthFormatter)}"
        }

        return FinancialPeriod(
            key = periodKey,
            startDate = startDate,
            endDate = endDate,
            displayLabel = displayLabel
        )
    }

    /**
     * Given a period key like "2026-11" and startDay, returns the FinancialPeriod.
     */
    fun fromKey(periodKey: String, startDay: Int = 1): FinancialPeriod {
        val ym = try {
            YearMonth.parse(periodKey)
        } catch (e: Exception) {
            YearMonth.now()
        }
        val safeStartDay = startDay.coerceIn(1, 28)
        val startDate = ym.atDay(safeStartDay)
        return resolve(startDate, safeStartDay)
    }
}

object DateUtils {
    private val indonesianLocale = Locale("id", "ID")
    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesianLocale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indonesianLocale)
    private val dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", indonesianLocale)

    fun formatDisplayDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> "Hari Ini"
            date == today.minusDays(1) -> "Kemarin"
            date.year == today.year -> "${date.format(dayNameFormatter)}, ${date.dayOfMonth} ${date.format(DateTimeFormatter.ofPattern("MMM", indonesianLocale))}"
            else -> date.format(shortDateFormatter)
        }
    }

    fun formatFullDate(date: LocalDate): String {
        return date.format(dayMonthYearFormatter)
    }

    fun toEpochDay(date: LocalDate): Int = date.toEpochDay().toInt()

    fun fromEpochDay(epochDay: Int): LocalDate = LocalDate.ofEpochDay(epochDay.toLong())
}
