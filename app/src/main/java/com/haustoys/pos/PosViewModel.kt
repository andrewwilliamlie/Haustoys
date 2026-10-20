package com.haustoys.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haustoys.pos.core.Cart
import com.haustoys.pos.core.DailySummary
import com.haustoys.pos.core.PaymentMethod
import com.haustoys.pos.core.Product
import com.haustoys.pos.core.ReceiptFormatter
import com.haustoys.pos.core.Sale
import com.haustoys.pos.core.SaleItem
import com.haustoys.pos.core.StoreInfo
import com.haustoys.pos.data.PosDatabase
import com.haustoys.pos.data.StoreSettings
import com.haustoys.pos.print.PrinterStatus
import com.haustoys.pos.print.createPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

class PosViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PosDatabase(app)
    private val settings = StoreSettings(app)
    private val printer = createPrinter(app)
    private val printLock = Mutex()
    val formatter = ReceiptFormatter()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()

    /** The sale just completed, shown in the receipt dialog until dismissed. */
    private val _lastSale = MutableStateFlow<Sale?>(null)
    val lastSale: StateFlow<Sale?> = _lastSale.asStateFlow()

    private val _day = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val day: StateFlow<Long> = _day.asStateFlow()

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()

    private val _store = MutableStateFlow(settings.store)
    val store: StateFlow<StoreInfo> = _store.asStateFlow()

    private val _autoPrint = MutableStateFlow(settings.autoPrint)
    val autoPrint: StateFlow<Boolean> = _autoPrint.asStateFlow()

    private val _printerStatus = MutableStateFlow(PrinterStatus(false, "Checking..."))
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()
    val printerName: String get() = printer.name

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    init {
        reloadProducts()
        reloadSales()
        refreshPrinterStatus()
    }

    // --- Products ---

    fun saveProduct(id: Long?, name: String, price: Long) = io {
        if (id == null) db.addProduct(name.trim(), price) else db.updateProduct(Product(id, name.trim(), price))
        reloadProductsNow()
    }

    fun deleteProduct(product: Product) = io {
        db.deleteProduct(product.id)
        _cart.update { it.remove(product.id) }
        reloadProductsNow()
    }

    // --- Cart & checkout ---

    fun addToCart(product: Product) = _cart.update { it.add(product) }
    fun setQty(productId: Long, qty: Int) = _cart.update { it.setQty(productId, qty) }
    fun clearCart() = _cart.update { Cart() }

    fun checkout(method: PaymentMethod, paid: Long) = io {
        val cart = _cart.value
        if (cart.isEmpty) return@io
        val sale = db.saveSale(cart, paid, method, System.currentTimeMillis())
        _cart.value = Cart()
        _lastSale.value = sale
        if (_day.value == startOfDay(sale.createdAt)) reloadSalesNow()
        if (_autoPrint.value) printNow(receiptText(sale))
    }

    fun dismissLastSale() {
        _lastSale.value = null
    }

    // --- History ---

    fun shiftDay(days: Int) {
        _day.value = Calendar.getInstance().apply {
            timeInMillis = _day.value
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis
        reloadSales()
    }

    fun summary(): DailySummary = DailySummary.of(_day.value, _sales.value)

    // --- Printing ---

    fun receiptText(sale: Sale): String = formatter.receipt(_store.value, sale)

    fun printReceipt(sale: Sale) = io { printNow(receiptText(sale)) }

    fun printDailyReport() = io {
        printNow(formatter.dailyReport(_store.value, summary(), System.currentTimeMillis()))
    }

    fun printTest() = io {
        printNow(formatter.receipt(_store.value, SAMPLE_SALE.copy(createdAt = System.currentTimeMillis())))
    }

    fun refreshPrinterStatus() = io { _printerStatus.value = printer.status() }

    // --- Settings ---

    fun saveStore(info: StoreInfo) {
        settings.store = info
        _store.value = info
        _messages.tryEmit("Store details saved")
    }

    fun setAutoPrint(enabled: Boolean) {
        settings.autoPrint = enabled
        _autoPrint.value = enabled
    }

    // --- Internals ---

    private suspend fun printNow(text: String) {
        printLock.withLock {
            try {
                printer.print(text)
            } catch (e: Exception) {
                _messages.emit("Print failed: ${e.message}")
            }
            _printerStatus.value = printer.status()
        }
    }

    private fun reloadProducts() = io { reloadProductsNow() }
    private fun reloadProductsNow() {
        _products.value = db.products()
    }

    private fun reloadSales() = io { reloadSalesNow() }
    private fun reloadSalesNow() {
        val from = _day.value
        val to = Calendar.getInstance().apply {
            timeInMillis = from
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        _sales.value = db.salesBetween(from, to)
    }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: Exception) {
                _messages.emit("Error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        db.close()
    }

    companion object {
        fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private val SAMPLE_SALE = Sale(
            id = 0,
            createdAt = 0,
            items = listOf(SaleItem("TEST PRINT", 1_000, 1)),
            total = 1_000,
            paid = 1_000,
            change = 0,
            method = PaymentMethod.CASH,
        )
    }
}
