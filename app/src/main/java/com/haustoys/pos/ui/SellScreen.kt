package com.haustoys.pos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haustoys.pos.PosViewModel
import com.haustoys.pos.core.Cart
import com.haustoys.pos.core.Money
import com.haustoys.pos.core.PaymentMethod
import com.haustoys.pos.core.Product

@Composable
fun SellScreen(vm: PosViewModel, modifier: Modifier = Modifier) {
    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    var showCart by rememberSaveable { mutableStateOf(false) }
    var showCheckout by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (showCart && !cart.isEmpty) {
                CartList(cart, onQty = vm::setQty)
            } else {
                ProductGrid(
                    products = products.filter { it.name.contains(query.trim(), ignoreCase = true) },
                    query = query,
                    onQuery = { query = it },
                    onAdd = vm::addToCart,
                )
            }
        }
        CartBar(
            cart = cart,
            showingCart = showCart && !cart.isEmpty,
            onToggleCart = { showCart = !showCart },
            onClear = { vm.clearCart(); showCart = false },
            onCharge = { showCheckout = true },
        )
    }

    if (showCheckout && !cart.isEmpty) {
        CheckoutDialog(
            total = cart.total,
            onConfirm = { method, paid ->
                vm.checkout(method, paid)
                showCheckout = false
                showCart = false
            },
            onDismiss = { showCheckout = false },
        )
    }
}

@Composable
private fun ProductGrid(products: List<Product>, query: String, onQuery: (String) -> Unit, onAdd: (Product) -> Unit) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search products") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (products.isEmpty()) {
            Text(
                if (query.isBlank()) "No products yet. Add some in the Products tab." else "No match for \"$query\"",
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(products, key = { it.id }) { product ->
                Card(Modifier.clickable { onAdd(product) }) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(40.dp),
                        )
                        Text(Money.rp(product.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CartList(cart: Cart, onQty: (Long, Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(cart.lines, key = { it.product.id }) { line ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(line.product.name, style = MaterialTheme.typography.titleSmall)
                    Text(Money.rp(line.product.price), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { onQty(line.product.id, line.qty - 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("−") }
                Text(line.qty.toString(), modifier = Modifier.width(36.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                OutlinedButton(onClick = { onQty(line.product.id, line.qty + 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("+") }
                Text(
                    Money.plain(line.subtotal),
                    modifier = Modifier.width(88.dp),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CartBar(cart: Cart, showingCart: Boolean, onToggleCart: () -> Unit, onClear: () -> Unit, onCharge: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgedBox(badge = { if (cart.itemCount > 0) Badge { Text(cart.itemCount.toString()) } }) {
                    TextButton(onClick = onToggleCart, enabled = !cart.isEmpty) {
                        Text(if (showingCart) "Back to products" else "View cart")
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(Money.rp(cart.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, enabled = !cart.isEmpty) { Text("Clear") }
                Button(onClick = onCharge, enabled = !cart.isEmpty, modifier = Modifier.weight(1f)) { Text("Charge") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutDialog(total: Long, onConfirm: (PaymentMethod, Long) -> Unit, onDismiss: () -> Unit) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var cash by remember { mutableStateOf<Long?>(null) }
    val paid = if (method == PaymentMethod.CASH) cash ?: 0L else total
    val canPay = paid >= total

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Total ${Money.rp(total)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.label) })
                    }
                }
                if (method == PaymentMethod.CASH) {
                    MoneyField(cash, { cash = it }, label = "Cash received")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickCashAmounts(total).forEach { amount ->
                            OutlinedButton(onClick = { cash = amount }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(if (amount == total) "Exact" else Money.plain(amount))
                            }
                        }
                    }
                    Text(
                        if (canPay) "Change: ${Money.rp(paid - total)}" else "Short: ${Money.rp(total - paid)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (canPay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Take the ${method.label} payment on the bank's EDC/QRIS app first, then confirm here to record the sale.")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(method, paid) }, enabled = canPay) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "Exact" plus the next two round banknote totals above it, e.g. 37.000 -> 37.000, 50.000, 100.000. */
private fun quickCashAmounts(total: Long): List<Long> {
    val notes = listOf(10_000L, 20_000L, 50_000L, 100_000L)
    val rounded = notes.map { note -> ((total + note - 1) / note) * note }.filter { it > total }.distinct().sorted()
    return (listOf(total) + rounded).take(3)
}
