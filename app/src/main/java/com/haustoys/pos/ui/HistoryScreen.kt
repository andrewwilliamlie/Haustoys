package com.haustoys.pos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haustoys.pos.PosViewModel
import com.haustoys.pos.core.DailySummary
import com.haustoys.pos.core.Money
import com.haustoys.pos.core.PaymentMethod
import com.haustoys.pos.core.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: PosViewModel, modifier: Modifier = Modifier) {
    val day by vm.day.collectAsStateWithLifecycle()
    val sales by vm.sales.collectAsStateWithLifecycle()
    val summary = remember(day, sales) { DailySummary.of(day, sales) }
    var viewing by remember { mutableStateOf<Sale?>(null) }
    val isToday = day == PosViewModel.startOfDay(System.currentTimeMillis())

    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = { vm.shiftDay(-1) }) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous day") }
            Text(
                (if (isToday) "Today, " else "") + SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).format(Date(day)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.shiftDay(1) }, enabled = !isToday) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }

        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                SummaryRow("Total sales", Money.rp(summary.total), bold = true)
                SummaryRow("Transactions", summary.transactions.toString())
                SummaryRow("Items sold", summary.itemsSold.toString())
                PaymentMethod.entries.forEach { SummaryRow(it.label, Money.rp(summary.byMethod[it] ?: 0L)) }
                OutlinedButton(onClick = vm::printDailyReport, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Print daily report")
                }
            }
        }

        if (sales.isEmpty()) {
            Text("No sales on this day.", modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.weight(1f)) {
            items(sales, key = { it.id }) { sale ->
                ListItem(
                    headlineContent = { Text(vm.formatter.receiptNo(sale)) },
                    supportingContent = {
                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sale.createdAt)) + " · ${sale.itemCount} items · ${sale.method.label}")
                    },
                    trailingContent = { Text(Money.rp(sale.total), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.clickable { viewing = sale },
                )
                HorizontalDivider()
            }
        }
    }

    viewing?.let { sale ->
        ReceiptDialog(
            title = vm.formatter.receiptNo(sale),
            text = vm.receiptText(sale),
            onPrint = { vm.printReceipt(sale) },
            onDismiss = { viewing = null },
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontWeight = weight)
        Spacer(Modifier.weight(1f))
        Text(value, fontWeight = weight)
    }
}
