package com.haustoys.pos.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.haustoys.pos.core.Cart
import com.haustoys.pos.core.PaymentMethod
import com.haustoys.pos.core.Product
import com.haustoys.pos.core.Sale
import com.haustoys.pos.core.SaleItem

/** All calls block; run them off the main thread. */
class PosDatabase(context: Context) : SQLiteOpenHelper(context, "pos.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                created_at INTEGER NOT NULL,
                total INTEGER NOT NULL,
                paid INTEGER NOT NULL,
                change_amount INTEGER NOT NULL,
                method TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL REFERENCES sales(id),
                name TEXT NOT NULL,
                price INTEGER NOT NULL,
                qty INTEGER NOT NULL)"""
        )
        db.execSQL("CREATE INDEX idx_sales_created_at ON sales(created_at)")
        db.execSQL("CREATE INDEX idx_sale_items_sale_id ON sale_items(sale_id)")

        // A few sample products so the till isn't empty on first launch.
        listOf("Sample Toy Car" to 25_000L, "Sample Puzzle" to 40_000L, "Sample Doll" to 55_000L, "Gift Wrap" to 5_000L)
            .forEach { (name, price) -> db.insert("products", null, productValues(name, price)) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun products(): List<Product> =
        readableDatabase.rawQuery("SELECT id, name, price FROM products ORDER BY name COLLATE NOCASE", null).use { c ->
            buildList { while (c.moveToNext()) add(Product(c.getLong(0), c.getString(1), c.getLong(2))) }
        }

    fun addProduct(name: String, price: Long) {
        writableDatabase.insert("products", null, productValues(name, price))
    }

    fun updateProduct(product: Product) {
        writableDatabase.update("products", productValues(product.name, product.price), "id = ?", arrayOf(product.id.toString()))
    }

    fun deleteProduct(id: Long) {
        writableDatabase.delete("products", "id = ?", arrayOf(id.toString()))
    }

    fun saveSale(cart: Cart, paid: Long, method: PaymentMethod, now: Long): Sale {
        val db = writableDatabase
        val total = cart.total
        val change = if (method == PaymentMethod.CASH) paid - total else 0L
        val items = cart.toSaleItems()
        db.beginTransaction()
        try {
            val saleId = db.insertOrThrow("sales", null, ContentValues().apply {
                put("created_at", now)
                put("total", total)
                put("paid", paid)
                put("change_amount", change)
                put("method", method.name)
            })
            items.forEach { item ->
                db.insertOrThrow("sale_items", null, ContentValues().apply {
                    put("sale_id", saleId)
                    put("name", item.name)
                    put("price", item.price)
                    put("qty", item.qty)
                })
            }
            db.setTransactionSuccessful()
            return Sale(saleId, now, items, total, paid, change, method)
        } finally {
            db.endTransaction()
        }
    }

    /** Sales with createdAt in [from, to), newest first. */
    fun salesBetween(from: Long, to: Long): List<Sale> {
        val db = readableDatabase
        val args = arrayOf(from.toString(), to.toString())
        val items = db.rawQuery(
            """SELECT i.sale_id, i.name, i.price, i.qty FROM sale_items i
               JOIN sales s ON s.id = i.sale_id
               WHERE s.created_at >= ? AND s.created_at < ? ORDER BY i.id""", args
        ).use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0) to SaleItem(c.getString(1), c.getLong(2), c.getInt(3))) }
        }.groupBy({ it.first }, { it.second })

        return db.rawQuery(
            "SELECT id, created_at, total, paid, change_amount, method FROM sales WHERE created_at >= ? AND created_at < ? ORDER BY created_at DESC",
            args
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    add(
                        Sale(
                            id = id,
                            createdAt = c.getLong(1),
                            items = items[id].orEmpty(),
                            total = c.getLong(2),
                            paid = c.getLong(3),
                            change = c.getLong(4),
                            method = PaymentMethod.entries.find { it.name == c.getString(5) } ?: PaymentMethod.CASH,
                        )
                    )
                }
            }
        }
    }

    private fun productValues(name: String, price: Long) = ContentValues().apply {
        put("name", name)
        put("price", price)
    }
}
