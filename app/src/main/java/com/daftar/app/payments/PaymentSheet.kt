package com.daftar.app.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.theme.DaftarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    customers: List<CustomerEntity>,
    types: List<ItemTypeEntity>,
    suggestionLabel: (typeId: String, askedUnit: Long) -> String,
    onSave: (amount: Long, customerId: String?, itemTypeId: String?, askedUnit: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<ItemTypeEntity?>(null) }
    val amount = ArabicNumbers.parseAmount(amountText)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("دفعة جديدة", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = amountText,
                onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                label = { Text("المبلغ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth(),
            )
            if (types.isNotEmpty()) {
                Text(
                    "عن ماذا؟ (اختياري — D37)",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("غير محدد") },
                        )
                    }
                    items(types, key = { it.id }) { type ->
                        FilterChip(
                            selected = selectedType?.id == type.id,
                            onClick = {
                                selectedType = type
                                if (amountText.isBlank()) {
                                    amountText = type.askingPrice.toString()
                                }
                            },
                            label = { Text("${type.name} ${ArabicNumbers.format(type.askingPrice)}") },
                        )
                    }
                }
                selectedType?.let { type ->
                    Text(
                        "المصدر تقريباً: ${suggestionLabel(type.id, type.askingPrice)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DaftarColors.TextSecondary,
                    )
                }
            }
            Text(
                "من؟ (اختياري — D11)",
                style = MaterialTheme.typography.labelMedium,
                color = DaftarColors.TextSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCustomerId == null,
                        onClick = { selectedCustomerId = null },
                        label = { Text("غير محدد") },
                    )
                }
                items(customers, key = { it.id }) { customer ->
                    FilterChip(
                        selected = selectedCustomerId == customer.id,
                        onClick = { selectedCustomerId = customer.id },
                        label = { Text(customer.name) },
                    )
                }
            }
            Button(
                onClick = {
                    onSave(amount, selectedCustomerId, selectedType?.id, selectedType?.askingPrice)
                },
                enabled = amount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("حفظ الدفعة")
            }
        }
    }
}
