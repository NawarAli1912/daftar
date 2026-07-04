package com.daftar.app.kernel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.format.ArabicNumbers

// D42 (field review #1): money moves in taps, not typing — ±step per tap,
// tap the number itself to type fractions.
@Composable
fun AmountField(
    value: Long,
    onValue: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    step: Long = 500,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { onValue((value - step).coerceAtLeast(0)) },
            modifier = Modifier.width(56.dp),
        ) { Text("−") }
        OutlinedTextField(
            value = if (value == 0L) "" else value.toString(),
            onValueChange = { new -> onValue(ArabicNumbers.parseAmount(new)) },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = { onValue(value + step) },
            modifier = Modifier.width(56.dp),
        ) { Text("+") }
    }
}

@Composable
fun QtyField(
    value: Int,
    onValue: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { onValue((value - 1).coerceAtLeast(0)) },
            modifier = Modifier.width(56.dp),
        ) { Text("−") }
        OutlinedTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { new -> onValue(ArabicNumbers.parseAmount(new).toInt()) },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = { onValue(value + 1) },
            modifier = Modifier.width(56.dp),
        ) { Text("+") }
    }
}
