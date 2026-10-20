package com.haustoys.pos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haustoys.pos.PosViewModel
import com.haustoys.pos.core.Money
import com.haustoys.pos.core.Product

@Composable
fun ProductsScreen(vm: PosViewModel, modifier: Modifier = Modifier) {
    val products by vm.products.collectAsStateWithLifecycle()
    // null = closed; Product(id = 0) = adding a new product.
    var editing by remember { mutableStateOf<Product?>(null) }
    var deleting by remember { mutableStateOf<Product?>(null) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(products, key = { it.id }) { product ->
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = { Text(Money.rp(product.price)) },
                    trailingContent = {
                        IconButton(onClick = { deleting = product }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${product.name}")
                        }
                    },
                    modifier = Modifier.clickable { editing = product },
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(
            onClick = { editing = Product(name = "", price = 0) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add product")
        }
    }

    editing?.let { product ->
        ProductDialog(
            product = product,
            onSave = { name, price ->
                vm.saveProduct(product.id.takeIf { it != 0L }, name, price)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    deleting?.let { product ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete product?") },
            text = { Text("\"${product.name}\" will be removed. Past sales keep their records.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProduct(product); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProductDialog(product: Product, onSave: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.takeIf { it > 0 }) }
    val valid = name.isNotBlank() && (price ?: 0L) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product.id == 0L) "New product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(price, { price = it }, label = "Price")
            }
        },
        confirmButton = { Button(onClick = { onSave(name, price ?: 0L) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
