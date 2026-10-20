package com.haustoys.pos.core

data class CartLine(val product: Product, val qty: Int) {
    val subtotal: Long get() = product.price * qty
}

/** Immutable cart; every change returns a new Cart. */
data class Cart(val lines: List<CartLine> = emptyList()) {
    val total: Long get() = lines.sumOf { it.subtotal }
    val itemCount: Int get() = lines.sumOf { it.qty }
    val isEmpty: Boolean get() = lines.isEmpty()

    fun add(product: Product): Cart {
        val existing = lines.find { it.product.id == product.id }
            ?: return Cart(lines + CartLine(product, 1))
        return setQty(product.id, existing.qty + 1)
    }

    fun setQty(productId: Long, qty: Int): Cart =
        if (qty <= 0) remove(productId)
        else Cart(lines.map { if (it.product.id == productId) it.copy(qty = qty) else it })

    fun remove(productId: Long): Cart = Cart(lines.filterNot { it.product.id == productId })

    fun toSaleItems(): List<SaleItem> = lines.map { SaleItem(it.product.name, it.product.price, it.qty) }
}
