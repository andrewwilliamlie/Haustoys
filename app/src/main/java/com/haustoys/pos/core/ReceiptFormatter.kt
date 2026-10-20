package com.haustoys.pos.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds plain-text receipts for a 58mm thermal printer. PAX printers fit
 * 32 characters per line with the 12x24 ASCII font. The same text is shown
 * on screen as the receipt preview.
 */
class ReceiptFormatter(
    private val width: Int = 32,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {
    private val divider get() = "-".repeat(width)

    fun receiptNo(sale: Sale): String = "INV-" + format("yyyyMMdd", sale.createdAt) + "-" + sale.id.toString().padStart(4, '0')

    fun receipt(store: StoreInfo, sale: Sale): String = buildList {
        addAll(header(store))
        add(divider)
        add(row("No", receiptNo(sale)))
        add(row("Date", format("dd/MM/yyyy HH:mm", sale.createdAt)))
        add(divider)
        sale.items.forEach { item ->
            addAll(wrap(item.name))
            add(row("  ${item.qty} x ${Money.plain(item.price)}", Money.plain(item.subtotal)))
        }
        add(divider)
        add(row("TOTAL", Money.plain(sale.total)))
        add(row(sale.method.label.uppercase(), Money.plain(sale.paid)))
        if (sale.method == PaymentMethod.CASH) add(row("CHANGE", Money.plain(sale.change)))
        add(divider)
        store.footer.lines().filter { it.isNotBlank() }.forEach { addAll(wrap(it).map(::center)) }
    }.joinToString("\n")

    fun dailyReport(store: StoreInfo, summary: DailySummary, printedAt: Long): String = buildList {
        addAll(header(store))
        add(divider)
        add(center("DAILY SALES REPORT"))
        add(row("Date", format("dd/MM/yyyy", summary.dayStart)))
        add(row("Printed", format("dd/MM/yyyy HH:mm", printedAt)))
        add(divider)
        add(row("Transactions", summary.transactions.toString()))
        add(row("Items sold", summary.itemsSold.toString()))
        add(divider)
        PaymentMethod.entries.forEach { add(row(it.label, Money.plain(summary.byMethod[it] ?: 0L))) }
        add(divider)
        add(row("TOTAL", Money.plain(summary.total)))
        add(divider)
    }.joinToString("\n")

    private fun header(store: StoreInfo): List<String> =
        (listOf(store.name) + store.address.lines())
            .filter { it.isNotBlank() }
            .flatMap { wrap(it) }
            .map(::center)

    /** Left text and right-aligned value on one line; the left side is cut if both don't fit. */
    private fun row(left: String, right: String): String {
        val room = (width - right.length - 1).coerceAtLeast(0)
        val l = if (left.length > room) left.take(room) else left
        return l + " ".repeat((width - l.length - right.length).coerceAtLeast(1)) + right
    }

    private fun center(text: String): String {
        val t = text.trim().take(width)
        return " ".repeat((width - t.length) / 2) + t
    }

    private fun wrap(text: String): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in text.trim().split(Regex("\\s+"))) {
            word.chunked(width).forEach { part ->
                line = when {
                    line.isEmpty() -> part
                    line.length + 1 + part.length <= width -> "$line $part"
                    else -> { out += line; part }
                }
            }
        }
        if (line.isNotEmpty()) out += line
        return out
    }

    private fun format(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = this@ReceiptFormatter.timeZone }.format(Date(millis))
}
