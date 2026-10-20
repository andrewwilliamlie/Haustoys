package com.haustoys.pos.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haustoys.pos.core.Money

@Composable
fun PosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1E5AA8),
            secondary = Color(0xFF2E7D32),
        ),
        content = content,
    )
}

/** Shows receipt text exactly as it prints, with Print and a dismiss button. */
@Composable
fun ReceiptDialog(
    title: String,
    text: String,
    onPrint: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Close",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Surface(tonalElevation = 2.dp, modifier = Modifier.heightIn(max = 420.dp)) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    softWrap = false,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
        dismissButton = { TextButton(onClick = onPrint) { Text("Print") } },
    )
}

/** Number-only amount input showing "Rp" grouping, e.g. 25.000. */
@Composable
fun MoneyField(value: Long?, onValueChange: (Long?) -> Unit, label: String, modifier: Modifier = Modifier) {
    // Keep the cursor at the end: grouping dots are re-inserted on every keystroke.
    val text = value?.let(Money::plain) ?: ""
    OutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { onValueChange(Money.parse(it.text)) },
        label = { Text(label) },
        prefix = { Text("Rp ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}
