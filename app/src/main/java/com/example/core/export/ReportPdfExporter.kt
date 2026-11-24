package com.example.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.money.MoneyFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Laporan arus kas satu periode dalam bentuk PDF A4 (595 x 842 pt).
 *
 * Semua angka berasal dari data periode yang sedang dilihat di layar Laporan,
 * jadi PDF tidak pernah menampilkan angka yang berbeda dari aplikasi.
 */
data class ReportPdfModel(
    val periodLabel: String,
    val periodStartLabel: String,
    val periodEndLabel: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
    val savingsRatioPercent: Int,
    val avgExpensePerDay: Long,
    val activeDays: Int,
    val elapsedDays: Int,
    val busiestDayLabel: String?,
    val busiestDayAmount: Long,
    val categories: List<CategoryLine>,
    val dailyExpenses: List<DayBar> = emptyList(),
    val balanceTotal: Long = 0L,
    val balances: List<AccountLine> = emptyList()
) {
    data class DayBar(val dayOfMonth: Int, val amount: Long)
    data class CategoryLine(val name: String, val amount: Long, val percent: Int)
    data class AccountLine(val name: String, val amount: Long, val includeInTotal: Boolean)
}

object ReportPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private val ink = Color.parseColor("#12120F")
    private val coral = Color.parseColor("#F2735C")
    private val mint = Color.parseColor("#55D6A0")
    private val amber = Color.parseColor("#E8B45C")
    private val grey = Color.parseColor("#6F6C63")
    private val hairline = Color.parseColor("#DEDAD0")

    fun export(context: Context, model: ReportPdfModel): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        drawReport(page.canvas, model)
        document.finishPage(page)

        val dir = File(context.cacheDir, "laporan").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale("id", "ID")).format(Date())
        val safePeriod = model.periodLabel.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        val file = File(dir, "KEPENG_Laporan_${safePeriod}_$stamp.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawReport(canvas: Canvas, model: ReportPdfModel) {
        val thin = paint(Color.GRAY, 8.5f)
        val label = paint(grey, 8.5f).apply { letterSpacing = 0.12f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
        val body = paint(ink, 10f)
        val bodyBold = paint(ink, 10f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
        val h2 = paint(ink, 12f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }

        // ---------- Header ----------
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 128f, paint(ink, 1f))
        canvas.drawRect(0f, 128f, PAGE_WIDTH.toFloat(), 132f, paint(coral, 1f))

        canvas.drawText("KEPENG", MARGIN, 48f, paint(Color.WHITE, 22f).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.18f
        })
        canvas.drawText("Laporan Arus Kas", MARGIN, 70f, paint(Color.parseColor("#C2BFB4"), 10f))
        canvas.drawText(model.periodLabel, MARGIN, 100f, paint(Color.WHITE, 14f).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val printedAt = SimpleDateFormat("d MMM yyyy · HH:mm", Locale("id", "ID")).format(Date())
        drawRight(canvas, "Dicetak $printedAt", PAGE_WIDTH - MARGIN, 70f, paint(Color.parseColor("#928F84"), 8.5f))
        drawRight(canvas, "${model.periodStartLabel} – ${model.periodEndLabel}", PAGE_WIDTH - MARGIN, 100f, paint(Color.parseColor("#C2BFB4"), 9f))

        // ---------- Money summary ----------
        var y = 168f
        val colW = (PAGE_WIDTH - MARGIN * 2) / 3f
        drawSummaryCell(canvas, "PEMASUKAN", MoneyFormatter.format(model.totalIncome), MARGIN, y, colW, mint, bodyBold, label)
        drawSummaryCell(canvas, "PENGELUARAN", MoneyFormatter.format(model.totalExpense), MARGIN + colW, y, colW, coral, bodyBold, label)
        drawSummaryCell(
            canvas, "SISA BERSIH", MoneyFormatter.format(model.netBalance),
            MARGIN + colW * 2, y, colW, ink, bodyBold, label, alignRight = true
        )

        y += 46f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(hairline, 0.8f))

        // ---------- Period stats ----------
        y += 24f
        canvas.drawText("RINGKASAN PERIODE", MARGIN, y, label)
        y += 18f
        val stats = listOf(
            "Rata-rata pengeluaran / hari" to MoneyFormatter.format(model.avgExpensePerDay),
            "Hari ada pengeluaran" to "${model.activeDays} dari ${model.elapsedDays} hari",
            "Hari paling boros" to if (model.busiestDayLabel != null) {
                "${model.busiestDayLabel} · ${MoneyFormatter.format(model.busiestDayAmount)}"
            } else "Belum ada pengeluaran",
            "Rasio tabungan" to "${model.savingsRatioPercent}% dari pemasukan"
        )
        stats.forEach { (name, value) ->
            canvas.drawText(name, MARGIN, y, body)
            drawRight(canvas, value, PAGE_WIDTH - MARGIN, y, bodyBold)
            y += 17f
        }

        // ---------- Daily rhythm chart ----------
        if (model.dailyExpenses.isNotEmpty()) {
            y += 18f
            canvas.drawText("POLA PENGELUARAN HARIAN", MARGIN, y, label)
            y += 12f
            drawDailyChart(canvas, model.dailyExpenses, y, body, label)
            y += 132f
        }

        // ---------- Category composition ----------
        if (model.categories.isNotEmpty()) {
            y += 12f
            canvas.drawText("KOMPOSISI PENGELUARAN", MARGIN, y, label)
            y += 16f
            val barLeft = MARGIN
            val barRight = PAGE_WIDTH - MARGIN
            model.categories.take(8).forEach { line ->
                canvas.drawText(line.name, barLeft, y, body)
                drawRight(canvas, "${line.percent}% · ${MoneyFormatter.format(line.amount)}", barRight, y, bodyBold)
                y += 8f
                canvas.drawRoundRect(RectF(barLeft, y, barRight, y + 6f), 3f, 3f, paint(Color.parseColor("#EDEAE2"), 1f))
                val width = (barRight - barLeft) * (line.percent.coerceIn(0, 100) / 100f)
                if (width > 0f) {
                    canvas.drawRoundRect(RectF(barLeft, y, barLeft + maxOf(width, 3f), y + 6f), 3f, 3f, paint(coral, 1f))
                }
                y += 20f
            }
        }

        // ---------- Saldo ----------
        if (model.balances.isNotEmpty() || model.balanceTotal != 0L) {
            y += 8f
            canvas.drawText("SISA SALDO", MARGIN, y, label)
            y += 18f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(hairline, 0.8f))
            y += 20f

            canvas.drawText("Total di semua akun", MARGIN, y, bodyBold)
            drawRight(
                canvas,
                MoneyFormatter.format(model.balanceTotal),
                PAGE_WIDTH - MARGIN,
                y,
                Paint(bodyBold).apply {
                    textSize = 14f
                    color = if (model.balanceTotal < 0) coral else mint
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            )
            y += 20f

            model.balances.forEach { account ->
                if (y > PAGE_HEIGHT - 60f) return@forEach
                val name = ellipsize(account.name, body, PAGE_WIDTH - MARGIN * 2 - 120f)
                canvas.drawText(name, MARGIN, y, body)
                if (!account.includeInTotal) {
                    canvas.drawText(
                        "tidak dihitung",
                        MARGIN + body.measureText(name) + 6f,
                        y,
                        paint(grey, 7.5f)
                    )
                }
                drawRight(canvas, MoneyFormatter.format(account.amount), PAGE_WIDTH - MARGIN, y, bodyBold)
                y += 17f
            }
        }

        // ---------- Footer ----------
        val footerY = PAGE_HEIGHT - 34f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, paint(hairline, 0.8f))
        canvas.drawText("Dibuat otomatis oleh KEPENG · data tersimpan lokal di perangkat", MARGIN, footerY, paint(grey, 8f))
        drawRight(canvas, "Halaman 1", PAGE_WIDTH - MARGIN, footerY, paint(grey, 8f))
        thin.strokeWidth = 0.8f
        h2.textSize = 12f
    }

    private fun drawSummaryCell(
        canvas: Canvas,
        title: String,
        value: String,
        left: Float,
        baseline: Float,
        width: Float,
        valueColor: Int,
        valuePaint: Paint,
        labelPaint: Paint,
        alignRight: Boolean = false
    ) {
        val big = Paint(valuePaint).apply {
            textSize = 15f
            color = valueColor
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val text = ellipsize(value, big, width - 8f)
        if (alignRight) {
            canvas.drawText(title, left + width - 8f - labelPaint.measureText(title), baseline, labelPaint)
            drawRight(canvas, text, left + width - 8f, baseline + 22f, big)
        } else {
            canvas.drawText(title, left, baseline, labelPaint)
            canvas.drawText(text, left, baseline + 22f, big)
        }
    }

    /**
     * Grafik batang pengeluaran per hari. Tinggi 96 pt, sumbu bawah dengan label tanggal.
     */
    private fun drawDailyChart(
        canvas: Canvas,
        days: List<ReportPdfModel.DayBar>,
        top: Float,
        body: Paint,
        labelPaint: Paint
    ) {
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        val baseline = top + 96f
        val maxAmount = maxOf(1L, days.maxOfOrNull { it.amount } ?: 0L)

        canvas.drawLine(left, baseline, right, baseline, paint(hairline, 0.8f))

        val slot = (right - left) / days.size.coerceAtLeast(1)
        val barWidth = (slot * 0.62f).coerceAtLeast(2.5f)
        days.forEachIndexed { index, day ->
            if (day.amount <= 0L) return@forEachIndexed
            val ratio = day.amount.toFloat() / maxAmount.toFloat()
            val height = maxOf(3f, ratio * 88f)
            val x = left + slot * index + (slot - barWidth) / 2f
            val isPeak = day.amount == maxAmount
            canvas.drawRoundRect(
                RectF(x, baseline - height, x + barWidth, baseline),
                barWidth / 2f,
                barWidth / 2f,
                paint(if (isPeak) coral else Color.parseColor("#F5B5A8"), 1f)
            )
        }

        // Sumbu: tanggal 1, tengah, terakhir
        val first = days.first().dayOfMonth
        val last = days.last().dayOfMonth
        val mid = days[days.size / 2].dayOfMonth
        canvas.drawText("$first", left, baseline + 14f, labelPaint)
        canvas.drawText("$mid", (left + right) / 2f - labelPaint.measureText("$mid") / 2f, baseline + 14f, labelPaint)
        drawRight(canvas, "$last", right, baseline + 14f, labelPaint)

        val peak = days.maxByOrNull { it.amount }
        if (peak != null && peak.amount > 0) {
            drawRight(
                canvas,
                "Puncak ${peak.dayOfMonth} · ${MoneyFormatter.format(peak.amount)}",
                right,
                top + 6f,
                paint(grey, 8.5f)
            )
        }
    }

    private fun paint(color: Int, size: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun drawRight(canvas: Canvas, text: String, right: Float, baseline: Float, p: Paint) {
        canvas.drawText(text, right - p.measureText(text), baseline, p)
    }

    private fun ellipsize(text: String, p: Paint, maxWidth: Float): String {
        if (p.measureText(text) <= maxWidth) return text
        var cut = text
        while (cut.isNotEmpty() && p.measureText("$cut…") > maxWidth) {
            cut = cut.dropLast(1)
        }
        return if (cut.isEmpty()) "…" else "$cut…"
    }
}
