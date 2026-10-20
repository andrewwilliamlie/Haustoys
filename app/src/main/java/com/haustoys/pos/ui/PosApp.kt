package com.haustoys.pos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haustoys.pos.PosViewModel

private enum class Tab(val title: String, val icon: ImageVector) {
    SELL("Sell", Icons.Filled.ShoppingCart),
    PRODUCTS("Products", Icons.Filled.List),
    HISTORY("History", Icons.Filled.DateRange),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(vm: PosViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.SELL) }
    val snackbar = remember { SnackbarHostState() }
    val store by vm.store.collectAsStateWithLifecycle()
    val lastSale by vm.lastSale.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (tab == Tab.SELL) store.name else tab.title) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.SELL -> SellScreen(vm, modifier)
            Tab.PRODUCTS -> ProductsScreen(vm, modifier)
            Tab.HISTORY -> HistoryScreen(vm, modifier)
            Tab.SETTINGS -> SettingsScreen(vm, modifier)
        }
    }

    lastSale?.let { sale ->
        ReceiptDialog(
            title = "Payment complete",
            text = vm.receiptText(sale),
            onPrint = { vm.printReceipt(sale) },
            onDismiss = vm::dismissLastSale,
            dismissLabel = "New sale",
        )
    }
}
