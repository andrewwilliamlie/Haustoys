package com.haustoys.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.haustoys.pos.core.StoreInfo

@Composable
fun SettingsScreen(vm: PosViewModel, modifier: Modifier = Modifier) {
    val store by vm.store.collectAsStateWithLifecycle()
    val autoPrint by vm.autoPrint.collectAsStateWithLifecycle()
    val status by vm.printerStatus.collectAsStateWithLifecycle()
    var name by remember(store) { mutableStateOf(store.name) }
    var address by remember(store) { mutableStateOf(store.address) }
    var footer by remember(store) { mutableStateOf(store.footer) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Receipt header & footer", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text("Store name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("Address / phone") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(footer, { footer = it }, label = { Text("Footer message") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { vm.saveStore(StoreInfo(name.trim(), address.trim(), footer.trim())) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Printer", style = MaterialTheme.typography.titleMedium)
        Text("${vm.printerName}: ${status.message}", color = if (status.ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Print receipt automatically after each sale", modifier = Modifier.weight(1f))
            Spacer(Modifier.padding(4.dp))
            Switch(checked = autoPrint, onCheckedChange = vm::setAutoPrint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::refreshPrinterStatus) { Text("Check status") }
            OutlinedButton(onClick = vm::printTest) { Text("Test print") }
        }
    }
}
