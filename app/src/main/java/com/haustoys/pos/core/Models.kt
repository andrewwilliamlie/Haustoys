package com.haustoys.pos.core

/** Amounts are whole Rupiah stored as Long. */
data class Product(
    val id: Long = 0,
    val name: String,
    val price: Long,
)

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    CARD("Card (EDC)"),
    QRIS("QRIS"),
}

data class SaleItem(
    val name: String,
    val price: Long,
    val qty: Int,
) {
    val subtotal: Long get() = price * qty
}

data class Sale(
    val id: Long,
    val createdAt: Long,
    val items: List<SaleItem>,
    val total: Long,
    val paid: Long,
    val change: Long,
    val method: PaymentMethod,
) {
    val itemCount: Int get() = items.sumOf { it.qty }
}

data class StoreInfo(
    val name: String = "HAUSTOYS",
    val address: String = "",
    val footer: String = "Thank you for shopping!",
)

data class DailySummary(
    val dayStart: Long,
    val transactions: Int,
    val itemsSold: Int,
    val total: Long,
    val byMethod: Map<PaymentMethod, Long>,
) {
    companion object {
        fun of(dayStart: Long, sales: List<Sale>) = DailySummary(
            dayStart = dayStart,
            transactions = sales.size,
            itemsSold = sales.sumOf { it.itemCount },
            total = sales.sumOf { it.total },
            byMethod = PaymentMethod.entries.associateWith { m ->
                sales.filter { it.method == m }.sumOf { it.total }
            },
        )
    }
}
