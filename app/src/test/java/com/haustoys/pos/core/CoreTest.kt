package com.haustoys.pos.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class MoneyTest {
    @Test fun groupsThousands() {
        assertEquals("0", Money.plain(0))
        assertEquals("5.000", Money.plain(5_000))
        assertEquals("1.234.567", Money.plain(1_234_567))
        assertEquals("Rp 25.000", Money.rp(25_000))
    }

    @Test fun parsesUserInput() {
        assertEquals(25_000L, Money.parse("25.000"))
        assertEquals(25_000L, Money.parse("Rp 25,000"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
    }
}

class CartTest {
    private val toy = Product(1, "Toy", 25_000)
    private val wrap = Product(2, "Wrap", 5_000)

    @Test fun addingSameProductIncrementsQty() {
        val cart = Cart().add(toy).add(toy).add(wrap)
        assertEquals(2, cart.lines.size)
        assertEquals(3, cart.itemCount)
        assertEquals(55_000L, cart.total)
    }

    @Test fun qtyZeroRemovesLine() {
        val cart = Cart().add(toy).add(wrap).setQty(toy.id, 0)
        assertEquals(listOf(wrap.id), cart.lines.map { it.product.id })
    }
}

class ReceiptFormatterTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val formatter = ReceiptFormatter(width = 32, timeZone = utc)
    private val store = StoreInfo(name = "HAUSTOYS", address = "Jl. Contoh 1", footer = "Thank you!")
    private val sale = Sale(
        id = 7,
        createdAt = 1_760_918_400_000, // 2025-10-20 00:00 UTC
        items = listOf(SaleItem("A very long product name that must wrap", 25_000, 2), SaleItem("Wrap", 5_000, 1)),
        total = 55_000,
        paid = 100_000,
        change = 45_000,
        method = PaymentMethod.CASH,
    )

    @Test fun everyLineFitsThePaper() {
        val text = formatter.receipt(store, sale)
        text.lines().forEach { assertTrue("too wide: '$it'", it.length <= 32) }
    }

    @Test fun showsNumberTotalsAndChange() {
        val lines = formatter.receipt(store, sale).lines()
        assertEquals("INV-20251020-0007", formatter.receiptNo(sale))
        assertTrue(lines.contains("TOTAL" + " ".repeat(21) + "55.000"))
        assertTrue(lines.contains("CHANGE" + " ".repeat(20) + "45.000"))
        assertTrue(lines.contains("  2 x 25.000" + " ".repeat(14) + "50.000"))
    }

    @Test fun cardSaleHasNoChangeLine() {
        val text = formatter.receipt(store, sale.copy(method = PaymentMethod.CARD, paid = 55_000, change = 0))
        assertTrue(text.lines().none { it.startsWith("CHANGE") })
    }

    @Test fun dailyReportSumsByMethod() {
        val sales = listOf(sale, sale.copy(id = 8, total = 10_000, method = PaymentMethod.QRIS))
        val summary = DailySummary.of(sale.createdAt, sales)
        assertEquals(65_000L, summary.total)
        assertEquals(10_000L, summary.byMethod[PaymentMethod.QRIS])
        val text = formatter.dailyReport(store, summary, sale.createdAt)
        text.lines().forEach { assertTrue("too wide: '$it'", it.length <= 32) }
        assertTrue(text.contains("TOTAL" + " ".repeat(21) + "65.000"))
    }
}
